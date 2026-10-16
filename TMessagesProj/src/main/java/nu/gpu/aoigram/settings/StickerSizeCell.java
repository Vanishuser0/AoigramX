package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.SeekBarView;

import nu.gpu.aoigram.config.AoiConfig;

@SuppressLint("ViewConstructor")
public final class StickerSizeCell extends FrameLayout {

    private static final int MIN = 30;
    private static final int MAX = 130;
    private static final int OFFICIAL = 100;
    private static final int SNAP = 3;

    private final AnimatedTextView valueView;
    private final ImageView resetButton;
    private final SeekBarView seekBarView;
    private final Runnable onChanged;

    public StickerSizeCell(Context context, Runnable onChanged) {
        super(context);
        this.onChanged = onChanged;

        LinearLayout headerLayout = new LinearLayout(context);
        headerLayout.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);

        TextView titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        titleView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        titleView.setMinHeight(dp(33));
        titleView.setText(getString(R.string.StickerSize));
        headerLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        valueView = new AnimatedTextView(context, false, true, true) {
            private final Drawable backgroundDrawable = Theme.createRoundRectDrawable(dp(4), Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader), 0.15f));

            @Override
            protected void onDraw(@NonNull Canvas canvas) {
                backgroundDrawable.setBounds(0, 0, (int) (getPaddingLeft() + getDrawable().getCurrentWidth() + getPaddingRight()), getMeasuredHeight());
                backgroundDrawable.draw(canvas);
                super.onDraw(canvas);
            }
        };
        valueView.setAnimationProperties(.45f, 0, 240, CubicBezierInterpolator.EASE_OUT_QUINT);
        valueView.getDrawable().setAllowCancel(true);
        valueView.setTypeface(AndroidUtilities.bold());
        valueView.setPadding(dp(5.33f), dp(2), dp(5.33f), dp(2));
        valueView.setTextSize(dp(12));
        valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        headerLayout.addView(valueView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 17, Gravity.CENTER_VERTICAL, 6, 1, 0, 0));
        addView(headerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 20, 7, 20, 0));

        FrameLayout labelsView = new FrameLayout(context);
        labelsView.addView(createLabel(context, R.string.StickerSizeSmall), LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL));
        labelsView.addView(createLabel(context, R.string.StickerSizeLarge), LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        addView(labelsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 21, 52, 21, 0));

        seekBarView = new SeekBarView(context, true, null);
        seekBarView.setReportChanges(true);
        seekBarView.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                int size = MIN + Math.round(progress * (MAX - MIN));
                if (Math.abs(size - OFFICIAL) <= SNAP) {
                    size = OFFICIAL;
                    seekBarView.setProgress(getProgress(OFFICIAL));
                }
                if (size != AoiConfig.stickerSize.get()) {
                    setSize(size);
                    if (size == MIN || size == MAX || size == OFFICIAL) {
                        AndroidUtilities.vibrateCursor(seekBarView);
                    }
                }
            }

            @Override
            public CharSequence getContentDescription() {
                return getString(R.string.StickerSize) + " " + AoiConfig.stickerSize.get() + "%";
            }
        });
        seekBarView.setProgress(getProgress(AoiConfig.stickerSize.get()));
        addView(seekBarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38 + 6, Gravity.TOP, 6, 68, 6, 0));

        resetButton = new ImageView(context);
        resetButton.setScaleType(ImageView.ScaleType.FIT_XY);
        resetButton.setPadding(dp(11), dp(11), dp(11), dp(11));
        resetButton.setImageResource(R.drawable.msg_reset);
        resetButton.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        resetButton.setBackground(Theme.createSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader), .1f), Theme.RIPPLE_MASK_CIRCLE_20DP, dp(16)));
        resetButton.setContentDescription(getString(R.string.Reset));
        resetButton.setOnClickListener(v -> {
            ValueAnimator animator = ValueAnimator.ofFloat(AoiConfig.stickerSize.get(), AoiConfig.stickerSize.getDefaultValue());
            animator.setDuration(250);
            animator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            animator.addUpdateListener(a -> {
                float value = (float) a.getAnimatedValue();
                int size = Math.round(value);
                seekBarView.setProgress(getProgress(value));
                if (size != AoiConfig.stickerSize.get()) {
                    setSize(size);
                }
            });
            animator.start();
        });
        ScaleStateListAnimator.apply(resetButton);
        addView(resetButton, LayoutHelper.createFrame(40, 40, Gravity.TOP | (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT), 10, 4, 10, 0));

        updateValue(false);
    }

    private TextView createLabel(Context context, int textResId) {
        TextView textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        textView.setText(getString(textResId));
        return textView;
    }

    private static float getProgress(float size) {
        return (size - MIN) / (float) (MAX - MIN);
    }

    private void setSize(int size) {
        AoiConfig.stickerSize.set(size);
        updateValue(true);
        onChanged.run();
    }

    private void updateValue(boolean animated) {
        int size = AoiConfig.stickerSize.get();
        valueView.setText(size + "%", animated);
        AndroidUtilities.updateViewVisibilityAnimated(resetButton, size != AoiConfig.stickerSize.getDefaultValue(), 0.5f, animated);
    }
}
