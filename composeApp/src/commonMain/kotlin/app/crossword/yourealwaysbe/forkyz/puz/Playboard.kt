
package app.crossword.yourealwaysbe.forkyz.puz

import kotlin.math.min
import kotlin.random.Random

import de.cketti.codepoints.codePointCount
import de.cketti.codepoints.deluxe.codePointSequence

import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.util.PuzzleUtils
import app.crossword.yourealwaysbe.forkyz.util.SortedSet
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

class Playboard(puzzle : Puzzle) {
    /**
     * What to do with cells that are filled by crossing words
     * DELETE: delete clears all cells
     * PRESERVE_FILLED_WORDS: delete does not delete cells in crossing
     * words if the crossing word is completely full
     * PRESERVE_FILLED_CELLS: delete does not delete cells if the
     * neighbouring cell of a crossing word is also filled
     */
    enum class DeleteCrossingMode {
        DELETE,
        PRESERVE_FILLED_WORDS,
        PRESERVE_FILLED_CELLS,
    }

    enum class ShowErrorsMode {
        NONE,
        CURSOR,
        FILLED_WORD_CELLS,
        FILLED_WORD_WORD,
        GRID,
    }

    var movementStrategy : MovementStrategy = MovementStrategy.MOVE_NEXT_ON_AXIS
    val puzzle : Puzzle

    /**
     * Who set the response if tracked
     */
    var responder : String? = null
    var isSkipCompletedLetters : Boolean = false

    private val sortedClueListNames = puzzle.clueListNames.sorted()
    private var showErrorsMode = ShowErrorsMode.NONE
    private var preserveCorrectLettersInShowErrors = false
    private var toggleBeforeMove = false
    private var deleteCrossingMode = DeleteCrossingMode.DELETE
    private var scratchMode = false
    private var splitResponses = false
    private var previousWord : Word? = null
    private var previousPosition : Position? = null

    // used by findZoneDelta to track last index to handle clues with
    // repeated positions
    private var lastFoundZoneIndex = -1

    // Zone for the cells without clues attached
    private var detachedCellsZone : Zone? = null

    // to toggle between selecting zone and single cell
    // false so toggled to true the first time the detached cells are
    // selected
    private var selectDetachedZone = true

    private val listeners = mutableSetOf<PlayboardListener?>()
    private val pendingListenerRemovals = mutableSetOf<PlayboardListener?>()
    private val pendingListenerAdditions = mutableSetOf<PlayboardListener?>()

    private var isNotifying = false
    private var notificationDisabledDepth = 0
    private var pauseDeleteUntil : Long = -1

    private val changedPositions = mutableSetOf<Position>()
    private val changedClueIDs = mutableSetOf<ClueID>()

    // reuse same one for all notifications
    private val playLetterUndoStack = PlayLetterUndoStack(false)

    constructor(
        puzzle : Puzzle,
        movementStrategy : MovementStrategy,
        preserveCorrectLettersInShowErrors : Boolean,
        deleteCrossingMode : DeleteCrossingMode,
        showErrorsMode : ShowErrorsMode,
        toggleBeforeMove : Boolean,
        skipCompletedLetters : Boolean,
        undoEnabled : Boolean,
        scratchMode : Boolean,
        splitResponses : Boolean,
    ) : this(
        puzzle,
        movementStrategy,
        preserveCorrectLettersInShowErrors,
        deleteCrossingMode,
    ) {
        this.showErrorsMode = showErrorsMode
        this.toggleBeforeMove = toggleBeforeMove
        this.isSkipCompletedLetters = skipCompletedLetters
        playLetterUndoStack.setEnabled(undoEnabled)
        this.scratchMode = scratchMode
        this.splitResponses = splitResponses
    }

    constructor(
        puzzle : Puzzle,
        movementStrategy : MovementStrategy,
        preserveCorrectLettersInShowErrors : Boolean,
        deleteCrossingMode : DeleteCrossingMode,
    ) : this(puzzle, movementStrategy) {
        this.preserveCorrectLettersInShowErrors =
            preserveCorrectLettersInShowErrors
        this.deleteCrossingMode = deleteCrossingMode
    }

    constructor(
        puzzle : Puzzle,
        movementStrategy : MovementStrategy,
    ) : this(puzzle) {
        this.movementStrategy = movementStrategy
    }

    init {
        this.puzzle = puzzle

        // setup bookkeeping

        // coerce currentClueID to a clue in the current box if needed
        puzzle.checkedGetBox(puzzle.position)?.let { box ->
            val isPartOfClues = box.getIsPartOfClues()
            if (
                !isPartOfClues.isEmpty()
                && puzzle.currentClueID !in isPartOfClues
            ) {
                puzzle.currentClueID = isPartOfClues.first()
            }
        }

        val curID = puzzle.currentClueID

        updateHistory(curID)

        previousPosition = puzzle.position

        val clue = puzzle.getClue(curID)
        if (clue == null || !clue.hasZone()) {
            previousWord = Word(getDetachedCellsZone(puzzle))
        } else {
            previousWord = Word(clue.zone, clue.clueID)
        }
    }

    /**
     * Whether to record played letters and undo on delete
     */
    fun setPlayLetterUndoStackEnabled(enabled : Boolean) {
        playLetterUndoStack.setEnabled(enabled)
    }

    fun setPreserveCorrectLettersInShowErrors(value : Boolean) {
        this.preserveCorrectLettersInShowErrors = value
    }

    fun setToggleBeforeMove(value : Boolean) {
        this.toggleBeforeMove = value
    }

    /**
     * Whether to delete characters from crossing words
     */
    fun setDeleteCrossingMode(value : DeleteCrossingMode) {
        this.deleteCrossingMode = value
    }

    /**
     * Whether playResponse goes to scratch or to cell as normal
     */
    fun setScratchMode(scratchMode : Boolean) {
        this.scratchMode = scratchMode
    }

    /**
     * Whether playResponses get split into characters
     *
     * Normally a word will go into one cell. With this mode, the word
     * is split into characters and entered into neighbouring cells in
     * the clue. Spaces in playResponse will be ignored when multichars.
     */
    fun setSplitResponses(splitResponses : Boolean) {
        this.splitResponses = splitResponses
    }

    val boxes : Array<Array<Box?>>
        get() = puzzle.boxes

    val clue : Clue?
        /**
         * Returns null if no clue for current position
         */
        get() {
            return clueID?.let { puzzle.getClue(clueID) }
        }

    val clueID : ClueID?
        get() = puzzle.currentClueID

    val currentClueList : ClueList?
        /**
         * Gets the currently selected clue list from the puzzle
         *
         * Or null if none selected
         */
        get() {
            return currentClueListName?.let { puzzle.getClues(it) }
        }

    val currentClueListName : String?
        /**
         * Gets the currently selected clue list name from the puzzle
         *
         * Or null if none selected
         */
        get() {
            return clueID?.let { it.listName }
        }

    val clueNumber : String?
        /**
         * Clue number for current position or null if none
         */
        get() {
            return clueID?.let {
                puzzle.getClue(it)?.clueNumber
            }
        }

    val currentClueIndex : Int
        /**
         * Returns -1 if no current clue selected
         */
        get() {
            return clue?.clueID?.index ?: -1
        }

    val note : Note?
        get() {
            return this.clue?.let { puzzle.getNote(it.clueID) }
        }

    val currentBox : Box?
        get() = getCurrentBoxOffset(0, 0)

    val currentResponse : String?
        /**
         * The response in the currently highlighted box or null
         */
        get() {
            return currentBox?.response
        }

    /**
     * Get the box at the given offset from current box
     *
     * Null if no box
     */
    private fun getCurrentBoxOffset(
        offsetAcross : Int,
        offsetDown : Int,
    ) : Box? {
        val currentPos = highlightLetter
        val offAcross = currentPos.col + offsetAcross
        val offDown = currentPos.row + offsetDown
        return puzzle.checkedGetBox(offDown, offAcross)
    }

    val currentWord : Word
        get() {
            val word = getClueWord(clueID)
            if (word != null)
                return word

            if (selectDetachedZone) {
                return Word(getDetachedCellsZone())
            } else {
                val zone = Zone()
                zone.addPosition(highlightLetter)
                return Word(zone)
            }
        }

    /**
     * Return the word associated with the clue
     *
     * @return null if no clue or no zone for clue
     */
    fun getClueWord(clueID : ClueID?) : Word? {
        val clue = puzzle.getClue(clueID)
        if (clue == null || !clue.hasZone()) {
            return null
        } else {
            return Word(clue.zone, clue.clueID)
        }
    }

    val currentWordBoxes : List<Box>
        get() = getWordBoxes(currentWord)

    fun getWordBoxes(word : Word?) : List<Box> {
        val zone = word?.zone
        if (zone == null)
            return listOf<Box>()

        return (0 until zone.size()).map { i ->
            puzzle.checkedGetBox(zone.getPosition(i))
        }.filterNotNull()
    }

    /**
     * Set current word from string
     *
     * One character per box
     */
    fun setCurrentWordFromString(response : String) {
        val zone = currentZone
        if (zone == null)
            return

        val length = min(zone.size(), response.length)
        for (i in 0..<length) {
            val pos = zone.getPosition(i)
            puzzle.checkedGetBox(pos)?.let { box ->
                if (!isBlock(box))
                    box.response = response.get(i).toString()
            }
            flagChange(pos)
        }
        notifyChange()
    }

    fun setHighlightLetter(highlightLetter : Position?) : Word {
        playLetterUndoStack.clearIfEnabled()

        val w = this.currentWord

        if (highlightLetter == null)
            return w

        pushNotificationDisabled()

        val currentHighlight = this.highlightLetter

        if (highlightLetter == currentHighlight) {
            toggleSelection()
        } else {
            puzzle.checkedGetBox(highlightLetter)?.let { box ->
                if (!isBlock(box)) {
                    flagChange(currentHighlight, highlightLetter)

                    puzzle.position = highlightLetter

                    // if box is part of a clue, only toggle if it's not
                    // part of the current clue (i.e. toggle to the right
                    // clue selection)
                    // else it's not part of a clue (detached), so only
                    // toggle if there we're moving from a non-detached cell
                    // to a detached one.
                    if (box.isPartOfClues()) {
                        val zone = w.zone
                        if (!zone.hasPosition(highlightLetter)) {
                            toggleSelection()
                        }
                    } else if (this.clueID != null) {
                        toggleSelection()
                    }
                }
            }
        }

        popNotificationDisabled()

        notifyChange()

        return w
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
    fun setHighlightLetter(highlightLetter : Position, cid : ClueID?) : Word {
        if (cid == null)
            return setHighlightLetter(highlightLetter)

        playLetterUndoStack.clearIfEnabled()

        val w = currentWord

        val box = puzzle.checkedGetBox(highlightLetter)
        if (box == null || isBlock(box))
            return w

        val curHighlightLetter = this.highlightLetter
        val curCID = this.clueID

        val sameSelection = (
            curHighlightLetter == highlightLetter
            && curCID == cid
        )

        if (sameSelection)
            return w

        if (box.getIsPartOfClues().contains(cid)) {
            puzzle.position = highlightLetter
            puzzle.currentClueID = cid

            flagChange(curHighlightLetter, highlightLetter)
            // don't flag change of word as notify picks this up from
            // current/previous word (and this call could be
            // transitional)
            notifyChange()
        }

        return w
    }

    /**
     * Returns true if the position is part of a word
     *
     * Words may be single cells that are not part of any clue
     */
    fun isInWord(p : Position?) : Boolean {
        return !isBlock(puzzle.checkedGetBox(p))
    }

    // TODO: Puzzle has to enforce always a position
    val highlightLetter : Position
        get() = puzzle.position

    fun setShowErrorsMode(mode : ShowErrorsMode) {
        val changed = (showErrorsMode != mode)
        showErrorsMode = mode
        if (changed)
            notifyChange(wholeBoard = true)
    }

    fun getShowErrorsMode() : ShowErrorsMode = showErrorsMode

    val isShowErrors : Boolean
        get() = showErrorsMode != ShowErrorsMode.NONE

    /**
     * Whether any box in the clue's zone has an incorrect response
     */
    fun isHasErrorsClue(clueID : ClueID?) : Boolean {
        val zone = getZone(clueID) ?: return false
        for (pos in zone) {
            val box = puzzle.checkedGetBox(pos) ?: continue
            if (
                !box.isBlank()
                && box.hasSolution()
                && box.solution != box.response
            ) {
                return true
            }
        }
        return false
    }

    /**
     * If the clue has been filled in
     *
     * Always false if clue has a null or empty zone
     */
    fun isFilledClueID(clueID : ClueID?) : Boolean {
        val zone = getZone(clueID)

        if (zone == null || zone.isEmpty)
            return false

        for (pos in zone) {
            if (puzzle.checkedGetBox(pos)?.isBlank() ?: false)
                return false
        }

        return true
    }

    /**
     * Delete response according to scratchMode
     */
    fun deleteOrUndoResponse() : Word {
        if (scratchMode)
            return deleteScratchLetter()
        else
            return deleteOrUndoLetter()
    }

    /**
     * Handler for the backspace key.  Uses the following algorithm:
     * -If current box is empty, move back one character.  If not, stay still.
     * -Delete the letter in the current box.
     *
     * Prefer deleteOrUndoResponse unless letter not scratch needed.
     */
    fun deleteOrUndoLetter() : Word {
        var currentBox = this.currentBox
        var wordToReturn = this.currentWord

        if (isPausedDelete) {
            // pause again to make user stop sending rapid deletes
            pauseDelete()
            // notify needed for android keyboard (bit of a hack i
            // guess)
            notifyChange()
            return wordToReturn
        }

        pushNotificationDisabled()

        // do a standard delete if undo stack is empty
        if (playLetterUndoStack.isEmpty) {
            if ((currentBox?.isBlank() ?: true) || this.isDontDeleteCurrent) {
                wordToReturn = previousLetter()
                currentBox = this.currentBox
            }

            // If preserving letters, skip over them
            // break if we hit a wall or loop all the way around
            val startPosition = this.highlightLetter
            val startClueID = this.clueID
            var lastPosition : Position? = startPosition
            while (
                !(currentBox?.isBlank() ?: true)
                && this.isDontDeleteCurrent
            ) {
                previousLetter()

                val newPosition = this.highlightLetter
                val stuck = (
                    newPosition == startPosition
                    && startClueID == this.clueID
                ) || newPosition == lastPosition

                if (stuck)
                    break
                else
                    lastPosition = newPosition
            }

            if (!this.isDontDeleteCurrent) {
                // flag now while we know what's highlighted
                // the delete will unhighlight
                flagFilledWordErrorCells(this.clueID, currentZone)
                currentBox = this.currentBox
                currentBox?.setBlank()
                flagChange(this.highlightLetter)
            }
        } else {
            // Grab the last position a letter was entered and pop it
            // from the list.
            val undo = playLetterUndoStack.pop()
            val pos = undo.position
            // Jump back to last box and delete the letter.
            currentBox = puzzle.checkedGetBox(pos)
            if (currentBox != null) {
                // flag now while we know what's highlighted
                // the delete will unhighlight
                if (undo.oldResponse == Box.BLANK)
                    flagFilledWordErrorCells(this.clueID, currentZone)
                currentBox.response = undo.oldResponse
                playLetterUndoStack.pushClearDisabled()
                setHighlightLetter(pos)
                playLetterUndoStack.popClearDisabled()
                puzzle.currentClueID = undo.clueID
                flagChange(pos)
            }
            // pause delete when empty stack reached to provide a
            // natural break from held delete
            if (playLetterUndoStack.isEmpty)
                pauseDelete()
        }

        popNotificationDisabled()

        notifyChange()

        return wordToReturn
    }

    fun deleteScratchLetter() : Word {
        val prevWord = this.currentWord

        this.currentBox?.let { currentBox ->
            pushNotificationDisabled()

            if (currentBox.isBlank()) {
                var note = this.note

                if (note != null) {
                    this.clueID?.let { cid ->
                        val cluePos = currentBox.getCluePosition(cid)
                        val length = currentWordLength
                        if (cluePos >= 0 && cluePos < length) {
                            note = note.withDeleteScratchLetterAt(cluePos)
                            puzzle.setNote(cid, note)
                        }
                        flagChange(this.highlightLetter)
                    }
                }
            }

            previousLetter()

            popNotificationDisabled()
            notifyChange()
        }

        return prevWord
    }

    private val isDontDeleteCurrent : Boolean
        /**
         * Returns true if the current letter should not be deleted
         *
         * E.g. because it is correct and show errors is show. Being blank
         * is not a reason for not deleting the current box.
         */
        get() {
            val currentBox = this.currentBox
            if (currentBox == null)
                return true

            // Prohibit deleting correct letters
            val skipCorrect = (
                preserveCorrectLettersInShowErrors
                && currentBox.response == currentBox.solution
                && this.isShowErrors
            )

            // don't delete crossing doesn't make sense for acrostic
            val skipAdjacent = (
                deleteCrossingMode != DeleteCrossingMode.DELETE
                && (PuzzleKind.ACROSTIC != puzzle.kind)
                && currentBoxHasFilledAdjacent(
                    deleteCrossingMode
                        == DeleteCrossingMode.PRESERVE_FILLED_WORDS
                )
            )

            return skipCorrect || skipAdjacent
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
    private fun currentBoxHasFilledAdjacent(
        filledWordsOnly : Boolean,
    ) : Boolean {
        val currentCID = this.clueID
        if (currentCID == null)
            return false

        val currentBox = this.currentBox
        if (currentBox == null)
            return false

        val currentPos = this.highlightLetter
        val currentBoxClues = currentBox.getIsPartOfClues()

        for (otherCID in currentBoxClues) {
            if (currentCID == otherCID)
                continue

            if (filledWordsOnly) {
                return isFilledClueID(otherCID)
            } else {
                puzzle.getClue(otherCID)?.zone?.let { zone ->
                    val curPos = zone.indexOf(currentPos)
                    val posBefore = if (curPos > 0)
                        zone.getPosition(curPos - 1)
                    else
                        null

                    val boxBefore = puzzle.checkedGetBox(posBefore)
                    if (
                        boxBefore != null
                        && !isBlock(boxBefore)
                        && !boxBefore.isBlank()
                    ) {
                        return true
                    }

                    val posAfter = if (curPos < zone.size() - 1)
                        zone.getPosition(curPos + 1)
                    else
                        null

                    val boxAfter = puzzle.checkedGetBox(posAfter)
                    if (
                        boxAfter != null
                        && !isBlock(boxAfter)
                        && !boxAfter.isBlank()
                    ) {
                        return true
                    }
                }
            }
        }

        return false
    }

    /**
     * Ignored if clue not on board
     */
    fun jumpToClue(clueID : ClueID?, tag : Any? = null) {
        if (clueID == null)
            return

        pushNotificationDisabled()

        val start = getClueStart(clueID)
        val pos = getZone(clueID)
            ?.firstOrNull { !skipPosition(it, isSkipCompletedLetters) }
            ?: start

        if (pos != null) {
            setHighlightLetter(pos)
            puzzle.currentClueID = clueID
        }

        popNotificationDisabled()

        if (pos != null) {
            // no need to flag changed words, notifyChange will take
            // care of that
            notifyChange(tag = tag)
        }
    }

    fun jumpToClue(clue : Clue?, tag : Any? = null) {
        if (clue != null)
            jumpToClue(clue.clueID, tag)
    }

    /**
     * Try to jump to given number
     *
     * Search current list, then all lists, do nothing if not found
     */
    fun jumpToClue(number : String) {
        var clues = this.currentClueList
        var clue = clues?.getClueByNumber(number)
        if (clue != null) {
            jumpToClue(clue)
        } else {
            val puz = this.puzzle
            for (clueListName in sortedClueListNames) {
                clues = puz.getClues(clueListName)
                clue = clues?.getClueByNumber(number)
                if (clue != null) {
                    jumpToClue(clue)
                    return
                }
            }
        }
    }

    /**
     * Ignored if clue not on board
     */
    fun jumpToClueEnd(clueID : ClueID?) {
        if (clueID == null)
            return

        pushNotificationDisabled()

        val pos = getClueEnd(clueID)

        if (pos != null) {
            setHighlightLetter(pos)
            puzzle.currentClueID = clueID
        }

        popNotificationDisabled()

        if (pos != null) {
            // no need to flag changed words, notifyChange will take
            // care of that
            notifyChange()
        }
    }

    fun jumpToClueEnd(clue : Clue?) {
        if (clue != null)
            jumpToClueEnd(clue.clueID)
    }

    fun pickRandomUnfilledClue() {
        val currentID = clueID

        val unfilledClues = puzzle.allClues.filter { clue ->
            clue.clueID != currentID && !isFilledClueID(clue.clueID)
        }.toList()

        if (!unfilledClues.isEmpty()) {
            // bit inefficient, but saves a field
            val idx = Random.nextInt(unfilledClues.size)
            jumpToClue(unfilledClues[idx])
        }
    }

    /**
     * Move in direction, or not if no position found
     */
    fun moveDown() : Word {
        return moveDelta(1, 0)
    }

    /**
     * Move in direction, or not if no position found
     */
    fun moveLeft() : Word {
        return moveDelta(0, -1)
    }

    /**
     * Move in direction, or not if no position found
     */
    fun moveRight() : Word {
        return moveDelta(0, 1)
    }

    /**
     * Move in direction, or not if no position found
     */
    fun moveUp() : Word {
        return moveDelta(-1, 0)
    }

    /**
     * Finds next box in direction
     *
     * @return next box, or null
     */
    fun findNextDelta(
        original : Position,
        skipCompleted : Boolean,
        drow : Int,
        dcol : Int,
    ) : Position? {
        val next = Position(
            original.row + drow,
            original.col + dcol,
        )

        if (
            next.col < 0
            || next.row < 0
            || next.col >= puzzle.width
            || next.row >= puzzle.height
        ) {
            return null
        }

        val value = puzzle.checkedGetBox(next)

        if (isBlock(value) || skipBox(value, skipCompleted))
            return findNextDelta(next, skipCompleted, drow, dcol)
        else
            return next
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
    fun moveDelta(drow : Int, dcol : Int) : Word {
        val w = this.currentWord
        val oldPos = this.highlightLetter
        val newPos = findNextDelta(oldPos, false, drow, dcol)
        val newBox = puzzle.checkedGetBox(newPos)

        if (newPos == null || newBox == null || isBlock(newBox))
            return w

        val oldBox = puzzle.checkedGetBox(oldPos)
        if (oldBox == null)
            return w

        val newBoxClues = newBox.getIsPartOfClues()
        val curClue = w.clueID

        if (
            toggleBeforeMove
            && !isBlock(oldBox)
            && curClue != null
            && !newBoxClues.contains(curClue)
        ) {
            // moving out of clue, need to toggle if possible
            val oldBoxClues = oldBox.getIsPartOfClues()

            val nextClue = newBoxClues
                .filter { o -> oldBoxClues.contains(o) }
                .firstOrNull()

            if (nextClue != null) {
                puzzle.currentClueID = nextClue
                notifyChange()
            } else {
                setHighlightLetter(newPos)
            }
        } else {
            setHighlightLetter(newPos)
        }

        return w
    }

    /**
     * Move to the next position in the current word zone
     *
     * Returns new position, or null if run out
     */
    fun findNextZone(
        original : Position?,
        skipCompleted : Boolean,
    ) : Position? {
        return findZoneDelta(original, skipCompleted, 1)
    }

    /**
     * Move to the next position in zone
     *
     * Does not move if a new position can't be found
     */
    fun moveZoneForward(skipCompleted : Boolean) : Word {
        return moveZone(skipCompleted, 1)
    }

    /**
     * Move to the previous position in the current word zone
     *
     * Returns new position, or null if run out
     */
    fun findPrevZone(
        original : Position?,
        skipCompleted : Boolean,
    ) : Position? {
        return findZoneDelta(original, skipCompleted, -1)
    }

    /**
     * Move to the prev position in zone
     *
     * Does not move if a new position can't be found
     */
    fun moveZoneBack(skipCompleted : Boolean) : Word {
        return moveZone(skipCompleted, -1)
    }

    /**
     * Find next position in the current word zone by delta steps
     *
     * Returns new position, or null if none found
     */
    fun findZoneDelta(
        original : Position?,
        skipCompleted : Boolean,
        delta : Int,
    ) : Position? {
        val zone = currentZone
        if (zone == null)
            return null

        val index : Int

        if (
            lastFoundZoneIndex >= 0
            && lastFoundZoneIndex < zone.size()
            && original == zone.getPosition(lastFoundZoneIndex)
        ) {
            index = lastFoundZoneIndex
        } else {
            index = zone.indexOf(original)
        }

        if (index < 0)
            return null

        val newIndex = findZoneDelta(index, skipCompleted, delta)

        if (newIndex < 0) {
            lastFoundZoneIndex = -1
            return null
        } else {
            lastFoundZoneIndex = newIndex
            return zone.getPosition(newIndex)
        }
    }

    /**
     * As findZoneDelta but with index of position as first argument
     */
    fun findZoneDelta(
        original : Int,
        skipCompleted : Boolean,
        delta : Int,
    ) : Int {
        val zone = currentZone

        if (zone == null)
            return -1

        var nextIdx = original + delta
        if (nextIdx < 0 || nextIdx >= zone.size()) {
            return -1
        } else {
            val next = zone.getPosition(nextIdx)
            val box = puzzle.checkedGetBox(next)
            if (isBlock(box) || skipBox(box, skipCompleted)) {
                nextIdx = findZoneDelta(nextIdx, skipCompleted, delta)
            }
            return nextIdx
        }
    }

    /**
     * Moves by delta along the zone
     *
     * Does not move if no new position found
     */
    fun moveZone(skipCompleted : Boolean, delta : Int) : Word {
        val w = this.currentWord
        val oldPos = this.highlightLetter
        val newPos = findZoneDelta(oldPos, skipCompleted, delta)
        if (newPos != null)
            setHighlightLetter(newPos)
        return w
    }

    fun nextLetter(
        skipCompletedLetters : Boolean = this.isSkipCompletedLetters,
    ) : Word {
        return this.movementStrategy.move(this, skipCompletedLetters)
    }

    /**
     * Jump to next word, regardless of movement strategy
     *
     * Still skips completed letters if setting chosen
     */
    fun nextWord() : Word {
        val previous = this.currentWord
        val curPos = this.highlightLetter
        val newPos = getClueEnd(this.clueID)

        pushNotificationDisabled()

        if (newPos != curPos) {
            setHighlightLetter(newPos)
        }

        MovementStrategy.MOVE_NEXT_CLUE.move(this, this.isSkipCompletedLetters)

        popNotificationDisabled()

        notifyChange()

        return previous
    }

    /**
     * Insert the answer into the current word
     *
     * Ignores length mismatch and goes ahead anyway, stopping when it runs out
     * of input or space.
     */
    fun playAnswer(answer : String?) {
        if (answer == null)
            return

        pushNotificationDisabled()
        jumpToClue(this.clueID)
        // use STOP_ON_END movement for entering answers
        val oldMovementStrategy = this.movementStrategy
        this.movementStrategy = MovementStrategy.STOP_ON_END
        playLetters(answer)
        this.movementStrategy = oldMovementStrategy
        popNotificationDisabled()
        notifyChange()
    }

    /**
     * Insert the string as a sequence of letters into cells
     *
     * Starts from current pos and enters box-by-box until letters or
     * current word runs out.
     *
     * See playResponse.
     */
    fun playLetters(letters : String) : Word {
        return playBoxLetters(
            letters,
            { box, letter ->
                if (
                    !preserveCorrectLettersInShowErrors
                    || (box.response != box.solution)
                    || !this.isShowErrors
                ) {
                    box.response = letter
                    box.responder = this.responder
                    return@playBoxLetters true
                }
                false
            }
        )
    }

    /**
     * Clear the current word
     */
    fun clearWord() {
        val startPos = this.highlightLetter
        val zone = currentZone

        if (zone == null || zone.isEmpty)
            return

        pushNotificationDisabled()
        for (pos in zone) {
            val current = this.highlightLetter
            if (current != pos)
                setHighlightLetter(pos)
            deleteOrUndoLetter()
        }

        val currentPos = this.highlightLetter
        if (currentPos != startPos)
            setHighlightLetter(startPos)

        popNotificationDisabled()
        notifyChange()
    }

    /**
     * Plays the response and returns the previous word
     *
     * If response is null, nothing happens and the current word is
     * returned. Response is added according to splitWords and
     * scratchMode.
     *
     * This method should be preferred over playLetter(s) and
     * playScratchLetter(s) unless a specific action is required.
     */
    fun playResponse(response : String?) : Word? {
        if (scratchMode)
            return playScratchResponse(response)
        else
            return playCellResponse(response)
    }

    /**
     * See playResponse, play to cell not scratch
     */
    fun playCellResponse(response : String?) : Word? {
        if (response == null)
            return this.currentWord

        val split = (
            splitResponses
            && (response.codePointCount(0, response.length) > 1)
        )

        if (split)
            return playLetters(response)
        else
            return playLetter(response)
    }

    /**
     * See playResponse
     */
    fun playScratchResponse(response : String?) : Word {
        if (response == null)
            return this.currentWord

        val split = (
            splitResponses
            && (response.codePointCount(0, response.length) > 1)
        )

        if (split)
            return playScratchLetters(response)
        else
            return playScratchLetter(response)
    }

    /**
     * See playResponse with String
     */
    fun playResponse(letter : Char) : Word? {
        return playResponse(letter.toString())
    }

    /**
     * See playResponse
     */
    fun playLetter(letter : Char) : Word? {
        return playLetter(letter.toString())
    }

    /**
     * See playResponse
     */
    fun playLetter(letter : String) : Word? {
        val pos = this.highlightLetter
        val b = puzzle.checkedGetBox(pos)

        if (b == null || isBlock(b)) {
            return null
        }

        if (
            preserveCorrectLettersInShowErrors
            && b.response == b.solution
            && this.isShowErrors
        ) {
            // Prohibit replacing correct letters
            return this.currentWord
        } else {
            pushNotificationDisabled()

            pushPlayLetterUndoStackCurrent()

            b.response = letter
            b.responder = this.responder
            flagChange(pos)

            val wordClueID = this.clueID
            val wordZone = currentZone

            // movement normally clears undo stack but we want to keep
            // it here
            playLetterUndoStack.pushClearDisabled()
            val next = this.nextLetter()
            playLetterUndoStack.popClearDisabled()

            flagFilledWordErrorCells(wordClueID, wordZone)

            popNotificationDisabled()
            notifyChange()

            return next
        }
    }

    /**
     * Insert the string as a sequence of letters into cells scratch
     *
     * Starts from current pos and enters box-by-box until letters or
     * current word runs out.
     *
     * See playResponse.
     */
    fun playScratchLetters(letters : String) : Word {
        return playBoxLetters(
            letters,
            { box, letter ->
                if (letter.length > 0) {
                    return@playBoxLetters setScratchLetter(
                        box,
                        letter.get(0)
                    )
                } else {
                    return@playBoxLetters false
                }
            }
        )
    }

    /**
     * See playResponse
     */
    fun playScratchLetter(letter : String) : Word {
        if (!letter.isEmpty())
            return playScratchLetter(letter.get(0))
        else
            return this.currentWord
    }

    /**
     * See playResponse
     */
    fun playScratchLetter(letter : Char) : Word {
        val prevWord = this.currentWord
        pushNotificationDisabled()
        val highlightPos = this.highlightLetter
        puzzle.checkedGetBox(highlightPos)?.let { box ->
            setScratchLetter(box, letter)
        }
        nextLetter()
        popNotificationDisabled()
        notifyChange()
        return prevWord
    }

    /**
     * Copy non-blank scratch to non-blank responses in current cell
     *
     * Does nothing if box has multiple conflicting non-blank scratch
     * entries
     */
    fun promoteScratchCell(
        position : Position = highlightLetter,
    ) {
        val box = puzzle.checkedGetBox(position)
        if (box == null || !box.isBlank())
            return

        var response = Box.BLANK

        for (cid in box.getIsPartOfClues()) {
            val cluePosition = box.getCluePosition(cid)
            val scratch = puzzle.getNote(cid)?.scratch
            if (
                scratch != null
                && 0 <= cluePosition
                && cluePosition < scratch.length
            ) {
                val newResponse = scratch[cluePosition].toString()
                if (response == Box.BLANK) {
                    response = newResponse
                } else if (
                    newResponse != Box.BLANK
                    && response != newResponse
                ) {
                    // conflict, so do nothing
                    return
                }
            }
        }

        if (response != Box.BLANK) {
            box.response = response
            flagChange(position)
            notifyChange()
        }
    }

    /**
     * Copy non-blank scratch to non-blank responses in current word
     */
    fun promoteScratchCurrentWord() {
        for (pos in (currentZone ?: emptyList()))
            promoteScratchCell(pos)
    }

    /**
     * Copy non-blank scratch to non-blank responses in current word
     */
    fun promoteScratchGrid() {
        puzzle.forEachPosition { promoteScratchCell(it) }
    }

    fun previousLetter() : Word {
        return movementStrategy.back(this)
    }

    /**
     * Moves to start of previous word regardless of movement strategy
     *
     * Start takes skipCompletedLetters into account and highlights
     * first empty cell in previous word. If no empty cell, then just
     * start of word.
     */
    fun previousWord() : Word {
        val previousCID = this.clueID
        val previous = this.currentWord

        val curPos = this.highlightLetter
        val newPos = getClueStart(this.clueID)

        pushNotificationDisabled()

        if (curPos != newPos)
            this.setHighlightLetter(newPos)

        var goBack = true
        while (goBack) {
            // only go back again if discovered needed later
            goBack = false

            // Idea: use back for move_next_clue, which takes us to
            // previous clue. Move to the start of that clue. Then
            // search for blank cell to move to. If none found, try
            // going around the loop again, unless we've already reached
            // the originally selected clue
            MovementStrategy.MOVE_NEXT_CLUE.back(this)

            val newCID = this.clueID
            val start = getClueStart(newCID)
            if (start != this.highlightLetter) setHighlightLetter(start)

            val box = puzzle.checkedGetBox(start)
            if (box != null && skipBox(box, this.isSkipCompletedLetters)) {
                moveZoneForward(this.isSkipCompletedLetters)
                goBack = (
                    start == this.highlightLetter
                    && previousCID != newCID
                )
            }
        }

        popNotificationDisabled()

        notifyChange()

        return previous
    }

    fun revealInitialLetter() : Position? {
        val highlightLetter = this.highlightLetter
        val b = puzzle.checkedGetBox(highlightLetter)

        if (b != null && !isBlock(b) && b.hasInitialValue()) {
            b.initialValue?.let { initialValue ->
                if (b.response != initialValue) {
                    b.response = initialValue
                    flagChange(highlightLetter)
                    notifyChange()
                    return highlightLetter
                }
            }
        }

        return null
    }

    fun revealInitialLetters() {
        var changed = false

        for (row in 0..<puzzle.height) {
            for (col in 0..<puzzle.width) {
                val b = puzzle.checkedGetBox(row, col)
                val reveal = (
                    b != null
                    && !isBlock(b)
                    && b.hasInitialValue()
                    && (b.response != b.initialValue)
                )

                if (reveal) {
                    b.initialValue?.let { initialValue ->
                        b.response = initialValue
                        flagChange(Position(row, col))
                        changed = true
                    }
                }
            }
        }

        if (changed)
            notifyChange()
    }

    fun revealLetter() : Position? {
        val highlightLetter = this.highlightLetter
        val b = puzzle.checkedGetBox(highlightLetter)

        if (b != null && !isBlock(b)) {
            val correctResponse = b.solution == b.response
            if (!correctResponse) {
                b.solution?.let { solution ->
                    b.isCheated = true
                    b.response = solution
                    flagChange(highlightLetter)
                    notifyChange()
                }
                return highlightLetter
            }
        }

        return null
    }

    /**
     * Reveals the correct answers for any "red" squares on the board.
     *
     * This covers hidden and visible incorrect responses, as well as
     * squares that are marked as "cheated" from previously erased
     * incorrect responses.
     */
    fun revealErrors() : List<Position> {
        val changes = mutableListOf<Position>()
        val boxes = this.boxes

        for (row in 0..<puzzle.height) {
            for (col in 0..<puzzle.width) {
                puzzle.checkedGetBox(row, col)?.let { b ->
                    if (!isBlock(b)) {
                        val correctResponse = b.solution == b.response
                        if (b.isCheated || (!b.isBlank() && !correctResponse)) {
                            b.solution?.let { solution ->
                                b.isCheated = true
                                b.response = solution
                                val pos = Position(row, col)
                                flagChange(pos)
                                changes.add(pos)
                            }
                        }
                    }
                }
            }
        }

        notifyChange()

        return changes
    }

    fun revealPuzzle() : List<Position> {
        val changes = mutableListOf<Position>()
        val boxes = this.boxes

        for (row in 0..<puzzle.height) {
            for (col in 0..<puzzle.width) {
                puzzle.checkedGetBox(row, col)?.let { b ->
                    if (!isBlock(b)) {
                        val correctResponse = b.solution == b.response
                        if (!correctResponse) {
                            b.isCheated = true
                            b.solution?.let { solution ->
                                b.response = solution
                                val pos = Position(row, col)
                                flagChange(pos)
                                changes.add(pos)
                            }
                        }
                    }
                }
            }
        }

        notifyChange()

        return changes
    }

    fun revealWord() : List<Position> {
        val changes = mutableListOf<Position>()
        val zone = this.currentZone
        if (zone == null || zone.isEmpty)
            return changes

        val curPos = this.highlightLetter
        val startPos = zone.getPosition(0)

        pushNotificationDisabled()

        if (curPos != startPos)
            setHighlightLetter(startPos)

        for (i in 0..<zone.size()) {
            val p = revealLetter()
            if (p != null) {
                flagChange(p)
                changes.add(p)
            }
            nextLetter(false)
        }

        popNotificationDisabled()

        setHighlightLetter(curPos)

        return changes
    }

    fun skipPosition(p : Position?, skipCompleted : Boolean) : Boolean {
        val box = puzzle.checkedGetBox(p)
        return if (isBlock(box))
            false
        else
            skipBox(box, skipCompleted)
    }

    fun skipBox(b : Box?, skipCompleted : Boolean) : Boolean {
        return if (b == null)
            true
        else (
            skipCompleted
            && !b.isBlank()
            && (
                !this.isShowErrors
                || !b.hasSolution()
                || b.response == b.solution
            )
        )
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
    fun toggleSelection() : Word {
        playLetterUndoStack.clearIfEnabled()

        val w = this.currentWord

        val box = puzzle.checkedGetBox(this.highlightLetter)
        if (box == null || isBlock(box))
            return w

        var changed = false

        val boxClues : SortedSet<ClueID> = box.getIsPartOfClues()

        if (boxClues.isEmpty()) {
            // if is a detached cell, toggle between selecting all
            // detached cells and just this single cell. Only toggle if
            // we were previously in a detached cell, else keep the
            // current detached "direction".
            changed = true
            if (this.clueID == null)
                selectDetachedZone = !selectDetachedZone
            puzzle.currentClueID = null
        } else {
            val curCid = this.clueID

            // if in current clue, toggle, else try to stay in same list
            if (curCid != null && boxClues.contains(curCid)) {
                var newCid = boxClues.higher(curCid)
                if (newCid == null)
                    newCid = boxClues.first()
                puzzle.currentClueID = newCid
                changed = curCid != newCid
            } else {
                var newCid : ClueID? = null
                if (curCid != null)
                    newCid = box.getIsPartOfClue(curCid.listName)
                if (newCid == null)
                    newCid = boxClues.first()
                puzzle.currentClueID = newCid
                changed = true
            }
        }

        if (changed) {
            // don't flag changed selected words (we might have multiple
            // toggles). notifyChange will take care of the final result
            notifyChange()
        }

        return w
    }

    fun addListener(listener : PlayboardListener) {
        if (isNotifying)
            addPendingListenerAddition(listener)
        else listeners.add(listener)
    }

    fun removeListener(listener : PlayboardListener) {
        if (this.isNotifying)
            addPendingListenerRemoval(listener)
        else listeners.remove(listener)
    }

    fun editClue(cid : ClueID?, newHint : String) {
        val clue = puzzle.getClue(cid)
        if (clue != null)
            editClue(clue, newHint)
    }

    fun editClue(clue : Clue, newHint : String) {
        clue.hint = newHint
        flagClueChange(clue)
        notifyChange()
    }

    /**
     * Call this instead of Puzzle.setNote to update listeners
     */
    fun setClueNote(cid : ClueID?, note : Note?) {
        if (cid != null && note != puzzle.getNote(cid)) {
            puzzle.setNote(cid, note)
            flagClueChange(cid)
            notifyChange()
        }
    }

    /**
     * Call this instead of Puzzle.setPlayerNote
     *
     * For consistency with setClueNote really.
     */
    fun setPlayerNote(note : Note?) {
        puzzle.playerNote = note
    }

    fun flagClue(cid : ClueID?, flag : Boolean) {
        val clue = puzzle.getClue(cid)
        if (clue != null)
            flagClue(clue, flag)
    }

    /**
     * Call this instead of puzzle.flagClue to update listeners
     */
    fun flagClue(clue : Clue?, flag : Boolean) {
        if (clue != null) {
            clue.isFlagged = flag
            flagClueChange(clue)
            notifyChange()
        }
    }

    fun setFlagColor(cid : ClueID?, color : Int) {
        val clue = puzzle.getClue(cid)
        if (clue != null)
            setFlagColor(clue, color)
    }

    /**
     * Call this instead of setting color directly to update listeners
     */
    fun setFlagColor(clue : Clue?, color : Int) {
        if (clue != null) {
            clue.flagColor = color
            flagClueChange(clue)
            notifyChange()
        }
    }

    /**
     * Call this instead of setting box flag directly to update listeners
     */
    fun flagPosition(pos : Position, flag : Boolean) {
        val box = puzzle.checkedGetBox(pos)
        if (box != null) {
            box.isFlagged = flag
            flagChange(pos)
            notifyChange()
        }
    }

    /**
     * Call instead of setting box color directly to update listeners
     */
    fun setFlagColor(pos : Position, color : Int) {
        val box = puzzle.checkedGetBox(pos)
        if (box != null) {
            box.flagColor = color
            flagChange(pos)
            notifyChange()
        }
    }

    /**
     * Framework for entering sequence of letters into cells
     *
     * Starts from current pos and enters box-by-box until letters or
     * current word runs out. Calls back so entering action can be
     * customised (e.g. main response or scratch). Returns true if
     * changed. Spaces will be ignored.
     *
     * @return previous word
     */
    fun playBoxLetters(
        letters : String,
        callback : (Box, String) -> Boolean,
    ) : Word {
        val prevWord = this.currentWord

        val zone = this.currentZone
        if (zone == null)
            return prevWord

        pushNotificationDisabled()

        val curID = this.clueID
        val startPos = this.highlightLetter

        var foundStart = false

        // bit of a hacky version of isWhitespace..
        val nonSpaceCodePoints = letters.codePointSequence()
            .filter { it.toString().trim().isNotEmpty() }
            .iterator()
        val positions = zone.iterator()
        var pos : Position? = null

        while (positions.hasNext() && nonSpaceCodePoints.hasNext()) {
            pos = positions.next()

            foundStart = foundStart or (pos == startPos)
            if (!foundStart)
                continue

            val b = puzzle.checkedGetBox(pos)
            if (b == null || isBlock(b))
                break

            // get next letter and try to handle unicode
            val letter = nonSpaceCodePoints.next().toString()
            val changed = callback(b, letter)
            if (changed)
                flagChange(pos)
        }

        if (pos != null) {
            setHighlightLetter(pos, curID)
            // don't clear undo stack as not user requested
            playLetterUndoStack.pushClearDisabled()
            val next = this.nextLetter()
            playLetterUndoStack.popClearDisabled()
        }

        flagFilledWordErrorCells(curID, zone)

        popNotificationDisabled()

        notifyChange()

        return prevWord
    }

    /**
     * Play letter into scratch of box for current clue
     *
     * Does not change current position. Returns true if changed. Blocks
     * change notifications, so caller must notify if change.
     */
    private fun setScratchLetter(box : Box, letter : Char) : Boolean {
        if (isBlock(box))
            return false


        return this.clueID?.let { cid ->
            pushNotificationDisabled()

            var note = this.note
            val length = this.currentWordLength

            // Create a note for this clue if we don't already have one
            if (note == null) {
                note = Note(length)
                puzzle.setNote(cid, note)
            }

            var changed = false

            // Update the scratch text
            val pos = box.getCluePosition(cid)
            if (pos >= 0 && pos < length) {
                note = note.withScratchLetter(pos, letter)
                puzzle.setNote(cid, note)
                changed = true
            }

            popNotificationDisabled()

            changed
        } ?: false
    }

    private fun flagNotifying(isNotifying : Boolean) {
        // if stopping notifying, clear pending removals
        val stoppingNotifying = !isNotifying && this.isNotifying
        this.isNotifying = isNotifying

        if (stoppingNotifying) {
            for (listener in pendingListenerRemovals) {
                listener?.let { removeListener(it) }
            }
            pendingListenerRemovals.clear()
            for (listener in pendingListenerAdditions) {
                listener?.let { addListener(it) }
            }
            pendingListenerAdditions.clear()
        }

        this.isNotifying = isNotifying
    }

    private fun addPendingListenerRemoval(listener : PlayboardListener?) {
        pendingListenerRemovals.add(listener)
    }

    private fun addPendingListenerAddition(listener : PlayboardListener?) {
        pendingListenerAdditions.add(listener)
    }

    private fun notifyChange(wholeBoard : Boolean = false, tag : Any? = null) {
        if (notificationDisabledDepth == 0) {
            flagNotifying(true)

            val lastHistoryIndex = updateHistory()
            // if the current clue was already top of the list, then no
            // change
            val historyChange = lastHistoryIndex != 0

            val currentWord = this.currentWord

            if (currentWord != previousWord) {
                // "unusual" is to stop me calling it elsewhere
                flagUnusualWordChange(previousWord)
                flagUnusualWordChange(currentWord)
            }

            val posChanges = if (wholeBoard)
                null
            else
                this.positionChanges.toSet()

            val clueEdits = getChangedClueIDs().toSet()

            val tags = if (tag == null)
                setOf<Any>()
            else
                setOf<Any>(tag)

            val notificationChanges = PlayboardChanges(
                currentWord,
                previousWord,
                previousPosition,
                posChanges,
                historyChange,
                lastHistoryIndex,
                clueEdits,
                tags,
            )

            for (listener in listeners) {
                listener?.let { it.onPlayboardChange(notificationChanges) }
            }

            previousWord = currentWord
            previousPosition = this.highlightLetter

            clearChanges()

            flagNotifying(false)
        }
    }

    private fun pushNotificationDisabled() {
        notificationDisabledDepth += 1
    }

    private fun popNotificationDisabled() {
        if (notificationDisabledDepth > 0)
            notificationDisabledDepth -= 1
    }

    /**
     * Returns lastHistoryIndex
     *
     * Note, will be -1 if the current clue wasn't in the history before and if
     * there is no current clue. Detect latter case with getClueID() == null.
     */
    private fun updateHistory(cid : ClueID? = this.clueID) : Int {
        if (cid == null)
            return -1
        return puzzle.updateHistory(cid)
    }

    /**
     * Returns the zone of the clue, or null
     */
    private fun getZone(clueID : ClueID?) : Zone? {
        val clue = puzzle.getClue(clueID)
        return clue?.zone
    }

    /**
     * Get start of clue position
     *
     * @return null if no such clue or not on board
     */
    private fun getClueStart(clueID : ClueID?) : Position? {
        val clue = puzzle.getClue(clueID)
        val zone = clue?.zone

        return if (zone == null || zone.isEmpty)
            null
        else
            zone.getPosition(0)
    }

    /**
     * Get end of clue position
     *
     * @return null if no such clue or not on board
     */
    private fun getClueEnd(clueID : ClueID?) : Position? {
        val clue = puzzle.getClue(clueID)
        val zone = clue?.zone

        return if (zone == null || zone.isEmpty)
            null
        else
            zone.getPosition(zone.size() - 1)
    }

    private val currentWordLength : Int
        get() {
            return currentZone?.size() ?: 0
        }

    private fun clearChanges() {
        changedPositions.clear()
        changedClueIDs.clear()
    }

    private val positionChanges : Collection<Position>
        get() = changedPositions

    private fun getChangedClueIDs() : Collection<ClueID> {
        return changedClueIDs
    }

    private fun flagChange(vararg positions : Position) {
        for (pos in positions) {
            changedPositions.add(pos)
        }
    }

    /**
     * In filled-word error modes, flag cells that would be highlighted
     */
    private fun flagFilledWordErrorCells(clueID : ClueID?, zone : Zone?) {
        if (zone == null) return
        when (showErrorsMode) {
            ShowErrorsMode.FILLED_WORD_WORD -> {
                if (isFilledClueID(clueID) && isHasErrorsClue(clueID)) {
                    for (pos in zone) {
                        flagChange(pos)
                    }
                }
            }
            ShowErrorsMode.FILLED_WORD_CELLS -> {
                if (isFilledClueID(clueID)) {
                    for (pos in zone) {
                        val box = puzzle.checkedGetBox(pos) ?: continue
                        if (
                            !box.isBlank()
                            && box.hasSolution()
                            && box.solution != box.response
                        ) {
                            flagChange(pos)
                        }
                    }
                }
            }
            else -> {}
        }
    }

    private fun flagClueChange(clue : Clue) {
        flagClueChange(clue.clueID)
    }

    private fun flagClueChange(cid : ClueID) {
        changedClueIDs.add(cid)
    }

    /**
     * Don't use these for changes to selected word
     *
     * notifyChange will take care of that
     */
    private fun flagUnusualWordChange(word : Word?) {
        val zone = word?.zone
        flagUnusualZoneChange(zone)
    }

    /**
     * Don't use these for changes to selected word
     *
     * notifyChange will take care of that
     */
    private fun flagUnusualZoneChange(zone : Zone?) {
        if (zone == null)
            return
        for (pos in zone)
            flagChange(pos)
    }

    val currentZone : Zone?
        /**
         * Get currently selected zone
         *
         * Normally the highlight letter is in the currently selected clue.
         *
         * If there is no currently selected clue, return the zone of
         * detached cells if the highlighted cell is in them. Else, just
         * return a zone with the current position.
         */
        get() {
            val clue = this.puzzle.getClue(this.clueID)
            if (clue == null || !clue.hasZone()) {
                val highlight = this.highlightLetter
                val puz = this.puzzle
                val box = puz.checkedGetBox(highlight)
                if (box != null && !isBlock(box) && !box.isPartOfClues()) {
                    return getDetachedCellsZone()
                } else {
                    val zone = Zone()
                    zone.addPosition(highlight)
                    return zone
                }
            } else {
                return clue.zone
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
    private fun getDetachedCellsZone() : Zone {
        return getDetachedCellsZone(this.puzzle)
    }

    /**
     * Version without this escape for constructor
     */
    private fun getDetachedCellsZone(puz : Puzzle) : Zone {
        val curDetachedCellsZone = detachedCellsZone
        if (curDetachedCellsZone != null)
            return curDetachedCellsZone

        val newDetachedCellsZone = Zone()
        // because Zone.hasPosition is O(n)
        val oldPositions = mutableSetOf<Position>()

        for (row in 0..<puz.height) {
            for (col in 0..<puz.width) {
                val box = puz.checkedGetBox(row, col)
                if (box != null && !isBlock(box) && !box.isPartOfClues()) {
                    val pos = Position(row, col)
                    if (oldPositions.contains(pos))
                        continue

                    val across = PuzzleUtils.getAcrossZone(puz, pos)
                    if (across.size() > 1) {
                        addZoneToDetached(
                            across,
                            newDetachedCellsZone,
                            oldPositions,
                            puz,
                        )
                    } else {
                        val down = PuzzleUtils.getDownZone(puz, pos)
                        addZoneToDetached(
                            down,
                            newDetachedCellsZone,
                            oldPositions,
                            puz,
                        )
                    }
                }
            }
        }

        detachedCellsZone = newDetachedCellsZone

        return newDetachedCellsZone
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
    private fun addZoneToDetached(
        zone : Zone,
        detachedZone : Zone,
        oldPositions : MutableSet<Position>,
        puz : Puzzle,
    ) {
        for (zonePos in zone) {
            if (!oldPositions.contains(zonePos)) {
                val zoneBox = puz.checkedGetBox(zonePos)

                // if isn't detached, abort
                if (
                    zoneBox == null
                    || isBlock(zoneBox)
                    || zoneBox.isPartOfClues()
                )
                    return

                if (!oldPositions.contains(zonePos)) {
                    detachedZone.addPosition(zonePos)
                    oldPositions.add(zonePos)
                }
            }
        }
    }

    /**
     * Add current board position to play letter undo stack
     */
    private fun pushPlayLetterUndoStackCurrent() {
        val pos = this.highlightLetter
        puzzle.checkedGetBox(pos)?.let { box ->
            playLetterUndoStack.push(pos, this.clueID, box.response)
        }
    }

    private fun pauseDelete() {
        pauseDeleteUntil = TimeUtils.currentTimeMillis() + PAUSE_DELETE_DELAY
    }

    private val isPausedDelete : Boolean
        /**
         * If delete is paused by pauseDelete
         */
        get() = (
            0 <= pauseDeleteUntil
            && TimeUtils.currentTimeMillis() < pauseDeleteUntil
        )

    /**
     * A word on the grid
     *
     * A Zone and possibly the clue it goes with (null if no clue).
     */
    data class Word(
        val zone : Zone,
        val clueID : ClueID? = null,
    ) {
        fun checkInWord(pos : Position?) : Boolean {
            return zone.hasPosition(pos)
        }

        fun checkInWord(row : Int, col : Int) : Boolean {
            return zone.hasPosition(Position(row, col))
        }

        fun indexOf(pos : Position?) : Int {
            return zone.indexOf(pos)
        }

        val length : Int
            get() = zone.size()

        override fun toString() : String {
            return clueID.toString() + " | " + zone
        }
    }

    /**
     * Playboard listeners will be updated when the highlighted letter
     * changes or the contents of a box changes.
     */
    fun interface PlayboardListener {
        fun onPlayboardChange(changes : PlayboardChanges)
    }

    class PlayboardChanges(
        /**
         * The currently selected word
         */
        val currentWord : Word,
        /**
         * The word selected at last notification (may be null)
         */
        val previousWord : Word?,
        /**
         * The position selected at last notification (may be null)
         */
        val previousPosition : Position?,
        /**
         * A set of changed cell positions since update
         *
         * Null means whole board changed. Fast lookup.
         */
        val cellChanges : Collection<Position>?,
        /**
         * True if something moved to the top of the history list
         *
         * See lastHistoryIndex.
         */
        val isHistoryChange : Boolean,
        /**
         * Where the current word used to sit in the history list
         *
         * Before it became the first :) Can be -1 if wasn't there
         * before or no current clue selected (so list unchanged).
         */
        val lastHistoryIndex : Int,
        /**
         * List of clues that have had num/hint edited or flagged
         */
        val changedClueIDs : Collection<ClueID>,
        /**
         * Tags added by external callers of Playboard methods
         *
         * To identify which updates were triggered by types of external
         * calls. Useful in some cases. E.g. so a clue list widget won't
         * scroll away from a clue that has just been clicked.
         */
        val tags : Collection<Any>,
    ) {
        override fun toString() : String {
            return ("currentWord = " + currentWord
                    + ", previousWord = " + previousWord
                    + ", previousPosition = " + previousPosition
                    + ", cellChanges = " + cellChanges
                    + ", historyChange = " + this.isHistoryChange
                    + ", lastHistoryIndex = " + lastHistoryIndex
                    + ", changedClueIDs = " + changedClueIDs
                    + ", tags = " + tags)
        }
    }

    /**
     * Position of a play letter in undo stack
     */
    private data class PlayLetterInfo(
        val position : Position,
        val clueID : ClueID?,
        val oldResponse : String,
    )

    /**
     * For managing an undo stack of played letters
     */
    private class PlayLetterUndoStack(private var enabled : Boolean) {
        private var clearDisabledDepth = 0
        private val undoStack = ArrayDeque<PlayLetterInfo>()

        /**
         * Stack will be cleared and not accept push if disabled
         */
        fun setEnabled(enabled : Boolean) {
            if (!enabled)
                undoStack.clear()
            this.enabled = enabled
        }

        fun isEnabled() : Boolean {
            return enabled
        }

        /**
         * Clear if clearing is enabled (not if whole stack is enabled)
         */
        fun clearIfEnabled() {
            if (!this.isClearDisabled)
                undoStack.clear()
        }

        fun pushClearDisabled() {
            clearDisabledDepth += 1
        }

        fun popClearDisabled() {
            if (this.isClearDisabled)
                clearDisabledDepth -= 1
        }

        val isClearDisabled : Boolean
            get() = clearDisabledDepth > 0

        fun push(
            position : Position,
            clueID : ClueID?,
            oldResponse : String,
        ) {
            if (!enabled)
                return
            undoStack.addFirst(PlayLetterInfo(position, clueID, oldResponse))
        }

        fun pop() : PlayLetterInfo {
            return undoStack.removeFirst()
        }

        val isEmpty : Boolean
            get() = undoStack.isEmpty()
    }

    companion object {
        private const val PAUSE_DELETE_DELAY : Long = 150 // ms
    }
}

