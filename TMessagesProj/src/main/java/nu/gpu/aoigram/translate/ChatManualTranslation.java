package nu.gpu.aoigram.translate;

import android.text.TextUtils;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.TranslateController.PollText;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.translate.providers.TelegramTranslation;
import nu.gpu.aoigram.utils.ErrorFeedback;

public final class ChatManualTranslation {

    private record MessageKey(long dialog, int id) {
        MessageKey(MessageObject message) {
            this(message.getDialogId(), message.getId());
        }
    }

    private final int account;
    private final Map<MessageKey, TranslationTask> tasks = new HashMap<>();
    private final Set<MessageObject> manual = new HashSet<>();

    public ChatManualTranslation(int account) {
        this.account = account;
    }

    public void showOriginal(MessageObject message) {
        MessageKey key = new MessageKey(message);
        manual.removeIf(copy -> {
            if (!key.equals(new MessageKey(copy))) return false;
            if (message.messageOwner.summarizedOpen) {
                copy.aoiManualSummaryLanguage = null;
            } else {
                copy.aoiManualTranslation = false;
            }
            return !copy.aoiManualTranslation && copy.aoiManualSummaryLanguage == null;
        });
        refresh(message);
    }

    public void translateDefault(BaseFragment fragment, MessageObject message) {
        translate(fragment, message, TranslationOptions.defaults(TranslationOptions.defaultLanguage()), false);
    }

    public void check(BaseFragment fragment, MessageObject message, boolean onScreen) {
        if (message == null || !onScreen) return;
        if (message.translated && !message.isVoiceTranscriptionOpen() && !message.summarized
                && message.aoiOriginalMode != AoiConfig.translationShowOriginal.get()
        ) {
            refresh(message);
        }
        if (tasks.containsKey(new MessageKey(message))) return;
        if (message.messageOwner.summarizedOpen) {
            if (message.aoiManualSummaryLanguage != null
                    && (message.messageOwner.translatedSummaryText == null
                    || !TextUtils.equals(message.aoiManualSummaryLanguage, message.messageOwner.translatedSummaryLanguage))) {
                translate(fragment, message, new TranslationOptions(TranslationOptions.TELEGRAM, message.aoiManualSummaryLanguage));
            }
            return;
        }
        if (!message.aoiManualTranslation) return;
        PollText cachedPoll = message.messageOwner.translatedPoll;
        if (!TranslationSignature.valid(message) || cachedPoll != null && !PollText.isFullyTranslated(message, cachedPoll)) {
            translate(fragment, message, TranslationSignature.options(message, TranslationOptions.defaultLanguage()), message.aoiManualLanguage != null);
        }
    }

    public void translate(BaseFragment fragment, MessageObject message, TranslationOptions options) {
        translate(fragment, message, options, true);
    }

    private void translate(BaseFragment fragment, MessageObject message, TranslationOptions options, boolean fixed) {
        boolean summary = message.messageOwner.summarizedOpen;
        boolean transcription = message.isVoiceTranscriptionOpen();
        MessageKey key = new MessageKey(message);
        TranslationTask previous = tasks.remove(key);
        if (previous != null) {
            previous.cancel();
        }
        message.aoiTranslationLoading = false;
        String signature = summary ? null : TranslationSignature.of(message, options);
        if (summary) {
            message.aoiManualSummaryLanguage = options.language();
        } else {
            message.aoiManualTranslation = true;
            message.aoiManualProvider = options.provider();
            message.aoiManualLanguage = fixed ? options.language() : null;
        }
        manual.add(message);
        PollText cachedPoll = message.messageOwner.translatedPoll;
        TLRPC.TL_textWithEntities cachedText = transcription ? message.messageOwner.translatedVoiceTranscription : message.messageOwner.translatedText;
        boolean cached;
        if (summary) {
            cached = message.messageOwner.translatedSummaryText != null
                    && TextUtils.equals(options.language(), message.messageOwner.translatedSummaryLanguage);
        } else {
            cached = signature.equals(message.messageOwner.aoiTranslationSignature)
                    && (cachedPoll == null || PollText.isFullyTranslated(message, cachedPoll))
                    && (cachedText != null || cachedPoll != null || message.messageOwner.translatedRichMessage != null);
        }
        if (cached) {
            refresh(message);
            return;
        }
        if (!summary) {
            TranslationSignature.invalidate(message, options.language());
        }
        message.aoiTranslationLoading = true;
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.messageTranslating, message);
        TranslationTask task = new TranslationTask();
        tasks.put(key, task);
        TranslationService.ResultCallback<Object> done = (result, error) -> {
            tasks.remove(key);
            message.aoiTranslationLoading = false;
            if (!summary && !signature.equals(TranslationSignature.of(message, TranslationSignature.options(message, options.language())))) {
                translate(fragment, message, TranslationSignature.options(message, options.language()), fixed);
                return;
            }
            if (error != null) {
                if (summary) {
                    message.aoiManualSummaryLanguage = null;
                } else {
                    message.aoiManualTranslation = false;
                }
                ErrorFeedback.show(error, BulletinFactory.of(fragment), fragment::showDialog, true);
            } else {
                if (summary) {
                    message.messageOwner.translatedSummaryText = (TLRPC.TL_textWithEntities) result;
                    message.messageOwner.translatedSummaryLanguage = options.language();
                } else {
                    message.messageOwner.translatedToLanguage = options.language();
                    message.messageOwner.aoiTranslationSignature = signature;
                    if (result instanceof TL_iv.RichMessage rich) {
                        message.messageOwner.translatedRichMessage = rich;
                    } else if (result instanceof PollText poll) {
                        message.messageOwner.translatedPoll = poll;
                    } else if (transcription) {
                        message.messageOwner.translatedVoiceTranscription = (TLRPC.TL_textWithEntities) result;
                    } else {
                        message.messageOwner.translatedText = (TLRPC.TL_textWithEntities) result;
                    }
                }
                MessagesStorage.getInstance(account).updateMessageCustomParams(message.getDialogId(), message.messageOwner);
            }
            refresh(message);
        };
        if (summary && (options.provider() == TranslationOptions.TELEGRAM || message.messageOwner.summaryText == null)) {
            TelegramTranslation.summarize(task, account, message, TranslationLanguages.map(TranslationOptions.TELEGRAM, options.language()), done::done);
            return;
        }
        String context = MessagesController.getInstance(account).getTranslateController().aoiTranslationContext.collect(message, options);
        if (summary) {
            TranslationService.translate(task, account, message.messageOwner.summaryText, options, context, done::done);
        } else if (message.messageOwner.rich_message != null) {
            if (options.provider() == TranslationOptions.TELEGRAM) {
                TelegramTranslation.translateRich(task, account, message, TranslationLanguages.map(TranslationOptions.TELEGRAM, options.language()), done::done);
            } else {
                RichTranslation.translate(task, message.messageOwner.rich_message, options, context, done::done);
            }
        } else if (MessageObject.getMedia(message) instanceof TLRPC.TL_messageMediaPoll poll) {
            TranslationService.translatePoll(task, account, PollText.fromPoll(poll), message.messageOwner.translatedPoll, options, context, done::done);
        } else {
            TLRPC.TL_textWithEntities source = new TLRPC.TL_textWithEntities();
            source.text = transcription ? message.messageOwner.voiceTranscription : message.messageOwner.message;
            if (!transcription) {
                source.entities = message.messageOwner.entities;
            }
            TranslationService.translate(task, account, source, options, context, done::done);
        }
    }

    private void refresh(MessageObject message) {
        NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.messageTranslated, message);
    }

    public void copyState(MessageObject from, MessageObject to) {
        if (from == to) return;
        to.messageOwner.aoiTranslationSignature = from.messageOwner.aoiTranslationSignature;
        to.messageOwner.translatedPoll = from.messageOwner.translatedPoll;
        to.messageOwner.translatedRichMessage = from.messageOwner.translatedRichMessage;
        to.messageOwner.translatedVoiceTranscription = from.messageOwner.translatedVoiceTranscription;
        to.messageOwner.translatedSummaryText = from.messageOwner.translatedSummaryText;
        to.messageOwner.translatedSummaryLanguage = from.messageOwner.translatedSummaryLanguage;
        to.aoiManualTranslation = from.aoiManualTranslation;
        to.aoiManualSummaryLanguage = from.aoiManualSummaryLanguage;
        to.aoiManualProvider = from.aoiManualProvider;
        to.aoiManualLanguage = from.aoiManualLanguage;
        to.aoiTranslationLoading = from.aoiTranslationLoading;
        if (from.aoiManualTranslation || from.aoiManualSummaryLanguage != null) {
            manual.add(to);
        }
    }

    public void clear(boolean updateMessages) {
        for (TranslationTask task : tasks.values()) {
            task.cancel();
        }
        tasks.clear();
        for (MessageObject message : manual) {
            message.aoiManualTranslation = false;
            message.aoiManualSummaryLanguage = null;
            message.aoiTranslationLoading = false;
            if (updateMessages) {
                message.updateTranslation(true);
            }
        }
        manual.clear();
    }
}
