package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.config.BoolConfig;

public final class AoiDeveloperSettingsActivity extends BaseAoiSettingsActivity {

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiDeveloperSettings);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(asConfigCheck(AoiConfig.disableAds, R.string.DisableAds));
        items.add(asConfigCheck(AoiConfig.dontSendTyping, R.string.DontSendTyping));
        items.add(asConfigCheck(AoiConfig.hideEmptyStarButton, R.string.HideEmptyStarButton));
        items.add(asConfigCheck(AoiConfig.forceCopy, R.string.ForceCopy));
        items.add(asConfigCheck(AoiConfig.allowScreenshots, R.string.AllowScreenshots));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onConfigToggled(BoolConfig config) {
        if (config == AoiConfig.disableAds) {
            for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
                if (UserConfig.getInstance(account).isClientActivated()) {
                    MessagesController.getInstance(account).checkPromoInfo(true);
                }
            }
        }
    }
}
