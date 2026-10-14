package com.example.kineticvault.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import com.example.kineticvault.R;

/** Combines the existing image renderer with accessible keyboard and touch control. */
public final class WipeSliderView extends LinearLayout {
    private final WipeComparisonView comparison;
    public WipeSliderView(Context context) { this(context, null); }
    public WipeSliderView(Context context, AttributeSet attrs) {
        super(context, attrs); setOrientation(VERTICAL);
        comparison = new WipeComparisonView(context);
        addView(comparison, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        SeekBar slider = new SeekBar(context);
        slider.setMax(100); slider.setProgress(50);
        slider.setContentDescription(context.getString(R.string.hide_wipe_description));
        addView(slider, new LayoutParams(LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.touch_min)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean user) { comparison.setFraction(value / 100f); }
            public void onStartTrackingTouch(SeekBar bar) { }
            public void onStopTrackingTouch(SeekBar bar) {
                bar.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            }
        });
    }
    public void setImages(Bitmap before, Bitmap after) { comparison.setImages(before, after); }
}
