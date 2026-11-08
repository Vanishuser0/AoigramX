package nu.gpu.aoigram.cloud;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_bots;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AoiCloudStorageHelper extends AccountInstance {

    public static final String BOT_USERNAME = "AoigramXBot";
    public static final long BOT_ID = 9003919885L;

    private static final AoiCloudStorageHelper[] Instance = new AoiCloudStorageHelper[UserConfig.MAX_ACCOUNT_COUNT];

    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Gson GSON = new Gson();

    public AoiCloudStorageHelper(int num) {
        super(num);
    }

    public static AoiCloudStorageHelper getInstance(int num) {
        AoiCloudStorageHelper localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (AoiCloudStorageHelper.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new AoiCloudStorageHelper(num);
                }
            }
        }
        return localInstance;
    }

    private void resolveBot(Consumer<TLRPC.User> callback) {
        TLRPC.User cached = getMessagesController().getUser(BOT_ID);
        if (cached != null) {
            callback.accept(cached);
            return;
        }
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = BOT_USERNAME;
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (response instanceof TLRPC.TL_contacts_resolvedPeer peer) {
                getMessagesController().putUsers(peer.users, false);
                getMessagesController().putChats(peer.chats, false);
                getMessagesStorage().putUsersAndChats(peer.users, peer.chats, true, true);
                TLRPC.User user = getMessagesController().getUser(BOT_ID);
                if (user == null && peer.users != null && !peer.users.isEmpty()) {
                    user = peer.users.get(0);
                }
                callback.accept(user);
            } else {
                callback.accept(null);
            }
        }));
    }

    private void invokeWebViewCustomMethod(String method, String data, @NonNull BiConsumer<String, String> callback) {
        invokeWebViewCustomMethod(method, data, true, callback);
    }

    private void invokeWebViewCustomMethod(String method, String data, boolean searchUser, @NonNull BiConsumer<String, String> callback) {
        TLRPC.User user = getMessagesController().getUser(BOT_ID);
        if (user == null) {
            if (searchUser) {
                resolveBot(resolvedUser -> {
                    if (resolvedUser != null) {
                        invokeWebViewCustomMethod(method, data, false, callback);
                    } else {
                        callback.accept(null, "USER_NOT_FOUND");
                    }
                });
            } else {
                callback.accept(null, "USER_NOT_FOUND");
            }
            return;
        }
        TL_bots.invokeWebViewCustomMethod req = new TL_bots.invokeWebViewCustomMethod();
        req.bot = getMessagesController().getInputUser(user);
        req.custom_method = method;
        req.params = new TLRPC.TL_dataJSON();
        req.params.data = data;
        getConnectionsManager().sendRequest(req, (res, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                callback.accept(null, error.text);
            } else if (res instanceof TLRPC.TL_dataJSON dataJSON) {
                callback.accept(dataJSON.data, null);
            } else {
                callback.accept(null, null);
            }
        }));
    }

    public void setItem(String key, String value, @NonNull BiConsumer<String, String> callback) {
        Map<String, String> map = new HashMap<>();
        map.put("key", key);
        map.put("value", value);
        invokeWebViewCustomMethod("saveStorageValue", GSON.toJson(map), callback);
    }

    public void getItem(String key, @NonNull BiConsumer<String, String> callback) {
        getItems(new String[]{key}, (res, error) -> {
            if (res != null) {
                callback.accept(res.get(key), null);
            } else {
                callback.accept(null, error);
            }
        });
    }

    public void getItems(String[] keys, @NonNull BiConsumer<Map<String, String>, String> callback) {
        Map<String, String[]> map = new HashMap<>();
        map.put("keys", keys);
        invokeWebViewCustomMethod("getStorageValues", GSON.toJson(map), (res, error) -> {
            if (error == null && res != null) {
                try {
                    callback.accept(GSON.fromJson(res, STRING_MAP_TYPE), null);
                } catch (Exception e) {
                    callback.accept(null, e.getMessage());
                }
            } else {
                callback.accept(null, error);
            }
        });
    }

    public void removeItem(String key, @NonNull BiConsumer<String, String> callback) {
        removeItems(new String[]{key}, callback);
    }

    public void removeItems(String[] keys, @NonNull BiConsumer<String, String> callback) {
        Map<String, String[]> map = new HashMap<>();
        map.put("keys", keys);
        invokeWebViewCustomMethod("deleteStorageValues", GSON.toJson(map), callback);
    }

    public void getKeys(BiConsumer<String[], String> callback) {
        invokeWebViewCustomMethod("getStorageKeys", "{}", (res, error) -> {
            if (error == null && res != null) {
                try {
                    String[] keys = GSON.fromJson(res, String[].class);
                    callback.accept(keys, null);
                } catch (Exception e) {
                    callback.accept(null, e.getMessage());
                }
            } else {
                callback.accept(null, error);
            }
        });
    }
}
