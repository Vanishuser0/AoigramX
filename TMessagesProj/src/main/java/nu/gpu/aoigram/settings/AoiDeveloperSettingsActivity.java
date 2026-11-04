package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.config.BoolConfig;

public final class AoiDeveloperSettingsActivity extends BaseAoiSettingsActivity {

    private static final int CRASH_ROW = 1;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiDeveloperSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.DEVELOPER_PAGE;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(asConfigCheck(AoiConfig.disableAds, R.string.DisableAds));
        items.add(asConfigCheck(AoiConfig.dontSendTyping, R.string.DontSendTyping));
        items.add(asConfigCheck(AoiConfig.hideEmptyStarButton, R.string.HideEmptyStarButton));
        items.add(asConfigCheck(AoiConfig.forceCopy, R.string.ForceCopy));
        items.add(asConfigCheck(AoiConfig.allowScreenshots, R.string.AllowScreenshots));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(CRASH_ROW, getString(R.string.TestCrash)).red());
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == CRASH_ROW) {
            throw new RuntimeException("Test crash");
        }
        super.onItemClick(item, view, position, x, y);
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
