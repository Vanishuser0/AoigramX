package nu.gpu.aoigram.config;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

import java.util.ArrayList;

import nu.gpu.aoigram.chat.VideoMessageCamera;
import nu.gpu.aoigram.dialogs.FolderIcons;
import nu.gpu.aoigram.dialogs.FolderUnreadCounter;
import nu.gpu.aoigram.messages.DoubleTap;
import nu.gpu.aoigram.network.DownloadSpeedBoost;
import nu.gpu.aoigram.profile.PeerIdDc;
import nu.gpu.aoigram.remote.AppUpdates;
import nu.gpu.aoigram.tabs.MainTabs;

@SuppressLint("ApplySharedPref")
public final class AoiConfig {

    private static final String PREFERENCES_NAME = "aoi_config";

    static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    private static final ArrayList<ConfigItem<?>> items = new ArrayList<>();

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
    public static final BoolConfig removeArchive = register(new BoolConfig("removeArchive", false));
    public static final BoolConfig disableUnarchiveSwipe = register(new BoolConfig("disableUnarchiveSwipe", false));
    public static final IntConfig downloadSpeedBoost = register(new IntConfig("downloadSpeedBoost", DownloadSpeedBoost.OFF));
    public static final BoolConfig uploadSpeedBoost = register(new BoolConfig("uploadSpeedBoost", false));
    public static final BoolConfig preferOriginalQuality = register(new BoolConfig("preferOriginalQuality", false));

    public static final BoolConfig systemFonts = register(new BoolConfig("systemFonts", true));
    public static final BoolConfig systemEmoji = register(new BoolConfig("systemEmoji", false));
    public static final BoolConfig glareEffects = register(new BoolConfig("glareEffects", false));
    public static final BoolConfig predictiveBack = register(new BoolConfig("predictiveBack", false));
    public static final BoolConfig hideStories = register(new BoolConfig("hideStories", false));
    public static final BoolConfig hideSearchBar = register(new BoolConfig("hideSearchBar", false));
    public static final BoolConfig showSenderAvatar = register(new BoolConfig("showSenderAvatar", false));
    public static final BoolConfig sortByUnread = register(new BoolConfig("sortByUnread", false));
    public static final IntConfig folderTabStyle = register(new IntConfig("folderTabStyle", FolderIcons.TITLE));
    public static final IntConfig folderUnreadCounter = register(new IntConfig("folderUnreadCounter", FolderUnreadCounter.ALL));
    public static final BoolConfig hideAllChatsTab = register(new BoolConfig("hideAllChatsTab", false));
    public static final IntConfig mainTabsVisibility = register(new IntConfig("mainTabsVisibility", MainTabs.ALWAYS));
    public static final BoolConfig hideMainTabsTitles = register(new BoolConfig("hideMainTabsTitles", false));
    public static final BoolConfig hideContactsTab = register(new BoolConfig("hideContactsTab", false));

    public static final StringConfig translationProvider = register(new StringConfig("translationProvider", "google"));
    public static final StringConfig translationLanguage = register(new StringConfig("translationLanguage", ""));
    public static final StringConfig pinnedTranslationLanguages = register(new StringConfig("pinnedTranslationLanguages", ""));
    public static final IntConfig translationShowOriginal = register(new IntConfig("translationShowOriginal", 0));
    public static final StringConfig inputTranslationLanguage = register(new StringConfig("inputTranslationLanguage", "en"));
    public static final StringConfig llmProvider = register(new StringConfig("llmProvider", "custom"));
    public static final StringConfig llmUrl = register(new StringConfig("llmUrl", ""));
    public static final StringConfig llmCustomKey = register(new StringConfig("llmCustomKey", "", true));
    public static final StringConfig llmCustomModel = register(new StringConfig("llmCustomModel", ""));
    public static final StringConfig llmCerebrasKey = register(new StringConfig("llmCerebrasKey", "", true));
    public static final StringConfig llmCerebrasModel = register(new StringConfig("llmCerebrasModel", ""));
    public static final StringConfig llmDeepSeekKey = register(new StringConfig("llmDeepSeekKey", "", true));
    public static final StringConfig llmDeepSeekModel = register(new StringConfig("llmDeepSeekModel", ""));
    public static final StringConfig llmGoogleAgentPlatformKey = register(new StringConfig("llmGoogleAgentPlatformKey", "", true));
    public static final StringConfig llmGoogleAgentPlatformModel = register(new StringConfig("llmGoogleAgentPlatformModel", ""));
    public static final StringConfig llmGoogleAIStudioKey = register(new StringConfig("llmGoogleAIStudioKey", "", true));
    public static final StringConfig llmGoogleAIStudioModel = register(new StringConfig("llmGoogleAIStudioModel", ""));
    public static final StringConfig llmGroqKey = register(new StringConfig("llmGroqKey", "", true));
    public static final StringConfig llmGroqModel = register(new StringConfig("llmGroqModel", ""));
    public static final StringConfig llmOllamaKey = register(new StringConfig("llmOllamaKey", "", true));
    public static final StringConfig llmOllamaModel = register(new StringConfig("llmOllamaModel", ""));
    public static final StringConfig llmOpenAIKey = register(new StringConfig("llmOpenAIKey", "", true));
    public static final StringConfig llmOpenAIModel = register(new StringConfig("llmOpenAIModel", ""));
    public static final StringConfig llmOpenRouterKey = register(new StringConfig("llmOpenRouterKey", "", true));
    public static final StringConfig llmOpenRouterModel = register(new StringConfig("llmOpenRouterModel", ""));
    public static final StringConfig llmXAIKey = register(new StringConfig("llmXAIKey", "", true));
    public static final StringConfig llmXAIModel = register(new StringConfig("llmXAIModel", ""));
    public static final StringConfig llmVercelKey = register(new StringConfig("llmVercelKey", "", true));
    public static final StringConfig llmVercelModel = register(new StringConfig("llmVercelModel", ""));
    public static final StringConfig llmSystemPrompt = register(new StringConfig("llmSystemPrompt", ""));
    public static final StringConfig llmUserPrompt = register(new StringConfig("llmUserPrompt", ""));
    public static final BoolConfig llmContext = register(new BoolConfig("llmContext", false));
    public static final IntConfig llmContextCount = register(new IntConfig("llmContextCount", 5));
    public static final StringConfig llmRequestBodies = register(new StringConfig("llmRequestBodies", "{}"));

    public static final IntConfig stickerSize = register(new IntConfig("stickerSize", 80));
    public static final BoolConfig showOnlineStatus = register(new BoolConfig("showOnlineStatus", false));
    public static final BoolConfig hideShareButton = register(new BoolConfig("hideShareButton", false));
    public static final BoolConfig hideKeyboardOnScroll = register(new BoolConfig("hideKeyboardOnScroll", false));
    public static final BoolConfig hideChannelBottomBar = register(new BoolConfig("hideChannelBottomBar", false));
    public static final BoolConfig usePinnedReactionsChats = register(new BoolConfig("usePinnedReactionsChats", false));
    public static final StringConfig pinnedReactionsChats = register(new StringConfig("pinnedReactionsChats", ""));
    public static final BoolConfig usePinnedReactionsChannels = register(new BoolConfig("usePinnedReactionsChannels", false));
    public static final BoolConfig hideStarReactionChannels = register(new BoolConfig("hideStarReactionChannels", false));
    public static final StringConfig pinnedReactionsChannels = register(new StringConfig("pinnedReactionsChannels", ""));
    public static final StringConfig transcriptionProvider = register(new StringConfig("transcriptionProvider", "telegram"));
    public static final BoolConfig transcriptionPremium = register(new BoolConfig("transcriptionPremium", false));
    public static final StringConfig transcriptionCloudflareAccount = register(new StringConfig("transcriptionCloudflareAccount", ""));
    public static final StringConfig transcriptionCloudflareToken = register(new StringConfig("transcriptionCloudflareToken", "", true));
    public static final StringConfig transcriptionGroqModel = register(new StringConfig("transcriptionGroqModel", ""));
    public static final StringConfig transcriptionOpenAIModel = register(new StringConfig("transcriptionOpenAIModel", ""));
    public static final StringConfig transcriptionGoogleAIStudioModel = register(new StringConfig("transcriptionGoogleAIStudioModel", ""));
    public static final StringConfig transcriptionCustomUrl = register(new StringConfig("transcriptionCustomUrl", ""));
    public static final StringConfig transcriptionCustomKey = register(new StringConfig("transcriptionCustomKey", "", true));
    public static final StringConfig transcriptionCustomModel = register(new StringConfig("transcriptionCustomModel", ""));
    public static final StringConfig transcriptionPrompt = register(new StringConfig("transcriptionPrompt", ""));
    public static final IntConfig doubleTapIncomingAction = register(new IntConfig("doubleTapIncomingAction", DoubleTap.REACTION));
    public static final IntConfig doubleTapOutgoingAction = register(new IntConfig("doubleTapOutgoingAction", DoubleTap.REACTION));
    public static final BoolConfig disableInstantCamera = register(new BoolConfig("disableInstantCamera", true));
    public static final IntConfig videoMessageCamera = register(new IntConfig("videoMessageCamera", VideoMessageCamera.DEFAULT));
    public static final BoolConfig groupMessageMenu = register(new BoolConfig("groupMessageMenu", true));
    public static final BoolConfig showCopyPhoto = register(new BoolConfig("showCopyPhoto", true));
    public static final BoolConfig showSave = register(new BoolConfig("showSave", true));
    public static final BoolConfig showRepeat = register(new BoolConfig("showRepeat", true));
    public static final BoolConfig showReport = register(new BoolConfig("showReport", false));
    public static final BoolConfig showClear = register(new BoolConfig("showClear", true));
    public static final BoolConfig showDetails = register(new BoolConfig("showDetails", true));
    public static final BoolConfig repeatConfirm = register(new BoolConfig("repeatConfirm", true));
    public static final BoolConfig botCommandConfirm = register(new BoolConfig("botCommandConfirm", false));
    public static final BoolConfig previewRecording = register(new BoolConfig("previewRecording", true));

    public static final IntConfig updateChannel = register(new IntConfig("updateChannel", AppUpdates.STABLE));
    public static final BoolConfig autoCheckUpdates = register(new BoolConfig("autoCheckUpdates", true));

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

    public static JSONObject exportJson() throws JSONException {
        JSONObject json = new JSONObject();
        for (ConfigItem<?> item : items) {
            if (preferences.contains(item.getKey())) {
                json.put(item.getKey(), item.get());
            }
        }
        return json;
    }

    public static boolean hasSecrets() {
        return items.stream().anyMatch(item -> item instanceof StringConfig config && config.secret && !config.get().isEmpty());
    }

    public static void importJson(JSONObject json) throws JSONException {
        SharedPreferences.Editor editor = preferences.edit().clear();
        for (ConfigItem<?> item : items) {
            if (json.has(item.getKey())) {
                item.write(editor, json);
            }
        }
        editor.commit();
    }

    public static void reset() {
        preferences.edit().clear().commit();
    }

    private static <C extends ConfigItem<?>> C register(C item) {
        item.load();
        items.add(item);
        return item;
    }
}
