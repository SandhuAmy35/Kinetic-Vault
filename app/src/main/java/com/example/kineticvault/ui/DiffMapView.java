package com.example.kineticvault.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import com.example.kineticvault.R;

public final class DiffMapView extends View {
    private final Paint paint = new Paint();
    private final RectF area = new RectF();
    private DiffMapData data;
    private ValueAnimator sweep;
    private float reveal = 1;
    private int pixelX = -1, pixelY;
    private boolean loupe;

    public DiffMapView(Context context) { this(context, null); }
    public DiffMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setContentDescription(context.getString(R.string.diff_description));
        setFocusable(true); setClickable(true);
    }

    public void setData(DiffMapData value) { stopSweep(); data = value; pixelX = -1; invalidate(); }

    public void startSweep() {
        stopSweep();
        if (data == null) return;
        sweep = ValueAnimator.ofFloat(0, 1);
        sweep.setDuration(1400);
        sweep.addUpdateListener(a -> { reveal = (float) a.getAnimatedValue(); invalidate(); });
        sweep.start();
    }

    public void stopSweep() {
        if (sweep != null) { sweep.cancel(); sweep = null; }
        reveal = 1; invalidate();
    }

    public void bindLoupe(TextView label) {
        setOnTouchListener((view, event) -> touch(event, label));
        setOnClickListener(v -> {
            if (data != null) inspect(data.width / 2, data.height / 2, label);
        });
    }

    private boolean touch(MotionEvent event, TextView label) {
        if (data == null) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && !area.contains(event.getX(), event.getY())) return false;
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            getParent().requestDisallowInterceptTouchEvent(true);
            int x = Math.max(0, Math.min(data.width - 1, (int) ((event.getX() - area.left) * data.width / area.width())));
            int y = Math.max(0, Math.min(data.height - 1, (int) ((event.getY() - area.top) * data.height / area.height())));
            inspect(x, y, label);
            if (action == MotionEvent.ACTION_DOWN) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            getParent().requestDisallowInterceptTouchEvent(false);
            if (action == MotionEvent.ACTION_UP) { loupe = true; performClick(); loupe = false; }
        }
        return true;
    }

    private void inspect(int x, int y, TextView label) {
        pixelX = x; pixelY = y;
        label.setText(getContext().getString(R.string.diff_pixel, x, y,
                rgb(data.original(x, y)), rgb(data.encoded(x, y))));
        invalidate();
    }

    private String rgb(int color) {
        int r = (color >>> 16) & 255, g = (color >>> 8) & 255, b = color & 255;
        return getContext().getString(R.string.diff_rgb, r, g, b, r & 1, g & 1, b & 1);
    }

    @Override public boolean performClick() {
        if (loupe) { sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED); return true; }
        return super.performClick();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);
        if (data == null) return;
        float scale = Math.min(getWidth() / (float) data.width, getHeight() / (float) data.height);
        float w = data.width * scale, h = data.height * scale;
        area.set((getWidth() - w) / 2, (getHeight() - h) / 2, (getWidth() + w) / 2, (getHeight() + h) / 2);
        int saved = canvas.save();
        canvas.clipRect(area.left, area.top, area.right, area.top + h * reveal);
        canvas.drawBitmap(data.diff, null, area, paint); canvas.restoreToCount(saved);
        paint.setColor(getContext().getColor(R.color.kv_primary)); paint.setStrokeWidth(2);
        if (reveal < 1) canvas.drawLine(area.left, area.top + h * reveal, area.right, area.top + h * reveal, paint);
        if (pixelX >= 0) drawLoupe(canvas);
    }

    private void drawLoupe(Canvas canvas) {
        int x = pixelX * data.diff.getWidth() / data.width;
        int y = pixelY * data.diff.getHeight() / data.height;
        Rect source = new Rect(Math.max(0, x - 4), Math.max(0, y - 4),
                Math.min(data.diff.getWidth(), x + 5), Math.min(data.diff.getHeight(), y + 5));
        float size = Math.min(getWidth() / 3f, 96 * getResources().getDisplayMetrics().density);
        RectF lens = new RectF(getWidth() - size - 4, 4, getWidth() - 4, size + 4);
        canvas.drawBitmap(data.diff, source, lens, paint);
        paint.setStyle(Paint.Style.STROKE); canvas.drawRect(lens, paint); paint.setStyle(Paint.Style.FILL);
        float cx = area.left + (pixelX + .5f) * area.width() / data.width;
        float cy = area.top + (pixelY + .5f) * area.height() / data.height;
        canvas.drawLine(cx - 8, cy, cx + 8, cy, paint); canvas.drawLine(cx, cy - 8, cx, cy + 8, paint);
    }

    @Override protected void onDetachedFromWindow() { stopSweep(); super.onDetachedFromWindow(); }
    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) stopSweep();
    }
}
