package nu.gpu.aoigram.dialogs;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.FilterTabsView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;

import nu.gpu.aoigram.config.AoiConfig;

@SuppressLint("ViewConstructor")
public final class FolderTabsPreview extends FrameLayout {

    private static final int[] TITLES = {R.string.FilterAllChats, R.string.FolderLinkPreviewRight, R.string.FilterGroups, R.string.FilterChannels, R.string.FilterBots};
    private static final String[] EMOTICONS = {"💬", "👤", "👥", "📢", "🤖"};
    private static final int[] UNMUTED = {9, 3, 5, 0, 1};
    private static final int[] MUTED = {51, 0, 19, 32, 0};

    private final BaseFragment fragment;
    private final FilterTabsView tabsView;
    private final BlurredBackgroundSourceColor backgroundSource = new BlurredBackgroundSourceColor();

    public FolderTabsPreview(Context context, BaseFragment fragment) {
        super(context);
        this.fragment = fragment;
        setWillNotDraw(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        tabsView = new FilterTabsView(context, fragment.getResourceProvider());
        tabsView.setDelegate(new FilterTabsView.FilterTabsViewDelegate() {
            @Override
            public void onPageSelected(FilterTabsView.Tab tab, boolean forward) {
            }

            @Override
            public void onPageScrolled(float progress) {
            }

            @Override
            public void onSamePageSelected() {
            }

            @Override
            public int getTabCounter(int tabId) {
                if (FolderUnreadCounter.isHidden()) {
                    return 0;
                }
                boolean includeMuted = tabId == 0 ? NotificationsController.getInstance(fragment.getCurrentAccount()).showBadgeMuted : !FolderUnreadCounter.isUnmutedOnly();
                return UNMUTED[tabId] + (includeMuted ? MUTED[tabId] : 0);
            }

            @Override
            public int getTabIcon(int tabId) {
                return FolderIcons.getTabIcon(EMOTICONS[tabId]);
            }

            @Override
            public boolean didSelectTab(FilterTabsView.TabView tabView, boolean selected) {
                return false;
            }

            @Override
            public boolean isTabMenuVisible() {
                return false;
            }

            @Override
            public void onDeletePressed(int id) {
            }

            @Override
            public void onPageReorder(int fromId, int toId) {
            }

            @Override
            public boolean canPerformActions() {
                return false;
            }
        });
        BlurredBackgroundDrawable background = new BlurredBackgroundDrawableViewFactory(backgroundSource).create(tabsView, BlurredBackgroundProviderImpl.topPanel(fragment.getResourceProvider()));
        background.setRadius(dp(18));
        background.setPadding(dp(6.666f));
        tabsView.setPadding(0, dp(7), 0, dp(7));
        tabsView.setBlurredBackground(background);
        updateTabs(false);
        addView(tabsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 36 + 7 + 7, Gravity.TOP, 10, 8, 10, 8));
        updateColors();
    }

    public void updateTabs(boolean animated) {
        tabsView.removeTabs();
        tabsView.resetTabId();
        for (int id = AoiConfig.hideAllChatsTab.get() ? 1 : 0; id < TITLES.length; id++) {
            tabsView.addTab(id, id, getString(TITLES[id]), false, id == 0, false);
        }
        tabsView.finishAddingTabs(animated);
    }

    public void updateCounters() {
        tabsView.checkTabsCounter();
    }

    public void updateColors() {
        backgroundSource.setColor(fragment.getThemedColor(Theme.key_windowBackgroundWhite));
        tabsView.setColors(Theme.key_actionBarTabLine, Theme.key_actionBarTabActiveText, Theme.key_actionBarTabUnactiveText, Theme.key_actionBarTabSelector, Theme.key_actionBarDefault);
        tabsView.updateColors();
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
    }
}
