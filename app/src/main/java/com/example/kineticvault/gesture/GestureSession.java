package com.example.kineticvault.gesture;

import com.example.kineticvault.core.Constants;
import java.util.Arrays;
import java.util.Objects;

/** In-memory token owner; use on one thread and close on cancellation or backgrounding. */
public final class GestureSession implements AutoCloseable {
    public interface Listener {
        void onProgress(int tokenCount);
        void onFinished();
        void onTooShort(int tokenCount);
    }

    private final byte[] tokens = new byte[Constants.MAX_TOKENS];
    private final Listener listener;
    private int count;
    private long lastMove = -1;
    private long lastMotion = -1;
    private long stillSince = -1;
    private boolean finished;
    private boolean closed;

    public GestureSession(Listener listener) {
        this.listener = Objects.requireNonNull(listener);
    }

    /** Accepts either recognizer or pad tokens; timestamps share a monotonic ms clock. */
    public boolean addToken(byte token, long timestampMillis) {
        if (closed || finished || count == Constants.MAX_TOKENS) return false;
        if (token < Constants.LEFT || token > Constants.FLAT || timestampMillis < 0) {
            throw new IllegalArgumentException("Invalid gesture event");
        }
        if (timestampMillis < lastMotion || (lastMove >= 0
                && timestampMillis - lastMove < Constants.MOVE_GAP_MS)) return false;
        tokens[count++] = token;
        lastMove = timestampMillis;
        stillSince = -1;
        listener.onProgress(count);
        return true;
    }

    /** Wire directly to recognizer onMotion; missing samples never count as stillness. */
    public void onMotion(long timestampMillis, boolean still) {
        if (closed || finished || timestampMillis < 0 || timestampMillis <= lastMotion
                || timestampMillis < lastMove) return;
        boolean gap = lastMotion >= 0 && timestampMillis - lastMotion
                > Constants.SENSOR_MAX_DT_SECONDS * 1000;
        lastMotion = timestampMillis;
        if (!still || gap || count < Constants.MIN_TOKENS) stillSince = -1;
        if (!still || count < Constants.MIN_TOKENS) return;
        if (stillSince < 0) stillSince = timestampMillis;
        if (timestampMillis - stillSince >= Constants.STILL_FINISH_MS) done();
    }

    /** Too-short attempts remain editable so the host can ask for more moves. */
    public boolean done() {
        if (closed) return false;
        if (finished) return true;
        if (count < Constants.MIN_TOKENS) {
            listener.onTooShort(count);
            return false;
        }
        finished = true;
        stillSince = -1;
        listener.onFinished();
        return true;
    }

    public int size() { return count; }

    public boolean isFinished() { return finished && !closed; }

    /** Enrollment succeeds only for two distinct, completed performances. */
    public static boolean matches(GestureSession first, GestureSession second) {
        if (first == null || second == null || first == second
                || !first.isFinished() || !second.isFinished()) return false;
        int difference = first.count ^ second.count;
        for (int i = 0; i < Constants.MAX_TOKENS; i++) {
            difference |= first.tokens[i] ^ second.tokens[i];
        }
        return difference == 0;
    }

    /** Transfers a correctly sized copy for VaultEngine; caller must wipe it in finally. */
    public byte[] takeTokens() {
        if (!isFinished()) throw new IllegalStateException("Gesture is not complete");
        byte[] result = Arrays.copyOf(tokens, count);
        close();
        return result;
    }

    public void reset() {
        if (closed) throw new IllegalStateException("Gesture session is closed");
        clear();
        listener.onProgress(0);
    }

    private void clear() {
        Arrays.fill(tokens, (byte) 0);
        count = 0;
        finished = false;
        lastMove = lastMotion = stillSince = -1;
    }

    @Override public void close() {
        clear();
        closed = true;
    }
}
