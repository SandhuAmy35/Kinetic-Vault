package com.example.kineticvault.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import com.example.kineticvault.R;
import java.util.Random;

public final class MatrixRainView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final float[] drops = new float[24];
    private float progress = -1;
    private long lastFrame;
    private boolean running;

    public MatrixRainView(Context context) { this(context, null); }
    public MatrixRainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        for (int i = 0; i < drops.length; i++) drops[i] = -random.nextFloat();
    }

    /** total == 0 is indeterminate. Animation is decoration, never an invented percentage. */
    public void setProgress(int completed, int total) {
        progress = total > 0 ? Math.max(0, Math.min(1, completed / (float) total)) : -1;
        invalidate();
    }

    public void setRunning(boolean value) {
        running = value;
        lastFrame = 0;
        if (value) postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cell = Math.max(1, getWidth() / (float) drops.length);
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        paint.setTypeface(android.graphics.Typeface.MONOSPACE);
        paint.setTextSize(cell * 0.85f);
        for (int i = 0; i < drops.length; i++) {
            if (running) drops[i] += dt * (0.18f + (i % 4) * 0.035f + Math.max(0, progress) * 0.3f);
            if (drops[i] * getHeight() > getHeight() + cell * 9) drops[i] = -random.nextFloat();
            for (int tail = 0; tail < 9; tail++) {
                paint.setColor(getContext().getColor(tail == 0 ? R.color.kv_text : R.color.kv_primary));
                paint.setAlpha((int) ((1 - tail / 9f) * (progress < 0 ? 150 : 150 + progress * 105)));
                canvas.drawText(((i + tail + (int) (drops[i] * 40)) & 1) == 0 ? "0" : "1",
                        i * cell, drops[i] * getHeight() - tail * cell, paint);
            }
        }
        if (running && isShown() && getWindowVisibility() == VISIBLE) postInvalidateOnAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        setRunning(false);
        super.onDetachedFromWindow();
    }
}
