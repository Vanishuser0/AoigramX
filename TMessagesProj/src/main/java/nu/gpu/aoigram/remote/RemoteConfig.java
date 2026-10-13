package nu.gpu.aoigram.remote;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class RemoteConfig {

    private static final String CHANNEL_USERNAME = "AoigramRemoteConfig";
    private static final long CHANNEL_ID = 4429546637L;
    private static final int HISTORY_LIMIT = 100;

    private final Map<String, JSONObject> sections = new HashMap<>();
    private final Map<Integer, TLRPC.Message> messages = new HashMap<>();

    private RemoteConfig(ArrayList<TLRPC.Message> history) {
        for (TLRPC.Message message : history) {
            messages.put(message.id, message);
            String text = message.message;
            int newline = text == null ? -1 : text.indexOf('\n');
            if (newline < 0 || !text.startsWith("#")) {
                continue;
            }
            String tag = text.substring(1, newline).trim();
            if (sections.containsKey(tag)) {
                continue;
            }
            try {
                sections.put(tag, new JSONObject(text.substring(newline + 1)));
            } catch (JSONException e) {
                FileLog.e(e);
            }
        }
    }

    public JSONObject section(String tag) {
        return sections.get(tag);
    }

    public TLRPC.Message message(int id) {
        return messages.get(id);
    }

    public TLRPC.Document document(int id) {
        TLRPC.Message message = messages.get(id);
        return message == null ? null : MessageObject.getDocument(message);
    }

    public static void fetch(int currentAccount, Utilities.Callback2<RemoteConfig, TLRPC.TL_error> callback) {
        AndroidUtilities.runOnUIThread(() -> MessagesController.getInstance(currentAccount).getUserNameResolver().resolve(CHANNEL_USERNAME, peerId -> {
            if (peerId == null || peerId != -CHANNEL_ID) {
                TLRPC.TL_error error = new TLRPC.TL_error();
                error.text = "CHANNEL_INVALID";
                Utilities.stageQueue.postRunnable(() -> callback.run(null, error));
                return;
            }
            TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(peerId);
            req.limit = HISTORY_LIMIT;
            ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> {
                if (response instanceof TLRPC.messages_Messages res) {
                    callback.run(new RemoteConfig(res.messages), null);
                } else {
                    callback.run(null, error);
                }
            });
        }));
    }
}
