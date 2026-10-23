package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.utils.AppUtil;

public final class AoiOtherSettingsActivity extends BaseAoiSettingsActivity {

    private static final String EXPORT_ITEM_KEY = "export";
    private static final String RESET_ITEM_KEY = "reset";
    private static final String RESTART_ITEM_KEY = "restart";
    private static final int EXPORT_ITEM_ROW = 1;
    private static final int RESET_ITEM_ROW = 2;
    private static final int RESTART_ITEM_ROW = 3;

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
            case RESTART_ITEM_ROW -> RESTART_ITEM_KEY;
            default -> null;
        };
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asButton(EXPORT_ITEM_ROW, R.drawable.msg_saved, getString(R.string.ExportSettings)));
        items.add(UItem.asButton(RESET_ITEM_ROW, R.drawable.msg_reset, getString(R.string.ResetSettings)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(RESTART_ITEM_ROW, R.drawable.msg_retry, getString(R.string.RestartApp)));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == EXPORT_ITEM_ROW) {
            SettingsBackup.export(this);
        } else if (item.id == RESET_ITEM_ROW) {
            showResetAlert();
        } else if (item.id == RESTART_ITEM_ROW) {
            AppUtil.restart(getParentActivity());
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
