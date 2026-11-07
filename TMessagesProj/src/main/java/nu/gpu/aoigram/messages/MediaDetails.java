package nu.gpu.aoigram.messages;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.media.MediaDataSource;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.util.SparseArray;

import androidx.exifinterface.media.ExifInterface;

import com.google.common.io.ByteStreams;

import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.secretmedia.EncryptedFileInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public final class MediaDetails {

    public final ArrayList<MediaFormat> tracks = new ArrayList<>();
    public final SparseArray<String> metadata = new SparseArray<>();
    public final BitmapFactory.Options image = new BitmapFactory.Options();
    public ExifInterface exif;

    @SuppressLint("InlinedApi")
    public static MediaDetails read(File file, boolean isImage, boolean encrypted) {
        MediaDetails details = new MediaDetails();
        if (isImage) {
            details.image.inJustDecodeBounds = true;
            try (InputStream input = openStream(file, encrypted)) {
                BitmapFactory.decodeStream(input, null, details.image);
            } catch (Exception e) {
                FileLog.e(e);
            }
            if (details.image.outMimeType != null && ExifInterface.isSupportedMimeType(details.image.outMimeType)) {
                try (InputStream input = openStream(file, encrypted)) {
                    details.exif = new ExifInterface(input);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return details;
        }

        try (MediaDataSource source = encrypted ? new EncryptedMediaDataSource(file) : null;
             MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            if (encrypted) {
                retriever.setDataSource(source);
            } else {
                retriever.setDataSource(file.getAbsolutePath());
            }
            int[] keys = {
                    MediaMetadataRetriever.METADATA_KEY_MIMETYPE,
                    MediaMetadataRetriever.METADATA_KEY_DURATION,
                    MediaMetadataRetriever.METADATA_KEY_BITRATE,
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH,
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT,
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION,
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT,
                    MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE,
                    MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE,
                    MediaMetadataRetriever.METADATA_KEY_TITLE,
                    MediaMetadataRetriever.METADATA_KEY_ARTIST,
                    MediaMetadataRetriever.METADATA_KEY_ALBUM,
                    MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                    MediaMetadataRetriever.METADATA_KEY_COMPOSER,
                    MediaMetadataRetriever.METADATA_KEY_GENRE,
                    MediaMetadataRetriever.METADATA_KEY_YEAR,
                    MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER
            };
            for (int key : keys) {
                String value = retriever.extractMetadata(key);
                if (value != null && !value.isEmpty()) {
                    details.metadata.put(key, value);
                }
            }
        } catch (IOException | RuntimeException e) {
            FileLog.e(e);
        }

        MediaExtractor extractor = new MediaExtractor();
        try (MediaDataSource source = encrypted ? new EncryptedMediaDataSource(file) : null) {
            if (encrypted) {
                extractor.setDataSource(source);
            } else {
                extractor.setDataSource(file.getAbsolutePath());
            }
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                details.tracks.add(extractor.getTrackFormat(i));
            }
        } catch (IOException | RuntimeException e) {
            FileLog.e(e);
        } finally {
            extractor.release();
        }
        return details;
    }

    private static InputStream openStream(File file, boolean encrypted) throws Exception {
        if (!encrypted) {
            return new FileInputStream(file);
        }
        File keyFile = new File(FileLoader.getInternalCacheDir(), file.getName() + ".key");
        return new BufferedInputStream(new EncryptedFileInputStream(file, keyFile));
    }

    private static final class EncryptedMediaDataSource extends MediaDataSource {

        private final File file;
        private final long length;

        private EncryptedMediaDataSource(File file) {
            this.file = file;
            length = file.length();
        }

        @Override
        public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
            if (size == 0) {
                return 0;
            }
            if (position >= length) {
                return -1;
            }
            try (InputStream input = openStream(file, true)) {
                ByteStreams.skipFully(input, position);
                return input.read(buffer, offset, (int) Math.min(size, length - position));
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public long getSize() {
            return length;
        }

        @Override
        public void close() {
        }
    }
}
