package nu.gpu.aoigram.reactions;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble.VisibleReaction;
import org.telegram.ui.Components.ReactionsContainerLayout;
import org.telegram.ui.Components.SizeNotifierFrameLayout;

import java.util.List;

@SuppressLint("ViewConstructor")
public final class PinnedReactionsPreview extends SizeNotifierFrameLayout {

    private final PreviewBar reactionsBar;

    public PinnedReactionsPreview(Context context, BaseFragment fragment) {
        super(context, fragment.getParentLayout());
        setOccupyStatusBar(false);
        setBackgroundImage(new ColorDrawable(Theme.getColor(Theme.key_windowBackgroundGray, fragment.getResourceProvider())), false);
        reactionsBar = new PreviewBar(context, fragment);
        reactionsBar.setPadding(dp(4), dp(24), dp(4), dp(24));
        reactionsBar.setTop(false);
        reactionsBar.setClipChildren(false);
        reactionsBar.setClipToPadding(false);
        reactionsBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        addView(reactionsBar, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 92, Gravity.CENTER));
    }

    public void setReactions(List<VisibleReaction> reactions, boolean animated) {
        reactionsBar.setReactions(reactions, animated);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected boolean useRootView() {
        return false;
    }

    @Override
    protected boolean isActionBarVisible() {
        return false;
    }

    private static final class PreviewBar extends ReactionsContainerLayout {
        PreviewBar(Context context, BaseFragment fragment) {
            super(TYPE_DEFAULT, fragment, context, fragment.getCurrentAccount(), fragment.getResourceProvider());
            setDelegate((view, reaction, longPress, addToRecent) -> {});
        }

        @Override
        public boolean showCustomEmojiReaction() {
            return true;
        }

        void setReactions(List<VisibleReaction> reactions, boolean animated) {
            setVisibleReactionsList(reactions, animated);
            setTransitionProgress(1);
        }
    }
}
