package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

@SuppressLint("ViewConstructor")
public final class ExpandableSettingsCell extends FrameLayout {

    private final Theme.ResourcesProvider resourcesProvider;
    private final ImageView iconView;
    private final LinearLayout textLayout;
    private final TextView textView;
    private final AnimatedTextView countView;
    private final ImageView arrowView;
    private final Switch switchView;
    private final CheckBox2 checkBoxView;
    private UItem item;
    private boolean group;
    private boolean divider;

    public ExpandableSettingsCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setFocusable(true);

        iconView = new ImageView(context);
        addView(iconView, LayoutHelper.createFrameRelatively(24, 24, Gravity.START | Gravity.CENTER_VERTICAL, 20, 0, 20, 0));

        textView = new AppCompatTextView(context);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        countView = new AnimatedTextView(context, false, true, true);
        countView.setAnimationProperties(.35f, 0, 200, CubicBezierInterpolator.EASE_OUT_QUINT);
        countView.setTypeface(AndroidUtilities.bold());
        countView.setTextSize(dp(14));
        countView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        arrowView = new ImageView(context);
        arrowView.setImageResource(R.drawable.arrow_more);
        arrowView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);

        textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.HORIZONTAL);
        textLayout.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        if (LocaleController.isRTL) {
            textLayout.addView(arrowView, LayoutHelper.createLinear(16, 16, 0, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));
            textLayout.addView(countView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.CENTER_VERTICAL, 0, 0, 6, 0));
            textLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));
        } else {
            textLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));
            textLayout.addView(countView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));
            textLayout.addView(arrowView, LayoutHelper.createLinear(16, 16, 0, Gravity.CENTER_VERTICAL, 2, 0, 0, 0));
        }
        addView(textLayout);

        switchView = new Switch(context, resourcesProvider);
        switchView.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        switchView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(switchView, LayoutHelper.createFrameRelatively(37, 50, Gravity.END | Gravity.CENTER_VERTICAL, 19, 0, 19, 0));

        checkBoxView = new CheckBox2(context, 21, resourcesProvider);
        checkBoxView.setColor(Theme.key_radioBackgroundChecked, Theme.key_checkboxDisabled, Theme.key_checkboxCheck);
        checkBoxView.setDrawUnchecked(true);
        checkBoxView.setDrawBackgroundAsArc(10);
        checkBoxView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(checkBoxView, LayoutHelper.createFrameRelatively(21, 21, Gravity.START | Gravity.CENTER_VERTICAL, 64, 0, 0, 0));
    }

    private void set(UItem item, boolean divider) {
        boolean animated = this.item != null && this.item.id == item.id && this.item.object == item.object;
        this.item = item;
        this.divider = divider;
        group = isGroup(item);
        textView.setText(item.text);
        textLayout.setLayoutParams(LayoutHelper.createFrameRelatively(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, getTextInset(), 0, group ? 79 : 21, 0));
        iconView.setVisibility(group && item.iconResId != 0 ? VISIBLE : GONE);
        switchView.setVisibility(group ? VISIBLE : GONE);
        countView.setVisibility(group ? VISIBLE : GONE);
        arrowView.setVisibility(group ? VISIBLE : GONE);
        checkBoxView.setVisibility(group ? GONE : VISIBLE);
        if (group) {
            if (item.iconResId != 0) {
                iconView.setImageResource(item.iconResId);
            }
            countView.setText(item.intValue + "/" + item.longValue, animated && !LocaleController.isRTL);
            switchView.setChecked(item.checked, animated);
            arrowView.animate().cancel();
            if (animated) {
                arrowView.animate().rotation(item.collapsed ? 0 : 180).setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT).setDuration(240).start();
            } else {
                arrowView.setRotation(item.collapsed ? 0 : 180);
            }
        } else {
            checkBoxView.setChecked(item.checked, animated);
        }
        int textColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider);
        textView.setTextColor(textColor);
        countView.setTextColor(textColor);
        arrowView.setColorFilter(new PorterDuffColorFilter(textColor, PorterDuff.Mode.MULTIPLY));
        iconView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon, resourcesProvider), PorterDuff.Mode.MULTIPLY));
        setWillNotDraw(!divider && !group);
        invalidate();
    }

    private int getTextInset() {
        return group ? item.iconResId != 0 ? 64 : 21 : 105;
    }

    public static boolean isGroup(UItem item) {
        return item.instanceOf(Factory.class) && item.clickCallback != null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(dp(50), MeasureSpec.EXACTLY));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (group) {
            float x = LocaleController.isRTL ? dp(75) : getWidth() - dp(75);
            canvas.drawRect(x - dp(.66f), (getHeight() - dp(20)) / 2f, x, (getHeight() + dp(20)) / 2f, Theme.dividerPaint);
        }
        if (divider) {
            int inset = dp(getTextInset());
            canvas.drawLine(LocaleController.isRTL ? 0 : inset, getHeight() - 1, LocaleController.isRTL ? getWidth() - inset : getWidth(), getHeight() - 1, Theme.dividerPaint);
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(group ? "android.widget.Switch" : "android.widget.CheckBox");
        info.setCheckable(true);
        AccessibilityNodeInfoCompat.wrap(info).setChecked(item.checked ? AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE : AccessibilityNodeInfoCompat.CHECKED_STATE_FALSE);
        info.setContentDescription(group ? item.text + "\n" + LocaleController.formatString(R.string.Of, item.intValue, item.longValue) : item.text);
        if (group) {
            info.setStateDescription(getString(item.collapsed ? R.string.AccDescrCollapsed : R.string.AccDescrExpanded));
            info.addAction(item.collapsed ? AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND : AccessibilityNodeInfo.AccessibilityAction.ACTION_COLLAPSE);
        }
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (group && (action == AccessibilityNodeInfo.ACTION_EXPAND || action == AccessibilityNodeInfo.ACTION_COLLAPSE)) {
            item.clickCallback.onClick(this);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    public static final class Factory extends UItem.UItemFactory<ExpandableSettingsCell> {
        static { setup(new Factory()); }

        @Override
        public ExpandableSettingsCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new ExpandableSettingsCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            ((ExpandableSettingsCell) view).set(item, divider);
        }

        @Override
        public boolean equals(UItem a, UItem b) {
            return a.id == b.id && a.object == b.object;
        }

        public static UItem group(int id, int iconResId, CharSequence title, int enabled, int total, boolean expanded, View.OnClickListener onExpand) {
            UItem item = UItem.ofFactory(Factory.class);
            item.id = id;
            item.iconResId = iconResId;
            item.text = title;
            item.intValue = enabled;
            item.longValue = total;
            return item.setChecked(enabled > 0).setCollapsed(!expanded).setClickCallback(onExpand);
        }

        public static UItem option(CharSequence title, boolean checked) {
            UItem item = UItem.ofFactory(Factory.class);
            item.text = title;
            return item.setChecked(checked);
        }
    }
}
