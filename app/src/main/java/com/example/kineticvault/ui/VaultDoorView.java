package com.example.kineticvault.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.example.kineticvault.R;

public final class VaultDoorView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator animation;
    private float opening, denial;

    public VaultDoorView(Context context) { this(context, null); }
    public VaultDoorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setContentDescription(context.getString(R.string.reveal_door_locked));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) * .82f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.kv_surface_high));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, radius * .025f));
        paint.setColor(ContextCompat.getColor(getContext(), denial > 0
                ? R.color.kv_danger : opening > .9f ? R.color.kv_success : R.color.kv_primary));
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.drawCircle(cx, cy, radius * .85f, paint);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(radius * .07f);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4 + opening * .25;
            float inner = radius * (.27f + .52f * opening);
            canvas.drawLine(cx + (float) Math.cos(angle) * inner,
                    cy + (float) Math.sin(angle) * inner,
                    cx + (float) Math.cos(angle) * radius * .79f,
                    cy + (float) Math.sin(angle) * radius * .79f, paint);
        }
        canvas.drawCircle(cx, cy, radius * .18f * (1 - opening), paint);
    }

    public void open(Runnable completed) {
        reset();
        animation = ValueAnimator.ofFloat(0, 1);
        animation.setDuration(700);
        animation.addUpdateListener(value -> { opening = (float) value.getAnimatedValue(); invalidate(); });
        animation.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(Animator value) { cancelled = true; }
            @Override public void onAnimationEnd(Animator value) {
                if (!cancelled) {
                    setContentDescription(getContext().getString(R.string.reveal_door_open));
                    completed.run();
                }
            }
        });
        animation.start();
    }

    public void deny() {
        reset();
        animation = ValueAnimator.ofFloat(1, 0);
        animation.setDuration(650);
        animation.addUpdateListener(value -> { denial = (float) value.getAnimatedValue(); invalidate(); });
        animation.start();
    }

    public void reset() {
        if (animation != null) animation.cancel();
        animation = null; opening = denial = 0;
        clearAnimation(); invalidate();
        setContentDescription(getContext().getString(R.string.reveal_door_locked));
    }

    @Override protected void onDetachedFromWindow() { reset(); super.onDetachedFromWindow(); }
}
