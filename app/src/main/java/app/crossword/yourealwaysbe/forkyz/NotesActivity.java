package app.crossword.yourealwaysbe.forkyz;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import javax.inject.Inject;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.widget.CompoundButtonCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import dagger.hilt.android.AndroidEntryPoint;

import app.crossword.yourealwaysbe.forkyz.databinding.NotesBinding;
import app.crossword.yourealwaysbe.forkyz.util.ColorUtils;
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManager;
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.forkyz.view.BoardEditText.BoardEditFilter;
import app.crossword.yourealwaysbe.forkyz.view.BoardEditText;
import app.crossword.yourealwaysbe.forkyz.view.BoardEditView.BoardClickListener;
import app.crossword.yourealwaysbe.forkyz.view.ChooseFlagColorDialog;
import app.crossword.yourealwaysbe.forkyz.view.ScrollingImageView.Point;
import app.crossword.yourealwaysbe.forkyz.view.ScrollingImageView;
import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.Note;
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges;
import app.crossword.yourealwaysbe.puz.Playboard.Word;
import app.crossword.yourealwaysbe.puz.Playboard;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

public class NotesActivity extends PuzzleActivity {
    private static final Logger LOG = Logger.getLogger(
        NotesActivity.class.getCanonicalName()
    );

    /**
     * Start with intent extras Clue ID to note for, omit if puzzle
     * notes.
     */
    public static final String CLUE_NOTE_LISTNAME = "clueNoteListName";
    public static final String CLUE_NOTE_INDEX = "clueNoteIndex";

    private static final String TRANSFER_RESPONSE_REQUEST_KEY
        = "transferResponseRequest";

    private enum TransferResponseRequest {
        SCRATCH_TO_BOARD,
        ANAGRAM_SOL_TO_BOARD,
        BOARD_TO_SCRATCH,
        BOARD_TO_ANAGRAM_SOL
    }

    private NotesBinding binding;
    private KeyboardManager keyboardManager;
    private ColorStateList defaultFlagColors;

    private Random rand = new Random();

    private int numAnagramLetters = 0;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.puzzle_menu_flag_color) {
            onFlagClueSelected();
            return true;
        } else if (
            item.getItemId() == R.id.notes_menu_crossword_solver_anagram
        ) {
            callAnagramSolver();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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
            LOG.info("NotesActivity resumed but no Puzzle selected, finishing.");
            finish();
            return;
        }

        binding = NotesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        holographic();
        finishOnHomeButton();
        setupBottomInsets(binding.content);

        int clueTextSize
            = getResources().getInteger(R.integer.clue_text_size);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            binding.clueHolder.clueLine,
            5,
            clueTextSize,
            1,
            TypedValue.COMPLEX_UNIT_SP
        );

        binding.miniboard.setAllowOverScroll(false);
        binding.miniboard.addBoardClickListener(new BoardClickListener() {
            @Override
            public void onClick(Position position, Word previousWord) {
                NotesActivity.this
                    .keyboardManager
                    .showKeyboard(binding.miniboard);
            }

            @Override
            public void onLongClick(Position position) {
                View focused = getWindow().getCurrentFocus();
                int id = focused.getId();
                if (id == R.id.scratch_miniboard) {
                    NotesActivity.this.executeTransferResponseRequest(
                        TransferResponseRequest.BOARD_TO_SCRATCH, true
                    );
                } else if (
                    id == R.id.anagram_solution || id == R.id.anagram_source
                ) {
                    NotesActivity.this.executeTransferResponseRequest(
                        TransferResponseRequest.BOARD_TO_ANAGRAM_SOL, true
                    );
                }
            }
        });
        ViewCompat.replaceAccessibilityAction(
            binding.miniboard,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.transfer_board_to_scratch_or_anagram),
            null
        );

        binding.miniboard.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP)
                    return NotesActivity.this.onMiniboardKeyUp(keyCode, event);
                else
                    return false;
            }
        });
        binding.miniboard.setOnFocusChangeListener(
            new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean gainFocus) {
                    NotesActivity.this.onBoardViewFocusChanged(gainFocus);
                }
            }
        );

        binding.notesBox.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                NotesActivity.this.moveScratchToNote();
                return true;
            }
        });
        ViewCompat.replaceAccessibilityAction(
            binding.notesBox,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.transfer_scratch_to_notes),
            null
        );

        binding.notesBox.setOnFocusChangeListener(
            new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean gainFocus) {
                    NotesActivity.this.onNotesBoxFocusChanged(gainFocus);
                }
            }
        );
        if (isPuzzleNotes()) {
            binding.notesBox.setHint(getString(R.string.general_puzzle_notes));
        }

        binding.scratchMiniboard.setContextMenuListener(
            new ScrollingImageView.ClickListener() {
                @Override
                public void onTap(Point point) {
                    NotesActivity
                        .this
                        .keyboardManager
                        .showKeyboard(binding.scratchMiniboard);
                }

                @Override
                public void onContextMenu(Point point) {
                    if (isClueOnBoard()) {
                        executeTransferResponseRequest(
                            TransferResponseRequest.SCRATCH_TO_BOARD, true
                        );
                    }
                }
            }
        );
        ViewCompat.replaceAccessibilityAction(
            binding.scratchMiniboard,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.transfer_scratch_to_board),
            null
        );

        binding.anagramSource.setContextMenuListener(
            new ScrollingImageView.ClickListener() {
                @Override
                public void onTap(Point point) {
                    NotesActivity
                        .this
                        .keyboardManager
                        .showKeyboard(binding.anagramSource);
                }

                @Override
                public void onContextMenu(Point point) {
                    // reshuffle squares
                    int len = binding.anagramSource.getLength();
                    for (int i = 0; i < len; i++) {
                        int j = rand.nextInt(len);
                        char ci = binding.anagramSource.getResponse(i);
                        char cj = binding.anagramSource.getResponse(j);
                        binding.anagramSource.setResponse(i, cj);
                        binding.anagramSource.setResponse(j, ci);
                    }
                }
            }
        );
        ViewCompat.replaceAccessibilityAction(
            binding.anagramSource,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.shuffle_anagram_source),
            null
        );

        binding.anagramSolution.setContextMenuListener(
            new ScrollingImageView.ClickListener() {
                @Override
                public void onTap(Point point) {
                    NotesActivity.this
                        .keyboardManager
                        .showKeyboard(binding.anagramSolution);
                }

                @Override
                public void onContextMenu(Point point) {
                    if (isClueOnBoard()) {
                        executeTransferResponseRequest(
                            TransferResponseRequest.ANAGRAM_SOL_TO_BOARD, true
                        );
                    }
                }
            }
        );
        ViewCompat.replaceAccessibilityAction(
            binding.anagramSolution,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.transfer_anagram_solution_to_board),
            null
        );

        binding.flagClue.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                NotesActivity.this.onFlagClueSelected();
                return true;
            }
        });
        ViewCompat.replaceAccessibilityAction(
            binding.flagClue,
            AccessibilityActionCompat.ACTION_LONG_CLICK,
            getText(R.string.choose_flag_color),
            null
        );

        keyboardManager
            = new KeyboardManager(this, settings, binding.keyboard, null);
        keyboardManager.showKeyboard(
            isPuzzleNotes() ? binding.scratchMiniboard : binding.miniboard
        );

        setupVoiceButtons(binding.voiceButtonsInclude);
        setupVoiceCommands();

        addAccessibilityActions(binding.miniboard);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notes_menu, menu);
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean handled = isHandledKey(keyCode, event);
        if (!handled)
            return super.onKeyDown(keyCode, event);
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean handled = isHandledKey(keyCode, event);
        if (!handled)
            return super.onKeyUp(keyCode, event);

        int cancelled = event.getFlags()
            & (KeyEvent.FLAG_CANCELED | KeyEvent.FLAG_CANCELED_LONG_PRESS);
        if (cancelled > 0)
            return true;

        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
            onBackKey();
            return true;
        }

        return true;
    }

    public void onPause() {
        saveNoteToBoard();
        super.onPause();
        keyboardManager.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (keyboardManager != null)
            keyboardManager.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (keyboardManager != null)
            keyboardManager.onDestroy();
    }

    private void onFlagClueSelected() {
        Clue clue = getNotesClue();
        if (clue == null)
            return;

        // assume if want color then want flagged
        if (!binding.flagClue.isChecked()) {
            binding.flagClue.setChecked(true);
            setFlagClueColor();
        }

        DialogFragment dialog = new ChooseFlagColorDialog();
        Bundle args = new Bundle();
        ChooseFlagColorDialog.addClueToBundle(args, clue);
        dialog.setArguments(args);
        dialog.show(getSupportFragmentManager(), "ChooseFlagColorDialog");
    }

    private void setFlagClueColor() {
        Clue clue = getNotesClue();
        if (clue == null)
            return;

        if (clue.isDefaultFlagColor()) {
            if (defaultFlagColors != null) {
                CompoundButtonCompat.setButtonTintList(
                    binding.flagClue, defaultFlagColors
                );
            }
        } else {
            // save so can restore later
            if (defaultFlagColors == null) {
                defaultFlagColors = CompoundButtonCompat.getButtonTintList(
                    binding.flagClue
                );
            }

            CompoundButtonCompat.setButtonTintList(
                binding.flagClue,
                ColorStateList.valueOf(ColorUtils.addAlpha(clue.getFlagColor()))
            );
        }
    }

    private boolean onMiniboardKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
            return false;

        case KeyEvent.KEYCODE_DPAD_LEFT:
            onLeftKey();
            return true;

        case KeyEvent.KEYCODE_DPAD_RIGHT:
            onRightKey();
            return true;

        case KeyEvent.KEYCODE_DEL:
            onDeleteKey();
            return true;
        }

        char c = Character.toUpperCase(event.getDisplayLabel());

        if (utils.isAcceptableCharacterResponse(c)) {
            Playboard board = getBoard();
            if (board != null) {
                Word w = board.getCurrentWord();
                Zone zone = (w == null) ? null : w.getZone();
                Position last = null;

                if (zone != null && !zone.isEmpty()) {
                    last = zone.getPosition(zone.size() - 1);
                }

                board.playLetter(c);

                Position p = board.getHighlightLetter();
                int row = p.getRow();
                int col = p.getCol();

                if (!board.getCurrentWord().equals(w)
                        || Box.isBlock(board.getBoxes()[row][col])) {
                    getBoard().setHighlightLetter(last);
                }
            }
            return true;
        }

        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (board == null || puz == null) {
            LOG.info(
                "NotesActivity resumed but no Puzzle selected, "
                    + "finishing."
            );
            finish();
            // finish doesn't finish right away
            return;
        }

        Clue clue = getNotesClue();

        final int curWordLen = (clue == null || !clue.hasZone())
            ? Math.max(puz.getWidth(), puz.getHeight())
            : clue.getZone().size();

        if (curWordLen < 0) {
            LOG.info("NotesActivity needs a non-empty word");
            finish();
            return;
        }

        Note note;

        setClueLineText();

        if (isPuzzleNotes()) {
            note = puz.getPlayerNote();
            binding.miniboardLab.setVisibility(View.GONE);
            binding.miniboard.setVisibility(View.GONE);
            binding.flagClue.setVisibility(View.GONE);
        } else {
            settings.getPlayScratchDisplay(displayScratch -> {
                binding.miniboard.setBoard(
                    board, getSuppressNotesList(displayScratch)
                );
            });

            note = puz.getNote(clue);
            binding.flagClue.setChecked(clue.isFlagged());
            setFlagClueColor();
            int clueVisibility = isClueOnBoard() ? View.VISIBLE : View.GONE;
            binding.miniboardLab.setVisibility(clueVisibility);
            binding.miniboard.setVisibility(clueVisibility);
            binding.flagClue.setVisibility(View.VISIBLE);
        }

        // set up and erase any previous data
        binding.notesBox.setText("");

        // set lengths after fully set up
        binding.scratchMiniboard.clear();
        binding.anagramSource.clear();
        binding.anagramSolution.clear();

        numAnagramLetters = 0;

        BoardEditFilter sourceFilter = new BoardEditFilter() {
            public boolean delete(char oldChar, int pos) {
                if (!BoardEditText.isBlank(oldChar)) {
                    numAnagramLetters -= 1;
                }
                return true;
            }

            public char filter(char oldChar, char newChar, int pos) {
                if (BoardEditText.isBlank(newChar)) {
                    if (!BoardEditText.isBlank(oldChar))
                        numAnagramLetters -= 1;
                    return newChar;
                } else {
                    if (!BoardEditText.isBlank(oldChar)) {
                        return newChar;
                    } else if (numAnagramLetters < curWordLen) {
                        numAnagramLetters++;
                        return newChar;
                    } else {
                        return '\0';
                    }
                }
            }
        };

        binding.anagramSource.setFilters(
            new BoardEditFilter[]{sourceFilter}
        );

        BoardEditFilter solFilter = new BoardEditFilter() {
            public boolean delete(char oldChar, int pos) {
                if (!BoardEditText.isBlank(oldChar)) {
                    for (int i = 0; i < curWordLen; i++) {
                        if (binding.anagramSource.isBlank(i)) {
                            binding.anagramSource.setResponse(i, oldChar);
                            return true;
                        }
                    }
                }
                return true;
            }

            public char filter(char oldChar, char newChar, int pos) {
                boolean changed
                    = NotesActivity.this.preAnagramSolResponse(pos, newChar);
                return changed ? newChar : '\0';
            }
        };

        binding.anagramSolution.setFilters(new BoardEditFilter[]{solFilter});

        if (note != null) {
            binding.notesBox.setText(note.getText());

            binding.scratchMiniboard.setFromString(note.getScratch());

            String src = note.getAnagramSource();
            if (src != null) {
                binding.anagramSource.setFromString(src);
                numAnagramLetters += binding.anagramSource.getNumNonBlank();
            }

            String sol = note.getAnagramSolution();
            if (sol != null) {
                binding.anagramSolution.setFromString(sol);
                numAnagramLetters +=  binding.anagramSolution.getNumNonBlank();
            }
        }

        binding.anagramSolution.setLength(curWordLen);
        binding.scratchMiniboard.setLength(curWordLen);
        binding.anagramSource.setLength(curWordLen);

        keyboardManager.onResume();

        setVoiceButtonVisibility();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!super.onPrepareOptionsMenu(menu))
            return false;

        MenuItem specialEntry = menu.findItem(R.id.puzzle_menu_special_entry);

        boolean specialEntryEnabled = getCurrentFocus() == binding.miniboard;

        specialEntry.setVisible(specialEntryEnabled);
        specialEntry.setEnabled(specialEntryEnabled);

        return true;
    }

    @Override
    public void onPlayboardChange(PlayboardChanges changes) {
        super.onPlayboardChange(changes);

        Playboard board = getBoard();
        ClueID cid = (board == null) ? null : board.getClueID();

        if (cid != null && changes.getChangedClueIDs().contains(cid)) {
            setClueLineText();
            setFlagClueColor();
        }
    }

    private Set<String> getSuppressNotesList(boolean displayScratch) {
        if (!displayScratch)
            return null;

        ClueID cid = getBoard().getClueID();
        if (cid == null)
            return Collections.emptySet();

        String list = cid.getListName();
        if (list == null)
            return Collections.emptySet();
        else
            return Collections.singleton(list);
    }

    private void moveScratchToNote() {
        String notesText = binding.notesBox.getText().toString();

        String scratchText = binding.scratchMiniboard.toString().trim();

        if (scratchText.length() > 0) {
            if (notesText.length() > 0)
                notesText += "\n";
            notesText += scratchText;

            binding.scratchMiniboard.clear();
            binding.notesBox.setText(notesText);
        }
    }

    private void copyBoxesToAnagramSol(Box[] boxes) {
        for (int i = 0; i < boxes.length; i++) {
            if (!boxes[i].isBlank()) {
                String newResponse = boxes[i].getResponse();
                if (newResponse != null && !newResponse.isEmpty()) {
                    char newChar = newResponse.charAt(0);
                    boolean allowed = preAnagramSolResponse(i, newChar);
                    if (allowed)
                        binding.anagramSolution.setResponse(i, newChar);
                }
            }
        }
    }

    private boolean hasConflict(Box[] source,
                                Box[] dest,
                                boolean copyBlanks) {
        int length = Math.min(source.length, dest.length);
        for (int i = 0; i < length; i++) {
            if (
                (copyBlanks || !source[i].isBlank()) &&
                !dest[i].isBlank() &&
                !Objects.equals(
                    source[i].getResponse(), dest[i].getResponse()
                )
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Copies non-blank characters from response to the view
     */
    private void overlayBoxesOnBoardView(Box[] boxes,
                                         BoardEditText view) {
        int length = Math.min(boxes.length, view.getLength());
        for (int i = 0; i < length; i++) {
            if (!boxes[i].isBlank()) {
                view.setResponse(i, boxes[i].getResponse());
            }
        }
    }

    /**
     * Make arrangements for anagram letter to be played
     *
     * Changes source/sol boxes by moving required letters around.
     *
     * @return true if play of letter can proceed
     */
    private boolean preAnagramSolResponse(int pos, char newChar) {
        char oldChar = binding.anagramSolution.getResponse(pos);
        int sourceLen = binding.anagramSource.getLength();
        for (int i = 0; i < sourceLen; i++) {
            if (binding.anagramSource.getResponse(i) == newChar) {
                binding.anagramSource.setResponse(i, oldChar);
                return true;
            }
        }
        // if failed to find it in the source view, see if we can
        // find one to swap it with one in the solution
        int solLen = binding.anagramSolution.getLength();
        for (int i = 0; i < solLen; i++) {
            if (binding.anagramSolution.getResponse(i) == newChar) {
                binding.anagramSolution.setResponse(i, oldChar);
                return true;
            }
        }
        return false;
    }

    /**
     * Transfer one board view to another
     *
     * Somewhat inelegant as a dialog confirmation has to be robust
     * against activity recreation, so it all gets funnelled through
     * here with little external state.
     *
     * The alternative is to copy-paste the dialog construction several
     * times. I'm not sure which is better.
     */
    private void executeTransferResponseRequest(
        TransferResponseRequest request,
        boolean confirmOverwrite
    ) {
        Playboard board = getBoard();
        if (board == null)
            return;

        boolean conflict = false;
        Box[] curWordBoxes = board.getCurrentWordBoxes();

        if (confirmOverwrite) {
            switch (request) {
            case SCRATCH_TO_BOARD:
                conflict = hasConflict(
                    binding.scratchMiniboard.getBoxes(), curWordBoxes, true
                );
                break;
            case ANAGRAM_SOL_TO_BOARD:
                conflict = hasConflict(
                    binding.anagramSolution.getBoxes(), curWordBoxes, true
                );
                break;
            case BOARD_TO_SCRATCH:
                conflict = hasConflict(
                    curWordBoxes, binding.scratchMiniboard.getBoxes(), false
                );
                break;
            case BOARD_TO_ANAGRAM_SOL:
                conflict = false;
            }
        }

        if (conflict) {
            confirmAndExecuteTransferRequest(request);
        } else {
            switch (request) {
            case SCRATCH_TO_BOARD:
                board.setCurrentWord(binding.scratchMiniboard.getBoxes());
                break;
            case ANAGRAM_SOL_TO_BOARD:
                board.setCurrentWord(binding.anagramSolution.getBoxes());
                break;
            case BOARD_TO_SCRATCH:
                overlayBoxesOnBoardView(curWordBoxes, binding.scratchMiniboard);
                break;
            case BOARD_TO_ANAGRAM_SOL:
                copyBoxesToAnagramSol(curWordBoxes);
                break;
            }
        }
    }

    private void confirmAndExecuteTransferRequest(
        TransferResponseRequest request
    ) {
        DialogFragment dialog = new TransferResponseRequestDialog();
        Bundle args = new Bundle();
        args.putSerializable(TRANSFER_RESPONSE_REQUEST_KEY, request);
        dialog.setArguments(args);
        dialog.show(
            getSupportFragmentManager(), "TransferResponseRequestDialog"
        );
    }

    private void setupVoiceCommands() {
        registerVoiceCommandAnswer();
        registerVoiceCommandLetter();
        registerVoiceCommandClear();
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
            getString(R.string.command_back),
            args -> { onBackKey(); }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_scratch),
            text -> {
                Playboard board = getBoard();
                if (board == null)
                    return;

                // remove non-word as not usually entered into grids
                String prepped
                    = text.replaceAll("\\W+", "")
                        .toUpperCase(Locale.getDefault());

                // means you can't enter "clear" into scratch, but oh well
                String clear = getString(R.string.command_clear);
                if (clear.equalsIgnoreCase(text)) {
                    binding.scratchMiniboard.clear();
                } else {
                    int len = Math.min(
                        prepped.length(), binding.scratchMiniboard.getLength()
                    );
                    for (int i = 0; i < len; i++) {
                        binding.scratchMiniboard
                            .setResponse(i, prepped.charAt(i));
                    }
                }

                // need to save since is called between an onPause and an
                // onResume
                saveNoteToBoard();
            }
        ));
        registerVoiceCommand(new VoiceCommand(
            getString(R.string.command_flag),
            text -> {
                Playboard board = getBoard();
                if (board == null)
                    return;

                binding.flagClue.setChecked(true);

                // need to save since is called between an onPause and an
                // onResume
                saveNoteToBoard();
            }
        ));
    }

    private void onLeftKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveZoneBack(false);
    }

    private void onRightKey() {
        Playboard board = getBoard();
        if (board != null)
            board.moveZoneForward(false);
    }

    private void onDeleteKey() {
        Playboard board = getBoard();
        if (board == null)
            return;

        Word w = board.getCurrentWord();
        Zone zone = (w == null) ? null : w.getZone();
        Position first = null;

        if (zone != null && !zone.isEmpty()) {
            first = zone.getPosition(0);
        }

        board.deleteLetter();

        Position p = board.getHighlightLetter();

        if (!w.checkInWord(p) && first != null) {
            board.setHighlightLetter(first);
        }
    }

    private void onBackKey() {
        keyboardManager.handleBackKey(keyHandled -> {
            if (!keyHandled)
                this.finish();
        });
    }

    @AndroidEntryPoint
    public static class TransferResponseRequestDialog extends DialogFragment {

        @Inject
        protected AndroidVersionUtils utils;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final NotesActivity activity = (NotesActivity) getActivity();

            Bundle args = getArguments();
            TransferResponseRequest request = utils.getSerializable(
                args,
                TRANSFER_RESPONSE_REQUEST_KEY,
                TransferResponseRequest.class
            );

            MaterialAlertDialogBuilder builder
                = new MaterialAlertDialogBuilder(activity);

            builder.setTitle(R.string.copy_conflict)
                .setMessage(R.string.transfer_overwrite_warning)
                .setPositiveButton(R.string.yes,
                                      new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        NotesActivity activity = ((NotesActivity) getActivity());
                        activity.executeTransferResponseRequest(request, false);
                    }
                })
                .setNegativeButton(R.string.no,
                                          new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

            return builder.create();
        }
    }

    /**
     * Invalidate options menu so special entry can be hidden
     */
    private void onBoardViewFocusChanged(boolean gainFocus) {
        invalidateOptionsMenu();
    }

    /**
     * Force hide in-app keyboard if focus gained, hide soft keyboard if
     * focus lost
     */
    private void onNotesBoxFocusChanged(boolean gainFocus) {
        keyboardManager.onFocusNativeView(binding.notesBox, gainFocus);
    }

    /**
     * Read clue id from intent, or null if not valid or not present
     *
     * Will return null if isPuzzleNotes
     */
    private Clue getNotesClue() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();
        if (puz == null)
            return null;

        Intent intent = getIntent();
        String listName = intent.getStringExtra(CLUE_NOTE_LISTNAME);
        int index = intent.getIntExtra(CLUE_NOTE_INDEX, -1);

        if (listName == null || index < 0)
            return null;

        ClueID cid = new ClueID(listName, index);

        return puz.getClue(cid);
    }

    private boolean isPuzzleNotes() {
        return getNotesClue() == null;
    }

    /**
     * If the clue is on the board
     *
     * Returns false if isPuzzleNotes
     */
    private boolean isClueOnBoard() {
        Clue clue = getNotesClue();
        return clue != null && clue.hasZone();
    }

    private void saveNoteToBoard() {
        Playboard board = getBoard();
        Puzzle puz = getPuzzle();

        if (puz == null)
            return;

        String text = binding.notesBox.getText().toString();
        String scratch = binding.scratchMiniboard.toString();
        String anagramSource = binding.anagramSource.toString();
        String anagramSolution = binding.anagramSolution.toString();

        Note note = new Note(scratch, text, anagramSource, anagramSolution);

        if (isPuzzleNotes()) {
            puz.setPlayerNote(note);
        } else {
            Clue clue = getNotesClue();
            puz.setNote(clue, note);
            board.flagClue(clue, binding.flagClue.isChecked());
        }
    }

    /**
     * Is a key we handle
     *
     * Should match onKeyUp/Down
     */
    private boolean isHandledKey(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
        case KeyEvent.KEYCODE_ESCAPE:
            return true;
        }
        return false;
    }

    private void setClueLineText() {
        if (isPuzzleNotes()) {
            binding.clueHolder.clueLine.setText(
                getString(R.string.player_notes)
            );
        } else {
            Clue clue = getNotesClue();
            if (clue != null) {
                getLongClueText(clue, text -> {
                    binding.clueHolder.clueLine.setText(smartHtml(text));
                });
            } else {
                binding.clueHolder.clueLine.setText(
                    getString(R.string.unknown_hint)
                );
            }
        }
    }

    /**
     * Override to use scratch for puzzle notes page or if scratch focussed
     */
    protected void callCrosswordSolver() {
        View focused = getWindow().getCurrentFocus();
        int id = (focused == null) ? -1 : focused.getId();
        if (id == R.id.scratch_miniboard || isPuzzleNotes()) {
            StringBuilder request = new StringBuilder();
            getLetters(binding.scratchMiniboard, true, request);
            launchCrosswordSolver(CROSSWORD_SOLVER_MAIN, request.toString());
        } else {
            super.callCrosswordSolver();
        }
    }

    private void callAnagramSolver() {
        StringBuilder request = new StringBuilder();

        getLetters(binding.anagramSource, false, request);
        getLetters(binding.anagramSolution, false, request);

        launchCrosswordSolver(CROSSWORD_SOLVER_ANAGRAM, request.toString());
    }

    /**
     * Get letters from an edit text view
     *
     * @param source the edit text view
     * @param withBlanks whether to ignore the blank squares or add them
     * @param letters a string builder to put the letters into
     */
    private void getLetters(
        BoardEditText source, boolean withBlanks, StringBuilder letters
    ) {
        for (int i = 0; i < source.getLength(); i++) {
            if (withBlanks || !source.isBlank(i))
                letters.append(source.getResponse(i));
        }
    }
}
