package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.reactions.PinnedReactions;

public final class AoiChatSettingsActivity extends BaseAoiSettingsActivity {

    private static final int PINNED_REACTIONS = 1;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiChatSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.CHAT_PAGE;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(asConfigCheck(AoiConfig.hideKeyboardOnScroll, R.string.HideKeyboardOnScroll));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(PINNED_REACTIONS, R.drawable.msg_pin, getString(R.string.PinnedReactions), PinnedReactions.getLabel()));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected String getItemKey(UItem item) {
        return item.id == PINNED_REACTIONS ? AoiSettingsDeepLink.PINNED_REACTIONS_PAGE : super.getItemKey(item);
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == PINNED_REACTIONS) {
            presentFragment(new AoiPinnedReactionsActivity());
        } else {
            super.onItemClick(item, view, position, x, y);
        }
    }
}
