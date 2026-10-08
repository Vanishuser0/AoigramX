package nu.gpu.aoigram.media;

import static org.telegram.messenger.LocaleController.getString;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;

import nu.gpu.aoigram.utils.ErrorFeedback;

public final class PhotoClipboard {

    public static boolean canCopy(MessageObject message) {
        return (isPhoto(message) || message.isSticker() && !message.isVideoSticker()) && getFile(message).isFile();
    }

    public static boolean isPhoto(MessageObject message) {
        return message.isPhoto() || message.isDocument() && message.getMimeType().startsWith("image/") && !MessageObject.isGifDocument(message.getDocument());
    }

    public static File getFile(MessageObject message) {
        if (!TextUtils.isEmpty(message.messageOwner.attachPath)) {
            File file = new File(message.messageOwner.attachPath);
            if (file.isFile()) {
                return file;
            }
        }
        FileLoader fileLoader = FileLoader.getInstance(message.currentAccount);
        File file;
        if (message.isWebpage() && message.isPhoto()) {
            file = fileLoader.getPathToAttach(FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, AndroidUtilities.getPhotoSize(true), false, null, true), true);
        } else {
            file = fileLoader.getPathToMessage(message.messageOwner);
        }
        if (!file.isFile()) {
            file = new File(FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE), file.getName());
        }
        return file;
    }

    public static void copy(File file, boolean sticker, BulletinFactory bulletins) {
        try {
            Context context = ApplicationLoader.applicationContext;
            ClipboardManager clipboard = context.getSystemService(ClipboardManager.class);
            Uri uri = FileProvider.getUriForFile(context, ApplicationLoader.getApplicationId() + ".provider", file);
            clipboard.setPrimaryClip(ClipData.newUri(context.getContentResolver(), getString(R.string.CopyPhoto), uri));
        } catch (Exception e) {
            FileLog.e(e);
            ErrorFeedback.show(getString(R.string.ErrorOccurred), bulletins, AlertDialog::show, true);
            return;
        }
        bulletins.createCopyBulletin(getString(sticker ? R.string.StickerCopied : R.string.PhotoCopied)).show();
    }
}
