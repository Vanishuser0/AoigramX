package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor;
import org.telegram.ui.Components.blur3.ViewGroupPartRenderer;
import org.telegram.ui.Components.blur3.capture.IBlur3Capture;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;

import java.util.ArrayList;

import nu.gpu.aoigram.config.BoolConfig;
import nu.gpu.aoigram.config.ConfigItem;
import nu.gpu.aoigram.config.StringConfig;

public abstract class BaseAoiSettingsActivity extends BaseFragment {

    protected SizeNotifierFrameLayout contentView;
    protected UniversalRecyclerView listView;

    private View actionBarBackground;
    private boolean actionBarVisible;
    private ValueAnimator actionBarVisibleAnimator;
    private int navigationBarHeight;
    private String initialItemKey;
    private final SparseBooleanArray expandedGroups = new SparseBooleanArray();

    private final DownscaleScrollableNoiseSuppressor scrollableViewNoiseSuppressor;
    private final BlurredBackgroundSourceRenderNode iBlur3SourceGlassFrosted;
    private IBlur3Capture iBlur3Capture;
    private final ArrayList<RectF> iBlur3Positions = new ArrayList<>();
    private final RectF iBlur3PositionActionBar = new RectF();

    protected BaseAoiSettingsActivity() {
        scrollableViewNoiseSuppressor = new DownscaleScrollableNoiseSuppressor();
        iBlur3SourceGlassFrosted = new BlurredBackgroundSourceRenderNode(null);
        iBlur3Positions.add(iBlur3PositionActionBar);
    }

    @Override
    public View createView(Context context) {
        contentView = new SizeNotifierFrameLayout(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                blur3InvalidateBlur();

                int width = getMeasuredWidth();
                int height = getMeasuredHeight();
                if (!iBlur3SourceGlassFrosted.inRecording()) {
                    Canvas recordingCanvas = iBlur3SourceGlassFrosted.beginRecording(width, height);
                    recordingCanvas.drawColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    if (SharedConfig.chatBlurEnabled()) {
                        scrollableViewNoiseSuppressor.draw(recordingCanvas, DownscaleScrollableNoiseSuppressor.DRAW_FROSTED_GLASS);
                    }
                    iBlur3SourceGlassFrosted.endRecording();
                }
                super.dispatchDraw(canvas);
                AndroidUtilities.drawNavigationBarProtection(canvas, this, getThemedColor(Theme.key_windowBackgroundWhite), navigationBarHeight);
            }

            @Override
            public void drawBlurRect(Canvas canvas, float y, Rect rect, Paint blurScrimPaint, boolean top) {
                if (!SharedConfig.chatBlurEnabled()) {
                    canvas.drawRect(rect, blurScrimPaint);
                    return;
                }

                canvas.save();
                canvas.translate(0, -y);
                iBlur3SourceGlassFrosted.draw(canvas, rect.left, rect.top + y, rect.right, rect.bottom + y);
                canvas.restore();

                int oldScrimAlpha = blurScrimPaint.getAlpha();
                blurScrimPaint.setAlpha(ChatActivity.ACTION_BAR_BLUR_ALPHA);
                canvas.drawRect(rect, blurScrimPaint);
                blurScrimPaint.setAlpha(oldScrimAlpha);
            }

            @Override
            public void updateColors() {
                super.updateColors();
                BaseAoiSettingsActivity.this.updateColors();
            }
        };

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setUseContainerForTitles();
        actionBar.setTitle(getActionBarTitle());
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.setAddToContainer(false);
        actionBar.setOccupyStatusBar(true);
        actionBar.setBackgroundColor(Color.TRANSPARENT);
        actionBar.setBackground(null);

        onCreateSettingsView(context);

        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, this::onItemLongClick);
        listView.adapter.setApplyBackground(false);
        listView.setSections();
        listView.setPadding(0, getListTopPadding(), 0, 0);
        listView.setClipToPadding(false);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateActionBarVisible();
                if (listView.getScrollState() == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(fragmentView);
                }
                scrollableViewNoiseSuppressor.onScrolled(dx, dy);
                blur3InvalidateBlur();
            }
        });
        iBlur3Capture = new ViewGroupPartRenderer(listView, contentView, listView::drawChild);
        listView.addEdgeEffectListener(() -> listView.postOnAnimation(this::blur3InvalidateBlur));
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        actionBarBackground = new View(context) {
            private final Paint blurScrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(@NonNull Canvas canvas) {
                int top = actionBar.getHeight();
                AndroidUtilities.rectTmp2.set(0, 0, getMeasuredWidth(), top);
                blurScrimPaint.setColor(getThemedColor(Theme.key_actionBarDefault));
                contentView.drawBlurRect(canvas, 0, AndroidUtilities.rectTmp2, blurScrimPaint, true);
                if (getParentLayout() != null) {
                    getParentLayout().drawHeaderShadow(canvas, top);
                }
            }
        };
        contentView.addView(actionBarBackground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200, Gravity.TOP));
        contentView.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL | Gravity.TOP));

        applyInitialItem();
        updateActionBarVisible(true, false);
        updateColors();

        ViewCompat.setOnApplyWindowInsetsListener(contentView, this::onApplyWindowInsets);
        return fragmentView = contentView;
    }

    @Override
    public void onFragmentDestroy() {
        if (actionBarVisibleAnimator != null) {
            actionBarVisibleAnimator.cancel();
            actionBarVisibleAnimator = null;
        }
        super.onFragmentDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            listView.adapter.update(false);
        }
    }

    protected abstract CharSequence getActionBarTitle();

    protected abstract void fillItems(ArrayList<UItem> items, UniversalAdapter adapter);

    protected UItem asConfigCheck(BoolConfig config, @StringRes int titleResId) {
        UItem item = UItem.asCheck(getString(titleResId)).setChecked(config.get());
        item.object = config;
        return item;
    }

    protected UItem asConfigCheck(BoolConfig config, @StringRes int titleResId, @StringRes int detailsResId) {
        UItem item = asConfigCheck(config, titleResId);
        SpannableString arrow = new SpannableString("→");
        arrow.setSpan(new ColoredImageSpan(R.drawable.search_arrow, ColoredImageSpan.ALIGN_CENTER), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        item.subtext = AndroidUtilities.replaceMultipleCharSequence("→", getString(detailsResId), arrow);
        return item;
    }

    protected void addGroup(ArrayList<UItem> items, UniversalAdapter adapter, int id, int iconResId, @StringRes int titleResId, Option[] options) {
        int enabled = 0;
        for (Option option : options) {
            if (option.config.get()) {
                enabled++;
            }
        }
        boolean expanded = expandedGroups.get(id);
        UItem group = ExpandableSettingsCell.Factory.group(id, iconResId, getString(titleResId), enabled, options.length, expanded, view -> {
            expandedGroups.put(id, !expandedGroups.get(id));
            listView.adapter.update(true);
        });
        group.object = options;
        items.add(group);
        if (adapter == null || expanded) {
            for (Option option : options) {
                UItem item = ExpandableSettingsCell.Factory.option(getString(option.titleResId), option.config.get());
                item.object = option.config;
                items.add(item);
            }
        }
    }

    protected EditTextBoldCursor input(Context context, String value, String hint) {
        EditTextBoldCursor input = new EditTextBoldCursor(context);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(getThemedColor(Theme.key_dialogTextHint));
        input.setCursorColor(getThemedColor(Theme.key_dialogTextBlack));
        input.setBackground(Theme.createEditTextDrawable(context, true));
        input.setText(value);
        input.setHint(hint);
        input.setSelectAllOnFocus(true);
        return input;
    }

    protected void editString(CharSequence title, StringConfig config, String hint) {
        EditTextBoldCursor input = input(getContext(), config.get(), hint);
        LinearLayout layout = new LinearLayout(getContext());
        layout.addView(input, LayoutHelper.createLinear(-1, 48, 24, 0, 24, 0));
        AlertDialog dialog = new AlertDialog.Builder(getContext(), getResourceProvider()).setTitle(title).setView(layout)
                .setNegativeButton(getString(R.string.Cancel), null).setPositiveButton(getString(R.string.Save), (d, which) -> {
                    config.set(input.getText().toString().trim());
                    listView.adapter.update(true);
                })
                .create();
        Runnable showKeyboard = () -> {
            input.requestFocus();
            AndroidUtilities.showKeyboard(input);
        };
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(showKeyboard, 200));
        showDialog(dialog, d -> AndroidUtilities.cancelRunOnUIThread(showKeyboard));
    }

    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (ExpandableSettingsCell.isGroup(item)) {
            if (LocaleController.isRTL ? x > dp(75) : x < view.getWidth() - dp(75)) {
                item.clickCallback.onClick(view);
                return;
            }
            for (Option option : (Option[]) item.object) {
                if (option.config.get() == item.checked) {
                    option.config.set(!item.checked);
                    onConfigToggled(option.config);
                }
            }
            listView.adapter.update(true);
        } else if (item.object instanceof BoolConfig config) {
            config.toggle();
            listView.adapter.update(true);
            onConfigToggled(config);
        }
    }

    protected void onConfigToggled(BoolConfig config) {
    }

    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        String pageKey;
        String itemKey;
        if (item.object instanceof AoiSettingsSearchItem result) {
            pageKey = result.pageKey;
            itemKey = result.itemKey;
        } else {
            pageKey = getSettingsPageKey();
            itemKey = getItemKey(item);
            if (TextUtils.isEmpty(pageKey) || TextUtils.isEmpty(itemKey)) {
                return false;
            }
        }
        String deepLink = AoiSettingsDeepLink.create(pageKey, itemKey);

        ItemOptions.makeOptions(this, view)
                .setLongPressSelectionEnabled(false)
                .add(R.drawable.msg_link2, getString(R.string.CopyLink), () -> {
                    AndroidUtilities.addToClipboard(deepLink);
                    BulletinFactory.of(this).createCopyLinkBulletin().show();
                })
                .setScrimViewBackground(((RecyclerListView) view.getParent()).getClipBackground(view))
                .show();
        return true;
    }

    @Nullable
    protected String getSettingsPageKey() {
        return null;
    }

    @Nullable
    protected String getItemKey(UItem item) {
        return item.object instanceof ConfigItem<?> config ? config.getKey() : null;
    }

    protected void onCreateSettingsView(Context context) {
    }

    protected void updateCustomColors() {
    }

    protected boolean hasCollapsingHeader() {
        return false;
    }

    protected int getListTopPadding() {
        return hasCollapsingHeader() ? dp(12) : ActionBar.getCurrentActionBarHeight();
    }

    @Nullable
    public BaseAoiSettingsActivity setInitialItemKey(@Nullable String itemKey) {
        initialItemKey = itemKey;
        boolean found = TextUtils.isEmpty(itemKey);
        ArrayList<UItem> items = new ArrayList<>();
        fillItems(items, null);
        for (UItem item : items) {
            found = found || itemKey.equals(getItemKey(item));
            if (ExpandableSettingsCell.isGroup(item)) {
                for (Option option : (Option[]) item.object) {
                    if (option.config.getKey().equals(itemKey)) {
                        expandedGroups.put(item.id, true);
                    }
                }
            }
        }
        return found ? this : null;
    }

    private void applyInitialItem() {
        if (TextUtils.isEmpty(initialItemKey)) {
            return;
        }
        for (int position = 0; position < listView.adapter.getItemCount(); position++) {
            UItem item = listView.adapter.getItem(position);
            if (item != null && initialItemKey.equals(getItemKey(item))) {
                int finalPosition = position;
                listView.layoutManager.scrollToPositionWithOffset(finalPosition, dp(12));
                listView.highlightRow(() -> finalPosition);
                return;
            }
        }
    }

    private void updateColors() {
        if (contentView == null) {
            return;
        }
        contentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        actionBar.setTitleColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setItemsColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText), false);
        updateCustomColors();
        actionBarBackground.invalidate();
        listView.invalidate();
    }

    protected boolean shouldShowActionBarBackground() {
        if (hasCollapsingHeader()) {
            if (listView.getChildCount() > 0) {
                View firstChild = listView.getChildAt(0);
                return listView.getChildAdapterPosition(firstChild) > 0 || firstChild.getY() + firstChild.getHeight() < actionBar.getHeight();
            }
            return false;
        } else {
            return listView.canScrollVertically(-1);
        }
    }

    protected final void updateActionBarVisible() {
        updateActionBarVisible(false, true);
    }

    private void updateActionBarVisible(boolean force, boolean animated) {
        boolean visible = shouldShowActionBarBackground();
        if (actionBarVisible == visible && !force) {
            return;
        }

        actionBarVisible = visible;
        if (actionBarVisibleAnimator != null) {
            actionBarVisibleAnimator.cancel();
            actionBarVisibleAnimator = null;
        }
        if (!animated) {
            actionBar.getTitlesContainer().setAlpha(hasCollapsingHeader() && !visible ? 0.0f : 1.0f);
            actionBarBackground.setAlpha(visible ? 1.0f : 0.0f);
        } else {
            actionBarVisibleAnimator = ValueAnimator.ofFloat(actionBarBackground.getAlpha(), visible ? 1.0f : 0.0f);
            actionBarVisibleAnimator.addUpdateListener(animator -> {
                float progress = (float) animator.getAnimatedValue();
                if (hasCollapsingHeader()) {
                    actionBar.getTitlesContainer().setAlpha(progress);
                }
                actionBarBackground.setAlpha(progress);
            });
            actionBarVisibleAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            actionBarVisibleAnimator.setDuration(420);
            actionBarVisibleAnimator.start();
        }
    }

    private void blur3InvalidateBlur() {
        if (fragmentView == null) {
            return;
        }

        int additionalList = dp(48);
        iBlur3PositionActionBar.set(0, -additionalList, fragmentView.getMeasuredWidth(), actionBar.getMeasuredHeight() + additionalList);
        scrollableViewNoiseSuppressor.setupRenderNodes(iBlur3Positions, 1);
        scrollableViewNoiseSuppressor.invalidateResultRenderNodes(iBlur3Capture, fragmentView.getMeasuredWidth(), fragmentView.getMeasuredHeight());
    }

    @NonNull
    protected WindowInsetsCompat onApplyWindowInsets(@NonNull View view, @NonNull WindowInsetsCompat insets) {
        Insets systemInsets = AndroidUtilities.getDefaultWindowInsets(insets, false);
        navigationBarHeight = systemInsets.bottom;
        listView.setPadding(0, systemInsets.top + getListTopPadding(), 0, navigationBarHeight);
        return WindowInsetsCompat.CONSUMED;
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public boolean drawEdgeNavigationBar() {
        return false;
    }

    protected record Option(BoolConfig config, @StringRes int titleResId) {}
}
