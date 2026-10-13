package nu.gpu.aoigram.remote;

import static org.telegram.messenger.LocaleController.getString;

import android.os.Build;

import org.json.JSONObject;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLRPC;

import nu.gpu.aoigram.config.AoiConfig;

public final class AppUpdates {

    public static final int STABLE = 0;
    public static final int BETA = 1;

    public static String getLabel(int channel) {
        return getString(channel == BETA ? R.string.UpdateChannelBeta : R.string.UpdateChannelStable);
    }

    public static void request(int currentAccount, RequestDelegate delegate) {
        RemoteConfig.fetch(currentAccount, (config, error) -> {
            if (error != null) {
                delegate.run(null, error);
                return;
            }
            JSONObject update = config.section(AoiConfig.updateChannel.get() == BETA ? "update_beta" : "update");
            TLRPC.Document document = update == null || !isNewer(update) ? null : findApk(config, update);
            delegate.run(document == null ? new TLRPC.TL_help_noAppUpdate() : create(config, update, document), null);
        });
    }

    private static boolean isNewer(JSONObject update) {
        return update.optInt("code") >= SharedConfig.buildVersion() && update.optInt("timestamp") > BuildConfig.BUILD_TIMESTAMP;
    }

    private static TLRPC.Document findApk(RemoteConfig config, JSONObject update) {
        for (String abi : Build.SUPPORTED_ABIS) {
            TLRPC.Document document = config.document(update.optInt(abi));
            if (document != null) {
                return document;
            }
        }
        return config.document(update.optInt("universal"));
    }

    private static TLRPC.TL_help_appUpdate create(RemoteConfig config, JSONObject update, TLRPC.Document document) {
        TLRPC.TL_help_appUpdate appUpdate = new TLRPC.TL_help_appUpdate();
        appUpdate.id = update.optInt("timestamp");
        appUpdate.version = update.optString("version") + " (" + update.optInt("code") + ")";
        appUpdate.document = document;
        appUpdate.flags |= 2;
        appUpdate.text = "";
        TLRPC.Message changelog = config.message(update.optInt("changelog"));
        if (changelog != null) {
            appUpdate.text = changelog.message;
            appUpdate.entities = changelog.entities;
        }
        appUpdate.sticker = config.document(update.optInt("sticker"));
        if (appUpdate.sticker != null) {
            appUpdate.flags |= 8;
        }
        return appUpdate;
    }
}
