package com.example.kineticvault.diagnostics;

import android.graphics.Bitmap;
import com.example.kineticvault.image.ImageStats;
import java.util.ArrayList;
import java.util.List;

public final class HarnessReport implements AutoCloseable {
    public final List<Entry> entries = new ArrayList<>();
    public Bitmap preview;
    public long generationMs;
    public long totalMs;

    public static final class Entry {
        public boolean ghost;
        public boolean file;
        public long forgeMs;
        public long saveMs;
        public long reloadMs;
        public long unlockMs;
        public long totalMs;
        public String sha256;
        public ImageStats stats;
    }

    // Close only while this preview has not been handed to an ImageView.
    @Override public void close() {
        if (preview != null) {
            preview.recycle();
            preview = null;
        }
    }
}
