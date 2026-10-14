package com.example.kineticvault.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.core.content.IntentCompat;
import com.example.kineticvault.R;
import com.example.kineticvault.core.StegoCore;
import com.example.kineticvault.image.ImageUtils;
import com.example.kineticvault.ui.reveal.RevealActivity;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ScannerActivity extends InfoActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private XRayView xray;
    private TextView status;
    private Bitmap plane;
    private Uri selected, camera;
    private int generation;
    private boolean destroyed;
    private final ActivityResultLauncher<String[]> open = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> { if (uri != null) scan(uri); });
    private final ActivityResultLauncher<Uri> take = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), ok -> {
                if (ok && camera != null) scan(camera);
                else status.setText(R.string.scan_camera_cancelled);
            });
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); showPage(R.layout.activity_scanner);
        xray = findViewById(R.id.scan_xray); status = findViewById(R.id.scan_status);
        findViewById(R.id.scan_choose).setOnClickListener(v -> open.launch(new String[]{"image/*"}));
        findViewById(R.id.scan_camera).setOnClickListener(v -> capture());
        findViewById(R.id.scan_reveal).setOnClickListener(v -> reveal());
        if (state != null && state.getString("camera") != null) camera = Uri.parse(state.getString("camera"));
        if (state != null && state.getString("selected") != null) scan(Uri.parse(state.getString("selected")));
        else receive(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent); receive(intent);
    }
    private void receive(Intent intent) {
        Uri uri = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) uri = intent.getData();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri.class);
            if (uri == null && intent.getClipData() != null && intent.getClipData().getItemCount() > 0)
                uri = intent.getClipData().getItemAt(0).getUri();
        }
        if (uri != null) scan(uri);
    }
    private void capture() {
        status.setText(R.string.scan_camera_preparing);
        worker.execute(() -> {
            try {
                File folder = new File(getCacheDir(), "capture");
                if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException();
                File file = File.createTempFile("scan-", ".jpg", folder);
                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", file);
                main.post(() -> launchCamera(uri));
            } catch (IOException | RuntimeException error) {
                main.post(() -> { if (!destroyed) status.setText(R.string.scan_error); });
            }
        });
    }
    private void launchCamera(Uri uri) {
        if (destroyed) return;
        camera = uri;
        try { take.launch(uri); }
        catch (RuntimeException error) { status.setText(R.string.scan_camera_error); }
    }
    private void scan(Uri uri) {
        if (!"content".equals(uri.getScheme())) { status.setText(R.string.scan_error); return; }
        selected = uri; int ticket = ++generation;
        clearPlane(); status.setText(R.string.scan_working);
        findViewById(R.id.scan_reveal).setVisibility(View.GONE);
        worker.execute(() -> decode(uri, ticket));
    }
    private void decode(Uri uri, int ticket) {
        Bitmap image = null, preview = null; int[] pixels = null;
        try {
            image = ImageUtils.decodeExact(ImageUtils.source(getContentResolver(), uri), true);
            pixels = ImageUtils.pixels(image);
            boolean found;
            try { StegoCore.readHeader(pixels); found = true; }
            catch (IOException absent) { found = false; }
            if (!found) preview = DiffMapData.buildXRay(pixels, image.getWidth(), image.getHeight());
            Bitmap result = preview; boolean match = found;
            main.post(() -> showResult(ticket, match, result));
        } catch (IOException | RuntimeException | OutOfMemoryError error) {
            if (preview != null) preview.recycle();
            main.post(() -> { if (!destroyed && ticket == generation) status.setText(R.string.scan_error); });
        } finally {
            if (image != null) image.recycle();
            if (pixels != null) Arrays.fill(pixels, 0);
        }
    }
    private void showResult(int ticket, boolean found, Bitmap result) {
        if (destroyed || ticket != generation) { if (result != null) result.recycle(); return; }
        plane = result; xray.setPlane(result); xray.setVisibility(found ? View.GONE : View.VISIBLE);
        status.setText(found ? R.string.scan_found : R.string.scan_absent);
        findViewById(R.id.scan_reveal).setVisibility(found ? View.VISIBLE : View.GONE);
        status.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
    }
    private void reveal() {
        if (selected == null) return;
        Intent intent = new Intent(this, RevealActivity.class).setData(selected);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(intent);
    }
    private void clearPlane() {
        xray.setPlane(null); xray.setVisibility(View.GONE);
        if (plane != null) { plane.recycle(); plane = null; }
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (camera != null) state.putString("camera", camera.toString());
        if (selected != null) state.putString("selected", selected.toString());
    }
    @Override protected void onDestroy() {
        destroyed = true; generation++; clearPlane(); worker.shutdownNow(); super.onDestroy();
    }
}
