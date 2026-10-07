package nu.gpu.aoigram.reactions;

import static org.telegram.ui.Components.Reactions.ReactionsUtils.createAnimatedEmojiSpan;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import org.telegram.messenger.Emoji;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Reactions.CustomReactionEditText;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble.VisibleReaction;

import java.util.ArrayList;

@SuppressLint("ViewConstructor")
public final class PinnedReactionsEditor extends CustomReactionEditText {

    private final int currentAccount;
    private final Runnable onChanged;
    private ArrayList<VisibleReaction> reactions = new ArrayList<>();

    public PinnedReactionsEditor(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider, Runnable onChanged) {
        super(context, resourcesProvider, Integer.MAX_VALUE);
        this.currentAccount = currentAccount;
        this.onChanged = onChanged;
        NotificationCenter.listenEmojiLoading(this);
    }

    public void setReactions(ArrayList<VisibleReaction> reactions) {
        this.reactions = reactions;
        render(reactions.size());
    }

    public void refreshReactions() {
        render(Math.min(getEditTextSelectionEnd(), reactions.size()));
    }

    @Override
    public void removeReactionsSpan(boolean animate) {
        super.removeReactionsSpan(false);
    }

    @Override
    public void addReactionsSpan() {
        int start = getEditTextSelectionStart();
        int end = getEditTextSelectionEnd();
        super.addReactionsSpan();
        setSelection(start, end);
    }

    public void toggleReaction(VisibleReaction reaction) {
        int cursor = Math.min(getEditTextSelectionEnd(), reactions.size());
        int index = reactions.indexOf(reaction);
        if (index >= 0) {
            reactions.remove(index);
            if (index < cursor) cursor--;
        } else {
            reactions.add(cursor++, reaction);
        }
        render(cursor);
        onChanged.run();
    }

    public boolean deleteReactions() {
        int start = Math.min(getEditTextSelectionStart(), reactions.size());
        int end = Math.min(getEditTextSelectionEnd(), reactions.size());
        int from = Math.min(start, end);
        int to = Math.max(start, end);
        if (from == to) {
            if (from == 0) return false;
            from--;
        }
        reactions.subList(from, to).clear();
        render(from);
        onChanged.run();
        return true;
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    private void render(int cursor) {
        SpannableStringBuilder text = new SpannableStringBuilder();
        for (VisibleReaction reaction : reactions) {
            Object span;
            if (reaction.documentId != 0) {
                span = createAnimatedEmojiSpan(null, reaction.documentId, getFontMetricsInt());
            } else {
                TLRPC.TL_availableReaction available = MediaDataController.getInstance(currentAccount).getReactionsMap().get(reaction.emojicon);
                if (available != null) {
                    span = createAnimatedEmojiSpan(available.activate_animation, available.activate_animation.id, getFontMetricsInt());
                } else {
                    span = new Emoji.EmojiSpan(Emoji.getEmojiDrawable(reaction.emojicon), ImageSpan.ALIGN_BASELINE, getFontMetricsInt());
                }
            }
            int start = text.length();
            text.append('\uFFFC');
            text.setSpan(span, start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        setText(text);
        setSelection(cursor);
        if (!hasFocus()) addReactionsSpan();
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        if (id == R.id.menu_delete || id == android.R.id.cut) {
            return deleteReactions();
        }
        return false;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            deleteReactions();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL) {
            int cursor = getEditTextSelectionEnd();
            if (hasSelection()) {
                deleteReactions();
            } else if (cursor < reactions.size()) {
                setSelection(cursor, cursor + 1);
                deleteReactions();
            }
            return true;
        }
        if (event.isPrintingKey() || keyCode == KeyEvent.KEYCODE_ENTER) return true;
        return super.onKeyDown(keyCode, event);
    }
}
