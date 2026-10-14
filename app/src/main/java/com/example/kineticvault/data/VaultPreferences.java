package com.example.kineticvault.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Disk access and disguise hashing share a worker; screens use an in-memory snapshot. */
public final class VaultPreferences {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Values current = new Values(1, false, true, true, "", "", false, false, "");
    private VaultPreferences() { }
    public static Values current() { return current; }
    public static void load(Context context, Runnable ready) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            SharedPreferences p = preferences(app);
            current = new Values(p.getInt("sensitivity", 1), p.getBoolean("gesture_pad", false),
                    p.getBoolean("screenshot_protection", true), p.getBoolean("auto_lock", true),
                    p.getString("server_url", ""), p.getString("api_key", ""),
                    p.getBoolean("disguise", false), p.getBoolean("nerd", false), p.getString("disguise_hash", ""));
            MAIN.post(ready);
        });
    }
    public static void save(Context context, Values values, Consumer<Boolean> done) {
        save(context, values, new char[0], done);
    }
    /** Takes ownership of code and wipes it, including on a failed save. Empty retains the hash. */
    public static void save(Context context, Values values, char[] code, Consumer<Boolean> done) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            boolean saved = false;
            try {
                String hash = code.length == 0 ? values.codeHash : DisguiseCode.create(code);
                Values updated = new Values(values.sensitivity, values.pad, values.protection,
                        values.autoLock, values.url, values.apiKey, values.disguise, values.nerd, hash);
                if ((!updated.disguise || !hash.isEmpty()) && (code.length == 0 || DisguiseCode.valid(code)))
                    saved = persist(app, updated);
                if (saved) current = updated;
            } catch (java.security.GeneralSecurityException | RuntimeException error) { saved = false; }
            finally { Arrays.fill(code, '\0'); }
            boolean result = saved; MAIN.post(() -> done.accept(result));
        });
    }
    private static boolean persist(Context app, Values v) {
        return preferences(app).edit().putInt("sensitivity", v.sensitivity)
                .putBoolean("gesture_pad", v.pad).putBoolean("screenshot_protection", v.protection)
                .putBoolean("auto_lock", v.autoLock).putString("server_url", v.url)
                .putString("api_key", v.apiKey).putBoolean("disguise", v.disguise)
                .putBoolean("nerd", v.nerd).putString("disguise_hash", v.codeHash).commit();
    }
    public static void unlockDisguise(char[] code, Consumer<Boolean> done) {
        String hash = current.codeHash;
        WORKER.execute(() -> {
            boolean match;
            try { match = DisguiseCode.valid(code) && DisguiseCode.matches(code, hash); }
            finally { Arrays.fill(code, '\0'); }
            MAIN.post(() -> done.accept(match));
        });
    }
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }
    public static final class Values {
        public final int sensitivity;
        public final boolean pad, protection, autoLock, disguise, nerd;
        public final String url, apiKey, codeHash;
        public Values(int sensitivity, boolean pad, boolean protection, boolean autoLock, String url, String apiKey) {
            this(sensitivity, pad, protection, autoLock, url, apiKey, current.disguise, current.nerd, current.codeHash);
        }
        public Values(int sensitivity, boolean pad, boolean protection, boolean autoLock,
                      String url, String apiKey, boolean disguise, boolean nerd, String codeHash) {
            this.sensitivity = Math.max(0, Math.min(2, sensitivity));
            this.pad = pad; this.protection = protection; this.autoLock = autoLock;
            this.url = url; this.apiKey = apiKey; this.disguise = disguise;
            this.nerd = nerd; this.codeHash = codeHash;
        }
    }
}
