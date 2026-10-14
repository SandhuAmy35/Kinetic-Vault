package com.example.kineticvault.ui;

import android.app.Activity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

public final class ExportSheet {
    private ExportSheet() { }
    public static BottomSheetDialog show(Activity host, int note, int[] labels, Runnable[] actions) {
        BottomSheetDialog sheet = new BottomSheetDialog(host);
        View content = host.getLayoutInflater().inflate(R.layout.sheet_export, null);
        ((TextView) content.findViewById(R.id.export_note)).setText(note);
        LinearLayout options = content.findViewById(R.id.export_options);
        for (int i = 0; i < labels.length; i++) {
            MaterialButton button = new MaterialButton(host);
            button.setText(labels[i]); button.setMinHeight(dp(host, 56));
            Runnable action = actions[i];
            button.setOnClickListener(v -> {
                UiPolish.feedback(v); sheet.dismiss();
                if (!host.isFinishing() && !host.isDestroyed()) action.run();
            });
            options.addView(button, new LinearLayout.LayoutParams(-1, -2));
        }
        content.findViewById(R.id.export_cancel).setOnClickListener(v -> sheet.dismiss());
        sheet.setContentView(content); sheet.show();
        if ((host.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE) != 0
                && sheet.getWindow() != null) sheet.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            int pad = dp(host, 24); v.setPadding(pad + safe.left, pad, pad + safe.right, pad + safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(content); return sheet;
    }
    private static int dp(Activity host, int value) { return Math.round(value * host.getResources().getDisplayMetrics().density); }
}
