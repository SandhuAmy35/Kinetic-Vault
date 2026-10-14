package com.example.kineticvault.gesture;

import com.example.kineticvault.core.Constants;
import java.util.Objects;
import java.util.concurrent.Executor;

/** Pure motion recognizer. Calls and its injected callback executor must be serialized. */
public final class GestureRecognizer {
    public interface Listener {
        void onToken(byte token, long timestampMillis);
        void onMotion(float pitchDegrees, float rollDegrees, boolean still,
                      long timestampMillis);
    }

    private final Executor callbackExecutor;
    private final Listener listener;
    private volatile long generation;
    private long lastSample = -1;
    private long lastMove = -1;
    private long flatSince = -1;
    private long snapSince = -1;
    private boolean tiltLatched;
    private boolean flatLatched;
    private boolean snapRejected;

    /** Android callers inject a main-thread executor; SensorFusionManager does this. */
    public GestureRecognizer(Executor mainThreadExecutor, Listener listener) {
        callbackExecutor = Objects.requireNonNull(mainThreadExecutor);
        this.listener = Objects.requireNonNull(listener);
    }

    /** Angles are degrees, acceleration is gravity-removed device XYZ in m/s^2. */
    public void update(long time, float pitch, float roll, float ax, float ay,
                       float az, float angularSpeed) {
        if (time < 0 || time <= lastSample || !finite(pitch, roll, ax, ay, az, angularSpeed)) {
            return;
        }
        if (lastSample >= 0 && time - lastSample > Constants.SENSOR_MAX_DT_SECONDS * 1000) {
            flatSince = snapSince = -1;
            snapRejected = true;
        }
        lastSample = time;
        boolean still = Math.sqrt(ax * ax + ay * ay + az * az)
                <= Constants.STILL_ACCEL_TOLERANCE
                && Math.abs(angularSpeed) <= Constants.STILL_GYRO_RADIANS_PER_SECOND;
        byte token = recognize(time, pitch, roll, az, still);
        long stamp = generation;
        callbackExecutor.execute(() -> deliver(stamp, token, time, pitch, roll, still));
    }

    private void deliver(long stamp, byte token, long time, float pitch, float roll,
                         boolean still) {
        if (stamp != generation) return;
        if (token >= 0) listener.onToken(token, time);
        if (stamp == generation) listener.onMotion(pitch, roll, still, time);
    }

    private byte recognize(long time, float pitch, float roll, float az, boolean still) {
        boolean neutral = Math.abs(pitch) <= Constants.TILT_EXIT_DEGREES
                && Math.abs(roll) <= Constants.TILT_EXIT_DEGREES;
        if (neutral) tiltLatched = false;
        boolean snap = detectSnap(time, pitch, roll, az);
        boolean flat = detectFlat(time, pitch, roll, still);
        if (lastMove >= 0 && time - lastMove < Constants.MOVE_GAP_MS) return -1;
        byte token = -1;
        if (snap) token = Constants.SNAP;
        else if (!tiltLatched && Math.max(Math.abs(pitch), Math.abs(roll))
                >= Constants.TILT_ENTER_DEGREES) {
            token = Math.abs(roll) >= Math.abs(pitch)
                    ? (roll > 0 ? Constants.RIGHT : Constants.LEFT)
                    : (pitch > 0 ? Constants.BACK : Constants.FORWARD);
            tiltLatched = true;
        } else if (flat && !flatLatched) {
            token = Constants.FLAT;
            flatLatched = true;
        }
        if (token >= 0) lastMove = time;
        return token;
    }

    private boolean detectFlat(long time, float pitch, float roll, boolean still) {
        boolean level = Math.abs(pitch) <= Constants.FLAT_DEGREES
                && Math.abs(roll) <= Constants.FLAT_DEGREES;
        // Leaving the pose re-arms FLAT; tiny acceleration noise must not repeat it.
        if (!level) flatLatched = false;
        if (!level || !still) {
            flatSince = -1;
            return false;
        }
        if (flatSince < 0) flatSince = time;
        return time - flatSince >= Constants.FLAT_HOLD_MS;
    }

    private boolean detectSnap(long time, float pitch, float roll, float az) {
        if (Math.max(Math.abs(pitch), Math.abs(roll)) > Constants.SNAP_MAX_TILT_DEGREES) {
            snapSince = -1;
            snapRejected = true;
            return false;
        }
        if (az >= Constants.SNAP_RECOVERY_METERS_PER_SECOND_SQUARED) {
            boolean pulse = snapSince >= 0 && !snapRejected
                    && time - snapSince <= Constants.SNAP_MAX_MS;
            snapSince = -1;
            snapRejected = false;
            return pulse;
        }
        if (!snapRejected && snapSince < 0
                && az <= Constants.SNAP_DOWN_METERS_PER_SECOND_SQUARED) snapSince = time;
        if (snapSince >= 0 && time - snapSince > Constants.SNAP_MAX_MS) snapRejected = true;
        return false;
    }

    private static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }

    /** Invalidates queued callbacks as well as clearing recognition history. */
    public void reset() {
        generation++;
        lastSample = lastMove = flatSince = snapSince = -1;
        tiltLatched = flatLatched = snapRejected = false;
    }
}
