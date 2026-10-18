package nu.gpu.aoigram.chat;

import static org.telegram.messenger.LocaleController.getString;

import org.telegram.messenger.R;
import org.telegram.utils.recorder.round.RoundVideoSession;
import org.telegram.utils.settings.SharedSettings;

import nu.gpu.aoigram.config.AoiConfig;

public final class VideoMessageCamera {

    public static final int DEFAULT = 0;
    public static final int ASK = 1;
    public static final int FRONT = 2;
    public static final int REAR = 3;

    public static void apply(int type) {
        if (type == FRONT || type == REAR) {
            SharedSettings.roundVideoLastCamera.set(type == FRONT ? RoundVideoSession.CameraFacing.FRONT : RoundVideoSession.CameraFacing.BACK);
        }
    }

    public static boolean isFrontface(boolean frontface) {
        return AoiConfig.videoMessageCamera.get() == DEFAULT ? frontface : SharedSettings.roundVideoLastCamera.get() == RoundVideoSession.CameraFacing.FRONT;
    }

    public static String getLabel(int type) {
        return getString(switch (type) {
            case ASK -> R.string.VideoMessageCameraAsk;
            case FRONT -> R.string.FrontCamera;
            case REAR -> R.string.RearCamera;
            default -> R.string.Default;
        });
    }
}
