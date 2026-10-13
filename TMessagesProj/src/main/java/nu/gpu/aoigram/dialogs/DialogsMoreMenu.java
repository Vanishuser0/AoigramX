package nu.gpu.aoigram.dialogs;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ItemOptions;

public final class DialogsMoreMenu {

    public static void addMenu(BaseFragment fragment, ItemOptions o) {
        o.setSwipebackGravity(true, false);
        ItemOptions more = o.makeSwipeback().setMinWidth(196);
        more.add(R.drawable.msg_arrow_back, getString(R.string.Back), Theme.key_actionBarDefaultTitle, () -> {
            o.closeSwipeback();
            o.dismiss();
        });
        more.addGap();
        more.add(R.drawable.player_new_order, getString(R.string.SortBy), Theme.key_actionBarDefaultTitle, () -> {
            o.dismiss();
            UnreadFirstSort.show(fragment);
        });
        o.addGap();
        o.add(R.drawable.iv_media_dots, getString(R.string.MoreMenu), () -> o.openSwipeback(more));
        o.getLast().setRightIcon(R.drawable.msg_arrowright);
    }
}
