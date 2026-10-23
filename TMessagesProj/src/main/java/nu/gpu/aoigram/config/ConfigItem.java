package nu.gpu.aoigram.config;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class ConfigItem<T> {

    private final String key;
    private final T defaultValue;
    private volatile T value;

    ConfigItem(String key, T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public final String getKey() {
        return key;
    }

    public final T getDefaultValue() {
        return defaultValue;
    }

    public final T get() {
        return value;
    }

    public final synchronized void set(T value) {
        SharedPreferences.Editor editor = AoiConfig.preferences.edit();
        write(editor, value);
        editor.apply();
        this.value = value;
    }

    final void load() {
        value = read(AoiConfig.preferences);
    }

    final void write(SharedPreferences.Editor editor, JSONObject json) throws JSONException {
        write(editor, read(json));
    }

    @SuppressWarnings("SameParameterValue")
    abstract T read(SharedPreferences preferences);

    abstract T read(JSONObject json) throws JSONException;

    abstract void write(SharedPreferences.Editor editor, T value);
}
