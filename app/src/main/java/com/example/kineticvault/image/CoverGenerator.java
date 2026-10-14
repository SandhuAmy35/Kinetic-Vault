package com.example.kineticvault.image;

import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CancellationException;

public final class CoverGenerator {
    private CoverGenerator() { }

    public static Bitmap generate(int width, int height, long seed) throws IOException {
        ImageUtils.requireWorker();
        ImageMemory.check(width, height, true);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888,
                false, ColorSpace.get(ColorSpace.Named.SRGB));
        Random random = new Random(seed);
        int[] row = new int[width];
        try {
            for (int y = 0; y < height; y++) {
                if (Thread.currentThread().isInterrupted()) { throw new CancellationException(); }
                for (int x = 0; x < width; x++) {
                    row[x] = color(x / (double) width, y / (double) height, random);
                }
                bitmap.setPixels(row, 0, width, 0, y, width, 1);
            }
            return bitmap;
        } catch (RuntimeException failure) {
            bitmap.recycle();
            throw failure;
        }
    }

    private static int color(double x, double y, Random random) {
        double wave = Math.sin(x * 7.0 + Math.sin(y * 5.0)) * 0.5 + 0.5;
        double halo = Math.exp(-9.0 * ((x - 0.7) * (x - 0.7) + (y - 0.3) * (y - 0.3)));
        int noise = random.nextInt(13) - 6;
        int red = clamp((int) (18 + 83 * wave * (1 - y) + 20 * halo) + noise);
        int green = clamp((int) (25 + 95 * (1 - wave) + 90 * halo) + noise);
        int blue = clamp((int) (65 + 98 * wave + 70 * halo) + noise);
        return 0xff000000 | (red << 16) | (green << 8) | blue;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
