package nu.gpu.aoigram.translate.providers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.gpu.aoigram.translate.TranslationHttp;
import nu.gpu.aoigram.translate.TranslationService.ResultCallback;
import nu.gpu.aoigram.translate.TranslationTask;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;

public final class BingTranslation {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/151.0.4129.59";

    private record Config(String host, String ig, String iid, String key, String token, long expires) {}

    private record Result(String text, boolean genderDebiased) {}

    private static final AtomicInteger COUNT = new AtomicInteger();
    private static Config config;

    public static void translate(TranslationTask task, String text, String language, ResultCallback<String> callback) {
        Config current = config;
        if (current != null && current.expires >= System.currentTimeMillis()) {
            send(task, current, text, language, callback);
            return;
        }
        Request request = new Request.Builder()
                .url("https://www.bing.com/translator")
                .header("User-Agent", USER_AGENT)
                .build();
        TranslationHttp.send(task, request, false, (body, response) -> {
            String ig = extract(body, "IG:\\s*\"([^\"]+)\"");
            String iid = extract(body, "data-iid=\"([^\"]+)\"");
            JSONArray params = new JSONArray(extract(body, "params_AbusePreventionHelper\\s*=\\s*(\\[[^\\]]+\\])"));
            long expires = params.getLong(0) + params.getLong(2);
            return new Config(response.request().url().host(), ig, iid, String.valueOf(params.get(0)), params.getString(1), expires);
        }, (result, code, error) -> {
            if (error != null) {
                callback.done(null, error);
            } else {
                config = result;
                COUNT.set(0);
                send(task, result, text, language, callback);
            }
        });
    }

    private static void send(TranslationTask task, Config config, String text, String language, ResultCallback<String> callback) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host(config.host)
                .addPathSegment("ttranslatev3")
                .addQueryParameter("isVertical", "1")
                .addQueryParameter("IG", config.ig)
                .addQueryParameter("IID", config.iid)
                .addQueryParameter("SFX", Integer.toString(COUNT.incrementAndGet()))
                .addQueryParameter("ref", "TThis")
                .addQueryParameter("edgepdftranslator", "1")
                .build();
        FormBody.Builder body = new FormBody.Builder()
                .add("fromLang", "auto-detect")
                .add("to", language)
                .add("text", text)
                .add("token", config.token)
                .add("key", config.key)
                .add("tryFetchingGenderDebiasedTranslations", "true");
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", "https://" + config.host + "/translator")
                .post(body.build())
                .build();
        TranslationHttp.send(task, request, false, (response, metadata) -> {
            String type = metadata.header("Content-Type");
            if (type != null && type.startsWith("application/json")) {
                return new Result(new JSONArray(response).getJSONObject(0)
                        .getJSONArray("translations")
                        .getJSONObject(0)
                        .getString("text"), false);
            }
            if (metadata.header("isgenderdebiasedtranslation") != null) {
                return new Result(null, true);
            }
            throw new IllegalArgumentException("Unexpected Bing response");
        }, (result, code, error) -> {
            if (error != null) {
                callback.done(null, error);
            } else if (result.genderDebiased) {
                Request genderRequest = request.newBuilder().post(body.add("isGenderDebiasViewPresent", "true").build()).build();
                TranslationHttp.send(task, genderRequest, false,
                        (response, metadata) -> new JSONObject(response).getString("masculineTranslation"),
                        (translated, status, failure) -> callback.done(translated, failure));
            } else {
                callback.done(result.text, null);
            }
        });
    }

    private static String extract(String body, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(body);
        if (!matcher.find()) throw new IllegalArgumentException("Missing Bing configuration");
        return matcher.group(1);
    }
}
