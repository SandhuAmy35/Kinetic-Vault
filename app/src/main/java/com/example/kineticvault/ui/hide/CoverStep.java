package com.example.kineticvault.ui.hide;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.kineticvault.R;
import java.util.concurrent.Callable;

final class CoverStep {
    private final HideActivity host;
    private final HideWork work;
    private final ImageView preview;
    private final TextView info;
    private final ActivityResultLauncher<String[]> gallery;
    private final ActivityResultLauncher<Uri> camera;
    private boolean ready;
    private Uri pendingCamera;

    CoverStep(HideActivity host, HideWork work) {
        this.host = host; this.work = work;
        preview = host.findViewById(R.id.hide_cover_preview);
        info = host.findViewById(R.id.hide_cover_info);
        gallery = host.registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) load(() -> work.store.load(uri));
        });
        camera = host.registerForActivityResult(new CameraContract(), success -> {
            if (success) load(work.store::finishCapture);
            else { setBusy(false); deleteCapture(); host.message(R.string.hide_camera_cancelled); }
        });
        host.findViewById(R.id.hide_gallery).setOnClickListener(v -> openGallery());
        host.findViewById(R.id.hide_camera).setOnClickListener(v -> openCamera());
        host.findViewById(R.id.hide_generate).setOnClickListener(v -> load(work.store::generate));
        host.findViewById(R.id.hide_cover_next).setOnClickListener(v -> { if (ready) host.showStep(1); });
        setBusy(false);
    }

    private void openGallery() {
        try { gallery.launch(new String[]{"image/*"}); }
        catch (RuntimeException error) { host.message(R.string.hide_picker_error); }
    }

    private void openCamera() {
        setBusy(true);
        work.run(work.store::prepareCapture, new HideWork.Callback<Uri>() {
            public void success(Uri uri) {
                pendingCamera = uri;
                if (host.isForeground()) resume();
            }
            public void failure(int message) { setBusy(false); host.message(message); }
        });
    }

    private void deleteCapture() {
        work.run(() -> { work.store.deleteCapture(); return true; }, new HideWork.Callback<Boolean>() {
            public void success(Boolean ignored) { }
            public void failure(int message) { host.message(message); }
        });
    }

    private void load(Callable<HideStore.CoverInfo> action) {
        ready = false;
        preview.setImageDrawable(null);
        setBusy(true);
        info.setText(R.string.hide_cover_loading);
        work.run(action, new HideWork.Callback<HideStore.CoverInfo>() {
            public void success(HideStore.CoverInfo cover) {
                preview.setImageBitmap(cover.preview);
                info.setText(host.getString(R.string.hide_cover_details, cover.width, cover.height, cover.capacity));
                ready = true;
                setBusy(false);
            }
            public void failure(int message) {
                setBusy(false); info.setText(R.string.hide_cover_retry); host.message(message);
            }
        });
    }

    private void setBusy(boolean busy) {
        int[] controls = {R.id.hide_gallery, R.id.hide_camera, R.id.hide_generate};
        for (int id : controls) host.findViewById(id).setEnabled(!busy);
        host.findViewById(R.id.hide_cover_next).setEnabled(!busy && ready);
        host.findViewById(R.id.hide_cover_loading).setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    void resume() {
        if (pendingCamera == null) return;
        Uri uri = pendingCamera; pendingCamera = null;
        try { camera.launch(uri); }
        catch (RuntimeException error) {
            deleteCapture(); setBusy(false); host.message(R.string.hide_camera_error);
        }
    }

    void close() { pendingCamera = null; preview.setImageDrawable(null); }

    private static final class CameraContract extends ActivityResultContracts.TakePicture {
        @Override public Intent createIntent(Context context, Uri input) {
            Intent intent = super.createIntent(context, input);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.setClipData(ClipData.newRawUri(context.getString(R.string.hide_camera_output), input));
            return intent;
        }
    }
}
