package nu.gpu.aoigram.transcription;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;

final class AudioRemux {

    static byte[] remux(File input) throws IOException {
        File output = new File(AndroidUtilities.getCacheDir(), "transcription_" + Utilities.random.nextLong() + ".m4a");
        MediaExtractor extractor = new MediaExtractor();
        MediaMuxer muxer = null;
        try {
            extractor.setDataSource(input.getAbsolutePath());
            int track = 0;
            while (!extractor.getTrackFormat(track).getString(MediaFormat.KEY_MIME, "").startsWith("audio/")) track++;
            extractor.selectTrack(track);
            muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int index = muxer.addTrack(extractor.getTrackFormat(track));
            muxer.start();
            ByteBuffer buffer = ByteBuffer.allocate(1 << 16);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while ((info.size = extractor.readSampleData(buffer, 0)) >= 0) {
                info.presentationTimeUs = extractor.getSampleTime();
                info.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                muxer.writeSampleData(index, buffer, info);
                extractor.advance();
            }
            muxer.stop();
            return Files.readAllBytes(output.toPath());
        } finally {
            extractor.release();
            if (muxer != null) muxer.release();
            Files.deleteIfExists(output.toPath());
        }
    }
}
