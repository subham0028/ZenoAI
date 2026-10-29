package com.example.zenoai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying user and AI messages in RecyclerView.
 */
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnSpeakClickListener {
        void onSpeakClick(String text);
    }

    private final Context context;
    private final List<ChatMessage> messageList;
    private final OnSpeakClickListener speakListener;

    public ChatAdapter(Context context, List<ChatMessage> messageList, OnSpeakClickListener speakListener) {
        this.context = context;
        this.messageList = messageList;
        this.speakListener = speakListener;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getSender();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ChatMessage.SENDER_USER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_message, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ai_message, parent, false);
            return new AIViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder instanceof UserViewHolder) {
            UserViewHolder userHolder = (UserViewHolder) holder;
            userHolder.tvMessage.setText(message.getText());
            userHolder.tvTime.setText(message.getFormattedTime());
        } else if (holder instanceof AIViewHolder) {
            AIViewHolder aiHolder = (AIViewHolder) holder;
            aiHolder.tvMessage.setText(message.getText());
            aiHolder.tvTime.setText(message.getFormattedTime());

            aiHolder.btnCopy.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Zeno AI Response", message.getText());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                }
            });

            aiHolder.btnSpeak.setOnClickListener(v -> {
                if (speakListener != null) {
                    speakListener.onSpeakClick(message.getText());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }

    static class AIViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageButton btnCopy, btnSpeak;

        AIViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            btnCopy = itemView.findViewById(R.id.btnCopy);
            btnSpeak = itemView.findViewById(R.id.btnSpeak);
        }
    }
}