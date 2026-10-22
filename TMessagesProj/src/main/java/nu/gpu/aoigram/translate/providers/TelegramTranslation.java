package nu.gpu.aoigram.translate.providers;

import static nu.gpu.aoigram.translate.TranslationService.failureText;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.ui.Components.TranslateAlert2;

import nu.gpu.aoigram.translate.TranslationService.ResultCallback;
import nu.gpu.aoigram.translate.TranslationTask;

public final class TelegramTranslation {

    public static void translate(TranslationTask task, int account, TLRPC.TL_textWithEntities source, String language, ResultCallback<TLRPC.TL_textWithEntities> callback) {
        TLRPC.TL_messages_translateText request = new TLRPC.TL_messages_translateText();
        request.flags = 2;
        request.to_lang = language;
        request.text.add(source);
        int id = ConnectionsManager.getInstance(account).sendRequest(request, (response, error) -> task.post(() -> {
            if (response instanceof TLRPC.TL_messages_translateResult result && !result.result.isEmpty()) {
                callback.done(TranslateAlert2.preprocess(source, result.result.get(0)), null);
            } else {
                callback.done(null, error == null ? failureText() : error.text);
            }
        }));
        task.addTelegram(account, id);
    }

    public static void summarize(TranslationTask task, int account, MessageObject message, String language, ResultCallback<TLRPC.TL_textWithEntities> callback) {
        TLRPC.TL_messages_summarizeText request = new TLRPC.TL_messages_summarizeText();
        request.flags = 1;
        request.peer = MessagesController.getInstance(account).getInputPeer(message.getDialogId());
        request.id = message.getId();
        request.to_lang = language;
        int id = ConnectionsManager.getInstance(account).sendRequestTyped(request, task::post, (result, error) -> {
            if (result != null) {
                callback.done(result, null);
            } else {
                callback.done(null, error == null ? failureText() : error.text);
            }
        });
        task.addTelegram(account, id);
    }

    public static void translateRich(TranslationTask task, int account, MessageObject message, String language, ResultCallback<TL_iv.RichMessage> callback) {
        TLRPC.TL_messages_translateRichMessage request = new TLRPC.TL_messages_translateRichMessage();
        request.flags = 1;
        request.peer = MessagesController.getInstance(account).getInputPeer(message.getDialogId());
        request.id.add(message.getId());
        request.to_lang = language;
        int id = ConnectionsManager.getInstance(account).sendRequest(request, (result, error) -> task.post(() -> {
            if (result instanceof TLRPC.TL_messages_translatedRichMessage rich && !rich.result.isEmpty()) {
                callback.done(rich.result.get(0), null);
            } else {
                callback.done(null, error == null ? failureText() : error.text);
            }
        }));
        task.addTelegram(account, id);
    }
}
