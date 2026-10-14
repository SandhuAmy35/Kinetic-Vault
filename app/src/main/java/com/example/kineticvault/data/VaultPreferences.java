package com.example.kineticvault.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Preference disk access stays on one worker; screens consume an in-memory snapshot. */
public final class VaultPreferences {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static volatile Values current = new Values(1, false, true, true, "", "");
    private VaultPreferences() { }
    public static Values current() { return current; }
    public static void load(Context context, Runnable ready) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            SharedPreferences p = preferences(app);
            current = new Values(p.getInt("sensitivity", 1), p.getBoolean("gesture_pad", false),
                    p.getBoolean("screenshot_protection", true), p.getBoolean("auto_lock", true),
                    p.getString("server_url", ""), p.getString("api_key", ""));
            MAIN.post(ready);
        });
    }
    public static void save(Context context, Values values, java.util.function.Consumer<Boolean> done) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            boolean saved = preferences(app).edit().putInt("sensitivity", values.sensitivity)
                    .putBoolean("gesture_pad", values.pad).putBoolean("screenshot_protection", values.protection)
                    .putBoolean("auto_lock", values.autoLock).putString("server_url", values.url)
                    .putString("api_key", values.apiKey).commit();
            if (saved) current = values;
            MAIN.post(() -> done.accept(saved));
        });
    }
    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }
    public static final class Values {
        public final int sensitivity;
        public final boolean pad, protection, autoLock;
        public final String url, apiKey;
        public Values(int sensitivity, boolean pad, boolean protection, boolean autoLock, String url, String apiKey) {
            this.sensitivity = Math.max(0, Math.min(2, sensitivity));
            this.pad = pad; this.protection = protection; this.autoLock = autoLock;
            this.url = url; this.apiKey = apiKey;
        }
    }
}
