package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatPluralStringComma;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.config.StringConfig;
import nu.gpu.aoigram.translate.llm.LlmSettings;
import nu.gpu.aoigram.utils.ErrorFeedback;

public final class AoiTranslationPromptsActivity extends BaseAoiSettingsActivity {

    private final boolean systemPrompt;
    private final StringConfig config;
    private final String defaultPrompt;
    private String initialText;
    private EditTextBoldCursor editor;
    private TextInfoPrivacyCell counter;
    private TextInfoPrivacyCell placeholders;
    private int topInset;
    private int bottomInset;

    public AoiTranslationPromptsActivity(boolean systemPrompt) {
        this.systemPrompt = systemPrompt;
        config = systemPrompt ? AoiConfig.llmSystemPrompt : AoiConfig.llmUserPrompt;
        defaultPrompt = systemPrompt ? LlmSettings.SYSTEM_PROMPT : LlmSettings.USER_PROMPT;
    }

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(systemPrompt ? R.string.LlmSystemPrompt : R.string.LlmUserPrompt);
    }

    @Override
    protected String getSettingsPageKey() {
        return systemPrompt ? AoiSettingsDeepLink.PROMPTS_PAGE : AoiSettingsDeepLink.USER_PROMPT_PAGE;
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        topInset = listView.getPaddingTop();
        if (!systemPrompt) {
            placeholders = new TextInfoPrivacyCell(context, getResourceProvider());
            placeholders.setText(AndroidUtilities.replaceMultipleTags(getString(R.string.LlmPlaceholdersInfo), () -> insertPlaceholder("@text"), () -> insertPlaceholder("@toLang")));
            placeholders.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
            placeholders.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateListPadding());
            FrameLayout.LayoutParams params = LayoutHelper.createFrame(-1, -2, Gravity.TOP);
            params.topMargin = topInset;
            contentView.addView(placeholders, params);
        }
        return view;
    }

    @Override
    protected void onCreateSettingsView(Context context) {
        initialText = config.get().isEmpty() ? defaultPrompt : config.get();
        editor = new EditTextBoldCursor(context);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTextSize(16);
        editor.setMinLines(4);
        editor.setPadding(dp(22), dp(12), dp(22), dp(12));
        editor.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        editor.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        editor.setCursorColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        editor.setText(initialText);
        counter = new TextInfoPrivacyCell(context, getResourceProvider());
        counter.setText(formatPluralStringComma("PromptCharacters", initialText.length()));
        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable text) {
                counter.setText(formatPluralStringComma("PromptCharacters", text.length()));
            }
        });
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(1, R.drawable.outline_header_reset).setContentDescription(getString(R.string.LlmRestoreDefault));
        menu.addItemWithWidth(2, R.drawable.ic_ab_done, dp(56), getString(R.string.Done));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == 1) editor.setText(defaultPrompt);
                else if (id == 2) save();
                else if (id == -1 && onBackPressed(true)) close();
            }
        });
    }

    protected @NonNull WindowInsetsCompat onApplyWindowInsets(@NonNull View view, @NonNull WindowInsetsCompat insets) {
        super.onApplyWindowInsets(view, insets);
        topInset = listView.getPaddingTop();
        bottomInset = Math.max(AndroidUtilities.getDefaultWindowInsets(insets, false).bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
        if (placeholders != null) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) placeholders.getLayoutParams();
            params.topMargin = topInset;
            placeholders.setLayoutParams(params);
        }
        updateListPadding();
        return WindowInsetsCompat.CONSUMED;
    }

    private void updateListPadding() {
        listView.setPadding(0, topInset + (placeholders == null ? 0 : placeholders.getHeight()), 0, bottomInset);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (editor != null) {
            items.add(UItem.asCustom(editor));
            items.add(UItem.asCustomShadow(counter));
        }
    }

    private void insertPlaceholder(String placeholder) {
        int selectionStart = Math.max(0, editor.getSelectionStart());
        int selectionEnd = Math.max(0, editor.getSelectionEnd());
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        editor.getText().replace(start, end, placeholder);
        editor.setSelection(start + placeholder.length());
        editor.requestFocus();
        AndroidUtilities.showKeyboard(editor);
    }

    private void save() {
        String text = editor.getText().toString();
        if (config == AoiConfig.llmUserPrompt && !text.contains("@text")) {
            ErrorFeedback.vibrate(editor);
            return;
        }
        config.set(text.equals(defaultPrompt) ? "" : text);
        close();
    }

    private void close() {
        AndroidUtilities.hideKeyboard(editor);
        finishFragment();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (editor == null || initialText.contentEquals(editor.getText())) return true;
        if (invoked) {
            showDialog(new AlertDialog.Builder(getContext(), getResourceProvider())
                    .setTitle(getString(R.string.UnsavedChanges))
                    .setMessage(getString(R.string.LlmPromptUnsaved))
                    .setPositiveButton(getString(R.string.ApplyTheme), (dialog, which) -> save())
                    .setNegativeButton(getString(R.string.PassportDiscard), (dialog, which) -> close())
                    .create());
        }
        return false;
    }

    @Override
    public boolean canBeginSlide() {
        return onBackPressed(true);
    }

    @Override
    protected void updateCustomColors() {
        if (editor != null) {
            editor.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            editor.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            editor.setCursorColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        }
        if (placeholders != null) placeholders.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
    }
}
