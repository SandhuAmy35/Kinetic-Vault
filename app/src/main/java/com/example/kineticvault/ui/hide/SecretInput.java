package com.example.kineticvault.ui.hide;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import com.example.kineticvault.core.Constants;
import com.example.kineticvault.core.PayloadCodec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Owns a transient edit snapshot. No plaintext is written to disk or a saved Bundle. */
public final class SecretInput implements AutoCloseable {
    public final boolean compress, ghost;
    private final char[] text;
    private final Uri file;

    public SecretInput(char[] text, Uri file, boolean compress, boolean ghost) {
        this.text = text;
        this.file = file;
        this.compress = compress;
        this.ghost = ghost;
    }

    public PayloadCodec.Payload read(ContentResolver resolver) throws IOException {
        byte[] bytes = null;
        try {
            String name = file == null ? "" : filename(resolver);
            bytes = file == null ? encodeText() : readFile(resolver);
            ensureRoom(bytes.length);
            PayloadCodec.Payload payload = new PayloadCodec.Payload(file == null
                    ? PayloadCodec.TEXT : PayloadCodec.FILE, name, bytes);
            bytes = null;
            return payload;
        } finally {
            if (bytes != null) Arrays.fill(bytes, (byte) 0);
        }
    }

    private byte[] encodeText() throws IOException {
        if (text == null || text.length == 0) throw new IOException("Missing secret");
        ensureRoom((long) text.length * 4);
        ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(text));
        try {
            if (encoded.remaining() > Constants.MAX_PAYLOAD_BYTES) throw new IOException("Secret too large");
            byte[] result = new byte[encoded.remaining()];
            encoded.get(result);
            return result;
        } finally {
            if (encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0);
        }
    }

    private byte[] readFile(ContentResolver resolver) throws IOException {
        byte[] chunk = new byte[8192];
        try (InputStream input = resolver.openInputStream(file); WipingBuffer output = new WipingBuffer()) {
            if (input == null) throw new IOException("File unavailable");
            int count;
            while ((count = input.read(chunk)) != -1) {
                if (count > Constants.MAX_PAYLOAD_BYTES - output.size()) throw new IOException("Secret too large");
                ensureRoom((long) output.size() + count);
                output.write(chunk, 0, count);
            }
            return output.toByteArray();
        } finally {
            Arrays.fill(chunk, (byte) 0);
        }
    }

    private String filename(ContentResolver resolver) throws IOException {
        String name = null;
        try (Cursor cursor = resolver.query(file, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
        }
        if (name == null || name.isEmpty()) name = "secret.bin";
        name = name.replaceAll("[\\\\/:\\p{Cntrl}]", "_");
        if (name.equals(".") || name.equals("..")) name = "secret.bin";
        if (name.getBytes(StandardCharsets.UTF_8).length > Constants.MAX_FILENAME_BYTES) {
            throw new IOException("Filename too long");
        }
        return name;
    }

    static void ensureRoom(long bytes) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        long remaining = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
        // Packing, compression and encryption can coexist with the original payload.
        if (bytes * 6 + 24L * 1024 * 1024 > remaining) throw new IOException("Payload exceeds memory budget");
    }

    @Override public void close() { if (text != null) Arrays.fill(text, '\0'); }

    private static final class WipingBuffer extends ByteArrayOutputStream {
        @Override public synchronized void write(byte[] bytes, int offset, int length) {
            byte[] previous = buf;
            super.write(bytes, offset, length);
            if (previous != buf) Arrays.fill(previous, (byte) 0);
        }
        @Override public void close() { Arrays.fill(buf, (byte) 0); reset(); }
    }
}
