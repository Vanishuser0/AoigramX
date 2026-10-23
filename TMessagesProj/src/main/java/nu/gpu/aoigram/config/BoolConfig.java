package nu.gpu.aoigram.config;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

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
    Boolean read(JSONObject json) throws JSONException {
        return json.getBoolean(getKey());
    }

    @Override
    void write(SharedPreferences.Editor editor, Boolean value) {
        editor.putBoolean(getKey(), value);
    }
}
