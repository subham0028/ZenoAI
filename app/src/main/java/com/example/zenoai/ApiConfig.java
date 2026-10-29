package com.example.zenoai;

/**
 * Constants and helpers for AI API configuration.
 */
public class ApiConfig {
    public static final String PROVIDER_GEMINI = "gemini";
    public static final String PROVIDER_OPENAI = "openai";
    public static final String PROVIDER_CUSTOM = "custom";

    public static final String DEFAULT_GEMINI_URL = "https://generativelanguage.googleapis.com/";
    public static final String DEFAULT_OPENAI_URL = "https://api.openai.com/";

    public static final String SYSTEM_INSTRUCTION = 
            "You are Zeno AI, a helpful, polite, intelligent AI assistant. " +
            "You fluently support English, Hindi, and Hinglish. " +
            "Provide clear, concise, and friendly responses suitable for an AI chat assistant.";
}