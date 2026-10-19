package nu.gpu.aoigram.messages;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.telegram.messenger.R;
import org.telegram.ui.Components.ColoredImageSpan;

public final class ChannelSenderIcon {

    private static SpannableString icon;

    public static SpannableStringBuilder create() {
        if (icon == null) {
            ColoredImageSpan span = new ColoredImageSpan(R.drawable.settings_channel);
            span.setSize(dp(16));
            span.setWidth(dp(11));
            span.setTranslateX(-dp(2.5f));
            icon = new SpannableString("​");
            icon.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return new SpannableStringBuilder(icon);
    }
}
