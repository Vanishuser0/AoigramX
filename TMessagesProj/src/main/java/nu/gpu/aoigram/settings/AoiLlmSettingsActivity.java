package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
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
import java.util.Locale;

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
        items.add(UItem.asButton(3, getString(R.string.LlmKey), KeysDialog.summary(preset.key())));
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
        } else if (item.id == 2) editString(getString(R.string.LlmUrl), AoiConfig.llmUrl, "https://…/v1");
        else if (item.id == 3) new KeysDialog(this, getString(R.string.LlmKey), selected.key(), selected.keyUrl()).show();
        else if (item.id == 4) {
            modelDialog = new ModelDialog(selected);
            modelDialog.show();
        } else if (item.id == 5 || item.id == 8) presentFragment(item.id == 5 ? AoiPromptActivity.systemPrompt() : AoiPromptActivity.userPrompt());
        else if (item.id == 6) {
            if (new LlmSettings().isConfigured()) presentFragment(new AoiLlmRequestBodyActivity());
            else ErrorFeedback.show(getString(R.string.LlmConfigurationRequired), BulletinFactory.of(this), this::showDialog, true);
        } else super.onItemClick(item, view, position, x, y);
    }

    @Override
    public void onFragmentDestroy() {
        if (modelDialog != null) modelDialog.cancelRequests();
        super.onFragmentDestroy();
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
