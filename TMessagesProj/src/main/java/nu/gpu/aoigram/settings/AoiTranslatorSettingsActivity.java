package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.RestrictedLanguagesSelectActivity;

import java.util.ArrayList;
import java.util.Locale;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.translate.TranslationLanguages;
import nu.gpu.aoigram.translate.TranslationOptions;

public final class AoiTranslatorSettingsActivity extends BaseAoiSettingsActivity {

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiTranslatorSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.TRANSLATOR_PAGE;
    }

    @Override
    protected String getItemKey(UItem item) {
        return switch (item.id) {
            case 1 -> "translationProvider";
            case 2 -> "translationLanguage";
            case 3 -> "restrictedLanguages";
            case 4 -> "translationShowOriginal";
            case 5 -> "llm";
            case 6 -> "contextTranslate";
            case 7 -> "chatTranslate";
            default -> null;
        };
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        int provider = TranslationOptions.defaultProvider();
        String language = AoiConfig.translationLanguage.get();
        TranslateController controller = getMessagesController().getTranslateController();
        items.add(UItem.asHeader(getString(R.string.TranslateMessages)));
        if (provider != TranslationOptions.TELEGRAM || getMessagesController().isTranslationsManualEnabled()) {
            items.add(UItem.asSwitchNoIcon(6, getString(R.string.ShowTranslateButton)).setChecked(controller.isContextTranslateEnabled()));
        }
        if (provider != TranslationOptions.TELEGRAM || getMessagesController().isTranslationsAutoEnabled() && !getMessagesController().premiumFeaturesBlocked()) {
            UItem item = UItem.asSwitchNoIcon(7, getString(R.string.ShowTranslateChatButton)).setChecked(controller.isChatTranslateEnabled());
            if (provider == TranslationOptions.TELEGRAM && !getUserConfig().isPremium()) item.locked();
            items.add(item);
        }
        ArrayList<String> restrictedLanguages = new ArrayList<>();
        for (String code : RestrictedLanguagesSelectActivity.getRestrictedLanguages()) {
            restrictedLanguages.add(TranslateAlert2.capitalFirst(TranslateAlert2.languageName(code)));
        }
        items.add(UItem.asShadow(getString(R.string.TranslateMessagesInfo1)));
        items.add(UItem.asHeader(getString(R.string.TranslateMessage)));
        items.add(UItem.asButton(1, getString(R.string.TranslationProvider), TranslationOptions.NAMES[provider]));
        items.add(UItem.asButton(2, getString(R.string.TranslationLanguage), language.isEmpty() ? getString(R.string.TranslationFollowApp) : Locale.forLanguageTag(language).getDisplayName()));
        items.add(UItem.asButton(3, getString(R.string.DoNotTranslate).replace("Not T", "not t"), TextUtils.join(", ", restrictedLanguages)));
        if (!TranslationLanguages.supports(provider, TranslationOptions.defaultLanguage())) items.add(UItem.asShadow(getString(R.string.TranslationUnsupportedLanguage)));
        items.add(UItem.asButton(4, getString(R.string.TranslationShowOriginal), originalLabels()[AoiConfig.translationShowOriginal.get()]));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(5, R.drawable.msg_ai_translate, getString(R.string.LlmSettings)));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 1) {
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), TranslationOptions.NAMES, getString(R.string.TranslationProvider), TranslationOptions.defaultProvider(), (dialog, which) -> {
                AoiConfig.translationProvider.set(TranslationOptions.IDS.get(which));
                listView.adapter.update(true);
            }));
        } else if (item.id == 2) {
            presentFragment(new AoiTranslationLanguageActivity());
        } else if (item.id == 3) {
            presentFragment(new RestrictedLanguagesSelectActivity());
        } else if (item.id == 4) {
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), originalLabels(), getString(R.string.TranslationShowOriginal), AoiConfig.translationShowOriginal.get(), (dialog, which) -> {
                AoiConfig.translationShowOriginal.set(which);
                listView.adapter.update(true);
            }));
        } else if (item.id == 5) presentFragment(new AoiLlmSettingsActivity());
        else if (item.id == 6 || item.id == 7) {
            TranslateController controller = getMessagesController().getTranslateController();
            if (item.id == 6) {
                controller.setContextTranslateEnabled(!controller.isContextTranslateEnabled());
            } else {
                boolean enabled = !controller.isChatTranslateEnabled();
                if (enabled && TranslationOptions.defaultProvider() == TranslationOptions.TELEGRAM && !getUserConfig().isPremium()) {
                    showDialog(new PremiumFeatureBottomSheet(this, PremiumPreviewFragment.PREMIUM_FEATURE_TRANSLATIONS, false));
                    return;
                }
                controller.setChatTranslateEnabled(enabled);
            }
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateSearchSettings);
            listView.adapter.update(true);
        }
    }

    private String[] originalLabels() {
        return new String[]{getString(R.string.TranslationShowOriginalOff), getString(R.string.TranslationShowOriginalManualOnly), getString(R.string.TranslationShowOriginalOn)};
    }
}
