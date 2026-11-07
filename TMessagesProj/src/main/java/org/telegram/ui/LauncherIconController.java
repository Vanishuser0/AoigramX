package org.telegram.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

import java.util.Arrays;

public class LauncherIconController {
    private static Boolean officialEnabled;

    public static void setIconAndSplash(Activity activity, LauncherIcon icon) {
        setIcon(icon);
        if (Build.VERSION.SDK_INT >= 33) {
            int theme = switch (icon) {
                case DEFAULT, CLASSIC, INVERTED -> Resources.ID_NULL;
                case SORA -> R.style.Theme_TMessages_Start_Sora;
                case UMI -> R.style.Theme_TMessages_Start_Umi;
                case SAKURA -> R.style.Theme_TMessages_Start_Sakura;
                case YORU -> R.style.Theme_TMessages_Start_Yoru;
                default -> R.style.Theme_TMessages_Start_Official;
            };
            activity.getSplashScreen().setSplashScreenTheme(theme);
        }
    }

    public static int getNotificationIcon() {
        return isOfficialEnabled() ? R.drawable.notification : R.drawable.icon_aoi_notification;
    }

    public static int getNotificationPersonIcon() {
        return isOfficialEnabled() ? R.drawable.ic_launcher_dr : R.drawable.icon_aoi_round;
    }

    public static boolean isOfficialEnabled() {
        if (officialEnabled == null) {
            officialEnabled = Arrays.stream(LauncherIcon.values()).anyMatch(icon -> isOfficial(icon) && isEnabled(icon));
        }
        return officialEnabled;
    }

    private static boolean isOfficial(LauncherIcon icon) {
        return switch (icon) {
            case DEFAULT, CLASSIC, INVERTED, SORA, UMI, SAKURA, YORU -> false;
            default -> true;
        };
    }

    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        officialEnabled = isOfficial(icon);
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.drawable.icon_aoi_background, R.mipmap.icon_aoi_default_foreground, R.string.AppIconDefault),
        CLASSIC("ClassicIcon", R.drawable.icon_aoi_background, R.drawable.icon_aoi_foreground, R.string.AppIconClassic),
        INVERTED("InvertedIcon", R.drawable.icon_aoi_inverted_background, R.drawable.icon_aoi_inverted_foreground, R.string.AppIconInverted),
        SORA("SoraIcon", R.drawable.icon_aoi_sora_background, R.drawable.icon_aoi_inverted_foreground, R.string.AppIconSora),
        UMI("UmiIcon", R.drawable.icon_aoi_umi_background, R.drawable.icon_aoi_inverted_foreground, R.string.AppIconUmi),
        SAKURA("SakuraIcon", R.drawable.icon_aoi_sakura_background, R.drawable.icon_aoi_inverted_foreground, R.string.AppIconSakura),
        YORU("YoruIcon", R.drawable.icon_aoi_yoru_background, R.drawable.icon_aoi_inverted_foreground, R.string.AppIconYoru),
        TELEGRAM("TelegramIcon", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconTelegram),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
