package com.example.zenoai;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * Data model for a single chat message in Zeno AI.
 */
public class ChatMessage {
    public static final int SENDER_USER = 1;
    public static final int SENDER_AI = 2;

    private String id;
    private String text;
    private int sender;
    private long timestamp;
    private boolean isVoice;

    public ChatMessage(String text, int sender, boolean isVoice) {
        this.id = UUID.randomUUID().toString();
        this.text = text;
        this.sender = sender;
        this.timestamp = System.currentTimeMillis();
        this.isVoice = isVoice;
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getSender() {
        return sender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isVoice() {
        return isVoice;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}