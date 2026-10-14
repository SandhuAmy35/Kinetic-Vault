package com.example.kineticvault.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Only this explicit allowlist may cross the network boundary. */
final class SyncStore implements AutoCloseable {
    private final VaultDatabaseHelper helper;
    SyncStore(Context context) { helper = new VaultDatabaseHelper(context); }
    List<JSONObject> vaults() throws Exception {
        VaultRepository.requireWorker();
        List<JSONObject> result = new ArrayList<>();
        String[] columns = {"uuid", "image_sha256", "payload_bytes", "capacity_bytes", "ghost",
                "uses_passphrase", "created_at", "expires_at", "deleted", "sync_revision", "sync_updated_at"};
        try (Cursor c = helper.getReadableDatabase().query("vaults", columns, "sync_state != ?",
                new String[]{"1"}, null, null, "id", "50")) {
            while (c.moveToNext()) result.add(vault(c));
        }
        return result;
    }
    private JSONObject vault(Cursor c) throws Exception {
        return new JSONObject().put("uuid", c.getString(0)).put("sha256", c.getString(1))
                .put("payload_bytes", c.getLong(2)).put("capacity_bytes", c.getLong(3))
                .put("ghost", c.getInt(4) != 0).put("uses_passphrase", c.getInt(5) != 0)
                .put("created_at", c.getLong(6)).put("expires_at", c.isNull(7) ? JSONObject.NULL : c.getLong(7))
                .put("action", c.getInt(8) != 0 ? "delete_vault" : "upsert_vault")
                .put("revision", c.getLong(9)).put("updated_at", Math.max(c.getLong(6), c.getLong(10)));
    }
    List<JSONObject> events() throws Exception {
        VaultRepository.requireWorker();
        List<JSONObject> result = new ArrayList<>();
        try (Cursor c = helper.getReadableDatabase().query("sync_events", null, "sync_state != ?",
                new String[]{"1"}, null, null, "ts,event_id", "50")) {
            while (c.moveToNext()) result.add(new JSONObject().put("action", "log_event")
                    .put("event_id", c.getString(c.getColumnIndexOrThrow("event_id")))
                    .put("vault_uuid", c.isNull(c.getColumnIndexOrThrow("vault_uuid")) ? JSONObject.NULL
                            : c.getString(c.getColumnIndexOrThrow("vault_uuid")))
                    .put("type", c.getString(c.getColumnIndexOrThrow("type")))
                    .put("ts", c.getLong(c.getColumnIndexOrThrow("ts"))));
        }
        return result;
    }
    void acknowledge(JSONObject item, boolean success) throws Exception {
        VaultRepository.requireWorker();
        ContentValues values = new ContentValues(); values.put("sync_state", success ? 1 : 2);
        SQLiteDatabase db = helper.getWritableDatabase();
        if (item.has("event_id")) db.update("sync_events", values, "event_id=?",
                new String[]{item.getString("event_id")});
        else db.update("vaults", values, "uuid=? AND sync_revision=?",
                new String[]{item.getString("uuid"), item.getString("revision")});
    }
    int pending() {
        VaultRepository.requireWorker();
        try (Cursor c = helper.getReadableDatabase().rawQuery("SELECT "
                + "(SELECT COUNT(*) FROM vaults WHERE sync_state!=1) + "
                + "(SELECT COUNT(*) FROM sync_events WHERE sync_state!=1)", null)) {
            c.moveToFirst(); return c.getInt(0);
        }
    }
    void airplane(boolean enabled) {
        VaultRepository.requireWorker();
        ContentValues v = new ContentValues(); v.put("event_id", UUID.randomUUID().toString());
        v.putNull("vault_uuid"); v.put("type", enabled ? "AIRPLANE_ON" : "AIRPLANE_OFF");
        v.put("ts", System.currentTimeMillis()); helper.getWritableDatabase().insertOrThrow("sync_events", null, v);
    }
    void resetDestination() {
        VaultRepository.requireWorker();
        ContentValues v = new ContentValues(); v.put("sync_state", 0);
        helper.getWritableDatabase().update("vaults", v, null, null);
        helper.getWritableDatabase().update("sync_events", v, null, null);
    }
    @Override public void close() { VaultRepository.requireWorker(); helper.close(); }
}
