package nu.gpu.aoigram.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

public final class AoiConfig {

    private static final String PREFERENCES_NAME = "aoi_config";

    static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);

    private AoiConfig() {
    }

    public static SharedPreferences accountData(int currentAccount) {
        long clientUserId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (clientUserId == 0) {
            throw new IllegalStateException("Account is not logged in: " + currentAccount);
        }
        String accountKey = (ConnectionsManager.getInstance(currentAccount).isTestBackend() ? "test_" : "prod_") + clientUserId;
        return ApplicationLoader.applicationContext.getSharedPreferences("aoi_account_data_" + accountKey, Context.MODE_PRIVATE);
    }

    private static <C extends ConfigItem<?>> C register(C item) {
        item.load();
        return item;
    }
}
