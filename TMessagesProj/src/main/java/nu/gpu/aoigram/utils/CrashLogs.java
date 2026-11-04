package nu.gpu.aoigram.utils;

import android.os.Build;

import org.json.JSONException;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import nu.gpu.aoigram.config.AoiConfig;

@SuppressWarnings({"DataFlowIssue", "ResultOfMethodCallIgnored"})
public final class CrashLogs {

    private static final int MAX_COUNT = 10;
    private static final String PENDING = "pending";
    private static final String SUFFIX = ".txt";

    private static boolean pending;

    public static void install() {
        pending = new File(getDir(), PENDING).exists();
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                write(thread, throwable);
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                handler.uncaughtException(thread, throwable);
            }
        });
    }

    public static File[] list() {
        File[] logs = getDir().listFiles((dir, name) -> name.endsWith(SUFFIX));
        Arrays.sort(logs, Comparator.comparing(File::getName).reversed());
        return logs;
    }

    public static long getTime(File log) {
        String name = log.getName();
        return Long.parseLong(name.substring(0, name.indexOf('_')));
    }

    public static String getType(File log) {
        String name = log.getName();
        return name.substring(name.indexOf('_') + 1, name.length() - SUFFIX.length());
    }

    public static String read(File log) throws IOException {
        return new String(Files.readAllBytes(log.toPath()), StandardCharsets.UTF_8);
    }

    public static void clear() {
        for (File log : list()) {
            log.delete();
        }
    }

    public static boolean hasPending() {
        return pending;
    }

    public static void clearPending() {
        pending = false;
        new File(getDir(), PENDING).delete();
    }

    private static void write(Thread thread, Throwable throwable) throws IOException, JSONException {
        long time = System.currentTimeMillis();
        StringWriter stackTrace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stackTrace));
        String report = "Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(time) + "\n"
                + "Version: " + AppUtil.getVersionName() + "\n"
                + "Commit: " + BuildConfig.COMMIT_ID.substring(0, 7) + "\n"
                + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "Android: " + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")\n"
                + "Thread: " + thread.getName() + "\n\n"
                + stackTrace + "\n"
                + "Config: " + AoiConfig.exportJson(false).toString(2) + "\n";
        File dir = getDir();
        Files.write(new File(dir, time + "_" + throwable.getClass().getSimpleName() + SUFFIX).toPath(), report.getBytes(StandardCharsets.UTF_8));
        new File(dir, PENDING).createNewFile();
        File[] logs = list();
        for (int i = MAX_COUNT; i < logs.length; i++) {
            logs[i].delete();
        }
    }

    private static File getDir() {
        return ApplicationLoader.getFilesDirFixed("logs/crashes");
    }
}
