package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.WindowInsetsCompat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.recyclerview.LinearSmoothScrollerCustom;
import org.telegram.utils.code.highlight.CodeHighlighting;

import java.util.ArrayList;

import nu.gpu.aoigram.translate.TranslationHttp;
import nu.gpu.aoigram.translate.TranslationOptions;
import nu.gpu.aoigram.translate.TranslationTask;
import nu.gpu.aoigram.translate.llm.LlmSettings;
import nu.gpu.aoigram.translate.llm.LlmTranslation;
import nu.gpu.aoigram.utils.ErrorFeedback;

import okhttp3.Request;

@SuppressWarnings("DataFlowIssue")
public final class AoiLlmRequestBodyActivity extends BaseAoiSettingsActivity {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().setStrictness(Strictness.STRICT).create();

    private final LlmSettings settings = new LlmSettings();
    private JsonObject base;
    private JsonObject defaults;
    private String head;
    private String defaultText;
    private String initialText;
    private CodeView code;
    private TextView headView;
    private TextView tailView;
    private EditTextBoldCursor editor;
    private CodeView response;
    private TextView statusView;
    private TextView responseView;
    private TranslationTask test;
    private final Runnable highlight = this::highlight;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.LlmRequestBody);
    }

    @Override
    protected void onCreateSettingsView(Context context) {
        String key = settings.keys().get(0);
        try {
            base = JsonParser.parseString(LlmTranslation.previewBody(settings).toString()).getAsJsonObject();
            defaults = JsonParser.parseString(LlmTranslation.defaultParameters(settings).toString()).getAsJsonObject();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Request request = LlmTranslation.createRequest(settings, key, new JSONObject());
        StringBuilder lines = new StringBuilder(request.method()).append(' ').append(request.url());
        for (int i = 0; i < request.headers().size(); i++) {
            String value = request.headers().value(i);
            lines.append('\n')
                    .append(request.headers().name(i)).append(": ")
                    .append(value, 0, value.length() - key.length())
                    .append(KeysDialog.mask(key));
        }
        lines.append("\nContent-Type: ").append(request.body().contentType());
        String json = GSON.toJson(base);
        head = lines + "\n\n" + json.substring(0, json.length() - 2);
        defaultText = members(defaults);
        initialText = settings.body == null ? defaultText : members(JsonParser.parseString(settings.body).getAsJsonObject());

        code = new CodeView(context);
        headView = codeText(context);
        tailView = codeText(context);
        tailView.setText("}");
        editor = new EditTextBoldCursor(context);
        editor.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editor.setGravity(Gravity.TOP | Gravity.START);
        editor.setTypeface(Typeface.MONOSPACE);
        editor.setTextSize(13);
        editor.setPadding(0, 0, 0, 0);
        editor.setBackground(null);
        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable text) {
                updateHead();
                code.invalidate();
                AndroidUtilities.cancelRunOnUIThread(highlight);
                AndroidUtilities.runOnUIThread(highlight, 300);
            }
        });
        code.addView(headView, LayoutHelper.createLinear(-1, -2));
        code.addView(editor, LayoutHelper.createLinear(-1, -2));
        code.addView(tailView, LayoutHelper.createLinear(-1, -2));
        editor.setText(initialText);

        response = new CodeView(context);
        statusView = codeText(context);
        responseView = codeText(context);
        responseView.setTextIsSelectable(true);
        response.addView(statusView, LayoutHelper.createLinear(-1, -2));
        response.addView(responseView, LayoutHelper.createLinear(-1, -2));

        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(1, R.drawable.outline_header_reset).setContentDescription(getString(R.string.LlmRestoreDefault));
        menu.addItemWithWidth(2, R.drawable.ic_ab_done, dp(56), getString(R.string.Done));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == 1) editor.setText(defaultText);
                else if (id == 2) save();
                else if (id == -1 && onBackPressed(true)) close();
            }
        });
    }

    private TextView codeText(Context context) {
        TextView text = new TextView(context);
        text.setTypeface(Typeface.MONOSPACE);
        text.setTextSize(13);
        return text;
    }

    private static String members(JsonObject json) {
        String text = GSON.toJson(json);
        return json.isEmpty() ? "" : text.substring(2, text.length() - 2);
    }

    private void updateHead() {
        String text = editor.getText().toString().isBlank() ? head : head + ",";
        if (!text.contentEquals(headView.getText())) headView.setText(text);
    }

    private void highlight() {
        String snapshot = editor.getText().toString();
        CodeHighlighting.highlightEditable(snapshot, "json", result -> {
            Editable text = editor.getText();
            if (!snapshot.contentEquals(text)) return;
            for (CodeHighlighting.ColorSpan span : text.getSpans(0, text.length(), CodeHighlighting.ColorSpan.class)) {
                text.removeSpan(span);
            }
            for (CodeHighlighting.ColorSpan span : result.getSpans(0, result.length(), CodeHighlighting.ColorSpan.class)) {
                text.setSpan(span, result.getSpanStart(span), result.getSpanEnd(span), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        });
    }

    @Override
    protected @NonNull WindowInsetsCompat onApplyWindowInsets(@NonNull View view, @NonNull WindowInsetsCompat insets) {
        super.onApplyWindowInsets(view, insets);
        int bottom = Math.max(AndroidUtilities.getDefaultWindowInsets(insets, false).bottom, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
        listView.setPadding(0, listView.getPaddingTop(), 0, bottom);
        return WindowInsetsCompat.CONSUMED;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCustom(code));
        items.add(UItem.asShadow(AndroidUtilities.replaceTags(formatString(R.string.LlmRequestBodyInfo, settings.model))));
        items.add((test == null ? UItem.asButton(2, getString(R.string.LlmTest)) : UItem.asButtonLoadingValue(2, getString(R.string.LlmTest))).accent());
        items.add(UItem.asShadow(null));
        if (statusView.length() > 0) {
            items.add(UItem.asHeader(3, getString(R.string.LlmResponse)));
            items.add(UItem.asCustom(response));
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == 2 && test == null) test();
    }

    private JsonObject parse() {
        JsonObject body;
        try {
            String offset = "\n".repeat((int) head.chars().filter(c -> c == '\n').count());
            body = GSON.fromJson(offset + "{\n" + editor.getText() + "\n}", JsonObject.class);
            new JSONObject(body.toString());
        } catch (JsonParseException | JSONException e) {
            String message = (e.getCause() == null ? e : e.getCause()).getMessage();
            ErrorFeedback.show(message.split("\n", 2)[0], BulletinFactory.of(this), this::showDialog, true);
            return null;
        }
        for (String name : base.keySet()) {
            if (body.has(name)) {
                ErrorFeedback.show(formatString(R.string.LlmRequestBodyReserved, name), BulletinFactory.of(this), this::showDialog, true);
                return null;
            }
        }
        return body;
    }

    private void save() {
        JsonObject body = parse();
        if (body == null) return;
        settings.saveBody(body.equals(defaults) ? null : body);
        close();
    }

    @SuppressLint("SetTextI18n")
    private void test() {
        JsonObject body = parse();
        if (body == null) return;
        Request request;
        try {
            request = LlmTranslation.testRequest(settings, "Hello, world!", TranslationOptions.defaultLanguage(), new JSONObject(body.toString()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        AndroidUtilities.hideKeyboard(editor);
        long start = System.currentTimeMillis();
        TranslationTask task = test = new TranslationTask();
        listView.adapter.update(true);
        TranslationHttp.send(task, request, true, (raw, metadata) -> TranslationHttp.format(raw.trim()), (result, code, error) -> {
            test = null;
            String[] parts = error == null ? new String[] {"HTTP " + code, result} : error.split("\n", 2);
            String text = parts.length > 1 ? parts[1] : "";
            statusView.setText(parts[0] + " · " + (System.currentTimeMillis() - start) + " ms");
            responseView.setText(text);
            CodeHighlighting.highlightEditable(text, "json", highlighted -> {
                if (text.contentEquals(responseView.getText())) responseView.setText(highlighted);
            });
            listView.adapter.update(true);
            LinearSmoothScrollerCustom scroller = new LinearSmoothScrollerCustom(getContext(), LinearSmoothScrollerCustom.POSITION_TOP);
            scroller.setTargetPosition(listView.findPositionByItemId(3));
            listView.layoutManager.startSmoothScroll(scroller);
        });
    }

    private void close() {
        AndroidUtilities.hideKeyboard(editor);
        finishFragment();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (initialText.contentEquals(editor.getText())) return true;
        if (invoked) {
            showDialog(new AlertDialog.Builder(getContext(), getResourceProvider())
                    .setTitle(getString(R.string.UnsavedChanges))
                    .setMessage(getString(R.string.LlmRequestBodyUnsaved))
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
    public void onFragmentDestroy() {
        AndroidUtilities.cancelRunOnUIThread(highlight);
        if (test != null) test.cancel();
        super.onFragmentDestroy();
    }

    @Override
    protected void updateCustomColors() {
        int text = getThemedColor(Theme.key_windowBackgroundWhiteBlackText);
        int locked = getThemedColor(Theme.key_windowBackgroundWhiteGrayText);
        code.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        code.paint.setColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        headView.setTextColor(locked);
        tailView.setTextColor(locked);
        editor.setTextColor(text);
        editor.setCursorColor(text);
        code.invalidate();
        response.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
        response.paint.setColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        statusView.setTextColor(locked);
        responseView.setTextColor(text);
        response.invalidate();
    }

    private static final class CodeView extends LinearLayout {

        final TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        CodeView(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setWillNotDraw(false);
            setPadding(dp(48), dp(12), dp(16), dp(12));
            paint.setTypeface(Typeface.MONOSPACE);
            paint.setTextAlign(Paint.Align.RIGHT);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            int number = 0;
            for (int i = 0; i < getChildCount(); i++) {
                TextView child = (TextView) getChildAt(i);
                Layout layout = child.getLayout();
                CharSequence text = child.getText();
                paint.setTextSize(child.getTextSize());
                for (int line = 0; line < layout.getLineCount(); line++) {
                    int start = layout.getLineStart(line);
                    if (start > 0 && text.charAt(start - 1) != '\n') continue;
                    canvas.drawText(Integer.toString(++number), dp(36), child.getTop() + child.getTotalPaddingTop() + layout.getLineBaseline(line), paint);
                }
            }
        }
    }
}
