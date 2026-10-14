package com.example.kineticvault.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.example.kineticvault.R;
import com.example.kineticvault.core.StegoCore;
import com.example.kineticvault.image.ImageUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/** Worker-only hooks accept encrypted images and metadata, never decrypted content. */
public final class VaultLogger {
    private static final Object LOCK = new Object();
    private VaultLogger() { }

    public static VaultEntry capture(Context context, Uri source, Bitmap image,
            boolean ghost, boolean usesPassphrase) throws IOException {
        VaultRepository.requireWorker();
        synchronized (LOCK) { return captureLocked(context, source, image, ghost, usesPassphrase); }
    }

    private static VaultEntry captureLocked(Context context, Uri source, Bitmap image,
            boolean ghost, boolean usesPassphrase) throws IOException {
        File directory = new File(context.getFilesDir(), "vaults");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Vault storage unavailable");
        String uuid = UUID.randomUUID().toString();
        File copy = new File(directory, uuid + ".png");
        boolean keep = false;
        try (VaultRepository repository = new VaultRepository(context)) {
            copy(context, source, copy);
            String hash = ImageUtils.sha256(copy);
            VaultEntry existing = repository.findByHash(hash);
            if (existing != null) return existing;
            VaultEntry entry = entry(context, image, uuid, copy, hash, ghost, usesPassphrase);
            repository.insert(entry);
            keep = true;
            return entry;
        } finally { if (!keep) copy.delete(); }
    }

    private static VaultEntry entry(Context context, Bitmap image, String uuid, File file,
            String hash, boolean ghost, boolean usesPassphrase) throws IOException {
        VaultEntry entry = new VaultEntry();
        entry.uuid = uuid; entry.title = context.getString(R.string.vault_default_title);
        entry.imageUri = FileProvider.getUriForFile(context, context.getPackageName() + ".files", file).toString();
        entry.imageSha256 = hash; entry.ghost = ghost; entry.usesPassphrase = usesPassphrase;
        entry.capacityBytes = StegoCore.capacityBytes(image.getWidth() * image.getHeight());
        entry.createdAt = System.currentTimeMillis();
        Bitmap thumbnail = ImageUtils.thumbnail(image, 96);
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            if (!thumbnail.compress(Bitmap.CompressFormat.PNG, 100, bytes)) throw new IOException("Thumbnail unavailable");
            entry.thumb = bytes.toByteArray();
        } finally { thumbnail.recycle(); }
        return entry;
    }

    private static void copy(Context context, Uri source, File destination) throws IOException {
        try (InputStream input = context.getContentResolver().openInputStream(source);
                FileOutputStream output = new FileOutputStream(destination)) {
            if (input == null) throw new IOException("Vault image unavailable");
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
        }
    }

    public static void logHide(Context context, File source, Bitmap image, int type, long bytes,
            boolean ghost, boolean usesPassphrase) throws IOException {
        VaultEntry entry = capture(context, Uri.fromFile(source), image, ghost, usesPassphrase);
        record(context, entry, "HIDE", type, bytes);
    }

    public static void logReveal(Context context, VaultEntry entry, boolean success, int type, long bytes) {
        record(context, entry, success ? "REVEAL_OK" : "REVEAL_FAIL", success ? type : null, success ? bytes : null);
    }

    public static void logExport(Context context, VaultEntry entry) { record(context, entry, "EXPORT", null, null); }

    public static void logExport(Context context, File image) throws IOException {
        VaultRepository.requireWorker();
        try (VaultRepository repository = new VaultRepository(context)) {
            VaultEntry entry = repository.findByHash(ImageUtils.sha256(image));
            if (entry != null) repository.record(entry, "EXPORT", null, null);
        }
    }

    public static void logAutoLock(Context context, VaultEntry entry) { record(context, entry, "AUTOLOCK", null, null); }

    private static void record(Context context, VaultEntry entry, String event, Integer type, Long bytes) {
        VaultRepository.requireWorker();
        if (entry == null) return;
        try (VaultRepository repository = new VaultRepository(context)) { repository.record(entry, event, type, bytes); }
    }
}
