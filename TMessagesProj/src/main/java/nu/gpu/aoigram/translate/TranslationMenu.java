package nu.gpu.aoigram.translate;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow.ActionBarPopupWindowLayout;
import org.telegram.ui.ActionBar.ActionBarPopupWindow.GapView;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;

import java.util.Locale;

import nu.gpu.aoigram.translate.llm.LlmSettings;

public final class TranslationMenu {

    @SuppressWarnings("DataFlowIssue")
    public static void bind(ChatManualTranslation session, BaseFragment fragment, ActionBarPopupWindowLayout popup, ActionBarMenuSubItem item, MessageObject selected, MessageObject.GroupedMessages group, BlurredBackgroundDrawableViewFactory backgroundFactory, Runnable close) {
        MessageObject caption = group == null ? null : group.findCaptionMessageObject();
        MessageObject message = caption == null ? selected : caption;
        var controller = MessagesController.getInstance(fragment.getCurrentAccount()).getTranslateController();
        if (!message.isOutOwner() && controller.isTranslatingDialog(message.getDialogId()) || !message.translated && !controller.isContextTranslateEnabled()) {
            item.setVisibility(View.GONE);
            return;
        }
        item.setText(getString(message.translated ? R.string.TranslationShowOriginal : R.string.TranslateMessage));
        item.setOnClickListener(view -> {
            close.run();
            if (message.translated) {
                session.showOriginal(message);
            } else {
                session.translateDefault(fragment, message);
            }
        });
        if (message.translated) return;
        item.setOnLongClickListener(view -> {
            int index = addPage(popup, languagePage(session, fragment, popup, message, backgroundFactory, close));
            item.setOnLongClickListener(v -> {
                popup.getSwipeBack().openForeground(index);
                return true;
            });
            popup.getSwipeBack().openForeground(index);
            return true;
        });
        setupRightButton(item, fragment.getResourceProvider(), R.string.TranslationProvider);
        item.getRightIcon().setOnClickListener(view -> {
            ActionBarPopupWindowLayout menu = page(fragment, popup, backgroundFactory);
            ActionBarMenuSubItem providerItem = action(fragment, menu, getString(R.string.TranslationProvider), null);
            providerItem.setRightIcon(R.drawable.msg_arrowright);
            providerItem.setOnClickListener(v -> {
                ActionBarPopupWindowLayout providers = page(fragment, menu, backgroundFactory);
                for (int i = 0; i < TranslationOptions.NAMES.length; i++) {
                    if (i == TranslationOptions.LLM) continue;
                    int provider = i;
                    action(fragment, providers, TranslationOptions.NAMES[i], () -> {
                        close.run();
                        session.translate(fragment, message, new TranslationOptions(provider, TranslationOptions.defaultLanguage()));
                    });
                }
                int index = addPage(menu, providers);
                providerItem.setOnClickListener(v1 -> menu.getSwipeBack().openForeground(index));
                menu.getSwipeBack().openForeground(index);
            });
            ActionBarMenuSubItem languageItem = action(fragment, menu, getString(R.string.TranslationLanguage), null);
            languageItem.setRightIcon(R.drawable.msg_arrowright);
            languageItem.setOnClickListener(v -> {
                int index = addPage(menu, languagePage(session, fragment, menu, message, backgroundFactory, close));
                languageItem.setOnClickListener(v1 -> menu.getSwipeBack().openForeground(index));
                menu.getSwipeBack().openForeground(index);
            });
            if (new LlmSettings().isConfigured()) {
                action(fragment, menu, getString(R.string.TranslateWithAI), () -> {
                    close.run();
                    session.translate(fragment, message, new TranslationOptions(TranslationOptions.LLM, TranslationOptions.defaultLanguage()));
                });
            }
            int index = addPage(popup, menu);
            item.getRightIcon().setOnClickListener(v -> popup.getSwipeBack().openForeground(index));
            popup.getSwipeBack().openForeground(index);
        });
    }

    static void setupRightButton(ActionBarMenuSubItem item, Theme.ResourcesProvider resourcesProvider, int description) {
        item.setRightIcon(R.drawable.msg_arrowright);
        item.setPadding(dp(LocaleController.isRTL ? 0 : 18), 0, dp(LocaleController.isRTL ? 18 : 0), 0);
        item.getRightIcon().getLayoutParams().width = dp(40);
        int selector = Theme.getColor(Theme.key_dialogButtonSelector, resourcesProvider);
        item.getRightIcon().setBackground(LocaleController.isRTL
                ? Theme.createRadSelectorDrawable(selector, 0, 6, 6, 0)
                : Theme.createRadSelectorDrawable(selector, 6, 0, 0, 6));
        item.getRightIcon().setContentDescription(getString(description));
    }

    private static ActionBarPopupWindowLayout languagePage(ChatManualTranslation session, BaseFragment fragment, ActionBarPopupWindowLayout parent, MessageObject message, BlurredBackgroundDrawableViewFactory backgroundFactory, Runnable close) {
        ActionBarPopupWindowLayout languages = page(fragment, parent, backgroundFactory);
        int provider = TranslationOptions.defaultProvider();
        String defaultLanguage = TranslationLanguages.map(provider, TranslationOptions.defaultLanguage());
        Locale locale = Locale.forLanguageTag(defaultLanguage);
        action(fragment, languages, LocaleController.formatString(R.string.TranslationDefault, locale.getDisplayName(locale)), () -> {
            close.run();
            session.translateDefault(fragment, message);
        });
        for (TranslationLanguages.Language language : TranslationLanguages.list(provider)) {
            if (language.code().equals(defaultLanguage)) continue;
            action(fragment, languages, TranslationLanguages.label(language.nativeName(), language.pinned()), () -> {
                close.run();
                session.translate(fragment, message, TranslationOptions.defaults(language.code()));
            });
        }
        return languages;
    }

    @SuppressWarnings("DataFlowIssue")
    private static int addPage(ActionBarPopupWindowLayout parent, ActionBarPopupWindowLayout page) {
        int index = parent.addViewToSwipeBack(page);
        page.getSwipeBack().setOnWidthUpdateListener(width -> parent.getSwipeBack().setNewForegroundWidth(index, width));
        page.getSwipeBack().setOnHeightUpdateListener(height -> parent.getSwipeBack().setNewForegroundHeight(index, height, false));
        return index;
    }

    @SuppressWarnings("DataFlowIssue")
    private static ActionBarPopupWindowLayout page(BaseFragment fragment, ActionBarPopupWindowLayout parent, BlurredBackgroundDrawableViewFactory backgroundFactory) {
        ActionBarPopupWindowLayout page = new ActionBarPopupWindowLayout(fragment.getContext(), 0, fragment.getResourceProvider(), ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK);
        PopupSwipeBackLayout swipeBack = parent.getSwipeBack();
        swipeBack.setForegroundBackground(backgroundFactory.create(swipeBack, true).setColorProvider(BlurredBackgroundProviderImpl.messageMenuBackground(fragment.getResourceProvider())));
        page.setFitItems(true);
        page.getSwipeBack().addOnSwipeBackProgressListener((layout, toProgress, progress) -> parent.getSwipeBack().setSwipeBackDisallowed(progress > 0));
        ActionBarMenuSubItem back = action(fragment, page, getString(R.string.Back), () -> parent.getSwipeBack().closeForeground());
        back.setTextAndIcon(getString(R.string.Back), R.drawable.msg_arrow_back);
        GapView gap = new GapView(fragment.getContext(), fragment.getResourceProvider());
        gap.setColor(Theme.multAlpha(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, fragment.getResourceProvider()), 0.06f));
        page.addView(gap, LayoutHelper.createLinear(-1, 8));
        return page;
    }

    private static ActionBarMenuSubItem action(BaseFragment fragment, ActionBarPopupWindowLayout page, CharSequence title, Runnable click) {
        ActionBarMenuSubItem item = new ActionBarMenuSubItem(fragment.getContext(), false, false, fragment.getResourceProvider());
        item.setMinimumWidth(dp(200));
        item.setTextAndIcon(title, 0);
        if (click != null) {
            item.setOnClickListener(view -> click.run());
        }
        page.addView(item);
        return item;
    }
}
