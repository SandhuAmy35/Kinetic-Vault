package com.example.kineticvault.data;

import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Separate UI disguise credential; never used by the vault encryption format. */
public final class DisguiseCode {
    private DisguiseCode() { }
    public static boolean valid(char[] code) {
        if (code.length < 6 || code.length > 12) return false;
        for (char c : code) if (c < '0' || c > '9') return false;
        return true;
    }
    public static String create(char[] code) throws java.security.GeneralSecurityException {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        byte[] hash = derive(code, salt);
        try { return Base64.encodeToString(salt, Base64.NO_WRAP) + ":"
                + Base64.encodeToString(hash, Base64.NO_WRAP); }
        finally { Arrays.fill(hash, (byte) 0); Arrays.fill(salt, (byte) 0); }
    }
    public static boolean matches(char[] code, String stored) {
        byte[] expected = null, actual = null, salt = null;
        try {
            String[] parts = stored.split(":");
            if (parts.length != 2) return false;
            salt = Base64.decode(parts[0], Base64.NO_WRAP);
            expected = Base64.decode(parts[1], Base64.NO_WRAP);
            if (salt.length != 16 || expected.length != 32) return false;
            actual = derive(code, salt);
            return MessageDigest.isEqual(expected, actual);
        } catch (java.security.GeneralSecurityException | IllegalArgumentException error) { return false; }
        finally {
            if (expected != null) Arrays.fill(expected, (byte) 0);
            if (actual != null) Arrays.fill(actual, (byte) 0);
            if (salt != null) Arrays.fill(salt, (byte) 0);
        }
    }
    private static byte[] derive(char[] code, byte[] salt) throws java.security.GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(code, salt, 120000, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
}
