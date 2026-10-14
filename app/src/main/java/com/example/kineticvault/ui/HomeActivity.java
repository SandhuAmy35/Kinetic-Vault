package com.example.kineticvault.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;
import com.example.kineticvault.diagnostics.TestHarnessActivity;
import com.example.kineticvault.ui.hide.HideActivity;
import com.example.kineticvault.ui.reveal.RevealActivity;
import com.example.kineticvault.ui.vault.VaultActivity;
import com.google.android.material.snackbar.Snackbar;

public final class HomeActivity extends AppCompatActivity {
    private android.animation.ValueAnimator pulse;
    private final com.example.kineticvault.data.SyncManager.Listener ledger = this::showLedger;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        View root = findViewById(R.id.home_root);
        int spacing = getResources().getDimensionPixelSize(R.dimen.space_lg);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(spacing + safe.left, safe.top, spacing + safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        findViewById(R.id.home_hide).setOnClickListener(v -> startActivity(new Intent(this, HideActivity.class)));
        findViewById(R.id.home_lab).setOnClickListener(v -> startActivity(new Intent(this, TestHarnessActivity.class)));
        findViewById(R.id.home_reveal).setOnClickListener(v -> startActivity(new Intent(this, RevealActivity.class)));
        findViewById(R.id.home_history).setOnClickListener(v -> startActivity(new Intent(this, VaultActivity.class)));
        findViewById(R.id.home_settings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.home_more).setOnClickListener(this::overflow);
        findViewById(R.id.cloud_retry).setOnClickListener(v ->
                com.example.kineticvault.data.SyncManager.get(this).configurationChanged());
        findViewById(R.id.home_scan).setOnClickListener(v -> startActivity(new Intent(this, ScannerActivity.class)));
        findViewById(R.id.home_learn).setOnClickListener(v -> startActivity(new Intent(this, LearnActivity.class)));
        int[] tiles = {R.id.home_hide, R.id.home_reveal, R.id.home_lab, R.id.home_scan, R.id.home_history, R.id.home_settings, R.id.home_learn};
        for (int i = 0; i < tiles.length; i++) {
            View tile = findViewById(tiles[i]); tile.setAlpha(0); tile.setTranslationY(32);
            tile.animate().alpha(1).translationY(0).setStartDelay(i * 75L).setDuration(350).start();
        }
    }
    private void overflow(View anchor) {
        androidx.appcompat.widget.PopupMenu menu = new androidx.appcompat.widget.PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, R.string.settings_title);
        menu.getMenu().add(0, 2, 1, R.string.learn_title);
        menu.getMenu().add(0, 3, 2, R.string.about_title);
        menu.setOnMenuItemClickListener(item -> {
            Class<?> screen = item.getItemId() == 1 ? SettingsActivity.class
                    : item.getItemId() == 2 ? LearnActivity.class : AboutActivity.class;
            startActivity(new Intent(this, screen)); return true;
        }); menu.show();
    }
    private void showLedger(com.example.kineticvault.data.SyncManager.Status status) {
        android.widget.TextView chip = findViewById(R.id.cloud_status);
        chip.setText(status.offline ? R.string.cloud_offline : status.pending > 0
                ? R.string.cloud_pending : R.string.cloud_synced);
        android.widget.TextView counts = findViewById(R.id.cloud_stats);
        counts.setText(status.statsAvailable ? getString(status.offline || status.pending > 0
                ? R.string.cloud_stats_cached : R.string.cloud_stats_live, status.vaults, status.events, status.bytes)
                : getString(R.string.cloud_stats_unavailable));
        android.widget.TextView queue = findViewById(R.id.cloud_queue);
        queue.setText(getString(R.string.cloud_queue, status.pending));
        findViewById(R.id.cloud_airplane).setVisibility(status.airplane ? View.VISIBLE : View.GONE);
        findViewById(R.id.cloud_retry).setEnabled(!status.airplane);
    }
    @Override protected void onStart() {
        super.onStart(); com.example.kineticvault.data.SyncManager.get(this).addListener(ledger);
    }
    @Override protected void onStop() {
        com.example.kineticvault.data.SyncManager.get(this).removeListener(ledger); super.onStop();
    }
    @Override protected void onResume() {
        super.onResume();
        View hero = findViewById(R.id.home_orb);
        pulse = android.animation.ValueAnimator.ofFloat(.94f, 1.04f);
        pulse.setDuration(1600); pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        pulse.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        pulse.addUpdateListener(a -> { float value = (float) a.getAnimatedValue(); hero.setScaleX(value); hero.setScaleY(value); });
        pulse.start();
    }
    @Override protected void onPause() { if (pulse != null) pulse.cancel(); super.onPause(); }
}
