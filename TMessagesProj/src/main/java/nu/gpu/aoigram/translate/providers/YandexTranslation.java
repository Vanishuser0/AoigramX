package nu.gpu.aoigram.translate.providers;

import org.json.JSONObject;

import java.util.UUID;

import nu.gpu.aoigram.translate.TranslationHttp;
import nu.gpu.aoigram.translate.TranslationService.ResultCallback;
import nu.gpu.aoigram.translate.TranslationTask;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;

public final class YandexTranslation {

    private static final String YANDEX_ID = UUID.randomUUID().toString().replace("-", "");

    public static void translate(TranslationTask task, String input, String language, ResultCallback<String> callback) {
        String requestId = UUID.randomUUID().toString().replace("-", "") + "-9-0";
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("translate.yandex.net")
                .addPathSegments("api/v1/tr.json/translate")
                .addQueryParameter("srv", "android")
                .addQueryParameter("uuid", YANDEX_ID)
                .addQueryParameter("id", requestId)
                .build();
        FormBody form = new FormBody.Builder()
                .add("text", input)
                .add("lang", language)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .post(form)
                .build();
        TranslationHttp.send(task, request, false,
                (body, response) -> new JSONObject(body).getJSONArray("text").getString(0),
                (result, code, error) -> callback.done(result, error));
    }
}
