package nu.gpu.aoigram.translate;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LineBackgroundSpan;

import androidx.annotation.NonNull;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import nu.gpu.aoigram.config.AoiConfig;

public final class TranslationDisplay {

    public static boolean prepare(MessageObject message) {
        if (message.messageOwner.voiceTranscriptionOpen || message.messageOwner.summarizedOpen) {
            message.aoiDisplayText = null;
            message.aoiDisplaySource = null;
            return false;
        }
        int mode = AoiConfig.translationShowOriginal.get();
        TLRPC.TL_textWithEntities translated = message.messageOwner.translatedText;
        if (message.aoiOriginalMode == mode && message.aoiDisplaySource == translated
                && message.aoiDisplayManual == message.aoiManualTranslation
        ) {
            return false;
        }
        message.aoiOriginalMode = mode;
        message.aoiDisplaySource = translated;
        message.aoiDisplayManual = message.aoiManualTranslation;
        message.aoiDisplayText = null;
        if (message.translated) {
            message.caption = null;
        }
        if (translated == null || message.messageOwner.rich_message != null
                || message.isPoll() || message.isVoiceTranscriptionOpen()
                || mode == 0 || mode == 1 && !message.aoiManualTranslation
                || message.messageOwner.message == null || message.messageOwner.message.isEmpty()
        ) {
            return true;
        }
        TLRPC.TL_textWithEntities combined = new TLRPC.TL_textWithEntities();
        String original = message.messageOwner.message;
        String gap = "\n\n";
        for (TLRPC.MessageEntity entity : translated.entities) {
            if (entity.offset == 0 && (entity instanceof TLRPC.TL_messageEntityPre || entity instanceof TLRPC.TL_messageEntityBlockquote)) {
                gap = "\n​\n";
                break;
            }
        }
        combined.text = original + gap + translated.text;
        for (TLRPC.MessageEntity entity : message.messageOwner.entities) {
            combined.entities.add(TLObject.deepCopy(entity, TLRPC.MessageEntity::TLdeserialize));
        }
        for (TLRPC.MessageEntity entity : translated.entities) {
            TLRPC.MessageEntity copy = TLObject.deepCopy(entity, TLRPC.MessageEntity::TLdeserialize);
            copy.offset += original.length() + gap.length();
            combined.entities.add(copy);
        }
        message.aoiDisplayText = combined;
        return true;
    }

    public static TLRPC.TL_textWithEntities result(MessageObject message) {
        return message.aoiDisplayText == null ? message.messageOwner.translatedText : message.aoiDisplayText;
    }

    public static CharSequence text(MessageObject message, CharSequence translated) {
        if (message.aoiDisplayText == null) return translated;
        SpannableStringBuilder text = new SpannableStringBuilder(message.aoiDisplayText.text);
        int offset = message.messageOwner.message.length() + 1;
        text.setSpan(new Separator(), offset, offset + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }

    public static void setSeparatorBounds(CharSequence text, float left, float right) {
        if (!(text instanceof Spanned spanned)) return;
        for (Separator separator : spanned.getSpans(0, text.length(), Separator.class)) {
            separator.left = left;
            separator.right = right;
        }
    }

    private static final class Separator implements LineBackgroundSpan {
        private float left;
        private float right = Float.MAX_VALUE;

        @Override
        public void drawBackground(@NonNull Canvas canvas, @NonNull Paint paint, int left, int right, int top, int baseline, int bottom, @NonNull CharSequence text, int start, int end, int lineNumber) {
            if (end - start != 1) return;
            int color = paint.getColor();
            paint.setColor(Theme.multAlpha(color, 0.2f));
            float y = (top + bottom) / 2f;
            canvas.drawRect(Math.max(left, this.left), y, Math.min(right, this.right), y + dp(1), paint);
            paint.setColor(color);
        }
    }
}
