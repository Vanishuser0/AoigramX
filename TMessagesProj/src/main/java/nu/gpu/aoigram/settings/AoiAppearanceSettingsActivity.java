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
import nu.gpu.aoigram.dialogs.FolderTabsPreview;
import nu.gpu.aoigram.dialogs.FolderUnreadCounter;

public final class AoiAppearanceSettingsActivity extends BaseAoiSettingsActivity {

    private FolderTabsPreview folderTabsPreview;

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
    }

    @Override
    protected void updateCustomColors() {
        folderTabsPreview.updateColors();
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
        UItem unreadCounterItem = UItem.asButton(0, getString(R.string.FolderUnreadCounter), FolderUnreadCounter.getLabel(AoiConfig.folderUnreadCounter.get()));
        unreadCounterItem.object = AoiConfig.folderUnreadCounter;
        items.add(unreadCounterItem);
        items.add(asConfigCheck(AoiConfig.hideAllChatsTab, R.string.HideAllChatsTab));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        super.onItemClick(item, view, position, x, y);
        if (item.object == AoiConfig.glareEffects) {
            folderTabsPreview.updateColors();
        } else if (item.object == AoiConfig.hideSearchBar) {
            parentLayout.rebuildFragments(0);
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
        }
    }
}
