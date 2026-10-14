package com.example.kineticvault.ui;

import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;

/** Shared chrome for the small, scrollable information screens. */
public abstract class InfoActivity extends AppCompatActivity {
    protected void showPage(int layout) {
        EdgeToEdge.enable(this); setContentView(layout);
        View root = findViewById(R.id.info_root);
        int pad = getResources().getDimensionPixelSize(R.dimen.space_lg);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(pad + safe.left, safe.top, pad + safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        findViewById(R.id.info_back).setOnClickListener(v -> finish());
    }
}
