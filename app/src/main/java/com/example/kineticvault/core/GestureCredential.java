package com.example.kineticvault.core;

public final class GestureCredential {
    private GestureCredential() { }

    public static char[] canonical(byte[] tokens, char[] passphrase) {
        validate(tokens, passphrase);
        char[] result = new char[9 + tokens.length + passphrase.length * 4];
        char[] prefix = {'K', 'V', 'L', 'T', '1', '|'};
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        result[6] = (char) ('0' + tokens.length);
        result[7] = '|';
        for (int i = 0; i < tokens.length; i++) {
            result[8 + i] = (char) ('0' + tokens[i]);
        }
        result[8 + tokens.length] = '|';
        writePassphrase(result, 9 + tokens.length, passphrase);
        return result;
    }

    private static void validate(byte[] tokens, char[] passphrase) {
        if (tokens == null || tokens.length < Constants.MIN_TOKENS
                || tokens.length > Constants.MAX_TOKENS) {
            throw new IllegalArgumentException("Gesture must have 4 to 8 tokens");
        }
        if (passphrase == null || passphrase.length > Constants.MAX_PASSPHRASE_CHARS) {
            throw new IllegalArgumentException("Invalid passphrase length");
        }
        for (byte token : tokens) {
            if (token < Constants.LEFT || token > Constants.FLAT) {
                throw new IllegalArgumentException("Unknown gesture token");
            }
        }
    }

    private static void writePassphrase(char[] out, int offset, char[] passphrase) {
        // ASCII hex avoids provider differences in PBKDF2 password encoding.
        // Preserve case, spaces and Unicode exactly; never trim a password.
        char[] digits = "0123456789abcdef".toCharArray();
        for (char character : passphrase) {
            for (int shift = 12; shift >= 0; shift -= 4) {
                out[offset++] = digits[(character >>> shift) & 15];
            }
        }
    }

    public static double entropyBits(int tokenCount) {
        if (tokenCount < 0 || tokenCount > Constants.MAX_TOKENS) {
            throw new IllegalArgumentException("Invalid token count");
        }
        // This is an upper bound: human choices are usually less random.
        return tokenCount * Math.log(6.0) / Math.log(2.0);
    }
}
