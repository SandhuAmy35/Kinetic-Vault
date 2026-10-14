package com.example.kineticvault.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.example.kineticvault.R;

/** Draws a worker-built LSB plane; never needs the original cover. */
public final class XRayView extends View {
    private final Paint paint = new Paint();
    private final RectF area = new RectF();
    private Bitmap plane;
    public XRayView(Context context) { this(context, null); }
    public XRayView(Context context, AttributeSet attrs) {
        super(context, attrs); setContentDescription(context.getString(R.string.diff_xray_description));
    }
    public void setPlane(Bitmap value) { plane = value; invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.drawColor(Color.BLACK);
        if (plane == null) return;
        float scale = Math.min(getWidth() / (float) plane.getWidth(), getHeight() / (float) plane.getHeight());
        float w = plane.getWidth() * scale, h = plane.getHeight() * scale;
        area.set((getWidth() - w) / 2, (getHeight() - h) / 2, (getWidth() + w) / 2, (getHeight() + h) / 2);
        canvas.drawBitmap(plane, null, area, paint);
    }
}
