package nu.gpu.aoigram.translate;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.Utilities;

import java.util.List;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.translate.llm.LlmSettings;

public record TranslationOptions(int provider, String language, LlmSettings llm) {

    public static final int LLM = 0, BING = 1, GOOGLE = 2, TELEGRAM = 3, YANDEX = 4;
    public static final String[] NAMES = {"AI", "Bing", "Google", "Telegram", "Yandex"};
    public static final List<String> IDS = List.of("llm", "bing", "google", "telegram", "yandex");

    public TranslationOptions(int provider, String language) {
        this(provider, language, provider == LLM ? new LlmSettings() : null);
    }

    public static int defaultProvider() {
        int provider = IDS.indexOf(AoiConfig.translationProvider.get());
        return provider < 0 ? GOOGLE : provider;
    }

    public static TranslationOptions defaults(String language) {
        return new TranslationOptions(defaultProvider(), language);
    }

    public static String defaultLanguage() {
        String language = AoiConfig.translationLanguage.get();
        return language.isEmpty() ? LocaleController.getInstance().getCurrentLocaleInfo().pluralLangCode.replace('_', '-') : language;
    }

    public String signature() {
        return Utilities.MD5(IDS.get(provider) + "\n" + language + "\n" + (llm == null ? "" : llm.signature()));
    }
}
