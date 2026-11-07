package nu.gpu.aoigram.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageInfo;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.ui.LaunchActivity;

import java.util.Locale;

public final class AppUtil {

    public static void restart(Activity activity) {
        activity.startActivity(Intent.makeRestartActivityTask(new ComponentName(activity, LaunchActivity.class)));
        Runtime.getRuntime().exit(0);
    }

    public static String getVersionName() {
        try {
            PackageInfo packageInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            return String.format(Locale.US, "%s (%d)", packageInfo.versionName, packageInfo.getLongVersionCode());
        } catch (Exception ignore) {
            return BuildVars.BUILD_VERSION_STRING;
        }
    }
}
