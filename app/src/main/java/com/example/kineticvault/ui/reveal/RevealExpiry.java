package com.example.kineticvault.ui.reveal;

import android.content.Context;
import com.example.kineticvault.data.VaultEntry;
import com.example.kineticvault.data.VaultRepository;
import java.io.IOException;

/** A local policy, never a promise that exported copies can expire. */
final class RevealExpiry {
    static final class Expired extends IOException { }
    private RevealExpiry() { }

    static void requireAllowed(Context context, VaultEntry entry) throws IOException {
        if (entry == null) throw new IOException();
        try (VaultRepository repository = new VaultRepository(context)) {
            VaultEntry fresh = repository.findByHash(entry.imageSha256);
            if (fresh == null) throw new IOException();
            if (fresh.expiresAt != null && System.currentTimeMillis() >= fresh.expiresAt)
                throw new Expired();
        }
    }
}
