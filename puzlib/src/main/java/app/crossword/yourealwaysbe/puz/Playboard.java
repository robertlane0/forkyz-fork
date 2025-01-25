package app.crossword.yourealwaysbe.puz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.puz.util.PuzzleUtils;
import app.crossword.yourealwaysbe.puz.util.WeakSet;

public class Playboard {
    private static final Logger LOG = Logger.getLogger(Playboard.class.getCanonicalName());

    private static final long PAUSE_DELETE_DELAY = 150; // ms

    /**
     * What to do with cells that are filled by crossing words
     * DELETE: delete clears all cells
     * PRESERVE_FILLED_WORDS: delete does not delete cells in crossing
     * words if the crossing word is completely full
     * PRESERVE_FILLED_CELLS: delete does not delete cells if the
     * neighbouring cell of a crossing word is also filled
     */
    public enum DeleteCrossingMode {
        DELETE, PRESERVE_FILLED_WORDS, PRESERVE_FILLED_CELLS
    }

    private MovementStrategy movementStrategy = MovementStrategy.MOVE_NEXT_ON_AXIS;
    private Puzzle puzzle;
    private String responder;
    private ArrayList<String> sortedClueListNames = new ArrayList<>();
    private boolean showErrorsGrid;
    private boolean showErrorsCursor;
    private boolean showErrorsClue;
    private boolean skipCompletedLetters;
    private boolean preserveCorrectLettersInShowErrors;
    private boolean toggleBeforeMove;
    private DeleteCrossingMode deleteCrossingMode = DeleteCrossingMode.DELETE;
    private Word previousWord = null;
    private Position previousPosition = null;
    // used by findZoneDelta to track last index to handle clues with
    // repeated positions
    private int lastFoundZoneIndex = -1;
    // Zone for the cells without clues attached
    private Zone detachedCellsZone;
    // to toggle between selecting zone and single cell
    // false so toggled to true the first time the detached cells are
    // selected
    private boolean selectDetachedZone = true;

    private transient Set<PlayboardListener> listeners = WeakSet.buildSet();
    private transient Set<PlayboardListener> pendingListenerRemovals
        = WeakSet.buildSet();
    private transient Set<PlayboardListener> pendingListenerAdditions
        = WeakSet.buildSet();
    private boolean isNotifying = false;
    private int notificationDisabledDepth = 0;
    private long pauseDeleteUntil = -1;

    private HashSet<Position> changedPositions = new HashSet<>();
    private HashSet<ClueID> changedClueIDs = new HashSet<>();
    // reuse same one for all notifications
    private PlayboardChanges notificationChanges = new PlayboardChanges();
    private PlayLetterUndoStack playLetterUndoStack
        = new PlayLetterUndoStack(false);

    public Playboard(Puzzle puzzle,
                     MovementStrategy movementStrategy,
                     boolean preserveCorrectLettersInShowErrors,
                     DeleteCrossingMode deleteCrossingMode){
        this(puzzle, movementStrategy);
        this.preserveCorrectLettersInShowErrors = preserveCorrectLettersInShowErrors;
        this.deleteCrossingMode = deleteCrossingMode;
    }

    public Playboard(Puzzle puzzle, MovementStrategy movementStrategy) {
        this(puzzle);
        this.movementStrategy = movementStrategy;
    }

    public Playboard(Puzzle puzzle) {
        if (puzzle == null) {
            throw new IllegalArgumentException(
                "Cannot initialise a playboard with a null puzzle."
            );
        }

        this.puzzle = puzzle;

        sortedClueListNames.addAll(puzzle.getClueListNames());
        Collections.sort(this.sortedClueListNames);

        if (puzzle.getPosition() == null)
            selectFirstPosition();

        // setup bookkeeping
        ClueID curID = puzzle.getCurrentClueID();

        updateHistory(curID);

        previousPosition = puzzle.getPosition();

        Clue clue = puzzle.getClue(curID);
        if (clue == null || !clue.hasZone()) {
            previousWord  = new Word(getDetachedCellsZone(puzzle));
        } else {
            previousWord = new Word(clue.getZone(), clue.getClueID());
        }
    }

    /**
     * Whether to record played letters and undo on delete
     */
    public void setPlayLetterUndoStackEnabled(boolean enabled) {
        playLetterUndoStack.setEnabled(enabled);
    }

    public void setPreserveCorrectLettersInShowErrors(boolean value){
        this.preserveCorrectLettersInShowErrors = value;
    }

    public void setToggleBeforeMove(boolean value) {
        this.toggleBeforeMove = value;
    }

    /**
     * Whether to delete characters from crossing words
     */
    public void setDeleteCrossingMode(DeleteCrossingMode value){
        this.deleteCrossingMode = value;
    }

    public Box[][] getBoxes() {
        return puzzle.getBoxes();
    }

    /**
     * Returns null if no clue for current position
     */
    public Clue getClue() {
        ClueID clueID = getClueID();
        if (clueID == null)
            return null;
        return puzzle.getClue(clueID);
    }

    public ClueID getClueID() {
        return puzzle.getCurrentClueID();
    }

    /**
     * Gets the currently selected clue list from the puzzle
     *
     * Or null if none selected
     */
    public ClueList getCurrentClueList() {
        String name = getCurrentClueListName();
        return (name == null) ? null : puzzle.getClues(name);
    }

    /**
     * Gets the currently selected clue list name from the puzzle
     *
     * Or null if none selected
     */
    public String getCurrentClueListName() {
        ClueID clueID = getClueID();
        return (clueID == null) ? null : clueID.getListName();
    }

    /**
     * Clue number for current position or null if none
     */
    public String getClueNumber() {
        ClueID cid = getClueID();
        if (cid == null)
            return null;
        Clue clue = puzzle.getClue(cid);
        if (clue == null)
            return null;
        return clue.getClueNumber();
    }

    /**
     * Returns -1 if no current clue selected
     */
    public int getCurrentClueIndex() {
        Clue clue = getClue();
        return clue == null ? -1 : clue.getClueID().getIndex();
    }

    public Note getNote() {
        Clue c = this.getClue();
        return (c == null) ? null : this.puzzle.getNote(c.getClueID());
    }

    public Box getCurrentBox() {
        return getCurrentBoxOffset(0, 0);
    }

    /**
     * Get the box at the given offset from current box
     *
     * Null if no box
     */
    private Box getCurrentBoxOffset(int offsetAcross, int offsetDown) {
        Position currentPos = getHighlightLetter();
        int offAcross = currentPos.getCol() + offsetAcross;
        int offDown = currentPos.getRow() + offsetDown;
        return puzzle.checkedGetBox(offDown, offAcross);
    }

    public Word getCurrentWord() {
        Word word = getClueWord(getClueID());
        if (word != null)
            return word;

        if (selectDetachedZone) {
            return new Word(getDetachedCellsZone());
        } else {
            Zone zone = new Zone();
            zone.addPosition(getHighlightLetter());
            return new Word(zone);
        }
    }

    /**
     * Return the word associated with the clue
     *
     * @return null if no clue or no zone for clue
     */
    public Word getClueWord(ClueID clueID) {
        Clue clue = getPuzzle().getClue(clueID);
        if (clue == null || !clue.hasZone()) {
            return null;
        } else {
            return new Word(clue.getZone(), clue.getClueID());
        }
    }

    public Box[] getCurrentWordBoxes() {
        return getWordBoxes(getCurrentWord());
    }

    public Box[] getWordBoxes(Word word) {
        Zone zone = (word == null) ? null : word.getZone();
        if (zone == null)
            return new Box[0];

        Box[] result = new Box[zone.size()];
        Box[][] boxes = getBoxes();

        for (int i = 0; i < zone.size(); i++) {
            Position pos = zone.getPosition(i);
            result[i] = boxes[pos.getRow()][pos.getCol()];
        }

        return result;

    }

    public void setCurrentWord(Box[] response) {
        Zone zone = getCurrentZone();
        if (zone == null)
            return;

        int length = Math.min(zone.size(), response.length);
        for (int i = 0; i < length; i++) {
            Position pos = zone.getPosition(i);
            Box box = puzzle.checkedGetBox(pos);
            if (!Box.isBlock(box))
                box.setResponse(response[i].getResponse());
            flagChange(pos);
        }
        notifyChange();
    }

    public Word setHighlightLetter(Position highlightLetter) {
        playLetterUndoStack.clearIfEnabled();

        Word w = this.getCurrentWord();

        if (highlightLetter == null)
            return w;

        pushNotificationDisabled();

        Position currentHighlight = getHighlightLetter();

        if (highlightLetter.equals(currentHighlight)) {
            toggleSelection();
        } else {
            Box box = puzzle.checkedGetBox(highlightLetter);
            if (!Box.isBlock(box)) {
                flagChange(currentHighlight, highlightLetter);

                puzzle.setPosition(highlightLetter);

                // if box is part of a clue, only toggle if it's not
                // part of the current clue (i.e. toggle to the right
                // clue selection)
                // else it's not part of a clue (detached), so only
                // toggle if there we're moving from a non-detached cell
                // to a detached one.
                if (box.isPartOfClues()) {
                    Zone zone = w.getZone();
                    if (zone == null || !zone.hasPosition(highlightLetter)) {
                        toggleSelection();
                    }
                } else if (getClueID() != null) {
                    toggleSelection();
                }
            }
        }

        popNotificationDisabled();

        notifyChange();

        return w;
    }

    /**
     * Change position and selects given clue
     *
     * Selects given clue and position only if the position is part of the
     * clue. Notifies listeners only if there is a change.
     *
     * Acts list setHighlightLetter without cid if null given.
     *
     * @return the current word before the call
     */
    public Word setHighlightLetter(Position highlightLetter, ClueID cid) {
        if (cid == null)
            return setHighlightLetter(highlightLetter);

        playLetterUndoStack.clearIfEnabled();

        Word w = getCurrentWord();

        Box box = puzzle.checkedGetBox(highlightLetter);
        if (Box.isBlock(box))
            return w;

        Position curHighlightLetter = getHighlightLetter();
        ClueID curCID = getClueID();

        boolean sameSelection
            = Objects.equals(curHighlightLetter, highlightLetter)
                && Objects.equals(curCID, cid);

        if (sameSelection)
            return w;

        if (box.getIsPartOfClues().contains(cid)) {
            puzzle.setPosition(highlightLetter);
            puzzle.setCurrentClueID(cid);

            flagChange(curHighlightLetter, highlightLetter);
            // don't flag change of word as notify picks this up from
            // current/previous word (and this call could be
            // transitional)
            notifyChange();
        }

        return w;
    }

    /**
     * Returns true if the position is part of a word
     *
     * Words may be single cells that are not part of any clue
     */
    public boolean isInWord(Position p) {
        return !Box.isBlock(puzzle.checkedGetBox(p));
    }

    public Position getHighlightLetter() {
        return puzzle.getPosition();
    }

    public void setMovementStrategy(MovementStrategy movementStrategy) {
        this.movementStrategy = movementStrategy;
    }

    public Puzzle getPuzzle() {
        return puzzle;
    }

    /**
     * @param responder the responder to set
     */
    public void setResponder(String responder) {
        this.responder = responder;
    }

    /**
     * @return the responder
     */
    public String getResponder() {
        return responder;
    }

    /**
     * Show errors across the whole grid
     */
    public void setShowErrorsGrid(boolean showErrors) {
        boolean changed = (this.showErrorsGrid != showErrors);
        this.showErrorsGrid = showErrors;
        if (changed)
            notifyChange(true);
    }

    /**
     * Show errors under the cursor only
     */
    public void setShowErrorsCursor(boolean showErrorsCursor) {
        boolean changed = (this.showErrorsCursor != showErrorsCursor);
        this.showErrorsCursor = showErrorsCursor;
        if (changed) {
            flagChange(getHighlightLetter());
            notifyChange();
        }
    }

    /**
     * Show errors for filled in clues
     */
    public void setShowErrorsClue(boolean showErrors) {
        boolean changed = (this.showErrorsClue != showErrors);
        this.showErrorsClue = showErrors;
        if (changed) {
            flagUnusualZoneChange(getCurrentZone());
            notifyChange();
        }
    }

    /**
     * Toggle show errors across the grid
     */
    public void toggleShowErrorsGrid() {
        this.showErrorsGrid = !this.showErrorsGrid;
        notifyChange(true);
    }

    /**
     * Toggle show errors across under cursor
     */
    public void toggleShowErrorsCursor() {
        this.showErrorsCursor = !this.showErrorsCursor;
        flagChange(getHighlightLetter());
        notifyChange();
    }

    /**
     * Toggle show errors for filled clues
     */
    public void toggleShowErrorsClue() {
        setShowErrorsClue(!isShowErrorsClue());
    }

    /**
     * Is showing errors across the whole grid
     */
    public boolean isShowErrorsGrid() {
        return this.showErrorsGrid;
    }

    /**
     * Is showing errors across under cursor
     */
    public boolean isShowErrorsCursor() {
        return this.showErrorsCursor;
    }

    /**
     * Is showing errors for filled clues
     */
    public boolean isShowErrorsClue() {
        return this.showErrorsClue;
    }

    /**
     * Is showing errors in some form
     */
    public boolean isShowErrors() {
        return isShowErrorsGrid()
            || isShowErrorsCursor()
            || isShowErrorsClue();
    }

    public void setSkipCompletedLetters(boolean skipCompletedLetters) {
        this.skipCompletedLetters = skipCompletedLetters;
    }

    public boolean isSkipCompletedLetters() {
        return skipCompletedLetters;
    }

    /**
     * If the clue has been filled in
     *
     * Always false if clue has a null or empty zone
     */
    public boolean isFilledClueID(ClueID clueID) {
        Zone zone = getZone(clueID);

        if(zone == null || zone.isEmpty())
            return false;

        Box[][] boxes = getBoxes();

        for (Position pos : zone) {
            if (boxes[pos.getRow()][pos.getCol()].isBlank())
                return false;
        }

        return true;
    }

    /**
     * Handler for the backspace key.  Uses the following algorithm:
     * -If current box is empty, move back one character.  If not, stay still.
     * -Delete the letter in the current box.
     */
    public Word deleteOrUndoLetter() {
        Box currentBox = this.getCurrentBox();
        Word wordToReturn = this.getCurrentWord();

        if (isPausedDelete()) {
            // pause again to make user stop sending rapid deletes
            pauseDelete();
            // notify needed for android keyboard (bit of a hack i
            // guess)
            notifyChange();
            return wordToReturn;
        }

        pushNotificationDisabled();

        // do a standard delete if undo stack is empty
        if (playLetterUndoStack.isEmpty()) {
            if (currentBox.isBlank() || isDontDeleteCurrent()) {
                wordToReturn = this.previousLetter();
                currentBox = this.getCurrentBox();
            }

            // If preserving letters, skip over them
            // break if we hit a wall or loop all the way around
            Position startPosition = getHighlightLetter();
            ClueID startClueID = getClueID();
            Position lastPosition = startPosition;
            while (!currentBox.isBlank() && isDontDeleteCurrent()) {
                this.previousLetter();

                Position newPosition = getHighlightLetter();
                boolean stuck = (
                    Objects.equals(newPosition, startPosition)
                        && Objects.equals(startClueID, getClueID())
                ) || Objects.equals(newPosition, lastPosition);

                if (stuck)
                    break;
                else
                    lastPosition = newPosition;
            }

            if (!isDontDeleteCurrent()) {
                currentBox = this.getCurrentBox();
                if (currentBox != null) {
                    currentBox.setBlank();
                    flagChange(getHighlightLetter());
                }
            }
        } else {
            // Grab the last position a letter was entered and pop it from the list.
            PlayLetterInfo undo = playLetterUndoStack.pop();
            Position pos = undo.getPosition();
            // Jump back to last box and delete the letter.
            currentBox = puzzle.checkedGetBox(pos);
            if (currentBox != null) {
                currentBox.setResponse(undo.getOldResponse());
                playLetterUndoStack.pushClearDisabled();
                setHighlightLetter(pos);
                playLetterUndoStack.popClearDisabled();
                puzzle.setCurrentClueID(undo.getClueID());
                flagChange(pos);
            }
            // pause delete when empty stack reached to provide a
            // natural break from held delete
            if (playLetterUndoStack.isEmpty())
                pauseDelete();
        }

        popNotificationDisabled();

        notifyChange();

        return wordToReturn;
    }

    public void deleteScratchLetter() {
        Box currentBox = this.getCurrentBox();

        pushNotificationDisabled();

        if (currentBox.isBlank()) {
            Note note = this.getNote();

            if (note != null) {
                ClueID cid = getClueID();
                int cluePos = currentBox.getCluePosition(cid);
                int length = this.getCurrentWordLength();
                if (cluePos >= 0 && cluePos < length)
                    note.deleteScratchLetterAt(cluePos);
                flagChange(getHighlightLetter());
            }
        }

        this.previousLetter();

        popNotificationDisabled();
        notifyChange();
    }

    /**
     * Returns true if the current letter should not be deleted
     *
     * E.g. because it is correct and show errors is show. Being blank
     * is not a reason for not deleting the current box.
     */
    private boolean isDontDeleteCurrent() {
        Box currentBox = getCurrentBox();

        if (currentBox == null)
            return true;

        // Prohibit deleting correct letters
        boolean skipCorrect = (
            preserveCorrectLettersInShowErrors
            && Objects.equals(
                currentBox.getResponse(), currentBox.getSolution()
            )
            && this.isShowErrors()
        );

        // don't delete crossing doesn't make sense for acrostic
        boolean skipAdjacent =
            deleteCrossingMode != DeleteCrossingMode.DELETE
            && !Puzzle.Kind.ACROSTIC.equals(puzzle.getKind())
            && currentBoxHasFilledAdjacent(
                deleteCrossingMode == DeleteCrossingMode.PRESERVE_FILLED_WORDS
            );

        return skipCorrect || skipAdjacent;
    }

    /**
     * Where current cell has an adjacent word with filled cells
     *
     * Looks at words that have the currently selected cell, but are not
     * the currently selected word (crossing). Determines whether cell
     * is part of a crossing word that is completed by checking if
     * adjacent cells have entries in the crossing word. If
     * filledWordsOnly is true, then the entire crossing word must be
     * filled for the cell to be considered to have an adjacent word
     * will filled cells. Else, just neighbouring cells in the crossing
     * word have to be filled.
     */
    private boolean currentBoxHasFilledAdjacent(boolean filledWordsOnly) {
        ClueID currentCID = getClueID();
        if (currentCID == null)
            return false;

        Position currentPos = getHighlightLetter();
        Box currentBox = getCurrentBox();
        Set<ClueID> currentBoxClues = currentBox.getIsPartOfClues();

        for (ClueID otherCID : currentBoxClues) {
            if (currentCID.equals(otherCID))
                continue;

            if (filledWordsOnly) {
                return isFilledClueID(otherCID);
            } else {
                Zone zone = puzzle.getClue(otherCID).getZone();
                int curPos = zone.indexOf(currentPos);
                Position posBefore = (curPos > 0)
                    ? zone.getPosition(curPos - 1)
                    : null;

                Box boxBefore = puzzle.checkedGetBox(posBefore);
                if (!Box.isBlock(boxBefore) && !boxBefore.isBlank())
                    return true;

                Position posAfter = (curPos < zone.size() - 1)
                    ? zone.getPosition(curPos + 1)
                    : null;

                Box boxAfter = puzzle.checkedGetBox(posAfter);
                if (!Box.isBlock(boxAfter) && !boxAfter.isBlank())
                    return true;
            }
        }

        return false;
    }

    /**
     * Ignored if clue not on board
     */
    public void jumpToClue(ClueID clueID) {
        if (clueID == null)
            return;

        pushNotificationDisabled();

        Position pos = getClueStart(clueID);

        if (pos != null) {
            setHighlightLetter(pos);
            puzzle.setCurrentClueID(clueID);
        }

        popNotificationDisabled();

        if (pos != null) {
            // no need to flag changed words, notifyChange will take
            // care of that
            notifyChange();
        }
    }

    public void jumpToClue(Clue clue) {
        if (clue != null)
            jumpToClue(clue.getClueID());
    }

    /**
     * Try to jump to given number
     *
     * Search current list, then all lists, do nothing if not found
     */
    public void jumpToClue(String number) {
        ClueList clues = getCurrentClueList();
        Clue clue = clues.getClueByNumber(number);
        if (clue != null) {
            jumpToClue(clue);
        } else {
            Puzzle puz = getPuzzle();
            for (String clueListName : sortedClueListNames) {
                clues = puz.getClues(clueListName);
                clue = clues.getClueByNumber(number);
                if (clue != null) {
                    jumpToClue(clue);
                    return;
                }
            }
        }
    }

    /**
     * Ignored if clue not on board
     */
    public void jumpToClueEnd(ClueID clueID) {
        if (clueID == null)
            return;

        pushNotificationDisabled();

        Position pos = getClueEnd(clueID);

        if (pos != null) {
            setHighlightLetter(pos);
            puzzle.setCurrentClueID(clueID);
        }

        popNotificationDisabled();

        if (pos != null) {
            // no need to flag changed words, notifyChange will take
            // care of that
            notifyChange();
        }
    }

    public void jumpToClueEnd(Clue clue) {
        if (clue != null)
            jumpToClueEnd(clue.getClueID());
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveDown() {
        return moveDelta(1, 0);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveLeft() {
        return moveDelta(0, -1);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveRight() {
        return moveDelta(0, 1);
    }

    /**
     * Move in direction, or not if no position found
     */
    public Word moveUp() {
        return moveDelta(-1, 0);
    }

    /**
     * Finds next box in direction
     *
     * @return next box, or null
     */
    public Position findNextDelta(
        Position original, boolean skipCompleted, int drow, int dcol
    ) {
        Position next = new Position(
            original.getRow() + drow, original.getCol() + dcol
        );

        if (
            next.getCol() < 0
            || next.getRow() < 0
            || next.getCol() >= puzzle.getWidth()
            || next.getRow() >= puzzle.getHeight()
        ) {
            return null;
        }

        Box value = puzzle.checkedGetBox(next);

        if (Box.isBlock(value) || skipBox(value, skipCompleted)) {
            next = findNextDelta(next, skipCompleted, drow, dcol);
        }

        return next;
    }

    /**
     * Move in direction, or not if no position found
     *
     * If toggleBeforeMove is enabled it will toggle the current
     * position rather than change clue to a clue shared with the new
     * cell. If no overlap, just move.
     *
     * TODO: write tests
     */
    public Word moveDelta(int drow, int dcol) {
        Word w = getCurrentWord();
        Position oldPos = getHighlightLetter();
        Position newPos = findNextDelta(oldPos, false, drow, dcol);
        Box newBox = puzzle.checkedGetBox(newPos);

        if (newPos == null || Box.isBlock(newBox))
            return w;

        Box oldBox = puzzle.checkedGetBox(oldPos);
        Collection<ClueID> newBoxClues = newBox.getIsPartOfClues();
        ClueID curClue = w.getClueID();

        if (
            toggleBeforeMove
            && !Box.isBlock(oldBox)
            && curClue != null
            && !newBoxClues.contains(curClue)
        ) {
            // moving out of clue, need to toggle if possible
            Collection<ClueID> oldBoxClues = oldBox.getIsPartOfClues();

            Optional<ClueID> nextClue
                = newBoxClues.stream()
                    .filter(oldBoxClues::contains)
                    .findFirst();

            if (nextClue.isPresent()) {
                puzzle.setCurrentClueID(nextClue.get());
                notifyChange();
            } else {
                setHighlightLetter(newPos);
            }
        } else {
            setHighlightLetter(newPos);
        }

        return w;
    }

    /**
     * Move to the next position in the current word zone
     *
     * Returns new position, or null if run out
     */
    public Position findNextZone(Position original, boolean skipCompleted) {
        return findZoneDelta(original, skipCompleted, 1);
    }

    /**
     * Move to the next position in zone
     *
     * Does not move if a new position can't be found
     */
    public Word moveZoneForward(boolean skipCompleted) {
        return moveZone(skipCompleted, 1);
    }

    /**
     * Move to the previous position in the current word zone
     *
     * Returns new position, or null if run out
     */
    public Position findPrevZone(Position original, boolean skipCompleted) {
        return findZoneDelta(original, skipCompleted, -1);
    }

    /**
     * Move to the prev position in zone
     *
     * Does not move if a new position can't be found
     */
    public Word moveZoneBack(boolean skipCompleted) {
        return moveZone(skipCompleted, -1);
    }

    /**
     * Find next position in the current word zone by delta steps
     *
     * Returns new position, or null if none found
     */
    public Position findZoneDelta(
        Position original, boolean skipCompleted, int delta
    ) {
        Zone zone = getCurrentZone();

        if (zone == null)
            return null;

        int index;

        if (Objects.equals(original, zone.getPosition(lastFoundZoneIndex))) {
            index = lastFoundZoneIndex;
        } else {
            index = zone.indexOf(original);
        }

        if (index < 0)
            return null;

        int newIndex = findZoneDelta(index, skipCompleted, delta);

        if (newIndex < 0) {
            lastFoundZoneIndex = -1;
            return null;
        } else {
            lastFoundZoneIndex = newIndex;
            return zone.getPosition(newIndex);
        }
    }

    /**
     * As findZoneDelta but with index of position as first argument
     */
    public int findZoneDelta(
        int original, boolean skipCompleted, int delta
    ) {
        Zone zone = getCurrentZone();

        if (zone == null)
            return -1;

        int nextIdx = original + delta;
        if (nextIdx < 0 || nextIdx >= zone.size()) {
            return -1;
        } else {
            Position next = zone.getPosition(nextIdx);
            Box box = puzzle.checkedGetBox(next);
            if (Box.isBlock(box) || skipBox(box, skipCompleted)) {
                nextIdx = findZoneDelta(nextIdx, skipCompleted, delta);
            }
            return nextIdx;
        }
    }

    /**
     * Moves by delta along the zone
     *
     * Does not move if no new position found
     */
    public Word moveZone(boolean skipCompleted, int delta) {
        Word w = this.getCurrentWord();
        Position oldPos = getHighlightLetter();
        Position newPos = findZoneDelta(oldPos, skipCompleted, delta);
        if (newPos != null)
            setHighlightLetter(newPos);
        return w;
    }

    public Word nextLetter(boolean skipCompletedLetters) {
        return this.movementStrategy.move(this, skipCompletedLetters);
    }

    public Word nextLetter() {
        return nextLetter(this.skipCompletedLetters);
    }

    /**
     * Jump to next word, regardless of movement strategy
     *
     * Still skips completed letters if setting chosen
     */
    public Word nextWord() {
        Word previous = this.getCurrentWord();
        Position curPos = this.getHighlightLetter();
        Position newPos = getClueEnd(getClueID());

        pushNotificationDisabled();

        if (!Objects.equals(newPos, curPos)) {
            setHighlightLetter(newPos);
        }

        MovementStrategy.MOVE_NEXT_CLUE.move(this, skipCompletedLetters);

        popNotificationDisabled();

        notifyChange();

        return previous;
    }

    /**
     * Insert the answer into the current word
     *
     * Ignores length mismatch and goes ahead anyway, stopping when it runs out
     * of input or space.
     */
    public void playAnswer(String answer) {
        if (answer == null)
            return;

        Position startPos = getHighlightLetter();
        Zone zone = getCurrentZone();
        if (zone == null)
            return;

        pushNotificationDisabled();

        int idx = 0;
        for (Position pos : zone) {
            if (idx >= answer.length())
                break;

            Box b = puzzle.checkedGetBox(pos);
            if (Box.isBlock(b))
                break;

            if (
                !preserveCorrectLettersInShowErrors
                || !Objects.equals(b.getResponse(), b.getSolution())
                || !isShowErrors()
            ) {
                b.setResponse(answer.charAt(idx));
                b.setResponder(this.responder);
                flagChange(pos);
            }

            idx += 1;
        }

        Position currentPos = getHighlightLetter();
        if (!Objects.equals(currentPos, startPos))
            setHighlightLetter(startPos);

        popNotificationDisabled();

        notifyChange();
    }

    /**
     * Clear the current word
     */
    public void clearWord() {
        Position startPos = getHighlightLetter();
        Zone zone = getCurrentZone();

        if (zone == null || zone.isEmpty())
            return;

        pushNotificationDisabled();
        for (Position pos : zone) {
            Position current = getHighlightLetter();
            if (!Objects.equals(current, pos))
                setHighlightLetter(pos);
            deleteOrUndoLetter();
        }

        Position currentPos = getHighlightLetter();
        if (!Objects.equals(currentPos, startPos))
            setHighlightLetter(startPos);

        popNotificationDisabled();
        notifyChange();
    }

    public Word playLetter(char letter) {
        return playLetter(String.valueOf(letter));
    }

    public Word playLetter(String letter) {
        Position pos = getHighlightLetter();
        Box b = puzzle.checkedGetBox(pos);

        if (Box.isBlock(b)) {
            return null;
        }

        if (
            preserveCorrectLettersInShowErrors
            && Objects.equals(b.getResponse(), b.getSolution())
            && isShowErrors()
        ) {
            // Prohibit replacing correct letters
            return this.getCurrentWord();
        } else {
            pushNotificationDisabled();

            pushPlayLetterUndoStackCurrent();

            b.setResponse(letter);
            b.setResponder(this.responder);
            flagChange(pos);

            // movement normally clears undo stack but we want to keep
            // it here
            playLetterUndoStack.pushClearDisabled();
            Word next = this.nextLetter();
            playLetterUndoStack.popClearDisabled();

            popNotificationDisabled();
            notifyChange();

            return next;
        }
    }

    public void playScratchLetter(char letter) {
        Position highlightPos = getHighlightLetter();
        Box box = puzzle.checkedGetBox(highlightPos);
        if (Box.isBlock(box))
            return;

        pushNotificationDisabled();

        ClueID cid = getClueID();
        Note note = getNote();
        int length = getCurrentWordLength();

        // Create a note for this clue if we don't already have one
        if (note == null) {
            note = new Note(length);
            this.puzzle.setNote(cid, note);
        }

        // Update the scratch text
        int pos = box.getCluePosition(cid);
        if (pos >= 0 && pos < length) {
            note.setScratchLetter(pos, letter);
            flagChange(highlightPos);
        }

        nextLetter();
        popNotificationDisabled();

        notifyChange();
    }

    public Word previousLetter() {
        return movementStrategy.back(this);
    }

    /**
     * Moves to start of previous word regardless of movement strategy
     *
     * Start takes skipCompletedLetters into account and highlights
     * first empty cell in previous word. If no empty cell, then just
     * start of word.
     */
    public Word previousWord() {
        ClueID previousCID = getClueID();
        Word previous = getCurrentWord();

        Position curPos = getHighlightLetter();
        Position newPos = getClueStart(getClueID());

        pushNotificationDisabled();

        if (!Objects.equals(curPos, newPos))
            this.setHighlightLetter(newPos);

        boolean goBack = true;
        while (goBack) {
            // only go back again if discovered needed later
            goBack = false;

            // Idea: use back for move_next_clue, which takes us to
            // previous clue. Move to the start of that clue. Then
            // search for blank cell to move to. If none found, try
            // going around the loop again, unless we've already reached
            // the originally selected clue

            MovementStrategy.MOVE_NEXT_CLUE.back(this);

            ClueID newCID = getClueID();
            Position start = getClueStart(newCID);
            if (!Objects.equals(start, getHighlightLetter()))
                setHighlightLetter(start);

            Box box = puzzle.checkedGetBox(start);
            if (box != null && skipBox(box, skipCompletedLetters)) {
                moveZoneForward(skipCompletedLetters);
                goBack = Objects.equals(start, getHighlightLetter())
                    && !Objects.equals(previousCID, newCID);
            }
        }

        popNotificationDisabled();

        notifyChange();

        return previous;
    }

    public Position revealInitialLetter() {
        Position highlightLetter = getHighlightLetter();
        Box b = puzzle.checkedGetBox(highlightLetter);

        if (!Box.isBlock(b) && b.hasInitialValue()) {
            if (!Objects.equals(b.getResponse(), b.getInitialValue())) {
                b.setResponse(b.getInitialValue());
                flagChange(highlightLetter);
                notifyChange();
                return highlightLetter;
            }
        }

        return null;
    }

    public void revealInitialLetters() {
        boolean changed = false;

        for (int row = 0; row < puzzle.getHeight(); row++) {
            for (int col = 0; col < puzzle.getWidth(); col++) {
                Box b = puzzle.checkedGetBox(row, col);
                boolean reveal =
                    !Box.isBlock(b) && b.hasInitialValue()
                    && !Objects.equals(b.getResponse(), b.getInitialValue());

                if (reveal) {
                    b.setResponse(b.getInitialValue());
                    flagChange(new Position(row, col));
                    changed = true;
                }
            }
        }

        if (changed)
            notifyChange();
    }

    public Position revealLetter() {
        Position highlightLetter = getHighlightLetter();
        Box b = puzzle.checkedGetBox(highlightLetter);

        if (!Box.isBlock(b)) {
            boolean correctResponse
                = Objects.equals(b.getSolution(), b.getResponse());
            if (!correctResponse) {
                b.setCheated(true);
                b.setResponse(b.getSolution());
                flagChange(highlightLetter);
                notifyChange();
                return highlightLetter;
            }
        }

        return null;
    }

    /**
     * Reveals the correct answers for any "red" squares on the board.
     *
     * This covers hidden and visible incorrect responses, as well as squares that are marked as
     * "cheated" from previously erased incorrect responses.
     *
     * @return
     */
    public List<Position> revealErrors() {
        ArrayList<Position> changes = new ArrayList<Position>();
        Box[][] boxes = getBoxes();

        for (int row = 0; row < puzzle.getHeight(); row++) {
            for (int col = 0; col < puzzle.getWidth(); col++) {
                Box b = boxes[row][col];
                if (!Box.isBlock(b)) {
                    boolean correctResponse
                        = Objects.equals(b.getSolution(), b.getResponse());
                    if (b.isCheated() || (!b.isBlank() && !correctResponse)) {
                        b.setCheated(true);
                        b.setResponse(b.getSolution());
                        Position pos = new Position(row, col);
                        flagChange(pos);
                        changes.add(pos);
                    }
                }
            }
        }

        notifyChange();

        return changes;
    }

    public List<Position> revealPuzzle() {
        ArrayList<Position> changes = new ArrayList<Position>();
        Box[][] boxes = getBoxes();

        for (int row = 0; row < puzzle.getHeight(); row++) {
            for (int col = 0; col < puzzle.getWidth(); col++) {
                Box b = boxes[row][col];
                if (!Box.isBlock(b)) {
                    boolean correctResponse
                        = Objects.equals(b.getSolution(), b.getResponse());
                    if (!correctResponse) {
                        b.setCheated(true);
                        b.setResponse(b.getSolution());
                        Position pos = new Position(row, col);
                        flagChange(pos);
                        changes.add(pos);
                    }
                }
            }
        }

        notifyChange();

        return changes;
    }

    public List<Position> revealWord() {
        ArrayList<Position> changes = new ArrayList<Position>();
        Zone zone = getCurrentZone();
        if (zone == null || zone.isEmpty())
            return changes;

        Position curPos = getHighlightLetter();
        Position startPos = zone.getPosition(0);

        pushNotificationDisabled();

        if (!Objects.equals(curPos, startPos))
            setHighlightLetter(startPos);

        for (int i = 0; i < zone.size(); i++) {
            Position p = revealLetter();
            if (p != null) {
                flagChange(p);
                changes.add(p);
            }
            nextLetter(false);
        }

        popNotificationDisabled();

        setHighlightLetter(curPos);

        return changes;
    }

    public boolean skipPosition(Position p, boolean skipCompleted) {
        Box box = puzzle.checkedGetBox(p);
        return Box.isBlock(box) ? false : skipBox(box, skipCompleted);
    }

    public boolean skipBox(Box b, boolean skipCompleted) {
        return skipCompleted
            && !b.isBlank()
            && (
                !this.isShowErrors()
                || Objects.equals(b.getResponse(), b.getSolution())
            );
    }

    /**
     * Toggle the board selection
     *
     * This is more like choose the appropriate direction. If the
     * current position is in the current clue, change to the next
     * direction. If the current position isn't in the current clue,
     * update the clue/direction to contain the current position. Favour
     * the current direction first.
     */
    public Word toggleSelection() {
        playLetterUndoStack.clearIfEnabled();

        Word w = this.getCurrentWord();

        Box box = puzzle.checkedGetBox(getHighlightLetter());
        if (Box.isBlock(box))
            return w;

        boolean changed = false;

        NavigableSet<ClueID> boxClues = box.getIsPartOfClues();

        if (boxClues.isEmpty()) {
            // if is a detached cell, toggle between selecting all
            // detached cells and just this single cell. Only toggle if
            // we were previously in a detached cell, else keep the
            // current detached "direction".
            changed = true;
            if (getClueID() == null)
                selectDetachedZone = !selectDetachedZone;
            puzzle.setCurrentClueID(null);
        } else {
            ClueID curCid = getClueID();

            // if in current clue, toggle, else try to stay in same list
            if (curCid != null && boxClues.contains(curCid)) {
                ClueID newCid = boxClues.higher(curCid);
                if (newCid == null)
                    newCid = boxClues.first();
                puzzle.setCurrentClueID(newCid);
                changed = !Objects.equals(curCid, newCid);
            } else {
                ClueID newCid = null;
                if (curCid != null)
                    newCid = box.getIsPartOfClue(curCid.getListName());
                if (newCid == null)
                    newCid = boxClues.first();
                puzzle.setCurrentClueID(newCid);
                changed = true;
            }
        }

        if (changed) {
            // don't flag changed selected words (we might have multiple
            // toggles). notifyChange will take care of the final result
            notifyChange();
        }

        return w;
    }

    public void addListener(PlayboardListener listener) {
        if (isNotifying)
            addPendingListenerAddition(listener);
        else
            listeners.add(listener);
    }

    public void removeListener(PlayboardListener listener) {
        if (isNotifying())
            addPendingListenerRemoval(listener);
        else
            listeners.remove(listener);
    }

    public void editClue(Clue clue, String newHint) {
        clue.setHint(newHint);
        flagClueChange(clue);
        notifyChange();
    }

    /**
     * Call this instead of puzzle.flagClue to update listeners
     */
    public void flagClue(Clue clue, boolean flag) {
        if (clue != null) {
            clue.setFlagged(flag);
            flagClueChange(clue);
            notifyChange();
        }
    }

    /**
     * Call this instead of setting color directly to update listeners
     */
    public void setFlagColor(Clue clue, int color) {
        if (clue != null) {
            clue.setFlagColor(color);
            flagClueChange(clue);
            notifyChange();
        }
    }

    /**
     * Call this instead of setting box flag directly to update listeners
     */
    public void flagPosition(Position pos, boolean flag) {
        Box box = puzzle.checkedGetBox(pos);
        if (box != null) {
            box.setFlagged(flag);
            flagChange(pos);
            notifyChange();
        }
    }

    /**
     * Call instead of setting box color directly to update listeners
     */
    public void setFlagColor(Position pos, int color) {
        Box box = puzzle.checkedGetBox(pos);
        if (box != null) {
            box.setFlagColor(color);
            flagChange(pos);
            notifyChange();
        }
    }

    private void flagNotifying(boolean isNotifying) {
        // if stopping notifying, clear pending removals
        boolean stoppingNotifying = !isNotifying && this.isNotifying;
        this.isNotifying = isNotifying;

        if (stoppingNotifying) {
            for (PlayboardListener listener : pendingListenerRemovals) {
                removeListener(listener);
            }
            pendingListenerRemovals.clear();
            for (PlayboardListener listener : pendingListenerAdditions) {
                addListener(listener);
            }
            pendingListenerAdditions.clear();
        }

        this.isNotifying = isNotifying;
    }

    private boolean isNotifying() {
        return isNotifying;
    }

    private void addPendingListenerRemoval(PlayboardListener listener) {
        pendingListenerRemovals.add(listener);
    }

    private void addPendingListenerAddition(PlayboardListener listener) {
        pendingListenerAdditions.add(listener);
    }

    private void notifyChange() { notifyChange(false); }

    private void notifyChange(boolean wholeBoard) {
        if (notificationDisabledDepth == 0) {
            flagNotifying(true);

            int lastHistoryIndex = updateHistory();
            boolean historyChange = (getClueID() != null);

            Word currentWord = getCurrentWord();

            if (!Objects.equals(currentWord, previousWord)) {
                // "unusual" is to stop me calling it elsewhere
                flagUnusualWordChange(previousWord);
                flagUnusualWordChange(currentWord);
            }

            Collection<Position> posChanges
                = wholeBoard ? null : getPositionChanges();
            Collection<ClueID> clueEdits = getChangedClueIDs();

            notificationChanges.setValues(
                currentWord, previousWord, previousPosition, posChanges,
                historyChange, lastHistoryIndex, clueEdits
            );

            for (PlayboardListener listener : listeners) {
                listener.onPlayboardChange(notificationChanges);
            }

            previousWord = currentWord;
            previousPosition = getHighlightLetter();

            clearChanges();

            flagNotifying(false);
        }
    }

    private void pushNotificationDisabled() {
        notificationDisabledDepth += 1;
    }

    private void popNotificationDisabled() {
        if (notificationDisabledDepth > 0)
            notificationDisabledDepth -= 1;
    }

    /**
     * Returns lastHistoryIndex
     *
     * Note, will be -1 if the current clue wasn't in the history before and if
     * there is no current clue. Detect latter case with getClueID() == null.
     */
    private int updateHistory() {
        return updateHistory(getClueID());
    }

    /**
     * Version without this escape for constructor
     */
    private int updateHistory(ClueID cid) {
        if (cid == null)
            return -1;
        return puzzle.updateHistory(cid);
    }

    /**
     * Find the first clue with a non-empty zone in board order, or all
     * back to first clue with zone found
     *
     * Requires sortedClueListNames
     */
    private void selectFirstPosition() {
        for (ClueID cid : puzzle.getBoardClueIDs()) {
            Clue clue = puzzle.getClue(cid);
            if (clue.hasZone()) {
                puzzle.setPosition(clue.getZone().getPosition(0));
                puzzle.setCurrentClueID(cid);
                return;
            }
        }
        // try all clues
        for (Clue clue : puzzle.getAllClues()) {
            if (clue.hasZone()) {
                puzzle.setPosition(clue.getZone().getPosition(0));
                puzzle.setCurrentClueID(clue.getClueID());
                return;
            }
        }
        // fall back to first cell in grid
        int width = puzzle.getWidth();
        int height = puzzle.getHeight();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (!Box.isBlock(puzzle.checkedGetBox(row, col))) {
                    puzzle.setPosition(new Position(row, col));
                    return;
                }
            }
        }
        throw new IllegalArgumentException(
            "Can't handled grids with no cells"
        );
    }

    /**
     * Returns the zone of the clue, or null
     */
    private Zone getZone(ClueID clueID) {
        Clue clue = puzzle.getClue(clueID);
        return (clue == null) ? null : clue.getZone();
    }

    /**
     * Get start of clue position
     *
     * @return null if no such clue or not on board
     */
    private Position getClueStart(ClueID clueID) {
        Clue clue = puzzle.getClue(clueID);
        Zone zone = clue == null ? null : clue.getZone();

        if (zone == null || zone.isEmpty())
            return null;
        else
            return zone.getPosition(0);
    }

    /**
     * Get end of clue position
     *
     * @return null if no such clue or not on board
     */
    private Position getClueEnd(ClueID clueID) {
        Clue clue = puzzle.getClue(clueID);
        Zone zone = clue == null ? null : clue.getZone();

        if (zone == null || zone.isEmpty())
            return null;
        else
            return zone.getPosition(zone.size() - 1);
    }

    private int getCurrentWordLength() {
        Zone zone = getCurrentZone();
        return zone == null ? 0 : zone.size();
    }

    private void clearChanges() {
        changedPositions.clear();
        changedClueIDs.clear();
    }

    private Collection<Position> getPositionChanges() {
        return changedPositions;
    }

    private Collection<ClueID> getChangedClueIDs() {
        return changedClueIDs;
    }

    private void flagChange(Position... positions) {
        for (Position pos : positions) {
            if (pos != null)
                changedPositions.add(pos);
        }
    }

    private void flagClueChange(Clue clue) {
        changedClueIDs.add(clue.getClueID());
    }

    /**
     * Don't use these for changes to selected word
     *
     * notifyChange will take care of that
     */
    private void flagUnusualWordChange(Word word) {
        Zone zone = (word == null) ? null : word.getZone();
        flagUnusualZoneChange(zone);
    }

    /**
     * Don't use these for changes to selected word
     *
     * notifyChange will take care of that
     */
    private void flagUnusualZoneChange(Zone zone) {
        if (zone == null)
            return;
        for (Position pos : zone)
            flagChange(pos);
    }

    /**
     * Get currently selected zone
     *
     * Normally the highlight letter is in the currently selected clue.
     *
     * If there is no currently selected clue, return the zone of
     * detached cells if the highlighted cell is in them. Else, just
     * return a zone with the current position.
     */
    public Zone getCurrentZone() {
        Clue clue = getPuzzle().getClue(getClueID());
        if (clue == null || !clue.hasZone()) {
            Position highlight = getHighlightLetter();
            Puzzle puz = getPuzzle();
            Box box = puz.checkedGetBox(highlight);
            if (!Box.isBlock(box) && !box.isPartOfClues()) {
                return getDetachedCellsZone();
            } else {
                Zone zone = new Zone();
                zone.addPosition(highlight);
                return zone;
            }
        } else {
            return clue.getZone();
        }
    }

    /**
     * A word for all the cells that are not part of a clue
     *
     * Tries to order the detached cells "intuitively". A sequence of
     * across detached cells will go together. Similarly, a sequence of
     * down detached cells go together.
     *
     * Scan left-to-right, top-to-bottom. First look for an across seq
     * of more than one box, then add. If there isn't one, try finding a
     * down sequence. Add whatever sequence found. Don't add boxes that
     * have been added before. Then move on with the scan.
     */
    private Zone getDetachedCellsZone() {
        return getDetachedCellsZone(getPuzzle());
    }

    /**
     * Version without this escape for constructor
     */
    private Zone getDetachedCellsZone(Puzzle puz) {
        if (detachedCellsZone != null)
            return detachedCellsZone;

        detachedCellsZone = new Zone();
        // because Zone.hasPosition is O(n)
        Set<Position> oldPositions = new HashSet<>();

        for (int row = 0; row < puz.getHeight(); row++) {
            for (int col = 0; col < puz.getWidth(); col++) {
                Box box = puz.checkedGetBox(row, col);
                if (!Box.isBlock(box) && !box.isPartOfClues()) {
                    Position pos = new Position(row, col);
                    if (oldPositions.contains(pos))
                        continue;

                    Zone across = PuzzleUtils.getAcrossZone(puz, pos);
                    if (across.size() > 1) {
                        addZoneToDetached(
                            across, detachedCellsZone, oldPositions, puz
                        );
                    } else {
                        Zone down = PuzzleUtils.getDownZone(puz, pos);
                        addZoneToDetached(
                            down, detachedCellsZone, oldPositions, puz
                        );
                    }
                }
            }
        }

        return detachedCellsZone;
    }

    /**
     * Helper for getDetachedCellsWord
     *
     * Put the cells from the zone into detachedZone up to the first
     * that shouldn't be in there. Skip oldPositions, and add to
     * oldPositions those added to detachedZone.
     *
     * By shouldn't be there, we mean positions that are part of a clue.
     */
    private void addZoneToDetached(
        Zone zone, Zone detachedZone, Set<Position> oldPositions, Puzzle puz
    ) {
        for (Position zonePos : zone) {
            if (!oldPositions.contains(zonePos)) {
                Box zoneBox = puz.checkedGetBox(zonePos);

                // if isn't detached, abort
                if (Box.isBlock(zoneBox) || zoneBox.isPartOfClues())
                    return;

                if (!oldPositions.contains(zonePos)) {
                    detachedZone.addPosition(zonePos);
                    oldPositions.add(zonePos);
                }
            }
        }
    }

    /**
     * Add current board position to play letter undo stack
     */
    private void pushPlayLetterUndoStackCurrent() {
        Position pos = getHighlightLetter();
        Box box = puzzle.checkedGetBox(pos);
        String response = (box == null) ? null : box.getResponse();
        playLetterUndoStack.push(pos, getClueID(), response);
    }

    private void pauseDelete() {
        pauseDeleteUntil = System.currentTimeMillis() + PAUSE_DELETE_DELAY;
    }

    /**
     * If delete is paused by pauseDelete
     */
    private boolean isPausedDelete() {
        return (
            0 <= pauseDeleteUntil
            && System.currentTimeMillis() < pauseDeleteUntil
        );
    }

    /**
     * A word on the grid
     *
     * A Zone and possibly the clue it goes with (null if no clue).
     */
    public static class Word {
        private final Zone zone;
        private final ClueID clueID;

        public Word(Zone zone, ClueID clueID) {
            this.zone = zone;
            this.clueID = clueID;
        }

        public Word(Zone zone) {
            this.zone = zone;
            this.clueID = null;
        }

        public Zone getZone() { return zone; }
        public ClueID getClueID() { return clueID; }

        public boolean checkInWord(Position pos) {
            return zone.hasPosition(pos);
        }

        public boolean checkInWord(int row, int col) {
            return zone.hasPosition(new Position(row, col));
        }

        public int indexOf(Position pos) {
            return zone.indexOf(pos);
        }

        /**
         * Length of word
         *
         * @return len or -1 if no zone
         */
        public int getLength() {
            return (zone == null) ? -1 : zone.size();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;

            if (o == null)
                return false;

            if (!(o instanceof Word))
                return false;

            Word check = (Word) o;

            return Objects.equals(zone, check.zone)
                && Objects.equals(clueID, check.clueID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(zone, clueID);
        }
    }

    /**
     * Playboard listeners will be updated when the highlighted letter
     * changes or the contents of a box changes.
     */
    public interface PlayboardListener {
        public void onPlayboardChange(PlayboardChanges changes);
    }

    public static class PlayboardChanges implements Cloneable {
        private Word currentWord;
        private Word previousWord;
        private Position previousPosition;
        private Collection<Position> cellChanges;
        private boolean historyChange;
        private int lastHistoryIndex;
        private Collection<ClueID> changedClueIDs;

        private static PlayboardChanges emptyChanges = new PlayboardChanges();
        static {
            emptyChanges.currentWord = null;
            emptyChanges.previousWord = null;
            emptyChanges.previousPosition = null;
            emptyChanges.cellChanges = Collections.emptySet();
            emptyChanges.historyChange = false;
            emptyChanges.lastHistoryIndex = -1;
            emptyChanges.changedClueIDs = Collections.emptySet();
        }

        public static PlayboardChanges getEmptyChanges() {
            return emptyChanges;
        }

        private void setValues(
            Word currentWord,
            Word previousWord,
            Position previousPosition,
            Collection<Position> cellChanges,
            boolean historyChange,
            int lastHistoryIndex,
            Collection<ClueID> changedClueIDs
        ) {
            this.currentWord = currentWord;
            this.previousWord = previousWord;
            this.previousPosition = previousPosition;
            this.cellChanges = cellChanges;
            this.historyChange = historyChange;
            this.lastHistoryIndex = lastHistoryIndex;
            this.changedClueIDs = changedClueIDs;
        }

        /**
         * The currently selected word
         *
         * May only be null if gotten via getEmptyChanges().
         */
        public Word getCurrentWord() { return currentWord; }

        /**
         * The word selected at last notification (may be null)
         */
        public Word getPreviousWord() { return previousWord; }

        /**
         * The position selected at last notification (may be null)
         */
        public Position getPreviousPosition() { return previousPosition; }

        /**
         * A set of changed cell positions since update
         *
         * Null means whole board changed. Fast lookup.
         */
        public Collection<Position> getCellChanges() { return cellChanges; }

        /**
         * True if something moved to the top of the history list
         *
         * See lastHistoryIndex. Note moving top item to top is a change!
         */
        public boolean isHistoryChange() { return historyChange; }

        /**
         * Where the current word used to sit in the history list
         *
         * Before it became the first :) Can be -1 if wasn't there
         * before or no current clue selected (so list unchanged).
         */
        public int getLastHistoryIndex() { return lastHistoryIndex; }

        /**
         * List of clues that have had num/hint edited or flagged
         */
        public Collection<ClueID> getChangedClueIDs() { return changedClueIDs; }
    }

    /**
     * Position of a play letter in undo stack
     */
    private static class PlayLetterInfo {
        private Position position;
        private ClueID clueID;
        private String oldResponse;

        public PlayLetterInfo(
            Position position, ClueID clueID, String oldResponse
        ) {
            this.position = position;
            this.clueID = clueID;
            this.oldResponse = oldResponse;
        }

        public Position getPosition() { return position; }
        public ClueID getClueID() { return clueID; }
        public String getOldResponse() { return oldResponse; }
    }

    /**
     * For managing an undo stack of played letters
     */
    private static class PlayLetterUndoStack {
        private int clearDisabledDepth = 0;
        private Stack<PlayLetterInfo> undoStack = new Stack<>();
        private boolean enabled;

        public PlayLetterUndoStack(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Stack will be cleared and not accept push if disabled
         */
        public void setEnabled(boolean enabled) {
            if (!enabled)
                undoStack.clear();
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Clear if clearing is enabled (not if whole stack is enabled)
         */
        public void clearIfEnabled() {
            if (!isClearDisabled())
                undoStack.clear();
        }

        public void pushClearDisabled() {
            clearDisabledDepth += 1;
        }

        public void popClearDisabled() {
            if (isClearDisabled())
                clearDisabledDepth -= 1;
        }

        private boolean isClearDisabled() {
            return clearDisabledDepth > 0;
        }

        public void push(Position position, ClueID clueID, String oldResponse) {
            if (!enabled)
                return;

            undoStack.push(new PlayLetterInfo(position, clueID, oldResponse));
        }

        public PlayLetterInfo pop() {
            return undoStack.pop();
        }

        public boolean isEmpty() {
            return undoStack.isEmpty();
        }
    }
}
