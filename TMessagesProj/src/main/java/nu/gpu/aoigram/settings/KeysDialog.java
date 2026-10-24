package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import nu.gpu.aoigram.config.StringConfig;
import nu.gpu.aoigram.translate.llm.LlmSettings;

final class KeysDialog {

    private final BaseAoiSettingsActivity fragment;
    private final StringConfig config;
    private final List<String> saved;
    private ArrayList<String> keys;
    private final EditTextBoldCursor input;
    private final ImageView confirm;
    private final LinearLayout rows;
    private final AlertDialog dialog;
    private int editing = -1;

    KeysDialog(BaseAoiSettingsActivity fragment, CharSequence title, StringConfig config, String keyUrl) {
        this.fragment = fragment;
        this.config = config;
        Context context = fragment.getContext();
        saved = LlmSettings.parseKeys(config.get());
        keys = new ArrayList<>(saved);
        input = fragment.input(context, "", title.toString());
        input.setPadding(input.getPaddingLeft(), input.getPaddingTop(), dp(40), input.getPaddingBottom());
        input.wrapCanvasToFixClipping = false;
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, actionId, event) -> {
            commit();
            return true;
        });
        confirm = button(R.drawable.msg_add, this::commit);
        FrameLayout field = new FrameLayout(context);
        field.addView(input, LayoutHelper.createFrame(-1, 48, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));
        field.addView(confirm, LayoutHelper.createFrame(48, 48, Gravity.END | Gravity.CENTER_VERTICAL));
        rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(field, LayoutHelper.createLinear(-1, 48, 24, 0, 12, 0));
        layout.addView(rows, LayoutHelper.createLinear(-1, -2, 0, 8, 0, 0));
        CharSequence help = keyUrl == null
                ? getString(R.string.LlmCustomKeyHelp)
                : AndroidUtilities.replaceSingleTag("**" + getString(R.string.LlmHowToObtain) + "**", -1, AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD, () -> Browser.openUrl(fragment.getParentActivity(), keyUrl), fragment.getResourceProvider());
        dialog = new AlertDialog.Builder(context, fragment.getResourceProvider()) {
            @Override
            protected AlertDialog createAlertDialog(Context context, int progressViewStyle, Theme.ResourcesProvider resourcesProvider) {
                return new AlertDialog(context, progressViewStyle, resourcesProvider) {
                    @Override
                    public void cancel() {
                        if (pending().equals(saved)) super.cancel();
                        else confirmDiscard();
                    }
                };
            }
        }.setTitle(title).setMessage(help).setView(layout)
                .setNegativeButton(getString(R.string.Cancel), (d, which) -> d.dismiss())
                .setPositiveButton(getString(R.string.Save), (d, which) -> save()).create();
        dialog.setDismissDialogByButtons(false);
        bind();
    }

    static String mask(String key) {
        return "••••" + key.substring(Math.max(0, key.length() - 4));
    }

    static String summary(StringConfig config) {
        List<String> keys = LlmSettings.parseKeys(config.get());
        return keys.isEmpty() ? getString(R.string.None) : keys.stream().map(KeysDialog::mask).collect(Collectors.joining(", "));
    }

    void show() {
        Runnable showKeyboard = () -> {
            input.requestFocus();
            AndroidUtilities.showKeyboard(input);
        };
        if (keys.isEmpty()) dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(showKeyboard, 200));
        fragment.showDialog(dialog, d -> AndroidUtilities.cancelRunOnUIThread(showKeyboard));
    }

    private void save() {
        config.set(String.join(",", pending()));
        fragment.listView.adapter.update(true);
        dialog.dismiss();
    }

    private void confirmDiscard() {
        new AlertDialog.Builder(fragment.getContext(), fragment.getResourceProvider())
                .setTitle(getString(R.string.UnsavedChanges))
                .setMessage(getString(R.string.LlmKeysUnsaved))
                .setPositiveButton(getString(R.string.Save), (d, which) -> save())
                .setNegativeButton(getString(R.string.PassportDiscard), (d, which) -> dialog.dismiss())
                .show();
    }

    private void bind() {
        confirm.setImageResource(editing < 0 ? R.drawable.msg_add : R.drawable.ic_ab_done);
        rows.removeAllViews();
        for (int i = 0; i < keys.size(); i++) {
            int index = i;
            LinearLayout row = new LinearLayout(fragment.getContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            TextView text = new TextView(fragment.getContext());
            text.setText(mask(keys.get(i)));
            text.setTextSize(16);
            text.setTextColor(fragment.getThemedColor(i == editing ? Theme.key_dialogTextBlue : Theme.key_dialogTextBlack));
            row.addView(text, LayoutHelper.createLinear(0, -2, 1f));
            row.addView(button(R.drawable.msg_edit, () -> edit(index)), LayoutHelper.createLinear(48, 48));
            row.addView(button(R.drawable.msg_delete, () -> delete(index)), LayoutHelper.createLinear(48, 48));
            rows.addView(row, LayoutHelper.createLinear(-1, 48, 24, 0, 12, 0));
        }
    }

    private ImageView button(int icon, Runnable action) {
        ImageView button = new ImageView(fragment.getContext());
        button.setImageResource(icon);
        button.setScaleType(ImageView.ScaleType.CENTER);
        button.setColorFilter(new PorterDuffColorFilter(fragment.getThemedColor(Theme.key_dialogIcon), PorterDuff.Mode.MULTIPLY));
        button.setBackground(Theme.createSelectorDrawable(fragment.getThemedColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_20DP));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private ArrayList<String> pending() {
        ArrayList<String> result = new ArrayList<>(keys);
        List<String> values = LlmSettings.parseKeys(input.getText().toString());
        if (editing >= 0) result.remove(editing);
        result.addAll(editing >= 0 ? editing : result.size(), values);
        return result;
    }

    private void commit() {
        keys = pending();
        editing = -1;
        input.setText("");
        bind();
    }

    private void edit(int index) {
        editing = index;
        input.setText(keys.get(index));
        input.requestFocus();
        input.setSelection(input.length());
        AndroidUtilities.showKeyboard(input);
        bind();
    }

    private void delete(int index) {
        keys.remove(index);
        if (editing == index) {
            editing = -1;
            input.setText("");
        } else if (editing > index) editing--;
        bind();
    }
}
