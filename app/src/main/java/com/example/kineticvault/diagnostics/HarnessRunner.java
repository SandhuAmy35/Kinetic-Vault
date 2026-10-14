package com.example.kineticvault.diagnostics;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import com.example.kineticvault.R;
import com.example.kineticvault.core.PayloadCodec;
import com.example.kineticvault.core.VaultEngine;
import com.example.kineticvault.core.WorkProgress;
import com.example.kineticvault.image.CoverGenerator;
import com.example.kineticvault.image.ImageUtils;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

public final class HarnessRunner {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;

    public interface Listener {
        void onStage(int testIndex, int stringResource);
    }

    private HarnessRunner() { }

    public static HarnessReport run(Context context, Listener listener)
            throws IOException, GeneralSecurityException {
        ImageUtils.requireWorker();
        HarnessReport report = new HarnessReport();
        long start = SystemClock.elapsedRealtime();
        boolean success = false;
        try {
            stage(listener, 0, R.string.stage_generate);
            int[] cover = generate(report);
            report.generationMs = SystemClock.elapsedRealtime() - start;
            for (int i = 0; i < 4; i++) {
                report.entries.add(runCase(context, cover, i, listener));
            }
            report.totalMs = SystemClock.elapsedRealtime() - start;
            success = true;
            return report;
        } finally {
            if (!success) { report.close(); }
        }
    }

    private static int[] generate(HarnessReport report) throws IOException {
        Bitmap bitmap = CoverGenerator.generate(WIDTH, HEIGHT, 0x4b564c54L);
        try {
            report.preview = ImageUtils.thumbnail(bitmap, 512);
            return ImageUtils.pixels(bitmap);
        } finally {
            bitmap.recycle();
        }
    }

    private static HarnessReport.Entry runCase(Context context, int[] cover, int index,
            Listener listener) throws IOException, GeneralSecurityException {
        HarnessReport.Entry entry = new HarnessReport.Entry();
        entry.ghost = index >= 2;
        entry.file = (index % 2) == 1;
        ImageUtils.checkMemory(WIDTH, HEIGHT, entry.ghost);
        // These are public fixtures, never enrolled credentials or real secrets.
        byte[] tokens = {0, 0, 4, 5};
        char[] passphrase = "phase1b-public-fixture".toCharArray();
        File file = null;
        long start = SystemClock.elapsedRealtime();
        try (PayloadCodec.Payload payload = payload(context, entry.file)) {
            file = temporaryPng(context);
            roundTrip(cover, payload, tokens, passphrase, file, entry, index, listener);
            entry.totalMs = SystemClock.elapsedRealtime() - start;
            return entry;
        } finally {
            Arrays.fill(tokens, (byte) 0);
            Arrays.fill(passphrase, '\0');
            if (file != null) { Files.deleteIfExists(file.toPath()); }
        }
    }

    private static void roundTrip(int[] cover, PayloadCodec.Payload payload, byte[] tokens,
            char[] passphrase, File file, HarnessReport.Entry entry, int index, Listener listener)
            throws IOException, GeneralSecurityException {
        stage(listener, index, R.string.stage_forge);
        WorkProgress progress = (step, done, total) -> checkCancelled();
        long start = SystemClock.elapsedRealtime();
        int[] encoded = VaultEngine.hide(cover, payload, tokens, passphrase, true, entry.ghost, progress);
        entry.forgeMs = SystemClock.elapsedRealtime() - start;
        save(encoded, file, entry, index, listener);
        int[] reloaded = reload(file, entry, index, listener);
        if (!Arrays.equals(encoded, reloaded)) {
            throw new IOException("PNG reload changed the encoded pixels");
        }
        unlock(reloaded, payload, tokens, passphrase, entry, index, listener);
        stage(listener, index, R.string.stage_compare);
        entry.stats = ImageUtils.statistics(cover, reloaded);
        if (entry.stats.maxChannelChange > 1 || entry.stats.pixelsChanged == 0) {
            throw new IOException("Unexpected LSB changes");
        }
        entry.sha256 = ImageUtils.sha256(file);
    }

    private static void save(int[] encoded, File file, HarnessReport.Entry entry,
            int index, Listener listener) throws IOException {
        stage(listener, index, R.string.stage_save);
        long start = SystemClock.elapsedRealtime();
        Bitmap bitmap = ImageUtils.fromPixels(encoded, WIDTH, HEIGHT);
        try {
            ImageUtils.savePng(bitmap, file);
        } finally {
            bitmap.recycle();
        }
        entry.saveMs = SystemClock.elapsedRealtime() - start;
    }

    private static int[] reload(File file, HarnessReport.Entry entry, int index,
            Listener listener) throws IOException {
        stage(listener, index, R.string.stage_reload);
        long start = SystemClock.elapsedRealtime();
        Bitmap bitmap = ImageUtils.decodeExact(ImageUtils.source(file), entry.ghost);
        try {
            int[] pixels = ImageUtils.pixels(bitmap);
            entry.reloadMs = SystemClock.elapsedRealtime() - start;
            return pixels;
        } finally {
            bitmap.recycle();
        }
    }

    private static void unlock(int[] pixels, PayloadCodec.Payload expected, byte[] tokens,
            char[] passphrase, HarnessReport.Entry entry, int index, Listener listener)
            throws IOException, GeneralSecurityException {
        stage(listener, index, R.string.stage_unlock);
        long start = SystemClock.elapsedRealtime();
        WorkProgress progress = (step, done, total) -> checkCancelled();
        try (PayloadCodec.Payload actual = VaultEngine.reveal(pixels, tokens, passphrase, progress)) {
            if (actual.type != expected.type || !actual.filename.equals(expected.filename)
                    || !Arrays.equals(actual.data, expected.data)) {
                throw new IOException("Recovered payload differs from the original");
            }
        }
        entry.unlockMs = SystemClock.elapsedRealtime() - start;
    }

    private static PayloadCodec.Payload payload(Context context, boolean file) throws IOException {
        if (!file) {
            byte[] text = context.getString(R.string.harness_fixture_text).getBytes(StandardCharsets.UTF_8);
            return new PayloadCodec.Payload(PayloadCodec.TEXT, "", text);
        }
        byte[] bytes = new byte[4096];
        for (int i = 0; i < bytes.length; i++) { bytes[i] = (byte) (i * 31); }
        return new PayloadCodec.Payload(PayloadCodec.FILE, "fixture.bin", bytes);
    }

    private static File temporaryPng(Context context) throws IOException {
        File directory = new File(context.getCacheDir(), "shared");
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Cannot create test directory");
        }
        // A unique name prevents cancelled and newly launched tests colliding.
        return File.createTempFile("phase1b-", ".png", directory);
    }

    private static void stage(Listener listener, int index, int resource) {
        checkCancelled();
        if (listener != null) { listener.onStage(index, resource); }
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) { throw new CancellationException(); }
    }
}
