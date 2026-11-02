package nu.gpu.aoigram.chat;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.View;

import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;

import nu.gpu.aoigram.config.AoiConfig;

public final class OnlineStatus {

    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    private TLRPC.User user;
    private int color;
    private float progress;

    public void drawAvatar(Canvas canvas, ImageReceiver avatarImage, TLRPC.User user, int currentAccount, Theme.ResourcesProvider resourcesProvider, View parent) {
        int targetColor = getColor(user, currentAccount, resourcesProvider);
        if (this.user != user) {
            this.user = user;
            progress = targetColor != 0 ? 1f : 0f;
        }
        if (targetColor != 0) {
            color = targetColor;
            if (progress < 1f) {
                progress = Math.min(1f, progress + 16f / 150f);
                parent.invalidate();
            }
        } else if (progress > 0f) {
            progress = Math.max(0f, progress - 16f / 150f);
            parent.invalidate();
        }
        if (progress == 0f) {
            avatarImage.draw(canvas);
            return;
        }
        float size = avatarImage.getImageWidth() * progress;
        float radius = size / 9f;
        float cx = avatarImage.getImageX2() - dp(6);
        float cy = avatarImage.getImageY2() - dp(8);
        int count = canvas.saveLayer(avatarImage.getImageX(), avatarImage.getImageY(), avatarImage.getImageX2(), avatarImage.getImageY2(), null);
        avatarImage.draw(canvas);
        canvas.drawCircle(cx, cy, radius + size / 27f, clearPaint);
        canvas.restoreToCount(count);
        paint.setColor(color);
        paint.setAlpha((int) (paint.getAlpha() * avatarImage.getAlpha()));
        canvas.drawCircle(cx, cy, radius, paint);
    }

    private static int getColor(TLRPC.User user, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
        if (!AoiConfig.showOnlineStatus.get() || user == null) {
            return 0;
        }
        TLRPC.User currentUser = MessagesController.getInstance(currentAccount).getUsers().get(user.id);
        if (currentUser != null) {
            user = currentUser;
        }
        if (user.status == null || user.self || user.bot || MessagesController.isSupportUser(user)) {
            return 0;
        }
        if (user.status.expires <= 0 && MessagesController.getInstance(currentAccount).onlinePrivacy.containsKey(user.id)) {
            return Theme.getColor(Theme.key_chats_onlineCircle, resourcesProvider);
        }
        int diff = user.status.expires - ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        if (diff > 0) {
            return Theme.getColor(Theme.key_chats_onlineCircle, resourcesProvider);
        } else if (diff > -15 * 60) {
            return Color.rgb(234, 234, 30);
        } else if (diff > -30 * 60) {
            return Color.rgb(234, 132, 30);
        } else if (diff > -60 * 60) {
            return Color.rgb(234, 30, 30);
        }
        return 0;
    }
}
