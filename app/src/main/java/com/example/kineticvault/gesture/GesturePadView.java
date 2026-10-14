package com.example.kineticvault.gesture;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import com.example.kineticvault.R;
import com.example.kineticvault.core.Constants;

/** Six Canvas controls. Hosts should reserve at least getMinimumWidth/Height(). */
public final class GesturePadView extends View {
    public interface OnTokenListener {
        /** Return true only when the session accepts the move. */
        boolean onToken(byte token, long timestampMillis);
    }

    private static final byte[] TOKENS = {Constants.LEFT, Constants.RIGHT,
            Constants.FORWARD, Constants.BACK, Constants.SNAP, Constants.FLAT};
    private static final int[] LABELS = {R.string.gesture_left, R.string.gesture_right,
            R.string.gesture_forward, R.string.gesture_back, R.string.gesture_snap,
            R.string.gesture_flat};
    private static final int[] ACTIONS = {R.id.gesture_pad_left, R.id.gesture_pad_right,
            R.id.gesture_pad_forward, R.id.gesture_pad_back, R.id.gesture_pad_snap,
            R.id.gesture_pad_flat};
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF[] cells = new RectF[TOKENS.length];
    private final String[] labels = new String[TOKENS.length];
    private final float gap;
    private final float radius;
    private final float stroke;
    private final float textSize;
    private final float touchMin;
    private OnTokenListener listener;
    private int selected;
    private int pressed = -1;
    private int pointer = -1;
    private long lastMove = -1;

    public GesturePadView(Context context) { this(context, null); }

    public GesturePadView(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public GesturePadView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        gap = dimension(R.dimen.space_sm);
        radius = dimension(R.dimen.control_radius);
        stroke = dimension(R.dimen.stroke_width);
        textSize = dimension(R.dimen.type_body);
        touchMin = dimension(R.dimen.touch_min);
        for (int i = 0; i < cells.length; i++) {
            cells[i] = new RectF();
            labels[i] = context.getString(LABELS[i]);
        }
        setMinimumWidth((int) Math.ceil(2 * touchMin + 3 * gap));
        setMinimumHeight((int) Math.ceil(3 * touchMin + 4 * gap));
        setFocusable(true);
        setClickable(true);
        setContentDescription(context.getString(R.string.gesture_pad_description));
        setAccessibilityDelegate(new PadAccessibility());
    }

    public void setOnTokenListener(OnTokenListener listener) { this.listener = listener; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = (int) dimension(R.dimen.gesture_pad_width) + getPaddingLeft() + getPaddingRight();
        int height = getMinimumHeight() + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(resolveSize(Math.max(getMinimumWidth(), width), widthSpec),
                resolveSize(height, heightSpec));
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float cellWidth = (w - getPaddingLeft() - getPaddingRight() - 3 * gap) / 2;
        float cellHeight = (h - getPaddingTop() - getPaddingBottom() - 4 * gap) / 3;
        for (int i = 0; i < cells.length; i++) {
            float x = getPaddingLeft() + gap + (i % 2) * (cellWidth + gap);
            float y = getPaddingTop() + gap + (i / 2) * (cellHeight + gap);
            cells[i].set(x, y, x + Math.max(0, cellWidth), y + Math.max(0, cellHeight));
        }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i = 0; i < cells.length; i++) drawCell(canvas, i);
    }

    private void drawCell(Canvas canvas, int index) {
        RectF cell = cells[index];
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getContext().getColor(pressed == index
                ? R.color.kv_surface_high : R.color.kv_surface));
        canvas.drawRoundRect(cell, radius, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setColor(getContext().getColor(hasFocus() && selected == index
                ? R.color.kv_primary : R.color.kv_outline));
        canvas.drawRoundRect(cell, radius, radius, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getContext().getColor(isEnabled() ? R.color.kv_text : R.color.kv_text_secondary));
        paint.setTextSize(textSize);
        float available = Math.max(0, cell.width() - 2 * gap);
        float measured = paint.measureText(labels[index]);
        if (measured > available && measured > 0) paint.setTextSize(textSize * available / measured);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(labels[index], cell.centerX(),
                cell.centerY() - (paint.ascent() + paint.descent()) / 2, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                pressed = hit(event.getX(), event.getY());
                if (pressed < 0) return false;
                pointer = event.getPointerId(0);
                selected = pressed;
                requestFocus();
                break;
            case MotionEvent.ACTION_MOVE:
                int position = event.findPointerIndex(pointer);
                if (position < 0 || hit(event.getX(position), event.getY(position)) != pressed) clearPress();
                break;
            case MotionEvent.ACTION_UP:
                boolean activate = pressed >= 0 && event.getPointerId(0) == pointer
                        && hit(event.getX(), event.getY()) == pressed;
                clearPress();
                if (activate) performClick();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_CANCEL:
                clearPress();
                break;
            default: break;
        }
        invalidate();
        return true;
    }

    @Override public boolean performClick() {
        if (!isEnabled()) return false;
        long now = SystemClock.elapsedRealtime();
        if (lastMove >= 0 && now - lastMove < Constants.MOVE_GAP_MS) return false;
        if (listener == null || !listener.onToken(TOKENS[selected], now)) return false;
        lastMove = now;
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        super.performClick();
        return true;
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isEnabled()) return super.onKeyDown(keyCode, event);
        int next = selected;
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) next = selected - selected % 2;
        else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) next = selected - selected % 2 + 1;
        else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) next = Math.max(0, selected - 2);
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) next = Math.min(cells.length - 1, selected + 2);
        else return super.onKeyDown(keyCode, event);
        selected = next;
        setContentDescription(getContext().getString(R.string.gesture_pad_selected, labels[selected]));
        invalidate();
        return true;
    }

    private int hit(float x, float y) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i].width() >= touchMin && cells[i].height() >= touchMin
                    && cells[i].contains(x, y)) return i;
        }
        return -1;
    }

    private void clearPress() { pressed = pointer = -1; }

    private float dimension(int resource) { return getResources().getDimension(resource); }

    @Override protected void onDetachedFromWindow() {
        clearPress();
        lastMove = -1;
        super.onDetachedFromWindow();
    }

    private final class PadAccessibility extends AccessibilityDelegate {
        @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            for (int i = 0; i < ACTIONS.length; i++) {
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(ACTIONS[i], labels[i]));
            }
        }

        @Override public boolean performAccessibilityAction(View host, int action, Bundle args) {
            for (int i = 0; i < ACTIONS.length; i++) {
                if (action == ACTIONS[i]) {
                    selected = i;
                    invalidate();
                    return performClick();
                }
            }
            return super.performAccessibilityAction(host, action, args);
        }
    }
}
