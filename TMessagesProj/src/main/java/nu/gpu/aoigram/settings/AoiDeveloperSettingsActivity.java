package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

public final class AoiDeveloperSettingsActivity extends BaseAoiSettingsActivity {

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiDeveloperSettings);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
    }
}
