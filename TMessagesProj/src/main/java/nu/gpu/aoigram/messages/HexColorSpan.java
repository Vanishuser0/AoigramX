package nu.gpu.aoigram.messages;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HexColorSpan extends ReplacementSpan {

    private static final Paint colorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int color;

    private HexColorSpan(int color) {
        this.color = color;
    }

    public static void apply(Spannable spannable, int start, int end) {
        int length = end - start;
        if (length != 7 && length != 9) {
            return;
        }
        try {
            int color = Color.parseColor(spannable.subSequence(start, end).toString());
            spannable.setSpan(new HexColorSpan(color), end - 1, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } catch (IllegalArgumentException ignore) {
        }
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        float size = paint.getTextSize() * 0.9f;
        return Math.round(paint.measureText(text, start, end) + size / 7.2f + size);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        for (CharacterStyle span : ((Spanned) text).getSpans(start, end, CharacterStyle.class)) {
            span.updateDrawState((TextPaint) paint);
        }
        canvas.drawText(text, start, end, x, y, paint);

        colorPaint.setColor(color);
        float size = paint.getTextSize() * 0.9f;
        float left = x + paint.measureText(text, start, end) + size / 7.2f;
        float boxTop = top + (bottom - top - size) / 2f;
        canvas.drawRoundRect(left, boxTop, left + size, boxTop + size, size * 0.285f, size * 0.285f, colorPaint);
    }
}
