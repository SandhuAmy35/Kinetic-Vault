package com.example.kineticvault.image;

import android.graphics.Bitmap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class ImageFiles {
    private ImageFiles() { }

    static void savePng(Bitmap bitmap, File destination) throws IOException {
        ImageUtils.requireWorker();
        ImageUtils.requireOpaque(bitmap);
        File parent = destination.getAbsoluteFile().getParentFile();
        if (parent == null || (!parent.isDirectory() && !parent.mkdirs())) {
            throw new IOException("Cannot create PNG directory");
        }
        File temporary = File.createTempFile("kvlt-", ".part", parent);
        try {
            write(bitmap, temporary);
            try {
                Files.move(temporary.toPath(), destination.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }

    private static void write(Bitmap bitmap, File file) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            // The format, not the filename or quality argument, guarantees PNG.
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                throw new IOException("PNG encoding failed");
            }
            output.flush();
            output.getFD().sync();
        }
    }

    static String sha256(File file) throws IOException {
        ImageUtils.requireWorker();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, count);
                }
            }
            StringBuilder text = new StringBuilder(64);
            for (byte value : digest.digest()) {
                text.append(Character.forDigit((value >>> 4) & 15, 16));
                text.append(Character.forDigit(value & 15, 16));
            }
            return text.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IOException("SHA-256 unavailable", failure);
        }
    }
}
