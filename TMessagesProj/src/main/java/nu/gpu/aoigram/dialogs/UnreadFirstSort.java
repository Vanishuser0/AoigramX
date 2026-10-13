package nu.gpu.aoigram.dialogs;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AlertsCreator;

import nu.gpu.aoigram.config.AoiConfig;

public final class UnreadFirstSort {

    public static void show(BaseFragment fragment) {
        String[] labels = {getString(R.string.SortByTime), getString(R.string.SortByUnread)};
        fragment.showDialog(AlertsCreator.createSingleChoiceDialog(fragment.getParentActivity(), labels, getString(R.string.SortBy), AoiConfig.sortByUnread.get() ? 1 : 0, (dialog, which) -> {
            AoiConfig.sortByUnread.set(which == 1);
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (UserConfig.getInstance(a).isClientActivated()) {
                    MessagesController.getInstance(a).sortDialogs(null);
                }
            }
            fragment.getNotificationCenter().postNotificationName(NotificationCenter.dialogsNeedReload);
        }));
    }

    public static int compare(int account, TLRPC.Dialog dialog1, TLRPC.Dialog dialog2) {
        if (!AoiConfig.sortByUnread.get()) {
            return 0;
        }
        return Boolean.compare(isUnread(account, dialog2), isUnread(account, dialog1));
    }

    private static boolean isUnread(int account, TLRPC.Dialog dialog) {
        if (dialog.unread_mark) {
            return true;
        }
        MessagesController controller = MessagesController.getInstance(account);
        TLRPC.Chat chat = controller.getChat(-dialog.id);
        if (chat != null && (chat.forum || chat.monoforum && ChatObject.canManageMonoForum(account, chat))) {
            int[] counts = controller.getTopicsController().getForumUnreadCount(chat.id);
            return counts[1] > 0 && !ChatObject.isMonoForum(chat) || counts[2] > 0 || counts[3] > 0 || counts[4] > 0;
        }
        if (dialog instanceof TLRPC.TL_dialogCommunity) {
            MessagesController.UnreadCounts counts = controller.getCommunityUnreadCount(-dialog.id);
            return counts.mentionCount > 0 || counts.reactionMentionCount > 0 || counts.pollVotesMentionCount > 0 || counts.hasUnmutedUnreadDialogs;
        }
        return dialog.unread_mentions_count > 0 && !ChatObject.isMonoForum(chat) || dialog.unread_reactions_count > 0 || dialog.unread_poll_votes_count > 0
                || dialog.unread_count > 0 && !controller.isDialogMuted(dialog.id, 0);
    }
}
