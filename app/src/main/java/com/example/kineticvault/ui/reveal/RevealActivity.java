package com.example.kineticvault.ui.reveal;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;
import com.example.kineticvault.core.VaultHeader;
import com.example.kineticvault.core.WorkProgress;
import com.example.kineticvault.ui.TextScrambler;
import com.example.kineticvault.ui.VaultDoorView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class RevealActivity extends AppCompatActivity {
    private RevealWork work;
    private com.google.android.material.bottomsheet.BottomSheetDialog sheet;
    private RevealGesture gesture;
    private VaultDoorView door;
    private TextScrambler scrambler;
    private EditText password;
    private VaultHeader header;
    private Uri selected;
    private boolean busy, registered, revealed, external;
    private int attempts;
    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                if (com.example.kineticvault.data.VaultPreferences.current().autoLock) {
                    lock(); work.discardExport(); finish();
                }
            }
        }
    };
    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) load(uri); });
    private final ActivityResultLauncher<String> saver = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"), uri -> {
                external = false;
                if (uri == null) { cancelledExport(); return; }
                work.save(uri, new RevealWork.Callback<Boolean>() {
                    public void success(Boolean logged) {
                        message(logged ? R.string.reveal_saved : R.string.vault_saved_event_error);
                    }
                    public void failure(int message) { RevealActivity.this.message(message); }
                });
            });
    private final ActivityResultLauncher<String> textSaver = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/plain"), uri -> {
                external = false;
                if (uri == null) { cancelledExport(); return; }
                work.save(uri, new RevealWork.Callback<Boolean>() {
                    public void success(Boolean logged) { message(logged ? R.string.reveal_saved : R.string.vault_saved_event_error); }
                    public void failure(int message) { RevealActivity.this.message(message); }
                });
            });
    private final ActivityResultLauncher<Intent> viewer = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                external = false; work.discardExport();
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this); setContentView(R.layout.activity_reveal);
        findViewById(R.id.reveal_root).setSaveFromParentEnabled(false);
        work = new RevealWork(getApplicationContext());
        password = findViewById(R.id.reveal_passphrase);
        door = findViewById(R.id.reveal_door);
        scrambler = new TextScrambler(findViewById(R.id.reveal_text));
        gesture = new RevealGesture(this);
        installInsets(); bind();
        Uri uri = state != null && state.getString("source") != null
                ? Uri.parse(state.getString("source")) : getIntent().getData();
        if (uri == null) uri = IntentCompat.getParcelableExtra(getIntent(), Intent.EXTRA_STREAM, Uri.class);
        if (uri == null && getIntent().getClipData() != null && getIntent().getClipData().getItemCount() > 0)
            uri = getIntent().getClipData().getItemAt(0).getUri();
        if (uri != null) load(uri);
    }

    private void bind() {
        findViewById(R.id.reveal_pick).setOnClickListener(v -> {
            lock();
            try { picker.launch(new String[]{"image/*"}); }
            catch (RuntimeException error) { message(R.string.reveal_picker_error); }
        });
        findViewById(R.id.reveal_back).setOnClickListener(v -> finish());
        findViewById(R.id.reveal_unlock).setOnClickListener(v -> unlock());
        findViewById(R.id.reveal_save).setOnClickListener(v -> exportOptions());
        findViewById(R.id.reveal_open).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.reveal_open).setMessage(R.string.reveal_open_warning)
                .setNegativeButton(R.string.hide_cancel, null)
                .setPositiveButton(R.string.reveal_open, (dialog, which) -> export(true)).show());
    }

    private void installInsets() {
        View root = findViewById(R.id.reveal_root);
        int spacing = getResources().getDimensionPixelSize(R.dimen.space_md);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left + spacing, safe.top, safe.right + spacing, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void load(Uri uri) {
        selected = uri;
        lock(); header = null; attempts = 0;
        ((TextView) findViewById(R.id.reveal_attempts)).setText(getString(R.string.reveal_attempts, attempts));
        setBusy(true); message(R.string.reveal_reading);
        work.load(uri, new RevealWork.Callback<VaultHeader>() {
            public void success(VaultHeader value) {
                header = value; setBusy(false);
                findViewById(R.id.reveal_credentials).setVisibility(View.VISIBLE);
                password.setVisibility(value.usesPassphrase() ? View.VISIBLE : View.GONE);
                message(R.string.reveal_gesture_prompt);
            }
            public void failure(int message) { setBusy(false); RevealActivity.this.message(message); }
        });
    }

    private void unlock() {
        if (busy || header == null) return;
        if (header.usesPassphrase() && password.length() == 0) {
            message(R.string.reveal_password_required); return;
        }
        byte[] tokens = gesture.takeTokens();
        if (tokens == null) return;
        char[] passphrase = new char[header.usesPassphrase() ? password.length() : 0];
        password.getText().getChars(0, passphrase.length, passphrase, 0);
        password.getText().clear(); gesture.clear();
        setBusy(true); message(R.string.reveal_unlocking);
        work.unlock(tokens, passphrase, this::progress, new RevealWork.Callback<Boolean>() {
            public void success(Boolean value) { unlocked(); if (!value) message(R.string.vault_event_error); }
            public void failure(int message) { denied(message); }
        });
    }

    private void unlocked() {
        revealed = true;
        setBusy(false);
        findViewById(R.id.reveal_credentials).setVisibility(View.GONE);
        message(R.string.reveal_success);
        door.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        door.open(() -> {
            findViewById(R.id.reveal_files).setVisibility(View.VISIBLE);
            findViewById(R.id.reveal_open).setVisibility(work.hasFile() ? View.VISIBLE : View.GONE);
            ((TextView) findViewById(R.id.reveal_save)).setText(R.string.export_title);
            if (!work.hasFile()) {
                char[] text = work.takeText();
                if (text != null) {
                    findViewById(R.id.reveal_text).setVisibility(View.VISIBLE);
                    scrambler.show(text);
                }
            }
        });
    }

    private void denied(int message) {
        setBusy(false); attempts++;
        ((TextView) findViewById(R.id.reveal_attempts)).setText(getString(R.string.reveal_attempts, attempts));
        gesture.clear(); message(message);
        door.deny(); door.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
        door.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    private void progress(WorkProgress.Stage stage, int done, int total) {
        int label = stage == WorkProgress.Stage.KEY ? R.string.hide_stage_key
                : stage == WorkProgress.Stage.EXTRACT ? R.string.reveal_extract
                : stage == WorkProgress.Stage.UNPACK ? R.string.reveal_unpack : R.string.reveal_decrypt;
        ProgressBar bar = findViewById(R.id.reveal_progress);
        bar.setIndeterminate(total <= 0);
        if (total > 0) bar.setProgress((int) (done * 100L / total));
        ((TextView) findViewById(R.id.reveal_notice)).setText(total > 0
                ? getString(R.string.hide_stage_percent, getString(label), (int) (done * 100L / total))
                : getString(label));
    }

    private void exportOptions() {
        if (busy || !work.hasPayload()) return;
        if (sheet != null) sheet.dismiss();
        int[] labels = work.hasFile() ? new int[]{R.string.reveal_save, R.string.reveal_open}
                : new int[]{R.string.txt_export};
        Runnable[] actions = work.hasFile() ? new Runnable[]{() -> export(false),
                () -> findViewById(R.id.reveal_open).performClick()} : new Runnable[]{() -> export(false)};
        sheet = com.example.kineticvault.ui.ExportSheet.show(this, R.string.reveal_file_note, labels, actions);
    }
    private void cancelledExport() {
        work.discardExport();
        message(R.string.audit_export_cancelled);
    }
    private void export(boolean open) {
        if (busy || !work.hasPayload()) return;
        setBusy(true); message(R.string.reveal_exporting);
        work.prepareExport(new RevealWork.Callback<RevealExport>() {
            public void success(RevealExport value) {
                setBusy(false);
                try {
                    external = true;
                    if (!open) {
                        if ("text/plain".equals(value.mime)) textSaver.launch(value.name);
                        else saver.launch(value.name);
                    }
                    else {
                        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(value.uri, value.mime)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setClipData(ClipData.newRawUri(getString(R.string.reveal_file), value.uri));
                        viewer.launch(intent);
                        work.opened(value, new RevealWork.Callback<Boolean>() {
                            public void success(Boolean ignored) { }
                            public void failure(int message) { RevealActivity.this.message(message); }
                        });
                    }
                } catch (RuntimeException error) {
                    external = false;
                    work.discardExport(); message(R.string.reveal_open_error);
                }
            }
            public void failure(int message) { setBusy(false); RevealActivity.this.message(message); }
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        findViewById(R.id.reveal_progress).setVisibility(value ? View.VISIBLE : View.GONE);
        ((ProgressBar) findViewById(R.id.reveal_progress)).setIndeterminate(true);
        findViewById(R.id.reveal_pick).setEnabled(!value);
        findViewById(R.id.reveal_credentials).setVisibility(!value && !revealed && header != null ? View.VISIBLE : View.GONE);
        findViewById(R.id.reveal_save).setEnabled(!value);
        findViewById(R.id.reveal_open).setEnabled(!value);
    }

    void message(int resource) {
        if (isFinishing()) return;
        TextView notice = findViewById(R.id.reveal_notice);
        notice.setText(resource); com.example.kineticvault.ui.UiPolish.notice(notice);
    }

    private void lock() {
        if (revealed || busy) work.autoLock();
        revealed = false;
        gesture.clear(); password.getText().clear(); scrambler.clear(); door.reset();
        work.clearSensitive(); setBusy(false);
        if (!external) work.discardExport();
        findViewById(R.id.reveal_text).setVisibility(View.GONE);
        findViewById(R.id.reveal_files).setVisibility(View.GONE);
        message(R.string.reveal_locked);
    }

    @Override protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED);
        registered = true;
        work.protection(enabled -> {
            if (enabled || com.example.kineticvault.data.VaultPreferences.current().nerd) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        });
    }

    @Override protected void onPause() {
        if (sheet != null) { sheet.dismiss(); sheet = null; }
        gesture.clear(); password.getText().clear();
        if (busy) lock();
        super.onPause();
    }

    @Override protected void onStop() {
        if (com.example.kineticvault.data.VaultPreferences.current().autoLock) lock();
        android.os.PowerManager power = getSystemService(android.os.PowerManager.class);
        if (com.example.kineticvault.data.VaultPreferences.current().autoLock && power != null && !power.isInteractive()) { work.discardExport(); finish(); }
        if (registered) { unregisterReceiver(screenOff); registered = false; }
        super.onStop();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (selected != null) state.putString("source", selected.toString());
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        gesture.clear(); password.getText().clear(); scrambler.clear(); door.reset();
        work.close(); super.onDestroy();
    }
}
