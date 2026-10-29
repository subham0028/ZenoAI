package com.example.zenoai;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Service to handle network communication with AI backend API asynchronously.
 */
public class AIService {

    public interface AICallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
        void onNotConfigured();
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final Handler mainHandler;

    public AIService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    public void sendMessage(Context context, List<ChatMessage> history, String userPrompt, AICallback callback) {
        if (!isNetworkAvailable(context)) {
            mainHandler.post(() -> callback.onError(context.getString(R.string.error_no_internet)));
            return;
        }

        String apiKey = ChatStorage.getApiKey(context);
        if (apiKey.isEmpty()) {
            mainHandler.post(callback::onNotConfigured);
            return;
        }

        String provider = ChatStorage.getProvider(context);
        String baseUrl = ChatStorage.getBaseUrl(context);

        if (ApiConfig.PROVIDER_OPENAI.equalsIgnoreCase(provider)) {
            sendOpenAIRequest(baseUrl, apiKey, history, userPrompt, callback);
        } else {
            sendGeminiRequest(baseUrl, apiKey, history, userPrompt, callback);
        }
    }

    private void sendGeminiRequest(String baseUrl, String apiKey, List<ChatMessage> history, String userPrompt, AICallback callback) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String endpointUrl = baseUrl + "v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        JsonObject rootJson = new JsonObject();
        
        // Add System Instruction
        JsonObject systemInst = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysTextPart = new JsonObject();
        sysTextPart.addProperty("text", ApiConfig.SYSTEM_INSTRUCTION);
        sysParts.add(sysTextPart);
        systemInst.add("parts", sysParts);
        rootJson.add("system_instruction", systemInst);

        // Contents array
        JsonArray contentsArray = new JsonArray();

        // Include up to 10 recent context messages
        int startIdx = Math.max(0, history.size() - 10);
        for (int i = startIdx; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            JsonObject item = new JsonObject();
            item.addProperty("role", msg.getSender() == ChatMessage.SENDER_USER ? "user" : "model");
            JsonArray parts = new JsonArray();
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", msg.getText());
            parts.add(textPart);
            item.add("parts", parts);
            contentsArray.add(item);
        }

        // Add current prompt
        JsonObject currentItem = new JsonObject();
        currentItem.addProperty("role", "user");
        JsonArray currentParts = new JsonArray();
        JsonObject currentTextPart = new JsonObject();
        currentTextPart.addProperty("text", userPrompt);
        currentParts.add(currentTextPart);
        currentItem.add("parts", currentParts);
        contentsArray.add(currentItem);

        rootJson.add("contents", contentsArray);

        RequestBody body = RequestBody.create(rootJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(endpointUrl)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network request failed: " + e.getLocalizedMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    mainHandler.post(() -> callback.onError("API Error (" + response.code() + "): " + extractErrorDetail(errorBody)));
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                try {
                    JsonObject resObj = JsonParser.parseString(responseBody).getAsJsonObject();
                    JsonArray candidates = resObj.getAsJsonArray("candidates");
                    if (candidates != null && candidates.size() > 0) {
                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        JsonObject content = candidate.getAsJsonObject("content");
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts != null && parts.size() > 0) {
                            String reply = parts.get(0).getAsJsonObject().get("text").getAsString();
                            mainHandler.post(() -> callback.onSuccess(reply));
                            return;
                        }
                    }
                    mainHandler.post(() -> callback.onError("Received empty or unexpected response format from AI."));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Failed to parse AI response: " + e.getMessage()));
                }
            }
        });
    }

    private void sendOpenAIRequest(String baseUrl, String apiKey, List<ChatMessage> history, String userPrompt, AICallback callback) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        String endpointUrl = baseUrl + "v1/chat/completions";

        JsonObject rootJson = new JsonObject();
        rootJson.addProperty("model", "gpt-3.5-turbo");

        JsonArray messagesArray = new JsonArray();

        // System prompt
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", ApiConfig.SYSTEM_INSTRUCTION);
        messagesArray.add(sysMsg);

        int startIdx = Math.max(0, history.size() - 10);
        for (int i = startIdx; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            JsonObject item = new JsonObject();
            item.addProperty("role", msg.getSender() == ChatMessage.SENDER_USER ? "user" : "assistant");
            item.addProperty("content", msg.getText());
            messagesArray.add(item);
        }

        JsonObject currentItem = new JsonObject();
        currentItem.addProperty("role", "user");
        currentItem.addProperty("content", userPrompt);
        messagesArray.add(currentItem);

        rootJson.add("messages", messagesArray);

        RequestBody body = RequestBody.create(rootJson.toString(), JSON);
        Request request = new Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network request failed: " + e.getLocalizedMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    mainHandler.post(() -> callback.onError("API Error (" + response.code() + "): " + extractErrorDetail(errorBody)));
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                try {
                    JsonObject resObj = JsonParser.parseString(responseBody).getAsJsonObject();
                    JsonArray choices = resObj.getAsJsonArray("choices");
                    if (choices != null && choices.size() > 0) {
                        JsonObject choice = choices.get(0).getAsJsonObject();
                        JsonObject msg = choice.getAsJsonObject("message");
                        String reply = msg.get("content").getAsString();
                        mainHandler.post(() -> callback.onSuccess(reply));
                        return;
                    }
                    mainHandler.post(() -> callback.onError("Received empty response from OpenAI."));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Failed to parse OpenAI response: " + e.getMessage()));
                }
            }
        });
    }

    private String extractErrorDetail(String jsonBody) {
        try {
            JsonObject obj = JsonParser.parseString(jsonBody).getAsJsonObject();
            if (obj.has("error")) {
                JsonObject errObj = obj.getAsJsonObject("error");
                if (errObj.has("message")) {
                    return errObj.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return "Check API key and URL permissions.";
    }
}