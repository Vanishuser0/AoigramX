package nu.gpu.aoigram.dialogs;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.util.SparseArray;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLiteException;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import nu.gpu.aoigram.config.AoiConfig;

public final class FolderIcons {

    public static final int TITLE = 0;
    public static final int ICON = 1;
    public static final int ICON_AND_TITLE = 2;
    public static final int ICON_SIZE = 24;

    private static final Map<String, Integer> ICONS = new LinkedHashMap<>();

    static {
        ICONS.put("🐱", R.drawable.filter_cat);
        ICONS.put("📕", R.drawable.filter_book);
        ICONS.put("💰", R.drawable.filter_money);
        ICONS.put("🎮", R.drawable.filter_game);
        ICONS.put("💡", R.drawable.filter_light);
        ICONS.put("👌", R.drawable.filter_like);
        ICONS.put("🎵", R.drawable.filter_note);
        ICONS.put("🎨", R.drawable.filter_palette);
        ICONS.put("✈", R.drawable.filter_travel);
        ICONS.put("⚽", R.drawable.filter_sport);
        ICONS.put("⭐", R.drawable.filter_favorite);
        ICONS.put("🎓", R.drawable.filter_study);
        ICONS.put("🛫", R.drawable.filter_airplane);
        ICONS.put("👤", R.drawable.filter_private);
        ICONS.put("👥", R.drawable.filter_group);
        ICONS.put("💬", R.drawable.filter_all);
        ICONS.put("✅", R.drawable.filter_unread);
        ICONS.put("🤖", R.drawable.filter_bots);
        ICONS.put("👑", R.drawable.filter_crown);
        ICONS.put("🌹", R.drawable.filter_flower);
        ICONS.put("🏠", R.drawable.filter_home);
        ICONS.put("❤", R.drawable.filter_love);
        ICONS.put("🎭", R.drawable.filter_mask);
        ICONS.put("🍸", R.drawable.filter_party);
        ICONS.put("📈", R.drawable.filter_trade);
        ICONS.put("💼", R.drawable.filter_work);
        ICONS.put("🔔", R.drawable.filter_unmuted);
        ICONS.put("📢", R.drawable.filter_channels);
        ICONS.put("📁", R.drawable.filter_custom);
        ICONS.put("📋", R.drawable.filter_setup);
    }

    public static String getLabel(int style) {
        return switch (style) {
            case ICON -> getString(R.string.FolderTabStyleIcon);
            case ICON_AND_TITLE -> getString(R.string.FolderTabStyleIconAndTitle);
            default -> getString(R.string.FolderTabStyleTitle);
        };
    }

    public static boolean showTitle() {
        return AoiConfig.folderTabStyle.get() != ICON;
    }

    public static int getIconWidth(int icon) {
        if (icon == 0) {
            return 0;
        }
        return dp(showTitle() ? ICON_SIZE + 6 : ICON_SIZE);
    }

    public static Set<String> getEmoticons() {
        return ICONS.keySet();
    }

    public static int getIcon(String emoticon) {
        Integer icon = ICONS.get(emoticon);
        return icon != null ? icon : R.drawable.filter_custom;
    }

    public static int getTabIcon(String emoticon) {
        return AoiConfig.folderTabStyle.get() == TITLE ? 0 : getIcon(emoticon);
    }

    public static int getTabIcon(MessagesController.DialogFilter filter) {
        return getTabIcon(getEmoticon(filter));
    }

    public static int getMenuIcon(MessagesController.DialogFilter filter) {
        return switch (getEmoticon(filter)) {
            case "💬" -> R.drawable.msg_discussion;
            case "👤" -> R.drawable.msg_openprofile;
            case "👥" -> R.drawable.msg_groups;
            case "📢" -> R.drawable.msg_channel;
            case "🤖" -> R.drawable.msg_bots;
            case "✅" -> R.drawable.msg_markunread;
            case "🔔" -> R.drawable.msg_notifications;
            case "⭐" -> R.drawable.msg_fave;
            case "❤" -> R.drawable.msg_input_like;
            case "🏠" -> R.drawable.msg_home;
            case "💼" -> R.drawable.msg_work;
            case "🎨" -> R.drawable.msg_palette;
            case "🎭" -> R.drawable.msg_mask;
            case "📈" -> R.drawable.msg_stats;
            case "🐱" -> R.drawable.msg_emoji_cat;
            case "✈" -> R.drawable.msg_emoji_travel;
            case "⚽" -> R.drawable.msg_emoji_activities;
            case "💡" -> R.drawable.msg_emoji_objects;
            default -> R.drawable.msg_folders;
        };
    }

    private static String getEmoticon(MessagesController.DialogFilter filter) {
        return filter.isDefault() ? "💬" : getEmoticon(filter.emoticon, filter.flags, filter.alwaysShow);
    }

    public static String getEmoticon(String emoticon, int flags, ArrayList<Long> alwaysShow) {
        if (emoticon != null) {
            return emoticon;
        }
        if (!alwaysShow.isEmpty()) {
            return "📁";
        }
        int types = flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS;
        int contacts = MessagesController.DIALOG_FILTER_FLAG_CONTACTS | MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
        if (types != 0 && (types & ~contacts) == 0) {
            return "👤";
        } else if (types == MessagesController.DIALOG_FILTER_FLAG_GROUPS) {
            return "👥";
        } else if (types == MessagesController.DIALOG_FILTER_FLAG_CHANNELS) {
            return "📢";
        } else if (types == MessagesController.DIALOG_FILTER_FLAG_BOTS) {
            return "🤖";
        }
        int excluded = flags & (MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ | MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED);
        if (excluded == MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) {
            return "✅";
        } else if (excluded == MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) {
            return "🔔";
        }
        return "📁";
    }

    public static void loadEmoticons(SQLiteDatabase database, SparseArray<MessagesController.DialogFilter> filters) throws SQLiteException {
        database.executeFast("CREATE TABLE IF NOT EXISTS dialog_filter_emoticon(id INTEGER PRIMARY KEY, emoticon TEXT)").stepThis().dispose();
        SQLiteCursor cursor = database.queryFinalized("SELECT id, emoticon FROM dialog_filter_emoticon");
        while (cursor.next()) {
            MessagesController.DialogFilter filter = filters.get(cursor.intValue(0));
            if (filter != null) {
                filter.emoticon = cursor.stringValue(1);
            }
        }
        cursor.dispose();
    }

    public static void saveEmoticon(SQLiteDatabase database, MessagesController.DialogFilter filter) throws SQLiteException {
        if (filter.emoticon == null) {
            deleteEmoticon(database, filter.id);
            return;
        }
        SQLitePreparedStatement state = database.executeFast("REPLACE INTO dialog_filter_emoticon VALUES(?, ?)");
        state.bindInteger(1, filter.id);
        state.bindString(2, filter.emoticon);
        state.step();
        state.dispose();
    }

    public static void deleteEmoticon(SQLiteDatabase database, int filterId) throws SQLiteException {
        database.executeFast("DELETE FROM dialog_filter_emoticon WHERE id = " + filterId).stepThis().dispose();
    }
}
