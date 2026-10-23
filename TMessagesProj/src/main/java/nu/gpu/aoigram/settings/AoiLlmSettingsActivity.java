package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.TextViewWithLoading;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.translate.TranslationOptions;
import nu.gpu.aoigram.translate.TranslationService;
import nu.gpu.aoigram.translate.TranslationTask;
import nu.gpu.aoigram.translate.llm.LlmSettings;
import nu.gpu.aoigram.translate.llm.LlmTranslation;
import nu.gpu.aoigram.translate.llm.ModelCatalog;
import nu.gpu.aoigram.utils.ErrorFeedback;

public final class AoiLlmSettingsActivity extends BaseAoiSettingsActivity {

    private ModelDialog modelDialog;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.LlmSettings);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.LLM_PAGE;
    }

    @Override
    protected String getItemKey(UItem item) {
        return switch (item.id) {
            case 1 -> "llmProvider";
            case 2 -> "llmUrl";
            case 3 -> "llmKey";
            case 4 -> "llmModel";
            case 5 -> "systemPrompt";
            case 6 -> "requestBody";
            case 8 -> "userPrompt";
            default -> super.getItemKey(item);
        };
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        LlmSettings.Preset preset = LlmSettings.selectedPreset();
        items.add(UItem.asHeader(getString(R.string.LlmService)));
        items.add(UItem.asButton(1, getString(R.string.LlmProvider), preset == LlmSettings.CUSTOM ? getString(R.string.LlmCustom) : preset.name()));
        if (preset == LlmSettings.CUSTOM) items.add(UItem.asButton(2, getString(R.string.LlmUrl), AoiConfig.llmUrl.get()));
        List<String> keys = LlmSettings.parseKeys(preset.key().get());
        items.add(UItem.asButton(3, getString(R.string.LlmKey), keys.isEmpty() ? getString(R.string.None) : keys.stream().map(AoiLlmSettingsActivity::mask).collect(Collectors.joining(", "))));
        items.add(UItem.asButton(4, getString(R.string.LlmModel), preset.effectiveModel()));
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.LlmPrompts)));
        items.add(UItem.asButton(5, getString(R.string.LlmSystemPrompt), getString(AoiConfig.llmSystemPrompt.get().isEmpty() ? R.string.Default : R.string.LlmCustom)));
        items.add(UItem.asButton(8, getString(R.string.LlmUserPrompt), getString(AoiConfig.llmUserPrompt.get().isEmpty() ? R.string.Default : R.string.LlmCustom)));
        items.add(UItem.asShadow(null));
        items.add(UItem.asHeader(getString(R.string.LlmContext)));
        items.add(asConfigCheck(AoiConfig.llmContext, R.string.LlmUseContext));
        if (AoiConfig.llmContext.get()) {
            items.add(UItem.asIntSlideView(0, 3, AoiConfig.llmContextCount.get(), 20, count -> Integer.toString(count), AoiConfig.llmContextCount::set));
        }
        items.add(UItem.asShadow(getString(R.string.LlmContextInfo)));
        items.add(UItem.asHeader(getString(R.string.LlmAdvanced)));
        items.add(UItem.asButton(6, getString(R.string.LlmRequestBody), getString(new LlmSettings().body == null ? R.string.Default : R.string.LlmCustom)));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        LlmSettings.Preset selected = LlmSettings.selectedPreset();
        if (item.id == 1) {
            String[] labels = new String[LlmSettings.PRESETS.size()];
            for (int i = 0; i < labels.length; i++) {
                LlmSettings.Preset preset = LlmSettings.PRESETS.get(i);
                labels[i] = preset == LlmSettings.CUSTOM ? getString(R.string.LlmCustom) : preset.name();
            }
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), labels, getString(R.string.LlmProvider), LlmSettings.PRESETS.indexOf(selected), (dialog, which) -> {
                AoiConfig.llmProvider.set(LlmSettings.PRESETS.get(which).id());
                listView.adapter.update(true);
            }));
        } else if (item.id == 2) editUrl();
        else if (item.id == 3) new KeysDialog(selected).show();
        else if (item.id == 4) {
            modelDialog = new ModelDialog(selected);
            modelDialog.show();
        } else if (item.id == 5 || item.id == 8) presentFragment(new AoiTranslationPromptsActivity(item.id == 5));
        else if (item.id == 6) {
            if (new LlmSettings().isConfigured()) presentFragment(new AoiLlmRequestBodyActivity());
            else ErrorFeedback.show(getString(R.string.LlmConfigurationRequired), BulletinFactory.of(this), this::showDialog, true);
        } else super.onItemClick(item, view, position, x, y);
    }

    private void editUrl() {
        EditTextBoldCursor input = input(getContext(), AoiConfig.llmUrl.get(), "https://…/v1");
        LinearLayout layout = new LinearLayout(getContext());
        layout.addView(input, LayoutHelper.createLinear(-1, 48, 24, 0, 24, 0));
        AlertDialog dialog = new AlertDialog.Builder(getContext(), getResourceProvider()).setTitle(getString(R.string.LlmUrl)).setView(layout)
            .setNegativeButton(getString(R.string.Cancel), null).setPositiveButton(getString(R.string.Save), (d, which) -> {
                AoiConfig.llmUrl.set(input.getText().toString().trim());
                listView.adapter.update(true);
            }).create();
        Runnable showKeyboard = () -> {
            input.requestFocus();
            AndroidUtilities.showKeyboard(input);
        };
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(showKeyboard, 200));
        showDialog(dialog, d -> AndroidUtilities.cancelRunOnUIThread(showKeyboard));
    }

    private CharSequence keyHelp(LlmSettings.Preset preset) {
        String url = preset.keyUrl();
        if (url == null) return getString(R.string.LlmCustomKeyHelp);
        return AndroidUtilities.replaceSingleTag("**" + getString(R.string.LlmHowToObtain) + "**", -1,
            AndroidUtilities.REPLACING_TAG_TYPE_LINKBOLD, () -> Browser.openUrl(getParentActivity(), url), getResourceProvider());
    }

    static String mask(String key) {
        return "••••" + key.substring(Math.max(0, key.length() - 4));
    }

    private EditTextBoldCursor input(Context context, String value, String hint) {
        EditTextBoldCursor input = new EditTextBoldCursor(context);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
        input.setHintTextColor(getThemedColor(Theme.key_dialogTextHint));
        input.setCursorColor(getThemedColor(Theme.key_dialogTextBlack));
        input.setBackground(Theme.createEditTextDrawable(context, true));
        input.setText(value);
        input.setHint(hint);
        input.setSelectAllOnFocus(true);
        return input;
    }

    @Override
    public void onFragmentDestroy() {
        if (modelDialog != null) modelDialog.cancelRequests();
        super.onFragmentDestroy();
    }

    private final class KeysDialog {
        final LlmSettings.Preset preset;
        final List<String> saved;
        ArrayList<String> keys;
        final EditTextBoldCursor input;
        final ImageView confirm;
        final LinearLayout rows;
        final AlertDialog dialog;
        int editing = -1;

        KeysDialog(LlmSettings.Preset preset) {
            this.preset = preset;
            Context context = getContext();
            saved = LlmSettings.parseKeys(preset.key().get());
            keys = new ArrayList<>(saved);
            input = input(context, "", getString(R.string.LlmKey));
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
            dialog = new AlertDialog.Builder(context, getResourceProvider()) {
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
            }.setTitle(getString(R.string.LlmKey)).setMessage(keyHelp(preset)).setView(layout)
                .setNegativeButton(getString(R.string.Cancel), (d, which) -> d.dismiss())
                .setPositiveButton(getString(R.string.Save), (d, which) -> save()).create();
            dialog.setDismissDialogByButtons(false);
            bind();
        }

        void show() {
            Runnable showKeyboard = () -> {
                input.requestFocus();
                AndroidUtilities.showKeyboard(input);
            };
            if (keys.isEmpty()) dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(showKeyboard, 200));
            showDialog(dialog, d -> AndroidUtilities.cancelRunOnUIThread(showKeyboard));
        }

        void save() {
            preset.key().set(String.join(",", pending()));
            listView.adapter.update(true);
            dialog.dismiss();
        }

        void confirmDiscard() {
            new AlertDialog.Builder(getContext(), getResourceProvider())
                .setTitle(getString(R.string.UnsavedChanges))
                .setMessage(getString(R.string.LlmKeysUnsaved))
                .setPositiveButton(getString(R.string.Save), (d, which) -> save())
                .setNegativeButton(getString(R.string.PassportDiscard), (d, which) -> dialog.dismiss())
                .show();
        }

        void bind() {
            confirm.setImageResource(editing < 0 ? R.drawable.msg_add : R.drawable.ic_ab_done);
            rows.removeAllViews();
            for (int i = 0; i < keys.size(); i++) {
                int index = i;
                LinearLayout row = new LinearLayout(getContext());
                row.setGravity(Gravity.CENTER_VERTICAL);
                TextView text = new TextView(getContext());
                text.setText(mask(keys.get(i)));
                text.setTextSize(16);
                text.setTextColor(getThemedColor(i == editing ? Theme.key_dialogTextBlue : Theme.key_dialogTextBlack));
                row.addView(text, LayoutHelper.createLinear(0, -2, 1f));
                row.addView(button(R.drawable.msg_edit, () -> edit(index)), LayoutHelper.createLinear(48, 48));
                row.addView(button(R.drawable.msg_delete, () -> delete(index)), LayoutHelper.createLinear(48, 48));
                rows.addView(row, LayoutHelper.createLinear(-1, 48, 24, 0, 12, 0));
            }
        }

        ImageView button(int icon, Runnable action) {
            ImageView button = new ImageView(getContext());
            button.setImageResource(icon);
            button.setScaleType(ImageView.ScaleType.CENTER);
            button.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_dialogIcon), PorterDuff.Mode.MULTIPLY));
            button.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector), Theme.RIPPLE_MASK_CIRCLE_20DP));
            button.setOnClickListener(v -> action.run());
            return button;
        }

        ArrayList<String> pending() {
            ArrayList<String> result = new ArrayList<>(keys);
            List<String> values = LlmSettings.parseKeys(input.getText().toString());
            if (editing >= 0) result.remove(editing);
            result.addAll(editing >= 0 ? editing : result.size(), values);
            return result;
        }

        void commit() {
            keys = pending();
            editing = -1;
            input.setText("");
            bind();
        }

        void edit(int index) {
            editing = index;
            input.setText(keys.get(index));
            input.requestFocus();
            input.setSelection(input.length());
            AndroidUtilities.showKeyboard(input);
            bind();
        }

        void delete(int index) {
            keys.remove(index);
            if (editing == index) {
                editing = -1;
                input.setText("");
            } else if (editing > index) editing--;
            bind();
        }
    }

    private final class ModelDialog {
        final LlmSettings.Preset preset;
        final EditTextBoldCursor input;
        UniversalRecyclerView models;
        final TextView status;
        final AlertDialog dialog;
        final LinkedHashSet<String> candidates = new LinkedHashSet<>();
        final HashSet<String> enabledModels = new HashSet<>();
        TranslationTask fetch;
        TranslationTask test;
        String query = "";

        ModelDialog(LlmSettings.Preset preset) {
            this.preset = preset;
            Context context = getContext();
            candidates.add(preset.defaultModel());
            candidates.remove("");
            input = input(context, preset.effectiveModel(), getString(R.string.LlmModelHint));
            status = new TextView(context);
            status.setText(getString(R.string.LlmLoadingModels));
            status.setTextSize(14);
            status.setTextColor(getThemedColor(Theme.key_dialogTextGray2));
            status.setGravity(Gravity.CENTER);
            status.setTextIsSelectable(true);
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.addView(input, LayoutHelper.createLinear(-1, 48, 24, 0, 24, 8));
            models = new UniversalRecyclerView(context, currentAccount, classGuid, true, (items, adapter) -> {
                ArrayList<String> keywords = new ArrayList<>(Arrays.asList(query.split("\\s+")));
                for (String candidate : candidates) {
                    String normalized = candidate.toLowerCase(Locale.ROOT);
                    if (!keywords.stream().allMatch(normalized::contains)) continue;
                    boolean enabled = enabledModels.contains(normalized) || ModelCatalog.isTextGeneration(candidate);
                    CharSequence title = enabled && !query.isEmpty()
                            ? AndroidUtilities.highlightText(candidate, keywords, getResourceProvider()) : candidate;
                    UItem item = UItem.asRadio(2, title).setChecked(candidate.equals(input.getText().toString())).setEnabled(enabled);
                    item.object = candidate;
                    items.add(item);
                }
            }, (item, view, position, x, y) -> {
                if (!item.enabled) return;
                input.setText((String) item.object);
                input.setSelection(input.length());
                query = "";
                models.adapter.update(false);
                AndroidUtilities.hideKeyboard(input);
            }, (item, view, position, x, y) -> {
                if (item.enabled) return false;
                enabledModels.add(((String) item.object).toLowerCase(Locale.ROOT));
                models.adapter.update(false);
                return true;
            }, getResourceProvider());
            models.adapter.setApplyBackground(false);
            models.setVisibility(View.GONE);
            FrameLayout content = new FrameLayout(context);
            content.addView(models, LayoutHelper.createFrame(-1, -1));
            content.addView(status, LayoutHelper.createFrame(-2, -2, Gravity.CENTER, 24, 0, 24, 0));
            layout.addView(content, LayoutHelper.createLinear(-1, 0, 1f));
            int height = Math.min(600, (int) (AndroidUtilities.displaySize.y / AndroidUtilities.density * 0.5f));
            dialog = new AlertDialog.Builder(context, getResourceProvider()).setTitle(getString(R.string.LlmModel)).setView(layout, height)
                    .setNegativeButton(getString(R.string.Cancel), (d, which) -> d.dismiss())
                    .setPositiveButton(getString(R.string.Save), (d, which) -> {
                        preset.model().set(input.getText().toString().trim());
                        listView.adapter.update(true);
                        d.dismiss();
                    })
                    .setNeutralButton(getString(R.string.LlmTest), (d, which) -> test())
                    .create();
            dialog.setDismissDialogByButtons(false);
            input.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    query = s.toString().trim().toLowerCase(Locale.ROOT);
                    models.adapter.update(false);
                }
                public void afterTextChanged(Editable text) {}
            });
        }

        void show() {
            showDialog(dialog, d -> cancelRequests());
            fetch = LlmTranslation.models(new LlmSettings(preset, null), (result, error) -> {
                if (error != null) {
                    status.setGravity(Gravity.START);
                    status.setLayoutParams(LayoutHelper.createFrame(-1, -2, Gravity.TOP, 24, 0, 24, 0));
                    status.setText(error);
                    return;
                }
                candidates.clear();
                String defaultModel = preset.defaultModel();
                if (!defaultModel.isEmpty()) candidates.add(defaultModel);
                candidates.addAll(result);
                status.setVisibility(View.GONE);
                models.setVisibility(View.VISIBLE);
                models.adapter.update(false);
            });
        }

        void test() {
            if (test != null) test.cancel();
            TextViewWithLoading button = (TextViewWithLoading) dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            button.setLoading(true, true);
            LlmSettings settings = new LlmSettings(preset, input.getText().toString());
            TLRPC.TL_textWithEntities source = new TLRPC.TL_textWithEntities();
            source.text = "Hello, world!";
            long start = System.currentTimeMillis();
            TranslationTask task = test = new TranslationTask();
            TranslationService.ResultCallback<TLRPC.TL_textWithEntities> done = (result, error) -> {
                task.cancel();
                button.setLoading(false, true);
                BulletinFactory bulletins = BulletinFactory.of(Bulletin.BulletinWindow.make(getContext()), getResourceProvider());
                if (error != null) {
                    ErrorFeedback.show(error, bulletins, AlertDialog::show, true);
                    return;
                }
                long elapsed = System.currentTimeMillis() - start;
                bulletins.createSimpleBulletin(R.raw.contact_check, result.text, String.format(Locale.ROOT, "%s · %d ms", settings.model, elapsed)).show();
            };
            TranslationService.translate(task, currentAccount, source, new TranslationOptions(TranslationOptions.LLM, TranslationOptions.defaultLanguage(), settings), null, done);
            task.post(() -> done.done(null, getString(R.string.LlmTestTimeout)), 10000);
        }

        void cancelRequests() {
            if (fetch != null) fetch.cancel();
            if (test != null) test.cancel();
        }
    }
}
