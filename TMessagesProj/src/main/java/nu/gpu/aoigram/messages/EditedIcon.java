package nu.gpu.aoigram.messages;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColoredImageSpan;

public final class EditedIcon {

    private static final String PLACEHOLDER = "\u200B";
    private static SpannableString stub;

    private SpannableString icon;

    public CharSequence get() {
        if (icon == null) {
            icon = create(true);
        }
        return icon;
    }

    public static CharSequence stub() {
        if (stub == null) {
            stub = create(false);
        }
        return stub;
    }

    public static int width() {
        return (int) Math.ceil(Layout.getDesiredWidth(stub(), Theme.chat_timePaint));
    }

    public static String describe(CharSequence text) {
        return text.toString().replace(PLACEHOLDER, getString(R.string.EditedMessage) + " ");
    }

    private static SpannableString create(boolean draw) {
        ColoredImageSpan span = new ColoredImageSpan(R.drawable.filled_profile_edit_24);
        span.setSize(dp(14));
        span.setWidth(dp(16.5f));
        span.setTranslateX(-dp(2.5f));
        span.draw = draw;
        SpannableString text = new SpannableString("\u200E" + PLACEHOLDER);
        text.setSpan(span, 1, 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return text;
    }
}
