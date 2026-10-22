package nu.gpu.aoigram.translate;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public final class TranslationContext {

    private record Source(long dialog, Supplier<List<MessageObject>> messages) {}

    private final LinkedHashMap<Object, Source> sources = new LinkedHashMap<>();

    public void attach(Object owner, long dialog, Supplier<List<MessageObject>> messages) {
        sources.put(owner, new Source(dialog, messages));
    }

    public void detach(Object owner) {
        sources.remove(owner);
    }

    public boolean has(long dialog) {
        for (Source source : sources.values()) {
            if (source.dialog == dialog) return true;
        }
        return false;
    }

    public String collect(MessageObject target, TranslationOptions options) {
        return collect(target.currentAccount, target.getDialogId(), target, target.replyMessageObject, options);
    }

    public String collectInput(int account, long dialog, MessageObject reply, TranslationOptions options) {
        return collect(account, dialog, null, reply, options);
    }

    private String collect(int account, long dialog, MessageObject target, MessageObject reply, TranslationOptions options) {
        if (options.provider() != TranslationOptions.LLM || !options.llm().context || DialogObject.isEncryptedDialog(dialog)) {
            return null;
        }

        if (dialog == UserConfig.getInstance(account).getClientUserId()) {
            return hasText(reply) && options.llm().contextCount > 0
                    ? "Replying to message:\n" + reply.messageOwner.message : null;
        }

        int limit = options.llm().contextCount;
        int replyId = reply == null ? 0 : reply.getId();
        ArrayList<MessageObject> selected = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (target != null) {
            seen.add(dialog + ":" + target.getId());
        }

        while (reply != null && selected.size() < limit && seen.add(reply.getDialogId() + ":" + reply.getId())) {
            if (hasText(reply)) {
                selected.add(reply);
            }
            reply = reply.replyMessageObject;
        }

        List<MessageObject> loaded = List.of();
        for (Source source : sources.values()) {
            if (source.dialog == dialog) {
                loaded = source.messages.get();
            }
        }

        ArrayList<MessageObject> previous = new ArrayList<>();
        for (MessageObject message : loaded) {
            if (!hasText(message)) {
                continue;
            }
            if (target == null || message.messageOwner.date < target.messageOwner.date
                    || message.messageOwner.date == target.messageOwner.date && message.getId() < target.getId()) {
                previous.add(message);
            }
        }
        previous.sort((a, b) -> {
            int date = Integer.compare(b.messageOwner.date, a.messageOwner.date);
            return date == 0 ? Integer.compare(b.getId(), a.getId()) : date;
        });

        for (MessageObject message : previous) {
            if (selected.size() >= limit) {
                break;
            }
            if (seen.add(message.getDialogId() + ":" + message.getId())) {
                selected.add(message);
            }
        }
        selected.sort(Comparator.comparingInt(message -> message.messageOwner.date));

        StringBuilder result = new StringBuilder();
        result.append("Chat: ").append(DialogObject.getDialogTitle(MessagesController.getInstance(account).getUserOrChat(dialog)));
        if (target != null) {
            result.append("\nTarget message: ");
            appendReference(result, target);
        } else {
            result.append("\nDraft message");
            if (replyId != 0) {
                result.append(" reply_to=").append(replyId);
            }
        }
        for (MessageObject message : selected) {
            result.append("\n\nMessage: ");
            appendReference(result, message);
            result.append('\n').append(message.messageOwner.message);
        }
        return result.toString();
    }

    private static boolean hasText(MessageObject message) {
        return message != null && message.messageOwner != null && message.messageOwner.message != null && !message.messageOwner.message.isBlank();
    }

    private static void appendReference(StringBuilder result, MessageObject message) {
        result.append("message_id=").append(message.getId());
        if (message.getReplyMsgId() != 0) {
            result.append(" reply_to=").append(message.getReplyMsgId());
        }
    }
}
