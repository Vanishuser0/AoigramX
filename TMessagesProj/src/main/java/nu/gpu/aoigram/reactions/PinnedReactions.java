package nu.gpu.aoigram.reactions;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble.VisibleReaction;

import java.util.ArrayList;
import java.util.List;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.config.BoolConfig;
import nu.gpu.aoigram.config.StringConfig;

public final class PinnedReactions {

    public static final int LIMIT = 7;

    private PinnedReactions() {
    }

    public static BoolConfig enabled(boolean channel) {
        return channel ? AoiConfig.usePinnedReactionsChannels : AoiConfig.usePinnedReactionsChats;
    }

    public static String getLabel() {
        boolean chats = enabled(false).get();
        boolean channels = enabled(true).get();
        if (chats && channels) return getString(R.string.PinnedReactionsOn);
        if (chats) return getString(R.string.PinnedReactionsChatsOnly);
        if (channels) return getString(R.string.PinnedReactionsChannelsOnly);
        return getString(R.string.PinnedReactionsOff);
    }

    private static StringConfig config(boolean channel) {
        return channel ? AoiConfig.pinnedReactionsChannels : AoiConfig.pinnedReactionsChats;
    }

    public static ArrayList<VisibleReaction> get(boolean channel) {
        ArrayList<VisibleReaction> reactions = new ArrayList<>();
        String value = config(channel).get();
        if (!value.isEmpty()) {
            for (String reaction : value.split(",")) {
                reactions.add(VisibleReaction.fromEmojicon(reaction));
            }
        }
        return reactions;
    }

    public static void set(boolean channel, List<VisibleReaction> reactions) {
        ArrayList<String> values = new ArrayList<>();
        for (VisibleReaction reaction : reactions) {
            values.add(reaction.documentId == 0 ? reaction.emojicon : "animated_" + reaction.documentId);
        }
        config(channel).set(String.join(",", values));
    }

    public static void prioritize(List<VisibleReaction> reactions, int currentAccount, boolean channel, boolean allReactionsAvailable) {
        if (!enabled(channel).get()) {
            return;
        }
        int position = !reactions.isEmpty() && reactions.get(0).isStar ? 1 : 0;
        for (VisibleReaction reaction : get(channel)) {
            if (reactions.remove(reaction)
                    || reaction.documentId != 0 && allReactionsAvailable && UserConfig.getInstance(currentAccount).isPremium()) {
                reactions.add(position++, reaction);
            }
        }
    }
}
