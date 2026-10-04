package nu.gpu.aoigram.profile;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.Locale;

import nu.gpu.aoigram.config.AoiConfig;

public final class PeerIdDc {

    public static final int HIDDEN = 0;
    public static final int TELEGRAM_API = 1;
    public static final int BOT_API = 2;

    private static final String[] DC_NAMES = {null, "Pluto", "Venus", "Aurora", "Vesta", "Flora"};
    private static final String[] DC_LOCATIONS = {null, "Miami", "Amsterdam", "Miami", "Amsterdam", "Singapore"};

    public static boolean isVisible() {
        return AoiConfig.idDcType.get() != HIDDEN;
    }

    public static String getLabel(int type) {
        return switch (type) {
            case TELEGRAM_API -> "Telegram API";
            case BOT_API -> "Bot API";
            default -> getString(R.string.Hidden);
        };
    }

    public static String getId(int currentAccount, long userId, long chatId) {
        if (userId != 0) {
            return String.valueOf(userId);
        } else if (AoiConfig.idDcType.get() != BOT_API) {
            return String.valueOf(chatId);
        }
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
        return String.valueOf(ChatObject.isChannel(chat) ? -1000000000000L - chatId : -chatId);
    }

    public static String getLink(long userId, long chatId) {
        return userId != 0 ? "tg://openmessage?user_id=" + userId : "tg://openmessage?chat_id=" + chatId;
    }

    public static String getDetails(int currentAccount, long userId, long chatId) {
        int dc = getDc(currentAccount, userId, chatId);
        return dc == 0 ? "DC " + getString(R.string.NumberUnknown) : formatDc(dc);
    }

    public static String formatDc(int dc) {
        return String.format(Locale.US, "DC%d %s, %s", dc, DC_NAMES[dc], DC_LOCATIONS[dc]);
    }

    private static int getDc(int currentAccount, long userId, long chatId) {
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        if (userId != 0) {
            TLRPC.User user = messagesController.getUser(userId);
            if (UserObject.isUserSelf(user)) {
                return ConnectionsManager.getInstance(currentAccount).getCurrentDatacenterId();
            }
            if (user.photo != null && user.photo.personal) {
                TLRPC.UserFull userFull = messagesController.getUserFull(userId);
                return userFull != null && userFull.profile_photo != null ? userFull.profile_photo.dc_id : 0;
            }
            return user.photo != null ? user.photo.dc_id : 0;
        }
        TLRPC.ChatFull chatFull = messagesController.getChatFull(chatId);
        if (chatFull != null && chatFull.stats_dc != 0) {
            return chatFull.stats_dc;
        }
        TLRPC.Chat chat = messagesController.getChat(chatId);
        return chat.photo != null ? chat.photo.dc_id : 0;
    }
}
