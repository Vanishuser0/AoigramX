package nu.gpu.aoigram.messages;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_iv;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.utils.code.highlight.CodeHighlighting;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.settings.BaseAoiSettingsActivity;

public final class MessageJsonActivity extends BaseAoiSettingsActivity {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(byte[].class, (JsonSerializer<byte[]>) (value, type, context) ->
                    new JsonPrimitive(Base64.encodeToString(value, Base64.NO_WRAP)))
            .registerTypeHierarchyAdapter(TLObject.class, (JsonSerializer<TLObject>) MessageJsonActivity::serializeObject)
            .setExclusionStrategies(new ExclusionStrategy() {
                @Override
                public boolean shouldSkipField(FieldAttributes field) {
                    return skipField(field.getDeclaringClass(), field.getName());
                }

                @Override
                public boolean shouldSkipClass(Class<?> type) {
                    return false;
                }
            })
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .create();

    private final JsonElement json;
    private final ArrayList<Node> nodes = new ArrayList<>();
    private final FlagSecureReason flagSecure;

    private MessageJsonActivity(BaseFragment parent, JsonElement json, boolean protectedContent) {
        this.json = json;
        setCurrentAccount(parent.getCurrentAccount());
        setResourceProvider(parent.getResourceProvider());
        flagSecure = new FlagSecureReason(parent.getParentActivity().getWindow(), () -> protectedContent && !AoiConfig.allowScreenshots.get());
        addNodes(json, null, 0, false);
    }

    public static void open(BaseFragment fragment, TLRPC.Message message, boolean protectedContent) {
        try {
            fragment.presentFragment(new MessageJsonActivity(fragment, GSON.toJsonTree(message), protectedContent));
        } catch (RuntimeException e) {
            FileLog.e(e);
            AlertsCreator.showSimpleAlert(fragment, getString(R.string.ErrorOccurred), getString(R.string.JsonExportError), fragment.getResourceProvider());
        }
    }

    @Override
    public void onBecomeFullyVisible() {
        super.onBecomeFullyVisible();
        flagSecure.attach();
    }

    @Override
    public void onBecomeFullyHidden() {
        flagSecure.detach();
        super.onBecomeFullyHidden();
    }

    @Override
    public void onFragmentDestroy() {
        flagSecure.detach();
        super.onFragmentDestroy();
    }

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.MessageJson);
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(1, R.drawable.msg_copy).setContentDescription(getString(R.string.Copy));
        ActionBarMenuItem more = menu.addItem(2, R.drawable.ic_ab_other);
        more.setContentDescription(getString(R.string.AccDescrMoreOptions));
        more.addSubItem(3, getString(R.string.JsonExpandAll));
        more.addSubItem(4, getString(R.string.JsonCollapseAll));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    copyJson(json);
                } else if (id == 3 || id == 4) {
                    for (Node node : nodes) {
                        node.expanded = id == 3 || node.depth == 0;
                    }
                    listView.adapter.update(false);
                    listView.scrollToPosition(0);
                }
            }
        });
        return view;
    }

    private void addNodes(JsonElement element, String key, int depth, boolean comma) {
        Node node = new Node(element, key, depth, comma);
        nodes.add(node);
        if (node.childCount == 0) {
            return;
        }
        if (element.isJsonObject()) {
            var iterator = element.getAsJsonObject().entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                addNodes(entry.getValue(), entry.getKey(), depth + 1, iterator.hasNext());
            }
        } else {
            for (int i = 0; i < node.childCount; i++) {
                addNodes(element.getAsJsonArray().get(i), null, depth + 1, i < node.childCount - 1);
            }
        }
        Node end = new Node(element, null, depth, comma);
        end.start = node;
        node.endIndex = nodes.size();
        nodes.add(end);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            items.add(NodeCell.Factory.of(i, node));
            if (node.start == null && node.childCount > 0 && !node.expanded) {
                i = node.endIndex;
            }
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (!(item.object instanceof Node node)) {
            return;
        }
        Node target = node.start == null ? node : node.start;
        if (target.childCount > 0 || target.isString()) {
            target.expanded = !target.expanded;
            listView.adapter.update(true);
            if (node.start != null) {
                for (int i = 0; i < listView.adapter.getItemCount(); i++) {
                    if (listView.adapter.getItem(i).object == target) {
                        listView.layoutManager.scrollToPositionWithOffset(i, 0);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        if (!(item.object instanceof Node node) || node.start != null) {
            return false;
        }
        ItemOptions.makeOptions(this, view)
                .setScrimViewBackground(listView.getClipBackground(view))
                .addIf(node.key != null, R.drawable.msg_copy, getString(R.string.JsonCopyKey), () -> copy(node.key))
                .add(R.drawable.msg_copy, getString(R.string.JsonCopyValue), () -> {
                    if (node.isString()) {
                        copy(node.element.getAsString());
                    } else {
                        copyJson(node.element);
                    }
                })
                .show();
        return true;
    }

    private void copyJson(JsonElement element) {
        SpannableString text = new SpannableString(GSON.toJson(element));
        text.setSpan(new CodeHighlighting.Span(false, 0, null, "json", null), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        copy(text);
    }

    private void copy(CharSequence text) {
        AndroidUtilities.addToClipboard(text);
        BulletinFactory.of(this).createCopyBulletin(getString(R.string.TextCopied)).show();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void updateCustomColors() {
        listView.adapter.notifyDataSetChanged();
    }

    @SuppressWarnings("DataFlowIssue")
    private static JsonElement serializeObject(TLObject source, Type type, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty("_type", source.getClass().getSimpleName());
        HashSet<String> names = new HashSet<>();
        for (Class<?> current = source.getClass(); current != TLObject.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (field.isSynthetic() || Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }
                String name = field.getName();
                if (!names.add(name) || skipField(field.getDeclaringClass(), name)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(source);
                    if (value != null) {
                        object.add(name, context.serialize(value));
                    }
                } catch (IllegalAccessException e) {
                    throw new JsonParseException(e);
                }
            }
        }
        return object;
    }

    private static boolean skipField(Class<?> type, String name) {
        if ("parentRichText".equals(name) || "bitmap".equals(name) || "mChangingConfigurations".equals(name)) {
            return true;
        }
        if (TL_iv.Page.class.isAssignableFrom(type)) {
            return "web".equals(name) || "local".equals(name);
        }
        if (TL_iv.textMath.class.isAssignableFrom(type)) {
            return "w".equals(name) || "h".equals(name) || "depth".equals(name) || "tried".equals(name);
        }
        if (TL_iv.PageBlock.class.isAssignableFrom(type)) {
            return switch (name) {
                case "first", "bottom", "level", "quoteLevels", "mid", "groupId", "thumb", "thumbObject", "cachedWidth", "cachedHeight" -> true;
                default -> false;
            };
        }
        return false;
    }

    private static final class Node {
        final JsonElement element;
        final String key;
        final int depth;
        final boolean comma;
        final int childCount;
        boolean expanded;
        int endIndex;
        Node start;

        Node(JsonElement element, String key, int depth, boolean comma) {
            this.element = element;
            this.key = key;
            this.depth = depth;
            this.comma = comma;
            childCount = element.isJsonObject() ? element.getAsJsonObject().size() : element.isJsonArray() ? element.getAsJsonArray().size() : 0;
            expanded = depth == 0;
        }

        boolean isString() {
            return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
        }
    }

    private static final class NodeCell extends AppCompatTextView {
        private final Theme.ResourcesProvider resourcesProvider;
        private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Node node;

        NodeCell(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            setTypeface(Typeface.MONOSPACE);
            setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
            setTextDirection(TEXT_DIRECTION_LTR);
            setLayoutDirection(LAYOUT_DIRECTION_LTR);
            setEllipsize(TextUtils.TruncateAt.END);
            setMinimumHeight(dp(40));
            guidePaint.setStrokeWidth(dp(1));
            guidePaint.setStrokeCap(Paint.Cap.ROUND);
            setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        void bind(Node node) {
            this.node = node;
            setPadding(dp(28 + Math.min(node.depth, 8) * 12), dp(8), dp(16), dp(8));
            setMaxLines(node.isString() && !node.expanded ? 4 : Integer.MAX_VALUE);
            setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            SpannableStringBuilder text = new SpannableStringBuilder();
            if (node.key != null) {
                text.append(new JsonPrimitive(node.key).toString()).append(": ");
            }
            int valueStart = text.length();
            int colorKey = Theme.key_windowBackgroundWhiteBlackText;
            if (node.element.isJsonObject() || node.element.isJsonArray()) {
                boolean object = node.element.isJsonObject();
                if (node.start != null) {
                    text.append(object ? "}" : "]");
                } else if (node.childCount == 0) {
                    text.append(object ? "{}" : "[]");
                } else {
                    text.append(object ? node.expanded ? "{" : "{…}" : node.expanded ? "[" : "[…]");
                }
            } else {
                text.append(node.element.toString());
                colorKey = node.element.isJsonNull()
                        ? Theme.key_code_keyword
                        : node.isString()
                        ? Theme.key_code_string
                        : node.element.getAsJsonPrimitive().isBoolean()
                        ? Theme.key_code_constant
                        : Theme.key_code_number;
            }
            text.setSpan(new ForegroundColorSpan(Theme.getColor(colorKey, resourcesProvider)), valueStart, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (node.comma && (node.start != null || node.childCount == 0 || !node.expanded)) {
                text.append(',');
            }
            if (node.start == null && node.childCount > 0 && !node.expanded) {
                int countStart = text.length();
                text.append("  ").append(LocaleController.formatPluralStringComma("items", node.childCount));
                text.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider)), countStart, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            setText(text);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int depth = Math.min(node.depth, 8);
            guidePaint.setColor(Theme.getColor(Theme.key_divider, resourcesProvider));
            for (int i = 0; i < depth; i++) {
                float x = dp(16 + i * 12);
                canvas.drawLine(x, 0, x, getHeight(), guidePaint);
            }
            if (node.start == null && node.childCount > 0) {
                float x = dp(16 + depth * 12);
                float y = dp(20);
                guidePaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
                canvas.save();
                canvas.rotate(node.expanded ? 90 : 0, x, y);
                canvas.drawLine(x - dp(2), y - dp(4), x + dp(2), y, guidePaint);
                canvas.drawLine(x + dp(2), y, x - dp(2), y + dp(4), guidePaint);
                canvas.restore();
            }
            super.onDraw(canvas);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            if (node.start == null && (node.childCount > 0 || node.isString())) {
                info.setStateDescription(getString(node.expanded ? R.string.AccDescrExpanded : R.string.AccDescrCollapsed));
            }
        }

        public static final class Factory extends UItem.UItemFactory<NodeCell> {
            static { setup(new Factory()); }

            static UItem of(int id, Node node) {
                UItem item = UItem.ofFactory(Factory.class);
                item.id = id;
                item.object = node;
                return item.setChecked(node.expanded);
            }

            @Override
            public NodeCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
                return new NodeCell(context, resourcesProvider);
            }

            @Override
            public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
                ((NodeCell) view).bind((Node) item.object);
            }

            @Override
            public boolean equals(UItem a, UItem b) {
                return a.id == b.id;
            }

            @Override
            public boolean contentsEquals(UItem a, UItem b) {
                return a.checked == b.checked;
            }
        }
    }
}
