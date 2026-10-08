package nu.gpu.aoigram.messages;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.MessageDrawable;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.SizeNotifierFrameLayout;

@SuppressLint({"ClickableViewAccessibility", "ViewConstructor"})
public final class DoubleTapPreview extends SizeNotifierFrameLayout {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bubble incoming = new Bubble(false);
    private final Bubble outgoing = new Bubble(true);
    private final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            Bubble bubble = incoming.contains(e) ? incoming : outgoing.contains(e) ? outgoing : null;
            if (bubble == null || bubble.icon == null) return false;
            bubble.pop.set(0, true);
            invalidate();
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            return true;
        }
    });

    public DoubleTapPreview(Context context, BaseFragment fragment) {
        super(context, fragment.getParentLayout());
        setOccupyStatusBar(false);
        setBackgroundImage(Theme.getCachedWallpaper(), false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeWidth(dp(6));
    }

    public void setActions(int incomingAction, int outgoingAction, boolean animated) {
        incoming.set(incomingAction, animated);
        outgoing.set(outgoingAction, animated);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(148), MeasureSpec.EXACTLY));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }

    @Override
    protected boolean useRootView() {
        return false;
    }

    @Override
    protected boolean isActionBarVisible() {
        return false;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        int width = Math.min(dp(260), getMeasuredWidth() - dp(80));
        incoming.draw(canvas, dp(8), dp(14), width);
        outgoing.draw(canvas, getMeasuredWidth() - dp(8) - width, dp(78), width);
    }

    private final class Bubble {

        private final boolean out;
        private final MessageDrawable background;
        private final AnimatedFloat progress = new AnimatedFloat(DoubleTapPreview.this, 320, CubicBezierInterpolator.EASE_OUT_QUINT);
        private final AnimatedFloat pop = new AnimatedFloat(1, DoubleTapPreview.this, 0, 450, CubicBezierInterpolator.EASE_OUT_BACK);
        private int action = -1;
        private Drawable icon;
        private Drawable oldIcon;

        Bubble(boolean out) {
            this.out = out;
            background = new MessageDrawable(MessageDrawable.TYPE_TEXT, out, false);
        }

        @SuppressWarnings("DataFlowIssue")
        void set(int action, boolean animated) {
            if (this.action == action) return;
            this.action = action;
            oldIcon = animated ? icon : null;
            icon = action == DoubleTap.NONE ? null : ContextCompat.getDrawable(getContext(), DoubleTap.getIcon(action)).mutate();
            progress.set(animated ? 0 : 1, true);
            invalidate();
        }

        boolean contains(MotionEvent e) {
            return background.getBounds().contains((int) e.getX(), (int) e.getY());
        }

        void draw(Canvas canvas, int left, int top, int width) {
            background.setTop(top, getMeasuredWidth(), getMeasuredHeight(), false, false);
            background.setBounds(left, top, left + width, top + dp(56));
            background.draw(canvas);

            float contentLeft = left + dp(out ? 16 : 22);
            float contentRight = left + width - dp(out ? 22 : 16);
            float cy = top + dp(28);
            linePaint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(out ? Theme.key_chat_messageTextOut : Theme.key_chat_messageTextIn), 0x33));
            canvas.drawLine(contentLeft, cy - dp(8), contentRight, cy - dp(8), linePaint);
            canvas.drawLine(contentLeft, cy + dp(8), contentLeft + (contentRight - contentLeft) * (out ? 0.45f : 0.6f), cy + dp(8), linePaint);

            float t = progress.set(1);
            float scale = oldIcon == null ? icon == null ? 0 : t : icon == null ? 1 - t : 1;
            if (scale <= 0) return;
            float cx = (contentLeft + contentRight) / 2f;
            int color = Theme.getColor(out ? Theme.key_chat_outReactionButtonBackground : Theme.key_chat_inReactionButtonBackground);
            fillPaint.setColor(ColorUtils.setAlphaComponent(color, (int) (Color.alpha(color) * 0.156f)));
            canvas.drawCircle(cx, cy, dp(21) * scale, fillPaint);
            float p = pop.set(1);
            if (p < 1) {
                fillPaint.setColor(ColorUtils.setAlphaComponent(color, (int) (Color.alpha(color) * 0.35f * (1 - p))));
                canvas.drawCircle(cx, cy, dp(14 + 16 * p), fillPaint);
            }
            canvas.save();
            float bounce = 0.7f + 0.3f * p;
            canvas.scale(bounce, bounce, cx, cy);
            fillPaint.setColor(color);
            canvas.drawCircle(cx, cy, dp(14) * scale, fillPaint);
            canvas.rotate(20 * (1 - p), cx, cy);
            int iconColor = Theme.getColor(out ? Theme.key_chat_outReactionButtonTextSelected : Theme.key_chat_inReactionButtonTextSelected);
            drawIcon(canvas, oldIcon, 1 - t, iconColor, cx, cy);
            drawIcon(canvas, icon, t, iconColor, cx, cy);
            canvas.restore();
        }

        private void drawIcon(Canvas canvas, Drawable drawable, float scale, int color, float cx, float cy) {
            if (drawable == null || scale <= 0) return;
            float half = dp(9) * scale;
            drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
            drawable.setBounds((int) (cx - half), (int) (cy - half), (int) (cx + half), (int) (cy + half));
            drawable.draw(canvas);
        }
    }
}
