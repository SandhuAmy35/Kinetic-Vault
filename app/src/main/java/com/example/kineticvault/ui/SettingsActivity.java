package com.example.kineticvault.ui;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import com.example.kineticvault.R;
import com.example.kineticvault.data.VaultPreferences;
import com.google.android.material.snackbar.Snackbar;

public final class SettingsActivity extends InfoActivity {
    private Spinner sensitivity;
    private EditText url, key;
    private int connectionRequest;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        // The server credential is visible here even when screenshot protection is off elsewhere.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        showPage(R.layout.activity_settings);
        sensitivity = findViewById(R.id.settings_sensitivity);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sensitivity_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sensitivity.setAdapter(adapter);
        url = findViewById(R.id.settings_url); key = findViewById(R.id.settings_key);
        findViewById(R.id.settings_save).setEnabled(false);
        VaultPreferences.load(this, () -> { if (!isDestroyed()) populate(); });
        findViewById(R.id.settings_save).setOnClickListener(v -> save());
        findViewById(R.id.settings_test).setOnClickListener(v -> testConnection());
    }
    private CheckBox toggle(int id) { return findViewById(id); }
    private void populate() {
        VaultPreferences.Values p = VaultPreferences.current();
        sensitivity.setSelection(p.sensitivity);
        toggle(R.id.settings_pad).setChecked(p.pad);
        toggle(R.id.settings_protection).setChecked(p.protection);
        toggle(R.id.settings_autolock).setChecked(p.autoLock);
        url.setText(p.url); key.setText(p.apiKey);
        findViewById(R.id.settings_save).setEnabled(true);
        findViewById(R.id.settings_test).setEnabled(true);
    }
    private void testConnection() {
        int request = ++connectionRequest;
        findViewById(R.id.settings_test).setEnabled(false);
        Snackbar.make(findViewById(R.id.info_root), R.string.cloud_connecting, Snackbar.LENGTH_SHORT).show();
        com.example.kineticvault.data.SyncManager.get(this).testConnection(url.getText().toString().trim(),
                key.getText().toString(), ok -> {
                    if (isDestroyed() || request != connectionRequest) return;
                    findViewById(R.id.settings_test).setEnabled(true);
                    findViewById(R.id.settings_test).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    Snackbar.make(findViewById(R.id.info_root), ok ? R.string.cloud_connection_ok
                            : R.string.cloud_connection_error, Snackbar.LENGTH_LONG).show();
                });
    }
    @Override protected void onStop() {
        ++connectionRequest; findViewById(R.id.settings_test).setEnabled(true); super.onStop();
    }
    private void save() {
        String server = url.getText().toString().trim();
        android.net.Uri parsed = android.net.Uri.parse(server);
        if (!server.isEmpty() && (!("https".equals(parsed.getScheme()) || "http".equals(parsed.getScheme()))
                || parsed.getHost() == null || parsed.getUserInfo() != null)) {
            url.setError(getString(R.string.settings_url_error)); return;
        }
        VaultPreferences.Values values = new VaultPreferences.Values(sensitivity.getSelectedItemPosition(),
                toggle(R.id.settings_pad).isChecked(), toggle(R.id.settings_protection).isChecked(),
                toggle(R.id.settings_autolock).isChecked(), server, key.getText().toString());
        findViewById(R.id.settings_save).setEnabled(false);
        VaultPreferences.save(this, values, saved -> {
            if (isDestroyed()) return;
            findViewById(R.id.settings_save).setEnabled(true);
            if (saved) {
                findViewById(R.id.settings_save).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                com.example.kineticvault.data.SyncManager.get(this).configurationChanged();
            }
            Snackbar.make(findViewById(R.id.info_root), saved ? R.string.settings_saved : R.string.settings_save_error,
                    Snackbar.LENGTH_LONG).show();
        });
    }
}
