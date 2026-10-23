package nu.gpu.aoigram.translate;

import static org.telegram.messenger.LocaleController.getString;
import static nu.gpu.aoigram.translate.TranslationService.failureText;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.telegram.messenger.R;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class TranslationHttp {

    public interface Parser<T> {
        T parse(String body, Response response) throws Exception;
    }

    public interface HttpCallback<T> {
        void done(T result, int code, String error);
    }

    public interface RetryCallback<T> {
        void done(T result, int code, String error, long retryAfter);
    }

    private static final OkHttpClient HTTP;
    private static final OkHttpClient LLM_HTTP;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final List<String> BILLING_ERRORS = List.of(
            "\"insufficient_quota\"",
            "\"credit_balance_exhausted\"",
            "\"organization_spend_limit_exceeded\"",
            "\"project_spend_limit_exceeded\"",
            "\"organization_usage_limit_exceeded\""
    );

    static {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(5);
        dispatcher.setMaxRequestsPerHost(5);
        HTTP = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .build();
        Dispatcher llmDispatcher = new Dispatcher();
        llmDispatcher.setMaxRequestsPerHost(llmDispatcher.getMaxRequests());
        LLM_HTTP = HTTP.newBuilder()
                .dispatcher(llmDispatcher)
                .callTimeout(40, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build();
    }

    public static String format(String body) {
        try {
            JsonElement json = JsonParser.parseString(body);
            if (json.isJsonObject() || json.isJsonArray()) {
                return GSON.toJson(json);
            }
        } catch (JsonParseException ignored) {
        }
        return body;
    }

    public static boolean retryable(int code, long retryAfter) {
        return (code == 0 || code == 408 || code == 429 || code >= 500) && retryAfter >= 0 && retryAfter <= 10;
    }

    public static <T> void send(TranslationTask task, Request request, boolean llm, Parser<T> parser, HttpCallback<T> callback) {
        send(task, request, llm, parser, (result, code, error, retryAfter) -> {
            if (!llm && error != null && retryable(code, retryAfter)) {
                long delay = retryAfter > 0 ? retryAfter * 1000 : 1000L;
                task.post(() -> send(task, request, false, parser, (retried, retriedCode, retriedError, ignored) -> callback.done(retried, retriedCode, retriedError)), delay);
            } else {
                callback.done(result, code, error);
            }
        });
    }

    public static <T> void send(TranslationTask task, Request request, boolean llm, Parser<T> parser, RetryCallback<T> callback) {
        Call call = (llm ? LLM_HTTP : HTTP).newCall(request);
        task.add(call);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException exception) {
                task.remove(call);
                task.httpCode = 0;
                task.post(() -> callback.done(null, 0, getString(R.string.TranslationNetworkError), exception instanceof InterruptedIOException ? -1 : 0));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    String body = response.body().string();
                    int code = llm && response.code() == 429 && BILLING_ERRORS.stream().anyMatch(body::contains) ? 402 : response.code();
                    task.httpCode = code;
                    if (!response.isSuccessful()) {
                        String status = "HTTP " + response.code();
                        String type = response.header("Content-Type");
                        boolean html = type != null && type.startsWith("text/html");
                        String error = body.isBlank() || html ? status : status + "\n" + format(body.trim());
                        String header = response.header("Retry-After");
                        long retryAfter = header != null && header.matches("\\d+") ? Long.parseLong(header) : 0;
                        task.post(() -> callback.done(null, code, error, retryAfter));
                    } else {
                        T result = parser.parse(body, response);
                        task.post(() -> callback.done(result, code, null, 0));
                    }
                } catch (IOException exception) {
                    task.httpCode = 0;
                    task.post(() -> callback.done(null, 0, getString(R.string.TranslationNetworkError), exception instanceof InterruptedIOException ? -1 : 0));
                } catch (Exception exception) {
                    task.post(() -> callback.done(null, -1, failureText(), 0));
                } finally {
                    task.remove(call);
                }
            }
        });
    }
}
