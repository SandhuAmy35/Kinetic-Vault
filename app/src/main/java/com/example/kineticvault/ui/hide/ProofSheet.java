package com.example.kineticvault.ui.hide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.kineticvault.R;
import com.example.kineticvault.image.ImageUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/** Worker-rendered presentation image with stripped preview LSBs and a coarse map. */
final class ProofSheet {
    private ProofSheet() { }

    static File create(Context context, HideForge.Result result) throws IOException {
        ImageUtils.requireWorker();
        if (result == null) throw new IOException();
        File directory = new File(context.getCacheDir(), "shared");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException();
        File file = new File(directory, "proof-" + UUID.randomUUID() + ".png");
        Bitmap sheet = Bitmap.createBitmap(1080, 660, Bitmap.Config.ARGB_8888);
        boolean saved = false;
        try {
            Canvas canvas = new Canvas(sheet);
            canvas.drawColor(Color.rgb(8, 12, 20));
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            paint.setColor(context.getColor(R.color.kv_text)); paint.setTextSize(32);
            canvas.drawText(context.getString(R.string.invention_proof_title), 30, 52, paint);
            paint.setTextSize(22);
            panel(canvas, paint, result.before, 30, context.getString(R.string.invention_before));
            panel(canvas, paint, result.after, 385, context.getString(R.string.invention_after));
            canvas.drawText(context.getString(R.string.invention_density), 740, 104, paint);
            density(canvas, paint, result.visuals.diff, context.getColor(R.color.kv_primary));
            paint.setColor(context.getColor(R.color.kv_text)); paint.setTextSize(24);
            canvas.drawText(context.getString(R.string.invention_proof_pixels,
                    result.visuals.width, result.visuals.height, result.stats.changedPercent), 30, 492, paint);
            canvas.drawText(context.getString(R.string.invention_proof_stats,
                    result.stats.psnrDb, result.stats.maxChannelChange), 30, 534, paint);
            paint.setTextSize(20);
            canvas.drawText(context.getString(R.string.invention_proof_footer), 30, 591, paint);
            canvas.drawText(context.getString(R.string.invention_proof_footer_two), 30, 624, paint);
            ImageUtils.savePng(sheet, file); saved = true;
            return file;
        } finally { sheet.recycle(); if (!saved) file.delete(); }
    }

    private static void panel(Canvas canvas, Paint paint, Bitmap source, int left, String title) {
        canvas.drawText(title, left, 104, paint);
        int[] pixels = new int[source.getWidth() * source.getHeight()];
        Bitmap clean = null;
        try {
            source.getPixels(pixels, 0, source.getWidth(), 0, 0, source.getWidth(), source.getHeight());
            // A presentation preview must not reproduce the encoded channel bits.
            for (int i = 0; i < pixels.length; i++) pixels[i] = 0xff000000 | (pixels[i] & 0x00fefefe);
            clean = Bitmap.createBitmap(pixels, source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
            float scale = Math.min(320f / clean.getWidth(), 320f / clean.getHeight());
            float w = clean.getWidth() * scale, h = clean.getHeight() * scale;
            float x = left + (320 - w) / 2, y = 126 + (320 - h) / 2;
            canvas.drawBitmap(clean, null, new RectF(x, y, x + w, y + h), paint);
        } finally { Arrays.fill(pixels, 0); if (clean != null) clean.recycle(); }
    }

    private static void density(Canvas canvas, Paint paint, Bitmap map, int color) {
        int w = map.getWidth(), h = map.getHeight();
        int[] pixels = new int[w * h];
        try {
            map.getPixels(pixels, 0, w, 0, 0, w, h);
            for (int row = 0; row < 16; row++) for (int col = 0; col < 16; col++) {
                int count = 0, total = 0;
                for (int y = row * h / 16; y < (row + 1) * h / 16; y++)
                    for (int x = col * w / 16; x < (col + 1) * w / 16; x++) {
                        total++; if ((pixels[y * w + x] & 0xffffff) != 0) count++;
                    }
                paint.setColor(color); paint.setAlpha(total == 0 ? 20 : 20 + 235 * count / total);
                canvas.drawRect(740 + col * 20, 126 + row * 20,
                        758 + col * 20, 144 + row * 20, paint);
            }
        } finally { paint.setAlpha(255); paint.setColor(Color.WHITE); Arrays.fill(pixels, 0); }
    }
}
