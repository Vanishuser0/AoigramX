package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.SizeNotifierFrameLayout;

import nu.gpu.aoigram.config.AoiConfig;

@SuppressLint("ViewConstructor")
public final class GroupMessageMenuCell extends LinearLayout {

    private static final float SCALE = .5f;
    private static final int[] LIST_ICONS = {R.drawable.menu_reply, R.drawable.msg_copy, R.drawable.msg_forward};
    private static final int[] LIST_LABELS = {44, 40, 48};
    private static final int[] GROUPED_ICONS = {R.drawable.msg_translate, R.drawable.msg_info};
    private static final int[] GROUPED_LABELS = {64, 48};
    private static final int[] BAR_ICONS = {R.drawable.menu_reply, R.drawable.msg_copy, R.drawable.msg_link, R.drawable.msg_forward};

    public GroupMessageMenuCell(Context context, BaseFragment fragment) {
        super(context);
        setOrientation(VERTICAL);
        HeaderCell headerCell = new HeaderCell(context);
        headerCell.setText(getString(R.string.GroupMessageMenu));
        addView(headerCell);
        LinearLayout optionsLayout = new LinearLayout(context);
        optionsLayout.setPadding(dp(21), dp(10), dp(21), 0);
        addView(optionsLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 139));
        OptionView[] options = new OptionView[2];
        for (int i = 0; i < options.length; i++) {
            boolean grouped = i == 1;
            options[i] = new OptionView(context, fragment, grouped);
            optionsLayout.addView(options[i], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, .5f, grouped ? 10 : 0, 0, 0, 0));
            ScaleStateListAnimator.apply(options[i].preview, .03f, 1.5f);
            options[i].preview.setOnClickListener(v -> {
                AoiConfig.groupMessageMenu.set(grouped);
                for (OptionView option : options) {
                    option.preview.setChecked(option.preview == v, true);
                }
            });
        }
    }

    @SuppressLint("ViewConstructor")
    private static final class OptionView extends FrameLayout {

        private final String title;
        private final MenuPreview preview;
        private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        OptionView(Context context, BaseFragment fragment, boolean grouped) {
            super(context);
            setWillNotDraw(false);
            title = getString(grouped ? R.string.GroupMessageMenuGrouped : R.string.GroupMessageMenuList);
            textPaint.setTextSize(dp(13));
            preview = new MenuPreview(context, fragment, grouped);
            preview.setContentDescription(title);
            addView(preview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 90));
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            canvas.drawText(title, (getMeasuredWidth() - textPaint.measureText(title)) / 2, dp(112), textPaint);
        }
    }

    @SuppressLint("ViewConstructor")
    private static final class MenuPreview extends SizeNotifierFrameLayout {

        private final boolean grouped;
        private final Drawable background;
        private final Rect backgroundPadding = new Rect();
        private final Drawable[] rowIcons;
        private final Drawable[] barIcons = new Drawable[BAR_ICONS.length];
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final AnimatedFloat progress = new AnimatedFloat(this, 250, CubicBezierInterpolator.EASE_OUT_QUINT);
        private boolean checked;

        @SuppressWarnings("DataFlowIssue")
        MenuPreview(Context context, BaseFragment fragment, boolean grouped) {
            super(context, fragment.getParentLayout());
            this.grouped = grouped;
            setOccupyStatusBar(false);
            setBackgroundImage(Theme.getCachedWallpaper(), false);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(10));
                }
            });
            setClipToOutline(true);
            background = ContextCompat.getDrawable(context, R.drawable.popup_fixed_alert4).mutate();
            background.getPadding(backgroundPadding);
            int[] icons = grouped ? GROUPED_ICONS : LIST_ICONS;
            rowIcons = new Drawable[icons.length];
            for (int i = 0; i < icons.length; i++) {
                rowIcons[i] = ContextCompat.getDrawable(context, icons[i]).mutate();
            }
            for (int i = 0; i < barIcons.length; i++) {
                barIcons[i] = ContextCompat.getDrawable(context, BAR_ICONS[i]).mutate();
            }
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(dp(2));
            setChecked(AoiConfig.groupMessageMenu.get() == grouped, false);
        }

        void setChecked(boolean checked, boolean animated) {
            this.checked = checked;
            progress.set(checked ? 1 : 0, !animated);
            invalidate();
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
                getParent().requestDisallowInterceptTouchEvent(false);
            }
            return super.dispatchTouchEvent(ev);
        }

        @SuppressLint("WrongConstant")
        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName(RadioButton.class.getName());
            info.setCheckable(true);
            AccessibilityNodeInfoCompat.wrap(info).setChecked(checked ? AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE : AccessibilityNodeInfoCompat.CHECKED_STATE_FALSE);
        }

        @Override
        protected boolean useRootView() {
            return false;
        }

        @Override
        protected boolean isActionBarVisible() {
            return false;
        }

        @Override
        protected void dispatchDraw(@NonNull Canvas canvas) {
            super.dispatchDraw(canvas);
            int[] labels = grouped ? GROUPED_LABELS : LIST_LABELS;
            int width = dp(200);
            int height = dp(48 * labels.length + (grouped ? 60 : 0));
            canvas.save();
            canvas.translate((getMeasuredWidth() - width * SCALE) / 2f, (getMeasuredHeight() - height * SCALE) / 2f);
            canvas.scale(SCALE, SCALE);
            background.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), PorterDuff.Mode.MULTIPLY));
            background.setBounds(-backgroundPadding.left, -backgroundPadding.top, width + backgroundPadding.right, height + backgroundPadding.bottom);
            background.draw(canvas);

            int iconColor = Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon);
            paint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem), 0x40));
            for (int i = 0; i < labels.length; i++) {
                int cy = dp(48 * i + 24);
                drawIcon(canvas, rowIcons[i], dp(30), cy, iconColor);
                rect.set(dp(60), cy - dp(5), dp(60 + labels[i]), cy + dp(5));
                canvas.drawRoundRect(rect, dp(5), dp(5), paint);
            }
            if (grouped) {
                int y = dp(48 * labels.length);
                paint.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuSeparator));
                canvas.drawRect(0, y, width, y + dp(8), paint);
                for (int i = 0; i < barIcons.length; i++) {
                    drawIcon(canvas, barIcons[i], dp(4) + (width - dp(8)) * (i + .5f) / barIcons.length, y + dp(34), iconColor);
                }
            }
            canvas.restore();

            float t = progress.set(checked ? 1 : 0);
            if (t <= 0) return;
            strokePaint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_radioBackgroundChecked), (int) (255 * t)));
            rect.set(dp(1), dp(1), getMeasuredWidth() - dp(1), getMeasuredHeight() - dp(1));
            canvas.drawRoundRect(rect, dp(9), dp(9), strokePaint);
        }

        private void drawIcon(Canvas canvas, Drawable drawable, float cx, float cy, int color) {
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            drawable.setBounds((int) cx - dp(12), (int) cy - dp(12), (int) cx + dp(12), (int) cy + dp(12));
            drawable.draw(canvas);
        }
    }
}
