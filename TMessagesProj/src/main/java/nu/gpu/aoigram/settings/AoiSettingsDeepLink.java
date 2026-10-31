package nu.gpu.aoigram.settings;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.List;

public final class AoiSettingsDeepLink {

    private static final String HOST = "aoi";
    private static final String ROOT_PATH = "aoisettings";
    static final String MAIN_PAGE = "main";
    static final String GENERAL_PAGE = "general";
    static final String APPEARANCE_PAGE = "appearance";
    static final String TRANSLATOR_PAGE = "translator";
    static final String LLM_PAGE = "translator-llm";
    static final String PROMPTS_PAGE = "translator-prompts";
    static final String USER_PROMPT_PAGE = "translator-user-prompt";
    static final String CHAT_PAGE = "chat";
    static final String PINNED_REACTIONS_PAGE = "pinned-reactions";
    static final String SPEECH_TO_TEXT_PAGE = "speech-to-text";
    static final String TRANSCRIPTION_PROMPT_PAGE = "speech-to-text-prompt";
    static final String OTHER_PAGE = "other";
    static final String ABOUT_PAGE = "about";
    static final String DEVELOPER_PAGE = "dev";

    private AoiSettingsDeepLink() {
    }

    static String create(String pageKey, String itemKey) {
        return new Uri.Builder()
                .scheme("tg")
                .authority(HOST)
                .appendPath(pageKey)
                .appendQueryParameter("r", itemKey)
                .build()
                .toString();
    }

    @Nullable
    public static Route parse(@Nullable Uri uri) {
        if (uri == null) {
            return null;
        }

        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            String host = uri.getHost();
            if (!"t.me".equalsIgnoreCase(host)
                    && !"telegram.me".equalsIgnoreCase(host)
                    && !"telegram.dog".equalsIgnoreCase(host)) {
                return null;
            }
            List<String> segments = uri.getPathSegments();
            if (segments.isEmpty() || !ROOT_PATH.equalsIgnoreCase(segments.get(0))) {
                return null;
            }
            return createRoute(uri, segments, 1);
        }

        if ("tg".equalsIgnoreCase(scheme)) {
            if (uri.isOpaque()) {
                String schemeSpecificPart = uri.getSchemeSpecificPart();
                if (TextUtils.isEmpty(schemeSpecificPart)) {
                    return null;
                }
                uri = Uri.parse("tg://" + schemeSpecificPart);
            }
            if (!HOST.equalsIgnoreCase(uri.getAuthority())) {
                return null;
            }
            return createRoute(uri, uri.getPathSegments(), 0);
        }

        return null;
    }

    private static Route createRoute(Uri uri, List<String> segments, int pageIndex) {
        boolean valid = segments.size() <= pageIndex + 1;
        String pageKey = segments.size() > pageIndex ? segments.get(pageIndex) : null;
        String itemKey = uri.getQueryParameter("r");
        return new Route(valid, pageKey, itemKey);
    }

    @Nullable
    static BaseAoiSettingsActivity createFragment(@Nullable String pageKey, @Nullable String itemKey) {
        BaseAoiSettingsActivity fragment;
        if (TextUtils.isEmpty(pageKey) || MAIN_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiSettingsActivity();
        } else if (GENERAL_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiGeneralSettingsActivity();
        } else if (APPEARANCE_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiAppearanceSettingsActivity();
        } else if (TRANSLATOR_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiTranslatorSettingsActivity();
        } else if (LLM_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiLlmSettingsActivity();
        } else if (PROMPTS_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = AoiPromptActivity.systemPrompt();
        } else if (USER_PROMPT_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = AoiPromptActivity.userPrompt();
        } else if (CHAT_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiChatSettingsActivity();
        } else if (PINNED_REACTIONS_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiPinnedReactionsActivity();
        } else if (SPEECH_TO_TEXT_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiSpeechToTextActivity();
        } else if (TRANSCRIPTION_PROMPT_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = AoiPromptActivity.transcriptionPrompt();
        } else if (OTHER_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiOtherSettingsActivity();
        } else if (ABOUT_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiAboutSettingsActivity();
        } else if (DEVELOPER_PAGE.equalsIgnoreCase(pageKey)) {
            fragment = new AoiDeveloperSettingsActivity();
        } else {
            return null;
        }
        return fragment.setInitialItemKey(itemKey);
    }

    public record Route(boolean valid, @Nullable String pageKey, @Nullable String itemKey) {
        @Nullable
        public BaseAoiSettingsActivity createFragment() {
            return valid ? AoiSettingsDeepLink.createFragment(pageKey, itemKey) : null;
        }
    }
}
