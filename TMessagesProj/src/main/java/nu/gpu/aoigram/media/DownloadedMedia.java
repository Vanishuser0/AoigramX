package nu.gpu.aoigram.media;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;

public final class DownloadedMedia {

    public static boolean canClear(MessageObject message) {
        return switch (message.type) {
            case MessageObject.TYPE_PHOTO, MessageObject.TYPE_VIDEO, MessageObject.TYPE_GIF,
                 MessageObject.TYPE_FILE,
                 MessageObject.TYPE_VOICE, MessageObject.TYPE_MUSIC,
                 MessageObject.TYPE_ROUND_VIDEO ->
                    (message.mediaExists() || message.hasVideoQualities() && message.cachedQuality != null) && !message.needDrawBluredPreview() && !DownloadController.getInstance(message.currentAccount).canDownloadMedia(message);
            case MessageObject.TYPE_TEXT -> message.mediaExists && message.isWebpage() && message.getDocument() != null && !DownloadController.getInstance(message.currentAccount).canDownloadMedia(message);
            default -> false;
        };
    }

    public static void clear(MessageObject message, Utilities.Callback<Long> done) {
        FileLoader fileLoader = FileLoader.getInstance(message.currentAccount);
        fileLoader.getFileLoaderQueue().postRunnable(() -> {
            ArrayList<File> files = new ArrayList<>();
            files.add(fileLoader.getPathToMessage(message.messageOwner));
            ArrayList<TLRPC.Document> documents = new ArrayList<>(MessageObject.getMedia(message).alt_documents);
            TLRPC.Document main = MessageObject.getDocument(message.messageOwner);
            if (main != null) {
                documents.add(main);
            }
            for (TLRPC.Document document : documents) {
                files.add(fileLoader.getPathToAttach(document, false));
                files.add(fileLoader.getPathToAttach(document, true));
            }
            long size = 0;
            for (File file : files) {
                long length = file.length();
                if (file.delete()) {
                    size += length;
                }
            }
            long freed = size;
            AndroidUtilities.runOnUIThread(() -> {
                message.aoiResetVideoQualities();
                message.checkMediaExistance();
                done.run(freed);
            });
        });
    }
}
