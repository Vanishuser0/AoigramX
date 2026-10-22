package nu.gpu.aoigram.translate.llm;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

final class LlmParameters {

    static void apply(JSONObject body, LlmSettings settings) throws JSONException {
        String fullModel = settings.model.toLowerCase(Locale.ROOT);
        String model = fullModel.substring(fullModel.lastIndexOf('/') + 1);
        if (isNonReasoning(model)) return;

        boolean routedModel = fullModel.contains("/");
        if (settings.preset == LlmSettings.GOOGLE_AGENT_PLATFORM) {
            body.put("generationConfig", new JSONObject().put("thinkingConfig", geminiThinkingConfig(model)));
        } else if (routedModel && settings.url.equals(LlmSettings.OPENROUTER.url())) {
            body.put("reasoning", new JSONObject().put("effort", reasoningEffort(model)));
        } else if (routedModel && settings.url.equals(LlmSettings.VERCEL.url())) {
            applyVercel(body, fullModel, model);
        } else if (model.contains("deepseek")) {
            body.put("thinking", new JSONObject().put("type", "disabled"));
        } else {
            body.put("reasoning_effort", reasoningEffort(model));
        }
    }

    private static boolean isNonReasoning(String model) {
        if (model.contains("non-reasoning")) return true;
        return model.startsWith("gpt-5") && (model.contains("instant") || model.contains("chat"));
    }

    private static JSONObject geminiThinkingConfig(String model) throws JSONException {
        if (model.startsWith("gemini-2")) {
            return new JSONObject().put("thinkingBudget", model.contains("pro") ? 128 : 0);
        }
        return new JSONObject().put("thinkingLevel", geminiThinkingLevel(model));
    }

    private static String geminiThinkingLevel(String model) {
        if (model.startsWith("gemini-3")) {
            return model.contains("pro") || model.startsWith("gemini-3.7") || model.startsWith("gemini-3.8") ? "LOW" : "MINIMAL";
        }
        if (model.contains("gemma4") || model.contains("gemma-4")) return "MINIMAL";
        return "LOW";
    }

    private static void applyVercel(JSONObject body, String fullModel, String model) throws JSONException {
        JSONObject providerOptions = new JSONObject().put("gateway", new JSONObject().put("sort", "ttft"));
        body.put("providerOptions", providerOptions);
        if (fullModel.contains("deepseek")) {
            providerOptions.put("deepseek", new JSONObject().put("thinking", new JSONObject().put("type", "disabled")));
        } else {
            body.put("reasoning", new JSONObject().put("effort", reasoningEffort(model)));
        }
    }

    private static String reasoningEffort(String model) {
        if (model.startsWith("gpt-oss")) return "low";
        if (model.startsWith("gpt-5.")) return "none";
        if (model.startsWith("gpt-5")) return "minimal";
        if (model.startsWith("gpt-6")) {
            return model.contains("luna") || model.contains("terra") ? "none" : "low";
        }
        if (model.startsWith("gemini")) {
            boolean legacy = model.startsWith("gemini-2") || model.startsWith("gemini-3-") || model.startsWith("gemini-3.1");
            if (model.contains("pro")) return "low";
            if (model.contains("flash-lite") && !legacy) return "minimal";
            if (model.endsWith("latest") || !legacy) return "low";
        }
        if (model.contains("gemma4") || model.contains("gemma-4")) return "minimal";
        if (model.startsWith("grok-4") || model.startsWith("glm-5.3")) return "low";
        if (model.startsWith("muse-spark")) return "minimal";
        return "none";
    }
}
