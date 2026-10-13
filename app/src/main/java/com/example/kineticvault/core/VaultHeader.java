package com.example.kineticvault.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class VaultHeader {
    private final byte[] encoded;

    private VaultHeader(byte[] encoded) {
        this.encoded = encoded.clone();
    }

    public static VaultHeader create(byte[] salt, byte[] iv, int flags, int length)
            throws IOException {
        if (salt == null || salt.length != Constants.SALT_BYTES
                || iv == null || iv.length != Constants.IV_BYTES) {
            throw new IOException("Invalid salt or IV length");
        }
        ByteBuffer out = ByteBuffer.allocate(Constants.HEADER_BYTES);
        out.order(ByteOrder.BIG_ENDIAN);
        out.put(new byte[]{'K', 'V', 'L', 'T'});
        out.put((byte) Constants.VERSION).put((byte) flags);
        out.put(salt).put(iv).putInt(length);
        if (flags < 0 || flags > 3) {
            throw new IOException("Unsupported header flags");
        }
        return parse(out.array());
    }

    public static VaultHeader parse(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length != Constants.HEADER_BYTES) {
            throw new IOException("Incomplete Kinetic Vault header");
        }
        if (bytes[0] != 'K' || bytes[1] != 'V' || bytes[2] != 'L' || bytes[3] != 'T') {
            throw new IOException("No Kinetic Vault data found");
        }
        if ((bytes[4] & 255) != Constants.VERSION || (bytes[5] & 252) != 0) {
            throw new IOException("Unsupported Kinetic Vault format");
        }
        int length = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt(34);
        if (length < Constants.TAG_BYTES || length > Constants.MAX_CIPHER_BYTES) {
            throw new IOException("Invalid ciphertext length");
        }
        return new VaultHeader(bytes);
    }

    public byte[] bytes() { return encoded.clone(); }
    public byte[] salt() { return Arrays.copyOfRange(encoded, 6, 22); }
    public byte[] iv() { return Arrays.copyOfRange(encoded, 22, 34); }
    public boolean usesPassphrase() { return (encoded[5] & 1) != 0; }
    public boolean isGhost() { return (encoded[5] & 2) != 0; }

    public int ciphertextLength() {
        return ByteBuffer.wrap(encoded).order(ByteOrder.BIG_ENDIAN).getInt(34);
    }
}
