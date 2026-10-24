package nu.gpu.aoigram.chat;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.view.HapticFeedbackConstants;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.utils.tlutils.TLKeyboardHelper;
import org.telegram.tgnet.tl.TL_keyboard;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Components.BulletinFactory;

import java.nio.charset.StandardCharsets;

public final class BotButtonMenu {

    public static void show(BaseFragment fragment, View cell, TL_keyboard.KeyboardButtonProto button) {
        TL_keyboard.TL_inlineButtonTypeCallback callback = TLKeyboardHelper.getType(button, TL_keyboard.TL_inlineButtonTypeCallback.class);
        String data = callback != null ? new String(callback.data, StandardCharsets.UTF_8) : null;
        BottomSheet.Builder builder = new BottomSheet.Builder(fragment.getParentActivity(), false, fragment.getResourceProvider());
        builder.setTitle(button.getText());
        builder.setTitleMultipleLines(true);
        builder.setItems(new CharSequence[] {
                getString(R.string.Copy),
                data != null ? getString(R.string.CopyCallbackData) : null
        }, (dialog, which) -> {
            String text = which == 0 ? button.getText() : data;
            AndroidUtilities.addToClipboard(text);
            BulletinFactory.of(fragment).createCopyBulletin(formatString(R.string.ExactTextCopied, text)).show();
        });
        fragment.showDialog(builder.create());
        cell.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
    }
}
