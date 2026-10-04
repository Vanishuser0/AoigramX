package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.profile.PeerIdDc;

public final class AoiGeneralSettingsActivity extends BaseAoiSettingsActivity {

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiGeneralSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.GENERAL_PAGE;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(asConfigCheck(AoiConfig.showFullNumbers, R.string.ShowFullNumbers, R.string.ShowFullNumbersInfo));
        items.add(asConfigCheck(AoiConfig.formatTimeWithSeconds, R.string.FormatTimeWithSeconds));
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.Profile)));
        UItem idDcItem = UItem.asButton(0, getString(R.string.ShowIdAndDc), PeerIdDc.getLabel(AoiConfig.idDcType.get()));
        idDcItem.object = AoiConfig.idDcType;
        items.add(idDcItem);
        items.add(asConfigCheck(AoiConfig.hidePhone, R.string.HidePhone));
        items.add(asConfigCheck(AoiConfig.showFullAbout, R.string.ShowFullAbout));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        super.onItemClick(item, view, position, x, y);
        if (item.object == AoiConfig.idDcType) {
            String[] labels = {PeerIdDc.getLabel(PeerIdDc.HIDDEN), PeerIdDc.getLabel(PeerIdDc.TELEGRAM_API), PeerIdDc.getLabel(PeerIdDc.BOT_API)};
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), labels, getString(R.string.ShowIdAndDc), AoiConfig.idDcType.get(), (dialog, which) -> {
                AoiConfig.idDcType.set(which);
                listView.adapter.update(true);
                parentLayout.rebuildFragments(0);
            }));
        } else if (item.object == AoiConfig.formatTimeWithSeconds) {
            parentLayout.rebuildFragments(0);
        } else if (item.object == AoiConfig.hidePhone) {
            getNotificationCenter().postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_PHONE);
        }
    }
}
