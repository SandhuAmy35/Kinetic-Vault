package com.example.kineticvault.core;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.GZIPOutputStream;
import static org.junit.Assert.*;

public class PayloadCodecTest {
    @Test public void emptyTextAndFileAreSupported() throws Exception {
        for (byte type : new byte[]{PayloadCodec.TEXT, PayloadCodec.FILE}) {
            String name = type == PayloadCodec.TEXT ? "" : "empty.bin";
            try (PayloadCodec.Payload payload = new PayloadCodec.Payload(type, name, new byte[0]);
                    PayloadCodec.Payload decoded = PayloadCodec.unpack(PayloadCodec.pack(payload))) {
                assertEquals(type, decoded.type);
                assertEquals(name, decoded.filename);
                assertEquals(0, decoded.data.length);
            }
        }
    }

    @Test public void filenameTraversalAndControlCharactersAreRejected() {
        for (String name : new String[]{"../secret", "a/b", "a\\b", "..", "a:b", "a\nb", ""}) {
            assertThrows(IOException.class,
                    () -> new PayloadCodec.Payload(PayloadCodec.FILE, name, new byte[0]));
        }
    }

    @Test public void unknownTypeAndLongNameAreRejected() {
        assertThrows(IOException.class, () -> new PayloadCodec.Payload((byte) 9, "", new byte[0]));
        char[] name = new char[241];
        Arrays.fill(name, 'a');
        assertThrows(IOException.class,
                () -> new PayloadCodec.Payload(PayloadCodec.FILE, new String(name), new byte[0]));
    }

    @Test public void packedLengthsMustMatchExactly() throws Exception {
        try (PayloadCodec.Payload payload = new PayloadCodec.Payload(PayloadCodec.TEXT, "", new byte[3])) {
            byte[] packed = PayloadCodec.pack(payload);
            byte[] extra = Arrays.copyOf(packed, packed.length + 1);
            assertThrows(IOException.class, () -> PayloadCodec.unpack(extra));
            ByteBuffer.wrap(packed).putInt(3, Integer.MAX_VALUE);
            assertThrows(IOException.class, () -> PayloadCodec.unpack(packed));
        }
    }

    @Test public void truncatedNameAndInvalidUtf8AreRejected() {
        assertThrows(IOException.class, () -> PayloadCodec.unpack(new byte[]{2, 0, 5, 1, 2, 3, 4}));
        assertThrows(IOException.class,
                () -> PayloadCodec.unpack(new byte[]{2, 0, 1, (byte) 0xff, 0, 0, 0, 0}));
    }

    @Test public void bothCompressionSettingsHaveGzipEnvelope() throws Exception {
        byte[] packed = new byte[1000];
        for (boolean enabled : new boolean[]{false, true}) {
            byte[] zipped = GzipCodec.compress(packed, enabled);
            assertEquals(0x1f, zipped[0] & 255);
            assertEquals(0x8b, zipped[1] & 255);
            assertArrayEquals(packed, GzipCodec.decompress(zipped));
        }
        assertTrue(GzipCodec.compress(packed, true).length < GzipCodec.compress(packed, false).length);
    }

    @Test public void corruptGzipIsRejected() throws Exception {
        byte[] zipped = GzipCodec.compress(new byte[100], true);
        zipped[zipped.length - 8] ^= 1;
        assertThrows(IOException.class, () -> GzipCodec.decompress(zipped));
        assertThrows(IOException.class, () -> GzipCodec.decompress(new byte[0]));
    }

    @Test public void gzipBombStopsAtExpansionLimit() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
            byte[] chunk = new byte[8192];
            int remaining = Constants.MAX_PACKED_BYTES + 1;
            while (remaining > 0) {
                int count = Math.min(remaining, chunk.length);
                gzip.write(chunk, 0, count);
                remaining -= count;
            }
        }
        assertThrows(IOException.class, () -> GzipCodec.decompress(buffer.toByteArray()));
    }
}
