package nu.gpu.aoigram.remote;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ConnectionsManager;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

/**
 * AoigramX Telegram 原生 MTProto Bot 内联查询助手
 *
 * 核心特性：
 * 1. 0 外部 HTTP 请求：所有数据查询全部通过 Telegram 原生底层加密通道 (MTProto) 与官方服务器通信
 * 2. 抓包 100% 纯净：不存在任何第三方域名连接，完全杜绝隐私泄漏隐患
 * 3. 官方安全中转：由 Telegram 官方服务器作为中转调用助手 Bot (@AoigramXBot)
 */
public final class AoiBotQueryHelper {

    public static final String BOT_USERNAME = "AoigramXBot";

    private AoiBotQueryHelper() {
    }

    /**
     * 向 @AoigramXBot 发起内联查询
     *
     * @param query    查询指令，如 "update" 或 "announcement"
     * @param callback 成功时返回 description 中携带的 JSON 字符串，失败时返回 null
     */
    public static void queryInline(String query, Utilities.Callback<String> callback) {
        int currentAccount = UserConfig.selectedAccount;
        MessagesController mc = MessagesController.getInstance(currentAccount);
        ConnectionsManager cm = ConnectionsManager.getInstance(currentAccount);

        TLRPC.User botUser = mc.getUser(BOT_USERNAME);
        if (botUser != null) {
            sendInlineRequest(cm, mc, botUser, query, callback);
        } else {
            // 本地无缓存时，通过 Telegram 官方联系人解析 RPC 获取 Bot 用户实体
            TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
            req.username = BOT_USERNAME;
            cm.sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (res instanceof TLRPC.TL_contacts_resolvedPeer) {
                    TLRPC.TL_contacts_resolvedPeer peer = (TLRPC.TL_contacts_resolvedPeer) res;
                    mc.putUsers(peer.users, false);
                    mc.putChats(peer.chats, false);
                    long uid = DialogObject.getPeerDialogId(peer.peer);
                    TLRPC.User resolved = mc.getUser(uid);
                    if (resolved != null) {
                        sendInlineRequest(cm, mc, resolved, query, callback);
                        return;
                    }
                }
                if (callback != null) {
                    callback.run(null);
                }
            }));
        }
    }

    private static void sendInlineRequest(ConnectionsManager cm, MessagesController mc, TLRPC.User botUser, String query, Utilities.Callback<String> callback) {
        try {
            TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
            req.bot = mc.getInputUser(botUser);
            req.query = query != null ? query : "";
            req.offset = "";
            req.peer = new TLRPC.TL_inputPeerEmpty();

            cm.sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                if (res instanceof TLRPC.messages_BotResults) {
                    TLRPC.messages_BotResults results = (TLRPC.messages_BotResults) res;
                    if (results.results != null && !results.results.isEmpty()) {
                        TLRPC.BotInlineResult first = results.results.get(0);
                        if (first instanceof TLRPC.TL_botInlineResult) {
                            TLRPC.TL_botInlineResult article = (TLRPC.TL_botInlineResult) first;
                            if (!TextUtils.isEmpty(article.description)) {
                                if (callback != null) {
                                    callback.run(article.description);
                                    return;
                                }
                            }
                        }
                    }
                }
                if (callback != null) {
                    callback.run(null);
                }
            }));
        } catch (Exception e) {
            FileLog.e(e);
            if (callback != null) {
                callback.run(null);
            }
        }
    }
}
