package nu.gpu.aoigram.translate;

import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.tl.TL_iv;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class RichTranslation {

    private record Part(TL_iv.RichText source, Consumer<TL_iv.RichText> set) {}

    public static TL_iv.RichMessage copy(TL_iv.RichMessage source) {
        TL_iv.RichMessage result = TLObject.deepCopy(source, TL_iv.RichMessage::TLdeserialize);
        ArrayList<Part> original = new ArrayList<>();
        ArrayList<Part> copied = new ArrayList<>();
        collect(source.blocks, original);
        collect(result.blocks, copied);
        for (int i = 0; i < original.size(); i++) {
            copyInlineImages(original.get(i).source, copied.get(i).source);
        }
        return result;
    }

    private static void copyInlineImages(TL_iv.RichText source, TL_iv.RichText target) {
        if (source instanceof TL_iv.textImage image) {
            ((TL_iv.textImage) target).photo_id = image.photo_id;
        }
        if (source instanceof TL_iv.textDiff diff) {
            TL_iv.textDiff copy = (TL_iv.textDiff) target;
            copyInlineImages(diff.text, copy.text);
            copyInlineImages(diff.old_text, copy.old_text);
        } else if (source.text != null) {
            copyInlineImages(source.text, target.text);
        }
        for (int i = 0; i < source.texts.size(); i++) {
            copyInlineImages(source.texts.get(i), target.texts.get(i));
        }
    }

    public static void translate(TranslationTask task, TL_iv.RichMessage source, TranslationOptions options, String context, TranslationService.ResultCallback<TL_iv.RichMessage> callback) {
        TL_iv.RichMessage result = copy(source);
        translateBlocks(task, result.blocks, options, context, progress -> {}, (success, error) -> callback.done(error == null ? result : null, error));
    }

    public static void translateBlocks(TranslationTask task, List<TL_iv.PageBlock> blocks, TranslationOptions options, String context, Consumer<Float> progress, BiConsumer<Boolean, String> done) {
        ArrayList<Part> parts = new ArrayList<>();
        collect(blocks, parts);
        int[] remaining = {parts.size()};
        boolean[] success = {false};
        String[] failure = {null};
        if (parts.isEmpty()) {
            task.post(() -> done.accept(false, null));
        }
        for (Part part : parts) {
            RichTranslationText text = new RichTranslationText();
            TranslationService.translateHtml(task, text.encode(part.source), options, context, (translated, error) -> {
                if (error == null) {
                    try {
                        part.set.accept(text.decode(translated));
                        success[0] = true;
                    } catch (RuntimeException exception) {
                        failure[0] = TranslationService.failureText();
                    }
                } else {
                    failure[0] = error;
                }
                progress.accept(1f - (float) --remaining[0] / parts.size());
                if (remaining[0] == 0) {
                    done.accept(success[0], failure[0]);
                }
            });
        }
    }

    private static void add(ArrayList<Part> parts, TL_iv.RichText text, Consumer<TL_iv.RichText> set) {
        if (text != null && !(text instanceof TL_iv.textEmpty)) {
            parts.add(new Part(text, set));
        }
    }

    @SuppressWarnings("IfCanBeSwitch")
    private static void collect(List<TL_iv.PageBlock> blocks, ArrayList<Part> parts) {
        for (TL_iv.PageBlock block : blocks) {
            if (!(block instanceof TL_iv.pageBlockPreformatted)) {
                add(parts, block.text, text -> block.text = text);
            }
            if (block.caption != null) {
                add(parts, block.caption.text, text -> block.caption.text = text);
                add(parts, block.caption.credit, text -> block.caption.credit = text);
            }
            if (block instanceof TL_iv.pageBlockAuthorDate author) {
                add(parts, author.author, text -> author.author = text);
            } else if (block instanceof TL_iv.pageBlockBlockquote quote) {
                add(parts, quote.caption, text -> quote.caption = text);
            } else if (block instanceof TL_iv.pageBlockPullquote quote) {
                add(parts, quote.caption, text -> quote.caption = text);
            } else if (block instanceof TL_iv.pageBlockBlockquoteBlocks quote) {
                add(parts, quote.caption, text -> quote.caption = text);
                collect(quote.blocks, parts);
            } else if (block instanceof TL_iv.pageBlockCover cover) {
                collect(List.of(cover.cover), parts);
            } else if (block instanceof TL_iv.pageBlockEmbedPost post) {
                collect(post.blocks, parts);
            } else if (block instanceof TL_iv.pageBlockCollage collage) {
                collect(collage.items, parts);
            } else if (block instanceof TL_iv.pageBlockSlideshow slideshow) {
                collect(slideshow.items, parts);
            } else if (block instanceof TL_iv.pageBlockDetails details) {
                add(parts, details.title, text -> details.title = text);
                collect(details.blocks, parts);
            } else if (block instanceof TL_iv.pageBlockRelatedArticles related) {
                add(parts, related.title, text -> related.title = text);
            } else if (block instanceof TL_iv.pageBlockTable table) {
                add(parts, table.title, text -> table.title = text);
                for (TL_iv.pageTableRow row : table.rows) {
                    for (TL_iv.pageTableCell cell : row.cells) {
                        add(parts, cell.text, text -> cell.text = text);
                    }
                }
            } else if (block instanceof TL_iv.pageBlockList list) {
                for (TL_iv.PageListItem item : list.items) {
                    if (item instanceof TL_iv.TL_pageListItemText text) {
                        add(parts, text.text, value -> text.text = value);
                    } else if (item instanceof TL_iv.TL_pageListItemBlocks nested) {
                        collect(nested.blocks, parts);
                    }
                }
            } else if (block instanceof TL_iv.pageBlockOrderedList list) {
                for (TL_iv.PageListOrderedItem item : list.items) {
                    if (item instanceof TL_iv.TL_pageListOrderedItemText text) {
                        add(parts, text.text, value -> text.text = value);
                    } else if (item instanceof TL_iv.TL_pageListOrderedItemBlocks nested) {
                        collect(nested.blocks, parts);
                    }
                }
            }
        }
    }
}
