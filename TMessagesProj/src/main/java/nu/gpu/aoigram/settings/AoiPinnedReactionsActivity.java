package nu.gpu.aoigram.settings;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FilledTabsView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Reactions.BackSpaceButtonView;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble.VisibleReaction;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.Components.ViewPagerFixed;
import org.telegram.ui.SelectAnimatedEmojiDialog;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.util.ArrayList;
import java.util.HashSet;

import nu.gpu.aoigram.config.AoiConfig;
import nu.gpu.aoigram.reactions.PinnedReactions;
import nu.gpu.aoigram.reactions.PinnedReactionsEditor;
import nu.gpu.aoigram.reactions.PinnedReactionsPreview;

public final class AoiPinnedReactionsActivity extends BaseAoiSettingsActivity implements NotificationCenter.NotificationCenterDelegate {

    private final ArrayList<VisibleReaction> chats = PinnedReactions.get(false);
    private final ArrayList<VisibleReaction> channels = PinnedReactions.get(true);
    private NotificationCenter.ObserversGroup observersGroup;
    private boolean initialChannel;
    private String initialItemKey;
    private FilledTabsView tabs;
    private ViewPagerFixed pager;
    private Page[] pages;

    @Override
    protected CharSequence getActionBarTitle() {
        return getString(R.string.PinnedReactions);
    }

    @Override
    protected String getSettingsPageKey() {
        return AoiSettingsDeepLink.PINNED_REACTIONS_PAGE;
    }

    @Override
    public BaseAoiSettingsActivity setInitialItemKey(String itemKey) {
        initialItemKey = itemKey;
        initialChannel = PinnedReactions.enabled(true).getKey().equals(itemKey) || AoiConfig.hideStarReactionChannels.getKey().equals(itemKey);
        return super.setInitialItemKey(itemKey);
    }

    @Override
    public boolean onFragmentCreate() {
        observersGroup = NotificationCenter.getGlobalInstance().createObserversGroup(this).add(NotificationCenter.reactionsDidLoad);
        return super.onFragmentCreate();
    }

    @Override
    protected void onCreateSettingsView(Context context) {
        tabs = new FilledTabsView(context);
        tabs.setTabs(getString(R.string.FilterChats), getString(R.string.FilterChannels));
        pages = new Page[]{new Page(context, false), new Page(context, true)};
        pager = new ViewPagerFixed(context) {
            @Override
            public void onTabAnimationUpdate(boolean manual) {
                tabs.setSelected(getPositionAnimated());
            }

            @Override
            protected void onScrollEnd() {
                super.onScrollEnd();
                updateActionBarVisible();
            }
        };
        pager.setAdapter(new ViewPagerFixed.Adapter() {
            @Override
            public int getItemCount() {
                return pages.length;
            }

            @Override
            public int getItemViewType(int position) {
                return position;
            }

            @Override
            public View createView(int viewType) {
                return pages[viewType];
            }

            @Override
            public void bindView(View view, int position, int viewType) {
            }

            @Override
            public boolean canScrollTo(int position) {
                closePickers();
                return true;
            }
        });
        pager.setPosition(initialChannel ? 1 : 0);
        UniversalRecyclerView initialList = pages[pager.getCurrentPosition()].listView;
        for (int position = 0; position < initialList.adapter.getItemCount(); position++) {
            UItem item = initialList.adapter.getItem(position);
            if (item != null && initialItemKey != null && initialItemKey.equals(getItemKey(item))) {
                int finalPosition = position;
                initialList.highlightRow(() -> finalPosition);
                break;
            }
        }
        tabs.onTabSelected(index -> {
            if (index == pager.getCurrentPosition()) return;
            closePickers();
            pager.scrollToPosition(index);
        });
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1 && onBackPressed(true)) finishFragment();
            }
        });
        Bulletin.addDelegate(this, new Bulletin.Delegate() {
            @Override
            public int getBottomOffset(int tag) {
                return listView.getPaddingBottom() + pages[pager.getCurrentPosition()].listView.getPaddingBottom();
            }
        });
    }

    @Override
    public View createView(Context context) {
        View view = super.createView(context);
        listView.setSections(0, 0, false);
        listView.setLayoutManager(listView.layoutManager = new LinearLayoutManager(context) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        return view;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (adapter == null) {
            addToggles(items, false);
            addToggles(items, true);
            addStarToggle(items);
            return;
        }
        items.add(UItem.asCustomShadow(tabs, 52));
        items.add(UItem.asFullscreenCustom(pager, dp(52), true));
    }

    private void addToggles(ArrayList<UItem> items, boolean channel) {
        items.add(asConfigCheck(PinnedReactions.enabled(channel), channel ? R.string.PinnedReactionsChannels : R.string.PinnedReactionsChats));
        items.add(UItem.asShadow(formatString(channel ? R.string.PinnedReactionsChannelsInfo : R.string.PinnedReactionsChatsInfo, PinnedReactions.LIMIT)));
    }

    private void addStarToggle(ArrayList<UItem> items) {
        items.add(asConfigCheck(AoiConfig.hideStarReactionChannels, R.string.HideStarReaction));
        items.add(UItem.asShadow(getString(R.string.HideStarReactionInfo)));
    }

    @Override
    protected boolean shouldShowActionBarBackground() {
        UniversalRecyclerView pageList = pages[pager.getCurrentPosition()].listView;
        if (pageList.getChildCount() == 0) return false;
        View firstRow = pageList.layoutManager.findViewByPosition(0);
        return firstRow == null || pageList.layoutManager.getDecoratedTop(firstRow) < pageList.getPaddingTop();
    }

    private void closePickers() {
        for (Page page : pages) {
            if (page.pickerVisible) page.closePicker();
        }
    }

    private boolean hasChanges() {
        return !chats.equals(PinnedReactions.get(false)) || !channels.equals(PinnedReactions.get(true));
    }

    private void save() {
        PinnedReactions.set(false, chats);
        PinnedReactions.set(true, channels);
        closePickers();
        finishFragment();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        Page page = pages[pager.getCurrentPosition()];
        if (page.pickerVisible) {
            if (invoked) page.closePicker();
            return false;
        }
        if (!hasChanges()) return true;
        if (invoked) {
            showDialog(new AlertDialog.Builder(getContext(), getResourceProvider())
                    .setTitle(getString(R.string.UnsavedChanges))
                    .setMessage(getString(R.string.ReactionApplyChangesDialog))
                    .setPositiveButton(getString(R.string.ApplyTheme), (dialog, which) -> save())
                    .setNegativeButton(getString(R.string.PassportDiscard), (dialog, which) -> finishFragment())
                    .create());
        }
        return false;
    }

    @Override
    public boolean canBeginSlide() {
        return pager.getCurrentPosition() == 0 && onBackPressed(true);
    }

    @NonNull
    @Override
    protected WindowInsetsCompat onApplyWindowInsets(@NonNull View view, @NonNull WindowInsetsCompat insets) {
        super.onApplyWindowInsets(view, insets);
        Insets systemInsets = AndroidUtilities.getDefaultWindowInsets(insets, false);
        int keyboardInset = Math.max(0, insets.getInsets(WindowInsetsCompat.Type.ime()).bottom - systemInsets.bottom);
        for (Page page : pages) {
            page.keyboardInset = keyboardInset;
            page.updateBottomLayout();
        }
        return WindowInsetsCompat.CONSUMED;
    }

    @Override
    protected void updateCustomColors() {
        tabs.setColors(getThemedColor(Theme.key_windowBackgroundGray), getThemedColor(Theme.key_windowBackgroundWhite),
                getThemedColor(Theme.key_windowBackgroundWhiteGrayText), getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        for (Page page : pages) page.updateColors();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (pages == null) return;
        for (Page page : pages) {
            page.editor.refreshReactions();
            page.updatePreview(false);
            if (page.pickerVisible) {
                page.closePicker(false);
                page.editor.requestFocus();
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        Bulletin.removeDelegate(this);
        observersGroup.removeAllObservers();
        if (pages != null) {
            for (Page page : pages) {
                page.applyButton.animate().cancel();
                page.closePicker(false);
            }
        }
        super.onFragmentDestroy();
    }

    private final class Page extends FrameLayout {
        private final boolean channel;
        private final UniversalRecyclerView listView;
        private final PinnedReactionsPreview preview;
        private final PinnedReactionsEditor editor;
        private final FrameLayout pickerPanel;
        private final ButtonWithCounterView applyButton;
        private SelectAnimatedEmojiDialog picker;
        private boolean pickerVisible;
        private float pickerProgress;
        private ValueAnimator pickerAnimator;
        private int keyboardInset;

        Page(Context context, boolean channel) {
            super(context);
            this.channel = channel;
            setFocusableInTouchMode(true);
            preview = new PinnedReactionsPreview(context, AoiPinnedReactionsActivity.this);
            editor = new PinnedReactionsEditor(context, currentAccount, getResourceProvider(), this::onReactionsChanged);
            editor.setOnFocused(this::showPicker);
            editor.setReactions(reactions());
            editor.setFocusableInTouchMode(PinnedReactions.enabled(channel).get());
            updatePreview(false);
            listView = new UniversalRecyclerView(context, currentAccount, classGuid, this::fillItems, this::onItemClick,
                    AoiPinnedReactionsActivity.this::onItemLongClick, getResourceProvider());
            listView.adapter.setApplyBackground(false);
            listView.setSections();
            listView.setClipToPadding(false);
            listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (pager.getCurrentView() == Page.this) updateActionBarVisible();
                    if (listView.scrollingByUser) AndroidUtilities.hideKeyboard(Page.this);
                }
            });
            addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
            pickerPanel = new FrameLayout(context);
            pickerPanel.setVisibility(View.GONE);
            pickerPanel.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateBottomLayout());
            addView(pickerPanel, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
            applyButton = new ButtonWithCounterView(context, getResourceProvider());
            applyButton.setRound();
            applyButton.setText(getString(R.string.PinnedReactionsApply), false);
            applyButton.setOnClickListener(v -> save());
            addView(applyButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 13, 13, 13, 13));
            updateApplyButtonVisibility(false);
        }

        private ArrayList<VisibleReaction> reactions() {
            return channel ? channels : chats;
        }

        private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
            addToggles(items, channel);
            if (PinnedReactions.enabled(channel).get()) {
                items.add(UItem.asCustom(preview, 120));
                items.add(UItem.asShadow(null));
                items.add(UItem.asHeader(getString(R.string.PinnedReactions)));
                items.add(UItem.asCustom(editor));
                items.add(UItem.asShadow(getString(R.string.PinnedReactionsEditInfo)));
            }
            if (channel) addStarToggle(items);
        }

        private void onItemClick(UItem item, View view, int position, float x, float y) {
            if (item.object == PinnedReactions.enabled(channel)) {
                requestFocus();
                PinnedReactions.enabled(channel).toggle();
                boolean enabled = PinnedReactions.enabled(channel).get();
                if (!enabled) {
                    closePicker();
                    reactions().clear();
                    reactions().addAll(PinnedReactions.get(channel));
                    editor.setReactions(reactions());
                    updatePreview(false);
                }
                editor.setFocusableInTouchMode(enabled);
                listView.adapter.update(true);
                updateApplyButtonVisibility(true);
            } else if (item.object == AoiConfig.hideStarReactionChannels) {
                AoiConfig.hideStarReactionChannels.toggle();
                updatePreview(true);
                listView.adapter.update(true);
            }
        }

        private void onReactionsChanged() {
            updatePreview(true);
            syncPickerSelection();
        }

        private void updatePreview(boolean animated) {
            ArrayList<VisibleReaction> reactions = new ArrayList<>(reactions());
            if (reactions.isEmpty()) {
                for (TLRPC.Reaction reaction : getMediaDataController().getTopReactions()) {
                    if (reaction instanceof TLRPC.TL_reactionEmoji) {
                        reactions.add(VisibleReaction.fromTL(reaction));
                        if (reactions.size() == PinnedReactions.LIMIT) break;
                    }
                }
                if (reactions.isEmpty()) {
                    for (TLRPC.TL_availableReaction reaction : getMediaDataController().getEnabledReactionsList()) {
                        reactions.add(VisibleReaction.fromEmojicon(reaction));
                        if (reactions.size() == PinnedReactions.LIMIT) break;
                    }
                }
            }
            if (channel && !AoiConfig.hideStarReactionChannels.get()) {
                reactions.add(0, VisibleReaction.asStar());
            }
            preview.setReactions(reactions, animated);
        }

        private void showPicker() {
            if (pickerVisible) return;
            if (picker != null) {
                syncPickerSelection();
                animatePicker(true);
                return;
            }
            picker = new SelectAnimatedEmojiDialog(AoiPinnedReactionsActivity.this, getContext(), false, null,
                    SelectAnimatedEmojiDialog.TYPE_CHAT_REACTIONS, false, getResourceProvider(), 16) {
                @Override
                protected void onEmojiSelected(View view, Long documentId, TLRPC.Document document, TL_stars.TL_starGiftUnique gift, Integer until) {
                    if (documentId == null) return;
                    if (gift != null) {
                        showUnsupported();
                        return;
                    }
                    VisibleReaction reaction = VisibleReaction.fromCustomEmoji(documentId);
                    for (TLRPC.TL_availableReaction available : getMediaDataController().getEnabledReactionsList()) {
                        if (available.activate_animation.id == documentId) {
                            reaction = VisibleReaction.fromEmojicon(available);
                            break;
                        }
                    }
                    selectReaction(reaction);
                }

                @Override
                protected void onReactionClick(ImageViewEmoji emoji, VisibleReaction reaction) {
                    selectReaction(reaction);
                }
            };
            picker.setDrawBackground(false);
            picker.setLongPressEnabled(false);
            picker.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            pickerPanel.addView(picker, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            BackSpaceButtonView backspace = new BackSpaceButtonView(getContext(), getResourceProvider());
            backspace.setOnBackspace(fast -> editor.deleteReactions());
            pickerPanel.addView(backspace, LayoutHelper.createFrame(42, 42, Gravity.BOTTOM | Gravity.RIGHT, 0, 0, 8, 8));
            syncPickerSelection();
            pickerPanel.setVisibility(View.VISIBLE);
            animatePicker(true);
            AndroidUtilities.doOnPreDraw(pickerPanel, () -> {
                if (pickerVisible) {
                    picker.onShow(null);
                    updateBottomLayout();
                    View editorRow = listView.findContainingItemView(editor);
                    if (editorRow != null) {
                        int scroll = editorRow.getBottom() - (listView.getHeight() - listView.getPaddingBottom());
                        if (scroll > 0) listView.smoothScrollBy(0, scroll);
                    }
                }
            });
        }

        private void selectReaction(VisibleReaction reaction) {
            if (channel && reaction.documentId != 0) {
                showUnsupported();
                return;
            }
            if (!reactions().contains(reaction) && reactions().size() == PinnedReactions.LIMIT) {
                BulletinFactory.of(AoiPinnedReactionsActivity.this).createSimpleBulletin(R.raw.chats_infotip, formatString(R.string.PinnedReactionsLimit, PinnedReactions.LIMIT)).show();
                return;
            }
            editor.toggleReaction(reaction);
        }

        private void showUnsupported() {
            BulletinFactory.of(AoiPinnedReactionsActivity.this).createSimpleBulletin(R.raw.chats_infotip, getString(R.string.PinnedReactionsUnsupported)).show();
        }

        @SuppressWarnings("DataFlowIssue")
        @SuppressLint("NotifyDataSetChanged")
        private void syncPickerSelection() {
            if (picker == null) return;
            picker.setSelectedReactions(new HashSet<>(reactions()));
            for (VisibleReaction reaction : reactions()) {
                if (reaction.documentId == 0) {
                    TLRPC.TL_availableReaction available = getMediaDataController().getReactionsMap().get(reaction.emojicon);
                    if (available != null) picker.setMultiSelected(available.activate_animation.id, false);
                }
            }
            picker.notifyDataSetChanged();
            picker.emojiSearchGridView.getAdapter().notifyDataSetChanged();
        }

        private void closePicker() {
            closePicker(true);
        }

        private void closePicker(boolean animated) {
            if (picker == null) return;
            AndroidUtilities.hideKeyboard(picker);
            requestFocus();
            if (animated) {
                if (pickerVisible) animatePicker(false);
            } else {
                cancelPickerAnimation();
                removePicker();
            }
        }

        private void cancelPickerAnimation() {
            if (pickerAnimator != null) {
                pickerAnimator.removeAllListeners();
                pickerAnimator.cancel();
                pickerAnimator = null;
            }
        }

        private void animatePicker(boolean visible) {
            cancelPickerAnimation();
            pickerVisible = visible;
            pickerAnimator = ValueAnimator.ofFloat(pickerProgress, visible ? 1 : 0);
            pickerAnimator.setDuration(350);
            pickerAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
            pickerAnimator.addUpdateListener(animation -> {
                pickerProgress = (float) animation.getAnimatedValue();
                updateBottomLayout();
            });
            pickerAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    pickerAnimator = null;
                    if (!pickerVisible) removePicker();
                }
            });
            updateBottomLayout();
            pickerAnimator.start();
        }

        private void removePicker() {
            pickerVisible = false;
            pickerProgress = 0;
            pickerPanel.setVisibility(View.GONE);
            pickerPanel.removeAllViews();
            picker = null;
            updateBottomLayout();
        }

        private void updateApplyButtonVisibility(boolean animated) {
            applyButton.animate().cancel();
            float alpha = PinnedReactions.enabled(channel).get() ? 1 : 0;
            if (animated) {
                applyButton.setVisibility(View.VISIBLE);
                applyButton.animate().alpha(alpha).setDuration(350).setInterpolator(CubicBezierInterpolator.DEFAULT)
                        .withEndAction(this::updateBottomLayout).start();
            } else {
                applyButton.setAlpha(alpha);
            }
            updateBottomLayout();
        }

        private void updateBottomLayout() {
            int bottom = keyboardInset;
            FrameLayout.LayoutParams panelParams = (FrameLayout.LayoutParams) pickerPanel.getLayoutParams();
            if (panelParams.bottomMargin != bottom) {
                panelParams.bottomMargin = bottom;
                pickerPanel.setLayoutParams(panelParams);
            }
            int panelHeight = picker == null ? 0 : pickerPanel.getMeasuredHeight();
            FrameLayout.LayoutParams buttonParams = (FrameLayout.LayoutParams) applyButton.getLayoutParams();
            int buttonBottom = bottom + dp(13);
            if (buttonParams.bottomMargin != buttonBottom) {
                buttonParams.bottomMargin = buttonBottom;
                applyButton.setLayoutParams(buttonParams);
            }
            pickerPanel.setTranslationY((panelHeight + bottom) * (1 - pickerProgress));
            applyButton.setTranslationY(-panelHeight * pickerProgress);
            boolean showApplyButton = PinnedReactions.enabled(channel).get();
            applyButton.setVisibility(keyboardInset > 0 || !showApplyButton && applyButton.getAlpha() == 0 ? View.INVISIBLE : View.VISIBLE);
            listView.setPadding(0, 0, 0, bottom + (pickerVisible ? panelHeight : 0) + dp(keyboardInset > 0 || !showApplyButton ? 12 : 74));
        }

        private void updateColors() {
            setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
            editor.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            editor.setTextColor(getThemedColor(Theme.key_chat_messagePanelText));
            editor.setCursorColor(getThemedColor(Theme.key_chat_messagePanelCursor));
            editor.setHighlightColor(getThemedColor(Theme.key_chat_inTextSelectionHighlight));
            editor.setHandlesColor(getThemedColor(Theme.key_chat_TextSelectionCursor));
            applyButton.updateColors();
            if (picker != null) picker.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            preview.invalidateBackground();
        }
    }
}
