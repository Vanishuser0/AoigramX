package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SavedMessagesController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.utils.AppUtil;
import nu.gpu.aoigram.utils.ErrorFeedback;

public final class SettingsBackup {

    private static final String FILE_PREFIX = "aoigram-settings-";
    private static final String FILE_SUFFIX = ".json";

    static void export(BaseFragment fragment) {
        File file = new File(AndroidUtilities.getCacheDir(), FILE_PREFIX + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + FILE_SUFFIX);
        try {
            Files.write(file.toPath(), AoiConfig.exportJson().toString(2).getBytes(StandardCharsets.UTF_8));
        } catch (IOException | JSONException e) {
            FileLog.e(e);
            ErrorFeedback.show(getString(R.string.ErrorOccurred), BulletinFactory.of(fragment), fragment::showDialog, true);
            return;
        }
        String caption = AoiConfig.hasSecrets() ? getString(R.string.SettingsBackupSecrets) : null;
        SendMessagesHelper.prepareSendingDocument(fragment.getAccountInstance(), file.getPath(), file.getPath(), null, caption, null, fragment.getUserConfig().getClientUserId(), null, null, null, null, null, true, 0, null, null, false);
        BulletinFactory.of(fragment).createSimpleBulletin(R.raw.saved_messages, AndroidUtilities.replaceSingleTag(getString(R.string.SettingsExported), -1, AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD, SavedMessagesController::openSavedMessages)).show();
    }

    public static boolean isBackup(MessageObject message) {
        String name = message.getDocumentName();
        return name.startsWith(FILE_PREFIX) && name.endsWith(FILE_SUFFIX);
    }

    public static void showImportAlert(BaseFragment fragment, MessageObject message) {
        AlertDialog alertDialog = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                .setTitle(getString(R.string.ImportSettings))
                .setMessage(getString(R.string.ImportSettingsAlert))
                .setNegativeButton(getString(R.string.Cancel), null)
                .setPositiveButton(getString(R.string.ImportSettingsButton), (dialog, which) -> {
                    try {
                        AoiConfig.importJson(new JSONObject(new String(Files.readAllBytes(getFile(message).toPath()), StandardCharsets.UTF_8)));
                    } catch (IOException | JSONException e) {
                        FileLog.e(e);
                        ErrorFeedback.show(getString(R.string.ErrorOccurred), BulletinFactory.of(fragment), fragment::showDialog, true);
                        return;
                    }
                    AppUtil.restart(fragment.getParentActivity());
                })
                .create();
        fragment.showDialog(alertDialog);
        alertDialog.redPositive();
    }

    private static File getFile(MessageObject message) {
        String attachPath = message.messageOwner.attachPath;
        if (!TextUtils.isEmpty(attachPath) && new File(attachPath).exists()) {
            return new File(attachPath);
        }
        return FileLoader.getInstance(message.currentAccount).getPathToMessage(message.messageOwner);
    }
}
