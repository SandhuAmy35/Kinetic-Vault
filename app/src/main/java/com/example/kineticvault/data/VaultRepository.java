package com.example.kineticvault.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

/** Synchronous API: callers must use their worker executor, including close(). */
public final class VaultRepository implements AutoCloseable {
    private final VaultDatabaseHelper helper;
    private final Context app;

    public VaultRepository(Context context) { app = context.getApplicationContext(); helper = new VaultDatabaseHelper(app); }

    public static void requireWorker() {
        if (Looper.myLooper() == Looper.getMainLooper())
            throw new IllegalStateException("Vault storage requires a worker thread");
    }

    public long insert(VaultEntry entry) {
        requireWorker();
        long id = helper.getWritableDatabase().insertOrThrow("vaults", null, values(entry));
        SyncManager.get(app).request(); return id;
    }

    public List<VaultEntry> search(String query, Long fromInclusive, Long toExclusive) {
        requireWorker();
        StringBuilder selection = new StringBuilder("deleted = ? AND title LIKE ? ESCAPE '\\'");
        List<String> args = new ArrayList<>();
        args.add("0"); args.add("%" + escape(query == null ? "" : query) + "%");
        if (fromInclusive != null) { selection.append(" AND created_at >= ?"); args.add(fromInclusive.toString()); }
        if (toExclusive != null) { selection.append(" AND created_at < ?"); args.add(toExclusive.toString()); }
        List<VaultEntry> rows = new ArrayList<>();
        try (Cursor cursor = helper.getReadableDatabase().query("vaults", null, selection.toString(),
                args.toArray(new String[0]), null, null, "created_at DESC, id DESC")) {
            while (cursor.moveToNext()) rows.add(VaultEntry.read(cursor));
        }
        return rows;
    }

    public VaultEntry findByHash(String hash) {
        requireWorker();
        try (Cursor cursor = helper.getReadableDatabase().query("vaults", null,
                "image_sha256 = ?", new String[]{hash}, null, null, null, "1")) {
            return cursor.moveToFirst() ? VaultEntry.read(cursor) : null;
        }
    }

    public void rename(String uuid, String title) {
        requireWorker();
        String label = title == null ? "" : title.trim();
        if (label.isEmpty() || label.length() > 80) throw new IllegalArgumentException("Invalid vault title");
        ContentValues values = dirty(); values.put("title", label); update(uuid, values);
    }

    public void setExpiry(String uuid, Long expiresAt) {
        requireWorker();
        ContentValues values = dirty();
        if (expiresAt == null) values.putNull("expires_at"); else values.put("expires_at", expiresAt);
        update(uuid, values);
    }

    public void softDelete(String uuid) {
        requireWorker();
        ContentValues values = dirty(); values.put("deleted", 1); update(uuid, values);
    }

    public void record(VaultEntry entry, String event, Integer payloadType, Long payloadBytes) {
        requireWorker();
        SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.insertWithOnConflict("vaults", null, values(entry), SQLiteDatabase.CONFLICT_IGNORE);
            if (payloadType != null) {
                ContentValues data = dirty(); data.put("payload_type", payloadType); data.put("payload_bytes", payloadBytes);
                db.update("vaults", data, "uuid = ?", new String[]{entry.uuid});
            }
            ContentValues values = new ContentValues();
            values.put("vault_uuid", entry.uuid); values.put("type", event); values.put("ts", System.currentTimeMillis());
            db.insertOrThrow("events", null, values);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        SyncManager.get(app).request();
    }

    private void update(String uuid, ContentValues values) {
        if (helper.getWritableDatabase().update("vaults", values, "uuid = ? AND deleted = ?",
                new String[]{uuid, "0"}) != 1) throw new IllegalStateException("Vault unavailable");
        SyncManager.get(app).request();
    }

    private static ContentValues dirty() {
        ContentValues values = new ContentValues(); values.put("sync_state", 0); return values;
    }

    private static ContentValues values(VaultEntry entry) {
        ContentValues values = dirty();
        values.put("uuid", entry.uuid); values.put("title", entry.title); values.put("image_uri", entry.imageUri);
        values.put("image_sha256", entry.imageSha256); values.put("payload_type", entry.payloadType);
        values.put("payload_bytes", entry.payloadBytes); values.put("capacity_bytes", entry.capacityBytes);
        values.put("ghost", entry.ghost ? 1 : 0); values.put("uses_passphrase", entry.usesPassphrase ? 1 : 0);
        values.put("created_at", entry.createdAt); values.put("expires_at", entry.expiresAt); values.put("thumb", entry.thumb);
        values.put("sync_state", entry.syncState); values.put("deleted", entry.deleted ? 1 : 0);
        values.put("lat", entry.lat); values.put("lng", entry.lng);
        return values;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    @Override public void close() { requireWorker(); helper.close(); }
}
