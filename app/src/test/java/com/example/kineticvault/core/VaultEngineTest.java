package com.example.kineticvault.core;

import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import static org.junit.Assert.*;

public class VaultEngineTest {
    private byte[] gesture() { return new byte[]{0, 0, 4, 5}; }
    private char[] phrase() { return " demo | café 🔐 ".toCharArray(); }

    private int[] cover() {
        int[] pixels = new int[40_000];
        Arrays.fill(pixels, 0xff547898);
        return pixels;
    }

    private int[] hide(boolean ghost, boolean compress) throws Exception {
        byte[] bytes = "Your hand is the key. नमस्ते 🌌".getBytes(StandardCharsets.UTF_8);
        try (PayloadCodec.Payload payload = new PayloadCodec.Payload(PayloadCodec.TEXT, "", bytes)) {
            return VaultEngine.hide(cover(), payload, gesture(), phrase(), compress, ghost, null);
        }
    }

    @Test public void textRoundTripsInBothModesAndCompressionLevels() throws Exception {
        for (boolean ghost : new boolean[]{false, true}) {
            for (boolean compress : new boolean[]{false, true}) {
                int[] image = hide(ghost, compress);
                try (PayloadCodec.Payload result = VaultEngine.reveal(image, gesture(), phrase(), null)) {
                    assertEquals(PayloadCodec.TEXT, result.type);
                    assertEquals("", result.filename);
                    assertEquals("Your hand is the key. नमस्ते 🌌",
                            new String(result.data, StandardCharsets.UTF_8));
                }
            }
        }
    }

    @Test public void arbitraryFileBytesRoundTrip() throws Exception {
        byte[] bytes = new byte[1024];
        for (int i = 0; i < bytes.length; i++) { bytes[i] = (byte) i; }
        try (PayloadCodec.Payload payload = new PayloadCodec.Payload(PayloadCodec.FILE, "résumé.bin", bytes)) {
            int[] image = VaultEngine.hide(cover(), payload, gesture(), new char[0], true, true, null);
            assertFalse(StegoCore.readHeader(image).usesPassphrase());
            try (PayloadCodec.Payload result = VaultEngine.reveal(image, gesture(), new char[0], null)) {
                assertEquals(PayloadCodec.FILE, result.type);
                assertEquals(payload.filename, result.filename);
                assertArrayEquals(bytes, result.data);
            }
        }
    }

    @Test public void wrongGestureFailsAuthentication() throws Exception {
        int[] image = hide(false, true);
        assertThrows(AEADBadTagException.class,
                () -> VaultEngine.reveal(image, new byte[]{1, 0, 4, 5}, phrase(), null));
    }

    @Test public void wrongPassphraseFailsAuthentication() throws Exception {
        int[] image = hide(true, true);
        assertThrows(AEADBadTagException.class,
                () -> VaultEngine.reveal(image, gesture(), "wrong".toCharArray(), null));
    }

    @Test public void passphraseWhitespaceIsSignificant() throws Exception {
        int[] image = hide(false, true);
        assertThrows(AEADBadTagException.class,
                () -> VaultEngine.reveal(image, gesture(), "demo | café 🔐".toCharArray(), null));
    }

    @Test public void ciphertextBitFlipFailsAuthentication() throws Exception {
        int[] image = hide(false, true);
        image[Constants.HEADER_PIXELS] ^= 1 << 16;
        assertThrows(AEADBadTagException.class,
                () -> VaultEngine.reveal(image, gesture(), phrase(), null));
    }

    @Test public void changedPassphraseFlagIsAuthenticated() throws Exception {
        int[] image = hide(false, true);
        flipHeaderBit(image, 5 * 8 + 7);
        assertFalse(StegoCore.readHeader(image).usesPassphrase());
        assertThrows(AEADBadTagException.class,
                () -> VaultEngine.reveal(image, gesture(), phrase(), null));
    }

    @Test public void changedIvIsAuthenticated() throws Exception {
        int[] image = hide(false, true);
        flipHeaderBit(image, 22 * 8);
        assertThrows(AEADBadTagException.class,
                () -> VaultEngine.reveal(image, gesture(), phrase(), null));
    }

    @Test public void changedSaltIsAuthenticated() throws Exception {
        int[] image = hide(false, true);
        flipHeaderBit(image, 6 * 8);
        assertThrows(AEADBadTagException.class,
                () -> VaultEngine.reveal(image, gesture(), phrase(), null));
    }

    @Test public void repeatedEncryptionUsesFreshSaltAndIv() throws Exception {
        int[] first = hide(false, true);
        int[] second = hide(false, true);
        assertFalse(Arrays.equals(StegoCore.readHeader(first).salt(), StegoCore.readHeader(second).salt()));
        assertFalse(Arrays.equals(StegoCore.readHeader(first).iv(), StegoCore.readHeader(second).iv()));
        assertFalse(Arrays.equals(first, second));
    }

    @Test public void oversizedSecretStopsBeforeKeyDerivation() throws Exception {
        boolean[] derived = {false};
        WorkProgress progress = (stage, done, total) -> {
            if (stage == WorkProgress.Stage.KEY) { derived[0] = true; }
        };
        int[] tiny = new int[150];
        Arrays.fill(tiny, 0xffeeeeee);
        try (PayloadCodec.Payload payload = new PayloadCodec.Payload(PayloadCodec.TEXT, "", new byte[100])) {
            assertThrows(IOException.class,
                    () -> VaultEngine.hide(tiny, payload, gesture(), phrase(), false, false, progress));
        }
        assertFalse(derived[0]);
    }

    @Test public void progressFinishesAtActualBitCount() throws Exception {
        int[] last = {0, 0};
        WorkProgress progress = (stage, done, total) -> {
            if (stage == WorkProgress.Stage.EMBED) { last[0] = done; last[1] = total; }
        };
        try (PayloadCodec.Payload payload = new PayloadCodec.Payload(PayloadCodec.TEXT, "", new byte[0])) {
            int[] image = VaultEngine.hide(cover(), payload, gesture(), new char[0], true, true, progress);
            assertEquals(StegoCore.readHeader(image).ciphertextLength() * 8, last[0]);
            assertEquals(last[0], last[1]);
        }
    }

    @Test public void canonicalEncodingIsStableAndUnambiguous() {
        assertArrayEquals("KVLT1|4|0045|0041007c00e9".toCharArray(),
                GestureCredential.canonical(gesture(), "A|é".toCharArray()));
        assertFalse(Arrays.equals(GestureCredential.canonical(gesture(), new char[0]),
                GestureCredential.canonical(gesture(), " ".toCharArray())));
        assertEquals(10.33985, GestureCredential.entropyBits(4), 0.00001);
    }

    @Test public void invalidGestureTokensAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> GestureCredential.canonical(new byte[]{0, 1, 2}, new char[0]));
        assertThrows(IllegalArgumentException.class,
                () -> GestureCredential.canonical(new byte[]{0, 1, 2, 6}, new char[0]));
        assertThrows(IllegalArgumentException.class,
                () -> GestureCredential.canonical(new byte[]{0, 1, 2, -1}, new char[0]));
        assertThrows(IllegalArgumentException.class,
                () -> GestureCredential.canonical(gesture(), new char[1025]));
    }

    @Test public void payloadCloseWipesOwnedPlaintext() throws Exception {
        byte[] data = {10, 20, 30};
        PayloadCodec.Payload payload = new PayloadCodec.Payload(PayloadCodec.FILE, "a.bin", data);
        payload.close();
        assertArrayEquals(new byte[3], data);
    }

    @Test public void decryptsIndependentNodeCryptoFixture() throws Exception {
        // Produced with Node crypto, not with CryptoCore.encrypt.
        byte[] header = hex("4b564c5401010102030405060708090a0b0c0d0e0f1015161718191a1b1c1d1e1f2000000018");
        byte[] cipher = hex("b2a56b97c30cc438c9e57ff141ef4de4804885e37f8f073a");
        byte[] plain = CryptoCore.decrypt(VaultHeader.parse(header), cipher,
                gesture(), "A|é".toCharArray(), null);
        assertArrayEquals("portable".getBytes(StandardCharsets.UTF_8), plain);
        Arrays.fill(plain, (byte) 0);
    }

    private byte[] hex(String text) {
        byte[] result = new byte[text.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(text.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    private void flipHeaderBit(int[] image, int bit) {
        image[bit / 3] ^= 1 << (16 - (bit % 3) * 8);
    }
}
