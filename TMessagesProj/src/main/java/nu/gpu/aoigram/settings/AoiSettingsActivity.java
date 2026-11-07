package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SettingsSearchCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.LauncherIconController;
import org.telegram.ui.LauncherIconController.LauncherIcon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import nu.gpu.aoigram.utils.AppUtil;

public class AoiSettingsActivity extends BaseAoiSettingsActivity {

    private static final int GENERAL_ROW = 1;
    private static final int APPEARANCE_ROW = 2;
    private static final int TRANSLATOR_ROW = 3;
    private static final int CHAT_ROW = 4;
    private static final int OTHER_ROW = 5;
    private static final int ABOUT_ROW = 6;

    private FrameLayout topView;
    private BackupImageView logoView;
    private int logoRes;
    private LauncherIcon launcherIcon;
    private TextView titleView;
    private TextView subtitleView;
    private EmptyTextProgressView searchEmptyView;
    private ActionBarMenuItem searchItem;
    private ArrayList<AoiSettingsSearchItem> searchItems;
    private ArrayList<AoiSettingsSearchItem> searchResults;

    @Override
    protected void onCreateSettingsView(Context context) {
        createTopView(context);
        createSearch(context);
    }

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.AoiSettings);
    }

    @Override
    protected boolean hasCollapsingHeader() {
        return true;
    }

    @Override
    protected boolean shouldShowActionBarBackground() {
        return searchItem.isSearchFieldVisible2() || super.shouldShowActionBarBackground();
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.MAIN_PAGE;
    }

    @Override
    protected String getItemKey(UItem item) {
        return switch (item.id) {
            case GENERAL_ROW -> AoiSettingsDeepLink.GENERAL_PAGE;
            case APPEARANCE_ROW -> AoiSettingsDeepLink.APPEARANCE_PAGE;
            case TRANSLATOR_ROW -> AoiSettingsDeepLink.TRANSLATOR_PAGE;
            case CHAT_ROW -> AoiSettingsDeepLink.CHAT_PAGE;
            case OTHER_ROW -> AoiSettingsDeepLink.OTHER_PAGE;
            case ABOUT_ROW -> AoiSettingsDeepLink.ABOUT_PAGE;
            default -> null;
        };
    }

    private void createTopView(Context context) {
        topView = new FrameLayout(context);

        FrameLayout logoContainer = new FrameLayout(context);
        logoView = new BackupImageView(context);
        logoRes = 0;
        launcherIcon = Arrays.stream(LauncherIcon.values()).filter(LauncherIconController::isEnabled).findFirst().orElse(LauncherIcon.DEFAULT);
        logoContainer.addView(logoView, LayoutHelper.createFrame(90, 90, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 15, 0, 0));
        topView.addView(logoContainer, LayoutHelper.createFrame(120, 120, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 11, 0, 0));

        titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setGravity(Gravity.CENTER);
        titleView.setSingleLine();
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setText(getString(R.string.AppNameAoi));
        topView.addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 16, 126.333f, 16, 0));

        subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setSingleLine();
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        subtitleView.setText(AppUtil.getVersionName());
        topView.addView(subtitleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 156, 0, 0));
    }

    private void createSearch(Context context) {
        searchItems = new ArrayList<>();
        searchResults = new ArrayList<>();

        searchEmptyView = new EmptyTextProgressView(context, null, getResourceProvider());
        searchEmptyView.setText(getString(R.string.SettingsNoResults));
        searchEmptyView.showTextView();

        ActionBarMenu menu = actionBar.createMenu();
        searchItem = menu.addItem(0, R.drawable.outline_header_search, getResourceProvider())
                .setIsSearchField(true)
                .setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
                    @Override
                    public void onSearchExpand() {
                        search("");
                        updateActionBarVisible();
                    }

                    @Override
                    public void onSearchCollapse() {
                        listView.adapter.update(false);
                        updateActionBarVisible();
                    }

                    @Override
                    public void onTextChanged(EditText editText) {
                        search(editText.getText().toString());
                    }
                });
        searchItem.setSearchFieldHint(getString(R.string.Search));
    }

    @Override
    public void onResume() {
        super.onResume();
        searchItems.clear();
        addSearchItems(new AoiGeneralSettingsActivity(), R.drawable.msg_media);
        addSearchItems(new AoiAppearanceSettingsActivity(), R.drawable.msg_theme);
        addSearchItems(new AoiTranslatorSettingsActivity(), R.drawable.msg_translate);
        addSearchItems(new AoiLlmSettingsActivity(), R.drawable.msg_ai_translate);
        addSearchItems(new AoiChatSettingsActivity(), R.drawable.msg_discussion);
        addSearchItems(new AoiPinnedReactionsActivity(), R.drawable.msg_pin);
        addSearchItems(new AoiSpeechToTextActivity(), R.drawable.menu_feature_voice);
        addSearchItems(new AoiOtherSettingsActivity(), R.drawable.msg_fave);
        addSearchItems(new AoiAboutSettingsActivity(), R.drawable.msg_info);
        if (searchItem.isSearchFieldVisible2()) {
            search(searchItem.getSearchField().getText().toString());
        }
    }

    private void addSearchItems(BaseAoiSettingsActivity fragment, int iconResId) {
        String pageTitle = fragment.getActionBarTitle().toString();
        String pageKey = fragment.getSettingsPageKey();
        ArrayList<UItem> items = new ArrayList<>();
        fragment.fillItems(items, null);
        String header = null;
        String section = null;
        for (UItem item : items) {
            if (item.viewType == UniversalAdapter.VIEW_TYPE_HEADER) {
                header = section = TextUtils.equals(item.text, pageTitle) ? null : item.text.toString();
                continue;
            }
            if (ExpandableSettingsCell.isGroup(item)) {
                section = item.text.toString();
                continue;
            }
            if (item.viewType == UniversalAdapter.VIEW_TYPE_SHADOW) {
                header = section = null;
                continue;
            }
            if (!item.instanceOf(ExpandableSettingsCell.Factory.class)) {
                section = header;
            }
            String itemKey = fragment.getItemKey(item);
            if (!TextUtils.isEmpty(itemKey)) {
                searchItems.add(new AoiSettingsSearchItem(searchItems.size() + 1, item.text.toString(), pageTitle, section, iconResId, pageKey, itemKey));
            }
        }
    }

    private void search(String query) {
        searchResults.clear();
        for (AoiSettingsSearchItem item : searchItems) {
            if (TextUtils.isEmpty(query) || item.matches(query)) {
                searchResults.add(item);
            }
        }
        if (listView != null) {
            listView.adapter.update(true);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (searchItem != null && searchItem.isSearchFieldVisible2()) {
            items.add(UItem.asSpace(ActionBar.getCurrentActionBarHeight()));
            if (searchResults.isEmpty()) {
                items.add(UItem.asFullscreenCustom(searchEmptyView, ActionBar.getCurrentActionBarHeight(), true));
            } else {
                String query = searchItem.getSearchField().getText().toString().trim().toLowerCase(Locale.ROOT);
                ArrayList<String> terms = new ArrayList<>();
                for (String term : query.split("\\s+")) {
                    terms.add(term);
                    String transliteratedTerm = LocaleController.getInstance().getTranslitString(term);
                    if (!transliteratedTerm.isEmpty() && !transliteratedTerm.equals(term)) {
                        terms.add(transliteratedTerm);
                    }
                }
                for (AoiSettingsSearchItem result : searchResults) {
                    CharSequence title = query.isEmpty() ? result.searchTitle : AndroidUtilities.highlightText(result.searchTitle, terms, getResourceProvider());
                    items.add(SettingsSearchCell.Factory.of(title, result));
                }
            }
            return;
        }
        items.add(UItem.asCustomShadow(topView, 188));
        items.add(UItem.asButton(GENERAL_ROW, R.drawable.msg_media, getString(R.string.AoiGeneralSettings)));
        items.add(UItem.asButton(APPEARANCE_ROW, R.drawable.msg_theme, getString(R.string.AoiAppearanceSettings)));
        items.add(UItem.asButton(TRANSLATOR_ROW, R.drawable.msg_translate, getString(R.string.AoiTranslatorSettings)));
        items.add(UItem.asButton(CHAT_ROW, R.drawable.msg_discussion, getString(R.string.AoiChatSettings)));
        items.add(UItem.asButton(OTHER_ROW, R.drawable.msg_fave, getString(R.string.AoiOtherSettings)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asButton(ABOUT_ROW, R.drawable.msg_info, getString(R.string.AoiAbout)));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.object instanceof AoiSettingsSearchItem result) {
            presentFragment(AoiSettingsDeepLink.createFragment(result.pageKey, result.itemKey));
            return;
        }
        switch (item.id) {
            case GENERAL_ROW -> presentFragment(new AoiGeneralSettingsActivity());
            case APPEARANCE_ROW -> presentFragment(new AoiAppearanceSettingsActivity());
            case TRANSLATOR_ROW -> presentFragment(new AoiTranslatorSettingsActivity());
            case CHAT_ROW -> presentFragment(new AoiChatSettingsActivity());
            case OTHER_ROW -> presentFragment(new AoiOtherSettingsActivity());
            case ABOUT_ROW -> presentFragment(new AoiAboutSettingsActivity());
        }
    }

    @Override
    protected void updateCustomColors() {
        int res = switch (launcherIcon) {
            case INVERTED -> R.mipmap.icon_aoi_inverted_launcher;
            case SORA -> R.mipmap.icon_aoi_sora_launcher;
            case UMI -> R.mipmap.icon_aoi_umi_launcher;
            case SAKURA -> R.mipmap.icon_aoi_sakura_launcher;
            case YORU -> R.mipmap.icon_aoi_yoru_launcher;
            default -> Theme.isCurrentThemeDark() ? R.mipmap.icon_aoi_inverted_launcher : R.mipmap.ic_launcher;
        };
        if (logoRes != res) {
            logoRes = res;
            logoView.setImageResource(res);
        }
        titleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        subtitleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        searchEmptyView.setTextColor(getThemedColor(Theme.key_emptyListPlaceholder));
        searchItem.updateColor();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (actionBar.isSearchFieldVisible()) {
            if (invoked) {
                actionBar.closeSearchField();
            }
            return false;
        }
        return super.onBackPressed(invoked);
    }
}
