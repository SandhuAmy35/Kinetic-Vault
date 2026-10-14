package com.example.kineticvault.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import com.example.kineticvault.core.Constants;
import com.example.kineticvault.gesture.GestureRecognizer;
import java.util.Arrays;

/** Portrait device axes: +pitch raises the top (BACK); +roll lowers the right (RIGHT). */
public final class SensorFusionManager implements SensorEventListener {
    public enum Mode { GYROSCOPE_FUSION, ACCELEROMETER_ONLY, UNAVAILABLE }

    public interface TelemetryListener {
        void onSample(float ax, float ay, float az, float gx, float gy, float gz,
                      float pitch, float roll, Mode mode);
    }
    private TelemetryListener telemetry;
    private final float[] gyro = new float[3];
    public void setTelemetryListener(TelemetryListener listener) { telemetry = listener; }
    private final SensorManager sensors;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final GestureRecognizer recognizer;
    private final float[] gravity = new float[3];
    private final float[] acceleration = new float[3];
    private Mode mode = Mode.UNAVAILABLE;
    private boolean initialized;
    private boolean running;
    private float pitch;
    private float roll;
    private float accelPitch;
    private float accelRoll;
    private float angularSpeed;
    private long lastGyroNanos = -1;
    private long lastAccelNanos = -1;

    public SensorFusionManager(Context context, GestureRecognizer.Listener listener) {
        sensors = (SensorManager) context.getApplicationContext()
                .getSystemService(Context.SENSOR_SERVICE);
        recognizer = new GestureRecognizer(command -> {
            if (Looper.myLooper() == Looper.getMainLooper()) command.run();
            else main.post(command);
        }, listener);
    }

    /** Call from onResume. UNAVAILABLE means the host should offer GesturePadView. */
    public Mode start() {
        requireMainThread();
        stop();
        recognizer.setSensitivity(com.example.kineticvault.data.VaultPreferences.current().sensitivity);
        if (sensors == null) return mode;
        Sensor accelerometer = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null || !sensors.registerListener(this, accelerometer,
                Constants.SENSOR_PERIOD_US, main)) return mode;
        Sensor gyroscope = sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        boolean gyroAvailable = gyroscope != null && sensors.registerListener(this,
                gyroscope, Constants.SENSOR_PERIOD_US, main);
        mode = gyroAvailable ? Mode.GYROSCOPE_FUSION : Mode.ACCELEROMETER_ONLY;
        running = true;
        return mode;
    }

    /** Call from onPause; the host must also close any active GestureSession. */
    public void stop() {
        requireMainThread();
        running = false;
        if (sensors != null) sensors.unregisterListener(this);
        recognizer.reset();
        Arrays.fill(gravity, 0f);
        Arrays.fill(acceleration, 0f);
        Arrays.fill(gyro, 0f);
        pitch = roll = accelPitch = accelRoll = angularSpeed = 0f;
        lastGyroNanos = lastAccelNanos = -1;
        initialized = false;
        mode = Mode.UNAVAILABLE;
    }

    /** Clear recognition between enrollment performances without re-registering sensors. */
    public void resetGesture() {
        requireMainThread();
        recognizer.reset();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (!running || event.values.length < 3 || event.timestamp < 0) return;
        for (int i = 0; i < 3; i++) if (!Float.isFinite(event.values[i])) return;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) accelerometer(event);
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(event.values, 0, gyro, 0, 3);
            gyroscope(event);
        }
    }

    private void accelerometer(SensorEvent event) {
        if (event.timestamp <= lastAccelNanos) return;
        float dt = seconds(event.timestamp, lastAccelNanos);
        boolean restart = !initialized || dt > Constants.SENSOR_MAX_DT_SECONDS;
        for (int i = 0; i < 3; i++) {
            acceleration[i] = event.values[i];
            gravity[i] = restart ? acceleration[i] : Constants.GRAVITY_FILTER_ALPHA
                    * gravity[i] + (1 - Constants.GRAVITY_FILTER_ALPHA) * acceleration[i];
        }
        lastAccelNanos = event.timestamp;
        if (magnitude(gravity) < Constants.SENSOR_MIN_GRAVITY_MAGNITUDE) return;
        accelPitch = degrees(Math.atan2(gravity[1], gravity[2]));
        accelRoll = degrees(Math.atan2(-gravity[0], Math.hypot(gravity[1], gravity[2])));
        boolean freshGyro = mode == Mode.GYROSCOPE_FUSION && lastGyroNanos >= 0
                && seconds(event.timestamp, lastGyroNanos) <= Constants.SENSOR_MAX_DT_SECONDS;
        if (restart || !freshGyro) useAccelerometer(dt, restart);
        initialized = true;
        emit(event.timestamp);
    }

    private void useAccelerometer(float dt, boolean restart) {
        // Angle speed keeps the no-gyro fallback from calling a moving tilt "still".
        float deltaPitch = wrap(accelPitch - pitch);
        float deltaRoll = wrap(accelRoll - roll);
        angularSpeed = restart || dt <= 0 ? 0f
                : (float) Math.toRadians(Math.hypot(deltaPitch, deltaRoll)) / dt;
        pitch = accelPitch;
        roll = accelRoll;
    }

    private void gyroscope(SensorEvent event) {
        if (!initialized || event.timestamp <= lastGyroNanos) return;
        float dt = seconds(event.timestamp, lastGyroNanos);
        lastGyroNanos = event.timestamp;
        angularSpeed = magnitude(event.values);
        double p = Math.toRadians(pitch);
        double r = Math.toRadians(roll);
        if (dt <= 0 || dt > Constants.SENSOR_MAX_DT_SECONDS
                || Math.abs(Math.cos(r)) < Constants.FUSION_MIN_ROLL_COSINE) {
            pitch = accelPitch;
            roll = accelRoll;
            return;
        }
        // +X lifts the top: ay rises. +Y lowers the right: ax becomes negative.
        // Thus gyro +X agrees with atan2(ay,az), and +Y with atan2(-ax,hypot(ay,az)).
        double pitchRate = event.values[0] + Math.sin(p) * Math.tan(r) * event.values[1]
                + Math.cos(p) * Math.tan(r) * event.values[2];
        double rollRate = Math.cos(p) * event.values[1] - Math.sin(p) * event.values[2];
        pitch = blend(pitch, degrees(pitchRate), dt, accelPitch);
        roll = blend(roll, degrees(rollRate), dt, accelRoll);
    }

    private static float blend(float theta, float omega, float dt, float accelAngle) {
        float predicted = wrap(theta + omega * dt);
        // Equivalent to .98*(theta+omega*dt)+.02*accelAngle across the +/-180 seam.
        return wrap(predicted + (1 - Constants.FILTER_ALPHA) * wrap(accelAngle - predicted));
    }

    private void emit(long timestampNanos) {
        double p = Math.toRadians(pitch);
        double r = Math.toRadians(roll);
        float g = Constants.GRAVITY_METERS_PER_SECOND_SQUARED;
        float ax = acceleration[0] + g * (float) Math.sin(r);
        float ay = acceleration[1] - g * (float) (Math.sin(p) * Math.cos(r));
        float az = acceleration[2] - g * (float) (Math.cos(p) * Math.cos(r));
        if (telemetry != null) telemetry.onSample(acceleration[0], acceleration[1], acceleration[2],
                gyro[0], gyro[1], gyro[2], pitch, roll, mode);
        recognizer.update(timestampNanos / 1_000_000L, pitch, roll, ax, ay, az, angularSpeed);
    }

    private static float magnitude(float[] values) {
        return (float) Math.sqrt(values[0] * values[0] + values[1] * values[1]
                + values[2] * values[2]);
    }

    private static float seconds(long now, long before) {
        return before < 0 ? 0f : (now - before) / 1_000_000_000f;
    }

    private static float degrees(double radians) { return (float) Math.toDegrees(radians); }

    private static float wrap(float degrees) {
        return (degrees + 180f - (float) Math.floor((degrees + 180f) / 360f) * 360f) - 180f;
    }

    private static void requireMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            throw new IllegalStateException("Sensor lifecycle must run on the main thread");
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
