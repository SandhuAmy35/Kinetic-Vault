package com.example.kineticvault.ui.reveal;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import com.example.kineticvault.core.VaultHeader;
import com.example.kineticvault.data.VaultEntry;
import com.example.kineticvault.data.VaultLogger;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

/** Keeps event identity independent of credentials and temporary decrypted exports. */
final class RevealLedger {
    private final Context context;
    private VaultEntry current;

    RevealLedger(Context context) { this.context = context.getApplicationContext(); }

    VaultEntry capture(Uri source, Bitmap image, VaultHeader header) throws IOException {
        return VaultLogger.capture(context, source, image, header.isGhost(), header.usesPassphrase());
    }

    synchronized void adopt(VaultEntry entry) { current = entry; }
    synchronized VaultEntry current() { return current; }

    boolean revealed(VaultEntry entry, boolean success, int type, long bytes) {
        try { VaultLogger.logReveal(context, entry, success, type, bytes); return true; }
        catch (RuntimeException error) { return false; }
    }

    boolean exported(VaultEntry entry) {
        try { VaultLogger.logExport(context, entry); return true; }
        catch (RuntimeException error) { return false; }
    }

    void autoLock(ExecutorService worker) {
        VaultEntry entry = current();
        worker.execute(() -> {
            try { VaultLogger.logAutoLock(context, entry); }
            catch (RuntimeException ignored) {
                // Losing an event must never delay wiping a secret during auto-lock.
            }
        });
    }
}
