package nu.gpu.aoigram.translate;

import android.text.TextUtils;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.Components.TranslateAlert2;

public final class TranslationSignature {

    public static String of(MessageObject message, TranslationOptions options) {
        return options.signature() + ":" + Utilities.MD5(message.messageOwner.message + "\n" + message.messageOwner.edit_date);
    }

    public static String of(TL_stories.StoryItem story, TranslationOptions options) {
        return options.signature() + ":" + Utilities.MD5(story.caption);
    }

    public static TranslationOptions options(MessageObject message, String language) {
        if (!message.aoiManualTranslation) return TranslationOptions.defaults(language);
        return message.aoiManualLanguage == null
                ? TranslationOptions.defaults(TranslationOptions.defaultLanguage())
                : new TranslationOptions(message.aoiManualProvider, message.aoiManualLanguage);
    }

    public static boolean valid(MessageObject message) {
        String language = MessagesController.getInstance(message.currentAccount).getTranslateController().getDialogTranslateTo(message.getDialogId());
        return valid(message, options(message, language));
    }

    public static boolean valid(MessageObject message, TranslationOptions options) {
        return TextUtils.equals(message.messageOwner.aoiTranslationSignature, of(message, options));
    }

    public static boolean valid(TL_stories.StoryItem story) {
        return story.translatedText != null
                && TextUtils.equals(story.aoiTranslationSignature, of(story, TranslationOptions.defaults(TranslateAlert2.getToLanguage())));
    }

    public static boolean accept(MessageObject message, String signature, Object result) {
        if (message.aoiManualTranslation) return false;
        if (result != null) {
            message.messageOwner.aoiTranslationSignature = signature;
        }
        return true;
    }

    public static void invalidate(MessageObject message, String language) {
        if (message.messageOwner.aoiTranslationSignature == null && message.messageOwner.translatedToLanguage == null) return;
        if (TextUtils.equals(message.messageOwner.aoiTranslationSignature, of(message, options(message, language)))) return;
        message.messageOwner.translatedText = null;
        message.messageOwner.translatedPoll = null;
        message.messageOwner.translatedVoiceTranscription = null;
        message.messageOwner.translatedRichMessage = null;
        message.messageOwner.translatedToLanguage = null;
        message.messageOwner.aoiTranslationSignature = null;
        NotificationCenter.getInstance(message.currentAccount).postNotificationName(NotificationCenter.messageTranslated, message);
    }
}
