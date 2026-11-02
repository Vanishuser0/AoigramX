package nu.gpu.aoigram.messages;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SizeNotifierFrameLayout;

@SuppressLint("ViewConstructor")
public final class StickerSizePreview extends SizeNotifierFrameLayout {

    private final ChatMessageCell stickerCell;
    private final ChatMessageCell replyCell;

    public StickerSizePreview(Context context, BaseFragment fragment) {
        super(context, fragment.getParentLayout());
        setOccupyStatusBar(false);
        setBackgroundImage(Theme.getCachedWallpaper(), false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);

        int account = fragment.getCurrentAccount();
        int date = ConnectionsManager.getInstance(account).getCurrentTime();
        long selfId = UserConfig.getInstance(account).getClientUserId();

        TLRPC.TL_message question = createMessage(1, date, false, selfId);
        question.message = getString(R.string.StickerSizePreviewQuote);
        MessageObject questionObject = new MessageObject(account, question, true, false);

        TLRPC.TL_message sticker = createMessage(2, date, true, selfId);
        sticker.reply_to = createReplyHeader(question.id);
        sticker.media = new TLRPC.TL_messageMediaDocument();
        sticker.media.document = new TLRPC.TL_document();
        sticker.media.document.mime_type = "image/webp";
        sticker.media.document.file_reference = new byte[0];
        TLRPC.TL_documentAttributeSticker attributeSticker = new TLRPC.TL_documentAttributeSticker();
        attributeSticker.alt = "🐈";
        attributeSticker.stickerset = new TLRPC.TL_inputStickerSetID();
        sticker.media.document.attributes.add(attributeSticker);
        TLRPC.TL_documentAttributeImageSize imageSize = new TLRPC.TL_documentAttributeImageSize();
        imageSize.w = imageSize.h = 512;
        sticker.media.document.attributes.add(imageSize);
        MessageObject stickerObject = new MessageObject(account, sticker, true, false);
        stickerObject.useCustomPhoto = true;
        stickerObject.mediaExists = true;
        stickerObject.customReplyName = getString(R.string.StickerSizePreviewName);
        stickerObject.replyMessageObject = questionObject;

        TLRPC.User sender = new TLRPC.TL_user();
        sender.status = new TLRPC.TL_userStatusOnline();
        sender.status.expires = Integer.MAX_VALUE;

        TLRPC.TL_message reply = createMessage(3, date, false, selfId);
        reply.message = getString(R.string.StickerSizePreviewReply);
        reply.reply_to = createReplyHeader(sticker.id);
        MessageObject replyObject = new MessageObject(account, reply, true, false);
        replyObject.replyMessageObject = stickerObject;
        replyObject.customName = getString(R.string.StickerSizePreviewName);
        replyObject.customAvatarDrawable = ContextCompat.getDrawable(context, R.drawable.preview_avatar);

        LinearLayout messagesLayout = new LinearLayout(context);
        messagesLayout.setOrientation(LinearLayout.VERTICAL);
        stickerCell = setupCell(new ChatMessageCell(context, account) {
            @Override
            public int getMaxNameWidth() {
                return super.getMaxNameWidth() - dp(24);
            }
        }, fragment, stickerObject);
        messagesLayout.addView(stickerCell);
        replyCell = new ChatMessageCell(context, account) {
            @Override
            protected void dispatchDraw(@NonNull Canvas canvas) {
                ImageReceiver avatarImage = getAvatarImage();
                avatarImage.setImageCoords(avatarImage.getImageX(), getMeasuredHeight() - avatarImage.getImageHeight() - dp(4), avatarImage.getImageWidth(), avatarImage.getImageHeight());
                onlineStatus.drawAvatar(canvas, avatarImage, sender, account, null, this);
                super.dispatchDraw(canvas);
            }
        };
        replyCell.isChat = true;
        messagesLayout.addView(setupCell(replyCell, fragment, replyObject));
        addView(messagesLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 11, 0, 11));
    }

    private static TLRPC.TL_message createMessage(int id, int date, boolean out, long selfId) {
        TLRPC.TL_message message = new TLRPC.TL_message();
        message.id = id;
        message.date = date;
        message.dialog_id = 1;
        message.out = out;
        message.message = "";
        message.media = new TLRPC.TL_messageMediaEmpty();
        message.from_id = new TLRPC.TL_peerUser();
        message.peer_id = new TLRPC.TL_peerUser();
        if (out) {
            message.flags = 259;
            message.from_id.user_id = selfId;
        } else {
            message.flags = 257;
            message.peer_id.user_id = selfId;
        }
        return message;
    }

    private static TLRPC.TL_messageReplyHeader createReplyHeader(int replyToId) {
        TLRPC.TL_messageReplyHeader replyHeader = new TLRPC.TL_messageReplyHeader();
        replyHeader.flags |= 16;
        replyHeader.reply_to_msg_id = replyToId;
        return replyHeader;
    }

    private static ChatMessageCell setupCell(ChatMessageCell cell, BaseFragment fragment, MessageObject messageObject) {
        Context context = cell.getContext();
        cell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
            @Override
            public boolean canPerformActions() {
                return true;
            }

            @Override
            public void didPressImage(ChatMessageCell cell, float x, float y, boolean fullPreview) {
                BulletinFactory.of(fragment).createSimpleBulletin(ContextCompat.getDrawable(context, R.drawable.filter_cat), getString(R.string.StickerSizePreviewMeow)).setDuration(Bulletin.DURATION_SHORT).show();
            }
        });
        cell.setFullyDraw(true);
        cell.setMessageObject(messageObject, null, false, false, false);
        return cell;
    }

    public void update() {
        stickerCell.forceResetMessageObject();
        stickerCell.requestLayout();
    }

    public void updateOnlineStatus() {
        replyCell.invalidate();
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
    }

    @Override
    protected boolean useRootView() {
        return false;
    }

    @Override
    protected boolean isActionBarVisible() {
        return false;
    }
}
