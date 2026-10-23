package nu.gpu.aoigram.config;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public final class FloatConfig extends ConfigItem<Float> {

    FloatConfig(String key, float defaultValue) {
        super(key, defaultValue);
    }

    @Override
    Float read(SharedPreferences preferences) {
        return preferences.getFloat(getKey(), getDefaultValue());
    }

    @Override
    Float read(JSONObject json) throws JSONException {
        return (float) json.getDouble(getKey());
    }

    @Override
    void write(SharedPreferences.Editor editor, Float value) {
        editor.putFloat(getKey(), value);
    }
}
