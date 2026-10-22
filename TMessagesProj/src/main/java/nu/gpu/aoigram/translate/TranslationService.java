package nu.gpu.aoigram.translate;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController.PollText;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

import nu.gpu.aoigram.translate.llm.LlmTranslation;
import nu.gpu.aoigram.translate.providers.BingTranslation;
import nu.gpu.aoigram.translate.providers.GoogleTranslation;
import nu.gpu.aoigram.translate.providers.TelegramTranslation;
import nu.gpu.aoigram.translate.providers.YandexTranslation;

public final class TranslationService {

    public interface ResultCallback<T> {
        void done(T result, String error);
    }

    public static void translate(TranslationTask task, int account, TLRPC.TL_textWithEntities source, TranslationOptions options, String context, ResultCallback<TLRPC.TL_textWithEntities> callback) {
        if (source.text == null || source.text.isEmpty()) {
            task.post(() -> callback.done(source, null));
            return;
        }
        if (options.provider() == TranslationOptions.TELEGRAM) {
            if (!TranslationLanguages.supports(options.provider(), options.language())) {
                task.post(() -> callback.done(null, getString(R.string.TranslationUnsupportedLanguage)));
                return;
            }
            String language = TranslationLanguages.map(options.provider(), options.language());
            TelegramTranslation.translate(task, account, source, language, callback);
            return;
        }

        TranslationEntities.Format format = format(options, source);
        List<TLRPC.TL_textWithEntities> parts = options.provider() == TranslationOptions.BING ? TranslationEntities.split(source, 3000) : List.of(source);
        TLRPC.TL_textWithEntities[] results = new TLRPC.TL_textWithEntities[parts.size()];
        int[] remaining = {parts.size()};
        for (int i = 0; i < parts.size(); i++) {
            int index = i;
            TLRPC.TL_textWithEntities part = parts.get(i);
            request(task, TranslationEntities.encode(part, format), options, context, format == TranslationEntities.Format.HTML, (text, error) -> {
                if (remaining[0] < 0) return;
                if (error == null) {
                    try {
                        results[index] = TranslationEntities.decode(part, text, format);
                    } catch (RuntimeException exception) {
                        error = failureText();
                    }
                }
                if (error != null) {
                    remaining[0] = -1;
                    callback.done(null, error);
                } else if (--remaining[0] == 0) {
                    callback.done(TranslationEntities.join(results), null);
                }
            });
        }
    }

    private static TranslationEntities.Format format(TranslationOptions options, TLRPC.TL_textWithEntities source) {
        if (options.provider() != TranslationOptions.GOOGLE) return TranslationEntities.Format.ID;
        if (source.entities.stream().anyMatch(entity -> entity instanceof TLRPC.TL_messageEntityPre)) return TranslationEntities.Format.HTML;
        return TranslationEntities.Format.BARE;
    }

    public static void translatePoll(TranslationTask task, int account, PollText source, PollText translated, TranslationOptions options, String context, ResultCallback<PollText> callback) {
        PollText result = new PollText();
        ArrayList<TLRPC.TL_textWithEntities> texts = new ArrayList<>();
        ArrayList<Utilities.Callback<TLRPC.TL_textWithEntities>> targets = new ArrayList<>();
        if (translated != null && translated.question != null) {
            result.question = translated.question;
        } else if (source.question != null) {
            texts.add(source.question);
            targets.add(text -> result.question = text);
        }
        if (translated != null && translated.answers.size() == source.answers.size()) {
            result.answers = translated.answers;
        } else {
            for (TLRPC.PollAnswer answer : source.answers) {
                TLRPC.TL_pollAnswer item = new TLRPC.TL_pollAnswer();
                item.option = answer.option;
                result.answers.add(item);
                texts.add(answer.text);
                targets.add(text -> item.text = text);
            }
        }
        if (translated != null && translated.solution != null) {
            result.solution = translated.solution;
        } else if (source.solution != null) {
            texts.add(source.solution);
            targets.add(text -> result.solution = text);
        }
        int[] remaining = {texts.size()};
        for (int i = 0; i < texts.size(); i++) {
            Utilities.Callback<TLRPC.TL_textWithEntities> target = targets.get(i);
            translate(task, account, texts.get(i), options, context, (text, error) -> {
                if (error != null) {
                    task.cancel();
                    callback.done(null, error);
                } else {
                    target.run(text);
                    if (--remaining[0] == 0) {
                        callback.done(result, null);
                    }
                }
            });
        }
    }

    static void translateHtml(TranslationTask task, String input, TranslationOptions options, String context, ResultCallback<String> callback) {
        request(task, input, options, context, true, callback);
    }

    private static void request(TranslationTask task, String input, TranslationOptions options, String context, boolean html, ResultCallback<String> callback) {
        if (!TranslationLanguages.supports(options.provider(), options.language())) {
            task.post(() -> callback.done(null, getString(R.string.TranslationUnsupportedLanguage)));
            return;
        }
        String language = TranslationLanguages.map(options.provider(), options.language());
        switch (options.provider()) {
            case TranslationOptions.GOOGLE -> {
                if (html) {
                    GoogleTranslation.translateHtml(task, input, language, callback);
                } else {
                    GoogleTranslation.translate(task, input, language, callback);
                }
            }
            case TranslationOptions.YANDEX -> YandexTranslation.translate(task, input, language, callback);
            case TranslationOptions.BING -> BingTranslation.translate(task, input, language, callback);
            case TranslationOptions.LLM -> LlmTranslation.translate(task, options.llm(), input, options.language(), context, callback);
        }
    }

    public static String failureText() {
        return getString(R.string.TranslationFailedAlert2);
    }
}
