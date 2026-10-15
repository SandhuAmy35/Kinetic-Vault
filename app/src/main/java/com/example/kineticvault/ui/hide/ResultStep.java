package com.example.kineticvault.ui.hide;

import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.kineticvault.R;
import com.example.kineticvault.data.VaultLogger;
import com.example.kineticvault.ui.WipeSliderView;
import com.example.kineticvault.ui.DiffMapView;
import com.example.kineticvault.ui.XRayView;
import com.example.kineticvault.ui.DiffStats;
import com.example.kineticvault.ui.GhostComparisonActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.concurrent.Callable;

final class ResultStep {
    private final HideActivity host;
    private final HideWork work;
    private com.google.android.material.bottomsheet.BottomSheetDialog sheet;
    private final WipeSliderView comparison;
    private final ActivityResultLauncher<String> document;

    ResultStep(HideActivity host, HideWork work) {
        this.host = host; this.work = work;
        comparison = host.findViewById(R.id.hide_comparison);
        document = host.registerForActivityResult(new ActivityResultContracts.CreateDocument("image/png"), uri -> {
            if (uri == null) { busy(false); return; }
            save(() -> HideExporter.saveDocument(host, work.store.output, uri));
        });
        ((DiffMapView) host.findViewById(R.id.hide_diff)).bindLoupe(host.findViewById(R.id.hide_loupe));
        host.findViewById(R.id.hide_diff_replay).setOnClickListener(v ->
                ((DiffMapView) host.findViewById(R.id.hide_diff)).startSweep());
        host.findViewById(R.id.hide_ghost_compare).setOnClickListener(v ->
                host.startActivity(new Intent(host, GhostComparisonActivity.class)));
        host.findViewById(R.id.hide_save).setOnClickListener(v -> exportOptions());
        host.findViewById(R.id.hide_share).setOnClickListener(v -> warnShare());
        host.findViewById(R.id.hide_finish).setOnClickListener(v -> host.finish());
    }

    void show(HideForge.Result result) {
        comparison.setImages(result.before, result.after);
        DiffMapView diff = host.findViewById(R.id.hide_diff);
        diff.setData(result.visuals);
        diff.startSweep();
        ((XRayView) host.findViewById(R.id.hide_xray)).setPlane(result.visuals.xray);
        DiffStats.show(host.findViewById(R.id.hide_statistics), result.stats);
        busy(false);
    }

    void pause() { if (sheet != null) { sheet.dismiss(); sheet = null; } ((DiffMapView) host.findViewById(R.id.hide_diff)).stopSweep(); }

    void close() {
        comparison.setImages(null, null);
        ((DiffMapView) host.findViewById(R.id.hide_diff)).setData(null);
        ((XRayView) host.findViewById(R.id.hide_xray)).setPlane(null);
    }

    private void exportOptions() {
        if (sheet != null) sheet.dismiss();
        int[] labels = Build.VERSION.SDK_INT >= 29
                ? new int[]{R.string.export_gallery, R.string.export_document, R.string.hide_share_file, R.string.invention_proof_share}
                : new int[]{R.string.export_document, R.string.hide_share_file, R.string.invention_proof_share};
        Runnable[] actions = Build.VERSION.SDK_INT >= 29
                ? new Runnable[]{this::savePng, this::saveDocument, this::warnShare, this::warnProof}
                : new Runnable[]{this::saveDocument, this::warnShare, this::warnProof};
        sheet = com.example.kineticvault.ui.ExportSheet.show(host, R.string.export_png_note, labels, actions);
    }
    private void saveDocument() {
        busy(true);
        try { document.launch(host.getString(R.string.audit_png_name)); }
        catch (RuntimeException error) { busy(false); host.message(R.string.hide_picker_error); }
    }
    private void savePng() {
        busy(true);
        if (Build.VERSION.SDK_INT >= 29) {
            save(() -> HideExporter.saveGallery(host, work.store.output));
        } else {
            try { document.launch(host.getString(R.string.audit_png_name)); }
            catch (RuntimeException error) { busy(false); host.message(R.string.hide_picker_error); }
        }
    }

    private void save(Callable<Boolean> action) {
        busy(true);
        work.run(action, new HideWork.Callback<Boolean>() {
            public void success(Boolean logged) {
                busy(false); host.message(logged ? R.string.hide_saved : R.string.vault_saved_event_error);
            }
            public void failure(int message) { busy(false); host.message(R.string.hide_export_error); }
        });
    }

    private void warnShare() {
        new MaterialAlertDialogBuilder(host).setTitle(R.string.hide_share_title)
                .setMessage(R.string.hide_share_warning).setNegativeButton(R.string.hide_cancel, null)
                .setPositiveButton(R.string.hide_share_file, (dialog, which) -> prepareShare()).show();
    }

    private void warnProof() {
        new MaterialAlertDialogBuilder(host).setTitle(R.string.hide_share_title)
                .setMessage(R.string.invention_proof_warning).setNegativeButton(R.string.hide_cancel, null)
                .setPositiveButton(R.string.invention_proof_share, (dialog, which) -> prepareShare(true)).show();
    }

    private void prepareShare() { prepareShare(false); }

    private void prepareShare(boolean proof) {
        busy(true);
        work.run(() -> proof ? work.store.proofUri() : HideExporter.shareUri(host, work.store.output), new HideWork.Callback<Uri>() {
            public void success(Uri uri) {
                busy(false);
                Intent intent = new Intent(Intent.ACTION_SEND).setType("image/png")
                        .putExtra(Intent.EXTRA_STREAM, uri)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setClipData(ClipData.newRawUri(host.getString(proof ? R.string.invention_proof_title : R.string.hide_png_file), uri));
                try {
                    host.startActivity(Intent.createChooser(intent, host.getString(R.string.hide_share_file)));
                    if (!proof) logShare();
                } catch (RuntimeException error) { host.message(R.string.hide_share_error); }
            }
            public void failure(int message) { busy(false); host.message(R.string.hide_export_error); }
        });
    }

    private void logShare() {
        work.run(() -> { VaultLogger.logExport(host, work.store.output); return true; }, new HideWork.Callback<Boolean>() {
            public void success(Boolean ignored) { }
            public void failure(int message) { host.message(R.string.vault_record_error); }
        });
    }

    private void busy(boolean value) {
        host.findViewById(R.id.hide_save).setEnabled(!value);
        host.findViewById(R.id.hide_share).setEnabled(!value);
    }
}
