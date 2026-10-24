package nu.gpu.aoigram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.transcription.Transcription;

public final class AoiSpeechToTextActivity extends BaseAoiSettingsActivity {

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.SpeechToText);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.SPEECH_TO_TEXT_PAGE;
    }

    @Override
    protected String getItemKey(UItem item) {
        return switch (item.id) {
            case 1 -> "transcriptionProvider";
            case 2 -> "transcriptionUrl";
            case 3 -> "transcriptionAccount";
            case 4 -> "transcriptionKey";
            case 5 -> "transcriptionModel";
            case 6 -> "transcriptionPrompt";
            default -> super.getItemKey(item);
        };
    }

    static String name(Transcription.Provider provider) {
        return provider == Transcription.CUSTOM ? getString(R.string.LlmCustom) : provider.name();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        Transcription.Provider provider = Transcription.provider();
        items.add(UItem.asHeader(getString(R.string.TranscriptionService)));
        items.add(UItem.asButton(1, getString(R.string.LlmProvider), name(provider)));
        if (provider == Transcription.TELEGRAM) {
            items.add(UItem.asShadow(getString(R.string.TranscriptionTelegramInfo)));
            return;
        }
        if (provider == Transcription.CUSTOM) items.add(UItem.asButton(2, getString(R.string.LlmUrl), AoiConfig.transcriptionCustomUrl.get()));
        if (provider == Transcription.CLOUDFLARE) items.add(UItem.asButton(3, getString(R.string.TranscriptionCloudflareAccount), AoiConfig.transcriptionCloudflareAccount.get()));
        items.add(UItem.asButton(4, getString(provider == Transcription.CLOUDFLARE ? R.string.TranscriptionCloudflareToken : R.string.LlmKey), KeysDialog.summary(provider.key())));
        if (provider.model() != null) items.add(UItem.asButton(5, getString(R.string.LlmModel), provider.effectiveModel()));
        boolean shared = provider == Transcription.GROQ || provider == Transcription.OPENAI || provider == Transcription.GOOGLE_AI_STUDIO;
        items.add(UItem.asShadow(shared ? AndroidUtilities.replaceSingleTag(getString(R.string.TranscriptionSharedKeyInfo), () -> presentFragment(new AoiLlmSettingsActivity())) : null));
        if (provider == Transcription.GOOGLE_AI_STUDIO) {
            items.add(UItem.asButton(6, getString(R.string.TranscriptionPrompt), getString(AoiConfig.transcriptionPrompt.get().isEmpty() ? R.string.Default : R.string.LlmCustom)));
            items.add(UItem.asShadow(getString(R.string.TranscriptionPromptInfo)));
        }
        if (Transcription.canPreferTelegram()) {
            items.add(asConfigCheck(AoiConfig.transcriptionPremium, R.string.TranscriptionPremium));
            items.add(UItem.asShadow(getString(R.string.TranscriptionPremiumInfo)));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        Transcription.Provider provider = Transcription.provider();
        if (item.id == 1) {
            String[] labels = Transcription.PROVIDERS.stream().map(AoiSpeechToTextActivity::name).toArray(String[]::new);
            showDialog(AlertsCreator.createSingleChoiceDialog(getParentActivity(), labels, getString(R.string.LlmProvider), Transcription.PROVIDERS.indexOf(provider), (dialog, which) -> {
                AoiConfig.transcriptionProvider.set(Transcription.PROVIDERS.get(which).id());
                listView.adapter.update(true);
            }));
        } else if (item.id == 2) editString(item.text, AoiConfig.transcriptionCustomUrl, "https://…/v1");
        else if (item.id == 3) editString(item.text, AoiConfig.transcriptionCloudflareAccount, "");
        else if (item.id == 4) new KeysDialog(this, item.text, provider.key(), provider.keyUrl()).show();
        else if (item.id == 5) editString(item.text, provider.model(), provider.defaultModel());
        else if (item.id == 6) presentFragment(AoiPromptActivity.transcriptionPrompt());
        else super.onItemClick(item, view, position, x, y);
    }
}
