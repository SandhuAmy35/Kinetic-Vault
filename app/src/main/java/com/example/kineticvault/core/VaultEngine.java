package com.example.kineticvault.core;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public final class VaultEngine {
    private VaultEngine() { }

    // All methods are synchronous. The Android UI must call them on its executor.
    // The caller owns payload, tokens and passphrase and wipes them after use.
    public static int[] hide(int[] cover, PayloadCodec.Payload payload,
            byte[] tokens, char[] passphrase, boolean compress, boolean ghost,
            WorkProgress progress) throws IOException, GeneralSecurityException {
        byte[] packed = null;
        byte[] compressed = null;
        try {
            WorkProgress.report(progress, WorkProgress.Stage.PACK, 0, 0);
            packed = PayloadCodec.pack(payload);
            WorkProgress.report(progress, WorkProgress.Stage.COMPRESS, 0, 0);
            compressed = GzipCodec.compress(packed, compress);
            if (cover == null || cover.length > Constants.MAX_PIXELS
                    || compressed.length + Constants.TAG_BYTES > StegoCore.capacityBytes(cover.length)) {
                throw new IOException("Choose a larger cover or a smaller secret");
            }
            CryptoCore.Sealed sealed = CryptoCore.encrypt(compressed, tokens, passphrase,
                    ghost, progress);
            return StegoCore.embed(cover, sealed.header, sealed.ciphertext, progress);
        } finally {
            wipe(packed);
            wipe(compressed);
        }
    }

    public static PayloadCodec.Payload reveal(int[] pixels, byte[] tokens,
            char[] passphrase, WorkProgress progress) throws IOException, GeneralSecurityException {
        byte[] compressed = null;
        byte[] packed = null;
        try {
            VaultHeader header = StegoCore.readHeader(pixels);
            byte[] ciphertext = StegoCore.extract(pixels, progress);
            compressed = CryptoCore.decrypt(header, ciphertext, tokens, passphrase, progress);
            WorkProgress.report(progress, WorkProgress.Stage.UNPACK, 0, 0);
            packed = GzipCodec.decompress(compressed);
            return PayloadCodec.unpack(packed);
        } finally {
            wipe(compressed);
            wipe(packed);
        }
    }

    private static void wipe(byte[] bytes) {
        if (bytes != null) { Arrays.fill(bytes, (byte) 0); }
    }
}
