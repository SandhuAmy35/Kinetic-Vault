package com.example.kineticvault.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.graphics.Rect;
import java.io.IOException;
import java.io.InputStream;

final class ImageDecoder {
    private ImageDecoder() { }

    static Bitmap cover(ImageUtils.Source source, boolean ghost) throws IOException {
        ImageUtils.requireWorker();
        BitmapFactory.Options bounds = bounds(source);
        int limit = ImageMemory.maximumPixels(ghost, ImageMemory.availableBudget());
        BitmapFactory.Options options = options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, limit);
        Bitmap decoded = decode(source, options);
        try {
            int[] size = ImageMemory.fitCover(decoded.getWidth(), decoded.getHeight(), limit);
            ImageMemory.check(size[0], size[1], ghost);
            return renderOpaque(decoded, size[0], size[1]);
        } finally {
            decoded.recycle();
        }
    }

    static Bitmap exact(ImageUtils.Source source, boolean ghost) throws IOException {
        ImageUtils.requireWorker();
        BitmapFactory.Options bounds = bounds(source);
        ImageMemory.check(bounds.outWidth, bounds.outHeight, ghost);
        // No sampling, density scaling, EXIF rotation, crop or thumbnail source.
        Bitmap decoded = decode(source, options());
        try {
            if (decoded.getWidth() != bounds.outWidth || decoded.getHeight() != bounds.outHeight) {
                throw new IOException("Image source changed during decoding");
            }
            ImageUtils.requireOpaque(decoded);
            return decoded;
        } catch (IOException | RuntimeException failure) {
            decoded.recycle();
            throw failure;
        }
    }

    private static BitmapFactory.Options bounds(ImageUtils.Source source) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream input = source.open()) {
            if (input == null) { throw new IOException("Image is unavailable"); }
            BitmapFactory.decodeStream(input, null, options);
        }
        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw new IOException("Unsupported or damaged image");
        }
        return options;
    }

    private static BitmapFactory.Options options() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
        options.inMutable = true;
        options.inScaled = false;
        options.inSampleSize = 1;
        return options;
    }

    private static Bitmap decode(ImageUtils.Source source, BitmapFactory.Options options)
            throws IOException {
        try (InputStream input = source.open()) {
            if (input == null) { throw new IOException("Image is unavailable"); }
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            if (bitmap == null) { throw new IOException("Cannot decode image"); }
            if (bitmap.getConfig() != Bitmap.Config.ARGB_8888 || !bitmap.isMutable()
                    || bitmap.getColorSpace() == null || !bitmap.getColorSpace().isSrgb()) {
                bitmap.recycle();
                throw new IOException("Decoder did not provide mutable sRGB ARGB_8888");
            }
            return bitmap;
        }
    }

    private static int sampleSize(int width, int height, int limit) {
        int sample = 1;
        while (sample < (1 << 30)) {
            long sampledWidth = (width + (long) sample - 1) / sample;
            long sampledHeight = (height + (long) sample - 1) / sample;
            if (sampledWidth * sampledHeight <= limit) { break; }
            sample *= 2;
        }
        return sample;
    }

    static Bitmap renderOpaque(Bitmap source, int width, int height) {
        Bitmap target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888,
                false, ColorSpace.get(ColorSpace.Named.SRGB));
        try {
            Canvas canvas = new Canvas(target);
            // Composite BEFORE embedding so premultiplied alpha cannot erase LSBs.
            canvas.drawColor(0xff0a0e1a);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(source, null, new Rect(0, 0, width, height), paint);
            return target;
        } catch (RuntimeException failure) {
            target.recycle();
            throw failure;
        }
    }
}
