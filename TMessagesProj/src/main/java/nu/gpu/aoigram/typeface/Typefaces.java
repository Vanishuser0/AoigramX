package nu.gpu.aoigram.typeface;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpr;
import static org.telegram.messenger.LocaleController.getString;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.graphics.fonts.FontStyle;
import android.graphics.fonts.SystemFonts;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.LeadingMarginSpan;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.EmojiData;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.TypefaceSpan;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Locale;

import nu.gpu.aoigram.config.AoiConfig;

public class Typefaces {

    private static final int TEST_SIZE = 40;
    private static final Typeface flexFamily = Typeface.create("google-sans-flex", Typeface.NORMAL);
    private static final Typeface sansFamily = flexFamily != Typeface.DEFAULT ? flexFamily : Typeface.create("google-sans-text", Typeface.NORMAL);
    private static final int MEDIUM_WEIGHT = flexFamily != Typeface.DEFAULT ? 550 : 500;
    private static final TextPaint emojiPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint.FontMetrics emojiMetrics = new Paint.FontMetrics();
    private static final String[][] systemEmojis = Arrays.stream(EmojiData.data).map(page -> new String[page.length]).toArray(String[][]::new);
    private static final String testText = switch (LocaleController.getInstance().getCurrentLocale().getLanguage()) {
        case "zh" -> "你好";
        case "ja" -> "こんにちは";
        case "ko" -> "안녕하세요";
        case "ar", "fa" -> "مرحبا";
        case "iw" -> "שלום";
        case "th" -> "สวัสดี";
        case "hi" -> "नमस्ते";
        case "ru", "uk", "ky", "be", "sr" -> "Привет";
        default -> "R";
    };
    private static final Typeface regularTypeface = AoiConfig.systemFonts.get() && sansFamily != Typeface.DEFAULT ? sans(400, false) : null;

    private static Typeface titleTypeface;

    static {
        emojiPaint.setTextAlign(Paint.Align.CENTER);
        emojiPaint.setTextLocale(Locale.forLanguageTag("und-Zsye"));
    }

    public static SpannableStringBuilder titleText() {
        if (titleTypeface == null) {
            titleTypeface = AoiConfig.systemFonts.get() ? sans(600, false) : Typeface.create(null, 600, false);
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(getString(R.string.AppNameAoi));
        builder.setSpan(new LeadingMarginSpan.Standard(dp(2), 0), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new TypefaceSpan(titleTypeface, 0, Theme.key_telegram_color_dialogsLogo, null), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new CharacterStyle() {
            @Override
            public void updateDrawState(TextPaint tp) {
                tp.baselineShift -= dpr(1.3f);
            }
        }, 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    public static Typeface system(String assetPath) {
        return switch (assetPath) {
            case AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM -> sans(MEDIUM_WEIGHT, false);
            case AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM_ITALIC -> sans(MEDIUM_WEIGHT, true);
            case AndroidUtilities.TYPEFACE_ROBOTO_EXTRA_BOLD -> sans(800, false);
            case "fonts/ritalic.ttf" -> sans(400, true);
            case "fonts/rcondensedbold.ttf" -> Typeface.create("sans-serif-condensed", Typeface.BOLD);
            case AndroidUtilities.TYPEFACE_ROBOTO_MONO -> Typeface.MONOSPACE;
            default -> null;
        };
    }

    public static Typeface asset(String assetPath) {
        Typeface.Builder builder = new Typeface.Builder(ApplicationLoader.applicationContext.getAssets(), assetPath);
        if (assetPath.contains("medium")) {
            builder.setWeight(700);
        }
        if (assetPath.contains("italic")) {
            builder.setItalic(true);
        }
        return builder.build();
    }

    public static Typeface regular() {
        return regularTypeface != null ? regularTypeface : Typeface.DEFAULT;
    }

    public static void setRegular(Paint... paints) {
        for (Paint paint : paints) {
            paint.setTypeface(regularTypeface);
        }
    }

    public static void applyTheme(Resources.Theme theme) {
        if (regularTypeface != null) {
            theme.applyStyle(sansFamily == flexFamily ? R.style.SystemFontsFlex : R.style.SystemFontsText, true);
        }
    }

    public static String systemEmoji(String emoji) {
        return emojiPaint.hasGlyph(emoji) ? emoji : null;
    }

    public static String systemEmoji(int page, int page2) {
        String emoji = systemEmojis[page][page2];
        if (emoji == null) {
            String fixed = Emoji.fixEmoji(EmojiData.data[page][page2]);
            emoji = systemEmojis[page][page2] = emojiPaint.hasGlyph(fixed) ? fixed : "";
        }
        return emoji.isEmpty() ? null : emoji;
    }

    public static void drawEmoji(Canvas canvas, String emoji, Rect rect, int alpha) {
        emojiPaint.setTextSize(rect.height() * 0.85f);
        emojiPaint.setAlpha(alpha);
        emojiPaint.getFontMetrics(emojiMetrics);
        canvas.drawText(emoji, rect.exactCenterX(), rect.exactCenterY() - (emojiMetrics.ascent + emojiMetrics.descent) / 2f, emojiPaint);
    }

    private static Typeface sans(int weight, boolean italic) {
        Typeface regular = Typeface.create(sansFamily, 400, italic);
        if (weight == 400) {
            return regular;
        }
        Typeface typeface = weight % 100 != 0 ? flex(weight, italic) : Typeface.create(sansFamily, weight, italic);
        return render(typeface).sameAs(render(regular)) ? Typeface.create(sansFamily, italic ? Typeface.BOLD_ITALIC : Typeface.BOLD) : typeface;
    }

    private static Typeface flex(int weight, boolean italic) {
        for (Font font : SystemFonts.getAvailableFonts()) {
            File file = font.getFile();
            if (file != null && file.getName().equals("GoogleSansFlex-Regular.ttf")) {
                try {
                    FontStyle style = new FontStyle(weight, italic ? FontStyle.FONT_SLANT_ITALIC : FontStyle.FONT_SLANT_UPRIGHT);
                    Font weighted = new Font.Builder(file)
                            .setWeight(weight)
                            .setSlant(style.getSlant())
                            .setFontVariationSettings("'wght' " + weight + (italic ? ", 'slnt' -10" : ""))
                            .build();
                    return new Typeface.CustomFallbackBuilder(new FontFamily.Builder(weighted).build()).setStyle(style).setSystemFallback("sans-serif").build();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return Typeface.create(sansFamily, weight, italic);
    }

    private static Bitmap render(Typeface typeface) {
        Bitmap bitmap = Bitmap.createBitmap(TEST_SIZE * 2, TEST_SIZE, Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        paint.setTextSize(TEST_SIZE / 2f);
        paint.setTypeface(typeface);
        new Canvas(bitmap).drawText(testText, 0, TEST_SIZE, paint);
        return bitmap;
    }
}
