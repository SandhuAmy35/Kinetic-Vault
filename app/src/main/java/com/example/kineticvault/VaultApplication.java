package com.example.kineticvault;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public final class VaultApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        com.example.kineticvault.data.VaultPreferences.load(this, () -> com.example.kineticvault.data.SyncManager.get(this).configurationChanged());
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            private int visible;
            @Override public void onActivityStarted(android.app.Activity activity) {
                if (++visible == 1) com.example.kineticvault.data.SyncManager.get(VaultApplication.this).start();
            }
            @Override public void onActivityStopped(android.app.Activity activity) {
                if (--visible == 0) com.example.kineticvault.data.SyncManager.get(VaultApplication.this).stop();
            }
            @Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityResumed(android.app.Activity a) { com.example.kineticvault.ui.UiPolish.install(a); }
            @Override public void onActivityPaused(android.app.Activity a) { com.example.kineticvault.ui.UiPolish.pause(a); }
            @Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) { }
            @Override public void onActivityDestroyed(android.app.Activity a) { }
        });
        // A fresh process always starts with the vault's dark identity.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
