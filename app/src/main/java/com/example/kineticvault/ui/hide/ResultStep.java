package com.example.kineticvault.ui.hide;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.kineticvault.R;
import com.example.kineticvault.data.VaultLogger;
import com.example.kineticvault.ui.WipeComparisonView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.concurrent.Callable;

final class ResultStep {
    private final HideActivity host;
    private final HideWork work;
    private final WipeComparisonView comparison;
    private final ActivityResultLauncher<String> document;

    ResultStep(HideActivity host, HideWork work) {
        this.host = host; this.work = work;
        comparison = host.findViewById(R.id.hide_comparison);
        document = host.registerForActivityResult(new ActivityResultContracts.CreateDocument("image/png"), uri -> {
            if (uri == null) { busy(false); return; }
            save(() -> { HideExporter.saveDocument(host, work.store.output, uri); return true; });
        });
        ((SeekBar) host.findViewById(R.id.hide_wipe)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) { comparison.setFraction(progress / 100f); }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) { }
        });
        host.findViewById(R.id.hide_save).setOnClickListener(v -> savePng());
        host.findViewById(R.id.hide_share).setOnClickListener(v -> warnShare());
        host.findViewById(R.id.hide_finish).setOnClickListener(v -> host.finish());
    }

    void show(HideForge.Result result) {
        comparison.setImages(result.before, result.after);
        ((TextView) host.findViewById(R.id.hide_statistics)).setText(host.getString(R.string.hide_stats,
                result.stats.psnrDb, result.stats.changedPercent));
        busy(false);
    }

    private void savePng() {
        busy(true);
        if (Build.VERSION.SDK_INT >= 29) {
            save(() -> { HideExporter.saveGallery(host, work.store.output); return true; });
        } else {
            try { document.launch("KineticVault.png"); }
            catch (RuntimeException error) { busy(false); host.message(R.string.hide_picker_error); }
        }
    }

    private void save(Callable<Boolean> action) {
        busy(true);
        work.run(action, new HideWork.Callback<Boolean>() {
            public void success(Boolean ignored) { busy(false); host.message(R.string.hide_saved); }
            public void failure(int message) { busy(false); host.message(R.string.hide_export_error); }
        });
    }

    private void warnShare() {
        new MaterialAlertDialogBuilder(host).setTitle(R.string.hide_share_title)
                .setMessage(R.string.hide_share_warning).setNegativeButton(R.string.hide_cancel, null)
                .setPositiveButton(R.string.hide_share_file, (dialog, which) -> prepareShare()).show();
    }

    private void prepareShare() {
        busy(true);
        work.run(() -> HideExporter.shareUri(host, work.store.output), new HideWork.Callback<Uri>() {
            public void success(Uri uri) {
                busy(false);
                Intent intent = new Intent(Intent.ACTION_SEND).setType("image/png")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setClipData(ClipData.newRawUri(host.getString(R.string.hide_png_file), uri));
                try {
                    host.startActivity(Intent.createChooser(intent, host.getString(R.string.hide_share_file)));
                    logShare();
                } catch (RuntimeException error) { host.message(R.string.hide_share_error); }
            }
            public void failure(int message) { busy(false); host.message(R.string.hide_export_error); }
        });
    }

    private void logShare() {
        work.run(() -> { VaultLogger.logExport(); return true; }, new HideWork.Callback<Boolean>() {
            public void success(Boolean ignored) { }
            public void failure(int message) { }
        });
    }

    private void busy(boolean value) {
        host.findViewById(R.id.hide_save).setEnabled(!value);
        host.findViewById(R.id.hide_share).setEnabled(!value);
    }
}
