package com.example.kineticvault.ui.hide;

import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.ViewFlipper;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;
import com.example.kineticvault.ui.MatrixRainView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

public final class HideActivity extends AppCompatActivity {
    private static final int[] TITLES = {R.string.hide_step_cover, R.string.hide_step_secret,
            R.string.hide_step_gesture, R.string.hide_step_forge, R.string.hide_step_result};
    private HideWork work;
    private SecretStep secret;
    private GestureStep gesture;
    private ResultStep result;
    private MatrixRainView rain;
    private int step;
    private boolean resumed;

    @Override protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        if (com.example.kineticvault.data.VaultPreferences.current().protection)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_hide);
        installInsets();
        work = new HideWork(getApplicationContext());
        new CoverStep(this, work);
        secret = new SecretStep(this, work);
        gesture = new GestureStep(this, secret);
        result = new ResultStep(this, work);
        rain = findViewById(R.id.hide_matrix);
        findViewById(R.id.hide_back).setOnClickListener(v -> back());
        findViewById(R.id.hide_cancel_forge).setOnClickListener(v -> back());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { back(); }
        });
        showStep(0);
        if (savedState != null) message(R.string.hide_restarted);
    }

    private void installInsets() {
        View root = findViewById(R.id.hide_root);
        int spacing = getResources().getDimensionPixelSize(R.dimen.space_md);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left + spacing, safe.top, safe.right + spacing, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    void showStep(int next) {
        secret.deactivate();
        if (step == 2 || next == 2) gesture.reset();
        step = next;
        findViewById(R.id.hide_notice).setVisibility(View.GONE);
        ((ViewFlipper) findViewById(R.id.hide_steps)).setDisplayedChild(next);
        ((TextView) findViewById(R.id.hide_step_title)).setText(
                getString(R.string.hide_step_title, next + 1, getString(TITLES[next])));
        ((LinearProgressIndicator) findViewById(R.id.hide_step_progress)).setProgressCompat(next * 25, false);
        rain.setRunning(resumed && next == 3);
        if (next != 1) hideKeyboard();
        if (next == 1 && resumed) secret.activate();
        findViewById(R.id.hide_steps).startAnimation(android.view.animation.AnimationUtils.loadAnimation(this, R.anim.fade_in));
    }

    void forge() {
        if (step != 2) return;
        if (!secret.validPassphrase()) {
            work.clearSensitive(); showStep(1); return;
        }
        byte[] tokens = gesture.takeTokens();
        if (tokens == null) return;
        char[] passphrase = secret.takePassphrase();
        showStep(3);
        secret.clearSensitiveUi();
        progress(R.string.hide_stage_pack, 0, 0);
        work.forge(tokens, passphrase, this::progress, new HideWork.Callback<HideForge.Result>() {
            public void success(HideForge.Result value) {
                if (step != 3) return;
                result.show(value); showStep(4);
                findViewById(R.id.hide_steps).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            }
            public void failure(int message) {
                if (step != 3) return;
                showStep(1); HideActivity.this.message(message);
            }
        });
    }

    private void progress(int label, int done, int total) {
        if (step != 3) return;
        rain.setProgress(done, total);
        TextView text = findViewById(R.id.hide_forge_status);
        text.setText(total > 0 ? getString(R.string.hide_stage_percent, getString(label),
                (int) (done * 100L / total)) : getString(label));
        LinearProgressIndicator bar = findViewById(R.id.hide_forge_progress);
        boolean indeterminate = total <= 0;
        if (bar.isIndeterminate() != indeterminate) {
            bar.setVisibility(View.INVISIBLE);
            bar.setIndeterminate(indeterminate);
            bar.setVisibility(View.VISIBLE);
        }
        if (!indeterminate) bar.setProgressCompat((int) (done * 1000L / total), false);
    }

    void message(int resource) {
        if (isFinishing() || isDestroyed()) return;
        TextView notice = findViewById(R.id.hide_notice);
        notice.setText(resource); notice.setVisibility(View.VISIBLE);
        Snackbar.make(findViewById(R.id.hide_root), resource, Snackbar.LENGTH_LONG).show();
    }

    private void back() {
        if (step == 0 || step == 4) { finish(); return; }
        if (step == 3) {
            new MaterialAlertDialogBuilder(this).setTitle(R.string.hide_cancel_forge)
                    .setMessage(R.string.hide_cancel_forge_message)
                    .setNegativeButton(R.string.hide_keep_forging, null)
                    .setPositiveButton(R.string.hide_cancel, (dialog, which) -> {
                        work.clearSensitive(); showStep(1); message(R.string.hide_forge_cancelled);
                    }).show();
            return;
        }
        work.clearSensitive();
        showStep(step - 1);
    }

    private void hideKeyboard() {
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(findViewById(R.id.hide_root).getWindowToken(), 0);
        View focused = getCurrentFocus();
        if (focused != null) focused.clearFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (com.example.kineticvault.data.VaultPreferences.current().protection)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        if (secret != null && step == 1) secret.activate();
    }

    @Override protected void onPause() {
        resumed = false;
        if (result != null) result.pause();
        if (gesture != null) gesture.reset();
        if (rain != null) rain.setRunning(false);
        if (secret != null) secret.deactivate();
        if (work != null) work.clearSensitive();
        if (step == 2 || step == 3) {
            showStep(1);
            secret.clearSensitiveUi();
            message(R.string.hide_session_paused);
        }
        super.onPause();
    }

    @Override protected void onStop() {
        if (secret != null) secret.clearSensitiveUi();
        super.onStop();
    }

    @Override protected void onDestroy() {
        if (secret != null) { secret.deactivate(); secret.close(); }
        if (result != null) result.close();
        if (work != null) work.close();
        super.onDestroy();
    }
}
