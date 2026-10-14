package com.example.kineticvault.ui;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.view.View;
import com.example.kineticvault.R;

public final class SplashLogoView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path logo = new Path(), drawn = new Path();
    private final PathMeasure measure = new PathMeasure();
    private float progress;
    public SplashLogoView(Context context) {
        super(context); setContentDescription(context.getString(R.string.splash_description));
    }
    public void setProgress(float value) { progress = value; invalidate(); }
    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        float s = Math.min(w, h) * .55f, x = w / 2f, y = h / 2f;
        logo.reset(); logo.moveTo(x, y - s / 2); logo.lineTo(x + s / 2, y - s / 4);
        logo.lineTo(x + s / 2, y + s / 5); logo.lineTo(x, y + s / 2);
        logo.lineTo(x - s / 2, y + s / 5); logo.lineTo(x - s / 2, y - s / 4);
        logo.close(); measure.setPath(logo, false);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); drawn.reset();
        measure.getSegment(0, measure.getLength() * progress, drawn, true);
        paint.setColor(getContext().getColor(R.color.kv_primary)); paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(getResources().getDisplayMetrics().density * 4);
        paint.setStrokeCap(Paint.Cap.ROUND); canvas.drawPath(drawn, paint);
        paint.setAlpha((int) (255 * progress));
        float x = getWidth() / 2f, y = getHeight() / 2f;
        canvas.drawLine(x, y - 20, x, y + 20, paint); paint.setAlpha(255);
    }
}
