package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.os.Build;
import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;

public final class AoiAppearanceSettingsActivity extends BaseAoiSettingsActivity {

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiAppearanceSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.APPEARANCE_PAGE;
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
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        super.onItemClick(item, view, position, x, y);
        if (item.object == AoiConfig.hideSearchBar) {
            parentLayout.rebuildFragments(0);
        }
    }
}
