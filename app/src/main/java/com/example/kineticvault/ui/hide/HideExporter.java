package com.example.kineticvault.ui.hide;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import com.example.kineticvault.data.VaultLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class HideExporter {
    private HideExporter() { }

    static boolean saveGallery(Context context, File source) throws IOException {
        if (Build.VERSION.SDK_INT < 29) throw new IOException("Document destination required");
        ContentResolver resolver = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, source.getName());
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KineticVault");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Gallery unavailable");
        boolean complete = false;
        try {
            copy(resolver, source, uri);
            values.clear();
            values.put(MediaStore.Images.Media.IS_PENDING, 0);
            if (resolver.update(uri, values, null, null) < 1) throw new IOException("Gallery publish failed");
            complete = true;
            return recordExport(context, source);
        } finally {
            if (!complete) resolver.delete(uri, null, null);
        }
    }

    static boolean saveDocument(Context context, File source, Uri destination) throws IOException {
        copy(context.getContentResolver(), source, destination);
        return recordExport(context, source);
    }

    private static boolean recordExport(Context context, File source) {
        try { VaultLogger.logExport(context, source); return true; }
        catch (IOException | RuntimeException error) { return false; }
    }

    static Uri shareUri(Context context, File source) throws IOException {
        if (source == null || !source.isFile()) throw new IOException("Forge a vault first");
        return FileProvider.getUriForFile(context, context.getPackageName() + ".files", source);
    }

    private static void copy(ContentResolver resolver, File source, Uri destination) throws IOException {
        if (source == null) throw new IOException("Forge a vault first");
        try (InputStream input = new FileInputStream(source);
             OutputStream output = resolver.openOutputStream(destination, "wt")) {
            if (output == null) throw new IOException("Destination unavailable");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
        }
    }
}
