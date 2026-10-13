package com.example.kineticvault.core;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

public final class CryptoCore {
    private CryptoCore() { }

    public static final class Sealed {
        public final VaultHeader header;
        public final byte[] ciphertext;

        private Sealed(VaultHeader header, byte[] ciphertext) {
            this.header = header;
            this.ciphertext = ciphertext;
        }
    }

    public static Sealed encrypt(byte[] compressed, byte[] tokens, char[] passphrase,
            boolean ghost, WorkProgress progress) throws GeneralSecurityException, IOException {
        if (compressed == null || compressed.length > Constants.MAX_CIPHER_BYTES - 16) {
            throw new IOException("Payload exceeds supported image capacity");
        }
        if (passphrase == null) {
            throw new IllegalArgumentException("Passphrase array is required");
        }
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[Constants.SALT_BYTES];
        byte[] iv = new byte[Constants.IV_BYTES];
        random.nextBytes(salt);
        random.nextBytes(iv);
        int flags = passphrase.length > 0 ? Constants.FLAG_PASSPHRASE : 0;
        if (ghost) { flags |= Constants.FLAG_GHOST; }
        VaultHeader header = VaultHeader.create(salt, iv, flags, compressed.length + 16);
        byte[] ciphertext = transform(Cipher.ENCRYPT_MODE, compressed, header,
                tokens, passphrase, progress);
        return new Sealed(header, ciphertext);
    }

    public static byte[] decrypt(VaultHeader header, byte[] ciphertext,
            byte[] tokens, char[] passphrase, WorkProgress progress)
            throws GeneralSecurityException, IOException {
        if (header == null || ciphertext == null
                || ciphertext.length != header.ciphertextLength()) {
            throw new IOException("Incomplete ciphertext");
        }
        // Let AEADBadTagException reach the UI as ACCESS DENIED.
        return transform(Cipher.DECRYPT_MODE, ciphertext, header, tokens, passphrase, progress);
    }

    private static byte[] transform(int mode, byte[] input, VaultHeader header,
            byte[] tokens, char[] passphrase, WorkProgress progress)
            throws GeneralSecurityException {
        WorkProgress.report(progress, WorkProgress.Stage.KEY, 0, 0);
        byte[] keyBytes = derive(tokens, passphrase, header.salt());
        SecretKeySpec key = null;
        try {
            key = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key, new GCMParameterSpec(128, header.iv()));
            // Authenticate public metadata, including the mode and the length.
            cipher.updateAAD(header.bytes());
            WorkProgress.Stage stage = mode == Cipher.ENCRYPT_MODE
                    ? WorkProgress.Stage.ENCRYPT : WorkProgress.Stage.DECRYPT;
            WorkProgress.report(progress, stage, 0, 0);
            return cipher.doFinal(input);
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
            destroy(key);
        }
    }

    private static byte[] derive(byte[] tokens, char[] passphrase, byte[] salt)
            throws GeneralSecurityException {
        char[] password = GestureCredential.canonical(tokens, passphrase);
        PBEKeySpec specification = null;
        SecretKey generated = null;
        try {
            specification = new PBEKeySpec(password, salt,
                    Constants.PBKDF2_ITERATIONS, Constants.KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            generated = factory.generateSecret(specification);
            return generated.getEncoded();
        } finally {
            Arrays.fill(password, '\0');
            if (specification != null) { specification.clearPassword(); }
            destroy(generated);
        }
    }

    private static void destroy(SecretKey key) {
        if (key == null) { return; }
        try {
            key.destroy();
        } catch (DestroyFailedException ignored) {
            // Some providers cannot wipe their internal copies. Do not log secrets.
        }
    }
}
