package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.dialogs.FolderIcons;
import nu.gpu.aoigram.dialogs.FolderTabsPreview;
import nu.gpu.aoigram.dialogs.FolderUnreadCounter;
import nu.gpu.aoigram.tabs.MainTabs;
import nu.gpu.aoigram.tabs.MainTabsPreview;

public final class AoiAppearanceSettingsActivity extends BaseAoiSettingsActivity {

    private FolderTabsPreview folderTabsPreview;
    private MainTabsPreview mainTabsPreview;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiAppearanceSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.APPEARANCE_PAGE;
    }

    @Override
    protected void onCreateSettingsView(Context context) {
        folderTabsPreview = new FolderTabsPreview(context, this);
        mainTabsPreview = new MainTabsPreview(context, this);
    }

    @Override
    protected void updateCustomColors() {
        folderTabsPreview.updateColors();
        mainTabsPreview.updateColors();
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        if (!folderTabsPreview.isAttachedToWindow()) {
            return true;
        }
        int[] location = new int[2];
        folderTabsPreview.getLocationOnScreen(location);
        float x = event.getRawX() - location[0];
        float y = event.getRawY() - location[1];
        return x < 0 || y < 0 || x >= folderTabsPreview.getWidth() || y >= folderTabsPreview.getHeight();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(asConfigCheck(AoiConfig.glareEffects, R.string.GlareEffects));
        if (Build.VERSION.SDK_INT >= 34) {
            items.add(asConfigCheck(AoiConfig.predictiveBack, R.string.PredictiveBack));
        }
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.DialogList)));
        items.add(asConfigCheck(AoiConfig.hideStories, R.string.HideStories));
        items.add(asConfigCheck(AoiConfig.hideSearchBar, R.string.HideSearchBar));
        items.add(asConfigCheck(AoiConfig.showSenderAvatar, R.string.ShowSenderAvatar));
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.SettingsFolders)));
        items.add(UItem.asCustom(folderTabsPreview));
        UItem tabStyleItem = UItem.asButton(0, getString(R.string.FolderTabStyle), FolderIcons.getLabel(AoiConfig.folderTabStyle.get()));
        tabStyleItem.object = AoiConfig.folderTabStyle;
        items.add(tabStyleItem);
        UItem unreadCounterItem = UItem.asButton(0, getString(R.string.FolderUnreadCounter), FolderUnreadCounter.getLabel(AoiConfig.folderUnreadCounter.get()));
        unreadCounterItem.object = AoiConfig.folderUnreadCounter;
        items.add(unreadCounterItem);
        items.add(asConfigCheck(AoiConfig.hideAllChatsTab, R.string.HideAllChatsTab));
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.MainTabs)));
        items.add(UItem.asCustom(mainTabsPreview));
        UItem visibilityItem = UItem.asButton(0, getString(R.string.MainTabsVisibility), MainTabs.getLabel(AoiConfig.mainTabsVisibility.get()));
        visibilityItem.object = AoiConfig.mainTabsVisibility;
        items.add(visibilityItem);
        if (!MainTabs.isHidden()) {
            items.add(asConfigCheck(AoiConfig.hideMainTabsTitles, R.string.HideMainTabsTitles));
            items.add(asConfigCheck(AoiConfig.hideContactsTab, R.string.HideContactsTab));
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        super.onItemClick(item, view, position, x, y);
        if (item.object == AoiConfig.glareEffects) {
            folderTabsPreview.updateColors();
            mainTabsPreview.updateColors();
        } else if (item.object == AoiConfig.hideSearchBar) {
            parentLayout.rebuildFragments(0);
        } else if (item.object == AoiConfig.folderTabStyle) {
            String[] labels = {FolderIcons.getLabel(FolderIcons.TITLE), FolderIcons.getLabel(FolderIcons.ICON), FolderIcons.getLabel(FolderIcons.ICON_AND_TITLE)};
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), labels, getString(R.string.FolderTabStyle), AoiConfig.folderTabStyle.get(), (dialog, which) -> {
                AoiConfig.folderTabStyle.set(which);
                listView.adapter.update(true);
                folderTabsPreview.updateTabs(true);
                parentLayout.rebuildFragments(0);
            }));
        } else if (item.object == AoiConfig.folderUnreadCounter) {
            String[] labels = {FolderUnreadCounter.getLabel(FolderUnreadCounter.ALL), FolderUnreadCounter.getLabel(FolderUnreadCounter.UNMUTED), FolderUnreadCounter.getLabel(FolderUnreadCounter.HIDDEN)};
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), labels, getString(R.string.FolderUnreadCounter), AoiConfig.folderUnreadCounter.get(), (dialog, which) -> {
                AoiConfig.folderUnreadCounter.set(which);
                listView.adapter.update(true);
                folderTabsPreview.updateCounters();
                FolderUnreadCounter.reload();
            }));
        } else if (item.object == AoiConfig.hideAllChatsTab) {
            folderTabsPreview.updateTabs(true);
            parentLayout.rebuildFragments(0);
        } else if (item.object == AoiConfig.mainTabsVisibility) {
            String[] labels = {MainTabs.getLabel(MainTabs.ALWAYS), MainTabs.getLabel(MainTabs.HIDE_ON_SCROLL), MainTabs.getLabel(MainTabs.HIDDEN)};
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), labels, getString(R.string.MainTabsVisibility), AoiConfig.mainTabsVisibility.get(), (dialog, which) -> {
                if (which == AoiConfig.mainTabsVisibility.get()) {
                    return;
                }
                boolean rebuild = MainTabs.isHidden() || which == MainTabs.HIDDEN;
                AoiConfig.mainTabsVisibility.set(which);
                listView.adapter.update(true);
                mainTabsPreview.updateVisibility();
                if (rebuild) {
                    parentLayout.rebuildFragments(0);
                }
            }));
        } else if (item.object == AoiConfig.hideMainTabsTitles) {
            mainTabsPreview.updateTitles();
            parentLayout.rebuildFragments(0);
        } else if (item.object == AoiConfig.hideContactsTab) {
            mainTabsPreview.updateContactsTab();
            parentLayout.rebuildFragments(0);
        }
    }
}
