package app.crossword.yourealwaysbe.forkyz.view;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.databinding.ClueListItemBinding;
import app.crossword.yourealwaysbe.forkyz.databinding.ClueTabsBinding;
import app.crossword.yourealwaysbe.forkyz.databinding.ClueTabsPageBinding;
import app.crossword.yourealwaysbe.forkyz.settings.ClueTabsDouble;
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.forkyz.util.ColorUtils;
import app.crossword.yourealwaysbe.forkyz.view.BoardEditView.BoardClickListener;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.util.WeakSet;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.inject.Inject;

@AndroidEntryPoint
public class ClueTabs extends ConstraintLayout
                      implements Playboard.PlayboardListener {
    private static final Logger LOG
        = Logger.getLogger("app.crossword.yourealwaysbe");

    /**
     * How wide before enable multiple clue lists
     */
    private static final float DOUBLE_UP_WIDTH_INCH = 3.0F;

    public static enum PageType {
        CLUES, HISTORY;
    }

    @Inject
    protected ForkyzSettings settings;

    private ClueTabsBinding binding;
    private Playboard board;
    private boolean listening = false;
    private Set<ClueTabsListener> listeners = WeakSet.buildSet();
    private boolean forceSnap = false;
    private List<String> listNames = new ArrayList<>();
    private boolean showWords = false;
    private float maxWordScale = 0.9F;
    private String onClueClickDescription = null;
    private String onClueLongClickDescription = null;
    private String onBarLongClickDescription = null;
    private String onClueBoardClickDescription = null;

    // workaround for making setPage work with ViewPager2 quirks
    private PagerState[] pagerStates = { new PagerState(), new PagerState() };

    public static interface ClueTabsListener {
        /**
         * When the user clicks a clue
         *
         * @param clue the clue clicked
         * @param view the view calling
         */
        default void onClueTabsClick(
            Clue clue, ClueTabs view
        ) { }

        /**
         * When the user long-presses a clue
         *
         * @param clue the clue clicked
         * @param view the view calling
         */
        default void onClueTabsLongClick(
            Clue clue, ClueTabs view
        ) { }

        /**
         * When the user clicks the board view of a clue
         *
         * @param clue the clue clicked
         * @param previousWord the previously selected word according to
         * onPlayboardChange
         * @param view the view calling
         */
        default void onClueTabsBoardClick(
            Clue clue, Word previousWord, ClueTabs view
        ) { }

        /**
         * When the user swipes up on the tab bar
         *
         * @param view the view calling
         */
        default void onClueTabsBarSwipeUp(ClueTabs view) { }

        /**
         * When the user swipes down on the tab bar
         *
         * @param view the view calling
         */
        default void onClueTabsBarSwipeDown(ClueTabs view) { }

        /**
         * When the user swipes down on the tab bar
         *
         * @param view the view calling
         */
        default void onClueTabsBarLongclick(ClueTabs view) { }
    }

    public ClueTabs(Context context, AttributeSet as) {
        super(context, as);
        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = ClueTabsBinding.inflate(inflater, this, true);

        binding.clueTabsPager0.addOnLayoutChangeListener(
            (View v,
             int left, int top, int right, int bottom,
             int oldLeft, int oldTop, int oldRight, int oldBottom) -> {
                onPagerSizeChange(0, right - left, bottom - top);
            }
        );
        binding.clueTabsPager1.addOnLayoutChangeListener(
            (View v,
             int left, int top, int right, int bottom,
             int oldLeft, int oldTop, int oldRight, int oldBottom) -> {
                onPagerSizeChange(1, right - left, bottom - top);
            }
        );
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // no lifecycle owner until attached
        settings.liveClueListClueTabsDouble().observe(
            ViewTreeLifecycleOwner.get(this),
            value -> { setDouble(); }
        );
    }

    public void setOnClueClickDescription(String description) {
        this.onClueClickDescription = description;
    }

    public void setOnClueLongClickDescription(String description) {
        this.onClueLongClickDescription = description;
    }

    public void setOnBarLongClickDescription(String description) {
        this.onBarLongClickDescription = description;
    }

    public void setOnClueBoardClickDescription(String description) {
        this.onClueBoardClickDescription = description;
    }

    /**
     * Show words beneath each clue in list
     */
    public void setShowWords(boolean showWords) {
        boolean changed = this.showWords != showWords;
        this.showWords = showWords;
        if (changed)
            refresh();
    }

    /**
     * Maximum size of words as a scale of default size
     *
     * Default 0.9
     */
    public void setMaxWordScale(float maxWordScale) {
        this.maxWordScale = maxWordScale;
    }

    /**
     * Does nothing if the same board is already set
     */
    public void setBoard(Playboard board) {
        if (board == null)
            return;

        // same board, nothing to do (avoid rebuilding adapters and
        // losing position)
        if (board == this.board)
            return;

        // ignore old board if there was one
        unlistenBoard();
        listNames.clear();

        this.board = board;
        Puzzle puz = board.getPuzzle();

        if (puz == null)
            return;

        listNames.addAll(puz.getClueListNames());
        Collections.sort(listNames);

        setupPager(binding.clueTabsPager0, binding.clueTabsTabLayout0, 0);
        setupPager(binding.clueTabsPager1, binding.clueTabsTabLayout1, 1);

        listenBoard();
    }

    /**
     * Set the page of the viewIdx-th clue tab
     *
     * Currently only two clue tab views, so viewIdx should be 0 or 1.
     */
    public void setPage(int viewIdx, int pageNumber) {
        // This is very hairy trying to deal with the asynchronicity and
        // quirks of ViewPager2. Cannot set the page too early in the
        // view pager setup, so need to save a pending change. But then
        // (see onSizeChange) it will reset to the initial page if the
        // size gets to 0, so need to save and restore states. Lots of
        // post() requests all need to line up.

        ViewPager2 pager = getPager(viewIdx);
        PagerState state = getPagerState(viewIdx);
        if (pager == null || state == null)
            return;

        if (state.pagerInitialised)
            pager.setCurrentItem(pageNumber, false);
        else
            state.pendingSetPage = pageNumber;

        // clobber any saved state as it is no longer relevant
        state.savedCurrentPage = -1;
    }

    /**
     * Returns -1 if oob
     */
    public int getCurrentPage(int viewIdx) {
        PagerState state = getPagerState(viewIdx);
        if (state == null)
            return -1;

        if (state.savedCurrentPage >= 0) {
            return state.savedCurrentPage;
        } else if (!state.pagerInitialised && state.pendingSetPage >= 0) {
            return state.pendingSetPage;
        } else {
            ViewPager2 pager = getPager(viewIdx);
            return (pager == null) ? -1 : pager.getCurrentItem();
        }
    }

    /**
     * Ensure list is shown
     *
     * If the listNum-th is not shown, then update the panel showing
     * replaceListNum with listNum (or the first panel if neither).
     * replaceListNum can be -1 if no replace.
     */
    public void ensureListShown(int listNum, int replaceListNum) {
        int curList0 = getCurrentPage(0);
        if (listNum == curList0)
            return;

        boolean pager1Visible = isPagerVisible(1);
        int curList1 = getCurrentPage(1);

        if (pager1Visible) {
            if (curList1 == listNum)
                return;

            if (curList1 == replaceListNum) {
                setPage(1, listNum);
                return;
            }
        }

        // change if replaceListNum matches or not
        setPage(0, listNum);
    }

    /**
     * Get number of clue list, or -1
     */
    public int getListNum(String listName) {
        return listNames.indexOf(listName);
    }

    /**
     * Return the type of the page at listNum (or null if oob)
     */
    public PageType getPageType(int listNum) {
        if (listNum == getHistoryListNum())
            return PageType.HISTORY;
        else
            return PageType.CLUES;
    }

    /**
     * Name of list or null if oob or history
     */
    public String getPageListName(int listNum) {
        if (listNum < 0 || listNum >= listNames.size())
            return null;
        else
            return listNames.get(listNum);
    }

    /**
     * Get index of the list to the right
     *
     * Does not wrap, but stops at ends
     */
    public int getNextListNum(int listNum) {
        return Math.min(listNum + 1, getNumLists() - 1);
    }

    /**
     * Get index of the list to the left
     *
     * Does not wrap, but stops at ends
     */
    public int getPrevListNum(int listNum) {
        return Math.max(listNum - 1, 0);
    }

    /**
     * Always snap to clue if true, else follow prefs
     */
    public void setForceSnap(boolean forceSnap) {
        this.forceSnap = forceSnap;
    }

    public void addListener(ClueTabsListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ClueTabsListener listener) {
        listeners.remove(listener);
    }

    public void onResume() {

    }

    public void onPause() {

    }

    public void onPlayboardChange(PlayboardChanges changes) {
        isSnapToClue(snapToClue -> {
            if (!snapToClue)
                return;

            ClueID cid = board.getClueID();
            String listName = (cid == null) ? null : cid.getListName();

            if (listName == null)
                return;

            int listIndex = listNames.indexOf(listName);
            if (listIndex < 0)
                return;

            int historyListNum = getHistoryListNum();

            int curPage0 = getCurrentPage(0);
            boolean listShown0
                = curPage0 == listIndex || curPage0 == historyListNum;

            int curPage1 = getCurrentPage(1);
            boolean listShown1
                = isPagerVisible(1) && (
                    curPage1 == listIndex || curPage1 == historyListNum
                );

            if (!listShown0 && !listShown1) {
                binding.clueTabsPager0.setCurrentItem(listIndex);
            }
        });
    }

    @Override
    protected void onSizeChanged(
        int newWidth, int newHeight, int oldWidth, int oldHeight
    ) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        if (newWidth > 0 && newHeight > 0) {
            post(() -> { setDouble(); });
        }

        // save pages for layout change
        // layout change of size 0 doesn't get called until the
        // pages are visible again.
        // Isn't Android great!
        for (int i = 0; i < pagerStates.length; i++) {
            pagerStates[i].pageOnLastSizeChange = getCurrentPage(i);
        }
    }

    private void onPagerSizeChange(int viewIdx, int width, int height) {
        // Size changes seem to reset view pagers to first page when the
        // view becomes 0-sized. Need to save/restore. I suspect there
        // is a proper way to do this, but i don't know what it is.
        PagerState state = getPagerState(viewIdx);
        if (state == null)
            return;

        if (width > 0 && height > 0) {
            if (state.savedCurrentPage >= 0) {
                post(() -> {
                    int page = state.savedCurrentPage;
                    // might have been overwritten by setPage
                    if (page >= 0)
                        setPage(viewIdx, state.savedCurrentPage);
                    state.savedCurrentPage = -1;
                });
            }
        } else {
            // because the layout change is too late and the current
            // page has changed already, use the page saved on last size
            // which is called early enough
            state.savedCurrentPage = state.pageOnLastSizeChange;
        }
    }

    private void setDouble() {
        int visible = isDouble() ? View.VISIBLE : View.GONE;
        binding.clueTabsPager1.setVisibility(visible);
        binding.clueTabsTabLayout1.setVisibility(visible);

        int csVisible = isDouble() ? ConstraintSet.VISIBLE : ConstraintSet.GONE;
        ConstraintSet set = new ConstraintSet();
        set.clone(binding.constraintLayout);
        set.setVisibility(binding.clueTabsPager1.getId(), csVisible);
        set.setVisibility(binding.clueTabsTabLayout1.getId(), csVisible);
        set.applyTo(binding.constraintLayout);
    }

    /**
     * If double clue tabs
     */
    private boolean isDouble() {
        ClueTabsDouble dbl = settings.liveClueListClueTabsDouble().getValue();
        if (dbl == null)
            return false;

        switch(dbl) {
        case ALWAYS:
            return true;
        case LANDSCAPE:
            int orientation = getResources().getConfiguration().orientation;
            return orientation == Configuration.ORIENTATION_LANDSCAPE;
        case WIDE:
            DisplayMetrics metrics
                = getContext().getResources().getDisplayMetrics();
            return metrics.densityDpi * DOUBLE_UP_WIDTH_INCH < getMeasuredWidth();
        case NEVER:
        default:
            return false;
        }
    }

    /**
     * Pagers being remade, so clear any pending/setup info
     */
    private void resetPagerInitialiseState(int viewIdx) {
        if (viewIdx < 0 || viewIdx > pagerStates.length)
            return;
        pagerStates[viewIdx] = new PagerState();
    }

    /**
     * Setup one of the clue tabs windows
     *
     * @param viewIdx whether it's the first or second set of tabs and pager
     */
    private void setupPager(ViewPager2 pager, TabLayout tabs, int viewIdx) {
        ClueTabsPagerAdapter adapter = new ClueTabsPagerAdapter(viewIdx);

        resetPagerInitialiseState(viewIdx);

        pager.setAdapter(adapter);

        new TabLayoutMediator(
            tabs,
            pager,
            (tab, position) -> { tab.setText(adapter.getPageTitle(position)); }
        ).attach();

        setTabLayoutOnTouchListener(tabs);

        LinearLayout tabStrip = (LinearLayout) tabs.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View view = tabStrip.getChildAt(i);
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClueTabs.this.notifyListenersTabsBarLongClick();
                    return true;
                }
            });
            if (onBarLongClickDescription != null) {
                ViewCompat.replaceAccessibilityAction(
                    view,
                    AccessibilityActionCompat.ACTION_LONG_CLICK,
                    onBarLongClickDescription,
                    null
                );
            }
        }

        pager.setCurrentItem(viewIdx % adapter.getItemCount(), false);
    }

    /**
     * Full refresh of view
     */
    @SuppressLint("NotifyDataSetChanged")
    private void refresh() {
        if (binding.clueTabsPager0.getAdapter() != null)
            binding.clueTabsPager0.getAdapter().notifyDataSetChanged();
        if (binding.clueTabsPager1.getAdapter() != null)
            binding.clueTabsPager1.getAdapter().notifyDataSetChanged();
    }

    private void isSnapToClue(Consumer<Boolean> cb) {
        if (forceSnap)
            cb.accept(true);
        else
            settings.getClueListSnapToClue(cb::accept);
    }

    private void notifyListenersClueClick(Clue clue) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsClick(clue, this);
    }

    private void notifyListenersClueBoardClick(Clue clue, Word previousWord) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBoardClick(clue, previousWord, this);
    }

    private void notifyListenersClueLongClick(Clue clue) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsLongClick(clue, this);
    }

    private void notifyListenersTabsBarSwipeUp() {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBarSwipeUp(this);
    }

    private void notifyListenersTabsBarSwipeDown() {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBarSwipeDown(this);
    }

    private void notifyListenersTabsBarLongClick() {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsBarSwipeDown(this);
    }

    private void listenBoard() {
        if (board != null && !listening) {
            board.addListener(this);
            listening = true;
        }
    }

    private void unlistenBoard() {
        if (board != null && listening) {
            board.removeListener(this);
            listening = false;
        }
    }

    private void onClueTabsPagerBind(int viewIdx) {
        PagerState state = getPagerState(viewIdx);
        if (state != null && !state.pagerInitialised) {
            state.pagerInitialised = true;
            if (state.pendingSetPage >= 0) {
                post(() -> {
                    setPage(viewIdx, state.pendingSetPage);
                    state.pendingSetPage = -1;
                });
            }
        }
    }

    private class ClueTabsPagerAdapter
            extends RecyclerView.Adapter<ClueListHolder> {

        public int pagerIndex;

        /**
         * Construct adapter for the pager at given index
         */
        public ClueTabsPagerAdapter(int pagerIndex) {
            this.pagerIndex = pagerIndex;
        }

        @Override
        public ClueListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ClueTabsPageBinding pageBinding = ClueTabsPageBinding.inflate(
                inflater, parent, false
            );
            return new ClueListHolder(pageBinding);
        }

        @Override
        public void onBindViewHolder(ClueListHolder holder, int position) {
            holder.setContents(getPageType(position), getPageTitle(position));
            onClueTabsPagerBind(pagerIndex);
        }

        public String getPageTitle(int position) {
            Context context = getContext();
            if (position < 0 || position > getNumLists())
                return null;
            else if (position == getHistoryListNum())
                return context.getString(R.string.clue_tab_history);
            else
                return listNames.get(position);
        }

        @Override
        public int getItemCount() {
            return getNumLists();
        }
    }

    private class ClueListHolder
            extends RecyclerView.ViewHolder
            implements Playboard.PlayboardListener {

        private ClueTabsPageBinding pageBinding;
        private ClueListAdapter clueListAdapter;
        private LinearLayoutManager layoutManager;
        private PageType pageType;
        private String listName;

        // used to follow moved clue in history list when selected
        private RecyclerView.AdapterDataObserver historyObserver
            = new RecyclerView.AdapterDataObserver() {
                public void onItemRangeInserted(int start, int count) {
                    boolean atStart
                        = layoutManager.findFirstVisibleItemPosition() == 0;
                    if (start == 0 && atStart)
                        layoutManager.scrollToPositionWithOffset(0, 0);
                }

                public void onItemRangeMoved(int from, int to, int count) {
                    boolean atStart
                        = layoutManager.findFirstVisibleItemPosition() == 0;
                    if (to == 0 && atStart)
                        layoutManager.scrollToPositionWithOffset(0, 0);
                }
            };

        public ClueListHolder(ClueTabsPageBinding pageBinding) {
            super(pageBinding.getRoot());

            this.pageBinding = pageBinding;

            Context context = itemView.getContext();
            layoutManager = new LinearLayoutManager(context);
            pageBinding.tabClueList.setLayoutManager(layoutManager);
            pageBinding.tabClueList.setItemAnimator(new DefaultItemAnimator());
            pageBinding.tabClueList.addItemDecoration(new DividerItemDecoration(
                context, DividerItemDecoration.VERTICAL
            ));

            if (ClueTabs.this.board != null)
                ClueTabs.this.board.addListener(this);
        }

        @Override
        public void onPlayboardChange(PlayboardChanges changes) {
            boolean wordChange = !Objects.equals(
                changes.getPreviousWord(), changes.getCurrentWord()
            );
            if (wordChange)
                snapToClueIfEnabled();
        }

        /**
         * List name only used when pageType is CLUES
         */
        @SuppressLint("NotifyDataSetChanged")
        public void setContents(PageType pageType, String listName) {
            Playboard board = ClueTabs.this.board;
            Puzzle puz = board.getPuzzle();

            boolean changed = (
                this.pageType != pageType
                || !Objects.equals(this.listName, listName)
            );

            if (board != null && changed) {
                // remove old observer if there was one
                if (clueListAdapter != null) {
                    if (this.pageType == PageType.HISTORY) {
                        clueListAdapter.unregisterAdapterDataObserver(
                            historyObserver
                        );
                    }
                }

                switch (pageType) {
                case CLUES:
                    if (puz != null) {
                        clueListAdapter = new PuzzleListAdapter(
                            listName, puz.getClues(listName)
                        );
                    } else {
                        // easier to create an empty history list that
                        // puzzle clue list
                        clueListAdapter = new HistoryListAdapter(
                            new LinkedList<>()
                        );
                    }
                    break;

                case HISTORY:
                    if (puz != null) {
                        clueListAdapter
                            = new HistoryListAdapter(puz.getHistory());
                        clueListAdapter.registerAdapterDataObserver(
                            historyObserver
                        );
                    } else {
                        clueListAdapter
                            = new HistoryListAdapter(new LinkedList<>());
                    }
                    break;
                }

                pageBinding.tabClueList.setAdapter(clueListAdapter);
                this.pageType = pageType;
                this.listName = listName;
            }

            clueListAdapter.notifyDataSetChanged();

            snapToClueIfEnabled();
        }

        private void snapToClueIfEnabled() {
            Playboard board = ClueTabs.this.board;
            if (board != null) {
                isSnapToClue(snapToClue -> {
                    if (snapToClue) {
                        switch (pageType) {
                        case CLUES:
                            String curList = board.getCurrentClueListName();
                            if (Objects.equals(listName, curList)) {
                                int position = board.getCurrentClueIndex();
                                layoutManager.scrollToPosition(position);
                            }
                            break;
                        case HISTORY:
                            layoutManager.scrollToPosition(0);
                            break;
                        }
                    }
                });
            }
        }
    }

    private abstract class ClueListAdapter
            extends RecyclerView.Adapter<ClueViewHolder>
            implements Playboard.PlayboardListener {

        boolean showDirection;

        public ClueListAdapter(boolean showDirection) {
            this.showDirection = showDirection;

            if (ClueTabs.this.board != null)
                ClueTabs.this.board.addListener(this);
        }

        @Override
        public ClueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            ClueListItemBinding clueBinding = ClueListItemBinding.inflate(
                inflater, parent, false
            );
            return new ClueViewHolder(clueBinding, showDirection);
        }

        @Override
        public void onViewRecycled(ClueViewHolder holder) {
            holder.setClue(null);
        }
    }

    private class PuzzleListAdapter extends ClueListAdapter {
        private ClueList clueList;
        private List<Clue> rawClueList;
        private String listName;

        public PuzzleListAdapter(String listName, ClueList clueList) {
            super(false);
            this.listName = listName;
            this.clueList = clueList;
            this.rawClueList = new ArrayList<Clue>(clueList.getClues());
        }

        @Override
        public void onBindViewHolder(ClueViewHolder holder, int position) {
            Clue clue = rawClueList.get(position);
            holder.setClue(clue);
        }

        @Override
        public int getItemCount() {
            return clueList.size();
        }

        @Override
        public void onPlayboardChange(PlayboardChanges changes) {
            // do nothing
        }
    }

    public class HistoryListAdapter
           extends ClueListAdapter {

        private List<ClueID> historyList;

        public HistoryListAdapter(List<ClueID> historyList) {
            super(true);
            this.historyList = historyList;
        }

        @Override
        public void onBindViewHolder(ClueViewHolder holder, int position) {
            ClueID item = historyList.get(position);
            Playboard board = ClueTabs.this.board;
            if (board != null) {
                Puzzle puz = board.getPuzzle();
                Clue clue = puz.getClue(item);
                if (puz != null && clue != null) {
                    holder.setClue(clue);
                }
            }
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        @Override
        public void onPlayboardChange(PlayboardChanges changes) {
            if (changes.isHistoryChange()) {
                int lastIndex = changes.getLastHistoryIndex();
                if (lastIndex < 0)
                    notifyItemInserted(0);
                else if (lastIndex != 0)
                    notifyItemMoved(lastIndex, 0);
            }
        }
    }

    private class ClueViewHolder
            extends RecyclerView.ViewHolder
            implements Playboard.PlayboardListener {

        private ClueListItemBinding clueBinding;
        private Clue clue;
        private boolean showDirection;

        public ClueViewHolder(
            ClueListItemBinding clueBinding, boolean showDirection
        ) {
            super(clueBinding.getRoot());
            this.clueBinding = clueBinding;
            this.showDirection = showDirection;

            clueBinding.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ClueTabs.this.notifyListenersClueClick(clue);
                }
            });
            if (onClueClickDescription != null) {
                ViewCompat.replaceAccessibilityAction(
                    clueBinding.getRoot(),
                    AccessibilityActionCompat.ACTION_CLICK,
                    onClueClickDescription,
                    null
                );
            }

            clueBinding.getRoot().setOnLongClickListener(
                new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        ClueTabs.this.notifyListenersClueLongClick(clue);
                        return true;
                    }
                }
            );
            if (onClueLongClickDescription != null) {
                ViewCompat.replaceAccessibilityAction(
                    clueBinding.getRoot(),
                    AccessibilityActionCompat.ACTION_LONG_CLICK,
                    onClueLongClickDescription,
                    null
                );
            }

            // assume gone unless proven otherwise
            clueBinding.miniboard.setVisibility(View.GONE);
            // potential memory leak as click listener has a strong
            // reference in the board view, but the board view lives as
            // long as this holder afaik
            clueBinding.miniboard.addBoardClickListener(
                new BoardClickListener() {
                    @Override
                    public void onClick(Position position, Word previousWord) {
                        ClueTabs.this.notifyListenersClueBoardClick(
                            clue, previousWord
                        );
                    }

                    @Override
                    public void onLongClick(Position position) {
                        ClueTabs.this.notifyListenersClueLongClick(clue);
                    }
                }
            );
            if (onClueBoardClickDescription != null) {
                ViewCompat.replaceAccessibilityAction(
                    clueBinding.miniboard,
                    AccessibilityActionCompat.ACTION_CLICK,
                    onClueBoardClickDescription,
                    null
                );
            }
            if (onClueLongClickDescription != null) {
                ViewCompat.replaceAccessibilityAction(
                    clueBinding.miniboard,
                    AccessibilityActionCompat.ACTION_LONG_CLICK,
                    onClueLongClickDescription,
                    null
                );
            }

            // so we can refresh if changes
            settings.livePlayShowCount().observe(
                ViewTreeLifecycleOwner.get(binding.getRoot()),
                showCount -> { setClueText(); }
            );
        }

        /**
         * Set to null to "deactivate" view from board listening
         */
        public void setClue(Clue clue) {
            this.clue = clue;

            if (clue == null) {
                clueBinding.miniboard.detach();
            } else {
                Playboard board = ClueTabs.this.board;

                setClueText();

                if (board != null)
                    board.addListener(this);

                setStyle();

                if (showWords && board != null) {
                    Word word = board.getClueWord(clue.getClueID());
                    // suppress board render until we set the word later
                    clueBinding.miniboard.setBoard(board, true);
                    clueBinding.miniboard.setMaxScale(maxWordScale);
                    clueBinding.miniboard.setWord(word);
                    clueBinding.miniboard.setVisibility(
                        word == null ? View.GONE : View.VISIBLE
                    );
                } else {
                    clueBinding.miniboard.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onPlayboardChange(PlayboardChanges changes) {
            Playboard board = ClueTabs.this.board;
            if (board == null)
                return;

            ClueID thisClueID = (clue == null) ? null : clue.getClueID();
            if (thisClueID == null)
                return;

            Word currentWord = changes.getCurrentWord();
            Word previousWord = changes.getPreviousWord();

            ClueID curClueID
                = (currentWord == null) ? null : currentWord.getClueID();
            ClueID prevClueID
                = (previousWord == null) ? null : previousWord.getClueID();

            boolean refresh =
                Objects.equals(thisClueID, curClueID)
                || Objects.equals(thisClueID, prevClueID);

            if (refresh)
                setStyle();

            if (changes.getChangedClueIDs().contains(thisClueID)) {
                setClueText();
                setStyle();
            }
        }

        private void setClueText() {
            if (clue == null) {
                clueBinding.clueTextView.setText("");
            } else {
                getClueText(clue, text -> {
                    clueBinding.clueTextView.setText(
                        HtmlCompat.fromHtml(text, 0)
                    );
                });
            }
        }

        private void setStyle() {
            Playboard board = ClueTabs.this.board;
            if (board == null)
                return;

            ColorStateList colors = clueBinding.clueTextView.getTextColors();
            int alpha
                = ClueTabs.this.getContext().getResources().getInteger(
                    board.isFilledClueID(clue.getClueID())
                    ? R.integer.filled_clue_alpha
                    : R.integer.unfilled_clue_alpha
                );
            clueBinding.clueTextView.setTextColor(colors.withAlpha(alpha));

            ClueID cid = board.getClueID();
            boolean selected = Objects.equals(clue.getClueID(), cid);
            clueBinding.clueTextView.setChecked(selected);

            Puzzle puz = board.getPuzzle();
            if (puz != null && clue.isFlagged()) {
                clueBinding.clueFlagView.setVisibility(View.VISIBLE);
                if (clue.isDefaultFlagColor()) {
                    clueBinding.clueFlagView.setBackgroundColor(
                        ContextCompat.getColor(
                            ClueTabs.this.getContext(), R.color.flagColor
                        )
                    );
                } else {
                    clueBinding.clueFlagView.setBackgroundColor(
                        ColorUtils.addAlpha(clue.getFlagColor())
                    );
                }
            } else {
                clueBinding.clueFlagView.setVisibility(View.INVISIBLE);
            }
        }

        private void getClueText(Clue clue, Consumer<String> cb) {
            settings.getPlayShowCount(showCount -> {
                String listName = getShortListName(clue);
                String displayNum = clue.getDisplayNumber();
                String hint = clue.getHint();
                boolean hasCount = clue.hasZone();
                int count = hasCount ? clue.getZone().size() : -1;

                if (showDirection) {
                    if (hasCount && showCount) {
                        if (displayNum != null) {
                            cb.accept(ClueTabs.this.getContext().getString(
                                R.string.clue_format_short_with_count,
                                displayNum, listName, hint, count
                            ));
                        } else {
                            cb.accept(ClueTabs.this.getContext().getString(
                                R.string.clue_format_short_no_num_with_count,
                                listName, hint, count
                            ));
                        }
                    } else {
                        if (displayNum != null) {
                            cb.accept(ClueTabs.this.getContext().getString(
                                R.string.clue_format_short,
                                displayNum, listName, hint
                            ));
                        } else {
                            cb.accept(ClueTabs.this.getContext().getString(
                                R.string.clue_format_short_no_num,
                                listName, hint
                            ));
                        }
                    }
                } else {
                    if (hasCount && showCount) {
                        if (displayNum != null) {
                            cb.accept(ClueTabs.this.getContext().getString(
                                R.string.clue_format_short_no_dir_with_count,
                                displayNum, hint, count
                            ));
                        } else {
                            cb.accept(ClueTabs.this.getContext().getString(
                                R.string.clue_format_short_no_num_no_dir_with_count,
                                hint, count
                            ));
                        }
                    } else {
                        if (displayNum != null) {
                            cb.accept(ClueTabs.this.getContext().getString(
                                R.string.clue_format_short_no_dir,
                                displayNum, hint
                            ));
                        } else {
                            cb.accept(ClueTabs.this.getContext().getString(
                                R.string.clue_format_short_no_num_no_dir,
                                hint
                            ));
                        }
                    }
                }
            });
        }

        private String getShortListName(Clue clue) {
            String listName = clue.getClueID().getListName();
            if (listName == null || listName.isEmpty())
                return "";
            else
                return listName.substring(0,1)
                    .toLowerCase(Locale.getDefault());
        }
    }

    // suppress because the swipe detector does not consume clicks
    @SuppressWarnings("ClickableViewAccessibility")
    private void setTabLayoutOnTouchListener(TabLayout tabs) {
        OnGestureListener tabSwipeListener
            = new SimpleOnGestureListener() {
                // as recommended by the docs
                // https://developer.android.com/training/gestures/detector
                public boolean onDown(MotionEvent e) {
                    return true;
                }

                public boolean onFling(MotionEvent e1,
                                       MotionEvent e2,
                                       float velocityX,
                                       float velocityY) {
                    if (Math.abs(velocityY) < Math.abs(velocityX))
                        return false;

                    if (velocityY > 0)
                        ClueTabs.this.notifyListenersTabsBarSwipeDown();
                    else
                        ClueTabs.this.notifyListenersTabsBarSwipeUp();

                    return true;
                }
            };

        GestureDetector tabSwipeDetector = new GestureDetector(
            tabs.getContext(), tabSwipeListener
        );

        tabs.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                return tabSwipeDetector.onTouchEvent(e);
            }
        });
    }

    /**
     * Get pager state or null if oob
     */
    private PagerState getPagerState(int viewIdx) {
        return (viewIdx >= 0 && viewIdx < pagerStates.length)
            ? pagerStates[viewIdx]
            : null;
    }

    /**
     * null if oob
     */
    private ViewPager2 getPager(int viewIdx) {
        switch (viewIdx) {
        case 0:
            return binding.clueTabsPager0;
        case 1:
            return binding.clueTabsPager1;
        default:
            return null;
        }
    }

    private int getNumLists() {
        // plus one for history
        return listNames.size() + 1;
    }

    private int getHistoryListNum() {
        // always the last
        return getNumLists() - 1;
    }

    private boolean isPagerVisible(int viewIdx) {
        switch (viewIdx) {
        case 0: return binding.clueTabsPager0.getVisibility() == View.VISIBLE;
        case 1: return binding.clueTabsPager1.getVisibility() == View.VISIBLE;
        default: return false;
        }
    }

    /**
     * Used for managing odd behaviour of ViewPager2
     *
     * Can't setCurrentItem until some layout stuff complete. Resets to
     * page 0 on show/hide. Need to workaround this.
     */
    private static class PagerState {
        // if the view has drawn and setCurrentItem will work
        public boolean pagerInitialised = false;
        // if a setPage is waiting for init (-1 if not)
        public int pendingSetPage = -1;
        // save current page if view was hidden (or -1)
        public int savedCurrentPage = -1;
        // because onLayoutChange doesn't get called until page has been
        // reset...
        public int pageOnLastSizeChange = -1;

        public String toString() {
            return "PagerState("
                + "pagerInitialised=" + pagerInitialised + ","
                + "pendingSetPage=" + pendingSetPage + ","
                + "savedCurrentPage=" + savedCurrentPage
                + "savedCurrentPage=" + savedCurrentPage
                + ")";
        }
    }
}

