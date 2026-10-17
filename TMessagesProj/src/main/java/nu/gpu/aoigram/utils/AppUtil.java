package nu.gpu.aoigram.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;

import org.telegram.ui.LaunchActivity;

public final class AppUtil {

    public static void restart(Activity activity) {
        activity.startActivity(Intent.makeRestartActivityTask(new ComponentName(activity, LaunchActivity.class)));
        Runtime.getRuntime().exit(0);
    }
}
