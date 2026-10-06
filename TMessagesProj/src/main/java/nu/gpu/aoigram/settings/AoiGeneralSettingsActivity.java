package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;

public final class AoiGeneralSettingsActivity extends BaseAoiSettingsActivity {

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiGeneralSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.GENERAL_PAGE;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(asConfigCheck(AoiConfig.showFullNumbers, R.string.ShowFullNumbers, R.string.ShowFullNumbersInfo));
        items.add(UItem.asShadow(null));
    }
}
