package com.example.kineticvault.ui.vault;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.example.kineticvault.R;
import com.example.kineticvault.data.VaultEntry;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class VaultAdapter extends BaseAdapter implements AutoCloseable {
    private final Context context;
    private final List<VaultEntry> entries = new ArrayList<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> images = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getAllocationByteCount(); }
    };
    private volatile boolean closed;

    VaultAdapter(Context context) { this.context = context; }

    void replace(List<VaultEntry> rows) {
        entries.clear(); entries.addAll(rows); notifyDataSetChanged();
    }

    @Override public int getCount() { return entries.size(); }
    @Override public VaultEntry getItem(int position) { return entries.get(position); }
    @Override public long getItemId(int position) { return getItem(position).id; }
    @Override public boolean hasStableIds() { return true; }

    @Override public View getView(int position, View recycled, ViewGroup parent) {
        View row = recycled == null ? LayoutInflater.from(context).inflate(R.layout.vault_row, parent, false) : recycled;
        VaultEntry entry = getItem(position);
        ((TextView) row.findViewById(R.id.vault_row_title)).setText(entry.title);
        ((TextView) row.findViewById(R.id.vault_row_date)).setText(date(entry.createdAt));
        ((TextView) row.findViewById(R.id.vault_row_size)).setText(entry.payloadType == 0
                ? context.getString(R.string.vault_size_unknown)
                : context.getString(R.string.vault_size, Formatter.formatFileSize(context, entry.payloadBytes),
                        Formatter.formatFileSize(context, entry.capacityBytes)));
        expiry(row.findViewById(R.id.vault_row_expiry), entry);
        thumbnail(row.findViewById(R.id.vault_row_thumbnail), entry);
        return row;
    }

    private String date(long timestamp) {
        Date date = new Date(timestamp);
        return context.getString(R.string.vault_date_time, DateFormat.getMediumDateFormat(context).format(date),
                DateFormat.getTimeFormat(context).format(date));
    }

    private void expiry(TextView view, VaultEntry entry) {
        view.setVisibility(entry.expiresAt == null ? View.GONE : View.VISIBLE);
        if (entry.expiresAt != null) view.setText(context.getString(entry.expiresAt <= System.currentTimeMillis()
                ? R.string.vault_expired : R.string.vault_expires, date(entry.expiresAt)));
    }

    private void thumbnail(ImageView view, VaultEntry entry) {
        view.setTag(entry.uuid);
        view.setImageResource(R.drawable.ic_lock);
        Bitmap cached = images.get(entry.uuid);
        if (cached != null) { view.setImageBitmap(cached); return; }
        if (entry.thumb == null || closed) return;
        worker.execute(() -> {
            if (closed) return;
            Bitmap bitmap;
            try { bitmap = BitmapFactory.decodeByteArray(entry.thumb, 0, entry.thumb.length); }
            catch (RuntimeException | OutOfMemoryError error) { return; }
            if (bitmap == null) return;
            main.post(() -> {
                if (closed) { bitmap.recycle(); return; }
                images.put(entry.uuid, bitmap);
                if (entry.uuid.equals(view.getTag())) view.setImageBitmap(bitmap);
            });
        });
    }

    @Override public void close() { closed = true; worker.shutdown(); images.evictAll(); }
}
