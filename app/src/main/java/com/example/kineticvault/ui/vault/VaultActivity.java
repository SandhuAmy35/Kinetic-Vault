package com.example.kineticvault.ui.vault;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.ContextMenu;
import android.view.HapticFeedbackConstants;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;
import com.example.kineticvault.data.VaultEntry;
import com.example.kineticvault.data.VaultRepository;
import com.example.kineticvault.ui.hide.HideActivity;
import com.example.kineticvault.ui.reveal.RevealActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class VaultActivity extends AppCompatActivity {
    private static final int RENAME = 1, EXPIRY = 2, DELETE = 3;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable search = this::load;
    private VaultRepository repository;
    private VaultAdapter adapter;
    private EditText query;
    private View root;
    private Long from, to;
    private VaultEntry selected;
    private int generation;
    private boolean closed;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this); setContentView(R.layout.activity_vault);
        root = findViewById(R.id.vault_root); query = findViewById(R.id.vault_search);
        repository = new VaultRepository(getApplicationContext()); adapter = new VaultAdapter(this);
        if (state != null) {
            if (state.containsKey("from")) { from = state.getLong("from"); to = state.getLong("to"); }
            query.setText(state.getString("query", ""));
        }
        insets(); bindList(); bindFilters(); rangeLabel();
    }

    private void insets() {
        int spacing = getResources().getDimensionPixelSize(R.dimen.space_md);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(spacing + safe.left, safe.top, spacing + safe.right, safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void bindList() {
        ListView list = findViewById(R.id.vault_list); list.setAdapter(adapter); registerForContextMenu(list);
        list.setOnItemClickListener((parent, view, position, id) -> open(adapter.getItem(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); return false;
        });
        findViewById(R.id.vault_back).setOnClickListener(view -> finish());
        findViewById(R.id.vault_empty_action).setOnClickListener(view -> {
            if (filtered()) clearFilters(); else startActivity(new Intent(this, HideActivity.class));
        });
        findViewById(R.id.vault_retry).setOnClickListener(view -> load());
    }

    private void bindFilters() {
        findViewById(R.id.vault_filter).setOnClickListener(view -> VaultDialogs.range(this, range -> {
            from = range[0]; to = range[1]; rangeLabel(); load();
        }));
        findViewById(R.id.vault_clear_filter).setOnClickListener(view -> clearFilters());
        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                generation++; rangeLabel(); main.removeCallbacks(search); main.postDelayed(search, 250);
            }
            public void afterTextChanged(Editable text) { }
        });
    }

    private void load() {
        if (closed) return;
        int ticket = ++generation; String text = query.getText().toString(); Long start = from, end = to;
        findViewById(R.id.vault_progress).setVisibility(View.VISIBLE);
        findViewById(R.id.vault_error).setVisibility(View.GONE);
        worker.execute(() -> {
            try {
                List<VaultEntry> entries = repository.search(text, start, end);
                main.post(() -> { if (!closed && ticket == generation) show(entries); });
            } catch (RuntimeException | OutOfMemoryError error) {
                main.post(() -> { if (!closed && ticket == generation) loadFailed(); });
            }
        });
    }

    private void show(List<VaultEntry> entries) {
        findViewById(R.id.vault_progress).setVisibility(View.GONE); adapter.replace(entries);
        findViewById(R.id.vault_empty).setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.vault_list).setVisibility(entries.isEmpty() ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.vault_empty_title)).setText(filtered() ? R.string.vault_no_matches : R.string.vault_empty_title);
        ((TextView) findViewById(R.id.vault_empty_text)).setText(filtered() ? R.string.vault_no_matches_text : R.string.vault_empty_text);
        ((TextView) findViewById(R.id.vault_empty_action)).setText(filtered() ? R.string.vault_clear_filters : R.string.home_hide);
    }

    private void loadFailed() {
        findViewById(R.id.vault_progress).setVisibility(View.GONE);
        findViewById(R.id.vault_error).setVisibility(View.VISIBLE);
        findViewById(R.id.vault_empty).setVisibility(View.GONE);
        findViewById(R.id.vault_list).setVisibility(View.GONE);
    }

    private boolean filtered() { return query.length() > 0 || from != null; }

    private void clearFilters() {
        from = null; to = null; query.setText(""); main.removeCallbacks(search); rangeLabel(); load();
    }

    private void rangeLabel() {
        TextView label = findViewById(R.id.vault_range);
        label.setText(from == null ? getString(R.string.vault_all_dates) : getString(R.string.vault_range_label,
                DateFormat.getMediumDateFormat(this).format(new Date(from)),
                DateFormat.getMediumDateFormat(this).format(new Date(to - 1))));
        findViewById(R.id.vault_clear_filter).setVisibility(filtered() ? View.VISIBLE : View.GONE);
    }

    private void open(VaultEntry entry) {
        try { startActivity(new Intent(this, RevealActivity.class).setData(Uri.parse(entry.imageUri))); }
        catch (RuntimeException error) { failure(R.string.vault_open_error); }
    }

    @Override public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo info) {
        super.onCreateContextMenu(menu, view, info);
        AdapterView.AdapterContextMenuInfo row = (AdapterView.AdapterContextMenuInfo) info;
        selected = adapter.getItem(row.position); menu.setHeaderTitle(selected.title);
        menu.add(0, RENAME, 0, R.string.vault_rename); menu.add(0, EXPIRY, 1, R.string.vault_set_expiry);
        menu.add(0, DELETE, 2, R.string.vault_delete);
    }

    @Override public boolean onContextItemSelected(MenuItem item) {
        VaultEntry entry = selected;
        if (entry == null) return super.onContextItemSelected(item);
        if (item.getItemId() == RENAME) VaultDialogs.rename(this, entry,
                title -> change(() -> repository.rename(entry.uuid, title)));
        else if (item.getItemId() == EXPIRY) VaultDialogs.expiry(this, entry,
                expiry -> change(() -> repository.setExpiry(entry.uuid, expiry)));
        else if (item.getItemId() == DELETE) new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.vault_delete).setMessage(R.string.vault_delete_note)
                .setNegativeButton(R.string.hide_cancel, null)
                .setPositiveButton(R.string.vault_delete, (dialog, which) -> change(() -> repository.softDelete(entry.uuid))).show();
        else return super.onContextItemSelected(item);
        return true;
    }

    private void change(Runnable mutation) {
        worker.execute(() -> {
            try {
                mutation.run();
                main.post(() -> { if (!closed) {
                    root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); load();
                    Snackbar.make(root, R.string.vault_updated, Snackbar.LENGTH_SHORT).show();
                } });
            } catch (RuntimeException error) { main.post(() -> { if (!closed) failure(R.string.vault_update_error); }); }
        });
    }

    private void failure(int message) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).setAction(R.string.vault_retry, view -> load()).show();
    }

    @Override protected void onResume() { super.onResume(); load(); }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("query", query.getText().toString());
        if (from != null) { state.putLong("from", from); state.putLong("to", to); }
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        closed = true; main.removeCallbacks(search); adapter.close();
        worker.execute(repository::close); worker.shutdown(); super.onDestroy();
    }
}
