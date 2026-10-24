package nu.gpu.aoigram.settings;

import androidx.annotation.Nullable;

import org.telegram.messenger.LocaleController;
import org.telegram.ui.ProfileActivity;

import java.util.Locale;

final class AoiSettingsSearchItem extends ProfileActivity.SearchAdapter.SearchResult {

    final String pageKey;
    final String itemKey;

    private final String searchText;

    AoiSettingsSearchItem(int id, String title, String path, @Nullable String section, int iconResId, String pageKey, String itemKey) {
        super(id, title, null, path, section, iconResId, () -> {});
        this.pageKey = pageKey;
        this.itemKey = itemKey;
        searchText = title.toLowerCase(Locale.ROOT);
    }

    boolean matches(String query) {
        String[] terms = query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        for (String term : terms) {
            if (!searchText.contains(term)) {
                String transliteratedTerm = LocaleController.getInstance().getTranslitString(term);
                if (transliteratedTerm.isEmpty() || !searchText.contains(transliteratedTerm)) {
                    return false;
                }
            }
        }
        return true;
    }
}
