package com.example.zenoai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Handles Speech-to-Text and Text-to-Speech operations for Zeno AI.
 */
public class VoiceAssistant implements TextToSpeech.OnInitListener {

    private static final String TAG = "VoiceAssistant";
    private static final String UTTERANCE_ID = "ZENO_TTS_ID";

    public interface VoiceListener {
        void onSpeechResult(String recognizedText);
        void onSpeechError(String errorMessage);
        void onSpeechStart();
        void onSpeechEnd();
        void onTtsFinished();
    }

    private final Context context;
    private VoiceListener listener;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private boolean isTtsInitialized = false;
    private boolean isListening = false;

    public VoiceAssistant(Context context, VoiceListener listener) {
        this.context = context;
        this.listener = listener;

        initSpeechRecognizer();
        initTextToSpeech();
    }

    private void initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    isListening = true;
                    if (listener != null) listener.onSpeechStart();
                }

                @Override
                public void onBeginningOfSpeech() {}

                @Override
                public void onRmsChanged(float rmsdB) {}

                @Override
                public void onBufferReceived(byte[] buffer) {}

                @Override
                public void onEndOfSpeech() {
                    isListening = false;
                    if (listener != null) listener.onSpeechEnd();
                }

                @Override
                public void onError(int error) {
                    isListening = false;
                    String message = getSpeechErrorMessage(error);
                    Log.e(TAG, "Speech Error: " + message);
                    if (listener != null) listener.onSpeechError(message);
                }

                @Override
                public void onResults(Bundle results) {
                    isListening = false;
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        String spokenText = matches.get(0);
                        if (listener != null) listener.onSpeechResult(spokenText);
                    } else {
                        if (listener != null) listener.onSpeechError("No speech detected.");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {}

                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        } else {
            Log.w(TAG, "Speech Recognition is not available on this device.");
        }
    }

    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Language not supported");
            } else {
                isTtsInitialized = true;
                textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {}

                    @Override
                    public void onDone(String utteranceId) {
                        if (UTTERANCE_ID.equals(utteranceId) && listener != null) {
                            listener.onTtsFinished();
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        if (listener != null) listener.onTtsFinished();
                    }
                });
            }
        } else {
            Log.e(TAG, "TTS Initialization failed!");
        }
    }

    public void startListening() {
        if (speechRecognizer == null) {
            if (listener != null) listener.onSpeechError("Speech recognition not supported on this device.");
            return;
        }

        stopSpeaking(); // Stop TTS if speaking before listening

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Zeno AI...");

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            if (listener != null) listener.onSpeechError("Error starting microphone: " + e.getLocalizedMessage());
        }
    }

    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            speechRecognizer.stopListening();
            isListening = false;
        }
    }

    public void speak(String text) {
        if (isTtsInitialized && textToSpeech != null) {
            stopListening();
            Bundle params = new Bundle();
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_ID);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID);
        }
    }

    public void stopSpeaking() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }

    private String getSpeechErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Microphone permission required";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error during speech recognition";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "Didn't catch that. Please speak again.";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Voice engine busy, retrying...";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input heard.";
            default:
                return "Speech recognition error (" + errorCode + ")";
        }
    }
}