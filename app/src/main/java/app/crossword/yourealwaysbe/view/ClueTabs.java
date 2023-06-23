package app.crossword.yourealwaysbe.view;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.databinding.ClueListItemBinding;
import app.crossword.yourealwaysbe.forkyz.databinding.ClueTabsBinding;
import app.crossword.yourealwaysbe.forkyz.databinding.ClueTabsPageBinding;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.settings.ForkyzSettings;
import app.crossword.yourealwaysbe.util.WeakSet;
import app.crossword.yourealwaysbe.view.BoardEditView.BoardClickListener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.core.text.HtmlCompat;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

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
public class ClueTabs extends LinearLayout
                      implements Playboard.PlayboardListener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

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

        /**
         * When the user changes the page being viewed
         *
         * @param view the view calling
         */
        default void onClueTabsPageChange(ClueTabs view, int pageNumber) { }
    }

    public ClueTabs(Context context, AttributeSet as) {
        super(context, as);
        LayoutInflater inflater = (LayoutInflater)
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = ClueTabsBinding.inflate(inflater, this, true);
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

        final ClueTabsPagerAdapter adapter = new ClueTabsPagerAdapter();

        binding.clueTabsPager.setAdapter(adapter);

        binding.clueTabsPager.registerOnPageChangeCallback(
            new ViewPager2.OnPageChangeCallback() {
                public void onPageSelected(int position) {
                    ClueTabs.this.notifyListenersPageChanged(position);
                }
            }
        );

        new TabLayoutMediator(
            binding.clueTabsTabLayout,
            binding.clueTabsPager,
            (tab, position) -> { tab.setText(adapter.getPageTitle(position)); }
        ).attach();

        setTabLayoutOnTouchListener();

        LinearLayout tabStrip = (LinearLayout) binding.clueTabsTabLayout.getChildAt(0);
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

        listenBoard();
    }

    public void setPage(int pageNumber) {
        binding.clueTabsPager.setCurrentItem(pageNumber, false);
    }

    public PageType getCurrentPageType() {
        int curPage = binding.clueTabsPager.getCurrentItem();
        ClueTabsPagerAdapter adapter
            = (ClueTabsPagerAdapter) binding.clueTabsPager.getAdapter();
        return adapter.getPageType(curPage);
    }

    /**
     * Name of current list or null if oob or history
     */
    public String getCurrentPageListName() {
        int curPage = binding.clueTabsPager.getCurrentItem();
        ClueTabsPagerAdapter adapter
            = (ClueTabsPagerAdapter) binding.clueTabsPager.getAdapter();
        return adapter.getPageListName(curPage);
    }

    public void nextPage() {
        int curPage = binding.clueTabsPager.getCurrentItem();
        int numPages = binding.clueTabsPager.getAdapter().getItemCount();
        binding.clueTabsPager.setCurrentItem((curPage + 1) % numPages, false);
    }

    public void prevPage() {
        int curPage = binding.clueTabsPager.getCurrentItem();
        int numPages = binding.clueTabsPager.getAdapter().getItemCount();
        int nextPage = curPage == 0 ? numPages - 1 : curPage - 1;
        binding.clueTabsPager.setCurrentItem(nextPage, false);
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

            binding.clueTabsPager.setCurrentItem(listIndex);
        });
    }

    /**
     * Full refresh of view
     */
    @SuppressLint("NotifyDataSetChanged")
    private void refresh() {
        if (binding.clueTabsPager.getAdapter() != null)
            binding.clueTabsPager.getAdapter().notifyDataSetChanged();
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

    private void notifyListenersPageChanged(int pageNumber) {
        for (ClueTabsListener listener : listeners)
            listener.onClueTabsPageChange(this, pageNumber);
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

    private class ClueTabsPagerAdapter extends RecyclerView.Adapter<ClueListHolder> {
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
        }

        public PageType getPageType(int position) {
            if (position == listNames.size())
                return PageType.HISTORY;
            else
                return PageType.CLUES;
        }

        public String getPageTitle(int position) {
            Context context = getContext();
            if (position < 0 || position > listNames.size())
                return null;
            else if (position == listNames.size())
                return context.getString(R.string.clue_tab_history);
            else
                return listNames.get(position);
        }

        /**
         * Name of current list or null if history or oob
         */
        public String getPageListName(int position) {
            if (position < 0 || position >= listNames.size())
                return null;
            else
                return listNames.get(position);
        }

        @Override
        public int getItemCount() {
            return listNames.size() + 1;
        }
    }

    private class ClueListHolder
            extends RecyclerView.ViewHolder {

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

            if (board != null) {
                isSnapToClue(snapToClue -> {
                    if (snapToClue) {
                        switch (pageType) {
                        case CLUES:
                            int position = board.getCurrentClueIndex();
                            layoutManager.scrollToPositionWithOffset(position, 0);
                            break;
                        case HISTORY:
                            layoutManager.scrollToPositionWithOffset(0, 0);
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
            if (puz != null && puz.isFlagged(clue)) {
                clueBinding.clueFlagView.setVisibility(View.VISIBLE);
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
    private void setTabLayoutOnTouchListener() {
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

        GestureDetectorCompat tabSwipeDetector = new GestureDetectorCompat(
            binding.clueTabsTabLayout.getContext(), tabSwipeListener
        );

        binding.clueTabsTabLayout.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                return tabSwipeDetector.onTouchEvent(e);
            }
        });
    }
}

