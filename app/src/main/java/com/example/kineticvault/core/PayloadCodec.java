package com.example.kineticvault.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class PayloadCodec {
    public static final byte TEXT = 1;
    public static final byte FILE = 2;

    private PayloadCodec() { }

    public static final class Payload implements AutoCloseable {
        public final byte type;
        public final String filename;
        public final byte[] data;

        public Payload(byte type, String filename, byte[] data) throws IOException {
            validate(type, filename, data);
            this.type = type;
            this.filename = filename;
            // Ownership transfers to this object, avoiding another plaintext copy.
            this.data = data;
        }

        @Override public void close() { Arrays.fill(data, (byte) 0); }
    }

    public static byte[] pack(Payload payload) throws IOException {
        if (payload == null) { throw new IOException("Missing payload"); }
        validate(payload.type, payload.filename, payload.data);
        byte[] name = payload.filename.getBytes(StandardCharsets.UTF_8);
        ByteBuffer out = ByteBuffer.allocate(7 + name.length + payload.data.length);
        out.order(ByteOrder.BIG_ENDIAN);
        out.put(payload.type).putShort((short) name.length);
        out.put(name).putInt(payload.data.length).put(payload.data);
        return out.array();
    }

    public static Payload unpack(byte[] packed) throws IOException {
        if (packed == null || packed.length < 7 || packed.length > Constants.MAX_PACKED_BYTES) {
            throw new IOException("Invalid payload size");
        }
        ByteBuffer input = ByteBuffer.wrap(packed).order(ByteOrder.BIG_ENDIAN);
        byte type = input.get();
        int nameLength = input.getShort() & 65535;
        if (nameLength > Constants.MAX_FILENAME_BYTES || nameLength > input.remaining() - 4) {
            throw new IOException("Invalid filename length");
        }
        byte[] name = new byte[nameLength];
        input.get(name);
        String filename = decodeName(name);
        int length = input.getInt();
        if (length < 0 || length > Constants.MAX_PAYLOAD_BYTES || length != input.remaining()) {
            throw new IOException("Invalid payload length");
        }
        validateName(type, filename);
        byte[] data = new byte[length];
        input.get(data);
        return new Payload(type, filename, data);
    }

    private static void validate(byte type, String filename, byte[] data) throws IOException {
        validateName(type, filename);
        if (data == null || data.length > Constants.MAX_PAYLOAD_BYTES) {
            throw new IOException("Secret is too large");
        }
    }

    private static void validateName(byte type, String filename) throws IOException {
        if ((type != TEXT && type != FILE) || filename == null) {
            throw new IOException("Unknown payload type or missing filename");
        }
        if ((type == TEXT && !filename.isEmpty()) || (type == FILE && filename.isEmpty())) {
            throw new IOException("Invalid filename for payload type");
        }
        if (filename.getBytes(StandardCharsets.UTF_8).length > Constants.MAX_FILENAME_BYTES
                || filename.equals(".") || filename.equals("..")) {
            throw new IOException("Invalid filename");
        }
        for (int i = 0; i < filename.length(); i++) {
            char character = filename.charAt(i);
            if (character < 32 || character == 127 || character == '/' || character == '\\'
                    || character == ':') {
                throw new IOException("Filename must be a basename");
            }
        }
        // Reject malformed Unicode instead of silently replacing characters.
        StandardCharsets.UTF_8.newEncoder().encode(java.nio.CharBuffer.wrap(filename));
    }

    private static String decodeName(byte[] name) throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(name)).toString();
    }
}
