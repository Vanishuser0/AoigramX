package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Components.CircularProgressDrawable;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.remote.AppUpdates;

public final class AoiAboutSettingsActivity extends BaseAoiSettingsActivity {

    private static final String CHECK_UPDATE_ITEM_KEY = "checkUpdate";
    private static final int CHECK_UPDATE_ROW = 1;

    private boolean checkingUpdate;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiAbout);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.ABOUT_PAGE;
    }

    @Override
    protected String getItemKey(UItem item) {
        return item.id == CHECK_UPDATE_ROW ? CHECK_UPDATE_ITEM_KEY : super.getItemKey(item);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        UItem channelItem = UItem.asButton(0, R.drawable.msg_download_settings, getString(R.string.UpdateChannel), AppUpdates.getLabel(AoiConfig.updateChannel.get()));
        channelItem.object = AoiConfig.updateChannel;
        items.add(channelItem);
        UItem checkUpdateItem = UItem.asButton(CHECK_UPDATE_ROW, R.drawable.msg_photo_switch2, getString(R.string.CheckForUpdates)).accent();
        if (checkingUpdate) {
            checkUpdateItem.object = new CircularProgressDrawable(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, getResourceProvider()));
        }
        items.add(checkUpdateItem);
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == CHECK_UPDATE_ROW) {
            checkUpdate();
        } else if (item.object == AoiConfig.updateChannel) {
            showUpdateChannelDialog();
        }
    }

    private void showUpdateChannelDialog() {
        Activity activity = getParentActivity();
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        for (int channel : new int[]{AppUpdates.STABLE, AppUpdates.BETA}) {
            RadioColorCell cell = new RadioColorCell(activity);
            cell.setPadding(dp(4), 0, dp(4), 0);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(AppUpdates.getLabel(channel), AoiConfig.updateChannel.get() == channel);
            cell.setOnClickListener(v -> {
                builder.getDismissRunnable().run();
                AoiConfig.updateChannel.set(channel);
                listView.adapter.update(true);
                checkUpdate();
            });
            linearLayout.addView(cell);
        }
        CheckBoxCell autoCheckCell = new CheckBoxCell(activity, 1);
        autoCheckCell.setBackground(Theme.getSelectorDrawable(false));
        autoCheckCell.setText(getString(R.string.AutoCheckForUpdates), "", AoiConfig.autoCheckUpdates.get(), false);
        autoCheckCell.setPadding(LocaleController.isRTL ? dp(16) : dp(9), 0, LocaleController.isRTL ? dp(9) : dp(16), 0);
        autoCheckCell.getCheckBoxView().setTranslationX(LocaleController.isRTL ? dp(2) : -dp(2));
        autoCheckCell.setOnClickListener(v -> {
            AoiConfig.autoCheckUpdates.toggle();
            autoCheckCell.setChecked(AoiConfig.autoCheckUpdates.get(), true);
        });
        linearLayout.addView(autoCheckCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        builder.setTitle(getString(R.string.UpdateChannel));
        builder.setView(linearLayout);
        builder.setPositiveButton(getString(R.string.Done), null);
        showDialog(builder.create());
    }

    private void checkUpdate() {
        if (checkingUpdate) {
            return;
        }
        checkingUpdate = true;
        listView.adapter.update(true);
        ((LaunchActivity) getParentActivity()).checkAppUpdate(true, new Browser.Progress(null, () -> {
            checkingUpdate = false;
            listView.adapter.update(true);
        }));
    }
}
