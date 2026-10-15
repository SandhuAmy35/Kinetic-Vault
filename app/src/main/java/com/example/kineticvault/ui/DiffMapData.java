package com.example.kineticvault.ui;

import android.graphics.Bitmap;
import com.example.kineticvault.image.ImageStats;
import com.example.kineticvault.image.ImageUtils;
import java.io.IOException;
import java.util.Arrays;

/** Worker-built visual assets; retains exact source arrays for the pixel inspector. */
public final class DiffMapData implements AutoCloseable {
    public final int width, height;
    public final Bitmap diff, xray;
    public final ImageStats stats;
    private final int[] before, after;

    private DiffMapData(int[] before, int[] after, int width, int height,
                        Bitmap diff, Bitmap xray, ImageStats stats) {
        this.before = before; this.after = after; this.width = width; this.height = height;
        this.diff = diff; this.xray = xray; this.stats = stats;
    }

    /** Transfers ownership of both arrays. Preview bins preserve every changed channel. */
    public static DiffMapData build(int[] before, int[] after, int width, int height) throws IOException {
        ImageUtils.requireWorker();
        if (width <= 0 || height <= 0 || (long) width * height != after.length
                || before.length != after.length) throw new IOException("Invalid image dimensions");
        float scale = Math.min(1f, 512f / Math.max(width, height));
        int w = Math.max(1, (int) (width * scale)), h = Math.max(1, (int) (height * scale));
        int[] map = new int[w * h];
        Arrays.fill(map, 0xff000000);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int delta = before[y * width + x] ^ after[y * width + x];
                int rgb = ((delta & 0xff0000) != 0 ? 0xff0000 : 0)
                        | ((delta & 0xff00) != 0 ? 0xff00 : 0) | ((delta & 255) != 0 ? 255 : 0);
                map[(y * h / height) * w + x * w / width] |= rgb;
            }
        }
        Bitmap diff = ImageUtils.fromPixels(map, w, h);
        Bitmap xray = null;
        try {
            xray = buildXRay(after, width, height);
            return new DiffMapData(before, after, width, height, diff, xray,
                    ImageUtils.statistics(before, after));
        } catch (IOException | RuntimeException | OutOfMemoryError error) {
            diff.recycle();
            if (xray != null) xray.recycle();
            throw error;
        }
    }

    /** No original image is required; one LSB per channel is amplified to 0 or 255. */
    public static Bitmap buildXRay(int[] pixels, int width, int height) throws IOException {
        ImageUtils.requireWorker();
        if (width <= 0 || height <= 0 || (long) width * height != pixels.length)
            throw new IOException("Invalid image dimensions");
        float scale = Math.min(1f, 512f / Math.max(width, height));
        int w = Math.max(1, (int) (width * scale)), h = Math.max(1, (int) (height * scale));
        int[] plane = new int[w * h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int pixel = pixels[(y * height / h) * width + x * width / w];
            plane[y * w + x] = 0xff000000 | (((pixel >>> 16) & 1) * 255 << 16)
                    | (((pixel >>> 8) & 1) * 255 << 8) | ((pixel & 1) * 255);
        }
        return ImageUtils.fromPixels(plane, w, h);
    }

    public int original(int x, int y) { return before[y * width + x]; }
    public int encoded(int x, int y) { return after[y * width + x]; }

    @Override public void close() {
        diff.recycle(); xray.recycle();
        Arrays.fill(before, 0); Arrays.fill(after, 0);
    }
}
