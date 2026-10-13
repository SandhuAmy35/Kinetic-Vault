package com.example.kineticvault.core;

import java.io.IOException;
import java.util.Random;

public final class StegoCore {
    private StegoCore() { }

    public static int capacityBytes(int pixelCount) {
        if (pixelCount < Constants.HEADER_PIXELS) { return 0; }
        // Two unused header bits cannot hold payload starting at pixel 102.
        long bits = (pixelCount - (long) Constants.HEADER_PIXELS) * 3;
        return (int) (bits / 8);
    }

    public static VaultHeader readHeader(int[] pixels) throws IOException {
        validatePixels(pixels);
        byte[] bytes = readBytes(pixels, Constants.HEADER_BYTES, 0, null, null);
        VaultHeader header = VaultHeader.parse(bytes);
        if (header.ciphertextLength() > capacityBytes(pixels.length)) {
            throw new IOException("Ciphertext length exceeds image capacity");
        }
        return header;
    }

    public static int[] embed(int[] cover, VaultHeader header, byte[] ciphertext,
            WorkProgress progress) throws IOException {
        validatePixels(cover);
        if (header == null || ciphertext == null
                || ciphertext.length != header.ciphertextLength()) {
            throw new IOException("Header and ciphertext lengths differ");
        }
        if (ciphertext.length > capacityBytes(cover.length)) {
            throw new IOException("Choose a larger cover or a smaller secret");
        }
        int[] order = pixelOrder(cover.length, header);
        int[] result = cover.clone();
        writeBytes(result, header.bytes(), 0, null, null);
        writeBytes(result, ciphertext, Constants.HEADER_PIXELS, order, progress);
        return result;
    }

    public static byte[] extract(int[] pixels, WorkProgress progress) throws IOException {
        VaultHeader header = readHeader(pixels);
        int[] order = pixelOrder(pixels.length, header);
        return readBytes(pixels, header.ciphertextLength(),
                Constants.HEADER_PIXELS, order, progress);
    }

    private static void validatePixels(int[] pixels) throws IOException {
        if (pixels == null || pixels.length < Constants.HEADER_PIXELS
                || pixels.length > Constants.MAX_PIXELS) {
            throw new IOException("Unsupported image dimensions");
        }
        for (int pixel : pixels) {
            if ((pixel >>> 24) != 255) {
                throw new IOException("Image must be opaque ARGB pixels");
            }
        }
    }

    private static int[] pixelOrder(int count, VaultHeader header) {
        if (!header.isGhost()) { return null; }
        int[] order = new int[count - Constants.HEADER_PIXELS];
        for (int i = 0; i < order.length; i++) {
            order[i] = Constants.HEADER_PIXELS + i;
        }
        // Public salt gives repeatable scattering, not additional encryption.
        long seed = 0;
        for (byte value : header.salt()) {
            seed = seed * 31 + (value & 255);
        }
        Random random = new Random(seed);
        for (int i = order.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temporary = order[i];
            order[i] = order[j];
            order[j] = temporary;
        }
        return order;
    }

    private static int pixelIndex(int bitIndex, int startPixel, int[] order) {
        int position = bitIndex / 3;
        return order == null ? startPixel + position : order[position];
    }

    private static void writeBytes(int[] pixels, byte[] bytes, int startPixel,
            int[] order, WorkProgress progress) {
        int total = bytes.length * 8;
        for (int bit = 0; bit < total; bit++) {
            int index = pixelIndex(bit, startPixel, order);
            int shift = 16 - (bit % 3) * 8;
            int value = (bytes[bit / 8] >>> (7 - bit % 8)) & 1;
            pixels[index] = (pixels[index] & ~(1 << shift)) | (value << shift);
            if ((bit & 65535) == 0) {
                WorkProgress.report(progress, WorkProgress.Stage.EMBED, bit, total);
            }
        }
        WorkProgress.report(progress, WorkProgress.Stage.EMBED, total, total);
    }

    private static byte[] readBytes(int[] pixels, int count, int startPixel,
            int[] order, WorkProgress progress) {
        byte[] bytes = new byte[count];
        int total = count * 8;
        for (int bit = 0; bit < total; bit++) {
            int index = pixelIndex(bit, startPixel, order);
            int shift = 16 - (bit % 3) * 8;
            int value = (pixels[index] >>> shift) & 1;
            bytes[bit / 8] |= (byte) (value << (7 - bit % 8));
            if ((bit & 65535) == 0) {
                WorkProgress.report(progress, WorkProgress.Stage.EXTRACT, bit, total);
            }
        }
        WorkProgress.report(progress, WorkProgress.Stage.EXTRACT, total, total);
        return bytes;
    }
}
