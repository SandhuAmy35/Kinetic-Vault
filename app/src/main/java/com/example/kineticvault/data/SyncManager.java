package com.example.kineticvault.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Foreground metadata sync; durable queues survive process death and remain usable offline. */
public final class SyncManager {
    public interface Listener { void onSync(Status status); }
    public static final class Status {
        public final boolean offline, airplane, statsAvailable;
        public final int pending;
        public final long vaults, events, bytes;
        Status(boolean offline, boolean airplane, int pending, JSONObject stats) {
            this.offline = offline; this.airplane = airplane; this.pending = pending;
            statsAvailable = stats != null; vaults = stats == null ? 0 : stats.optLong("vaults");
            events = stats == null ? 0 : stats.optLong("events"); bytes = stats == null ? 0 : stats.optLong("payload_bytes");
        }
    }
    private static SyncManager instance;
    private final Context app;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<Listener> listeners = new ArrayList<>();
    private volatile boolean active, airplane;
    private boolean busy, rerun, registered;
    private int pending;
    private JSONObject stats;
    private Status status = new Status(true, false, 0, null);
    private final Runnable poll = this::request;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) return;
            airplane = Settings.Global.getInt(app.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
            boolean enabled = airplane;
            publish(new Status(true, enabled, status.pending, null));
            worker.execute(() -> {
                try (SyncStore store = new SyncStore(app)) { store.airplane(enabled); }
                catch (Exception ignored) { /* Local vault features do not depend on the ledger. */ }
                main.post(SyncManager.this::request);
            });
        }
    };
    private SyncManager(Context context) { app = context.getApplicationContext(); }
    public static synchronized SyncManager get(Context context) {
        if (instance == null) instance = new SyncManager(context);
        return instance;
    }
    public void addListener(Listener listener) { listeners.add(listener); listener.onSync(status); }
    public void removeListener(Listener listener) { listeners.remove(listener); }
    public void start() {
        active = true;
        airplane = Settings.Global.getInt(app.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        if (!registered) {
            ContextCompat.registerReceiver(app, receiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED),
                    ContextCompat.RECEIVER_EXPORTED);
            registered = true;
        }
        request();
    }
    public void stop() {
        active = false; main.removeCallbacks(poll);
        if (registered) { app.unregisterReceiver(receiver); registered = false; }
    }
    public void request() {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post(this::request); return; }
        main.removeCallbacks(poll);
        if (!active) return;
        if (busy) { rerun = true; return; }
        busy = true; worker.execute(this::sync);
    }
    public void configurationChanged() {
        worker.execute(() -> preferences().edit().putLong("next_retry", 0).putInt("failures", 0).commit());
        request();
    }
    public void testConnection(String url, String key, Consumer<Boolean> done) {
        worker.execute(() -> {
            boolean ok = false;
            try {
                JSONObject result = SyncClient.post(url, key, new JSONObject().put("action", "health")
                        .put("device_id", deviceId()), () -> airplane || !active);
                ok = "kinetic-vault".equals(result.optString("service")) && result.optInt("version") == 1;
            } catch (Exception ignored) { }
            boolean result = ok; main.post(() -> done.accept(result));
        });
    }
    private SharedPreferences preferences() { return app.getSharedPreferences("metadata_sync", Context.MODE_PRIVATE); }
    private String deviceId() {
        SharedPreferences p = preferences(); String id = p.getString("device_id", null);
        if (id == null) {
            id = UUID.randomUUID().toString();
            if (!p.edit().putString("device_id", id).commit()) throw new IllegalStateException();
        }
        return id;
    }
    private void destination(SyncStore store, VaultPreferences.Values config) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                (config.url + "\n" + config.apiKey).getBytes(StandardCharsets.UTF_8));
        String signature = android.util.Base64.encodeToString(digest, android.util.Base64.NO_WRAP);
        SharedPreferences p = preferences();
        if (!signature.equals(p.getString("destination", ""))) {
            store.resetDestination(); stats = null;
            if (!p.edit().putString("destination", signature).putLong("next_retry", 0)
                    .putInt("failures", 0).commit()) throw new IllegalStateException();
        }
    }
    private void sync() {
        boolean offline = true; long delay = 30000;
        try (SyncStore store = new SyncStore(app)) {
            VaultPreferences.Values config = VaultPreferences.current();
            destination(store, config); pending = store.pending();
            long wait = preferences().getLong("next_retry", 0) - System.currentTimeMillis();
            if (!active || airplane || config.url.isEmpty() || config.apiKey.isEmpty()) return;
            if (wait > 0) { delay = Math.min(wait, 300000); return; }
            upload(store, store.vaults(), config); upload(store, store.events(), config);
            JSONObject result = send(config, new JSONObject().put("action", "stats"));
            stats = result; pending = store.pending(); offline = false;
            preferences().edit().putInt("failures", 0).putLong("next_retry", 0).commit();
            if (pending > 0) delay = 1000;
        } catch (Exception ignored) {
            int failures = Math.min(6, preferences().getInt("failures", 0) + 1);
            delay = Math.min(300000, 5000L << (failures - 1));
            preferences().edit().putInt("failures", failures)
                    .putLong("next_retry", System.currentTimeMillis() + delay).commit();
        } finally { finish(offline, delay); }
    }
    private void upload(SyncStore store, List<JSONObject> items, VaultPreferences.Values config) throws Exception {
        for (JSONObject item : items) {
            if (!active || airplane) throw new java.io.IOException();
            try { send(config, item); store.acknowledge(item, true); }
            catch (Exception error) { store.acknowledge(item, false); throw error; }
        }
    }
    private JSONObject send(VaultPreferences.Values config, JSONObject item) throws Exception {
        return SyncClient.post(config.url, config.apiKey, item.put("device_id", deviceId()), () -> airplane || !active);
    }
    private void finish(boolean offline, long delay) {
        Status next = new Status(offline, airplane, pending, stats);
        main.post(() -> {
            busy = false; publish(next);
            if (active) { main.removeCallbacks(poll); main.postDelayed(poll, rerun ? 0 : delay); }
            rerun = false;
        });
    }
    private void publish(Status next) {
        status = next;
        for (Listener listener : new ArrayList<>(listeners)) listener.onSync(next);
    }
}
