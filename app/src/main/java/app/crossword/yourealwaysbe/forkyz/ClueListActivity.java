package app.crossword.yourealwaysbe.forkyz;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;

import app.crossword.yourealwaysbe.forkyz.databinding.ClueListBinding;
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManager;
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand;
import app.crossword.yourealwaysbe.forkyz.view.BoardEditView.BoardClickListener;
import app.crossword.yourealwaysbe.forkyz.view.ClueTabs;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

public class ClueListActivity extends PuzzleActivity
                              implements ClueTabs.ClueTabsListener {
    private static final Logger LOG = Logger.getLogger(
        ClueListActivity.class.getCanonicalName()
    );

    private final static String STATE_CURRENT_LIST_NUM = "currentListNum";

    private ClueListBinding binding;
    private KeyboardManager keyboardManager;
    private int currentListNum = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clue_list_menu_show_words) {
            toggleShowWords();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Create the activity
     *
     * This only sets up the UI widgets. The set up for the current
     * puzzle/board is done in onResume as these are held by the
     * application and may change while paused!
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info(
                "ClueListActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            return;
        }

        binding = ClueListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        holographic();
        finishOnHomeButton();
        setupSideInsets(binding.toolbar);
        setupBottomInsets(binding.content);
        setupStatusBar(binding.appBarLayout);

        binding.miniboard.setAllowOverScroll(false);

        binding.miniboard.addBoardClickListener(new BoardClickListener() {
            @Override
            public void onClick(Position position, Word previousWord) {
                displayKeyboard();
            }

            @Override
            public void onLongClick(Position position) {
                Playboard board = getBoard();
                if (board != null)
                    launchClueNotes(board.getClueID());
            }
        });
        ViewCompat.replaceAccessibilityAction(
            binding.miniboard,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.open_clue_notes),
            null
        );

        binding.clueTabs.setOnClueLongClickDescription(
            getString(R.string.open_clue_notes)
        );
        binding.clueTabs.setOnClueClickDescription(
            getString(R.string.select_clue)
        );

        keyboardManager = new KeyboardManager(
            this, settings, binding.keyboard, binding.miniboard
        );

        setupVoiceButtons(binding.voiceButtonsInclude);
        setupVoiceCommands();

        addAccessibilityActions(binding.miniboard);

        settings.getLiveClueListShowWords().observe(
            this,
            showWords -> {
                setupShowWordsMode(showWords);
                invalidateOptionsMenu();
            }
        );

        binding.clueTabs.setBoard(board);
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt(STATE_CURRENT_LIST_NUM, currentListNum);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        currentListNum = state.getInt(STATE_CURRENT_LIST_NUM, 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info(
                "ClueListActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            return;
        }

        binding.miniboard.setBoard(board);

        binding.clueTabs.setBoard(board);
        binding.clueTabs.addListener(this);

        keyboardManager.onResume();

        setVoiceButtonVisibility();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.clue_list_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean result = super.onPrepareOptionsMenu(menu);

        if (result) {
            settings.getClueListShowWords(showWords -> {
                menu.findItem(R.id.clue_list_menu_show_words)
                    .setChecked(showWords);
            });
        }

        return result;
    }

    @Override
    public void onClueTabsClick(Clue clue, ClueTabs view) {
        onClueTabsClickGeneral(clue);
    }

    @Override
    public void onClueTabsBoardClick(
        Clue clue, Word previousWord, ClueTabs view
    ) {
        onClueTabsClickGeneral(clue);
    }

    @Override
    public void onClueTabsLongClick(Clue clue, ClueTabs view) {
        Playboard board = getBoard();
        if (board == null)
            return;
        if (!Objects.equals(clue.getClueID(), board.getClueID()))
            board.jumpToClue(clue);
        launchClueNotes(clue);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = isHandledKey(keyCode, event);
        if (!handled)
            return super.onKeyDown(keyCode, event);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean handled = isHandledKey(keyCode, event);
        if (!handled)
            return super.onKeyUp(keyCode, event);

        int cancelled = event.getFlags()
            & (KeyEvent.FLAG_CANCELED | KeyEvent.FLAG_CANCELED_LONG_PRESS);
        if (cancelled > 0)
            return true;

        switch (keyCode) {
        case KeyEvent.KEYCODE_ESCAPE:
            doBackAction();
            return true;

        case KeyEvent.KEYCODE_DPAD_LEFT:
            onLeftKey();
            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            onRightKey();
            return true;

        case KeyEvent.KEYCODE_DPAD_UP:
            onUpKey();
            return true;

        case KeyEvent.KEYCODE_DPAD_DOWN:
            onDownKey();
            return true;

        case KeyEvent.KEYCODE_DEL:
            onDeleteKey();
            return true;

        case KeyEvent.KEYCODE_SPACE:
            onSpaceKey();
            return true;
        }

        Playboard board = getBoard();
        char c = Character.toUpperCase(event.getDisplayLabel());

        if (board != null && Character.isLetterOrDigit(c))
            playLetter(c);

        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        keyboardManager.onPause();
        binding.clueTabs.removeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keyboardManager.onDestroy();
    }

    private void displayKeyboard() {
        keyboardManager.showKeyboard(binding.miniboard);
    }

    private void displayKeyboard(Word previousWord) {
        // only show keyboard if double click a word
        // hide if it's a new word
        Playboard board = getBoard();
        if (board != null) {
            Position newPos = board.getHighlightLetter();
            if ((previousWord != null) &&
                previousWord.checkInWord(newPos.getRow(), newPos.getCol())) {
                keyboardManager.showKeyboard(binding.miniboard);
            } else {
                keyboardManager.hideKeyboard();
            }
        }
    }

    /**
     * Find a clue to select and select it
     *
     * Searches for the first clue in the current page, else starts
     * flipping through pages in the given direction.
     *
     * Does nothing if no clue found.
     */
    private void selectFirstSelectableClue(boolean searchForwards) {
        boolean selectedClue = false;
        String startPage
            = binding.clueTabs.getPageListName(getCurrentListNum());
        do {
           selectedClue = selectFirstClue();
           if (!selectedClue) {
               if (searchForwards)
                   nextList();
               else
                   prevList();
            }
        } while (
            !selectedClue
            && !Objects.equals(
                startPage,
                binding.clueTabs.getPageListName(getCurrentListNum())
            )
        );
    }

    /**
     * Selects first clue in cur list
     *
     * @returns false if no selectable clue
     */
    private boolean selectFirstClue() {
        int listNum = getCurrentListNum();
        ClueTabs.PageType pageType = binding.clueTabs.getPageType(listNum);
        if (pageType == null)
            return false;

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        switch (pageType) {
        case CLUES:
            String listName = binding.clueTabs.getPageListName(listNum);
            int firstClue = puz.getClues(listName).getFirstZonedIndex();
            if (firstClue < 0) {
                return false;
            } else {
                forceSnapOnKey();
                board.jumpToClue(new ClueID(listName, firstClue));
                unforceSnapOnKey();
                return true;
            }
        case HISTORY:
            for (ClueID cid : puz.getHistory()) {
                Clue clue = puz.getClue(cid);
                if (clue.hasZone()) {
                    forceSnapOnKey();
                    board.jumpToClue(clue);
                    unforceSnapOnKey();
                    return true;
                }
            }
            return false;
        default:
            return false;
        }
    }

    private void toggleShowWords() {
        settings.getClueListShowWords(showWords -> {
            settings.setClueListShowWords(!showWords);
        });
    }

    private void setupShowWordsMode(boolean showWords) {
        if (showWords) {
            binding.miniboard.setIncognitoMode(true);
            binding.clueTabs.setShowWords(true);
        } else {
            binding.miniboard.setIncognitoMode(false);
            binding.clueTabs.setShowWords(false);
        }
    }

    private void setupVoiceCommands() {
        registerVoiceCommandAnswer();
        registerVoiceCommandLetter();
        registerVoiceCommandClear();
        registerVoiceCommandNumber();
        registerVoiceCommandAnnounceClue();
        registerVoiceCommandClueHelp();

        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_left),
            args -> { onLeftKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_right),
            args -> { onRightKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_up),
            args -> { onUpKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_down),
            args -> { onDownKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_back),
            args -> { doBackAction(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_next),
            args -> {
                nextList();
                selectFirstClue();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_previous),
            args -> {
                prevList();
                selectFirstClue();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_notes),
            args -> {
                Playboard board = getBoard();
                if (board != null)
                    launchClueNotes(board.getClueID());
            }
        ));
    }

    private void onLeftKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Position first = getCurrentFirstPosition();

        if (!board.getHighlightLetter().equals(first)) {
            forceSnapOnKey();
            board.moveZoneBack(false);
            unforceSnapOnKey();
        } else {
            if (prevList())
                selectFirstSelectableClue(false);
        }
    }

    private void onRightKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Position last = getCurrentLastPosition();

        if (!board.getHighlightLetter().equals(last)) {
            forceSnapOnKey();
            board.moveZoneForward(false);
            unforceSnapOnKey();
        } else {
            if (nextList())
                selectFirstSelectableClue(true);
        }
    }

    private void onUpKey() {
        ClueTabs.PageType pageType
            = binding.clueTabs.getPageType(getCurrentListNum());
        // do nothing on history
        if (pageType == null || pageType == ClueTabs.PageType.HISTORY)
            return;

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (board == null || puz == null)
            return;

        ClueID cid = board.getClueID();
        String curList = cid == null ? null : cid.getListName();
        int curClueIndex = cid == null ? -1 : cid.getIndex();

        if (curList != null && curClueIndex >= 0) {
            int prev = puz.getClues(curList)
                .getPreviousZonedIndex(curClueIndex, true);
            forceSnapOnKey();
            board.jumpToClue(new ClueID(curList, prev));
            unforceSnapOnKey();
        } else {
            selectFirstSelectableClue(false);
        }
    }

    private void onDownKey() {
        ClueTabs.PageType pageType
            = binding.clueTabs.getPageType(getCurrentListNum());
        if (pageType == null)
            return;

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (board == null || puz == null)
            return;

        switch (pageType) {
        case CLUES:
            ClueID cid = board.getClueID();
            String curList = cid == null ? null : cid.getListName();
            int curClueIndex = cid == null ? -1 : cid.getIndex();

            if (curList != null && curClueIndex >= 0) {
                int next = puz.getClues(curList)
                    .getNextZonedIndex(curClueIndex, true);
                forceSnapOnKey();
                board.jumpToClue(new ClueID(curList, next));
                unforceSnapOnKey();
            } else {
                selectFirstSelectableClue(true);
            }
            break;
        case HISTORY:
            List<ClueID> history = puz.getHistory();
            if (history.size() > 2) {
                forceSnapOnKey();
                // always at top of history
                board.jumpToClue(history.get(1));
                unforceSnapOnKey();
            }
           break;
        }
    }

    private void onDeleteKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Word w = board.getCurrentWord();
        Position first = getCurrentFirstPosition();

        settings.getPlayScratchMode(scratchMode -> {
            forceSnapOnKey();
            if (scratchMode)
                board.deleteScratchLetter();
            else
                board.deleteOrUndoLetter();

            Position p = board.getHighlightLetter();
            if (!w.checkInWord(p) && first != null) {
                board.setHighlightLetter(first);
            }
            unforceSnapOnKey();
        });
    }

    private void onSpaceKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        settings.getPlaySpaceChangesDirection(spaceChangesDirection -> {
            if (spaceChangesDirection)
                return;
            forceSnapOnKey();
            playLetter(' ');
            unforceSnapOnKey();
        });
    }

    private void playLetter(char c) {
        settings.getPlayScratchMode(scratchMode -> {
            Playboard board = getBoard();
            if (board == null)
                return;

            Word w = board.getCurrentWord();
            Position last = getCurrentLastPosition();

            if (scratchMode)
                board.playScratchLetter(c);
            else
                board.playLetter(c);

            Position curr = board.getHighlightLetter();
            int row = curr.getRow();
            int col = curr.getCol();

            if (!board.getCurrentWord().equals(w)
                    || Box.isBlock(board.getBoxes()[row][col])) {
                board.setHighlightLetter(last);
            }
        });
    }

    private void doBackAction() {
        if (!keyboardManager.doBackAction())
            this.finish();
    }

    /**
     * First position of current word or null
     */
    private Position getCurrentFirstPosition() {
        Playboard board = getBoard();
        if (board == null)
            return null;

        Word w = board.getCurrentWord();
        Zone zone = (w == null) ? null : w.getZone();
        Position first = null;
        if (zone != null && !zone.isEmpty())
            first = zone.getPosition(0);
        return first;
    }

    /**
     * Last position of current word or null
     */
    private Position getCurrentLastPosition() {
        Playboard board = getBoard();
        if (board == null)
            return null;

        Word w = board.getCurrentWord();
        Zone zone = (w == null) ? null : w.getZone();
        Position last = null;
        if (zone != null && !zone.isEmpty())
            last = zone.getPosition(zone.size() - 1);
        return last;
    }

    /**
     * General method for any part of a clue tab click
     */
    private void onClueTabsClickGeneral(Clue clue) {
        Playboard board = getBoard();
        if (board == null)
            return;

        if (clue.hasZone()) {
            Word old = board.getCurrentWord();
            if (!Objects.equals(clue.getClueID(), board.getClueID()))
                board.jumpToClue(clue);
            displayKeyboard(old);
        }
    }

    /**
     * Is a key event we handle
     *
     * Should match onKeyUp/Down
     */
    private boolean isHandledKey(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_ESCAPE:
        case KeyEvent.KEYCODE_DPAD_UP:
        case KeyEvent.KEYCODE_DPAD_DOWN:
        case KeyEvent.KEYCODE_DPAD_LEFT:
        case KeyEvent.KEYCODE_DPAD_RIGHT:
        case KeyEvent.KEYCODE_DEL:
        case KeyEvent.KEYCODE_SPACE:
            return true;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());
        if (Character.isLetterOrDigit(c))
            return true;

        return false;
    }

    /**
     * Move to next list, return true if change
     */
    private boolean nextList() {
        return setCurrentListNum(
            binding.clueTabs.getNextListNum(getCurrentListNum())
        );
    }

    /**
     * Move to prev list, return true if change
     */
    private boolean prevList() {
        return setCurrentListNum(
            binding.clueTabs.getPrevListNum(getCurrentListNum())
        );
    }

    /**
     * Update current list, return true if change
     */
    private boolean setCurrentListNum(int listNum) {
        binding.clueTabs.ensureListShown(listNum, currentListNum);

        if (currentListNum != listNum) {
            currentListNum = listNum;

            forceSnapOnKey();
            selectFirstClue();
            unforceSnapOnKey();

            return true;
        } else {
            return false;
        }
    }

    private int getCurrentListNum() {
        return currentListNum;
    }

    /**
     * After a snap, infer what the current list num should be
     */
    private void inferCurrentListNumAfterSnap() {
        ClueTabs.PageType pageType
            = binding.clueTabs.getPageType(getCurrentListNum());
        switch (pageType) {
        case CLUES:
            Playboard board = getBoard();
            Puzzle puz = getPuzzle();
            if (board == null || puz == null)
                return;

            ClueID cid = board.getClueID();
            String curList = cid == null ? null : cid.getListName();
            int newListNum = binding.clueTabs.getListNum(curList);
            currentListNum = newListNum;
            break;

        case HISTORY:
            // current listnum is ok
            break;
        }
    }

    private void forceSnapOnKey() {
        binding.clueTabs.setForceSnap(true);
    }

    private void unforceSnapOnKey() {
        binding.clueTabs.setForceSnap(false);
        inferCurrentListNumAfterSnap();
    }
}
