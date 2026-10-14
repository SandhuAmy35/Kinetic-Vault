package com.example.kineticvault.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;
import com.example.kineticvault.diagnostics.TestHarnessActivity;
import com.example.kineticvault.ui.hide.HideActivity;
import com.google.android.material.snackbar.Snackbar;

public final class HomeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        View root = findViewById(R.id.home_root);
        int spacing = getResources().getDimensionPixelSize(R.dimen.space_lg);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(spacing + safe.left, safe.top, spacing + safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        findViewById(R.id.home_hide).setOnClickListener(v -> startActivity(new Intent(this, HideActivity.class)));
        findViewById(R.id.home_lab).setOnClickListener(v -> startActivity(new Intent(this, TestHarnessActivity.class)));
        int[] upcoming = {R.id.home_reveal, R.id.home_scan, R.id.home_history, R.id.home_settings};
        for (int id : upcoming) {
            View tile = findViewById(id);
            ViewCompat.setStateDescription(tile, getString(R.string.home_coming_soon));
            tile.setOnClickListener(v -> Snackbar.make(root, R.string.home_coming_soon, Snackbar.LENGTH_LONG).show());
        }
    }
}
