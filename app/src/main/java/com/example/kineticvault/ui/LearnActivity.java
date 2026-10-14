package com.example.kineticvault.ui;
import android.os.Bundle;
import android.view.View;
import android.webkit.*;
import com.example.kineticvault.R;
public final class LearnActivity extends InfoActivity {
    private WebView web;
    private boolean remote, failed;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); showPage(R.layout.activity_learn);
        web = findViewById(R.id.learn_web);
        WebSettings settings = web.getSettings();
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                android.net.Uri uri = request.getUrl();
                if (!request.isForMainFrame()) return false;
                if (!"https".equals(uri.getScheme()) || !"en.wikipedia.org".equals(uri.getHost())) return true;
                remote = true; web.getSettings().setJavaScriptEnabled(false); return false;
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showError();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                if (request.isForMainFrame()) showError();
            }
        });
        findViewById(R.id.learn_local).setOnClickListener(v -> load(false));
        findViewById(R.id.learn_remote).setOnClickListener(v -> load(true));
        findViewById(R.id.learn_retry).setOnClickListener(v -> load(remote));
        load(state != null && state.getBoolean("remote"));
    }
    private void load(boolean online) {
        remote = online; failed = false; web.stopLoading();
        findViewById(R.id.learn_error).setVisibility(View.GONE); web.setVisibility(View.VISIBLE);
        web.getSettings().setJavaScriptEnabled(!online);
        web.loadUrl(online ? getString(R.string.learn_remote_url) : "file:///android_asset/how_it_works.html");
    }
    private void showError() {
        if (failed) return;
        failed = true; web.stopLoading(); web.setVisibility(View.GONE);
        findViewById(R.id.learn_error).setVisibility(View.VISIBLE);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putBoolean("remote", remote);
    }
    @Override protected void onPause() { web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); }
    @Override protected void onDestroy() {
        web.stopLoading(); web.setWebViewClient(new WebViewClient());
        ((android.view.ViewGroup) web.getParent()).removeView(web); web.destroy(); super.onDestroy();
    }
}
