package com.example.kineticvault.ui.hide;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.example.kineticvault.R;
import com.example.kineticvault.core.Constants;
import com.example.kineticvault.ui.KineticOrbView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Arrays;

/** Owns only an ephemeral copy, discarded on dismissal or the host pause. */
final class GestureReplay {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HideActivity host;
    private AlertDialog dialog;
    private KineticOrbView orb;
    private TextView label;
    private byte[] moves;
    private int index;
    private final Runnable tick = this::next;
    private static final int[] NAMES = {R.string.gesture_left, R.string.gesture_right,
            R.string.gesture_forward, R.string.gesture_back, R.string.gesture_snap, R.string.gesture_flat};

    GestureReplay(HideActivity host) { this.host = host; }

    void show(byte[] enrolled) {
        clear();
        if (enrolled == null) return;
        moves = enrolled.clone();
        host.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout panel = new LinearLayout(host);
        panel.setOrientation(LinearLayout.VERTICAL);
        int inset = host.getResources().getDimensionPixelSize(R.dimen.space_md);
        panel.setPadding(inset, inset, inset, inset);
        label = new TextView(host); label.setSaveEnabled(false);
        label.setAccessibilityLiveRegion(android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE);
        orb = new KineticOrbView(host); orb.setSaveEnabled(false);
        panel.addView(label);
        panel.addView(orb, new LinearLayout.LayoutParams(-1, inset * 12));
        dialog = new MaterialAlertDialogBuilder(host).setTitle(R.string.invention_replay)
                .setMessage(R.string.invention_replay_note).setView(panel)
                .setPositiveButton(android.R.string.ok, null).create();
        dialog.setOnDismissListener(ignored -> clear());
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        orb.setAnimating(ValueAnimator.areAnimatorsEnabled());
        next();
    }

    private void next() {
        if (moves == null || index == moves.length) return;
        int token = moves[index++];
        label.setText(host.getString(R.string.invention_replay_move, index, moves.length,
                host.getString(NAMES[token])));
        float angle = Constants.TILT_ENTER_DEGREES;
        orb.setAngles(token == 2 ? -angle : token == 3 ? angle : 0,
                token == 0 ? -angle : token == 1 ? angle : 0);
        orb.setTokenCount(index, false);
        if (index < moves.length) main.postDelayed(tick, 900);
        else { Arrays.fill(moves, (byte) 0); moves = null; }
    }

    void clear() {
        main.removeCallbacks(tick);
        if (moves != null) Arrays.fill(moves, (byte) 0);
        moves = null; index = 0;
        if (label != null) label.setText(null);
        if (orb != null) { orb.setAnimating(false); orb.reset(); }
        AlertDialog old = dialog; dialog = null;
        if (old != null) { old.setOnDismissListener(null); old.dismiss(); }
        label = null; orb = null;
        if (!com.example.kineticvault.data.VaultPreferences.current().protection
                && !com.example.kineticvault.data.VaultPreferences.current().nerd)
            host.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}
