package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

public final class AoiChatSettingsActivity extends BaseAoiSettingsActivity {

    private static final String DUMMY_ITEM_KEY = "dummy";
    private static final int DUMMY_ITEM_ROW = 1;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiChatSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.CHAT_PAGE;
    }

    @Override
    protected String getItemKey(UItem item) {
        return item.id == DUMMY_ITEM_ROW ? DUMMY_ITEM_KEY : null;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asButton(DUMMY_ITEM_ROW, getString(R.string.AoiChatSettings)));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
    }
}
