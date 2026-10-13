package com.example.kineticvault.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class GzipCodec {
    private GzipCodec() { }

    private static final class WipingBuffer extends ByteArrayOutputStream {
        @Override public synchronized void write(byte[] bytes, int offset, int length) {
            byte[] previous = buf;
            super.write(bytes, offset, length);
            if (previous != buf) { Arrays.fill(previous, (byte) 0); }
        }

        @Override public synchronized void write(int value) {
            byte[] previous = buf;
            super.write(value);
            if (previous != buf) { Arrays.fill(previous, (byte) 0); }
        }

        @Override public void close() {
            // Wipe the current backing buffer as well as caller-owned copies.
            Arrays.fill(buf, (byte) 0);
            reset();
        }
    }

    private static final class ConfiguredGzip extends GZIPOutputStream {
        ConfiguredGzip(ByteArrayOutputStream output, boolean compress) throws IOException {
            super(output);
            def.setLevel(compress ? Deflater.DEFAULT_COMPRESSION : Deflater.NO_COMPRESSION);
        }
    }

    public static byte[] compress(byte[] packed, boolean compress) throws IOException {
        if (packed == null || packed.length > Constants.MAX_PACKED_BYTES) {
            throw new IOException("Payload exceeds size limit");
        }
        try (WipingBuffer output = new WipingBuffer()) {
            try (ConfiguredGzip gzip = new ConfiguredGzip(output, compress)) {
                gzip.write(packed);
                gzip.finish();
                return output.toByteArray();
            }
        }
    }

    public static byte[] decompress(byte[] compressed) throws IOException {
        if (compressed == null || compressed.length > Constants.MAX_CIPHER_BYTES) {
            throw new IOException("Invalid compressed payload size");
        }
        byte[] chunk = new byte[8192];
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed));
                WipingBuffer output = new WipingBuffer()) {
            int count;
            while ((count = gzip.read(chunk)) != -1) {
                if (count > Constants.MAX_PACKED_BYTES - output.size()) {
                    throw new IOException("Expanded payload exceeds safety limit");
                }
                output.write(chunk, 0, count);
            }
            return output.toByteArray();
        } finally {
            Arrays.fill(chunk, (byte) 0);
        }
    }
}
