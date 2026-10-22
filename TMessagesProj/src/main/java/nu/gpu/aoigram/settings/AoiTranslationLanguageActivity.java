package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import android.widget.EditText;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.Locale;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.translate.TranslationLanguages;
import nu.gpu.aoigram.translate.TranslationOptions;

public final class AoiTranslationLanguageActivity extends BaseAoiSettingsActivity {

    private String query = "";
    private final ArrayList<TranslationLanguages.Language> languages = TranslationLanguages.list(TranslationOptions.defaultProvider());

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.TranslationLanguage);
    }

    @Override
    protected void onCreateSettingsView(Context context) {
        actionBar.createMenu().addItem(1, R.drawable.outline_header_search).setIsSearchField(true)
                .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    @Override
                    public void onSearchCollapse() {
                        query = "";
                        listView.adapter.update(false);
                    }

                    @Override
                    public void onTextChanged(EditText editText) {
                        query = editText.getText().toString().toLowerCase(Locale.getDefault()).trim();
                        listView.adapter.update(false);
                    }
                })
                .setSearchFieldHint(getString(R.string.Search));
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        String selected = AoiConfig.translationLanguage.get();
        String selectedCode = selected.isEmpty() ? "" : TranslationLanguages.map(TranslationOptions.defaultProvider(), selected);
        items.add(LanguageRow.of(1, getString(R.string.TranslationFollowApp), "").setChecked(selected.isEmpty()));
        if (!selected.isEmpty()) {
            Locale locale = Locale.forLanguageTag(selectedCode);
            UItem item = LanguageRow.of(2, locale.getDisplayName(locale), locale.getDisplayName()).setChecked(true);
            item.object = selected;
            items.add(item);
        }
        items.add(UItem.asShadow(null));
        for (TranslationLanguages.Language language : languages) {
            if (language.code().equals(selectedCode)) continue;
            String search = (language.code() + " " + language.name() + " " + language.nativeName()).toLowerCase(Locale.getDefault());
            if (!search.contains(query)) continue;
            UItem item = LanguageRow.of(2, language.nativeName(), language.name());
            item.object = language.code();
            items.add(item);
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        AoiConfig.translationLanguage.set(item.id == 1 ? "" : (String) item.object);
        finishFragment();
    }

    private static final class LanguageRow extends UItem.UItemFactory<TextRadioCell> {
        static { setup(new LanguageRow()); }

        @Override
        public TextRadioCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new TextRadioCell(context);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            TextRadioCell cell = (TextRadioCell) view;
            cell.updateRTL();
            if (item.textValue.length() == 0) {
                cell.setTextAndCheck(item.text.toString(), item.checked, divider);
            } else {
                cell.setTextAndValueAndCheck(item.text.toString(), item.textValue.toString(), item.checked, false, divider);
            }
        }

        static UItem of(int id, String text, String value) {
            UItem item = UItem.ofFactory(LanguageRow.class);
            item.id = id;
            item.text = text;
            item.textValue = value;
            return item;
        }
    }
}
