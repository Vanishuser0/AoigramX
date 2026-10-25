package nu.gpu.aoigram.dialogs;

import org.telegram.messenger.MessagesController;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.FiltersListBottomSheet;
import org.telegram.ui.Components.Premium.LimitReachedBottomSheet;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.FilterCreateActivity;

import java.util.ArrayList;

@SuppressWarnings("deprecation")
public final class AddToFolder {

    public static void show(BaseFragment fragment, long dialogId, UndoView undoView) {
        ArrayList<Long> dialogs = new ArrayList<>();
        dialogs.add(dialogId);
        FiltersListBottomSheet sheet = new FiltersListBottomSheet(fragment, dialogs);
        sheet.setDelegate((filter, checked) -> {
            ArrayList<Long> alwaysShow = FiltersListBottomSheet.getDialogsCount(fragment, filter, dialogs, true, false);
            if (filter == null) {
                fragment.presentFragment(new FilterCreateActivity(null, alwaysShow));
                return;
            }
            if (checked) {
                filter.neverShow.add(dialogId);
                filter.alwaysShow.remove(dialogId);
            } else {
                MessagesController controller = fragment.getMessagesController();
                int count = filter.alwaysShow.size() + alwaysShow.size();
                if (count > controller.dialogFiltersChatsLimitDefault && !fragment.getUserConfig().isPremium() || count > controller.dialogFiltersChatsLimitPremium) {
                    fragment.showDialog(new LimitReachedBottomSheet(fragment, fragment.getContext(), LimitReachedBottomSheet.TYPE_CHATS_IN_FOLDER, fragment.getCurrentAccount(), null));
                    return;
                }
                filter.neverShow.removeAll(alwaysShow);
                filter.alwaysShow.addAll(alwaysShow);
            }
            FilterCreateActivity.saveFilterToServer(filter, filter.flags, filter.name, filter.entities, filter.title_noanimate, filter.color, filter.alwaysShow, filter.neverShow, filter.pinnedDialogs, false, false, true, true, false, fragment, null);
            undoView.showWithAction(dialogId, checked ? UndoView.ACTION_REMOVED_FROM_FOLDER : UndoView.ACTION_ADDED_TO_FOLDER, 1, filter, null, null);
        });
        fragment.showDialog(sheet);
    }
}
