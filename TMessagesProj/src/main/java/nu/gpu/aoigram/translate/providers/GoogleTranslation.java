package nu.gpu.aoigram.translate.providers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Pattern;

import nu.gpu.aoigram.translate.TranslationHttp;
import nu.gpu.aoigram.translate.TranslationService.ResultCallback;
import nu.gpu.aoigram.translate.TranslationTask;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public final class GoogleTranslation {

    private static final MediaType JSON = MediaType.get("application/json+protobuf");
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/153.0.0.0 Safari/537.36";
    private static final Pattern TEXT = Pattern.compile("[^\n]+");
    private static final Pattern BREAKS = Pattern.compile("\n+");

    public static void translate(TranslationTask task, String input, String language, ResultCallback<String> callback) {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("translate-pa.googleapis.com")
                .addPathSegments("v1/translate")
                .addQueryParameter("params.client", "gtx")
                .addQueryParameter("query.source_language", "auto")
                .addQueryParameter("query.target_language", language)
                .addQueryParameter("query.display_language", "en-US")
                .addQueryParameter("data_types", "TRANSLATION")
                .addQueryParameter("key", "AIza" + "SyDLEeFI5OtFBwYBIoK_jj5m32rZK5CkCXA")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(new FormBody.Builder().add("query.text", input).build())
                .header("X-HTTP-Method-Override", "GET")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("User-Agent", USER_AGENT)
                .build();
        TranslationHttp.send(task, request, false,
                (body, response) -> restoreNewlines(input, new JSONObject(body).getString("translation")),
                (result, code, error) -> callback.done(result, error));
    }

    public static void translateHtml(TranslationTask task, String input, String language, ResultCallback<String> callback) {
        JSONArray texts = new JSONArray().put(input.replace("<br>", "<hr>"));
        JSONArray query = new JSONArray().put(new JSONArray().put(texts).put("auto").put(language)).put("te");
        Request request = new Request.Builder()
                .url("https://translate-pa.googleapis.com/v1/translateHtml")
                .post(RequestBody.create(query.toString(), JSON))
                .header("X-Goog-API-Key", "AIza" + "SyATBXajvzQLTDHEQbcpq0Ihe0vWDHmO520")
                .header("User-Agent", USER_AGENT)
                .build();
        TranslationHttp.send(task, request, false,
                (body, response) -> new JSONArray(body).getJSONArray(0).getString(0).replace("<hr> ", "<br>").replace("<hr>", "<br>"),
                (result, code, error) -> callback.done(result, error));
    }

    private static String restoreNewlines(String input, String translation) {
        String[] breaks = TEXT.split(input);
        String[] lines = BREAKS.split(translation);
        if (breaks.length != lines.length) return translation;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            result.append(breaks[i]).append(lines[i]);
        }
        return result.toString();
    }
}
