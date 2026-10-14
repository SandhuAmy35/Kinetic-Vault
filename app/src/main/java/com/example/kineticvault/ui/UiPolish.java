package com.example.kineticvault.ui;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import com.example.kineticvault.R;

/** Shared first-entrance motion and touch feedback without replacing click actions. */
public final class UiPolish {
    private UiPolish() { }
    public static void install(Activity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content == null || content.getTag(R.id.polish_installed) != null) return;
        content.setTag(R.id.polish_installed, Boolean.TRUE);
        buttons(content);
        if (!(activity instanceof HomeActivity) && !(activity instanceof SplashActivity)) enter(content);
    }
    @SuppressLint("ClickableViewAccessibility")
    private static void buttons(View view) {
        if (view instanceof Button) view.setOnTouchListener((button, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_UP && button.isPressed() && button.isEnabled())
                feedback(button);
            return false;
        });
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) buttons(group.getChildAt(i));
        }
    }
    public static void enter(View view) {
        if (ValueAnimator.areAnimatorsEnabled())
            view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), R.anim.slide_up));
    }
    public static void feedback(View view) { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); }
    public static void notice(View view) {
        view.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        if (ValueAnimator.areAnimatorsEnabled())
            view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_in));
    }
    public static void failure(View view) {
        notice(view); view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }
    public static void pause(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content != null) content.clearAnimation();
    }
}
