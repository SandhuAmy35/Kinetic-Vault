package com.example.kineticvault.ui;

import android.widget.TextView;
import com.example.kineticvault.R;
import com.example.kineticvault.image.ImageStats;

public final class DiffStats {
    private DiffStats() { }
    public static void show(TextView view, ImageStats stats) {
        String psnr = Double.isInfinite(stats.psnrDb) ? view.getContext().getString(R.string.diff_infinite)
                : view.getContext().getString(R.string.diff_db, stats.psnrDb);
        view.setText(view.getContext().getString(R.string.diff_stats, stats.pixelsChanged,
                stats.changedPercent, stats.maxChannelChange, psnr));
    }
}
