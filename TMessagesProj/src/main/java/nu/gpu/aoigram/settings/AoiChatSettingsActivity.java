package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.config.BoolConfig;
import nu.gpu.aoigram.config.IntConfig;
import nu.gpu.aoigram.messages.DoubleTap;
import nu.gpu.aoigram.messages.DoubleTapPreview;
import nu.gpu.aoigram.reactions.PinnedReactions;

public final class AoiChatSettingsActivity extends BaseAoiSettingsActivity {

    private static final int PINNED_REACTIONS = 1;
    private static final int MESSAGE_MENU = 2;
    private static final int CONFIRMATIONS = 3;

    private static final Option[] MESSAGE_MENU_OPTIONS = {
            new Option(AoiConfig.showCopyPhoto, R.string.CopyPhoto),
            new Option(AoiConfig.showSave, R.string.Save),
            new Option(AoiConfig.showRepeat, R.string.Repeat),
            new Option(AoiConfig.showReport, R.string.ReportChat),
            new Option(AoiConfig.showDetails, R.string.MessageDetails),
    };
    private static final Option[] CONFIRMATION_OPTIONS = {
            new Option(AoiConfig.repeatConfirm, R.string.RepeatConfirm),
    };

    private DoubleTapPreview doubleTapPreview;
    private final SparseBooleanArray expandedGroups = new SparseBooleanArray();

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiChatSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.CHAT_PAGE;
    }

    @Override
    protected void onCreateSettingsView(Context context) {
        doubleTapPreview = new DoubleTapPreview(context, this);
        doubleTapPreview.setActions(AoiConfig.doubleTapIncomingAction.get(), AoiConfig.doubleTapOutgoingAction.get(), false);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(asConfigCheck(AoiConfig.hideKeyboardOnScroll, R.string.HideKeyboardOnScroll));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(PINNED_REACTIONS, R.drawable.msg_pin, getString(R.string.PinnedReactions), PinnedReactions.getLabel()));
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.DoubleTapActions)));
        items.add(asDoubleTapAction(R.string.IncomingMessages, AoiConfig.doubleTapIncomingAction));
        UItem outgoingItem = asDoubleTapAction(R.string.OutgoingMessages, AoiConfig.doubleTapOutgoingAction);
        outgoingItem.hideDivider = true;
        items.add(outgoingItem);
        items.add(UItem.asCustom(doubleTapPreview));
        items.add(UItem.asShadow(null));
        items.add(asConfigCheck(AoiConfig.groupMessageMenu, R.string.GroupMessageMenu, R.string.GroupMessageMenuInfo));
        addGroup(items, adapter, MESSAGE_MENU, R.drawable.msg_list, R.string.MessageMenu, MESSAGE_MENU_OPTIONS);
        addGroup(items, adapter, CONFIRMATIONS, R.drawable.msg2_ask_question, R.string.Confirmations, CONFIRMATION_OPTIONS);
        items.add(UItem.asShadow(null));
    }

    private UItem asDoubleTapAction(int titleResId, IntConfig config) {
        UItem item = UItem.asButton(0, getString(titleResId), DoubleTap.getLabel(config.get()));
        item.object = config;
        return item;
    }

    private void addGroup(ArrayList<UItem> items, UniversalAdapter adapter, int id, int iconResId, int titleResId, Option[] options) {
        int enabled = 0;
        for (Option option : options) {
            if (option.config.get()) {
                enabled++;
            }
        }
        boolean expanded = expandedGroups.get(id);
        items.add(ExpandableSettingsCell.Factory.group(id, iconResId, getString(titleResId), enabled, options.length, expanded, view -> {
            expandedGroups.put(id, !expandedGroups.get(id));
            listView.adapter.update(true);
        }));
        if (adapter == null || expanded) {
            for (Option option : options) {
                UItem item = ExpandableSettingsCell.Factory.option(getString(option.titleResId), option.config.get());
                item.object = option.config;
                items.add(item);
            }
        }
    }

    private static Option[] getOptions(int groupId) {
        return groupId == MESSAGE_MENU ? MESSAGE_MENU_OPTIONS : CONFIRMATION_OPTIONS;
    }

    @Override
    public BaseAoiSettingsActivity setInitialItemKey(String itemKey) {
        for (int groupId : new int[] {MESSAGE_MENU, CONFIRMATIONS}) {
            for (Option option : getOptions(groupId)) {
                if (option.config.getKey().equals(itemKey)) {
                    expandedGroups.put(groupId, true);
                }
            }
        }
        return super.setInitialItemKey(itemKey);
    }

    @Override
    protected String getItemKey(UItem item) {
        return item.id == PINNED_REACTIONS ? AoiSettingsDeepLink.PINNED_REACTIONS_PAGE : super.getItemKey(item);
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == PINNED_REACTIONS) {
            presentFragment(new AoiPinnedReactionsActivity());
        } else if (item.object == AoiConfig.doubleTapIncomingAction || item.object == AoiConfig.doubleTapOutgoingAction) {
            IntConfig config = (IntConfig) item.object;
            String[] labels = new String[DoubleTap.DELETE + 1];
            for (int action = DoubleTap.NONE; action <= DoubleTap.DELETE; action++) {
                labels[action] = DoubleTap.getLabel(action);
            }
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), labels, item.text.toString(), config.get(), (dialog, which) -> {
                config.set(which);
                doubleTapPreview.setActions(AoiConfig.doubleTapIncomingAction.get(), AoiConfig.doubleTapOutgoingAction.get(), true);
                listView.adapter.update(true);
            }));
        } else if (item.id == MESSAGE_MENU || item.id == CONFIRMATIONS) {
            if (LocaleController.isRTL ? x > dp(75) : x < view.getWidth() - dp(75)) {
                item.clickCallback.onClick(view);
            } else {
                for (Option option : getOptions(item.id)) {
                    option.config.set(!item.checked);
                }
                listView.adapter.update(true);
            }
        } else {
            super.onItemClick(item, view, position, x, y);
        }
    }

    private record Option(BoolConfig config, int titleResId) {}
}
