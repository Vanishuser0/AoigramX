package nu.gpu.aoigram.translate.llm;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ModelCatalog {

    public static final List<String> GOOGLE_AGENT_PLATFORM_MODELS = List.of(
            "gemini-3.5-flash-lite", // default
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash-lite",
            "gemini-3.8-flash",
            "gemini-3.7-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-2.5-flash"
    );

    private static final List<String> NON_TEXT_KEYWORDS = List.of(
            "-live-",
            "-research",
            "-search",
            "antigravity-",
            "aqa",
            "asr-",
            "audio",
            "bge-",
            "chirp-",
            "computer-use",
            "csm-",
            "deepgram",
            "e5-",
            "embed",
            "flux",
            "gemini-omni",
            "gte-",
            "hailuo",
            "happyhorse",
            "i2v",
            "image",
            "imagine",
            "kling-v",
            "kokoro-",
            "krea-",
            "lyria",
            "minilm-",
            "minimax-h3",
            "moderation",
            "nano-banana",
            "orpheus-",
            "parakeet-",
            "perplexity",
            "quiverai",
            "r2v",
            "realtime",
            "recraft",
            "rerank",
            "riverflow",
            "robotics",
            "runway",
            "seedance",
            "seedream",
            "sentence-transformers",
            "sora",
            "speech",
            "stt",
            "t2v",
            "transcri",
            "tts",
            "veo-",
            "video",
            "voice",
            "voyage",
            "wan-",
            "whisper",
            "zonos"
    );

    public static boolean isTextGeneration(String model) {
        String normalized = model.toLowerCase(Locale.ROOT);
        for (String keyword : NON_TEXT_KEYWORDS) {
            if (normalized.contains(keyword)) return false;
        }
        return true;
    }

    public static void sort(LlmSettings.Preset preset, List<String> models) {
        if (preset == LlmSettings.GOOGLE_AGENT_PLATFORM) return;
        Comparator<String> order = String.CASE_INSENSITIVE_ORDER;
        if (preset == LlmSettings.OPENROUTER) {
            order = Comparator.comparing((String model) -> !model.toLowerCase(Locale.ROOT).endsWith(":free"))
                    .thenComparing(order);
        }
        models.sort(order);
    }
}
