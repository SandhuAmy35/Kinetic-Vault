package com.example.kineticvault.image;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

final class PngFixtures {
    private PngFixtures() { }

    static byte[] opaque(int width, int height) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(result);
        output.write(new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10});
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        DataOutputStream header = new DataOutputStream(headerBytes);
        header.writeInt(width);
        header.writeInt(height);
        header.write(new byte[]{8, 2, 0, 0, 0});
        chunk(output, "IHDR", headerBytes.toByteArray());
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        // Generate a large test PNG without allocating a large bitmap first.
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(compressed)) {
            byte[] row = new byte[1 + width * 3];
            for (int y = 0; y < height; y++) { deflater.write(row); }
        }
        chunk(output, "IDAT", compressed.toByteArray());
        chunk(output, "IEND", new byte[0]);
        return result.toByteArray();
    }

    private static void chunk(DataOutputStream output, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        output.writeInt(data.length);
        output.write(typeBytes);
        output.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        output.writeInt((int) crc.getValue());
    }
}
