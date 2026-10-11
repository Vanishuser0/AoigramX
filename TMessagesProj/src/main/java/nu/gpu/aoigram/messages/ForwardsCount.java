package nu.gpu.aoigram.messages;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.StaticLayout;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.BaseCell;
import org.telegram.ui.Cells.ChatMessageCell;

public final class ForwardsCount {

    private Drawable drawable;
    private int color;
    private int count;
    private StaticLayout layout;
    private StaticLayout lastLayout;
    private StaticLayout animateLayout;
    private boolean animating;
    private float lastX;
    private float animateFromX;

    public boolean isChanged(MessageObject messageObject) {
        return count != messageObject.messageOwner.forwards;
    }

    @SuppressWarnings("DataFlowIssue")
    public int measure(MessageObject messageObject) {
        count = messageObject.messageOwner.forwards;
        if (count <= 0) {
            layout = null;
            return 0;
        }
        if (drawable == null) {
            drawable = ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.drawable.msg_reply_small).mutate();
        }
        String text = LocaleController.formatShortNumber(count, null);
        layout = StaticLayout.Builder.obtain(text, 0, text.length(), Theme.chat_timePaint, (int) Math.ceil(Theme.chat_timePaint.measureText(text))).setIncludePad(false).build();
        return (int) Math.ceil(layout.getWidth() + drawable.getIntrinsicWidth() * Theme.chat_timePaint.getTextSize() / drawable.getIntrinsicHeight()) + dp(8);
    }

    public void recordDrawingState() {
        lastLayout = layout;
    }

    public boolean animateChange() {
        animateFromX = lastX;
        if (layout == null || lastLayout != null && TextUtils.equals(lastLayout.getText(), layout.getText())) {
            return false;
        }
        animating = true;
        animateLayout = lastLayout;
        return true;
    }

    public void resetAnimation() {
        animating = false;
        animateLayout = null;
    }

    public float draw(Canvas canvas, ChatMessageCell cell, float timeX, float offsetX, float timeY, float alpha, float progress, boolean selected) {
        if (layout == null) {
            return 0;
        }
        ChatMessageCell.TransitionParams params = cell.transitionParams;
        MessageObject messageObject = cell.getMessageObject();
        boolean inAnimation = animating && animateLayout == null;
        float animationProgress = animating ? params.animateChangeProgress : 1f;
        float x = (params.shouldAnimateTimeX ? cell.timeX : timeX) + offsetX;
        if (params.shouldAnimateTimeX && !inAnimation && !(messageObject.isRoundVideo() && params.animateDrawBackground)) {
            x = AndroidUtilities.lerp(animateFromX, x, params.animateChangeProgress);
        }
        MessageObject.GroupedMessages group = cell.getCurrentMessagesGroup();
        if (group != null && group.transitionParams.backgroundChangeBounds) {
            x += group.transitionParams.offsetRight;
        }
        if (params.animateBackgroundBoundsInner) {
            x += cell.getAnimationOffsetX();
        }
        lastX = x;

        int colorKey;
        if (cell.shouldDrawTimeOnMedia()) {
            colorKey = messageObject.shouldDrawWithoutBackground() ? Theme.key_chat_serviceText : Theme.key_chat_mediaViews;
        } else if (messageObject.isOutOwner()) {
            colorKey = selected ? Theme.key_chat_outViewsSelected : Theme.key_chat_outViews;
        } else {
            colorKey = selected ? Theme.key_chat_inViewsSelected : Theme.key_chat_inViews;
        }
        int color = cell.getThemedColor(colorKey);
        if (this.color != color) {
            this.color = color;
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }

        float w = BaseCell.setDrawableBounds(drawable, x - dp(1), timeY, Theme.chat_timePaint.getTextSize());
        canvas.save();
        if (progress != 1f) {
            float scale = 0.5f + 0.5f * progress;
            canvas.scale(scale, scale, x + (w + dp(1) + layout.getWidth()) / 2f, drawable.getBounds().centerY());
        }
        canvas.save();
        canvas.scale(-1f, 1f, drawable.getBounds().exactCenterX(), 0);
        drawable.setAlpha((int) (255 * alpha * (inAnimation ? animationProgress : 1f)));
        drawable.draw(canvas);
        canvas.restore();
        canvas.translate(x + w + dp(1), timeY);
        int timeAlpha = Theme.chat_timePaint.getAlpha();
        if (animateLayout != null) {
            Theme.chat_timePaint.setAlpha((int) (timeAlpha * (1f - animationProgress)));
            animateLayout.draw(canvas);
        }
        Theme.chat_timePaint.setAlpha((int) (timeAlpha * animationProgress));
        layout.draw(canvas);
        Theme.chat_timePaint.setAlpha(timeAlpha);
        canvas.restore();
        return w + layout.getWidth() + dp(8);
    }
}
