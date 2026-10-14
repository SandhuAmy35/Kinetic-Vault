package com.example.kineticvault.data;

import android.security.NetworkSecurityPolicy;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

final class SyncClient {
    private SyncClient() { }
    static JSONObject post(String address, String key, JSONObject body, BooleanSupplier paused) throws Exception {
        VaultRepository.requireWorker();
        URL url = new URL(address);
        if (!("https".equals(url.getProtocol()) || "http".equals(url.getProtocol()))
                || url.getHost().isEmpty() || url.getUserInfo() != null || url.getRef() != null
                || key.isEmpty() || key.chars().anyMatch(value -> value == 13 || value == 10)) throw new IOException();
        if ("http".equals(url.getProtocol()) && !NetworkSecurityPolicy.getInstance()
                .isCleartextTrafficPermitted(url.getHost())) throw new IOException();
        if (paused.getAsBoolean()) throw new IOException();
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        try {
            c.setConnectTimeout(5000); c.setReadTimeout(5000); c.setInstanceFollowRedirects(false);
            c.setRequestMethod("POST"); c.setDoOutput(true); c.setUseCaches(false);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("X-API-KEY", key);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            c.setFixedLengthStreamingMode(bytes.length);
            try (java.io.OutputStream out = c.getOutputStream()) { out.write(bytes); }
            if (paused.getAsBoolean() || c.getResponseCode() != 200) throw new IOException();
            try (InputStream in = c.getInputStream()) { return response(in, paused); }
        } finally { c.disconnect(); }
    }
    private static JSONObject response(InputStream in, BooleanSupplier paused) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int count;
        while ((count = in.read(buffer)) != -1) {
            if (paused.getAsBoolean() || out.size() + count > 65536) throw new IOException();
            out.write(buffer, 0, count);
        }
        JSONObject result = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
        if (!result.optBoolean("ok", false)) throw new IOException();
        return result;
    }
}
