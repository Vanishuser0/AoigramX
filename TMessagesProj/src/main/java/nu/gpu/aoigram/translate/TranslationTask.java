package nu.gpu.aoigram.translate;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.ConnectionsManager;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Call;

public final class TranslationTask {

    private final Set<Call> calls = ConcurrentHashMap.newKeySet();
    private final Set<Runnable> pending = ConcurrentHashMap.newKeySet();
    private final Set<Runnable> cancellations = ConcurrentHashMap.newKeySet();
    private volatile boolean cancelled;
    public int httpCode;

    void add(Call call) {
        calls.add(call);
        if (cancelled) {
            call.cancel();
        }
    }

    void remove(Call call) {
        calls.remove(call);
    }

    public void addTelegram(int account, int id) {
        onCancel(() -> ConnectionsManager.getInstance(account).cancelRequest(id, true));
    }

    public void onCancel(Runnable action) {
        cancellations.add(action);
        if (cancelled && cancellations.remove(action)) {
            action.run();
        }
    }

    public void post(Runnable action) {
        post(action, 0);
    }

    public void post(Runnable action, long delay) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                pending.remove(this);
                if (!cancelled) {
                    action.run();
                }
            }
        };
        pending.add(runnable);
        AndroidUtilities.runOnUIThread(runnable, delay);
        if (cancelled) {
            AndroidUtilities.cancelRunOnUIThread(runnable);
        }
    }

    public void cancel() {
        cancelled = true;
        for (Call call : calls) {
            call.cancel();
        }
        for (Runnable runnable : pending) {
            AndroidUtilities.cancelRunOnUIThread(runnable);
        }
        for (Runnable action : cancellations) {
            if (cancellations.remove(action)) {
                action.run();
            }
        }
        calls.clear();
        pending.clear();
    }
}
