package nu.gpu.aoigram.config;

import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public final class StringConfig extends ConfigItem<String> {

    final boolean secret;

    StringConfig(String key, String defaultValue) {
        this(key, defaultValue, false);
    }

    StringConfig(String key, String defaultValue, boolean secret) {
        super(key, defaultValue);
        this.secret = secret;
    }

    @Override
    String read(SharedPreferences preferences) {
        return preferences.getString(getKey(), getDefaultValue());
    }

    @Override
    String read(JSONObject json) throws JSONException {
        return json.getString(getKey());
    }

    @Override
    void write(SharedPreferences.Editor editor, String value) {
        editor.putString(getKey(), value);
    }
}
