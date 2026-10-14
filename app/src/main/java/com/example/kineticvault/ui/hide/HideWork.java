package com.example.kineticvault.ui.hide;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.example.kineticvault.R;
import com.example.kineticvault.core.WorkProgress;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

final class HideWork implements AutoCloseable {
    interface Callback<T> { void success(T value); void failure(int message); }
    interface Progress { void update(int label, int done, int total); }
    final HideStore store;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger secretGeneration = new AtomicInteger();
    private volatile boolean closed;

    HideWork(Context context) { store = new HideStore(context); }

    <T> void run(Callable<T> action, Callback<T> callback) { run(action, () -> { }, callback); }

    private <T> void run(Callable<T> action, Runnable cleanup, Callback<T> callback) {
        if (closed) { cleanup.run(); return; }
        worker.execute(() -> {
            try {
                if (closed) return;
                T result = action.call();
                main.post(() -> { if (!closed) callback.success(result); });
            } catch (CancellationException ignored) {
                // Cancellation still reaches cleanup, including queued credential owners.
            } catch (OutOfMemoryError error) {
                fail(callback, R.string.hide_memory_error);
            } catch (Exception error) {
                fail(callback, R.string.hide_operation_error);
            } finally { cleanup.run(); }
        });
    }

    void measure(SecretInput input, boolean keep, Callback<HideStore.Capacity> callback) {
        int generation = secretGeneration.get();
        run(() -> {
            guard(generation);
            HideStore.Capacity result = store.measure(input, keep);
            if (generation != secretGeneration.get()) { store.clearPrepared(); guard(generation); }
            return result;
        }, input::close, callback);
    }

    void forge(byte[] tokens, char[] passphrase, Progress progress, Callback<HideForge.Result> callback) {
        int generation = secretGeneration.get();
        run(() -> {
            guard(generation);
            return store.forge(tokens, passphrase, (label, done, total) -> {
                guard(generation);
                main.post(() -> {
                    if (!closed && generation == secretGeneration.get()) progress.update(label, done, total);
                });
            }, () -> guard(generation));
        }, () -> {
            Arrays.fill(tokens, (byte) 0);
            Arrays.fill(passphrase, '\0');
        }, new Callback<HideForge.Result>() {
            public void success(HideForge.Result result) {
                if (generation == secretGeneration.get()) callback.success(result);
            }
            public void failure(int message) {
                if (generation == secretGeneration.get()) callback.failure(message);
            }
        });
    }

    void clearSensitive() {
        secretGeneration.incrementAndGet();
        if (!closed) worker.execute(store::clearPrepared);
    }

    private void guard(int generation) {
        if (closed || generation != secretGeneration.get()) throw new CancellationException();
    }

    private <T> void fail(Callback<T> callback, int message) {
        main.post(() -> { if (!closed) callback.failure(message); });
    }

    static int stageLabel(WorkProgress.Stage stage) {
        switch (stage) {
            case PACK: return R.string.hide_stage_pack;
            case COMPRESS: return R.string.hide_stage_compress;
            case KEY: return R.string.hide_stage_key;
            case ENCRYPT: return R.string.hide_stage_encrypt;
            case EMBED: return R.string.hide_stage_embed;
            default: return R.string.hide_forging;
        }
    }

    @Override public void close() {
        closed = true;
        secretGeneration.incrementAndGet();
        // Drain rather than drop queued tasks: each owns arrays that must be wiped.
        worker.execute(store::close);
        worker.shutdown();
    }
}
