package com.example.kineticvault.core;

public interface WorkProgress {
    enum Stage { PACK, COMPRESS, KEY, ENCRYPT, EMBED, EXTRACT, DECRYPT, UNPACK }

    // Called on the worker thread. total == 0 means indeterminate progress.
    void onProgress(Stage stage, int completed, int total);

    static void report(WorkProgress listener, Stage stage, int done, int total) {
        if (listener != null) {
            listener.onProgress(stage, done, total);
        }
    }
}
