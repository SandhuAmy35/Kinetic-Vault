package com.example.kineticvault.ui.hide;

import android.content.Context;
import android.graphics.Bitmap;
import com.example.kineticvault.R;
import com.example.kineticvault.core.PayloadCodec;
import com.example.kineticvault.core.VaultEngine;
import com.example.kineticvault.data.VaultLogger;
import com.example.kineticvault.image.ImageStats;
import com.example.kineticvault.image.ImageUtils;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

final class HideForge {
    static final class Result {
        final File file;
        final Bitmap before, after;
        final ImageStats stats;
        Result(File file, Bitmap before, Bitmap after, ImageStats stats) {
            this.file = file; this.before = before; this.after = after; this.stats = stats;
        }
    }

    private HideForge() { }

    static Result run(Context context, Bitmap cover, PayloadCodec.Payload payload,
                      byte[] tokens, char[] passphrase, boolean compress, boolean ghost,
                      HideWork.Progress progress, Runnable guard) throws Exception {
        guard.run();
        SecretInput.ensureRoom(payload.data.length);
        ImageUtils.checkMemory(cover.getWidth(), cover.getHeight(), ghost);
        int[] original = ImageUtils.pixels(cover);
        int[] encoded = VaultEngine.hide(original, payload, tokens, passphrase, compress, ghost,
                (stage, done, total) -> {
                    guard.run();
                    progress.update(HideWork.stageLabel(stage), done, total);
                });
        guard.run();
        progress.update(R.string.hide_stage_render, 0, 0);
        Bitmap bitmap = ImageUtils.fromPixels(encoded, cover.getWidth(), cover.getHeight());
        try {
            return finish(context, cover, bitmap, original, encoded, progress, guard,
                    payload.type, payload.data.length, ghost, passphrase.length > 0);
        } finally { bitmap.recycle(); }
    }

    private static Result finish(Context context, Bitmap cover, Bitmap encoded, int[] original,
                                 int[] pixels, HideWork.Progress progress, Runnable guard,
                                 int type, long bytes, boolean ghost, boolean usesPassphrase) throws IOException {
        File directory = new File(context.getCacheDir(), "shared");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Output storage unavailable");
        File file = new File(directory, "kinetic-" + UUID.randomUUID() + ".png");
        Bitmap before = null, after = null;
        boolean complete = false;
        try {
            progress.update(R.string.hide_stage_save, 0, 0);
            ImageUtils.savePng(encoded, file);
            guard.run();
            ImageStats stats = ImageUtils.statistics(original, pixels);
            before = ImageUtils.thumbnail(cover, 512);
            after = ImageUtils.thumbnail(encoded, 512);
            guard.run();
            VaultLogger.logHide(context, file, encoded, type, bytes, ghost, usesPassphrase);
            complete = true;
            return new Result(file, before, after, stats);
        } finally {
            if (!complete) {
                file.delete();
                if (before != null) before.recycle();
                if (after != null) after.recycle();
            }
        }
    }
}
