package nu.gpu.aoigram.translate;

import org.telegram.tgnet.tl.TL_iv;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ArticleTranslation {

    public static final int MENU_ID = 1001;
    private final ArrayList<TL_iv.PageBlock> original;
    private TranslationTask task;
    private boolean translated;

    public ArticleTranslation(ArrayList<TL_iv.PageBlock> original) {
        this.original = original;
    }

    public boolean isTranslated() {
        return translated;
    }

    public void showOriginal(Consumer<List<TL_iv.PageBlock>> display) {
        translated = false;
        display.accept(original);
    }

    public TranslationTask translate(Consumer<List<TL_iv.PageBlock>> display, Consumer<Float> progress, Consumer<String> done) {
        cancel();
        TL_iv.RichMessage source = new TL_iv.RichMessage();
        source.blocks = original;
        TL_iv.RichMessage result = RichTranslation.copy(source);
        int provider = TranslationOptions.defaultProvider();
        if (provider == TranslationOptions.TELEGRAM) {
            provider = TranslationOptions.GOOGLE;
        }
        TranslationOptions options = new TranslationOptions(provider, TranslationOptions.defaultLanguage());
        task = new TranslationTask();
        RichTranslation.translateBlocks(task, result.blocks, options, null, progress, (success, error) -> {
            task = null;
            if (success) {
                translated = true;
                display.accept(result.blocks);
            }
            done.accept(error);
        });
        return task;
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
