package com.example.kineticvault.image;

import com.example.kineticvault.core.Constants;
import java.io.IOException;

public final class ImageMemory {
    private static final long MIB = 1024L * 1024L;
    private static final long WORK_RESERVE = 16 * MIB;

    private ImageMemory() { }

    public static long availableBudget() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long headroom = runtime.maxMemory() - used - 24 * MIB;
        // Leave room for the UI, decoder internals and native bitmap allocations.
        return Math.max(0, Math.min(runtime.maxMemory() / 2, headroom));
    }

    public static long requiredBytes(int pixels, boolean ghost) {
        // Includes bitmaps, pixel copies, PNG work and Ghost's int[] shuffle.
        return WORK_RESERVE + (long) pixels * (ghost ? 32 : 28);
    }

    public static int maximumPixels(boolean ghost, long budget) throws IOException {
        long count = (budget - WORK_RESERVE) / (ghost ? 32 : 28);
        if (count < Constants.HEADER_PIXELS) {
            throw new IOException("Insufficient image memory budget");
        }
        return (int) Math.min(Constants.MAX_PIXELS, count);
    }

    public static int pixelCount(int width, int height) throws IOException {
        long count = (long) width * height;
        if (width <= 0 || height <= 0 || count > Constants.MAX_PIXELS) {
            throw new IOException("Image exceeds the nine-million-pixel limit");
        }
        return (int) count;
    }

    public static void check(int width, int height, boolean ghost) throws IOException {
        int count = pixelCount(width, height);
        if (count > maximumPixels(ghost, availableBudget())) {
            throw new IOException("Image exceeds the available memory budget");
        }
    }

    public static int[] fitCover(int width, int height, int maxPixels) throws IOException {
        if (width <= 0 || height <= 0 || maxPixels < 1) {
            throw new IOException("Invalid image dimensions");
        }
        int limit = Math.min(maxPixels, Constants.MAX_PIXELS);
        double scale = Math.min(1.0, Constants.COVER_LONG_SIDE / (double) Math.max(width, height));
        scale = Math.min(scale, Math.sqrt(limit / ((double) width * height)));
        int targetWidth = Math.max(1, (int) Math.floor(width * scale));
        int targetHeight = Math.max(1, (int) Math.floor(height * scale));
        // Very thin images can round up to one pixel on their short side.
        while ((long) targetWidth * targetHeight > limit) {
            if (targetWidth >= targetHeight) { targetWidth--; }
            else { targetHeight--; }
        }
        return new int[]{targetWidth, targetHeight};
    }
}
