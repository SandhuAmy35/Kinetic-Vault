package com.example.kineticvault.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class VaultDatabaseHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;

    public VaultDatabaseHelper(Context context) {
        super(context.getApplicationContext(), "kinetic_vault.db", null, VERSION);
    }

    @Override public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS vaults (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "uuid TEXT NOT NULL UNIQUE, title TEXT NOT NULL, image_uri TEXT NOT NULL, "
                + "image_sha256 TEXT NOT NULL UNIQUE, payload_type INTEGER NOT NULL DEFAULT 0, "
                + "payload_bytes INTEGER NOT NULL DEFAULT 0, capacity_bytes INTEGER NOT NULL, "
                + "ghost INTEGER NOT NULL DEFAULT 0 CHECK(ghost IN (0,1)), "
                + "uses_passphrase INTEGER NOT NULL DEFAULT 0 CHECK(uses_passphrase IN (0,1)), "
                + "created_at INTEGER NOT NULL, expires_at INTEGER, thumb BLOB, "
                + "sync_state INTEGER NOT NULL DEFAULT 0 CHECK(sync_state IN (0,1,2)), "
                + "deleted INTEGER NOT NULL DEFAULT 0 CHECK(deleted IN (0,1)), lat REAL, lng REAL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS events (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "vault_uuid TEXT NOT NULL REFERENCES vaults(uuid), "
                + "type TEXT NOT NULL CHECK(type IN ('HIDE','REVEAL_OK','REVEAL_FAIL','AUTOLOCK','EXPORT')), "
                + "ts INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS vaults_created ON vaults(deleted, created_at)");
        db.execSQL("CREATE INDEX IF NOT EXISTS events_vault ON events(vault_uuid, ts)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add metadata columns in place so existing vaults and event history survive upgrades.
        addColumn(db, "expires_at", "INTEGER");
        addColumn(db, "thumb", "BLOB");
        addColumn(db, "sync_state", "INTEGER NOT NULL DEFAULT 0 CHECK(sync_state IN (0,1,2))");
        addColumn(db, "deleted", "INTEGER NOT NULL DEFAULT 0 CHECK(deleted IN (0,1))");
        addColumn(db, "lat", "REAL"); addColumn(db, "lng", "REAL");
        onCreate(db);
    }

    private static void addColumn(SQLiteDatabase db, String name, String definition) {
        try (Cursor columns = db.rawQuery("PRAGMA table_info(vaults)", null)) {
            while (columns.moveToNext()) {
                if (name.equals(columns.getString(columns.getColumnIndexOrThrow("name")))) return;
            }
        }
        // Identifiers and definitions come only from the fixed migration above.
        db.execSQL("ALTER TABLE vaults ADD COLUMN " + name + " " + definition);
    }
}
