package nu.gpu.aoigram.translate;

import android.text.Html;
import android.text.TextUtils;

import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.ui.iv.RichTextStyle;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;

final class RichTranslationText {

    private record Element(TL_iv.RichText node, ArrayList<TL_iv.RichText> children) {}

    private final ArrayList<TL_iv.RichText> nodes = new ArrayList<>();

    @SuppressWarnings("IfCanBeSwitch")
    String encode(TL_iv.RichText text) {
        if (text == null || text instanceof TL_iv.textEmpty) return "";
        if (text instanceof TL_iv.textPlain plain) return escape(plain.text);
        if (text instanceof TL_iv.textConcat concat) {
            StringBuilder result = new StringBuilder();
            for (TL_iv.RichText child : concat.texts) {
                result.append(encode(child));
            }
            return result.toString();
        }
        int id = nodes.size();
        nodes.add(text);
        String tag = tag(text);
        String attributes = " id=" + id;
        if (text instanceof TL_iv.textImage) return "<img" + attributes + ">";
        String content;
        if (text instanceof TL_iv.textMath math) {
            content = escape(math.source);
        } else if (text instanceof TL_iv.textCustomEmoji emoji) {
            content = escape(emoji.alt);
        } else if (text instanceof TL_iv.textDiff diff) {
            content = encode(RichTextStyle.isEmpty(diff.text) ? diff.old_text : diff.text);
        } else {
            content = encode(text.text);
        }
        return "<" + tag + attributes + ">" + content + "</" + tag + ">";
    }

    TL_iv.RichText decode(String html) {
        ArrayList<Element> stack = new ArrayList<>();
        Element root = new Element(null, new ArrayList<>());
        stack.add(root);
        Html.fromHtml("<aoi>" + html + "</aoi>", Html.FROM_HTML_MODE_COMPACT, null, (opening, tag, output, reader) -> {
            if (!opening || !tag.equals("aoi")) return;
            reader.setContentHandler(new DefaultHandler() {
                @Override
                public void characters(char[] ch, int start, int length) {
                    stack.get(stack.size() - 1).children.add(plain(new String(ch, start, length)));
                }

                @Override
                public void startElement(String uri, String local, String name, Attributes attributes) {
                    if (local.equals("br")) {
                        stack.get(stack.size() - 1).children.add(plain("\n"));
                        return;
                    }
                    TL_iv.RichText node;
                    String id = attributes.getValue("id");
                    if (id != null && id.matches("[0-9]+") && id.length() < 9 && Integer.parseInt(id) < nodes.size()) {
                        node = copy(nodes.get(Integer.parseInt(id)));
                    } else {
                        node = switch (local) {
                            case "b", "strong" -> new TL_iv.textBold();
                            case "i", "em" -> new TL_iv.textItalic();
                            case "u" -> new TL_iv.textUnderline();
                            case "s", "del", "strike" -> new TL_iv.textStrike();
                            case "code", "pre" -> new TL_iv.textFixed();
                            case "sub" -> new TL_iv.textSubscript();
                            case "sup" -> new TL_iv.textSuperscript();
                            case "mark" -> new TL_iv.textMarked();
                            default -> null;
                        };
                        if (local.equals("a") && attributes.getValue("href") != null) {
                            node = new TL_iv.textUrl();
                            node.url = attributes.getValue("href");
                        }
                    }
                    stack.add(new Element(node, new ArrayList<>()));
                }

                @Override
                public void endElement(String uri, String local, String name) {
                    if (local.equals("br") || stack.size() == 1) return;
                    Element element = stack.remove(stack.size() - 1);
                    TL_iv.RichText content = concat(element.children);
                    TL_iv.RichText node = element.node;
                    if (node == null) {
                        node = content;
                    } else if (node instanceof TL_iv.textMath math) {
                        math.source = RichTextStyle.plainOf(content);
                    } else if (node instanceof TL_iv.textCustomEmoji emoji) {
                        emoji.alt = RichTextStyle.plainOf(content);
                    } else if (node instanceof TL_iv.textDiff diff) {
                        if (RichTextStyle.isEmpty(diff.text)) {
                            diff.old_text = content;
                        } else {
                            diff.text = content;
                        }
                    } else if (!(node instanceof TL_iv.textImage)) {
                        node.text = content;
                    }
                    stack.get(stack.size() - 1).children.add(node);
                }
            });
        });
        return concat(root.children);
    }

    private static TL_iv.RichText copy(TL_iv.RichText source) {
        TL_iv.RichText result = TLObject.deepCopy(source, TL_iv.RichText::TLdeserialize);
        if (source instanceof TL_iv.textImage image) {
            ((TL_iv.textImage) result).photo_id = image.photo_id;
        }
        return result;
    }

    private static String tag(TL_iv.RichText text) {
        if (text instanceof TL_iv.textBold) return "b";
        if (text instanceof TL_iv.textItalic) return "i";
        if (text instanceof TL_iv.textUnderline) return "u";
        if (text instanceof TL_iv.textStrike) return "s";
        if (text instanceof TL_iv.textFixed || text instanceof TL_iv.textMath) return "code";
        if (text instanceof TL_iv.textSubscript) return "sub";
        if (text instanceof TL_iv.textSuperscript) return "sup";
        if (text instanceof TL_iv.textMarked) return "mark";
        if (text instanceof TL_iv.textUrl || text instanceof TL_iv.textEmail || text instanceof TL_iv.textPhone) return "a";
        return "span";
    }

    private static String escape(String text) {
        return TextUtils.htmlEncode(text).replace("\n", "<br>");
    }

    private static TL_iv.RichText plain(String value) {
        TL_iv.textPlain text = new TL_iv.textPlain();
        text.text = value;
        return text;
    }

    private static TL_iv.RichText concat(ArrayList<TL_iv.RichText> children) {
        if (children.isEmpty()) return new TL_iv.textEmpty();
        if (children.size() == 1) return children.get(0);
        TL_iv.textConcat concat = new TL_iv.textConcat();
        concat.texts = children;
        return concat;
    }
}
