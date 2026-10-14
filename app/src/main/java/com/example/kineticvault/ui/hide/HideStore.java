package com.example.kineticvault.ui.hide;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.example.kineticvault.core.Constants;
import com.example.kineticvault.core.GzipCodec;
import com.example.kineticvault.core.PayloadCodec;
import com.example.kineticvault.core.StegoCore;
import com.example.kineticvault.image.CoverGenerator;
import com.example.kineticvault.image.ImageUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

/** All fields and methods belong to HideWork's single worker thread. */
final class HideStore implements AutoCloseable {
    static final class CoverInfo {
        final Bitmap preview;
        final int width, height, capacity;
        CoverInfo(Bitmap preview, int width, int height) {
            this.preview = preview;
            this.width = width;
            this.height = height;
            capacity = StegoCore.capacityBytes(width * height);
        }
    }
    static final class Capacity {
        final int needed, available;
        Capacity(int needed, int available) { this.needed = needed; this.available = available; }
        boolean fits() { return needed <= available; }
    }

    private final Context context;
    private Bitmap cover;
    private PayloadCodec.Payload prepared;
    private boolean compress, ghost;
    private File capture;
    File output;
    private HideForge.Result rendered;

    HideStore(Context context) { this.context = context.getApplicationContext(); }

    CoverInfo load(Uri uri) throws IOException {
        clearCover();
        return adopt(ImageUtils.decodeCover(ImageUtils.source(context.getContentResolver(), uri), true));
    }

    CoverInfo generate() throws IOException {
        clearCover();
        return adopt(CoverGenerator.generate(1024, 1024, System.nanoTime()));
    }

    private CoverInfo adopt(Bitmap bitmap) throws IOException {
        try {
            Bitmap thumbnail = ImageUtils.thumbnail(bitmap, 512);
            cover = bitmap;
            return new CoverInfo(thumbnail, bitmap.getWidth(), bitmap.getHeight());
        } catch (IOException | RuntimeException | OutOfMemoryError error) {
            bitmap.recycle();
            throw error;
        }
    }

    Uri prepareCapture() throws IOException {
        deleteCapture();
        File directory = new File(context.getCacheDir(), "capture");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Camera storage unavailable");
        capture = new File(directory, UUID.randomUUID() + ".jpg");
        if (!capture.createNewFile()) throw new IOException("Camera storage unavailable");
        return FileProvider.getUriForFile(context, context.getPackageName() + ".files", capture);
    }

    CoverInfo finishCapture() throws IOException {
        if (capture == null) throw new IOException("Camera image unavailable");
        try {
            clearCover();
            return adopt(ImageUtils.decodeCover(ImageUtils.source(capture), true));
        } finally { deleteCapture(); }
    }

    void deleteCapture() {
        if (capture != null) capture.delete();
        capture = null;
    }

    Capacity measure(SecretInput input, boolean keep) throws IOException {
        if (cover == null) throw new IOException("Choose a cover");
        PayloadCodec.Payload payload = null;
        byte[] packed = null, zipped = null;
        try {
            if (keep) clearPrepared();
            payload = input.read(context.getContentResolver());
            packed = PayloadCodec.pack(payload);
            zipped = GzipCodec.compress(packed, input.compress);
            Capacity result = new Capacity(zipped.length + Constants.TAG_BYTES,
                    StegoCore.capacityBytes(cover.getWidth() * cover.getHeight()));
            if (keep && result.fits()) {
                prepared = payload;
                payload = null;
                compress = input.compress;
                ghost = input.ghost;
            }
            return result;
        } finally {
            if (payload != null) payload.close();
            if (packed != null) Arrays.fill(packed, (byte) 0);
            if (zipped != null) Arrays.fill(zipped, (byte) 0);
        }
    }

    HideForge.Result forge(byte[] tokens, char[] passphrase, HideWork.Progress progress,
                           Runnable guard) throws Exception {
        if (prepared == null || cover == null) throw new IOException("Prepare the secret again");
        try {
            releaseRendered();
            HideForge.Result result = HideForge.run(context, cover, prepared, tokens,
                    passphrase, compress, ghost, progress, guard);
            output = result.file;
            rendered = result;
            return result;
        } finally { clearPrepared(); }
    }

    void clearPrepared() {
        if (prepared != null) prepared.close();
        prepared = null;
    }

    private void clearCover() {
        clearPrepared();
        if (cover != null) cover.recycle();
        cover = null;
    }

    private void releaseRendered() {
        if (rendered == null) return;
        rendered.before.recycle(); rendered.after.recycle(); rendered.visuals.close();
        rendered = null;
    }

    @Override public void close() { clearCover(); deleteCapture(); releaseRendered(); }
}
