package com.example.kineticvault.diagnostics;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class TestHarnessActivity extends AppCompatActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Future<?> task;
    private TextView status;
    private TextView summary;
    private TextView standard;
    private TextView ghost;
    private ImageView preview;
    private MaterialButton run;
    private MaterialSwitch light;
    private View progress;
    private boolean busy;
    private int currentStage = R.string.stage_generate;
    private int currentIndex;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test_harness);
        bindViews();
        installInsets();
        light.setChecked((getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES);
        light.setOnCheckedChangeListener((button, checked) -> AppCompatDelegate.setDefaultNightMode(
                checked ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES));
        run.setOnClickListener(view -> startTest());
    }

    private void bindViews() {
        status = findViewById(R.id.status);
        summary = findViewById(R.id.summary);
        standard = findViewById(R.id.standard_result);
        ghost = findViewById(R.id.ghost_result);
        preview = findViewById(R.id.cover_preview);
        run = findViewById(R.id.run_test);
        light = findViewById(R.id.light_preview);
        progress = findViewById(R.id.test_progress);
    }

    private void installInsets() {
        View root = findViewById(R.id.harness_root);
        int padding = getResources().getDimensionPixelSize(R.dimen.space_lg);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left + padding, safe.top, safe.right + padding, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void startTest() {
        if (busy) { return; }
        setBusy(true);
        run.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        status.setText(R.string.harness_running);
        status.setTextColor(getColor(R.color.kv_primary));
        standard.setText(R.string.harness_pending);
        ghost.setText(R.string.harness_pending);
        currentStage = R.string.stage_generate;
        currentIndex = 0;
        summary.setText(currentStage);
        task = worker.submit(this::executeTest);
    }

    private void executeTest() {
        try {
            HarnessReport report = HarnessRunner.run(getApplicationContext(), (index, resource) ->
                    main.post(() -> showStage(index, resource)));
            main.post(() -> showReport(report));
        } catch (CancellationException ignored) {
            // Leaving this temporary screen cancels work at the next safe boundary.
        } catch (OutOfMemoryError failure) {
            fail(R.string.harness_memory_error);
        } catch (IOException failure) {
            fail(R.string.harness_io_error);
        } catch (GeneralSecurityException failure) {
            fail(R.string.harness_crypto_error);
        } catch (RuntimeException failure) {
            fail(R.string.harness_unexpected_error);
        }
    }

    private void showStage(int index, int resource) {
        if (isDestroyed() || isFinishing()) { return; }
        currentStage = resource;
        currentIndex = index;
        summary.setText(getString(R.string.harness_stage, index + 1, getString(resource)));
    }

    private void showReport(HarnessReport report) {
        if (isDestroyed() || isFinishing()) { report.close(); return; }
        setBusy(false);
        status.setText(R.string.harness_pass);
        status.setTextColor(getColor(R.color.kv_success));
        summary.setText(getString(R.string.harness_success, report.totalMs, report.generationMs));
        standard.setText(modeText(report, false));
        ghost.setText(modeText(report, true));
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setImageBitmap(report.preview);
        preview.setContentDescription(getString(R.string.harness_preview_description));
        // ImageView owns the tiny preview now; normal GC releases it after detaching.
        report.preview = null;
    }

    private String modeText(HarnessReport report, boolean ghostMode) {
        StringBuilder text = new StringBuilder();
        for (HarnessReport.Entry entry : report.entries) {
            if (entry.ghost != ghostMode) { continue; }
            if (text.length() > 0) { text.append("\n\n"); }
            text.append(getString(R.string.harness_case,
                    getString(entry.file ? R.string.harness_file : R.string.harness_text),
                    entry.totalMs, entry.forgeMs, entry.saveMs, entry.reloadMs, entry.unlockMs,
                    entry.stats.psnrDb, entry.stats.changedPercent, entry.sha256.substring(0, 12)));
        }
        return text.toString();
    }

    private void fail(int message) {
        main.post(() -> {
            if (isDestroyed() || isFinishing()) { return; }
            setBusy(false);
            status.setText(R.string.harness_fail);
            status.setTextColor(getColor(R.color.kv_danger));
            summary.setText(getString(R.string.harness_failure_stage,
                    getString(message), getString(R.string.harness_stage,
                            currentIndex + 1, getString(currentStage))));
        });
    }

    private void setBusy(boolean value) {
        busy = value;
        run.setEnabled(!value);
        light.setEnabled(!value);
        run.setText(value ? R.string.harness_running : R.string.harness_run_again);
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
    }

    @Override protected void onDestroy() {
        if (task != null) { task.cancel(true); }
        worker.shutdownNow();
        if (preview != null) { preview.setImageDrawable(null); }
        super.onDestroy();
    }
}
