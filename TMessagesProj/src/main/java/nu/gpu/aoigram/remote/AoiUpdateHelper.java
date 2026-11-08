package nu.gpu.aoigram.remote;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;

public final class AoiUpdateHelper {

    private AoiUpdateHelper() {
    }

    /**
     * 检查新版本（走 Telegram 原生 MTProto Inline Query 通道）
     */
    public static void checkUpdate(Context context, boolean manual, Utilities.Callback<Boolean> callback) {
        AoiBotQueryHelper.queryInline("update", (description) -> {
            if (!TextUtils.isEmpty(description)) {
                try {
                    JSONObject json = new JSONObject(description);
                    int currentCode = SharedConfig.buildVersion();
                    int remoteCode = json.optInt("versionCode", 0);
                    String remoteVersion = json.optString("version", "");
                    String downloadUrl = json.optString("downloadUrl", "");
                    String changelog = json.optString("changelog", "");
                    boolean force = json.optBoolean("force", false);
                    boolean hasUpdate = (remoteCode > currentCode);

                    if (hasUpdate && !TextUtils.isEmpty(downloadUrl)) {
                        showUpdateDialog(context, remoteVersion, changelog, downloadUrl, force);
                    } else if (manual) {
                        showBulletin(context, LocaleController.getString(R.string.YourVersionIsLatest));
                    }
                    if (callback != null) {
                        callback.run(hasUpdate);
                    }
                    return;
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            if (manual) {
                showBulletin(context, LocaleController.getString(R.string.AppUpdateFailed));
            }
            if (callback != null) {
                callback.run(false);
            }
        });
    }

    private static void showUpdateDialog(Context context, String version, String changelog, String downloadUrl, boolean force) {
        if (context instanceof Activity activity && activity.isFinishing()) {
            return;
        }
        if (context == null) {
            context = ApplicationLoader.applicationContext;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString(R.string.AppUpdateTitle) + " v" + version);
        builder.setMessage(AndroidUtilities.replaceTags(changelog));
        builder.setPositiveButton(LocaleController.getString(R.string.AppUpdateNow), (dialog, which) -> {
            Browser.openUrl(context, downloadUrl);
        });
        if (!force) {
            builder.setNegativeButton(LocaleController.getString(R.string.AppUpdateLater), null);
        } else {
            builder.setCancelable(false);
        }
        builder.show();
    }

    private static void showBulletin(Context context, String text) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                if (context != null) {
                    BulletinFactory.of(Bulletin.BulletinWindow.make(context), null).createSimpleBulletin(R.raw.chats_infotip, text).show();
                } else {
                    BulletinFactory global = BulletinFactory.global();
                    if (global != null) {
                        global.createSimpleBulletin(R.raw.chats_infotip, text).show();
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }
}
