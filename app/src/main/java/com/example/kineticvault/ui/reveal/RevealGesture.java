package com.example.kineticvault.ui.reveal;

import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import com.example.kineticvault.R;
import com.example.kineticvault.gesture.GesturePadView;
import com.example.kineticvault.gesture.GestureRecognizer;
import com.example.kineticvault.gesture.GestureSession;
import com.example.kineticvault.sensor.SensorFusionManager;
import java.util.Arrays;

final class RevealGesture {
    private final RevealActivity host;
    private final SensorFusionManager sensors;
    private final GesturePadView pad;
    private final CheckBox usePad;
    private final TextView status;
    private GestureSession session;
    private byte[] tokens;

    RevealGesture(RevealActivity host) {
        this.host = host;
        pad = host.findViewById(R.id.reveal_pad);
        usePad = host.findViewById(R.id.reveal_use_pad);
        status = host.findViewById(R.id.reveal_gesture_status);
        sensors = new SensorFusionManager(host, new GestureRecognizer.Listener() {
            public void onToken(byte token, long time) { if (session != null) session.addToken(token, time); }
            public void onMotion(float pitch, float roll, boolean still, long time) {
                if (session != null) session.onMotion(time, still);
            }
        });
        pad.setOnTokenListener((token, time) -> session != null && session.addToken(token, time));
        usePad.setOnCheckedChangeListener((button, checked) -> {
            clear(); pad.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        host.findViewById(R.id.reveal_record).setOnClickListener(v -> begin());
        host.findViewById(R.id.reveal_done).setOnClickListener(v -> { if (session != null) session.done(); });
        clear();
    }
        usePad.setChecked(com.example.kineticvault.data.VaultPreferences.current().pad);
        pad.setVisibility(usePad.isChecked() ? View.VISIBLE : View.GONE);

    private void begin() {
        clear();
        session = new GestureSession(new GestureSession.Listener() {
            public void onProgress(int count) { status.setText(host.getString(R.string.reveal_moves, count)); }
            public void onTooShort(int count) { host.message(R.string.gesture_too_short); }
            public void onFinished() {
                sensors.stop(); tokens = session.takeTokens(); session = null;
                pad.setEnabled(false); host.findViewById(R.id.reveal_done).setEnabled(false);
                host.findViewById(R.id.reveal_unlock).setEnabled(true);
                status.setText(R.string.reveal_gesture_ready);
            }
        });
        pad.setEnabled(true); host.findViewById(R.id.reveal_done).setEnabled(true);
        status.setText(R.string.reveal_recording);
        if (!usePad.isChecked()) {
            SensorFusionManager.Mode mode = sensors.start();
            if (mode == SensorFusionManager.Mode.UNAVAILABLE) {
                usePad.setChecked(true); host.message(R.string.gesture_sensor_unavailable);
            } else if (mode == SensorFusionManager.Mode.ACCELEROMETER_ONLY) {
                host.message(R.string.gesture_sensor_accelerometer_only);
            }
        }
    }

    byte[] takeTokens() { byte[] result = tokens; tokens = null; return result; }

    void clear() {
        sensors.stop();
        if (session != null) session.close();
        if (tokens != null) Arrays.fill(tokens, (byte) 0);
        session = null; tokens = null;
        pad.setEnabled(false);
        host.findViewById(R.id.reveal_done).setEnabled(false);
        host.findViewById(R.id.reveal_unlock).setEnabled(false);
        status.setText(R.string.reveal_gesture_prompt);
    }
}
