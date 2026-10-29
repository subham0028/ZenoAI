package com.example.zenoai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity is the core screen of Zeno AI handling chat interaction, voice input, and navigation.
 */
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, VoiceAssistant.VoiceListener {

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 100;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView rvChat;
    private LinearLayout layoutWelcome, layoutLoading, layoutVoiceStatus;
    private TextView tvVoiceStatus;
    private ImageView ivVoiceStatusIcon;
    private MaterialButton btnStopVoice;
    private EditText etMessage;
    private FloatingActionButton btnSend;
    private ImageButton btnMic, btnVoiceChatMode;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private AIService aiService;
    private VoiceAssistant voiceAssistant;

    private boolean isVoiceModeActive = false;
    private boolean lastQueryWasVoice = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved night mode before layout inflation
        boolean isDark = ChatStorage.isDarkMode(this);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupToolbarAndDrawer();
        setupRecyclerView();
        setupListeners();

        aiService = new AIService();
        voiceAssistant = new VoiceAssistant(this, this);

        loadChatHistory();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        rvChat = findViewById(R.id.rvChat);
        layoutWelcome = findViewById(R.id.layoutWelcome);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutVoiceStatus = findViewById(R.id.layoutVoiceStatus);
        tvVoiceStatus = findViewById(R.id.tvVoiceStatus);
        ivVoiceStatusIcon = findViewById(R.id.ivVoiceStatusIcon);
        btnStopVoice = findViewById(R.id.btnStopVoice);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        btnVoiceChatMode = findViewById(R.id.btnVoiceChatMode);
    }

    private void setupToolbarAndDrawer() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, messageList, textToSpeak -> voiceAssistant.speak(textToSpeak));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(chatAdapter);
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text, false);
            }
        });

        btnMic.setOnClickListener(v -> checkPermissionAndStartMic(false));

        btnVoiceChatMode.setOnClickListener(v -> {
            if (!isVoiceModeActive) {
                startVoiceConversationMode();
            } else {
                stopVoiceConversationMode();
            }
        });

        btnStopVoice.setOnClickListener(v -> stopVoiceConversationMode());

        Chip chip1 = findViewById(R.id.chip1);
        Chip chip2 = findViewById(R.id.chip2);
        Chip chip3 = findViewById(R.id.chip3);

        View.OnClickListener chipListener = v -> {
            Chip chip = (Chip) v;
            sendMessage(chip.getText().toString(), false);
        };

        if (chip1 != null) chip1.setOnClickListener(chipListener);
        if (chip2 != null) chip2.setOnClickListener(chipListener);
        if (chip3 != null) chip3.setOnClickListener(chipListener);
    }

    private void loadChatHistory() {
        List<ChatMessage> savedList = ChatStorage.getMessages(this);
        if (savedList != null && !savedList.isEmpty()) {
            messageList.clear();
            messageList.addAll(savedList);
            chatAdapter.notifyDataSetChanged();
            updateWelcomeVisibility();
            rvChat.scrollToPosition(messageList.size() - 1);
        } else {
            updateWelcomeVisibility();
        }
    }

    private void updateWelcomeVisibility() {
        if (messageList.isEmpty()) {
            layoutWelcome.setVisibility(View.VISIBLE);
            rvChat.setVisibility(View.GONE);
        } else {
            layoutWelcome.setVisibility(View.GONE);
            rvChat.setVisibility(View.VISIBLE);
        }
    }

    private void sendMessage(String userText, boolean isVoice) {
        lastQueryWasVoice = isVoice;
        etMessage.setText("");

        ChatMessage userMsg = new ChatMessage(userText, ChatMessage.SENDER_USER, isVoice);
        messageList.add(userMsg);
        chatAdapter.notifyItemInserted(messageList.size() - 1);
        rvChat.smoothScrollToPosition(messageList.size() - 1);
        updateWelcomeVisibility();
        ChatStorage.saveMessages(this, messageList);

        setProcessingState(true);
        updateVoiceStatus(getString(R.string.status_thinking));

        aiService.sendMessage(this, messageList, userText, new AIService.AICallback() {
            @Override
            public void onSuccess(String responseText) {
                setProcessingState(false);

                ChatMessage aiMsg = new ChatMessage(responseText, ChatMessage.SENDER_AI, isVoice);
                messageList.add(aiMsg);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChat.smoothScrollToPosition(messageList.size() - 1);
                ChatStorage.saveMessages(MainActivity.this, messageList);

                if (isVoiceModeActive || lastQueryWasVoice) {
                    updateVoiceStatus(getString(R.string.status_speaking));
                    voiceAssistant.speak(responseText);
                } else {
                    hideVoiceStatus();
                }
            }

            @Override
            public void onError(String errorMessage) {
                setProcessingState(false);
                hideVoiceStatus();

                ChatMessage errorMsg = new ChatMessage("⚠️ " + errorMessage, ChatMessage.SENDER_AI, false);
                messageList.add(errorMsg);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChat.smoothScrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onNotConfigured() {
                setProcessingState(false);
                hideVoiceStatus();

                ChatMessage configMsg = new ChatMessage("⚙️ " + getString(R.string.pref_api_not_configured), ChatMessage.SENDER_AI, false);
                messageList.add(configMsg);
                chatAdapter.notifyItemInserted(messageList.size() - 1);
                rvChat.smoothScrollToPosition(messageList.size() - 1);

                showSetupPromptDialog();
            }
        });
    }

    private void setProcessingState(boolean isProcessing) {
        layoutLoading.setVisibility(isProcessing ? View.VISIBLE : View.GONE);
        btnSend.setEnabled(!isProcessing);
        btnMic.setEnabled(!isProcessing);
    }

    private void checkPermissionAndStartMic(boolean isContinuousVoiceMode) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_RECORD_AUDIO
            );
        } else {
            if (isContinuousVoiceMode) {
                isVoiceModeActive = true;
                layoutVoiceStatus.setVisibility(View.VISIBLE);
            }
            updateVoiceStatus(getString(R.string.status_listening));
            voiceAssistant.startListening();
        }
    }

    private void startVoiceConversationMode() {
        checkPermissionAndStartMic(true);
    }

    private void stopVoiceConversationMode() {
        isVoiceModeActive = false;
        voiceAssistant.stopListening();
        voiceAssistant.stopSpeaking();
        hideVoiceStatus();
        Toast.makeText(this, "Voice mode stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateVoiceStatus(String text) {
        if (isVoiceModeActive || lastQueryWasVoice) {
            layoutVoiceStatus.setVisibility(View.VISIBLE);
            tvVoiceStatus.setText(text);
        }
    }

    private void hideVoiceStatus() {
        if (!isVoiceModeActive) {
            layoutVoiceStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSpeechResult(String recognizedText) {
        if (!recognizedText.isEmpty()) {
            sendMessage(recognizedText, true);
        }
    }

    @Override
    public void onSpeechError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        if (isVoiceModeActive) {
            // Restart listening if in continuous voice mode
            updateVoiceStatus(getString(R.string.status_listening));
            voiceAssistant.startListening();
        } else {
            hideVoiceStatus();
        }
    }

    @Override
    public void onSpeechStart() {
        updateVoiceStatus(getString(R.string.status_listening));
    }

    @Override
    public void onSpeechEnd() {
        updateVoiceStatus(getString(R.string.status_thinking));
    }

    @Override
    public void onTtsFinished() {
        if (isVoiceModeActive) {
            runOnUiThread(() -> {
                updateVoiceStatus(getString(R.string.status_listening));
                voiceAssistant.startListening();
            });
        } else {
            runOnUiThread(this::hideVoiceStatus);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissionAndStartMic(isVoiceModeActive);
            } else {
                Toast.makeText(this, R.string.permission_mic_required, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_chat) {
            // Already on chat
        } else if (id == R.id.nav_voice_assistant) {
            startVoiceConversationMode();
        } else if (id == R.id.nav_clear_history) {
            showClearHistoryConfirmation();
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_app_info) {
            showAppInfoDialog();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showClearHistoryConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.pref_clear_confirm_title)
                .setMessage(R.string.pref_clear_confirm_message)
                .setPositiveButton(R.string.btn_clear, (dialog, which) -> {
                    messageList.clear();
                    chatAdapter.notifyDataSetChanged();
                    ChatStorage.clearMessages(this);
                    updateWelcomeVisibility();
                    Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void showSetupPromptDialog() {
        new AlertDialog.Builder(this)
                .setTitle("API Key Required")
                .setMessage("Zeno AI needs an API key to generate responses. Would you like to go to Settings to configure your API key?")
                .setPositiveButton("Settings", (dialog, which) -> startActivity(new Intent(this, SettingsActivity.class)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAppInfoDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_app_info, null);
        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChatHistory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceAssistant != null) {
            voiceAssistant.destroy();
        }
    }
}