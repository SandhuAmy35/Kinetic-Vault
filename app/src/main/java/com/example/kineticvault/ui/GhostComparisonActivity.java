package com.example.kineticvault.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GhostComparisonActivity extends AppCompatActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private GhostComparisonWork.Pair pair;
    private DiffMapView standard, ghost;
    private volatile boolean closed;
    private boolean busy;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ghost_comparison);
        View root = findViewById(R.id.diff_root);
        int padding = getResources().getDimensionPixelSize(R.dimen.space_md);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(safe.left + padding, safe.top, safe.right + padding, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        standard = findViewById(R.id.diff_standard); ghost = findViewById(R.id.diff_ghost);
        standard.bindLoupe(findViewById(R.id.diff_standard_pixel));
        ghost.bindLoupe(findViewById(R.id.diff_ghost_pixel));
        findViewById(R.id.diff_back).setOnClickListener(v -> finish());
        findViewById(R.id.diff_retry).setOnClickListener(v -> generate());
        findViewById(R.id.diff_replay).setOnClickListener(v -> { standard.startSweep(); ghost.startSweep(); });
        generate();
    }

    private void generate() {
        if (busy || closed) return;
        busy = true; setBusy(true);
        ((TextView) findViewById(R.id.diff_status)).setText(R.string.diff_loading);
        String sample = getString(R.string.diff_demo_payload);
        worker.execute(() -> {
            try {
                GhostComparisonWork.Pair result = GhostComparisonWork.create(sample);
                main.post(() -> {
                    if (closed) { dispose(result); return; }
                    show(result);
                });
            } catch (Exception | OutOfMemoryError failure) {
                main.post(() -> {
                    if (closed) return;
                    busy = false; setBusy(false);
                    ((TextView) findViewById(R.id.diff_status)).setText(R.string.diff_error);
                });
            }
        });
    }

    private void show(GhostComparisonWork.Pair value) {
        GhostComparisonWork.Pair previous = pair;
        pair = value; standard.setData(value.standard); ghost.setData(value.ghost);
        DiffStats.show(findViewById(R.id.diff_standard_stats), value.standard.stats);
        DiffStats.show(findViewById(R.id.diff_ghost_stats), value.ghost.stats);
        ((TextView) findViewById(R.id.diff_standard_pixel)).setText(R.string.diff_touch);
        ((TextView) findViewById(R.id.diff_ghost_pixel)).setText(R.string.diff_touch);
        ((TextView) findViewById(R.id.diff_status)).setText(R.string.diff_ready);
        busy = false; setBusy(false);
        if (getLifecycle().getCurrentState().isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            standard.startSweep(); ghost.startSweep();
            standard.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        }
        dispose(previous);
    }

    private void setBusy(boolean value) {
        findViewById(R.id.diff_progress).setVisibility(value ? View.VISIBLE : View.GONE);
        findViewById(R.id.diff_retry).setEnabled(!value);
        findViewById(R.id.diff_replay).setEnabled(!value && pair != null);
    }

    private void dispose(GhostComparisonWork.Pair value) {
        if (value == null) return;
        // A late callback can arrive after shutdown; retain worker-only array cleanup.
        if (!worker.isShutdown()) worker.execute(value::close);
        else {
            ExecutorService cleanup = Executors.newSingleThreadExecutor();
            cleanup.execute(value::close); cleanup.shutdown();
        }
    }

    @Override protected void onPause() {
        standard.stopSweep(); ghost.stopSweep(); super.onPause();
    }

    @Override protected void onDestroy() {
        closed = true; standard.setData(null); ghost.setData(null);
        dispose(pair); pair = null; worker.shutdown();
        super.onDestroy();
    }
}
