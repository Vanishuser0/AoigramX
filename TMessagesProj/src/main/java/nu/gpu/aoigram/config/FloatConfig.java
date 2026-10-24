package nu.gpu.aoigram.config;

import android.content.SharedPreferences;

public final class FloatConfig extends ConfigItem<Float> {

    FloatConfig(String key, float defaultValue) {
        super(key, defaultValue);
    }

    @Override
    Float read(SharedPreferences preferences) {
        return preferences.getFloat(getKey(), getDefaultValue());
    }

    @Override
    void write(SharedPreferences.Editor editor, Float value) {
        editor.putFloat(getKey(), value);
    }
}
