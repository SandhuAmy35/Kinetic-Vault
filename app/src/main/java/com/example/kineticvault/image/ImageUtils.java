package com.example.kineticvault.image;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.ColorSpace;
import android.net.Uri;
import android.os.Looper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageUtils {
    public interface Source {
        // Supply the original bytes. The source must remain stable across opens.
        InputStream open() throws IOException;
    }

    private ImageUtils() { }

    public static Source source(File file) {
        return () -> new FileInputStream(file);
    }

    public static Source source(ContentResolver resolver, Uri uri) {
        // SAF/OpenDocument URIs are read directly, never through thumbnail APIs.
        return () -> resolver.openInputStream(uri);
    }

    public static Bitmap decodeCover(Source source, boolean ghost) throws IOException {
        return ImageDecoder.cover(source, ghost);
    }

    public static Bitmap decodeExact(Source source, boolean ghost) throws IOException {
        return ImageDecoder.exact(source, ghost);
    }

    public static Bitmap flattenOpaque(Bitmap source) throws IOException {
        requireWorker();
        requireSoftware(source);
        ImageMemory.check(source.getWidth(), source.getHeight(), false);
        return ImageDecoder.renderOpaque(source, source.getWidth(), source.getHeight());
    }

    public static Bitmap thumbnail(Bitmap source, int longSide) throws IOException {
        requireWorker();
        requireSoftware(source);
        if (longSide < 1 || longSide > 512) { throw new IOException("Invalid thumbnail size"); }
        double scale = Math.min(1.0, longSide / (double) Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.floor(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.floor(source.getHeight() * scale));
        // This is a display-only copy. Never pass a thumbnail to vault extraction.
        return ImageDecoder.renderOpaque(source, width, height);
    }

    public static int[] pixels(Bitmap bitmap) throws IOException {
        requireWorker();
        requireOpaque(bitmap);
        int count = ImageMemory.pixelCount(bitmap.getWidth(), bitmap.getHeight());
        int[] pixels = new int[count];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        return pixels;
    }

    public static Bitmap fromPixels(int[] pixels, int width, int height) throws IOException {
        requireWorker();
        int count = ImageMemory.pixelCount(width, height);
        if (pixels == null || pixels.length != count) { throw new IOException("Wrong pixel count"); }
        for (int pixel : pixels) {
            if ((pixel >>> 24) != 255) { throw new IOException("Pixels must be opaque"); }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888,
                false, ColorSpace.get(ColorSpace.Named.SRGB));
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static void savePng(Bitmap bitmap, File destination) throws IOException {
        ImageFiles.savePng(bitmap, destination);
    }

    public static String sha256(File file) throws IOException {
        return ImageFiles.sha256(file);
    }

    public static ImageStats statistics(int[] before, int[] after) {
        requireWorker();
        return ImageStats.compare(before, after);
    }

    public static double psnr(int[] before, int[] after) {
        return statistics(before, after).psnrDb;
    }

    public static void checkMemory(int width, int height, boolean ghost) throws IOException {
        ImageMemory.check(width, height, ghost);
    }

    static void requireSoftware(Bitmap bitmap) throws IOException {
        if (bitmap == null || bitmap.isRecycled() || bitmap.getConfig() != Bitmap.Config.ARGB_8888
                || bitmap.getColorSpace() == null || !bitmap.getColorSpace().isSrgb()) {
            throw new IOException("Expected software sRGB ARGB_8888 bitmap");
        }
    }

    static void requireOpaque(Bitmap bitmap) throws IOException {
        requireSoftware(bitmap);
        ImageMemory.pixelCount(bitmap.getWidth(), bitmap.getHeight());
        if (!bitmap.hasAlpha()) { return; }
        int[] chunk = new int[Math.min(8192, bitmap.getWidth())];
        for (int y = 0; y < bitmap.getHeight(); y++) {
            for (int x = 0; x < bitmap.getWidth(); x += chunk.length) {
                int count = Math.min(chunk.length, bitmap.getWidth() - x);
                bitmap.getPixels(chunk, 0, count, x, y, count, 1);
                for (int i = 0; i < count; i++) {
                    if ((chunk[i] >>> 24) != 255) {
                        throw new IOException("Transparent vault image cannot preserve LSBs");
                    }
                }
            }
        }
    }

    public static void requireWorker() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Image work must run off the main thread");
        }
    }
}
