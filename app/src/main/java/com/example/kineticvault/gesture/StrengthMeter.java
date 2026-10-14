package com.example.kineticvault.gesture;

import android.content.Context;
import com.example.kineticvault.R;
import com.example.kineticvault.core.Constants;
import com.example.kineticvault.core.GestureCredential;

/** Estimates only the uniform-token upper bound, never the user's passphrase entropy. */
public final class StrengthMeter {
    private StrengthMeter() { }

    public static double bits(int tokenCount) {
        return GestureCredential.entropyBits(tokenCount);
    }

    public static String label(Context context, int tokenCount, boolean hasPassphrase) {
        double upperBound = bits(tokenCount);
        if (tokenCount < Constants.MIN_TOKENS) {
            return context.getString(R.string.gesture_strength_incomplete, upperBound,
                    Constants.MIN_TOKENS - tokenCount);
        }
        return context.getString(hasPassphrase ? R.string.gesture_strength_with_passphrase
                : R.string.gesture_strength_add_passphrase, upperBound);
    }
}
