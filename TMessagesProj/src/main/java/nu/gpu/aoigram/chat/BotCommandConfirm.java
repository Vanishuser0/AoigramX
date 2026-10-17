package nu.gpu.aoigram.chat;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

import nu.gpu.aoigram.config.AoiConfig;

public final class BotCommandConfirm {

    public static void run(BaseFragment fragment, String command, Runnable send) {
        if (!AoiConfig.botCommandConfirm.get()) {
            send.run();
            return;
        }
        fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(getString(R.string.BotCommandConfirmText))
                .setMessage(command)
                .setPositiveButton(getString(R.string.Send), (dialog, which) -> send.run())
                .setNegativeButton(getString(R.string.Cancel), null)
                .create());
    }
}
