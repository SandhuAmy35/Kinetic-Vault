package com.example.kineticvault.ui;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.util.Arrays;

/** Owns and wipes the supplied characters; never saves animation state. */
public final class TextScrambler {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final TextView target;
    private char[] secret, frame;
    private int tick;
    private final Runnable next = this::advance;

    public TextScrambler(TextView target) { this.target = target; }

    public void show(char[] text) {
        clear(); secret = text;
        frame = new char[Math.min(text.length, 4096)]; tick = 0;
        target.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        advance();
    }

    private void advance() {
        if (secret == null) return;
        int visible = (int) (frame.length * Math.min(18, tick) / 18L);
        // Never reveal half of a surrogate pair while animating Unicode text.
        if (visible > 0 && visible < secret.length && Character.isHighSurrogate(secret[visible - 1])) visible--;
        for (int i = 0; i < frame.length; i++) {
            frame[i] = i < visible ? secret[i] : (char) ('A' + (i + tick * 7) % 26);
        }
        target.setText(frame, 0, frame.length);
        if (tick++ < 18) main.postDelayed(next, 35);
        else {
            target.setText(secret, 0, secret.length);
            target.setImportantForAccessibility(android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            Arrays.fill(secret, '\0'); Arrays.fill(frame, '\0');
            secret = frame = null;
        }
    }

    public void clear() {
        main.removeCallbacks(next);
        if (secret != null) Arrays.fill(secret, '\0');
        if (frame != null) Arrays.fill(frame, '\0');
        secret = frame = null;
        target.setText("");
    }
}
