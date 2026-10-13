package nu.gpu.aoigram.dialogs;

import static org.telegram.messenger.LocaleController.getString;

import androidx.collection.LongSparseArray;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.support.LongSparseIntArray;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;

public final class FolderUnreadCounter {

    public static final int ALL = 0;
    public static final int UNMUTED = 1;
    public static final int HIDDEN = 2;

    public static String getLabel(int type) {
        return switch (type) {
            case UNMUTED -> getString(R.string.FolderUnreadCounterUnmuted);
            case HIDDEN -> getString(R.string.Hidden);
            default -> getString(R.string.FolderUnreadCounterAll);
        };
    }

    public static boolean isHidden() {
        return AoiConfig.folderUnreadCounter.get() == HIDDEN;
    }

    public static boolean isUnmutedOnly() {
        return AoiConfig.folderUnreadCounter.get() == UNMUTED;
    }

    public static int getFlags(int flags) {
        return isUnmutedOnly() ? flags | MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED : flags;
    }

    public static boolean resetMentionChanged(ArrayList<MessagesController.DialogFilter> filters, LongSparseIntArray mentions, LongSparseArray<Boolean> mutedDialogs) {
        if (!isUnmutedOnly() || mentions == null) {
            return false;
        }
        boolean reset = false;
        for (int a = 0, N = filters.size(); a < N; a++) {
            MessagesController.DialogFilter filter = filters.get(a);
            for (int b = 0, N2 = mentions.size(); b < N2; b++) {
                long did = mentions.keyAt(b);
                if (mutedDialogs.indexOfKey(did) >= 0 && filter.alwaysShow.contains(did)) {
                    filter.pendingUnreadCount = -1;
                    reset = true;
                    break;
                }
            }
        }
        return reset;
    }

    public static void reload() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated()) {
                MessagesStorage storage = MessagesStorage.getInstance(a);
                storage.getStorageQueue().postRunnable(() -> storage.resetAllUnreadCounters(false));
            }
        }
    }
}
