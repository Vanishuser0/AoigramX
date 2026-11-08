package nu.gpu.aoigram.remote;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;

public final class AoiAnnouncementHelper {

    private static final String PREF_NAME = "aoi_announcement";
    private static final String KEY_LAST_SHOWN_ID = "last_shown_id";

    private AoiAnnouncementHelper() {
    }

    /**
     * 检查动态公告（走 Telegram 原生 MTProto Inline Query 通道）
     */
    public static void checkAnnouncement(Context context, boolean manual) {
        AoiBotQueryHelper.queryInline("announcement", (description) -> {
            if (!TextUtils.isEmpty(description)) {
                try {
                    JSONObject json = new JSONObject(description);
                    boolean hasAnnouncement = json.optBoolean("hasAnnouncement", true);
                    int id = json.optInt("id", 0);
                    boolean showOnce = json.optBoolean("showOnce", true);
                    String title = json.optString("title", LocaleController.getString(R.string.DynamicAnnouncement));
                    String message = json.optString("message", "");
                    String linkUrl = json.optString("url", "");
                    String buttonText = json.optString("buttonText", "");
                    if (TextUtils.isEmpty(buttonText)) {
                        buttonText = LocaleController.getString(R.string.DynamicAnnouncementDetail);
                    }

                    SharedPreferences sp = ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    int lastShownId = sp.getInt(KEY_LAST_SHOWN_ID, 0);

                    if (!hasAnnouncement || id <= 0 || TextUtils.isEmpty(message)) {
                        if (manual) {
                            showBulletin(context, LocaleController.getString(R.string.DynamicAnnouncementNoNew));
                        }
                        return;
                    }

                    if (!manual && showOnce && lastShownId >= id) {
                        return;
                    }

                    String finalButtonText = buttonText;
                    if (context instanceof Activity activity && activity.isFinishing()) {
                        return;
                    }
                    Context dialogContext = context != null ? context : ApplicationLoader.applicationContext;

                    AlertDialog.Builder builder = new AlertDialog.Builder(dialogContext);
                    builder.setTitle(title);
                    builder.setMessage(AndroidUtilities.replaceTags(message));

                    if (!TextUtils.isEmpty(linkUrl)) {
                        builder.setPositiveButton(finalButtonText, (dialog, which) -> {
                            Browser.openUrl(dialogContext, linkUrl);
                        });
                        builder.setNegativeButton(LocaleController.getString(R.string.Dismiss), null);
                    } else {
                        builder.setPositiveButton(LocaleController.getString(R.string.Dismiss), null);
                    }

                    builder.show();
                    sp.edit().putInt(KEY_LAST_SHOWN_ID, id).apply();
                    return;
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            if (manual) {
                showBulletin(context, LocaleController.getString(R.string.DynamicAnnouncementNoNew));
            }
        });
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
