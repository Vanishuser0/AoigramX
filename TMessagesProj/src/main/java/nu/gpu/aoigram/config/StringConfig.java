package nu.gpu.aoigram.config;

import android.content.SharedPreferences;

public final class StringConfig extends ConfigItem<String> {

    StringConfig(String key, String defaultValue) {
        super(key, defaultValue);
    }

    @Override
    String read(SharedPreferences preferences) {
        return preferences.getString(getKey(), getDefaultValue());
    }

    @Override
    void write(SharedPreferences.Editor editor, String value) {
        editor.putString(getKey(), value);
    }
}
