package nu.gpu.aoigram.chat;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.replaceTags;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

public final class MyMessages {

    private static final int[] OLDER_THAN_DAYS = {1, 7, 30, 365};

    public static boolean isAvailable(TLRPC.Chat chat) {
        return chat != null && (!ChatObject.isChannel(chat) || chat.megagroup) && !ChatObject.isMonoForum(chat);
    }

    public static void showDeleteAlert(BaseFragment fragment, TLRPC.Chat chat, int topicId, long mergeDialogId, int before, Utilities.Callback<Integer> onDeleted) {
        Context context = fragment.getParentActivity();
        Theme.ResourcesProvider resourcesProvider = fragment.getResourceProvider();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider)
                .setTitle(getString(R.string.DeleteMyMessages))
                .setMessage(replaceTags(before == 0 ? getString(R.string.DeleteMyMessagesAlert) : formatString(R.string.DeleteMyMessagesBeforeAlert, LocaleController.formatDateForBan(before))))
                .setNeutralButton(getString(R.string.DeleteMyMessagesOlderThan), (dialog, which) -> showOlderThanAlert(fragment, chat, topicId, mergeDialogId, onDeleted))
                .setNegativeButton(getString(R.string.Cancel), null);
        CheckBoxCell adminCell;
        if (ChatObject.isChannel(chat) && topicId == 0 && mergeDialogId == 0 && before == 0 && ChatObject.canUserDoAdminAction(chat, ChatObject.ACTION_DELETE_MESSAGES)) {
            adminCell = new CheckBoxCell(context, 1, resourcesProvider);
            adminCell.setBackground(Theme.getSelectorDrawable(false));
            adminCell.setText(getString(R.string.DeleteMyMessagesAdmin), "", false, false);
            adminCell.setPadding(LocaleController.isRTL ? dp(16) : dp(8), 0, LocaleController.isRTL ? dp(8) : dp(16), 0);
            adminCell.setOnClickListener(v -> adminCell.setChecked(!adminCell.isChecked(), true));
            builder.setView(adminCell);
        } else {
            adminCell = null;
        }
        builder.setPositiveButton(getString(R.string.DeleteAll), (dialog, which) -> {
            if (adminCell != null && adminCell.isChecked()) {
                int account = fragment.getCurrentAccount();
                showBulletin(fragment, getString(R.string.MyMessagesDeleted), () -> {
                    MessagesController.getInstance(account).deleteUserChannelHistory(chat, UserConfig.getInstance(account).getCurrentUser(), null, 0);
                    onDeleted.run(Integer.MAX_VALUE);
                });
            } else {
                deleteWithSearch(fragment, chat, topicId, mergeDialogId, before, onDeleted);
            }
        });
        AlertDialog alertDialog = builder.create();
        fragment.showDialog(alertDialog);
        alertDialog.redPositive();
    }

    private static void showOlderThanAlert(BaseFragment fragment, TLRPC.Chat chat, int topicId, long mergeDialogId, Utilities.Callback<Integer> onDeleted) {
        CharSequence[] items = {
                formatPluralString("Days", 1),
                formatPluralString("Weeks", 1),
                formatPluralString("Months", 1),
                formatPluralString("Years", 1)
        };
        fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(getString(R.string.DeleteMyMessagesOlderThan))
                .setItems(items, (dialog, which) -> showDeleteAlert(fragment, chat, topicId, mergeDialogId, ConnectionsManager.getInstance(fragment.getCurrentAccount()).getCurrentTime() - OLDER_THAN_DAYS[which] * 86400, onDeleted))
                .create());
    }

    private static void deleteWithSearch(BaseFragment fragment, TLRPC.Chat chat, int topicId, long mergeDialogId, int before, Utilities.Callback<Integer> onDeleted) {
        int account = fragment.getCurrentAccount();
        AlertDialog progress = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER, fragment.getResourceProvider());
        boolean[] cancelled = new boolean[1];
        progress.setOnCancelListener(dialog -> cancelled[0] = true);
        progress.showDelayed(300);
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<Integer> mergeIds = new ArrayList<>();
        Utilities.Callback<TLRPC.TL_error> finish = error -> {
            if (cancelled[0]) return;
            progress.dismiss();
            if (fragment.getParentActivity() == null) return;
            if (error != null) {
                BulletinFactory.of(fragment).showForError(error);
                return;
            }
            if (ids.isEmpty() && mergeIds.isEmpty()) {
                BulletinFactory.of(fragment).createSimpleBulletin(R.raw.info, getString(R.string.NoMessagesToDelete)).show();
                return;
            }
            showBulletin(fragment, formatPluralString("MessagesDeletedHint", ids.size() + mergeIds.size()), () -> {
                delete(account, -chat.id, topicId, ids);
                delete(account, mergeDialogId, 0, mergeIds);
                onDeleted.run(ids.size() + mergeIds.size());
            });
        };
        search(account, -chat.id, topicId, before, 0, ids, mergeDialogId == 0 ? finish : error -> {
            if (error == null) {
                search(account, mergeDialogId, 0, before, 0, mergeIds, finish);
            } else {
                finish.run(error);
            }
        });
    }

    private static void search(int account, long dialogId, int topicId, int before, int offsetId, ArrayList<Integer> ids, Utilities.Callback<TLRPC.TL_error> done) {
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = MessagesController.getInstance(account).getInputPeer(dialogId);
        req.q = "";
        req.from_id = MessagesController.getInstance(account).getInputPeer(UserConfig.getInstance(account).getClientUserId());
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        if (topicId != 0) {
            req.top_msg_id = topicId;
            req.flags |= 2;
        }
        req.max_date = before;
        req.offset_id = offsetId;
        req.limit = 100;
        ConnectionsManager.getInstance(account).sendRequest(req, (response, error) -> {
            if (error != null || ((TLRPC.messages_Messages) response).messages.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> done.run(error));
                return;
            }
            ArrayList<TLRPC.Message> messages = ((TLRPC.messages_Messages) response).messages;
            for (TLRPC.Message message : messages) {
                if (MessageObject.canDeleteMessage(account, false, message, null)) {
                    ids.add(message.id);
                }
            }
            search(account, dialogId, topicId, before, messages.get(messages.size() - 1).id, ids, done);
        });
    }

    private static void delete(int account, long dialogId, int topicId, ArrayList<Integer> ids) {
        for (int i = 0; i < ids.size(); i += 100) {
            MessagesController.getInstance(account).deleteMessages(new ArrayList<>(ids.subList(i, Math.min(ids.size(), i + 100))), null, null, dialogId, topicId, true, 0);
        }
    }

    private static void showBulletin(BaseFragment fragment, CharSequence text, Runnable action) {
        Bulletin.LottieLayout layout = new Bulletin.LottieLayout(fragment.getParentActivity(), fragment.getResourceProvider());
        layout.textView.setText(text);
        layout.setTimer();
        layout.setButton(new Bulletin.UndoButton(fragment.getParentActivity(), true, fragment.getResourceProvider()).setDelayedAction(action));
        Bulletin.make(fragment, layout, Bulletin.DURATION_PROLONG).show();
    }
}
