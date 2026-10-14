package com.example.kineticvault.ui;

import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import com.example.kineticvault.R;
import com.example.kineticvault.core.Constants;
import com.example.kineticvault.data.VaultPreferences;
import com.example.kineticvault.sensor.SensorFusionManager;

/** Transient display of the same samples used by recognition. Never saved or logged. */
public final class NerdOverlay implements SensorFusionManager.TelemetryListener {
    private static final int[] LABELS = {R.string.gesture_left, R.string.gesture_right,
            R.string.gesture_forward, R.string.gesture_back, R.string.gesture_snap, R.string.gesture_flat};
    private final TextView view;
    private final byte[] tokens = new byte[8];
    private int count;
    private long rendered;
    private String sample = "";
    public NerdOverlay(TextView view) {
        this.view = view; view.setSaveEnabled(false); clear();
    }
    public void clear() {
        java.util.Arrays.fill(tokens, (byte) 0); count = 0; rendered = 0; sample = "";
        view.setVisibility(VaultPreferences.current().nerd ? View.VISIBLE : View.GONE);
        view.setText(R.string.nerd_idle);
    }
    public void begin(boolean pad) {
        clear();
        if (pad) view.setText(R.string.nerd_pad);
    }
    public void token(byte token) {
        if (!VaultPreferences.current().nerd || token < 0 || token >= LABELS.length) return;
        if (count == tokens.length) { System.arraycopy(tokens, 1, tokens, 0, tokens.length - 1); count--; }
        tokens[count++] = token; render();
    }
    @Override public void onSample(float ax, float ay, float az, float gx, float gy, float gz,
                                   float pitch, float roll, SensorFusionManager.Mode mode) {
        if (!VaultPreferences.current().nerd || SystemClock.uptimeMillis() - rendered < 120) return;
        rendered = SystemClock.uptimeMillis();
        String gyro = mode == SensorFusionManager.Mode.GYROSCOPE_FUSION
                ? view.getContext().getString(R.string.nerd_gyro, gx, gy, gz)
                : view.getContext().getString(R.string.nerd_no_gyro);
        sample = view.getContext().getString(R.string.nerd_sample, ax, ay, az, gyro, pitch, roll,
                mode == SensorFusionManager.Mode.GYROSCOPE_FUSION ? Constants.FILTER_ALPHA : 0f);
        render();
    }
    private void render() {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) names.append(" · ");
            names.append(view.getContext().getString(LABELS[tokens[i]]));
        }
        view.setText(view.getContext().getString(R.string.nerd_panel,
                sample.isEmpty() ? view.getContext().getString(R.string.nerd_pad) : sample,
                names.length() == 0 ? view.getContext().getString(R.string.nerd_no_tokens) : names));
    }
}
