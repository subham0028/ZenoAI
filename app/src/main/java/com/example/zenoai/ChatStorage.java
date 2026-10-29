package com.example.zenoai;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Storage manager for saving and retrieving chat messages and preferences.
 */
public class ChatStorage {
    private static final String PREF_NAME = "zeno_ai_prefs";
    private static final String KEY_MESSAGES = "chat_messages";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_PROVIDER = "provider";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveMessages(Context context, List<ChatMessage> messages) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        Gson gson = new Gson();
        String json = gson.toJson(messages);
        editor.putString(KEY_MESSAGES, json);
        editor.apply();
    }

    public static List<ChatMessage> getMessages(Context context) {
        String json = getPrefs(context).getString(KEY_MESSAGES, null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        Gson gson = new Gson();
        Type type = new TypeToken<List<ChatMessage>>() {}.getType();
        List<ChatMessage> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public static void clearMessages(Context context) {
        SharedPreferences.Editor editor = getPrefs(context).edit();
        editor.remove(KEY_MESSAGES);
        editor.apply();
    }

    public static void setDarkMode(Context context, boolean isDark) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, isDark).apply();
    }

    public static boolean isDarkMode(Context context) {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    public static void saveApiKey(Context context, String apiKey) {
        getPrefs(context).edit().putString(KEY_API_KEY, apiKey.trim()).apply();
    }

    public static String getApiKey(Context context) {
        return getPrefs(context).getString(KEY_API_KEY, "");
    }

    public static void saveBaseUrl(Context context, String url) {
        getPrefs(context).edit().putString(KEY_BASE_URL, url.trim()).apply();
    }

    public static String getBaseUrl(Context context) {
        return getPrefs(context).getString(KEY_BASE_URL, ApiConfig.DEFAULT_GEMINI_URL);
    }

    public static void saveProvider(Context context, String provider) {
        getPrefs(context).edit().putString(KEY_PROVIDER, provider).apply();
    }

    public static String getProvider(Context context) {
        return getPrefs(context).getString(KEY_PROVIDER, ApiConfig.PROVIDER_GEMINI);
    }
}