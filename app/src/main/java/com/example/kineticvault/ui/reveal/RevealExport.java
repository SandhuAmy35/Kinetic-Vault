package com.example.kineticvault.ui.reveal;

import android.content.Context;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.example.kineticvault.data.VaultEntry;
import com.example.kineticvault.data.VaultRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/** Temporary decrypted export: the name stays in memory and never enters the ledger. */
final class RevealExport {
    final File file;
    final Uri uri;
    final String name, mime;
    final VaultEntry vault;

    private RevealExport(File file, Uri uri, String name, VaultEntry vault) {
        this.file = file; this.uri = uri; this.name = name; this.vault = vault;
        int dot = name.lastIndexOf('.');
        String type = dot < 0 ? null : android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(name.substring(dot + 1).toLowerCase(Locale.ROOT));
        mime = type == null ? "application/octet-stream" : type;
    }

    static RevealExport create(Context context, byte[] bytes, String name, VaultEntry vault) throws IOException {
        VaultRepository.requireWorker();
        File directory = new File(context.getCacheDir(), "reveal");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Export storage unavailable");
        File file = new File(directory, UUID.randomUUID().toString());
        boolean complete = false;
        try {
            try (FileOutputStream output = new FileOutputStream(file)) { output.write(bytes); }
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".files", file, name);
            RevealExport result = new RevealExport(file, uri, name, vault); complete = true; return result;
        } finally { if (!complete) file.delete(); }
    }

    void save(Context context, Uri destination) throws IOException {
        VaultRepository.requireWorker();
        byte[] buffer = new byte[8192];
        try (FileInputStream input = new FileInputStream(file);
                OutputStream output = context.getContentResolver().openOutputStream(destination, "wt")) {
            if (output == null) throw new IOException("Export destination unavailable");
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.flush();
        } finally { Arrays.fill(buffer, (byte) 0); }
    }
}
