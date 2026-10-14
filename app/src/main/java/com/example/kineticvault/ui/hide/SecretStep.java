package com.example.kineticvault.ui.hide;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.kineticvault.R;
import com.example.kineticvault.core.Constants;
import com.google.android.material.progressindicator.LinearProgressIndicator;

final class SecretStep {
    private final HideActivity host;
    private final HideWork work;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable measureLater = this::measure;
    private final EditText text, passphrase;
    private final CheckBox compression, ghost, usePassphrase;
    private final RadioGroup type;
    private final TextView capacity;
    private final LinearProgressIndicator meter;
    private final ActivityResultLauncher<String[]> picker;
    private final TextImport importer;
    private final ActivityResultLauncher<String[]> textPicker;
    private Uri file;
    private int version;
    private boolean active, busy, fits;

    SecretStep(HideActivity host, HideWork work) {
        this.host = host; this.work = work;
        text = host.findViewById(R.id.hide_secret_text);
        passphrase = host.findViewById(R.id.hide_passphrase);
        compression = host.findViewById(R.id.hide_compress);
        ghost = host.findViewById(R.id.hide_ghost);
        usePassphrase = host.findViewById(R.id.hide_use_passphrase);
        type = host.findViewById(R.id.hide_secret_type);
        capacity = host.findViewById(R.id.hide_capacity_text);
        meter = host.findViewById(R.id.hide_capacity_meter);
        text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.MAX_PAYLOAD_BYTES / 4)});
        passphrase.setFilters(new InputFilter[]{new InputFilter.LengthFilter(Constants.MAX_PASSPHRASE_CHARS)});
        picker = host.registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                file = uri;
                ((TextView) host.findViewById(R.id.hide_file_status)).setText(R.string.hide_file_selected);
                schedule();
            }
        });
        importer = new TextImport(host);
        textPicker = host.registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) importer.read(uri, value -> {
                type.check(R.id.hide_type_text);
                text.setText(value, 0, value.length); schedule();
            });
        });
        host.findViewById(R.id.hide_import_text).setOnClickListener(v -> {
            try { textPicker.launch(new String[]{"text/plain"}); }
            catch (RuntimeException error) { host.message(R.string.hide_picker_error); }
        });
        bind();
    }

    private void bind() {
        text.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            public void afterTextChanged(Editable value) { schedule(); }
        });
        type.setOnCheckedChangeListener((group, id) -> {
            text.setVisibility(isFile() ? View.GONE : View.VISIBLE);
            host.findViewById(R.id.hide_file_controls).setVisibility(isFile() ? View.VISIBLE : View.GONE);
            if (isFile()) text.setText("");
            schedule();
        });
        compression.setOnCheckedChangeListener((button, checked) -> schedule());
        ghost.setOnCheckedChangeListener((button, checked) -> schedule());
        usePassphrase.setOnCheckedChangeListener((button, checked) -> {
            passphrase.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (!checked) passphrase.setText("");
        });
        host.findViewById(R.id.hide_choose_file).setOnClickListener(v -> {
            try { picker.launch(new String[]{"*/*"}); }
            catch (RuntimeException error) { host.message(R.string.hide_picker_error); }
        });
        host.findViewById(R.id.hide_secret_next).setOnClickListener(v -> prepare());
    }

    void activate() { active = true; setBusy(false); schedule(); }

    void deactivate() {
        active = false; version++;
        main.removeCallbacks(measureLater);
        setBusy(false);
    }

    private boolean isFile() { return type.getCheckedRadioButtonId() == R.id.hide_type_file; }

    private boolean hasInput() { return isFile() ? file != null : text.length() > 0; }

    private void schedule() {
        version++;
        fits = false;
        host.findViewById(R.id.hide_secret_next).setEnabled(false);
        main.removeCallbacks(measureLater);
        if (!active || busy) return;
        capacity.setText(hasInput() ? R.string.hide_capacity_measuring : R.string.hide_capacity_hint);
        meter.setProgressCompat(0, false);
        if (hasInput()) main.postDelayed(measureLater, 300);
    }

    private SecretInput snapshot() {
        return new SecretInput(isFile() ? null : copy(text), isFile() ? file : null,
                compression.isChecked(), ghost.isChecked());
    }

    private void measure() {
        if (!active || busy || !hasInput()) return;
        int request = version;
        work.measure(snapshot(), false, new HideWork.Callback<HideStore.Capacity>() {
            public void success(HideStore.Capacity result) {
                if (request != version || !active) return;
                showCapacity(result);
            }
            public void failure(int message) {
                if (request != version || !active) return;
                capacity.setText(message);
            }
        });
    }

    private void showCapacity(HideStore.Capacity result) {
        fits = result.fits();
        capacity.setText(host.getString(fits ? R.string.hide_capacity_fits : R.string.hide_capacity_over,
                result.needed, result.available));
        meter.setIndicatorColor(host.getColor(fits ? R.color.kv_primary : R.color.kv_danger));
        meter.setProgressCompat(result.available == 0 ? 1000
                : (int) Math.min(1000L, result.needed * 1000L / result.available), false);
        host.findViewById(R.id.hide_secret_next).setEnabled(fits && !busy);
    }

    private void prepare() {
        if (!fits || busy || !hasInput() || !validPassphrase()) return;
        main.removeCallbacks(measureLater);
        int request = ++version;
        setBusy(true);
        capacity.setText(R.string.hide_preparing_secret);
        work.measure(snapshot(), true, new HideWork.Callback<HideStore.Capacity>() {
            public void success(HideStore.Capacity result) {
                if (request != version || !active) return;
                setBusy(false); showCapacity(result);
                if (result.fits()) host.showStep(2);
            }
            public void failure(int message) {
                if (request != version || !active) return;
                setBusy(false); capacity.setText(message); host.message(message);
            }
        });
    }

    boolean validPassphrase() {
        if (usePassphrase.isChecked() && passphrase.length() == 0) {
            passphrase.setError(host.getString(R.string.hide_passphrase_required));
            host.message(R.string.hide_passphrase_required);
            return false;
        }
        return true;
    }

    boolean hasPassphrase() { return usePassphrase.isChecked() && passphrase.length() > 0; }

    char[] takePassphrase() {
        char[] result = usePassphrase.isChecked() ? copy(passphrase) : new char[0];
        passphrase.setText("");
        return result;
    }

    void close() { importer.close(); }
    void clearSensitiveUi() { importer.cancel(); text.setText(""); passphrase.setText(""); }

    private static char[] copy(EditText view) {
        char[] result = new char[view.length()];
        view.getText().getChars(0, result.length, result, 0);
        return result;
    }

    private void setBusy(boolean value) {
        busy = value;
        int[] ids = {R.id.hide_secret_text, R.id.hide_passphrase, R.id.hide_type_text,
                R.id.hide_type_file, R.id.hide_choose_file, R.id.hide_import_text, R.id.hide_compress,
                R.id.hide_ghost, R.id.hide_use_passphrase};
        for (int id : ids) host.findViewById(id).setEnabled(!value);
        host.findViewById(R.id.hide_secret_next).setEnabled(!value && fits);
    }
}
