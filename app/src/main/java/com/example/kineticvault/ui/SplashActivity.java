package com.example.kineticvault.ui;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.example.kineticvault.R;
import com.example.kineticvault.data.VaultPreferences;

public final class SplashActivity extends AppCompatActivity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private ValueAnimator animator;
    private boolean elapsed, loaded, resumed, routed;
    private final Runnable delay = () -> { elapsed = true; route(); };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this);
        android.view.View blank = new android.view.View(this);
        blank.setBackgroundColor(getColor(R.color.kv_background)); setContentView(blank);
        VaultPreferences.load(this, () -> {
            if (isFinishing() || isDestroyed()) return;
            loaded = true;
            if (VaultPreferences.current().disguise) { elapsed = true; route(); }
            else showLogo();
        });
    }
    private void showLogo() {
        SplashLogoView logo = new SplashLogoView(this);
        logo.setBackgroundColor(getColor(R.color.kv_background)); setContentView(logo);
        animator = ValueAnimator.ofFloat(0, 1); animator.setDuration(1000);
        animator.addUpdateListener(value -> logo.setProgress((float) value.getAnimatedValue()));
        if (resumed) animator.start();
        main.postDelayed(delay, 1200);
    }
    private void route() {
        if (!elapsed || !loaded || !resumed || routed || isFinishing() || isDestroyed()) return;
        routed = true;
        startActivity(new Intent(this, VaultPreferences.current().disguise ? CalculatorActivity.class : HomeActivity.class));
        finish();
    }
    @Override protected void onResume() {
        super.onResume(); resumed = true;
        if (animator != null && !elapsed) animator.start(); route();
    }
    @Override protected void onPause() { resumed = false; if (animator != null) animator.cancel(); super.onPause(); }
    @Override protected void onDestroy() {
        main.removeCallbacks(delay); if (animator != null) animator.cancel(); super.onDestroy();
    }
}
