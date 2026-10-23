package nu.gpu.aoigram.translate.llm;

import static org.telegram.messenger.LocaleController.getString;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import nu.gpu.aoigram.translate.TranslationHttp;
import nu.gpu.aoigram.translate.TranslationService.ResultCallback;
import nu.gpu.aoigram.translate.TranslationTask;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public final class LlmTranslation {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ConcurrentHashMap<LlmSettings.Preset, AtomicInteger> KEY_POSITIONS = new ConcurrentHashMap<>();
    private static final Set<String> OPTIONAL_PARAMETERS_DISABLED = ConcurrentHashMap.newKeySet();

    public static void translate(TranslationTask task, LlmSettings settings, String text, String language, String context, ResultCallback<String> callback) {
        send(task, settings, text, language, context, 0, null, true, callback);
    }

    private static String nextKey(LlmSettings settings) {
        AtomicInteger position = KEY_POSITIONS.computeIfAbsent(settings.preset, preset -> new AtomicInteger());
        int index = position.getAndIncrement();
        List<String> keys = settings.keys();
        return keys.get(Math.floorMod(index, keys.size()));
    }

    private static void send(
            TranslationTask task,
            LlmSettings settings,
            String text,
            String language,
            String context,
            int attempt,
            String key,
            boolean parameters,
            ResultCallback<String> callback
    ) {
        if (!settings.isConfigured()) {
            task.post(() -> callback.done(null, getString(R.string.LlmConfigurationRequired)));
            return;
        }

        try {
            JSONArray messages = createLlmMessages(settings, text, language, context);
            String apiKey = key == null ? nextKey(settings) : key;
            String parameterKey = settings.url + "\n" + settings.model;
            boolean googleAgentPlatform = settings.preset == LlmSettings.GOOGLE_AGENT_PLATFORM;
            JSONObject extra;
            if (settings.body != null) {
                extra = new JSONObject(settings.body);
            } else if (parameters && !OPTIONAL_PARAMETERS_DISABLED.contains(parameterKey)) {
                extra = defaultParameters(settings);
            } else {
                extra = new JSONObject();
            }
            boolean withOptionalParameters = settings.body == null && !googleAgentPlatform && extra.length() > 0;
            Request request = createRequest(settings, apiKey, createBody(settings, messages, extra));

            TranslationHttp.send(task, request, true,
                    (response, metadata) -> parseLlmResponse(response, googleAgentPlatform),
                    (result, code, error, retryAfter) -> {
                        List<String> keys = settings.keys();
                        if (error == null) {
                            if (!parameters) {
                                OPTIONAL_PARAMETERS_DISABLED.add(parameterKey);
                            }
                            callback.done(result, null);
                        } else if (attempt >= 2) {
                            callback.done(null, error);
                        } else if (code == 400 && withOptionalParameters) {
                            send(task, settings, text, language, context, attempt + 1, apiKey, false, callback);
                        } else if ((code == 401 || code == 402) && keys.size() > 1) {
                            String fallbackKey = keys.get((keys.indexOf(apiKey) + 1) % keys.size());
                            send(task, settings, text, language, context, attempt + 1, fallbackKey, parameters, callback);
                        } else if (TranslationHttp.retryable(code, retryAfter)) {
                            long delay = retryAfter > 0 ? retryAfter * 1000 : 1000L << attempt;
                            task.post(() -> send(task, settings, text, language, context, attempt + 1, null, parameters, callback), delay);
                        } else {
                            callback.done(null, error);
                        }
                    });
        } catch (Exception exception) {
            task.post(() -> callback.done(null, getString(R.string.LlmConfigurationRequired)));
        }
    }

    public static Request createRequest(LlmSettings settings, String apiKey, JSONObject body) {
        Request.Builder requestBuilder = new Request.Builder().post(RequestBody.create(body.toString(), JSON));
        if (settings.preset != LlmSettings.GOOGLE_AGENT_PLATFORM) {
            return requestBuilder.url(settings.url + "/chat/completions").header("Authorization", "Bearer " + apiKey).build();
        }

        String model = settings.model;
        if (model.startsWith("models/")) {
            model = model.substring(7);
        }
        if (model.startsWith("google/")) {
            model = model.substring(7);
        }
        if (!model.startsWith("publishers/")) {
            model = "publishers/google/models/" + model;
        }
        return requestBuilder.url(settings.url + "/" + model + ":generateContent").header("x-goog-api-key", apiKey).build();
    }

    public static Request testRequest(LlmSettings settings, String text, String language, JSONObject extra) throws JSONException {
        return createRequest(settings, settings.keys().get(0), createBody(settings, createLlmMessages(settings, text, language, null), extra));
    }

    public static JSONObject previewBody(LlmSettings settings) throws JSONException {
        JSONArray messages = createLlmMessages(settings, "", "en", "…");
        for (int i = 0; i < messages.length(); i++) {
            messages.getJSONObject(i).put("content", "…");
        }
        return createBody(settings, messages, new JSONObject());
    }

    public static JSONObject defaultParameters(LlmSettings settings) throws JSONException {
        JSONObject parameters = new JSONObject();
        LlmParameters.apply(parameters, settings);
        return parameters;
    }

    private static JSONArray createLlmMessages(LlmSettings settings, String text, String language, String context) throws JSONException {
        String target = Locale.forLanguageTag(language).getDisplayName(Locale.getDefault());
        String source = settings.wrapText ? "<TEXT>" + text + "</TEXT>" : text;
        String prompt = settings.userPrompt
                .replace("@toLang", target)
                .replace("@text", source);

        JSONObject systemMessage = new JSONObject()
                .put("role", "system")
                .put("content", settings.systemPrompt);
        JSONArray messages = new JSONArray().put(systemMessage);

        if (settings.context && context != null && !context.isEmpty()) {
            String contextText = "Context for reference only; do not translate or repeat:\n"
                    + "<CONTEXT>\n" + context + "\n</CONTEXT>";
            JSONObject contextMessage = new JSONObject()
                    .put("role", "user")
                    .put("content", contextText);
            messages.put(contextMessage);
        }

        JSONObject userMessage = new JSONObject()
                .put("role", "user")
                .put("content", prompt);
        messages.put(userMessage);
        return messages;
    }

    private static JSONObject createBody(LlmSettings settings, JSONArray messages, JSONObject body) throws JSONException {
        if (settings.preset != LlmSettings.GOOGLE_AGENT_PLATFORM) {
            return body.put("model", settings.model).put("messages", messages);
        }

        JSONObject systemText = new JSONObject().put("text", messages.getJSONObject(0).getString("content"));
        JSONArray systemParts = new JSONArray().put(systemText);
        JSONObject systemInstruction = new JSONObject().put("parts", systemParts);

        JSONArray parts = new JSONArray();
        for (int i = 1; i < messages.length(); i++) {
            String text = messages.getJSONObject(i).getString("content");
            parts.put(new JSONObject().put("text", text));
        }
        JSONObject userContent = new JSONObject()
                .put("role", "user")
                .put("parts", parts);
        JSONArray contents = new JSONArray().put(userContent);

        return body
                .put("systemInstruction", systemInstruction)
                .put("contents", contents);
    }

    private static String parseLlmResponse(String response, boolean googleAgentPlatform) throws JSONException {
        JSONObject json = new JSONObject(response);
        String result;

        if (googleAgentPlatform) {
            JSONObject candidate = json.getJSONArray("candidates").getJSONObject(0);
            JSONArray parts = candidate.getJSONObject("content").getJSONArray("parts");
            StringBuilder output = new StringBuilder();
            for (int i = 0; i < parts.length(); i++) {
                JSONObject part = parts.getJSONObject(i);
                if (!part.optBoolean("thought")) {
                    output.append(part.optString("text", ""));
                }
            }
            result = output.toString();
        } else {
            JSONObject choice = json.getJSONArray("choices").getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            result = message.isNull("content") ? "" : message.getString("content");
        }

        if (result.isBlank()) {
            throw new IllegalArgumentException("Empty translation");
        }
        return result.trim();
    }

    public static TranslationTask models(LlmSettings settings, ResultCallback<List<String>> callback) {
        TranslationTask task = new TranslationTask();
        if (settings.preset == LlmSettings.GOOGLE_AGENT_PLATFORM) {
            task.post(() -> callback.done(ModelCatalog.GOOGLE_AGENT_PLATFORM_MODELS, null));
            return task;
        }
        if (settings.keys().isEmpty()) {
            task.post(() -> callback.done(null, getString(R.string.LlmConfigurationRequired)));
            return task;
        }

        try {
            Request request = new Request.Builder()
                    .url(settings.url + "/models")
                    .header("Authorization", "Bearer " + nextKey(settings))
                    .build();
            TranslationHttp.send(task, request, true, (body, response) -> {
                JSONObject json = new JSONObject(body);
                JSONArray data = json.optJSONArray("data");
                if (data == null) {
                    data = json.getJSONArray("models");
                }

                LinkedHashSet<String> ids = new LinkedHashSet<>();
                for (int i = 0; i < data.length(); i++) {
                    Object item = data.get(i);
                    String id;
                    if (item instanceof JSONObject object) {
                        id = object.optString("id", object.optString("name", ""));
                    } else {
                        id = item.toString();
                    }
                    id = id.trim();
                    if (id.startsWith("models/")) {
                        id = id.substring(7);
                    }
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                }

                ArrayList<String> result = new ArrayList<>(ids);
                ModelCatalog.sort(settings.preset, result);
                return result;
            }, (result, code, error) -> callback.done(result, error));
        } catch (IllegalArgumentException exception) {
            task.post(() -> callback.done(null, getString(R.string.LlmConfigurationRequired)));
        }
        return task;
    }
}
