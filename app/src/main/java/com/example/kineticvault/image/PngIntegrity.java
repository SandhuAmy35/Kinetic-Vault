package com.example.kineticvault.image;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.zip.CRC32;

/** Bitmap decoders may return partial pixels; reject broken PNG framing before decoding. */
final class PngIntegrity {
    private static final int[] SIGNATURE = {137, 80, 78, 71, 13, 10, 26, 10};
    private static final int IHDR = 0x49484452, IDAT = 0x49444154, IEND = 0x49454e44;

    private PngIntegrity() { }

    static void read(ImageUtils.Source source) throws IOException {
        try (InputStream opened = source.open()) {
            if (opened == null) throw new IOException("Image is unavailable");
            DataInputStream input = new DataInputStream(new BufferedInputStream(opened));
            for (int expected : SIGNATURE) {
                int value = input.read();
                if (value < 0) throw new IOException("Truncated image signature");
                if (value != expected) return;
            }
            chunks(input);
        }
    }

    private static void chunks(DataInputStream input) throws IOException {
        byte[] buffer = new byte[8192];
        boolean first = true, imageData = false;
        while (true) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            int length = input.readInt(), type = input.readInt();
            if (length < 0 || (first && (type != IHDR || length != 13))
                    || (!first && type == IHDR) || (type == IEND && length != 0))
                throw new IOException("Invalid PNG chunk");
            first = false;
            checksum(input, type, length, buffer);
            if (type == IDAT) imageData = true;
            if (type == IEND) {
                if (!imageData || input.read() != -1) throw new IOException("Invalid PNG ending");
                return;
            }
        }
    }

    private static void checksum(DataInputStream input, int type, int length, byte[] buffer)
            throws IOException {
        CRC32 crc = new CRC32();
        for (int shift = 24; shift >= 0; shift -= 8) crc.update((type >>> shift) & 255);
        while (length > 0) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException();
            int count = Math.min(length, buffer.length);
            input.readFully(buffer, 0, count);
            crc.update(buffer, 0, count);
            length -= count;
        }
        if (crc.getValue() != (input.readInt() & 0xffffffffL))
            throw new IOException("Damaged PNG chunk");
    }
}
