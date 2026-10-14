package com.example.kineticvault.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.example.kineticvault.R;

/** Display-only thumbnails. The associated SeekBar provides accessible wipe control. */
public final class WipeComparisonView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private Bitmap before, after;
    private float fraction = 0.5f;

    public WipeComparisonView(Context context) { this(context, null); }
    public WipeComparisonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setContentDescription(context.getString(R.string.hide_comparison_description));
    }

    public void setImages(Bitmap original, Bitmap encoded) {
        before = original;
        after = encoded;
        invalidate();
    }

    public void setFraction(float value) {
        fraction = Math.max(0, Math.min(1, value));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (before == null || after == null) return;
        float scale = Math.min(getWidth() / (float) before.getWidth(), getHeight() / (float) before.getHeight());
        float w = before.getWidth() * scale, h = before.getHeight() * scale;
        destination.set((getWidth() - w) / 2, (getHeight() - h) / 2,
                (getWidth() + w) / 2, (getHeight() + h) / 2);
        canvas.drawBitmap(before, null, destination, paint);
        float split = destination.left + w * fraction;
        int saved = canvas.save();
        canvas.clipRect(split, destination.top, destination.right, destination.bottom);
        canvas.drawBitmap(after, null, destination, paint);
        canvas.restoreToCount(saved);
        paint.setColor(getContext().getColor(R.color.kv_primary));
        paint.setStrokeWidth(getResources().getDimension(R.dimen.stroke_width) * 2);
        canvas.drawLine(split, destination.top, split, destination.bottom, paint);
    }
}
