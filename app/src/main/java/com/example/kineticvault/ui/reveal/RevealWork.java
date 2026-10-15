package com.example.kineticvault.ui.reveal;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.example.kineticvault.R;
import com.example.kineticvault.core.PayloadCodec;
import com.example.kineticvault.core.StegoCore;
import com.example.kineticvault.core.VaultEngine;
import com.example.kineticvault.core.VaultHeader;
import com.example.kineticvault.core.WorkProgress;
import com.example.kineticvault.data.VaultEntry;
import com.example.kineticvault.image.ImageUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class RevealWork {
    interface Callback<T> { void success(T value); void failure(int message); }
    private final Context context;
    private final RevealLedger ledger;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private int generation;
    private boolean closed;
    private int[] pixels;
    private PayloadCodec.Payload payload;
    private byte[] tokens;
    private char[] passphrase, text;
    private RevealExport exported;

    RevealWork(Context context) {
        this.context = context.getApplicationContext(); ledger = new RevealLedger(this.context);
        worker.execute(() -> {
            try { RevealExport.cleanupOrphans(this.context); }
            catch (IOException | RuntimeException ignored) { /* Export retries cleanup before writing. */ }
        });
    }

    void protection(java.util.function.Consumer<Boolean> callback) {
        worker.execute(() -> {
            boolean enabled = context.getSharedPreferences(context.getPackageName() + "_preferences", 0)
                    .getBoolean("screenshot_protection", true);
            main.post(() -> { if (!closed) callback.accept(enabled); });
        });
    }

    void load(Uri uri, Callback<VaultHeader> callback) {
        clearSensitive();
        final int ticket;
        synchronized (this) { pixels = null; ledger.adopt(null); ticket = generation; }
        worker.execute(() -> {
            Bitmap bitmap = null;
            boolean recording = false;
            try {
                bitmap = ImageUtils.decodeExact(ImageUtils.source(context.getContentResolver(), uri), true);
                int[] decoded = ImageUtils.pixels(bitmap);
                VaultHeader header = StegoCore.readHeader(decoded);
                synchronized (this) { requireCurrent(ticket); }
                recording = true;
                VaultEntry entry = ledger.capture(uri, bitmap, header);
                synchronized (this) { requireCurrent(ticket); pixels = decoded; ledger.adopt(entry); }
                deliver(ticket, () -> callback.success(header));
            } catch (CancellationException ignored) {
            } catch (OutOfMemoryError error) {
                deliver(ticket, () -> callback.failure(R.string.hide_memory_error));
            } catch (Exception error) {
                int message = recording ? R.string.vault_record_error : "No Kinetic Vault data found".equals(error.getMessage())
                        ? R.string.reveal_no_vault : R.string.reveal_image_error;
                deliver(ticket, () -> callback.failure(message));
            } finally { if (bitmap != null) bitmap.recycle(); }
        });
    }

    synchronized void unlock(byte[] moves, char[] password, WorkProgress progress, Callback<Boolean> callback) {
        tokens = moves; passphrase = password;
        int ticket = generation;
        int[] source = pixels;
        VaultEntry entry = ledger.current();
        worker.execute(() -> decrypt(ticket, source, moves, password, progress, callback, entry));
    }

    private void decrypt(int ticket, int[] source, byte[] moves, char[] password,
            WorkProgress progress, Callback<Boolean> callback, VaultEntry entry) {
        PayloadCodec.Payload result = null;
        try {
            synchronized (this) { requireCurrent(ticket); }
            RevealExpiry.requireAllowed(context, entry);
            result = VaultEngine.reveal(source, moves, password, (stage, done, total) -> {
                synchronized (this) { requireCurrent(ticket); }
                deliver(ticket, () -> progress.onProgress(stage, done, total));
            });
            RevealExpiry.requireAllowed(context, entry);
            char[] decoded = result.type == PayloadCodec.TEXT ? decodeText(result.data) : null;
            int type = result.type; long bytes = result.data.length;
            synchronized (this) {
                if (closed || ticket != generation) {
                    if (decoded != null) Arrays.fill(decoded, '\0');
                    throw new CancellationException();
                }
                requireCurrent(ticket);
                payload = result; result = null;
                text = decoded;
            }
            boolean logged = ledger.revealed(entry, true, type, bytes);
            deliver(ticket, () -> callback.success(logged));
        } catch (CancellationException ignored) {
        } catch (RevealExpiry.Expired error) {
            discardResult(ticket);
            deliver(ticket, () -> callback.failure(R.string.invention_expired));
        } catch (OutOfMemoryError error) {
            discardResult(ticket);
            ledger.revealed(entry, false, 0, 0);
            deliver(ticket, () -> callback.failure(R.string.hide_memory_error));
        } catch (Exception error) {
            discardResult(ticket);
            boolean logged = ledger.revealed(entry, false, 0, 0);
            deliver(ticket, () -> callback.failure(logged ? R.string.reveal_denied : R.string.vault_record_error));
        } finally {
            if (result != null) result.close();
            Arrays.fill(moves, (byte) 0); Arrays.fill(password, '\0');
            synchronized (this) { if (tokens == moves) { tokens = null; passphrase = null; } }
        }
    }

    private static char[] decodeText(byte[] bytes) {
        CharBuffer buffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
        char[] result = new char[buffer.remaining()];
        buffer.get(result);
        if (buffer.hasArray()) Arrays.fill(buffer.array(), '\0');
        return result;
    }

    synchronized char[] takeText() { char[] result = text; text = null; return result; }
    synchronized boolean hasPayload() { return payload != null; }
    synchronized boolean hasFile() { return payload != null && payload.type == PayloadCodec.FILE; }

    void prepareExport(Callback<RevealExport> callback) {
        final int ticket;
        synchronized (this) { ticket = generation; }
        worker.execute(() -> {
            RevealExport pending = null;
            byte[] bytes = null;
            try {
                String name;
                synchronized (this) {
                    requireCurrent(ticket);
                    if (!hasPayload()) throw new IOException();
                    bytes = payload.data.clone(); name = hasFile() ? payload.filename : context.getString(R.string.txt_name);
                }
                pending = RevealExport.create(context, bytes, name, ledger.current());
                RevealExport result = pending;
                synchronized (this) { requireCurrent(ticket); exported = result; }
                pending = null;
                deliver(ticket, () -> callback.success(result));
            } catch (CancellationException ignored) {
            } catch (Exception | OutOfMemoryError error) {
                deliver(ticket, () -> callback.failure(R.string.reveal_export_error));
            } finally {
                if (bytes != null) Arrays.fill(bytes, (byte) 0);
                if (pending != null) pending.file.delete();
            }
        });
    }

    void save(Uri destination, Callback<Boolean> callback) {
        final RevealExport source;
        synchronized (this) { source = exported; exported = null; }
        worker.execute(() -> {
            boolean success = false;
            boolean recorded = true;
            try {
                if (source == null) throw new IOException();
                source.save(context, destination);
                recorded = ledger.exported(source.vault);
                success = true;
            } catch (Exception ignored) {
            } finally {
                if (source != null) source.file.delete();
            }
            boolean saved = success;
            boolean logged = recorded;
            main.post(() -> { if (!closed) {
                if (saved) callback.success(logged); else callback.failure(R.string.reveal_export_error);
            } });
        });
    }

    void opened(RevealExport source, Callback<Boolean> callback) {
        worker.execute(() -> {
            boolean logged = ledger.exported(source.vault);
            main.post(() -> { if (!closed && !logged) callback.failure(R.string.vault_record_error); });
        });
    }

    void autoLock() { ledger.autoLock(worker); }

    void discardExport() {
        final RevealExport source;
        synchronized (this) { source = exported; exported = null; }
        if (source != null) worker.execute(() -> source.file.delete());
    }

    synchronized void clearSensitive() {
        generation++;
        if (tokens != null) Arrays.fill(tokens, (byte) 0);
        if (passphrase != null) Arrays.fill(passphrase, '\0');
        tokens = null; passphrase = null;
        discardResult(generation);
    }

    private synchronized void discardResult(int ticket) {
        if (ticket != generation) return;
        if (payload != null) payload.close();
        if (text != null) Arrays.fill(text, '\0');
        payload = null; text = null;
    }

    private synchronized void requireCurrent(int ticket) {
        if (closed || ticket != generation) throw new CancellationException();
    }

    private void deliver(int ticket, Runnable action) {
        main.post(() -> {
            synchronized (this) { if (closed || ticket != generation) return; }
            action.run();
        });
    }

    void close() {
        clearSensitive(); discardExport();
        synchronized (this) { closed = true; pixels = null; }
        worker.shutdown();
    }
}
