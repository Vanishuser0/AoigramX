package nu.gpu.aoigram.config;

import android.content.SharedPreferences;

public final class BoolConfig extends ConfigItem<Boolean> {

    BoolConfig(String key, boolean defaultValue) {
        super(key, defaultValue);
    }

    public synchronized void toggle() {
        set(!get());
    }

    @Override
    Boolean read(SharedPreferences preferences) {
        return preferences.getBoolean(getKey(), getDefaultValue());
    }

    @Override
    void write(SharedPreferences.Editor editor, Boolean value) {
        editor.putBoolean(getKey(), value);
    }
}
