package nu.gpu.aoigram.dialogs;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.annotation.SuppressLint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.EditEmojiTextCell;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ReplaceableIconDrawable;

@SuppressLint("ViewConstructor")
public final class FolderIconButton extends AppCompatImageView {

    private final BaseFragment fragment;
    private final Utilities.Callback0Return<String> emoticon;
    private final Utilities.Callback<String> onSelected;
    private final ReplaceableIconDrawable iconDrawable;

    public FolderIconButton(BaseFragment fragment, Utilities.Callback0Return<String> emoticon, Utilities.Callback<String> onSelected) {
        super(fragment.getContext());
        this.fragment = fragment;
        this.emoticon = emoticon;
        this.onSelected = onSelected;
        iconDrawable = new ReplaceableIconDrawable(getContext());
        iconDrawable.exactlyBounds = true;
        setImageDrawable(iconDrawable);
        setPadding(dp(4), dp(4), dp(4), dp(4));
        setColorFilter(new PorterDuffColorFilter(fragment.getThemedColor(Theme.key_chat_messagePanelIcons), PorterDuff.Mode.MULTIPLY));
        setBackground(Theme.createSelectorDrawable(fragment.getThemedColor(Theme.key_listSelector)));
        setContentDescription(getString(R.string.FolderIcon));
        setOnClickListener(v -> showPicker());
        update();
    }

    public void attachTo(EditEmojiTextCell cell) {
        cell.addView(this, LayoutHelper.createFrame(36, 36, Gravity.LEFT | Gravity.CENTER_VERTICAL, 12, 0, 0, 0));
        ((ViewGroup.MarginLayoutParams) cell.editTextEmoji.getLayoutParams()).leftMargin = dp(40);
    }

    public void update() {
        iconDrawable.setIcon(FolderIcons.getIcon(emoticon.run()), true);
    }

    private void showPicker() {
        ItemOptions options = ItemOptions.makeOptions(fragment, this);
        options.setScrimViewBackground(Theme.createCircleDrawable(dp(36), fragment.getThemedColor(Theme.key_windowBackgroundWhite)));
        int selectedColor = fragment.getThemedColor(Theme.key_windowBackgroundWhiteValueText);
        int color = fragment.getThemedColor(Theme.key_chat_messagePanelIcons);
        int selectorColor = fragment.getThemedColor(Theme.key_listSelector);
        String current = emoticon.run();
        GridLayout grid = new GridLayout(getContext());
        grid.setColumnCount(6);
        for (String item : FolderIcons.getEmoticons()) {
            boolean selected = item.equals(current);
            ImageView view = new ImageView(getContext());
            view.setPadding(dp(6), dp(6), dp(6), dp(6));
            view.setImageResource(FolderIcons.getIcon(item));
            view.setColorFilter(new PorterDuffColorFilter(selected ? selectedColor : color, PorterDuff.Mode.MULTIPLY));
            view.setBackground(Theme.createRadSelectorDrawable(selected ? Theme.multAlpha(selectedColor, 0.15f) : 0, selectorColor, 8, 8));
            view.setOnClickListener(v -> {
                options.dismiss();
                if (!selected) {
                    onSelected.run(item);
                }
            });
            grid.addView(view, dp(48), dp(48));
        }
        options.addView(grid, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 4, 4, 4, 4));
        options.show();
    }
}
