package nu.gpu.aoigram.translate;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.TranslateController.PollText;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_iv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nu.gpu.aoigram.utils.ErrorFeedback;

public final class ChatAutoTranslation {

    private static final int FATAL_ERROR = 1, NOTIFY_ERROR = 2;

    private final int account;
    private final TranslationContext context;
    private final Map<Long, Set<TranslationTask>> tasks = new HashMap<>();
    private final Map<Long, Set<Integer>> notified = new HashMap<>();

    public ChatAutoTranslation(int account, TranslationContext context) {
        this.account = account;
        this.context = context;
    }

    public static boolean hasTranslatableText(MessageObject message) {
        String text = message.messageOwner.message;
        if (text == null || text.isBlank() || message.getEmojiOnlyCount() > 0) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i)) && !isUntranslatableEntity(message, i)) return true;
        }
        return false;
    }

    private static boolean isUntranslatableEntity(MessageObject message, int index) {
        ArrayList<TLRPC.MessageEntity> entities = message.messageOwner.entities;
        for (int i = 0; i < entities.size(); i++) {
            TLRPC.MessageEntity entity = entities.get(i);
            if (index < entity.offset || index >= entity.offset + entity.length) continue;
            if (entity instanceof TLRPC.TL_messageEntityPre
                    || entity instanceof TLRPC.TL_messageEntityBotCommand
                    || entity instanceof TLRPC.TL_messageEntityEmail
                    || entity instanceof TLRPC.TL_messageEntityUrl
                    || entity instanceof TLRPC.TL_messageEntityMention
                    || entity instanceof TLRPC.TL_messageEntityCashtag
                    || entity instanceof TLRPC.TL_messageEntityHashtag
                    || entity instanceof TLRPC.TL_messageEntityBankCard
                    || entity instanceof TLRPC.TL_messageEntityPhone) {
                return true;
            }
        }
        return false;
    }

    public void translate(MessageObject message, TLRPC.TL_textWithEntities source, String language, Runnable finished, Utilities.Callback<TLRPC.TL_textWithEntities> callback) {
        send(message, language, finished, callback, (task, options, context, done) -> TranslationService.translate(task, account, source, options, context, done));
    }

    public void translatePoll(MessageObject message, String language, Runnable finished, Utilities.Callback<PollText> callback) {
        send(message, language, finished, callback, (task, options, context, done) -> TranslationService.translatePoll(task, account, PollText.fromMessage(message), message.messageOwner.translatedPoll, options, context, done));
    }

    public void translateRich(MessageObject message, String language, Runnable finished, Utilities.Callback<TL_iv.RichMessage> callback) {
        send(message, language, finished, callback, (task, options, context, done) -> RichTranslation.translate(task, message.messageOwner.rich_message, options, context, done));
    }

    private <T> void send(MessageObject message, String language, Runnable finished, Utilities.Callback<T> callback,
                          Utilities.Callback4<TranslationTask, TranslationOptions, String, TranslationService.ResultCallback<T>> request) {
        long dialog = message.getDialogId();
        TranslationOptions options = TranslationOptions.defaults(language);
        String signature = TranslationSignature.of(message, options);
        String unavailable = unavailable(options, language);
        TranslationTask task = new TranslationTask();
        tasks.computeIfAbsent(dialog, key -> new HashSet<>()).add(task);
        task.onCancel(() -> release(dialog, task));
        task.onCancel(finished);
        TranslationService.ResultCallback<T> done = (result, error) -> {
            task.cancel();
            TranslateController controller = MessagesController.getInstance(account).getTranslateController();
            if (!signature.equals(TranslationSignature.of(message, TranslationOptions.defaults(language)))) {
                notified.remove(dialog);
                controller.checkTranslation(message, true);
                return;
            }
            if (error != null) {
                int level = unavailable != null ? FATAL_ERROR : classify(dialog, task.httpCode);
                if (level == FATAL_ERROR) {
                    controller.toggleTranslatingDialog(dialog, false);
                }
                if (level != 0) {
                    ErrorFeedback.show(error, false);
                }
            }
            callback.run(result);
        };
        if (unavailable != null) {
            task.post(() -> done.done(null, unavailable));
        } else {
            request.run(task, options, context.collect(message, options), done);
        }
    }

    private static String unavailable(TranslationOptions options, String language) {
        if (!TranslationLanguages.supports(options.provider(), language)) return getString(R.string.TranslationUnsupportedLanguage);
        if (options.provider() == TranslationOptions.LLM && !options.llm().isConfigured()) return getString(R.string.LlmConfigurationRequired);
        return null;
    }

    private int classify(long dialog, int code) {
        if (code == 401 || code == 402 || code == 404) return FATAL_ERROR;
        return notified.computeIfAbsent(dialog, key -> new HashSet<>()).add(code) ? NOTIFY_ERROR : 0;
    }

    private void release(long dialog, TranslationTask task) {
        Set<TranslationTask> active = tasks.get(dialog);
        if (active != null) {
            active.remove(task);
            if (active.isEmpty()) {
                tasks.remove(dialog);
            }
        }
    }

    public void cancel(long dialog) {
        notified.remove(dialog);
        Set<TranslationTask> active = tasks.remove(dialog);
        if (active != null) {
            for (TranslationTask task : active) {
                task.cancel();
            }
        }
    }

    public void cancelAll() {
        notified.clear();
        for (long dialog : new ArrayList<>(tasks.keySet())) {
            cancel(dialog);
        }
    }
}
