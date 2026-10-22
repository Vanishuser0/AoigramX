package nu.gpu.aoigram.translate;

import static org.telegram.messenger.LocaleController.getString;

import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.EditTextCaption;

import java.util.function.Consumer;

import nu.gpu.aoigram.utils.ErrorFeedback;

public final class InputTranslation {

    private TranslationTask task;
    private AlertDialog progress;
    private Bulletin undo;

    public void translate(int account, long dialog, MessageObject reply, TranslationOptions options,
                          EditTextCaption field, Consumer<CharSequence> replace,
                          BulletinFactory bulletins, Theme.ResourcesProvider resourcesProvider) {
        cancel();
        SpannableStringBuilder original = new SpannableStringBuilder(field.getText());
        int selectionStart = field.getSelectionStart();
        int selectionEnd = field.getSelectionEnd();
        CharSequence[] text = {new SpannableStringBuilder(field.getTextToUse())};
        TLRPC.TL_textWithEntities source = new TLRPC.TL_textWithEntities();
        source.entities = MediaDataController.getInstance(account).getEntities(text, true);
        source.text = text[0].toString();
        String context = MessagesController.getInstance(account).getTranslateController().aoiTranslationContext.collectInput(account, dialog, reply, options);
        TranslationTask request = new TranslationTask();
        task = request;
        progress = new AlertDialog(field.getContext(), AlertDialog.ALERT_TYPE_SPINNER, resourcesProvider);
        progress.setOnCancelListener(dialogInterface -> cancel());
        progress.show();
        TranslationService.translate(request, account, source, options, context, (result, error) -> {
            progress.dismiss();
            progress = null;
            if (!field.isAttachedToWindow() || !TextUtils.equals(original, field.getText())) {
                cancel();
                return;
            }
            if (error != null) {
                cancel();
                ErrorFeedback.show(error, bulletins, AlertDialog::show, true);
                return;
            }
            CharSequence translated = ChatActivityEnterView.applyMessageEntities(result.entities, result.text, field.getPaint().getFontMetricsInt());
            replace.accept(translated);
            field.setSelection(field.length());
            String applied = field.getText().toString();
            undo = bulletins.createUndoBulletin(getString(R.string.InputTranslationDone), () -> {
                if (task == request && field.isAttachedToWindow() && TextUtils.equals(applied, field.getText())) {
                    replace.accept(original);
                    field.setSelection(selectionStart, selectionEnd);
                }
                if (task == request) {
                    cancel();
                }
            }, () -> {
                if (task == request) {
                    cancel();
                }
            });
            undo.show();
        });
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (progress != null) {
            progress.dismiss();
            progress = null;
        }
        if (undo != null) {
            Bulletin bulletin = undo;
            undo = null;
            bulletin.hide();
        }
    }
}
