package com.example.kineticvault.ui.hide;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.example.kineticvault.R;
import com.example.kineticvault.core.Constants;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A bounded erasable buffer avoids retaining imported plaintext in a String. */
final class TextImport implements AutoCloseable {
    private final HideActivity host;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile int generation;
    private volatile boolean closed;
    TextImport(HideActivity host) { this.host = host; }
    void read(Uri uri, java.util.function.Consumer<char[]> done) {
        int ticket = ++generation;
        worker.execute(() -> {
            char[] buffer = new char[4096];
            int length = 0;
            try (InputStream input = host.getContentResolver().openInputStream(uri)) {
                if (input == null) throw new java.io.IOException();
                try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT))) {
                    int value;
                    while ((value = reader.read()) != -1) {
                        if (closed || ticket != generation) return;
                        if (length == Constants.MAX_PAYLOAD_BYTES / 4) throw new java.io.IOException();
                        if (length == buffer.length) {
                            char[] next = Arrays.copyOf(buffer, Math.min(buffer.length * 2, Constants.MAX_PAYLOAD_BYTES / 4));
                            Arrays.fill(buffer, '\0'); buffer = next;
                        }
                        buffer[length++] = (char) value;
                    }
                }
                char[] result = Arrays.copyOf(buffer, length);
                main.post(() -> {
                    try { if (!closed && ticket == generation && !host.isDestroyed()) done.accept(result); }
                    finally { Arrays.fill(result, '\0'); }
                });
            } catch (Exception | OutOfMemoryError error) {
                main.post(() -> { if (!closed && ticket == generation) host.message(R.string.txt_import_error); });
            } finally { Arrays.fill(buffer, '\0'); }
        });
    }
    void cancel() { generation++; }
    @Override public void close() { closed = true; generation++; worker.shutdown(); }
}
