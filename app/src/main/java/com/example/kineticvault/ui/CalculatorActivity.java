package com.example.kineticvault.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.kineticvault.R;
import com.example.kineticvault.data.VaultPreferences;
import java.math.BigDecimal;
import java.math.MathContext;

/** Immediate-execution calculator: enter the configured digits and press equals to open Home. */
public final class CalculatorActivity extends AppCompatActivity {
    private final StringBuilder input = new StringBuilder();
    private TextView display;
    private BigDecimal accumulator;
    private String operation;
    private boolean fresh = true, busy;
    private int generation;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); EdgeToEdge.enable(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_calculator);
        display = findViewById(R.id.calc_display);
        View root = findViewById(R.id.calc_root);
        int space = getResources().getDimensionPixelSize(R.dimen.space_md);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(space + safe.left, space + safe.top, space + safe.right, space + safe.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        bind((android.view.ViewGroup) findViewById(R.id.calc_keys)); reset();
    }
    private void bind(android.view.ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof android.view.ViewGroup) bind((android.view.ViewGroup) child);
            else if (child.getTag() != null) child.setOnClickListener(v -> press(v.getTag().toString()));
        }
    }
    private void press(String key) {
        if (busy) return;
        if ("C".equals(key)) { reset(); return; }
        if ("=".equals(key)) { equalsPressed(); return; }
        if ("+-*/".contains(key)) { operator(key); return; }
        if ("back".equals(key)) {
            if (!fresh && input.length() > 0) input.deleteCharAt(input.length() - 1);
            render(); return;
        }
        if (fresh) { wipeInput(); fresh = false; }
        if (input.length() >= 16 || (".".equals(key) && input.indexOf(".") >= 0)) return;
        if (".".equals(key) && input.length() == 0) input.append('0');
        input.append(key); render();
    }
    private void equalsPressed() {
        if (operation != null || fresh || input.length() < 6 || input.length() > 12
                || input.indexOf(".") >= 0 || !VaultPreferences.current().disguise) { calculate(); return; }
        char[] code = new char[input.length()]; input.getChars(0, input.length(), code, 0);
        busy = true; int ticket = ++generation;
        VaultPreferences.unlockDisguise(code, match -> {
            if (isDestroyed() || isFinishing() || ticket != generation) return;
            busy = false;
            if (match) {
                reset(); display.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                startActivity(new Intent(this, HomeActivity.class)); finish();
            } else calculate();
        });
    }
    private void operator(String next) {
        try {
            if (!fresh || accumulator == null) {
                BigDecimal value = number();
                accumulator = operation == null ? value : apply(accumulator, value);
                showNumber(accumulator);
            }
            operation = next; fresh = true;
        } catch (ArithmeticException | NumberFormatException error) { fail(); }
    }
    private void calculate() {
        try {
            BigDecimal value = number();
            if (operation != null && accumulator != null && !fresh) value = apply(accumulator, value);
            showNumber(value); accumulator = null; operation = null; fresh = true;
        } catch (ArithmeticException | NumberFormatException error) { fail(); }
    }
    private BigDecimal apply(BigDecimal left, BigDecimal right) {
        switch (operation) {
            case "+": return left.add(right, MathContext.DECIMAL64);
            case "-": return left.subtract(right, MathContext.DECIMAL64);
            case "*": return left.multiply(right, MathContext.DECIMAL64);
            default: return left.divide(right, MathContext.DECIMAL64);
        }
    }
    private BigDecimal number() { return input.length() == 0 ? BigDecimal.ZERO : new BigDecimal(input.toString()); }
    private void showNumber(BigDecimal value) { wipeInput(); input.append(value.stripTrailingZeros().toPlainString()); render(); }
    private void render() { display.setText(input.length() == 0 ? getString(R.string.calc_zero) : input); }
    private void wipeInput() { for (int i = 0; i < input.length(); i++) input.setCharAt(i, '\0'); input.setLength(0); }
    private void reset() { wipeInput(); accumulator = null; operation = null; fresh = true; render(); }
    private void fail() {
        reset(); display.setText(R.string.calc_error);
        display.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
    }
    @Override protected void onPause() { generation++; busy = false; reset(); super.onPause(); }
}
