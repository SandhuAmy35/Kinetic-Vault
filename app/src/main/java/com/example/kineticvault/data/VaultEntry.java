package com.example.kineticvault.data;

import android.database.Cursor;

/** Metadata only; neither credentials nor decrypted content belongs here. */
public final class VaultEntry {
    public String uuid, title, imageUri, imageSha256;
    public long id, payloadBytes, capacityBytes, createdAt;
    public Long expiresAt;
    public int payloadType, syncState;
    public boolean ghost, usesPassphrase, deleted;
    public Double lat, lng;
    public byte[] thumb;

    public VaultEntry() { }

    static VaultEntry read(Cursor cursor) {
        VaultEntry entry = new VaultEntry();
        entry.id = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
        entry.uuid = string(cursor, "uuid"); entry.title = string(cursor, "title");
        entry.imageUri = string(cursor, "image_uri"); entry.imageSha256 = string(cursor, "image_sha256");
        entry.payloadType = (int) number(cursor, "payload_type");
        entry.payloadBytes = number(cursor, "payload_bytes"); entry.capacityBytes = number(cursor, "capacity_bytes");
        entry.createdAt = number(cursor, "created_at");
        int expiry = cursor.getColumnIndexOrThrow("expires_at");
        entry.expiresAt = cursor.isNull(expiry) ? null : cursor.getLong(expiry);
        entry.ghost = number(cursor, "ghost") != 0; entry.usesPassphrase = number(cursor, "uses_passphrase") != 0;
        entry.deleted = number(cursor, "deleted") != 0; entry.syncState = (int) number(cursor, "sync_state");
        entry.thumb = cursor.getBlob(cursor.getColumnIndexOrThrow("thumb"));
        entry.lat = coordinate(cursor, "lat"); entry.lng = coordinate(cursor, "lng");
        return entry;
    }

    private static String string(Cursor cursor, String name) {
        return cursor.getString(cursor.getColumnIndexOrThrow(name));
    }

    private static long number(Cursor cursor, String name) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(name));
    }

    private static Double coordinate(Cursor cursor, String name) {
        int column = cursor.getColumnIndexOrThrow(name);
        return cursor.isNull(column) ? null : cursor.getDouble(column);
    }
}
