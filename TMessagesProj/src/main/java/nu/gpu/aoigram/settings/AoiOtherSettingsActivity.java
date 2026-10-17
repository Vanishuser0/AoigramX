package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.utils.AppUtil;

public final class AoiOtherSettingsActivity extends BaseAoiSettingsActivity {

    private static final String RESTART_ITEM_KEY = "restart";
    private static final int RESTART_ITEM_ROW = 1;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiOtherSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.OTHER_PAGE;
    }

    @Override
    protected String getItemKey(UItem item) {
        return item.id == RESTART_ITEM_ROW ? RESTART_ITEM_KEY : null;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asButton(RESTART_ITEM_ROW, R.drawable.msg_retry, getString(R.string.RestartApp)));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == RESTART_ITEM_ROW) {
            AppUtil.restart(getParentActivity());
        }
    }
}
