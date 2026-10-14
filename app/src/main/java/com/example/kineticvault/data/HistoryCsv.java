package com.example.kineticvault.data;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.example.kineticvault.R;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/** Event ledger only: no credentials, payload names, thumbnails or decrypted content. */
public final class HistoryCsv {
    private HistoryCsv() { }
    public static void export(Context context, Uri destination) throws IOException {
        VaultRepository.requireWorker();
        try (VaultDatabaseHelper helper = new VaultDatabaseHelper(context);
                Cursor rows = helper.getReadableDatabase().rawQuery(
                        "SELECT id,vault_uuid,type,ts FROM events ORDER BY ts,id", null);
                OutputStream stream = context.getContentResolver().openOutputStream(destination, "wt")) {
            if (stream == null) throw new IOException();
            try (Writer out = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
                out.write(context.getString(R.string.history_csv_header)); out.write("\r\n");
                while (rows.moveToNext()) {
                    for (int i = 0; i < 4; i++) {
                        if (i > 0) out.write(',');
                        out.write(cell(rows.getString(i)));
                    }
                    out.write("\r\n");
                }
            }
        }
    }
    private static String cell(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty() && "=+-@\t\r\n".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
