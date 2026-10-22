package nu.gpu.aoigram.utils;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;

import java.util.function.Consumer;

public final class ErrorFeedback {

    public static void show(String error, boolean vibrate) {
        show(error, BulletinFactory.global(), AlertDialog::show, vibrate);
    }

    public static void show(String error, BulletinFactory bulletins, Consumer<AlertDialog> showDialog, boolean vibrate) {
        if (vibrate) BotWebViewVibrationEffect.APP_ERROR.vibrate();
        if (error.length() > 45) {
            showDialog.accept(AlertsCreator.createSimpleAlert(bulletins.getContext(),
                    getString(R.string.ErrorOccurred), error, bulletins.getResourcesProvider()).create());
        } else {
            bulletins.createSimpleBulletin(R.raw.chats_infotip, error).show();
        }
    }

    public static void vibrate(View view) {
        AndroidUtilities.shakeViewSpring(view, -6);
        BotWebViewVibrationEffect.APP_ERROR.vibrate();
    }
}
