package com.example.kineticvault.ui.vault;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.text.format.DateFormat;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import com.example.kineticvault.R;
import com.example.kineticvault.data.VaultEntry;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Calendar;
import java.util.function.Consumer;

final class VaultDialogs {
    private VaultDialogs() { }

    static void rename(VaultActivity host, VaultEntry entry, Consumer<String> save) {
        EditText input = new EditText(host);
        input.setHint(R.string.vault_title_hint); input.setText(entry.title);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        input.setMinHeight((int) (48 * host.getResources().getDisplayMetrics().density));
        input.setSaveEnabled(false); input.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        AlertDialog dialog = new MaterialAlertDialogBuilder(host).setTitle(R.string.vault_rename)
                .setMessage(R.string.vault_title_note).setView(input)
                .setNegativeButton(R.string.hide_cancel, null).setPositiveButton(R.string.vault_save, null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String title = input.getText().toString().trim();
            if (title.isEmpty()) { input.setError(host.getString(R.string.vault_title_required)); return; }
            save.accept(title); dialog.dismiss();
        }));
        dialog.show();
    }

    static void expiry(VaultActivity host, VaultEntry entry, Consumer<Long> save) {
        new MaterialAlertDialogBuilder(host).setTitle(R.string.vault_set_expiry)
                .setMessage(R.string.vault_expiry_note).setNegativeButton(R.string.hide_cancel, null)
                .setNeutralButton(R.string.vault_clear_expiry, (dialog, which) -> save.accept(null))
                .setPositiveButton(R.string.vault_choose_date, (dialog, which) -> expiryDate(host, entry, save)).show();
    }

    private static void expiryDate(VaultActivity host, VaultEntry entry, Consumer<Long> save) {
        Calendar date = Calendar.getInstance();
        if (entry.expiresAt != null) date.setTimeInMillis(entry.expiresAt);
        chooseDate(host, date, selected -> new TimePickerDialog(host, (picker, hour, minute) -> {
            selected.set(Calendar.HOUR_OF_DAY, hour); selected.set(Calendar.MINUTE, minute);
            selected.set(Calendar.SECOND, 0); selected.set(Calendar.MILLISECOND, 0);
            save.accept(selected.getTimeInMillis());
        }, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE), DateFormat.is24HourFormat(host)).show());
    }

    static void range(VaultActivity host, Consumer<long[]> save) {
        Calendar today = Calendar.getInstance();
        chooseDate(host, today, start -> {
            DatePickerDialog end = dateDialog(host, start, finish -> {
                finish.add(Calendar.DAY_OF_MONTH, 1);
                save.accept(new long[]{start.getTimeInMillis(), finish.getTimeInMillis()});
            });
            end.setTitle(R.string.vault_range_end); end.getDatePicker().setMinDate(start.getTimeInMillis()); end.show();
        }, R.string.vault_range_start);
    }

    private static void chooseDate(VaultActivity host, Calendar date, Consumer<Calendar> selected) {
        chooseDate(host, date, selected, R.string.vault_choose_date);
    }

    private static void chooseDate(VaultActivity host, Calendar date, Consumer<Calendar> selected, int title) {
        DatePickerDialog dialog = dateDialog(host, date, selected); dialog.setTitle(title); dialog.show();
    }

    private static DatePickerDialog dateDialog(VaultActivity host, Calendar date, Consumer<Calendar> selected) {
        return new DatePickerDialog(host, (picker, year, month, day) -> {
            Calendar value = Calendar.getInstance(); value.clear(); value.set(year, month, day);
            selected.accept(value);
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH));
    }
}
