package nu.gpu.aoigram.transcription;

import static org.telegram.messenger.LocaleController.getString;

import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.config.StringConfig;
import nu.gpu.aoigram.translate.TranslationHttp;
import nu.gpu.aoigram.translate.llm.LlmSettings;
import nu.gpu.aoigram.utils.ErrorFeedback;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class Transcription {

    public record Provider(String id, String name, String url, String keyUrl, StringConfig key, String defaultModel, StringConfig model) {
        public String effectiveModel() {
            return model == null || model.get().isBlank() ? defaultModel : model.get().trim();
        }
    }

    public static final Provider TELEGRAM = new Provider("telegram", "Telegram", null, null, null, null, null);
    public static final Provider CLOUDFLARE = new Provider(
            "cloudflare",
            "Cloudflare Workers AI",
            "https://api.cloudflare.com/client/v4/accounts/",
            "https://developers.cloudflare.com/workers-ai/get-started/rest-api/",
            AoiConfig.transcriptionCloudflareToken,
            "@cf/openai/whisper-large-v3-turbo",
            null);
    public static final Provider GROQ = new Provider(
            "groq",
            "Groq",
            LlmSettings.GROQ.url(),
            LlmSettings.GROQ.keyUrl(),
            LlmSettings.GROQ.key(),
            "whisper-large-v3-turbo",
            AoiConfig.transcriptionGroqModel);
    public static final Provider OPENAI = new Provider(
            "openai",
            "OpenAI",
            LlmSettings.OPENAI.url(),
            LlmSettings.OPENAI.keyUrl(),
            LlmSettings.OPENAI.key(),
            "gpt-transcribe",
            AoiConfig.transcriptionOpenAIModel);
    public static final Provider GOOGLE_AI_STUDIO = new Provider(
            "google_ai_studio",
            "Google AI Studio",
            "https://generativelanguage.googleapis.com/v1beta",
            LlmSettings.GOOGLE_AI_STUDIO.keyUrl(),
            LlmSettings.GOOGLE_AI_STUDIO.key(),
            LlmSettings.GOOGLE_AI_STUDIO.defaultModel(),
            AoiConfig.transcriptionGoogleAIStudioModel);
    public static final Provider CUSTOM = new Provider("custom", "Custom", null, null, AoiConfig.transcriptionCustomKey, "", AoiConfig.transcriptionCustomModel);
    public static final List<Provider> PROVIDERS = List.of(CUSTOM, CLOUDFLARE, GOOGLE_AI_STUDIO, GROQ, OPENAI, TELEGRAM);

    public static final String DEFAULT_PROMPT = """
            Transcribe the speech in this audio verbatim in its original language.
            """.trim();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build();
    private static final AtomicInteger KEY_POSITION = new AtomicInteger();

    public static Provider provider() {
        for (Provider provider : PROVIDERS) if (provider.id.equals(AoiConfig.transcriptionProvider.get())) return provider;
        return TELEGRAM;
    }

    public static boolean enabled(int account) {
        boolean preferTelegram = AoiConfig.transcriptionPremium.get() && UserConfig.getActivatedAccountsCount() > 1 && UserConfig.getInstance(account).isPremium();
        return provider() != TELEGRAM && !preferTelegram;
    }

    public static boolean canPreferTelegram() {
        if (UserConfig.getActivatedAccountsCount() < 2) {
            return false;
        }
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            UserConfig config = UserConfig.getInstance(a);
            if (config.isClientActivated() && config.isPremium()) {
                return true;
            }
        }
        return false;
    }

    public static boolean canTranscribeAgain(MessageObject message) {
        return enabled(message.currentAccount) && message.isVoiceTranscriptionOpen() && message.messageOwner.voiceTranscriptionFinal;
    }

    public static void transcribe(MessageObject message, Consumer<String> callback) {
        Provider provider = provider();
        List<String> keys = LlmSettings.parseKeys(provider.key().get());
        if (!isConfigured(provider, keys)) {
            done(callback, null, getString(R.string.TranscriptionNotConfigured));
            return;
        }

        String key = keys.get(Math.floorMod(KEY_POSITION.getAndIncrement(), keys.size()));
        File file = localFile(message);
        if (file.isFile()) {
            Utilities.globalQueue.postRunnable(() -> send(provider, key, message, file, callback));
            return;
        }

        String name = FileLoader.getAttachFileName(message.getDocument());
        new NotificationCenter.NotificationCenterDelegate() {
            final NotificationCenter.ObserversGroup observers = NotificationCenter.getInstance(message.currentAccount)
                    .createObserversGroup(this)
                    .add(NotificationCenter.fileLoaded)
                    .add(NotificationCenter.fileLoadFailed);

            @Override
            public void didReceivedNotification(int id, int account, Object... args) {
                if (!name.equals(args[0])) {
                    return;
                }
                observers.removeAllObservers();
                if (id == NotificationCenter.fileLoaded) {
                    File loaded = (File) args[1];
                    Utilities.globalQueue.postRunnable(() -> send(provider, key, message, loaded, callback));
                } else {
                    boolean canceled = (Integer) args[1] == 1;
                    done(callback, null, canceled ? null : getString(R.string.TranscriptionDownloadFailed));
                }
            }
        };
        FileLoader.getInstance(message.currentAccount).loadFile(message.getDocument(), message, FileLoader.PRIORITY_HIGH, 0);
    }

    private static File localFile(MessageObject message) {
        String attachPath = message.messageOwner.attachPath;
        if (attachPath != null && new File(attachPath).isFile()) {
            return new File(attachPath);
        }
        return FileLoader.getInstance(message.currentAccount).getPathToMessage(message.messageOwner);
    }

    private static void send(Provider provider, String key, MessageObject message, File file, Consumer<String> callback) {
        boolean round = message.isRoundVideo();
        String format = round ? "m4a" : voiceFormat(message.getMimeType());
        Request request;
        try {
            byte[] audio = round ? AudioRemux.remux(file) : Files.readAllBytes(file.toPath());
            request = createRequest(provider, key, audio, format);
        } catch (Exception exception) {
            FileLog.e(exception);
            done(callback, null, getString(R.string.ErrorOccurred));
            return;
        }

        HTTP.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException exception) {
                done(callback, null, getString(R.string.TranscriptionNetworkError));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                String body;
                try (response) {
                    body = response.body().string();
                } catch (IOException exception) {
                    done(callback, null, getString(R.string.TranscriptionNetworkError));
                    return;
                }
                if (!response.isSuccessful()) {
                    String status = "HTTP " + response.code();
                    done(callback, null, body.isBlank() ? status : status + "\n" + TranslationHttp.format(body.trim()));
                    return;
                }
                try {
                    done(callback, parse(provider, body), null);
                } catch (JSONException exception) {
                    done(callback, null, TranslationHttp.format(body.trim()));
                }
            }
        });
    }

    private static String voiceFormat(String mimeType) {
        return switch (mimeType) {
            case "audio/mpeg" -> "mp3";
            case "audio/mp4" -> "m4a";
            default -> "ogg";
        };
    }

    private static boolean isConfigured(Provider provider, List<String> keys) {
        if (keys.isEmpty()) {
            return false;
        }
        if (provider == CLOUDFLARE) {
            return !AoiConfig.transcriptionCloudflareAccount.get().isBlank();
        }
        if (provider == CUSTOM) {
            return HttpUrl.parse(baseUrl(provider)) != null && !provider.effectiveModel().isEmpty();
        }
        return true;
    }

    private static String baseUrl(Provider provider) {
        if (provider == CUSTOM) {
            return AoiConfig.transcriptionCustomUrl.get().trim().replaceAll("/+$", "");
        }
        return provider.url();
    }

    private static Request createRequest(Provider provider, String key, byte[] audio, String format) throws JSONException {
        if (provider == CLOUDFLARE) {
            return cloudflareRequest(key, audio);
        }
        if (provider == GOOGLE_AI_STUDIO) {
            return geminiRequest(key, audio, format);
        }
        return whisperRequest(provider, key, audio, format);
    }

    private static Request cloudflareRequest(String key, byte[] audio) throws JSONException {
        JSONObject body = new JSONObject();
        body.put("audio", Base64.encodeToString(audio, Base64.NO_WRAP));
        body.put("vad_filter", true);

        String account = AoiConfig.transcriptionCloudflareAccount.get().trim();
        return new Request.Builder()
                .url(CLOUDFLARE.url() + account + "/ai/run/" + CLOUDFLARE.effectiveModel())
                .header("Authorization", "Bearer " + key)
                .post(RequestBody.create(body.toString(), JSON))
                .build();
    }

    private static Request geminiRequest(String key, byte[] audio, String format) throws JSONException {
        String prompt = AoiConfig.transcriptionPrompt.get().trim();
        JSONObject inlineData = new JSONObject();
        inlineData.put("mime_type", "audio/" + format);
        inlineData.put("data", Base64.encodeToString(audio, Base64.NO_WRAP));

        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", prompt.isEmpty() ? DEFAULT_PROMPT : prompt));
        parts.put(new JSONObject().put("inline_data", inlineData));

        JSONObject content = new JSONObject().put("parts", parts);
        JSONObject body = new JSONObject().put("contents", new JSONArray().put(content));
        return new Request.Builder()
                .url(GOOGLE_AI_STUDIO.url() + "/models/" + GOOGLE_AI_STUDIO.effectiveModel() + ":generateContent")
                .header("x-goog-api-key", key)
                .post(RequestBody.create(body.toString(), JSON))
                .build();
    }

    private static Request whisperRequest(Provider provider, String key, byte[] audio, String format) {
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", provider.effectiveModel())
                .addFormDataPart("file", "audio." + format, RequestBody.create(audio, MediaType.get("audio/" + format)))
                .build();
        return new Request.Builder()
                .url(baseUrl(provider) + "/audio/transcriptions")
                .header("Authorization", "Bearer " + key)
                .post(body)
                .build();
    }

    private static String parse(Provider provider, String body) throws JSONException {
        JSONObject json = new JSONObject(body);
        if (provider == CLOUDFLARE) {
            return json.getJSONObject("result").getString("text").trim();
        }
        if (provider == GOOGLE_AI_STUDIO) {
            return parseGemini(json);
        }
        return json.getString("text").trim();
    }

    private static String parseGemini(JSONObject json) throws JSONException {
        JSONArray parts = json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts");
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            text.append(parts.getJSONObject(i).optString("text"));
        }
        return text.toString().trim();
    }

    private static void done(Consumer<String> callback, String text, String error) {
        AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                ErrorFeedback.show(error, true);
            }
            callback.accept(text);
        });
    }
}
