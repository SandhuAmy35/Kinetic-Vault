package com.example.kineticvault.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.View;
import com.example.kineticvault.R;
import com.example.kineticvault.core.Constants;
import java.util.Arrays;

public final class KineticOrbView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF dial = new RectF();
    private final float[] trailX = new float[24];
    private final float[] trailY = new float[24];
    private float targetX, targetY, x, y;
    private int tokens, trailCount, head;
    private boolean animating;

    public KineticOrbView(Context context) { this(context, null); }
    public KineticOrbView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setContentDescription(context.getString(R.string.orb_description, 0, Constants.MAX_TOKENS));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setAngles(float pitch, float roll) {
        targetX = Math.max(-1, Math.min(1, roll / Constants.TILT_ENTER_DEGREES));
        targetY = Math.max(-1, Math.min(1, -pitch / Constants.TILT_ENTER_DEGREES));
    }

    public void setTokenCount(int count, boolean haptic) {
        int next = Math.max(0, Math.min(Constants.MAX_TOKENS, count));
        if (haptic && next > tokens) performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        tokens = next;
        setContentDescription(getContext().getString(R.string.orb_description, tokens, Constants.MAX_TOKENS));
        invalidate();
    }

    public void setAnimating(boolean running) {
        animating = running;
        if (running) postInvalidateOnAnimation();
    }

    public void reset() {
        targetX = targetY = x = y = 0;
        trailCount = head = tokens = 0;
        Arrays.fill(trailX, 0);
        Arrays.fill(trailY, 0);
        setTokenCount(0, false);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float radius = Math.min(getWidth(), getHeight()) * 0.41f;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        drawDial(canvas, cx, cy, radius);
        if (animating) {
            x += (targetX - x) * 0.18f;
            y += (targetY - y) * 0.18f;
            trailX[head] = x;
            trailY[head] = y;
            head = (head + 1) % trailX.length;
            trailCount = Math.min(trailCount + 1, trailX.length);
        }
        drawOrb(canvas, cx, cy, radius);
        if (animating && isShown() && getWindowVisibility() == VISIBLE) postInvalidateOnAnimation();
    }

    private void drawDial(Canvas canvas, float cx, float cy, float radius) {
        dial.set(cx - radius, cy - radius, cx + radius, cy + radius);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.045f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < Constants.MAX_TOKENS; i++) {
            paint.setColor(getContext().getColor(i < tokens ? R.color.kv_primary : R.color.kv_outline));
            paint.setAlpha(i < tokens ? 255 : 70);
            canvas.drawArc(dial, -87 + i * 45, 36, false, paint);
        }
        paint.setAlpha(255);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawOrb(Canvas canvas, float cx, float cy, float radius) {
        paint.setColor(getContext().getColor(R.color.kv_primary));
        for (int i = 0; i < trailCount; i++) {
            int index = (head - trailCount + i + trailX.length) % trailX.length;
            float fade = (i + 1f) / trailX.length;
            paint.setAlpha((int) (fade * 80));
            canvas.drawCircle(cx + trailX[index] * radius * 0.52f,
                    cy + trailY[index] * radius * 0.52f, radius * 0.12f * fade, paint);
        }
        float bx = cx + x * radius * 0.52f, by = cy + y * radius * 0.52f;
        for (int i = 5; i >= 1; i--) {
            paint.setAlpha(15 + (5 - i) * 12);
            canvas.drawCircle(bx, by, radius * (0.12f + i * 0.035f), paint);
        }
        paint.setAlpha(255);
        canvas.drawCircle(bx, by, radius * 0.115f, paint);
        paint.setColor(getContext().getColor(R.color.kv_text));
        canvas.drawCircle(bx - radius * 0.025f, by - radius * 0.03f, radius * 0.035f, paint);
    }

    @Override protected void onDetachedFromWindow() {
        setAnimating(false);
        reset();
        super.onDetachedFromWindow();
    }
}
