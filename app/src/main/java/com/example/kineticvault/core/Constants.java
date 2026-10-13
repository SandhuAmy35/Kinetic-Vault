package com.example.kineticvault.core;

public final class Constants {
    private Constants() { }

    // Version 1 fixes these values because its header has no KDF settings.
    public static final int VERSION = 1;
    public static final int HEADER_BYTES = 38;
    public static final int HEADER_PIXELS = 102;
    public static final int SALT_BYTES = 16;
    public static final int IV_BYTES = 12;
    public static final int TAG_BYTES = 16;
    public static final int KEY_BITS = 256;
    public static final int PBKDF2_ITERATIONS = 120_000;
    public static final int FLAG_PASSPHRASE = 1;
    public static final int FLAG_GHOST = 2;

    // Bound allocations before reading untrusted images or expanding GZIP.
    public static final int MAX_PIXELS = 9_000_000;
    public static final int MAX_PAYLOAD_BYTES = 16 * 1024 * 1024;
    public static final int MAX_FILENAME_BYTES = 240;
    public static final int MAX_PACKED_BYTES = MAX_PAYLOAD_BYTES + 247;
    public static final int MAX_CIPHER_BYTES =
            (MAX_PIXELS - HEADER_PIXELS) * 3 / 8;
    public static final int MAX_PASSPHRASE_CHARS = 1024;
    public static final int COVER_LONG_SIDE = 3000;

    // These numeric token IDs are permanent parts of the version 1 format.
    public static final byte LEFT = 0;
    public static final byte RIGHT = 1;
    public static final byte FORWARD = 2;
    public static final byte BACK = 3;
    public static final byte SNAP = 4;
    public static final byte FLAT = 5;
    public static final int MIN_TOKENS = 4;
    public static final int MAX_TOKENS = 8;

    // Lower entry angles make tilts easier. Exit must stay below entry.
    public static final float FILTER_ALPHA = 0.98f;
    public static final float TILT_ENTER_DEGREES = 25f;
    public static final float TILT_EXIT_DEGREES = 12f;
    public static final float FLAT_DEGREES = 8f;
    public static final long MOVE_GAP_MS = 300;
    public static final long FLAT_HOLD_MS = 500;
    public static final long STILL_FINISH_MS = 1500;
    // SNAP uses gravity-removed acceleration along the face-up Z axis.
    public static final float SNAP_DOWN_METERS_PER_SECOND_SQUARED = -7f;
    public static final long SNAP_MAX_MS = 220;
    public static final float STILL_ACCEL_TOLERANCE = 0.8f;
    public static final float STILL_GYRO_RADIANS_PER_SECOND = 0.15f;
}
