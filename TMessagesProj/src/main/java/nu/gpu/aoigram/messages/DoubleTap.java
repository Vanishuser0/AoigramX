package nu.gpu.aoigram.messages;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;

import nu.gpu.aoigram.config.AoiConfig;

public final class DoubleTap {

    public static final int NONE = 0;
    public static final int REACTION = 1;
    public static final int REPLY = 2;
    public static final int COPY = 3;
    public static final int FORWARD = 4;
    public static final int SAVE = 5;
    public static final int REPEAT = 6;
    public static final int TRANSLATE = 7;
    public static final int TRANSLATE_AI = 8;
    public static final int EDIT = 9;
    public static final int DELETE = 10;

    private static final int[] LABELS = {
            R.string.None,
            R.string.SendReaction,
            R.string.Reply,
            R.string.Copy,
            R.string.Forward,
            R.string.Save,
            R.string.Repeat,
            R.string.TranslateMessage,
            R.string.TranslateWithAI,
            R.string.Edit,
            R.string.Delete
    };

    private static final int[] ICONS = {
            R.drawable.msg_block,
            R.drawable.msg_reactions2,
            R.drawable.menu_reply,
            R.drawable.msg_copy,
            R.drawable.msg_forward,
            R.drawable.msg_saved,
            R.drawable.msg_repeat,
            R.drawable.msg_translate,
            R.drawable.msg_ai_translate,
            R.drawable.msg_edit,
            R.drawable.msg_delete
    };

    public static int getAction(MessageObject message) {
        return message.isOutOwner() ? AoiConfig.doubleTapOutgoingAction.get() : AoiConfig.doubleTapIncomingAction.get();
    }

    public static String getLabel(int action) {
        return getString(LABELS[action]);
    }

    public static int getIcon(int action) {
        return ICONS[action];
    }
}
