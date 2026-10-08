package nu.gpu.aoigram.messages;

import static org.telegram.messenger.LocaleController.getString;

import android.text.TextUtils;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SendMessagesHelper.SendMessageParams;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.AccountFrozenAlert;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.HashMap;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.utils.ErrorFeedback;

public final class Repeat {

    public static boolean canCopy(MessageObject message) {
        TLRPC.MessageMedia media = message.messageOwner.media;
        return media instanceof TLRPC.TL_messageMediaPhoto && media.photo instanceof TLRPC.TL_photo
                || media instanceof TLRPC.TL_messageMediaDocument && media.document instanceof TLRPC.TL_document
                || media instanceof TLRPC.TL_messageMediaContact
                || media instanceof TLRPC.TL_messageMediaGeo
                || media instanceof TLRPC.TL_messageMediaVenue
                || isText(message);
    }

    public static boolean canCopyNoForwards(MessageObject message) {
        return message.isAnyKindOfSticker() && !message.isDice() || isText(message);
    }

    private static boolean isText(MessageObject message) {
        TLRPC.MessageMedia media = message.messageOwner.media;
        return (message.type == MessageObject.TYPE_TEXT || message.isAnimatedEmoji() || message.isAnimatedEmojiStickers())
                && (media == null || media instanceof TLRPC.TL_messageMediaEmpty || media instanceof TLRPC.TL_messageMediaWebPage);
    }

    private static MessageObject getTargetThread(ChatActivity chat, MessageObject message) {
        MessageObject thread = chat.getThreadMessage();
        if (thread == null && ChatObject.isForum(chat.getCurrentChat())) {
            TLRPC.TL_message topic = new TLRPC.TL_message();
            topic.id = (int) MessageObject.getTopicId(chat.getCurrentAccount(), message.messageOwner, MessagesStorage.FORUM_TYPE_CHAT);
            topic.peer_id = chat.getMessagesController().getPeer(chat.getDialogId());
            topic.message = "";
            thread = new MessageObject(chat.getCurrentAccount(), topic, false, false);
            thread.isTopicMainMessage = true;
        }
        return thread;
    }

    public static void send(ChatActivity chat, MessageObject message, MessageObject.GroupedMessages group, boolean asCopy) {
        if (chat.getMessagesController().isFrozen()) {
            AccountFrozenAlert.show(chat.getCurrentAccount());
            return;
        }
        if (chat.checkSlowMode(chat.getChatActivityEnterView().getSendButton())) {
            return;
        }
        ArrayList<MessageObject> messages = new ArrayList<>();
        if (group != null) {
            messages.addAll(group.messages);
        } else {
            messages.add(message);
        }
        boolean copyOnly = chat.isPeerNoForwards() || message.messageOwner.noforwards;
        boolean fromMyName = asCopy || copyOnly;
        MessageObject replyTo = fromMyName && message.hasValidReplyMessageObject() ? message.replyMessageObject : fromMyName || !chat.isTopic ? chat.getThreadMessage() : null;
        MessageObject thread = getTargetThread(chat, messages.get(0));
        Runnable send = () -> {
            if (!chat.checkSlowModeAlert()) {
                return;
            }
            if (replyTo == null && !copyOnly) {
                AlertsCreator.ensurePaidMessageConfirmation(chat.getCurrentAccount(), chat.getDialogId(), messages.size(), payStars -> {
                    int result = chat.getSendMessagesHelper().sendMessage(messages, chat.getDialogId(), fromMyName, false, true, 0, 0, thread, -1, payStars, chat.getSendMonoForumPeerId(), chat.getSendMessageSuggestionParams());
                    AlertsCreator.showSendMediaAlert(result, chat, chat.getResourceProvider());
                });
            } else {
                ArrayList<SendMessageParams> batch = new ArrayList<>();
                long groupId = messages.size() > 1 ? Utilities.random.nextLong() : 0;
                for (int i = 0; i < messages.size(); i++) {
                    MessageObject item = messages.get(i);
                    int sendError = SendMessagesHelper.canSendMessageToChat(chat.getCurrentChat(), item);
                    if (sendError != 0) {
                        AlertsCreator.showSendMediaAlert(sendError, chat, chat.getResourceProvider());
                        return;
                    }
                    SendMessageParams params = copyParams(item, chat.getDialogId(), replyTo != null ? replyTo : thread, thread);
                    if (params == null) {
                        ErrorFeedback.show(getString(R.string.UnsupportedAttachment), BulletinFactory.of(chat), chat::showDialog, true);
                        return;
                    }
                    params.sendMessageChatArguments = chat.getMessageChatSendParams();
                    params.monoForumPeer = chat.getSendMonoForumPeerId();
                    params.suggestionParams = chat.getSendMessageSuggestionParams();
                    params.invert_media = item.messageOwner.invert_media;
                    if (!ChatObject.canSendEmbed(chat.getCurrentChat())) params.mediaWebPage = null;
                    if (messages.size() > 1) {
                        params.params = new HashMap<>();
                        params.params.put("groupId", Long.toString(groupId));
                        if (i == messages.size() - 1) {
                            params.params.put("final", "1");
                        }
                    }
                    batch.add(params);
                }
                AlertsCreator.ensurePaidMessageConfirmation(chat.getCurrentAccount(), chat.getDialogId(), messages.size(), payStars -> {
                    for (SendMessageParams params : batch) {
                        params.payStars = payStars;
                        chat.getSendMessagesHelper().sendMessage(params);
                    }
                });
            }
        };
        if (AoiConfig.repeatConfirm.get() && chat.getMessagesController().getSendPaidMessagesStars(chat.getDialogId()) <= 0) {
            chat.showDialog(new AlertDialog.Builder(chat.getParentActivity(), chat.getResourceProvider())
                    .setTitle(getString(R.string.Repeat))
                    .setMessage(getString(messages.size() > 1 ? R.string.RepeatMessagesConfirmText : R.string.RepeatConfirmText))
                    .setPositiveButton(getString(R.string.OK), (dialog, which) -> send.run())
                    .setNegativeButton(getString(R.string.Cancel), null)
                    .create());
        } else {
            send.run();
        }
    }

    private static SendMessageParams copyParams(MessageObject message, long dialogId, MessageObject replyTo, MessageObject thread) {
        TLRPC.Message owner = message.messageOwner;
        TLRPC.MessageMedia media = owner.media;
        if (media instanceof TLRPC.TL_messageMediaPhoto && media.photo instanceof TLRPC.TL_photo photo) {
            return SendMessageParams.of(photo, null, dialogId, replyTo, thread, owner.message, owner.entities, null, null, true, 0, 0, media.ttl_seconds, message, false, message.hasMediaSpoilers());
        }
        if (media instanceof TLRPC.TL_messageMediaDocument && media.document instanceof TLRPC.TL_document document) {
            return SendMessageParams.of(document, null, owner.attachPath, dialogId, replyTo, thread, owner.message, owner.entities, null, null, true, 0, 0, media.ttl_seconds, message, null, false, message.hasMediaSpoilers());
        }
        if (media instanceof TLRPC.TL_messageMediaGeo || media instanceof TLRPC.TL_messageMediaVenue) {
            return SendMessageParams.of(media, dialogId, replyTo, thread, null, null, true, 0, 0);
        }
        if (media instanceof TLRPC.TL_messageMediaContact) {
            TLRPC.TL_userContact_old2 user = new TLRPC.TL_userContact_old2();
            user.id = media.user_id;
            user.phone = media.phone_number;
            user.first_name = media.first_name;
            user.last_name = media.last_name;
            if (!TextUtils.isEmpty(media.vcard)) {
                TLRPC.RestrictionReason vcard = new TLRPC.RestrictionReason();
                vcard.text = media.vcard;
                user.restriction_reason.add(vcard);
            }
            return SendMessageParams.of(user, dialogId, replyTo, thread, null, null, true, 0, 0);
        }
        if (isText(message)) {
            SendMessageParams params = SendMessageParams.of(owner.message, dialogId, replyTo, thread, null, false, owner.entities, null, null, true, 0, 0, null, false);
            if (media instanceof TLRPC.TL_messageMediaWebPage webpage) {
                params.mediaWebPage = webpage;
            }
            return params;
        }
        return null;
    }
}
