package nu.gpu.aoigram.translate;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.ui.Components.ColoredImageSpan;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import nu.gpu.aoigram.config.AoiConfig;

public final class TranslationLanguages {

    public record Language(String code, String name, String nativeName, boolean pinned) {}

    private static final List<String> GOOGLE = List.of("aa", "ab", "ace", "ach", "af", "ak", "alz", "am", "ar", "as", "av", "awa", "ay", "az", "ba", "bal", "ban", "bbc", "bci", "be", "bem", "ber", "ber-Latn", "bew", "bg", "bho", "bik", "bm", "bm-Nkoo", "bn", "bo", "br", "bs", "bts", "btx", "bua", "ca", "ce", "ceb", "cgg", "ch", "chk", "chm", "ckb", "cnh", "co", "crh", "crh-Latn", "crs", "cs", "cv", "cy", "da", "de", "din", "doi", "dov", "dv", "dyu", "dz", "ee", "el", "en", "eo", "es", "et", "eu", "fa", "fa-AF", "ff", "fi", "fj", "fo", "fon", "fr", "fr-CA", "fur", "fy", "ga", "gaa", "gd", "gl", "gn", "gom", "gu", "gv", "ha", "haw", "hi", "hil", "hmn", "hr", "hrx", "ht", "hu", "hy", "iba", "id", "ig", "ilo", "is", "it", "iu", "iu-Latn", "iw", "ja", "jam", "jw", "ka", "kac", "kek", "kg", "kha", "kk", "kl", "km", "kn", "ko", "kr", "kri", "ktu", "ku", "kv", "ky", "la", "lb", "lg", "li", "lij", "lmo", "ln", "lo", "lt", "ltg", "lua", "luo", "lus", "lv", "mad", "mai", "mak", "mam", "mfe", "mg", "mh", "mi", "min", "mk", "ml", "mn", "mni-Mtei", "mr", "ms", "ms-Arab", "mt", "mwr", "my", "ndc-ZW", "ne", "new", "nhe", "nl", "no", "nr", "nso", "nus", "ny", "oc", "om", "or", "os", "pa", "pa-Arab", "pag", "pam", "pap", "pl", "ps", "pt", "pt-PT", "qu", "rn", "ro", "rom", "ru", "rw", "sa", "sah", "sat", "sat-Latn", "scn", "sd", "se", "sg", "shn", "si", "sk", "sl", "sm", "sn", "so", "sq", "sr", "ss", "st", "su", "sus", "sv", "sw", "szl", "ta", "tcy", "te", "tet", "tg", "th", "ti", "tiv", "tk", "tl", "tn", "to", "tpi", "tr", "trp", "ts", "tt", "tum", "ty", "tyv", "udm", "ug", "uk", "ur", "uz", "ve", "vec", "vi", "war", "wo", "xh", "yi", "yo", "yua", "yue", "zap", "zh", "zh-CN", "zh-TW", "zu");
    private static final List<String> YANDEX = List.of("az", "sq", "am", "en", "ar", "hy", "af", "eu", "ba", "be", "bn", "my", "bg", "bs", "cy", "hu", "vi", "ht", "gl", "mrj", "el", "ka", "gu", "da", "he", "yi", "id", "ga", "is", "es", "it", "kk", "kn", "ca", "ky", "zh", "ko", "xh", "km", "lo", "la", "lv", "lt", "lb", "mk", "mg", "ms", "ml", "mt", "mi", "mr", "mhr", "mn", "de", "ne", "nl", "no", "pa", "pap", "fa", "pl", "pt", "pt-BR", "ro", "ru", "ceb", "sr", "si", "sk", "sl", "sw", "su", "tl", "tg", "th", "ta", "tt", "te", "tr", "udm", "uz", "uk", "ur", "fi", "fr", "hi", "hr", "cs", "cv", "sv", "gd", "sjn", "emj", "eo", "et", "jv", "sah", "ja");
    private static final List<String> BING = List.of("af", "sq", "am", "ar", "hy", "az", "bn", "bs", "bg", "ca", "zh-Hans", "zh-Hant", "hr", "cs", "da", "prs", "nl", "en", "et", "fil", "fi", "fr", "de", "el", "gu", "ht", "he", "hi", "hu", "is", "id", "iu", "ga", "it", "ja", "kn", "kk", "km", "ko", "ku", "lo", "lv", "lt", "mg", "ms", "ml", "mt", "mr", "my", "mi", "ne", "nb", "or", "ps", "fa", "pl", "pt", "pt-PT", "pa", "ro", "ru", "sm", "sr-Cyrl", "sr-Latn", "sk", "sl", "es", "sw", "sv", "ta", "te", "th", "to", "tr", "uk", "ur", "vi", "cy");
    private static final List<String> TELEGRAM = List.of("sq", "ar", "am", "az", "ga", "et", "eu", "be", "bg", "is", "pl", "bs", "fa", "af", "da", "de", "ru", "fr", "tl", "fi", "fy", "km", "ka", "gu", "kk", "ht", "ko", "ha", "nl", "ky", "gl", "ca", "cs", "kn", "co", "hr", "ku", "la", "lv", "lo", "lt", "lb", "ro", "mg", "mt", "mr", "ml", "ms", "mk", "mi", "mn", "bn", "my", "hmn", "xh", "zu", "ne", "no", "pa", "pt", "ps", "ny", "ja", "sv", "sm", "sr", "st", "si", "eo", "sk", "sl", "sw", "gd", "ceb", "so", "tg", "te", "ta", "th", "tr", "cy", "ur", "uk", "uz", "es", "iw", "el", "haw", "sd", "hu", "sn", "hy", "ig", "it", "yi", "hi", "su", "id", "jw", "en", "yo", "vi", "zh-TW", "zh-CN", "zh");

    public static String map(int provider, String language) {
        String code = language.replace('_', '-').toLowerCase(Locale.ROOT);
        boolean legacyCodes = provider == TranslationOptions.GOOGLE || provider == TranslationOptions.TELEGRAM;
        switch (code) {
            case "in" -> code = "id";
            case "nb" -> code = "no";
            case "he", "iw" -> code = legacyCodes ? "iw" : "he";
            case "jv", "jw" -> code = legacyCodes ? "jw" : "jv";
        }
        if (provider == TranslationOptions.BING && code.equals("sr")) return "sr-Cyrl";
        if (code.startsWith("zh")) {
            boolean traditional = code.contains("tw") || code.contains("hk") || code.contains("hant");
            if (provider == TranslationOptions.BING) return traditional ? "zh-Hant" : "zh-Hans";
            if (provider == TranslationOptions.YANDEX) return "zh";
            return traditional ? "zh-TW" : "zh-CN";
        }
        if (provider == TranslationOptions.BING && code.equals("no")) return "nb";
        if (provider == TranslationOptions.BING && code.equals("pt-pt")) return "pt-PT";
        if (provider == TranslationOptions.YANDEX && code.equals("pt-br")) return "pt-BR";
        if (provider == TranslationOptions.LLM) return code;
        for (String supported : codes(provider)) {
            if (supported.equalsIgnoreCase(code)) return supported;
        }
        return code.split("-")[0];
    }

    private static List<String> codes(int provider) {
        return switch (provider) {
            case TranslationOptions.GOOGLE -> GOOGLE;
            case TranslationOptions.YANDEX -> YANDEX;
            case TranslationOptions.BING -> BING;
            case TranslationOptions.TELEGRAM -> TELEGRAM;
            default -> TranslateController.getLanguages().stream().map(l -> l.code).toList();
        };
    }

    public static boolean supports(int provider, String code) {
        if (provider == TranslationOptions.LLM) return true;
        String mapped = map(provider, code);
        return codes(provider).stream().anyMatch(mapped::equalsIgnoreCase);
    }

    public static ArrayList<Language> list(int provider) {
        ArrayList<Language> result = new ArrayList<>();
        Set<String> added = new HashSet<>();
        List<String> pinned = pinned().stream().map(code -> map(provider, code)).toList();
        for (String supported : codes(provider)) {
            String code = map(provider, supported);
            if (!added.add(code)) continue;
            Locale locale = Locale.forLanguageTag(code);
            result.add(new Language(code, locale.getDisplayName(Locale.getDefault()), locale.getDisplayName(locale), pinned.contains(code)));
        }
        Collator collator = Collator.getInstance(Locale.getDefault());
        result.sort(Comparator.<Language>comparingInt(language -> language.pinned ? pinned.indexOf(language.code) : Integer.MAX_VALUE).thenComparing(Language::name, collator));
        return result;
    }

    public static CharSequence label(CharSequence name, boolean pinned) {
        if (!pinned) return name;
        SpannableStringBuilder text = new SpannableStringBuilder(name).append("  ");
        text.setSpan(new ColoredImageSpan(R.drawable.msg_pin_mini), text.length() - 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    public static void togglePinned(int provider, String code) {
        ArrayList<String> pinned = pinned();
        if (!pinned.removeIf(pin -> map(provider, pin).equalsIgnoreCase(map(provider, code)))) pinned.add(code);
        AoiConfig.pinnedTranslationLanguages.set(String.join(",", pinned));
    }

    private static ArrayList<String> pinned() {
        ArrayList<String> result = new ArrayList<>();
        for (String code : AoiConfig.pinnedTranslationLanguages.get().split(",")) {
            if (!code.isEmpty()) result.add(code);
        }
        return result;
    }
}
