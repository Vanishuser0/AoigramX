package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.os.Build;

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
    }
}
