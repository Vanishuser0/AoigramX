package nu.gpu.aoigram.translate;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow.ActionBarPopupWindowLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;

import java.util.Locale;
import java.util.function.Consumer;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.translate.llm.LlmSettings;

public final class InputTranslationMenu {

    public static void add(ItemOptions menu, View anchor, Theme.ResourcesProvider resourcesProvider, Consumer<TranslationOptions> translate) {
        Rect visible = new Rect();
        anchor.getWindowVisibleDisplayFrame(visible);
        menu.setMaxHeight(Math.max(dp(96), visible.height() - dp(32)));
        int height = Math.clamp(visible.height() / 2 - dp(56), dp(48), dp(280));
        menu.addGapIf(menu.getItemsCount() > 0);
        add(menu, resourcesProvider, R.string.InputTranslate, TranslationOptions.defaultProvider(), height, translate);
        if (new LlmSettings().isConfigured()) {
            add(menu, resourcesProvider, R.string.InputTranslateWithAI, TranslationOptions.LLM, height, translate);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private static void add(ItemOptions menu, Theme.ResourcesProvider resourcesProvider, int title, int provider, int height, Consumer<TranslationOptions> translate) {
        String language = AoiConfig.inputTranslationLanguage.get();
        int icon = provider == TranslationOptions.LLM ? R.drawable.msg_ai_translate : R.drawable.msg_translate;
        String text = LocaleController.formatString(title, language.toUpperCase(Locale.ROOT));
        menu.add(icon, text, () -> translate.accept(new TranslationOptions(provider, language)));
        ActionBarMenuSubItem item = menu.getLast();
        TranslationMenu.setupRightButton(item, resourcesProvider, R.string.TranslationLanguage);
        FrameLayout.LayoutParams textParams = (FrameLayout.LayoutParams) item.getTextView().getLayoutParams();
        textParams.leftMargin = LocaleController.isRTL ? dp(40) : 0;
        textParams.rightMargin = LocaleController.isRTL ? 0 : dp(40);

        ActionBarPopupWindowLayout popup = (ActionBarPopupWindowLayout) menu.getLayout();
        Runnable open = new Runnable() {
            private int index = -1;

            @Override
            public void run() {
                if (index < 0) {
                    index = addLanguagePage(popup, resourcesProvider, provider, language, height, translate);
                    menu.reposition();
                    AndroidUtilities.doOnPreDraw(popup, () -> popup.getSwipeBack().openForeground(index));
                } else {
                    popup.getSwipeBack().openForeground(index);
                }
            }
        };
        item.getRightIcon().setOnClickListener(view -> open.run());
        item.setOnLongClickListener(view -> {
            open.run();
            return true;
        });
    }

    @SuppressWarnings("DataFlowIssue")
    private static int addLanguagePage(ActionBarPopupWindowLayout popup, Theme.ResourcesProvider resourcesProvider,
                                       int provider, String language, int height, Consumer<TranslationOptions> translate) {
        ItemOptions page = ItemOptions.swipeback(popup, resourcesProvider).setDismissWithButtons(false);
        page.add(R.drawable.msg_arrow_back, getString(R.string.Back), () -> popup.getSwipeBack().closeForeground());
        page.getLast().updateSelectorBackground(true, false, 12);
        page.setGapBackgroundColor(Theme.multAlpha(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider), 0.06f));
        page.addGap();
        ItemOptions languages = ItemOptions.swipeback(popup, resourcesProvider).setDismissWithButtons(false);
        for (TranslationLanguages.Language target : TranslationLanguages.list(provider)) {
            languages.addChecked(target.code().equalsIgnoreCase(TranslationLanguages.map(provider, language)), target.nativeName(), () -> {
                AoiConfig.inputTranslationLanguage.set(target.code());
                translate.accept(new TranslationOptions(provider, target.code()));
            });
        }
        ScrollView scroll = new ScrollView(popup.getContext());
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(languages.getLinearLayout());
        page.addView(scroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, height / AndroidUtilities.density));
        page.getLinearLayout().setMinimumWidth(dp(220));
        return popup.addViewToSwipeBack(page.getLinearLayout());
    }
}
