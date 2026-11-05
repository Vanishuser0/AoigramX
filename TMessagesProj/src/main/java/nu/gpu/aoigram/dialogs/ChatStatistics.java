package nu.gpu.aoigram.dialogs;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public final class ChatStatistics {

    private static final int SECRET = 0;
    private static final int PRIVATE = 1;
    private static final int GROUPS = 2;
    private static final int CHANNELS = 3;
    private static final int BOTS = 4;
    private static final int CATEGORIES = 5;

    private static final int UNARCHIVED = 0;
    private static final int ARCHIVED = 1;

    private static final int[] FOLDERS = {0, 1};

    private static final int ROW_HEADER = 0;
    private static final int ROW_NORMAL = 1;
    private static final int ROW_TOTAL = 2;

    public static void show(BaseFragment fragment) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        int account = fragment.getCurrentAccount();
        AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER, fragment.getResourceProvider());
        Loader loader = new Loader(account, () -> {
            progress.dismiss();
            if (fragment.getParentActivity() != null) {
                showResult(fragment, count(MessagesController.getInstance(account)));
            }
        });
        progress.setOnCancelListener(dialog -> loader.cancel());
        progress.showDelayed(300);
        loader.start();
    }

    private static int[][] count(MessagesController controller) {
        int[][] counts = new int[CATEGORIES][2];
        ArrayList<TLRPC.Dialog> dialogs = controller.getAllDialogs();
        for (int i = 0, n = dialogs.size(); i < n; i++) {
            TLRPC.Dialog dialog = dialogs.get(i);
            if (dialog instanceof TLRPC.TL_dialogFolder || dialog instanceof TLRPC.TL_dialogCommunity || controller.isPromoDialog(dialog.id, true)) {
                continue;
            }
            counts[categoryOf(controller, dialog.id)][dialog.folder_id == 1 ? ARCHIVED : UNARCHIVED]++;
        }
        return counts;
    }

    private static int categoryOf(MessagesController controller, long dialogId) {
        if (DialogObject.isEncryptedDialog(dialogId)) {
            return SECRET;
        }
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = controller.getUser(dialogId);
            return user != null && user.bot ? BOTS : PRIVATE;
        }
        TLRPC.Chat chat = controller.getChat(-dialogId);
        return ChatObject.isChannelAndNotMegaGroup(chat) ? CHANNELS : GROUPS;
    }

    private static void showResult(BaseFragment fragment, int[][] counts) {
        Context context = fragment.getParentActivity();
        Theme.ResourcesProvider resourcesProvider = fragment.getResourceProvider();

        TableLayout table = new TableLayout(context);
        table.setColumnStretchable(0, true);
        table.setColumnShrinkable(0, true);
        table.setPadding(dp(24), dp(4), dp(24), dp(8));

        table.addView(row(context, resourcesProvider, ROW_HEADER, "",
                getString(R.string.ChatStatisticsUnarchived),
                getString(R.string.ChatStatisticsArchived),
                getString(R.string.ChatStatisticsTotal)));

        String[] labels = {
                getString(R.string.SecretChat),
                getString(R.string.FolderLinkPreviewRight),
                getString(R.string.FilterGroups),
                getString(R.string.FilterChannels),
                getString(R.string.FilterBots)
        };
        int[] totals = new int[2];
        for (int category = 0; category < CATEGORIES; category++) {
            int unarchived = counts[category][UNARCHIVED];
            int archived = counts[category][ARCHIVED];
            totals[UNARCHIVED] += unarchived;
            totals[ARCHIVED] += archived;
            if (category == SECRET && unarchived + archived == 0) {
                continue;
            }
            table.addView(row(context, resourcesProvider, ROW_NORMAL, labels[category], format(unarchived), format(archived), format(unarchived + archived)));
        }

        View divider = new View(context);
        divider.setBackgroundColor(Theme.getColor(Theme.key_divider, resourcesProvider));
        TableLayout.LayoutParams dividerParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(0.5f)));
        dividerParams.topMargin = dp(6);
        dividerParams.bottomMargin = dp(6);
        table.addView(divider, dividerParams);

        table.addView(row(context, resourcesProvider, ROW_TOTAL, getString(R.string.ChatStatisticsTotal),
                format(totals[UNARCHIVED]), format(totals[ARCHIVED]), format(totals[UNARCHIVED] + totals[ARCHIVED])));

        fragment.showDialog(new AlertDialog.Builder(context, resourcesProvider)
                .setTitle(getString(R.string.ChatStatistics))
                .setView(table)
                .setPositiveButton(getString(R.string.OK), null)
                .create());
    }

    private static TableRow row(Context context, Theme.ResourcesProvider resourcesProvider, int type, String label, String... values) {
        TableRow row = new TableRow(context);
        row.setPadding(0, dp(type == ROW_HEADER ? 4 : 6), 0, dp(type == ROW_HEADER ? 4 : 6));
        row.addView(cell(context, resourcesProvider, type, label, false));
        for (String value : values) {
            TextView cell = cell(context, resourcesProvider, type, value, true);
            TableRow.LayoutParams params = new TableRow.LayoutParams();
            params.setMarginStart(dp(12));
            row.addView(cell, params);
        }
        return row;
    }

    private static TextView cell(Context context, Theme.ResourcesProvider resourcesProvider, int type, String text, boolean number) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setMaxLines(2);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, type == ROW_HEADER ? 13 : 15);
        textView.setTextColor(Theme.getColor(type == ROW_HEADER ? Theme.key_dialogTextGray3 : Theme.key_dialogTextBlack, resourcesProvider));
        if (type == ROW_TOTAL || type == ROW_HEADER && number) {
            textView.setTypeface(AndroidUtilities.bold());
        }
        if (number) {
            textView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            textView.setFontFeatureSettings("tnum");
            if (type == ROW_HEADER) {
                textView.setMaxWidth(dp(72));
            }
        } else {
            textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        }
        return textView;
    }

    private static String format(int value) {
        return LocaleController.formatNumber(value, ',');
    }

    private static final class Loader implements NotificationCenter.NotificationCenterDelegate {

        private static final long RETRY_DELAY = 1000;

        private final int account;
        private final Runnable onLoaded;
        private final Runnable step = this::step;
        private final NotificationCenter.ObserversGroup observers;
        private boolean finished;

        Loader(int account, Runnable onLoaded) {
            this.account = account;
            this.onLoaded = onLoaded;
            observers = NotificationCenter.getInstance(account).createObserversGroup(this);
        }

        void start() {
            observers.add(NotificationCenter.dialogsNeedReload);
            step();
        }

        void cancel() {
            if (finished) {
                return;
            }
            finished = true;
            AndroidUtilities.cancelRunOnUIThread(step);
            observers.removeAllObservers();
        }

        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.dialogsNeedReload && !finished) {
                AndroidUtilities.cancelRunOnUIThread(step);
                AndroidUtilities.runOnUIThread(step);
            }
        }

        private void step() {
            if (finished) {
                return;
            }
            MessagesController controller = MessagesController.getInstance(account);
            for (int folderId : FOLDERS) {
                boolean loaded = controller.isDialogsEndReached(folderId) && controller.isServerDialogsEndReached(folderId);
                if (!loaded || !UserConfig.getInstance(account).isPinnedDialogsLoaded(folderId)) {
                    controller.loadPinnedDialogs(folderId, 0, null);
                    if (!loaded && !controller.isLoadingDialogs(folderId)) {
                        controller.loadDialogs(folderId, -1, 100, !controller.isDialogsEndReached(folderId));
                    }
                    AndroidUtilities.cancelRunOnUIThread(step);
                    AndroidUtilities.runOnUIThread(step, RETRY_DELAY);
                    return;
                }
            }
            cancel();
            onLoaded.run();
        }
    }
}
