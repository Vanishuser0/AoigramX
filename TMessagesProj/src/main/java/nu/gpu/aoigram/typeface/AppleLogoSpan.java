package nu.gpu.aoigram.typeface;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.core.graphics.PathParser;

public class AppleLogoSpan extends ReplacementSpan {

    private static final char LOGO = 0xF8FF;
    private static final float ADVANCE = 0.696f;
    private static final Path path = PathParser.createPathFromPathData("M360 -533C394 -533 447 -572 500 -572C553 -572 621 -525 634 -491C594 -461 560 -412 560 -348C560 -287 598 -211 658 -189C639 -132 575 4 492 4C450 4 405 -28 367 -28C330 -28 284 4 243 4C175 4 46 -147 46 -336C46 -450 137 -568 227 -568C280 -568 326 -533 360 -533ZM504 -752C424 -748 345 -669 352 -578C439 -593 507 -661 504 -752Z");

    public static boolean apply(Spannable s) {
        boolean applied = false;
        for (int i = TextUtils.indexOf(s, LOGO); i >= 0; i = TextUtils.indexOf(s, LOGO, i + 1)) {
            if (s.getSpans(i, i + 1, AppleLogoSpan.class).length == 0) {
                s.setSpan(new AppleLogoSpan(), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            applied = true;
        }
        return applied;
    }

    public static boolean contains(CharSequence s, int start, int end) {
        return TextUtils.indexOf(s, LOGO, start, end) >= 0;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        if (fm != null) {
            paint.getFontMetricsInt(fm);
        }
        return Math.round(paint.getTextSize() * ADVANCE);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        float scale = paint.getTextSize() / 1000f;
        for (CharacterStyle style : ((Spanned) text).getSpans(start, end, CharacterStyle.class)) {
            style.updateDrawState((TextPaint) paint);
        }
        canvas.save();
        canvas.translate(x, y);
        canvas.scale(scale, scale);
        canvas.drawPath(path, paint);
        canvas.restore();
    }
}
