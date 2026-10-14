package com.example.kineticvault;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public final class VaultApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        com.example.kineticvault.data.VaultPreferences.load(this, () -> { });
        // A fresh process always starts with the vault's dark identity.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
}
