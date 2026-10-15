package nu.gpu.aoigram.dialogs;

import static org.telegram.messenger.LocaleController.getString;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ScrollView;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.TopicsFragment;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;

public final class RecentChats {

    private static final String KEY = "recentChats";
    private static final int LIMIT = 10;

    public static void add(int currentAccount, long dialogId) {
        if (dialogId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return;
        }
        ArrayList<Long> dialogIds = get(currentAccount);
        dialogIds.remove(dialogId);
        dialogIds.add(0, dialogId);
        AoiConfig.accountData(currentAccount).edit().putString(KEY, TextUtils.join(",", dialogIds.subList(0, Math.min(dialogIds.size(), LIMIT)))).apply();
    }

    public static void addMenu(BaseFragment fragment, ItemOptions o) {
        int currentAccount = fragment.getCurrentAccount();
        ArrayList<TLObject> peers = new ArrayList<>();
        for (long dialogId : get(currentAccount)) {
            TLObject peer = fragment.getMessagesController().getUserOrChat(dialogId);
            if (peer != null) {
                peers.add(peer);
            }
        }
        if (peers.isEmpty()) {
            return;
        }
        ItemOptions recent = o.makeSwipeback();
        recent.add(R.drawable.ic_ab_back, getString(R.string.Back), () -> {
            o.closeSwipeback();
            o.dismiss();
        });
        recent.addGap();
        ItemOptions chats = ItemOptions.swipeback((ActionBarPopupWindow.ActionBarPopupWindowLayout) o.getLayout(), fragment.getResourceProvider());
        for (TLObject peer : peers) {
            long dialogId = DialogObject.getDialogId(peer);
            chats.addChat(peer, false, () -> {
                o.dismiss();
                open(fragment, dialogId);
            });
            chats.getLastView().setOnLongClickListener(v -> {
                o.dismiss();
                fragment.presentFragment(ProfileActivity.of(dialogId));
                return true;
            });
        }
        ScrollView scrollView = new ScrollView(o.getContext());
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.addView(chats.getLinearLayout());
        recent.addView(scrollView, LayoutHelper.createLinear(240, LayoutHelper.WRAP_CONTENT, 1f));
        recent.addGap();
        recent.add(R.drawable.msg_clear_recent, getString(R.string.ClearRecentHistory), true, () -> {
            o.dismiss();
            AoiConfig.accountData(currentAccount).edit().remove(KEY).apply();
        });
        o.add(R.drawable.msg_recent, getString(R.string.RecentChats), () -> o.openSwipeback(recent));
    }

    private static ArrayList<Long> get(int currentAccount) {
        ArrayList<Long> dialogIds = new ArrayList<>();
        String value = AoiConfig.accountData(currentAccount).getString(KEY, "");
        if (!value.isEmpty()) {
            for (String dialogId : value.split(",")) {
                dialogIds.add(Long.parseLong(dialogId));
            }
        }
        return dialogIds;
    }

    private static void open(BaseFragment fragment, long dialogId) {
        MessagesController messagesController = fragment.getMessagesController();
        TLRPC.Chat chat = messagesController.getChat(-dialogId);
        if (ChatObject.isForum(chat)) {
            if (ChatObject.areTabsEnabled(chat)) {
                ChatActivity chatActivity = ChatActivity.of(dialogId);
                ForumUtilities.applyTopic(chatActivity, MessagesStorage.TopicKey.of(dialogId, messagesController.getForumLastTopicId(chat.id)));
                fragment.presentFragment(chatActivity);
            } else {
                Bundle args = new Bundle();
                args.putLong("chat_id", chat.id);
                fragment.presentFragment(TopicsFragment.getTopicsOrChat(fragment, args));
            }
        } else {
            fragment.presentFragment(ChatActivity.of(dialogId));
        }
    }
}
