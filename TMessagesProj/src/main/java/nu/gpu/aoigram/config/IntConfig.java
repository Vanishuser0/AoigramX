package nu.gpu.aoigram.config;

import android.content.SharedPreferences;

public final class IntConfig extends ConfigItem<Integer> {

    IntConfig(String key, int defaultValue) {
        super(key, defaultValue);
    }

    @Override
    Integer read(SharedPreferences preferences) {
        return preferences.getInt(getKey(), getDefaultValue());
    }

    @Override
    void write(SharedPreferences.Editor editor, Integer value) {
        editor.putInt(getKey(), value);
    }
}
