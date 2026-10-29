package com.example.zenoai;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

/**
 * SettingsActivity allows users to configure themes, API keys, endpoints, and clear chat history.
 */
public class SettingsActivity extends AppCompatActivity {

    private MaterialSwitch switchDarkMode;
    private RadioGroup rgProvider;
    private RadioButton rbGemini, rbOpenAI, rbCustom;
    private TextInputEditText etApiKey, etBaseUrl;
    private Button btnSaveApi, btnClearHistory;
    private TextView tvApiStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchDarkMode = findViewById(R.id.switchDarkMode);
        rgProvider = findViewById(R.id.rgProvider);
        rbGemini = findViewById(R.id.rbGemini);
        rbOpenAI = findViewById(R.id.rbOpenAI);
        rbCustom = findViewById(R.id.rbCustom);
        etApiKey = findViewById(R.id.etApiKey);
        etBaseUrl = findViewById(R.id.etBaseUrl);
        btnSaveApi = findViewById(R.id.btnSaveApi);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        tvApiStatus = findViewById(R.id.tvApiStatus);

        loadCurrentSettings();

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ChatStorage.setDarkMode(this, isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        rgProvider.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbGemini) {
                etBaseUrl.setText(ApiConfig.DEFAULT_GEMINI_URL);
            } else if (checkedId == R.id.rbOpenAI) {
                etBaseUrl.setText(ApiConfig.DEFAULT_OPENAI_URL);
            }
        });

        btnSaveApi.setOnClickListener(v -> saveApiSettings());

        btnClearHistory.setOnClickListener(v -> showClearHistoryDialog());
    }

    private void loadCurrentSettings() {
        boolean isDark = ChatStorage.isDarkMode(this);
        switchDarkMode.setChecked(isDark);

        String provider = ChatStorage.getProvider(this);
        if (ApiConfig.PROVIDER_OPENAI.equalsIgnoreCase(provider)) {
            rbOpenAI.setChecked(true);
        } else if (ApiConfig.PROVIDER_CUSTOM.equalsIgnoreCase(provider)) {
            rbCustom.setChecked(true);
        } else {
            rbGemini.setChecked(true);
        }

        etApiKey.setText(ChatStorage.getApiKey(this));
        etBaseUrl.setText(ChatStorage.getBaseUrl(this));

        updateApiStatusText();
    }

    private void updateApiStatusText() {
        String apiKey = ChatStorage.getApiKey(this);
        if (!apiKey.isEmpty()) {
            tvApiStatus.setText(R.string.pref_api_configured);
            tvApiStatus.setTextColor(getColor(R.color.accent_teal));
        } else {
            tvApiStatus.setText(R.string.pref_api_not_configured);
            tvApiStatus.setTextColor(getColor(R.color.voice_active_red));
        }
    }

    private void saveApiSettings() {
        String apiKey = etApiKey.getText() != null ? etApiKey.getText().toString().trim() : "";
        String baseUrl = etBaseUrl.getText() != null ? etBaseUrl.getText().toString().trim() : "";

        String selectedProvider = ApiConfig.PROVIDER_GEMINI;
        int checkedId = rgProvider.getCheckedRadioButtonId();
        if (checkedId == R.id.rbOpenAI) {
            selectedProvider = ApiConfig.PROVIDER_OPENAI;
        } else if (checkedId == R.id.rbCustom) {
            selectedProvider = ApiConfig.PROVIDER_CUSTOM;
        }

        if (baseUrl.isEmpty()) {
            baseUrl = ApiConfig.DEFAULT_GEMINI_URL;
        }

        ChatStorage.saveApiKey(this, apiKey);
        ChatStorage.saveBaseUrl(this, baseUrl);
        ChatStorage.saveProvider(this, selectedProvider);

        updateApiStatusText();
        Toast.makeText(this, "API Settings saved successfully!", Toast.LENGTH_SHORT).show();
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.pref_clear_confirm_title)
                .setMessage(R.string.pref_clear_confirm_message)
                .setPositiveButton(R.string.btn_clear, (dialog, which) -> {
                    ChatStorage.clearMessages(this);
                    Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }
}