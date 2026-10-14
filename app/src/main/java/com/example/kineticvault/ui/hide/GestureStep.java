package com.example.kineticvault.ui.hide;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import com.example.kineticvault.R;
import com.example.kineticvault.gesture.GesturePadView;
import com.example.kineticvault.gesture.GestureRecognizer;
import com.example.kineticvault.gesture.GestureSession;
import com.example.kineticvault.gesture.StrengthMeter;
import com.example.kineticvault.sensor.SensorFusionManager;
import com.example.kineticvault.ui.KineticOrbView;
import com.google.android.material.button.MaterialButton;
import java.util.Arrays;

final class GestureStep {
    private final HideActivity host;
    private final SecretStep secret;
    private final KineticOrbView orb;
    private final GesturePadView pad;
    private final CheckBox usePad;
    private final TextView status, strength;
    private final MaterialButton record, done, forge;
    private final SensorFusionManager sensors;
    private GestureSession first, current;
    private byte[] enrolled;

    GestureStep(HideActivity host, SecretStep secret) {
        this.host = host; this.secret = secret;
        orb = host.findViewById(R.id.hide_orb);
        pad = host.findViewById(R.id.hide_gesture_pad);
        usePad = host.findViewById(R.id.hide_use_pad);
        status = host.findViewById(R.id.hide_gesture_status);
        strength = host.findViewById(R.id.hide_strength);
        record = host.findViewById(R.id.hide_record);
        done = host.findViewById(R.id.hide_gesture_done);
        forge = host.findViewById(R.id.hide_forge);
        sensors = new SensorFusionManager(host, new GestureRecognizer.Listener() {
            public void onToken(byte token, long time) { if (current != null) current.addToken(token, time); }
            public void onMotion(float pitch, float roll, boolean still, long time) {
                orb.setAngles(pitch, roll);
                if (current != null) current.onMotion(time, still);
            }
        });
        bind();
        usePad.setChecked(com.example.kineticvault.data.VaultPreferences.current().pad);
        pad.setVisibility(usePad.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void bind() {
        pad.setOnTokenListener((token, time) -> current != null && current.addToken(token, time));
        usePad.setOnCheckedChangeListener((button, checked) -> {
            reset(); pad.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        record.setOnClickListener(v -> begin());
        done.setOnClickListener(v -> { if (current != null) current.done(); });
        forge.setOnClickListener(v -> host.forge());
        host.findViewById(R.id.hide_gesture_reset).setOnClickListener(v -> reset());
        reset();
    }

    private void begin() {
        if (current != null || enrolled != null) return;
        orb.reset(); orb.setAnimating(true);
        strength.setText(StrengthMeter.label(host, 0, secret.hasPassphrase()));
        status.setText(first == null ? R.string.hide_gesture_record_first : R.string.hide_gesture_record_second);
        current = new GestureSession(new GestureSession.Listener() {
            public void onProgress(int count) {
                orb.setTokenCount(count, !usePad.isChecked());
                strength.setText(StrengthMeter.label(host, count, secret.hasPassphrase()));
            }
            public void onFinished() { finishAttempt(); }
            public void onTooShort(int count) { host.message(R.string.gesture_too_short); }
        });
        record.setEnabled(false); done.setEnabled(true); pad.setEnabled(true);
        if (!usePad.isChecked()) startSensors();
    }

    private void startSensors() {
        SensorFusionManager.Mode mode = sensors.start();
        if (mode == SensorFusionManager.Mode.UNAVAILABLE) {
            usePad.setChecked(true);
            host.message(R.string.gesture_sensor_unavailable);
        } else if (mode == SensorFusionManager.Mode.ACCELEROMETER_ONLY) {
            host.message(R.string.gesture_sensor_accelerometer_only);
        }
    }

    private void finishAttempt() {
        sensors.stop(); orb.setAnimating(false);
        done.setEnabled(false); pad.setEnabled(false);
        if (first == null) {
            first = current; current = null;
            record.setText(R.string.hide_repeat_gesture); record.setEnabled(true);
            status.setText(R.string.hide_gesture_repeat_prompt);
            return;
        }
        if (!GestureSession.matches(first, current)) {
            reset();
            status.setText(R.string.gesture_enrollment_mismatch);
            host.message(R.string.gesture_enrollment_mismatch);
            return;
        }
        enrolled = first.takeTokens();
        first = null;
        current.close(); current = null;
        status.setText(R.string.hide_gesture_enrolled);
        forge.setEnabled(true);
    }

    byte[] takeTokens() {
        if (enrolled == null) return null;
        byte[] result = enrolled;
        enrolled = null;
        forge.setEnabled(false);
        return result;
    }

    void reset() {
        sensors.stop(); orb.setAnimating(false); orb.reset();
        if (current != null) current.close();
        if (first != null) first.close();
        if (enrolled != null) Arrays.fill(enrolled, (byte) 0);
        current = first = null; enrolled = null;
        record.setText(R.string.hide_begin_gesture); record.setEnabled(true);
        done.setEnabled(false); forge.setEnabled(false); pad.setEnabled(false);
        status.setText(R.string.hide_gesture_ready);
        strength.setText(StrengthMeter.label(host, 0, secret.hasPassphrase()));
    }
}
