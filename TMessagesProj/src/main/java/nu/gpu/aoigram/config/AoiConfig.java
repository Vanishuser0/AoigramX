package nu.gpu.aoigram.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

import nu.gpu.aoigram.messages.DoubleTap;
import nu.gpu.aoigram.profile.PeerIdDc;

public final class AoiConfig {

    private static final String PREFERENCES_NAME = "aoi_config";

    static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

    public static final BoolConfig disableAds = register(new BoolConfig("disableAds", true));
    public static final BoolConfig dontSendTyping = register(new BoolConfig("dontSendTyping", false));
    public static final BoolConfig hideEmptyStarButton = register(new BoolConfig("hideEmptyStarButton", true));
    public static final BoolConfig forceCopy = register(new BoolConfig("forceCopy", true));
    public static final BoolConfig allowScreenshots = register(new BoolConfig("allowScreenshots", true));

    public static final BoolConfig showFullNumbers = register(new BoolConfig("showFullNumbers", false));
    public static final BoolConfig formatTimeWithSeconds = register(new BoolConfig("formatTimeWithSeconds", false));
    public static final IntConfig idDcType = register(new IntConfig("idDcType", PeerIdDc.TELEGRAM_API));
    public static final BoolConfig hidePhone = register(new BoolConfig("hidePhone", false));
    public static final BoolConfig showFullAbout = register(new BoolConfig("showFullAbout", false));
    public static final BoolConfig skipGiftsTab = register(new BoolConfig("skipGiftsTab", false));

    public static final BoolConfig glareEffects = register(new BoolConfig("glareEffects", false));
    public static final BoolConfig predictiveBack = register(new BoolConfig("predictiveBack", false));

    public static final StringConfig translationProvider = register(new StringConfig("translationProvider", "google"));
    public static final StringConfig translationLanguage = register(new StringConfig("translationLanguage", ""));
    public static final IntConfig translationShowOriginal = register(new IntConfig("translationShowOriginal", 0));
    public static final StringConfig inputTranslationLanguage = register(new StringConfig("inputTranslationLanguage", "en"));
    public static final StringConfig llmProvider = register(new StringConfig("llmProvider", "custom"));
    public static final StringConfig llmUrl = register(new StringConfig("llmUrl", ""));
    public static final StringConfig llmCustomKey = register(new StringConfig("llmCustomKey", ""));
    public static final StringConfig llmCustomModel = register(new StringConfig("llmCustomModel", ""));
    public static final StringConfig llmCerebrasKey = register(new StringConfig("llmCerebrasKey", ""));
    public static final StringConfig llmCerebrasModel = register(new StringConfig("llmCerebrasModel", ""));
    public static final StringConfig llmDeepSeekKey = register(new StringConfig("llmDeepSeekKey", ""));
    public static final StringConfig llmDeepSeekModel = register(new StringConfig("llmDeepSeekModel", ""));
    public static final StringConfig llmGoogleAgentPlatformKey = register(new StringConfig("llmGoogleAgentPlatformKey", ""));
    public static final StringConfig llmGoogleAgentPlatformModel = register(new StringConfig("llmGoogleAgentPlatformModel", ""));
    public static final StringConfig llmGoogleAIStudioKey = register(new StringConfig("llmGoogleAIStudioKey", ""));
    public static final StringConfig llmGoogleAIStudioModel = register(new StringConfig("llmGoogleAIStudioModel", ""));
    public static final StringConfig llmGroqKey = register(new StringConfig("llmGroqKey", ""));
    public static final StringConfig llmGroqModel = register(new StringConfig("llmGroqModel", ""));
    public static final StringConfig llmOllamaKey = register(new StringConfig("llmOllamaKey", ""));
    public static final StringConfig llmOllamaModel = register(new StringConfig("llmOllamaModel", ""));
    public static final StringConfig llmOpenAIKey = register(new StringConfig("llmOpenAIKey", ""));
    public static final StringConfig llmOpenAIModel = register(new StringConfig("llmOpenAIModel", ""));
    public static final StringConfig llmOpenRouterKey = register(new StringConfig("llmOpenRouterKey", ""));
    public static final StringConfig llmOpenRouterModel = register(new StringConfig("llmOpenRouterModel", ""));
    public static final StringConfig llmXAIKey = register(new StringConfig("llmXAIKey", ""));
    public static final StringConfig llmXAIModel = register(new StringConfig("llmXAIModel", ""));
    public static final StringConfig llmVercelKey = register(new StringConfig("llmVercelKey", ""));
    public static final StringConfig llmVercelModel = register(new StringConfig("llmVercelModel", ""));
    public static final StringConfig llmSystemPrompt = register(new StringConfig("llmSystemPrompt", ""));
    public static final StringConfig llmUserPrompt = register(new StringConfig("llmUserPrompt", ""));
    public static final BoolConfig llmContext = register(new BoolConfig("llmContext", false));
    public static final IntConfig llmContextCount = register(new IntConfig("llmContextCount", 5));

    public static final BoolConfig hideKeyboardOnScroll = register(new BoolConfig("hideKeyboardOnScroll", false));
    public static final BoolConfig hideChannelBottomBar = register(new BoolConfig("hideChannelBottomBar", false));
    public static final BoolConfig usePinnedReactionsChats = register(new BoolConfig("usePinnedReactionsChats", false));
    public static final StringConfig pinnedReactionsChats = register(new StringConfig("pinnedReactionsChats", ""));
    public static final BoolConfig usePinnedReactionsChannels = register(new BoolConfig("usePinnedReactionsChannels", false));
    public static final BoolConfig hideStarReactionChannels = register(new BoolConfig("hideStarReactionChannels", false));
    public static final StringConfig pinnedReactionsChannels = register(new StringConfig("pinnedReactionsChannels", ""));
    public static final IntConfig doubleTapIncomingAction = register(new IntConfig("doubleTapIncomingAction", DoubleTap.REACTION));
    public static final IntConfig doubleTapOutgoingAction = register(new IntConfig("doubleTapOutgoingAction", DoubleTap.REACTION));
    public static final BoolConfig groupMessageMenu = register(new BoolConfig("groupMessageMenu", true));
    public static final BoolConfig showCopyPhoto = register(new BoolConfig("showCopyPhoto", true));
    public static final BoolConfig showSave = register(new BoolConfig("showSave", true));
    public static final BoolConfig showRepeat = register(new BoolConfig("showRepeat", true));
    public static final BoolConfig showReport = register(new BoolConfig("showReport", false));
    public static final BoolConfig showDetails = register(new BoolConfig("showDetails", true));
    public static final BoolConfig repeatConfirm = register(new BoolConfig("repeatConfirm", true));
    public static final BoolConfig previewRecording = register(new BoolConfig("previewRecording", true));

    private AoiConfig() {
    }

    public static SharedPreferences accountData(int currentAccount) {
        long clientUserId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (clientUserId == 0) {
            throw new IllegalStateException("Account is not logged in: " + currentAccount);
        }
        String accountKey = (ConnectionsManager.getInstance(currentAccount).isTestBackend() ? "test_" : "prod_") + clientUserId;
        return ApplicationLoader.applicationContext.getSharedPreferences("aoi_account_data_" + accountKey, Context.MODE_PRIVATE);
    }

    private static <C extends ConfigItem<?>> C register(C item) {
        item.load();
        return item;
    }
}
