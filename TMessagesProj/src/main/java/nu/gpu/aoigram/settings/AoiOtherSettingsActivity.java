package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.config.BoolConfig;
import nu.gpu.aoigram.utils.AppUtil;

public final class AoiOtherSettingsActivity extends BaseAoiSettingsActivity {

    private static final String EXPORT_ITEM_KEY = "export";
    private static final String RESET_ITEM_KEY = "reset";
    private static final String CRASH_LOGS_ITEM_KEY = "crashLogs";
    private static final String RESTART_ITEM_KEY = "restart";
    private static final String TEST_CRASH_ITEM_KEY = "testCrash";
    private static final int EXPORT_ITEM_ROW = 1;
    private static final int RESET_ITEM_ROW = 2;
    private static final int RESTART_ITEM_ROW = 3;
    private static final int CRASH_LOGS_ITEM_ROW = 4;
    private static final int TEST_CRASH_ITEM_ROW = 5;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiOtherSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.OTHER_PAGE;
    }

    @Override
    protected String getItemKey(UItem item) {
        return switch (item.id) {
            case EXPORT_ITEM_ROW -> EXPORT_ITEM_KEY;
            case RESET_ITEM_ROW -> RESET_ITEM_KEY;
            case CRASH_LOGS_ITEM_ROW -> CRASH_LOGS_ITEM_KEY;
            case RESTART_ITEM_ROW -> RESTART_ITEM_KEY;
            case TEST_CRASH_ITEM_ROW -> TEST_CRASH_ITEM_KEY;
            default -> super.getItemKey(item);
        };
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.AoiDeveloperSettings)));
        items.add(asConfigCheck(AoiConfig.disableAds, R.string.DisableAds));
        items.add(asConfigCheck(AoiConfig.dontSendTyping, R.string.DontSendTyping));
        items.add(asConfigCheck(AoiConfig.hideEmptyStarButton, R.string.HideEmptyStarButton));
        items.add(asConfigCheck(AoiConfig.forceCopy, R.string.ForceCopy));
        items.add(asConfigCheck(AoiConfig.allowScreenshots, R.string.AllowScreenshots));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(EXPORT_ITEM_ROW, R.drawable.msg_saved, getString(R.string.ExportSettings)));
        items.add(UItem.asButton(RESET_ITEM_ROW, R.drawable.msg_reset, getString(R.string.ResetSettings)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(CRASH_LOGS_ITEM_ROW, R.drawable.msg_log, getString(R.string.CrashLogs)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(RESTART_ITEM_ROW, R.drawable.msg_retry, getString(R.string.RestartApp)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(TEST_CRASH_ITEM_ROW, getString(R.string.TestCrash)).red());
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == EXPORT_ITEM_ROW) {
            SettingsBackup.export(this);
        } else if (item.id == RESET_ITEM_ROW) {
            showResetAlert();
        } else if (item.id == CRASH_LOGS_ITEM_ROW) {
            presentFragment(new AoiCrashLogsActivity());
        } else if (item.id == RESTART_ITEM_ROW) {
            AppUtil.restart(getParentActivity());
        } else if (item.id == TEST_CRASH_ITEM_ROW) {
            throw new RuntimeException("Test crash");
        } else {
            super.onItemClick(item, view, position, x, y);
        }
    }

    @Override
    protected void onConfigToggled(BoolConfig config) {
        if (config == AoiConfig.disableAds) {
            for (int account = 0; account < UserConfig.MAX_ACCOUNT_COUNT; account++) {
                if (UserConfig.getInstance(account).isClientActivated()) {
                    MessagesController.getInstance(account).checkPromoInfo(true);
                }
            }
        }
    }

    private void showResetAlert() {
        AlertDialog alertDialog = new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                .setTitle(getString(R.string.ResetSettings))
                .setMessage(getString(R.string.ResetSettingsAlert))
                .setNegativeButton(getString(R.string.Cancel), null)
                .setPositiveButton(getString(R.string.Reset), (dialog, which) -> {
                    AoiConfig.reset();
                    AppUtil.restart(getParentActivity());
                })
                .create();
        showDialog(alertDialog);
        alertDialog.redPositive();
    }
}
