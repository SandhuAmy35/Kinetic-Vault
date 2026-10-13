package com.example.kineticvault.core;

import org.junit.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import static org.junit.Assert.*;

public class StegoCoreTest {
    private int[] cover(int count) {
        int[] result = new int[count];
        Arrays.fill(result, 0xff222222);
        return result;
    }

    private VaultHeader header(int flags, int length) throws IOException {
        return VaultHeader.create(new byte[16], new byte[12], flags, length);
    }

    @Test public void headerHasExactLayoutAndSize() throws Exception {
        byte[] salt = new byte[16];
        byte[] iv = new byte[12];
        Arrays.fill(salt, (byte) 0x56);
        Arrays.fill(iv, (byte) 0x78);
        byte[] bytes = VaultHeader.create(salt, iv, 3, 0x1234).bytes();
        assertEquals(38, bytes.length);
        assertArrayEquals(new byte[]{'K', 'V', 'L', 'T', 1, 3}, Arrays.copyOf(bytes, 6));
        assertArrayEquals(salt, Arrays.copyOfRange(bytes, 6, 22));
        assertArrayEquals(iv, Arrays.copyOfRange(bytes, 22, 34));
        assertEquals(0x1234, ByteBuffer.wrap(bytes).getInt(34));
    }

    @Test public void exactCapacityWorksForEveryPixelRemainder() throws Exception {
        for (int count = 200; count < 208; count++) {
            final int pixelCount = count;
            int capacity = StegoCore.capacityBytes(count);
            byte[] bytes = new byte[capacity];
            Arrays.fill(bytes, (byte) 0xab);
            for (int flags : new int[]{0, 2}) {
                int[] image = StegoCore.embed(cover(count), header(flags, capacity), bytes, null);
                assertArrayEquals(bytes, StegoCore.extract(image, null));
                assertThrows(IOException.class, () -> StegoCore.embed(cover(pixelCount),
                        header(flags, capacity + 1), new byte[capacity + 1], null));
            }
        }
    }

    @Test public void capacityAccountsForBothUnusedHeaderBits() {
        assertEquals(0, StegoCore.capacityBytes(101));
        assertEquals(0, StegoCore.capacityBytes(102));
        assertEquals(0, StegoCore.capacityBytes(104));
        assertEquals(1, StegoCore.capacityBytes(105));
        assertEquals(36, StegoCore.capacityBytes(200));
        assertEquals(3_374_961, StegoCore.capacityBytes(9_000_000));
    }

    @Test public void standardAndGhostBothRoundTripWithDifferentMaps() throws Exception {
        byte[] bytes = new byte[128];
        Arrays.fill(bytes, (byte) 0xff);
        int[] plain = cover(2000);
        int[] standard = StegoCore.embed(plain, header(0, bytes.length), bytes, null);
        int[] ghost = StegoCore.embed(plain, header(2, bytes.length), bytes, null);
        assertArrayEquals(bytes, StegoCore.extract(standard, null));
        assertArrayEquals(bytes, StegoCore.extract(ghost, null));
        assertFalse(Arrays.equals(standard, ghost));
        boolean scattered = false;
        int sequentialEnd = 102 + (bytes.length * 8 + 2) / 3;
        for (int i = sequentialEnd; i < plain.length; i++) {
            assertEquals(plain[i], standard[i]);
            if (plain[i] != ghost[i]) { scattered = true; }
        }
        assertTrue(scattered);
    }

    @Test public void ghostShuffleIsDeterministic() throws Exception {
        byte[] bytes = new byte[128];
        Arrays.fill(bytes, (byte) 0x95);
        VaultHeader header = header(2, bytes.length);
        assertArrayEquals(StegoCore.embed(cover(2000), header, bytes, null),
                StegoCore.embed(cover(2000), header, bytes, null));
    }

    @Test public void onlyRgbLeastSignificantBitsChangeAndCoverIsPreserved() throws Exception {
        int[] original = cover(2000);
        int[] copy = original.clone();
        int[] image = StegoCore.embed(original, header(2, 128), new byte[128], null);
        assertArrayEquals(copy, original);
        for (int i = 0; i < image.length; i++) {
            assertEquals(0, (original[i] ^ image[i]) & ~0x00010101);
            assertEquals(255, image[i] >>> 24);
        }
        assertEquals(original[101] & 0x000101, image[101] & 0x000101);
    }

    @Test public void absentSignatureAndTinyImagesAreRejected() {
        assertThrows(IOException.class, () -> StegoCore.readHeader(cover(2000)));
        assertThrows(IOException.class, () -> StegoCore.readHeader(cover(101)));
        assertThrows(IOException.class, () -> StegoCore.readHeader(null));
    }

    @Test public void transparencyIsRejected() throws Exception {
        int[] pixels = cover(2000);
        pixels[500] = 0x80222222;
        assertThrows(IOException.class,
                () -> StegoCore.embed(pixels, header(0, 16), new byte[16], null));
    }

    @Test public void corruptedMagicVersionAndFlagsAreRejected() throws Exception {
        byte[] valid = header(0, 16).bytes();
        for (int offset : new int[]{0, 4, 5}) {
            byte[] broken = valid.clone();
            broken[offset] = (byte) 0xff;
            assertThrows(IOException.class, () -> VaultHeader.parse(broken));
        }
    }

    @Test public void maliciousLengthsAreRejectedBeforeAllocation() throws Exception {
        for (int length : new int[]{-1, 0, 15, Integer.MAX_VALUE}) {
            byte[] broken = header(0, 16).bytes();
            ByteBuffer.wrap(broken).putInt(34, length);
            assertThrows(IOException.class, () -> VaultHeader.parse(broken));
        }
        int[] image = StegoCore.embed(cover(2000), header(0, 600), new byte[600], null);
        int[] cropped = Arrays.copyOf(image, 1000);
        assertThrows(IOException.class, () -> StegoCore.extract(cropped, null));
    }

    @Test public void headerCopiesCannotMutateItsContents() throws Exception {
        VaultHeader header = header(2, 16);
        byte[] bytes = header.bytes();
        bytes[0] = 0;
        byte[] salt = header.salt();
        salt[0] = 1;
        assertEquals('K', header.bytes()[0]);
        assertEquals(0, header.salt()[0]);
    }
}
