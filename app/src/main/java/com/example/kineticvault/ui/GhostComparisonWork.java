package com.example.kineticvault.ui;

import android.graphics.Bitmap;
import com.example.kineticvault.core.PayloadCodec;
import com.example.kineticvault.core.VaultEngine;
import com.example.kineticvault.core.WorkProgress;
import com.example.kineticvault.image.CoverGenerator;
import com.example.kineticvault.image.ImageUtils;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.CancellationException;

final class GhostComparisonWork {
    static final class Pair implements AutoCloseable {
        final DiffMapData standard, ghost;
        Pair(DiffMapData standard, DiffMapData ghost) { this.standard = standard; this.ghost = ghost; }
        @Override public void close() { standard.close(); ghost.close(); }
    }
    private GhostComparisonWork() { }

    static Pair create(String publicText) throws Exception {
        ImageUtils.requireWorker();
        Bitmap cover = CoverGenerator.generate(320, 320, System.nanoTime());
        byte[] tokens = new byte[8];
        char[] phrase = new char[32];
        DiffMapData standard = null, ghost = null;
        byte[] text = publicText.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[2048];
        try {
            SecureRandom random = new SecureRandom();
            for (int i = 0; i < tokens.length; i++) tokens[i] = (byte) random.nextInt(6);
            for (int i = 0; i < phrase.length; i++) phrase[i] = (char) ('a' + random.nextInt(26));
            for (int i = 0; i < bytes.length; i++) bytes[i] = text[i % text.length];
            int[] original = ImageUtils.pixels(cover);
            WorkProgress progress = (stage, done, total) -> guard();
            try (PayloadCodec.Payload payload = new PayloadCodec.Payload(PayloadCodec.TEXT, "", bytes)) {
                standard = encode(original, payload, tokens, phrase, false, progress);
                ghost = encode(original, payload, tokens, phrase, true, progress);
                guard(); return new Pair(standard, ghost);
            }
        } catch (Exception | OutOfMemoryError failure) {
            if (standard != null) standard.close();
            if (ghost != null) ghost.close();
            throw failure;
        } finally {
            cover.recycle(); Arrays.fill(tokens, (byte) 0); Arrays.fill(phrase, '\0');
            Arrays.fill(text, (byte) 0); Arrays.fill(bytes, (byte) 0);
        }
    }

    private static DiffMapData encode(int[] original, PayloadCodec.Payload payload,
                                      byte[] tokens, char[] phrase, boolean ghost,
                                      WorkProgress progress) throws Exception {
        guard();
        int[] encoded = VaultEngine.hide(original, payload, tokens, phrase, false, ghost, progress);
        return DiffMapData.build(original.clone(), encoded, 320, 320);
    }

    private static void guard() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
    }
}
