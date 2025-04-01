
package app.crossword.yourealwaysbe.forkyz

import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.exttools.CROSSWORD_SOLVER_BLANK
import app.crossword.yourealwaysbe.forkyz.exttools.CrosswordSolverData
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalDictionaries
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.getStopOnEndStrategy
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.forkyz.view.BoardEditText
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Note
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges

private val BLANK = Box.BLANK
private val BLANK_CHAR = Box.BLANK[0]

private fun isBlank(c : Char) : Boolean {
    return c.toString() == BLANK
}

private fun stripBlank(s : String) : String {
    return s.trim({ it == BLANK_CHAR })
}

private fun padBlank(s : String, len : Int) : String {
    return s.padEnd(len, BLANK_CHAR)
}

/**
 * Returns string with char at pos set to c
 *
 * Extends and pads with space if s is too short
 */
private fun setChar(s : String, pos : Int, c : Char) : String {
    if (pos < 0)
        return s
    else if (s.length <= pos)
        return s.padEnd(pos - 1, BLANK_CHAR) + c
    else
        return s.substring(0, pos) + c + s.substring(pos + 1)
}

private fun blanksInCrosswordSolverFormat(s : String) : String {
    return s.replace(BLANK_CHAR, CROSSWORD_SOLVER_BLANK)
}

/**
 * Turn string into a char array with any padding chars set blank
 *
 * @param s source string
 * @param len length of new array
 */
private fun charArrayWithBlanks(s : String, len : Int) : CharArray {
    return s.padEnd(len, BLANK_CHAR).toCharArray()
}

enum class NoteEntry {
    BOARD, SCRATCH, TEXT, ANAGRAM_SOURCE, ANAGRAM_SOLUTION
}

data class TransferRequest(
    val source : NoteEntry,
    val target : NoteEntry,
)

/**
 * UI State for notes
 *
 * @param notesClueID the ID of the clue if clue notes else null (puzzle
 * notes)
 * @param clueText the clue text to display with the clue, or null if
 * this is the zone of cells with no clue attached
 */
data class NotesActivityUIState(
    val notesClueID : ClueID? = null,
    val notesClue : Clue? = null,
    val clueText : String? = null,
    val boxesLength : Int = 0,
    val suppressNotesList : Set<String>? = null,
    val scratchValue : String = "",
    val textValue : String = "",
    val anagramSourceValue : String = "",
    val anagramSourcePredictionValue : String? = null,
    val anagramSolutionValue : String = "",
    val anagramPrediction : String? = null,
    val clueFlagged : Boolean = false,
    val clueFlagColor : Int = 0,
    val confirmTransferRequest : TransferRequest? = null,
    val inferSeparators : Boolean = false,
) {
    val isPuzzleNotes : Boolean
        get() = notesClueID == null

    val numRemainingAnagramCharacters : Int by lazy {
        (
            boxesLength
            - (anagramSourceValue.count { !isBlank(it) })
            - (anagramSolutionValue.count { !isBlank(it) })
        )
    }
}

@HiltViewModel
class NotesActivityViewModel @Inject constructor(
    application : Application,
    settings : ForkyzSettings,
    currentPuzzleHolder : CurrentPuzzleHolder,
    fileHandlerProvider : FileHandlerProvider,
    utils : AndroidVersionUtils,
) : PuzzleActivityViewModel(
    application,
    settings,
    currentPuzzleHolder,
    fileHandlerProvider,
    utils,
) {
    private val _notesUIState
        = MediatorLiveData<NotesActivityUIState>(NotesActivityUIState())
    public val notesUIState : LiveData<NotesActivityUIState> = _notesUIState

    private val _acceptedPredictionEvent = MutableStateFlow<Int?>(null)
    /**
     * The length of a prediction if just accepted or null
     */
    public val acceptedPredictionEvent : StateFlow<Int?>
        = _acceptedPredictionEvent

    init {
        setupVoiceCommands()

        _notesUIState.addSource(settings.livePlayScratchDisplay) {
            _notesUIState.value = notesUIState.value?.copy(
                suppressNotesList = getSuppressNotesList(notesClueID)
            )
        }

        _notesUIState.addSource(settings.livePlayInferSeparators) {
            _notesUIState.value = notesUIState.value?.copy(
                inferSeparators
                    = settings.livePlayInferSeparators.value ?: false,
            )
        }
    }

    /**
     * Clue id for notes instance
     *
     * Null means puzzle notes. Sets notes ui state.
     */
    var notesClueID : ClueID?
        get() { return notesUIState.value?.notesClueID }
        set(value) { updateState(value) }

    val notesClue : Clue?
        get() { return notesUIState.value?.notesClue }

    val isPuzzleNotes : Boolean
        get() { return notesUIState.value?.isPuzzleNotes ?: false }

    val boxesLength : Int
        get() { return notesUIState.value?.boxesLength ?: 0 }

    val numRemainingAnagramCharacters : Int
        get() {
            return notesUIState.value?.numRemainingAnagramCharacters ?: 0
        }

    /**
     * The value of the current word, but as a string not a string array
     *
     * For use when copying from board to notes, where notes are
     * represented as strings with one char per box
     */
    val boardNotesValue : String
        get() {
            return getBoard()?.let { board ->
                return String(
                    board.currentWordBoxes.map { box ->
                        if (box.isBlank())
                            BLANK_CHAR
                        else
                            box.response[0]
                    }.toCharArray()
                )
            } ?: ""
        }

    val scratchValue : String
        get() {
            return notesUIState.value?.scratchValue ?: ""
        }

    val textValue : String
        get() {
            return notesUIState.value?.textValue ?: ""
        }

    val anagramSourceValue : String
        get() {
            return notesUIState.value?.anagramSourceValue ?: ""
        }

    val anagramSourcePredictionValue : String?
        get() {
            return notesUIState.value?.anagramSourcePredictionValue
        }

    val anagramSolutionValue : String
        get() {
            return notesUIState.value?.anagramSolutionValue ?: ""
        }

    val note : Note
        get() {
            return getPuzzle()?.let { puz ->
                if (isPuzzleNotes) {
                    return puz.playerNote ?: Note(boxesLength)
                } else {
                    return puz.getNote(notesClueID) ?: Note(boxesLength)
                }
            } ?: Note(boxesLength)
        }

    fun playLetterToBoard(c : Char) {
        getBoard()?.let { board ->
            val movementStrategy = board.getMovementStrategy()
            getStopOnEndStrategy(settings) { stopOnEnd ->
                board.setMovementStrategy(stopOnEnd)
                board.playLetter(c)
                board.setMovementStrategy(movementStrategy)
            }
        }
    }

    fun deleteLetterFromBoard() {
        getBoard()?.let { board ->
            val movementStrategy = board.getMovementStrategy()
            getStopOnEndStrategy(settings) { stopOnEnd ->
                board.setMovementStrategy(stopOnEnd)
                board.deleteOrUndoLetter()
                board.setMovementStrategy(movementStrategy)
            }
        }
    }

    fun setScratchValue(value : String) {
        updateNote() { it.withScratch(value) }
    }

    fun setTextValue(value : String) {
        updateNote() { it.withText(value) }
    }

    /**
     * Check if update to anagram source is ok
     *
     * Returns \0 if not and change shouldn't be made. Does not update.
     */
    fun filterAnagramSource(oldChar : Char, newChar : Char) : Char {
        if (
            isBlank(newChar)
            || !isBlank(oldChar)
            || numRemainingAnagramCharacters > 0
        ) {
            return newChar
        } else {
            return 0.toChar()
        }
    }

    fun updateAnagramSource(
        pos : Int,
        oldChar : Char,
        isDelete : Boolean,
        newValue : String,
    ) {
        settings.getPlayPredictAnagramChars() { predict ->
            if (
                predict
                && !isDelete
                && isBlank(oldChar)
                && isBlank(newValue[pos])
                && anagramSourcePredictionValue != null
            ) {
                updateNote() {
                    it.withAnagramSource(anagramSourcePredictionValue)
                }
                _acceptedPredictionEvent.value =
                    anagramSourcePredictionValue?.length
            } else {
                updateNote() { it.withAnagramSource(newValue) }
            }
        }
    }

    fun clearAcceptedPredictionEvent() {
        _acceptedPredictionEvent.value = null
    }

    fun shuffleAnagramSource() {
        updateNote() { note ->
            note.withAnagramSource(
                note.anagramSource?.toList()?.shuffled()?.joinToString("")
            )
        }
    }

    /**
     * Check if update to anagram solution is ok
     *
     * Returns \0 if not and change shouldn't be made. Does not update.
     */
    fun filterAnagramSolution(newChar : Char) : Char {
        if (
            anagramSourceValue.contains(newChar)
            || anagramSolutionValue.contains(newChar)
        ) {
            return newChar
        } else {
            return 0.toChar()
        }
    }

    fun updateAnagramSolution(pos : Int, oldChar : Char, newValue : String) {
        if (pos < 0 || boxesLength <= pos)
            return

        var source = padBlank(anagramSourceValue, boxesLength)
        var solution = padBlank(newValue, boxesLength)
        var swapped = false
        val newChar = solution[pos]

        val sourcePos = source.indexOf(newChar)
        if (sourcePos >= 0) {
            source = setChar(source, sourcePos, oldChar)
            swapped = true
        } else {
            // use old solution value to find a solution char to swap
            // with (will never be looking for blank here, so don't need
            // to extend anagramSolutionValue with blanks)
            val solPos = anagramSolutionValue.indexOf(newChar)
            if (solPos >= 0) {
                solution = setChar(solution, solPos, oldChar)
                swapped = true
            }
        }

        if (swapped) {
            updateNote() { it.withAnagram(source, solution) }
        }
    }

    fun transferEntries(source : NoteEntry, target : NoteEntry) {
        if (source == target) {
            // nothing to do
        } else if (!hasCopyConflict(source, target)) {
            executeTransfer(source, target)
        } else {
            _notesUIState.value = notesUIState.value?.copy(
                confirmTransferRequest = TransferRequest(source, target),
            )
        }
    }

    fun confirmTransferRequest(execute : Boolean) {
        notesUIState.value?.confirmTransferRequest?.let { request ->
            _notesUIState.value = notesUIState.value?.copy(
                confirmTransferRequest = null,
            )
            if (execute)
                executeTransfer(request.source, request.target)
        }
    }

    fun setNotesClueFlag(flagged : Boolean) {
        getBoard()?.flagClue(notesClue, flagged)
    }

    fun toggleNotesClueFlag() {
        notesClue?.let { clue ->
            getBoard()?.flagClue(
                notesClue,
                !clue.isFlagged(),
            )
        }
    }

    fun longClickNotesClueFlag() {
        notesClueID?.let { cid ->
            // assume want selected
            setNotesClueFlag(true)
            launchClueFlagDialog(cid)
        }
    }

    fun moveLeft() {
        getBoard()?.moveZoneBack(false)
    }

    fun moveRight() {
        getBoard()?.moveZoneForward(false)
    }

    fun callAnagramSolver() {
        val sourceLetters = getUpperLetters(anagramSourceValue)
        val solutionLetters = getUpperLetters(anagramSolutionValue)
        externalToolEvent.postValue(
            CrosswordSolverData.buildAnagram(
                sourceLetters + solutionLetters
            )
        )
    }

    fun callExternalDictionary(entry : NoteEntry) {
        ExternalDictionaries.build(
            settings,
            getNotesValue(entry),
            externalToolEvent::postValue
        )
    }

    fun callCrosswordSolver(entry : NoteEntry) {
        val request = blanksInCrosswordSolverFormat(getNotesValue(entry))
        externalToolEvent.postValue(
            CrosswordSolverData.buildMissingLetters(request)
        )
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        super.onPlayboardChange(changes)
        updateState(notesUIState.value?.notesClueID)
    }

    private val maxPuzzleDimension : Int
        get() {
            return getPuzzle()?.let { puz ->
                Math.max(puz.width, puz.height)
            } ?: 0
        }

    private fun setupVoiceCommands() {
        val app : Application = getApplication()

        registerVoiceCommandAnswer()
        registerVoiceCommandLetter()
        registerVoiceCommandClear()
        registerVoiceCommandAnnounceClue()
        registerVoiceCommandClueHelp()
        registerVoiceCommandBack()

        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_left),
                { _ -> moveLeft() }
            )
        )
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_right),
                { _ -> moveRight() }
            )
        )
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_scratch),
                { text ->
                    // remove non-word as not usually entered into grids
                    val prepped = text.replace("\\W+", "")
                        .uppercase(Locale.getDefault())

                    // means you can't enter "clear" into scratch, but oh well
                    val clear = app.getString(R.string.command_clear)
                    if (clear.equals(text, ignoreCase = true)) {
                        setScratchValue("")
                    } else {
                        val len = Math.min(prepped.length, boxesLength)
                        setScratchValue(prepped.substring(0, len))
                    }
                }
            )
        )
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_flag),
                { _ -> toggleNotesClueFlag() }
            )
        )
    }

    /**
     * Update state for current clue ID
     */
    private fun updateState() {
        updateState(notesClueID)
    }

    /**
     * Update state for given clue ID
     */
    private fun updateState(cid : ClueID?) {
        val puz = getPuzzle()
        val suppressNotesList = getSuppressNotesList(cid)
        if (cid == null) {
            val note = puz?.playerNote
            _notesUIState.value = notesUIState.value?.copy(
                notesClueID = null,
                notesClue = null,
                clueText = null,
                boxesLength = maxPuzzleDimension,
                suppressNotesList = suppressNotesList,
                scratchValue = note?.scratch ?: "",
                textValue = note?.text ?: "",
                anagramSourceValue = note?.anagramSource ?: "",
                anagramSolutionValue = note?.anagramSolution ?: "",
                anagramPrediction = null,
                clueFlagged = false,
                clueFlagColor = 0,
            )
        } else {
            val note = puz?.getNote(cid)
            val clue = puz?.let { it.getClue(cid) }
            if (clue == null) {
                _notesUIState.value = notesUIState.value?.copy(
                    notesClueID = cid,
                    notesClue = clue,
                    clueText = null,
                    boxesLength = maxPuzzleDimension,
                    suppressNotesList = suppressNotesList,
                    scratchValue = note?.scratch ?: "",
                    textValue = note?.text ?: "",
                    anagramSourceValue = note?.anagramSource ?: "",
                    anagramSolutionValue = note?.anagramSolution ?: "",
                    anagramPrediction = null,
                    clueFlagged = false,
                    clueFlagColor = 0,
                )
            } else {
                val boxesLength = if (clue.hasZone())
                    clue.zone.size()
                else
                    maxPuzzleDimension

                getLongClueText(clue) { text ->
                    val oldAnagramSource
                        = notesUIState.value?.anagramSourceValue ?: ""
                    val newAnagramSource = note?.anagramSource ?: ""

                    _notesUIState.value = notesUIState.value?.copy(
                        notesClueID = cid,
                        notesClue = clue,
                        clueText = text,
                        boxesLength = boxesLength,
                        suppressNotesList = suppressNotesList,
                        scratchValue = note?.scratch ?: "",
                        textValue = note?.text ?: "",
                        anagramSourceValue = newAnagramSource,
                        anagramSolutionValue = note?.anagramSolution ?: "",
                        anagramPrediction = null,
                        clueFlagged = clue.isFlagged(),
                        clueFlagColor = clue.flagColor,
                    )

                    if (oldAnagramSource != newAnagramSource) {
                        settings.getPlayPredictAnagramChars() { predict ->
                            if (predict)
                                predictAnagramChars()
                        }
                    }
                }
            }
        }
    }

    /**
     * Get suppress notes list for cid and current displayScratch value
     */
    private fun getSuppressNotesList(cid : ClueID?) : Set<String>? {
        if (isPuzzleNotes)
            return null

        val displayScratch = settings.livePlayScratchDisplay.value ?: false
        if (!displayScratch)
            return null

        if (cid == null)
            return setOf()

        val list = cid.getListName();
        if (list == null)
            return setOf()
        else
            return setOf(list);
    }

    private fun updateNote(transformer : (Note) -> Note) {
        val puz = getPuzzle()
        if (puz == null)
            return

        val newNote = transformer(note)
        if (isPuzzleNotes) {
            puz.setPlayerNote(newNote)
        } else {
            puz.setNote(notesClueID, newNote)
        }
        updateState()
    }

    private fun hasCopyConflict(
        source : NoteEntry,
        target : NoteEntry,
    ) : Boolean {
        // we just append for this one, rest overwrite
        if (target == NoteEntry.TEXT)
            return false

        val sourceValue = getNotesValue(source)
        val targetValue = getNotesValue(target)

        // for anagram solution we just move letters
        val checkBlanks = target != NoteEntry.ANAGRAM_SOLUTION

        for (i in 0 until Math.min(sourceValue.length, targetValue.length)) {
            if (
                (checkBlanks || !isBlank(sourceValue[i]))
                && !isBlank(targetValue[i])
                && sourceValue[i] != targetValue[i]
            ) {
                return true
            }
        }

        return false
    }

    private fun executeTransfer(source : NoteEntry, target : NoteEntry) {
        // handle this particularly weird combination separately
        if (
            source == NoteEntry.ANAGRAM_SOLUTION
            && target == NoteEntry.ANAGRAM_SOURCE
        ) {
            val anagSol = anagramSolutionValue.toCharArray()
            for (i in 0 until anagSol.size) {
                val oldChar = anagSol[i]
                anagSol[i] = BLANK_CHAR
                updateAnagramSolution(i, oldChar, String(anagSol))
            }
        } else {
            copyValueToTarget(getNotesValue(source), target)

            // clear source in some cases when moving to text
            if (target == NoteEntry.TEXT) {
                when (source) {
                    NoteEntry.SCRATCH -> { setScratchValue("") }
                    NoteEntry.ANAGRAM_SOURCE -> {
                        updateNote() { it.withAnagramSource("") }
                    }
                    NoteEntry.ANAGRAM_SOLUTION -> {
                        updateNote() { it.withAnagramSolution("") }
                    }
                    else -> { /* no change */ }
                }
            }
        }
    }

    private fun copyValueToTarget(value : String, target : NoteEntry) {
        when (target) {
            NoteEntry.BOARD -> {
                getBoard()?.setCurrentWordFromString(value)
            }
            NoteEntry.SCRATCH -> {
                // overwrite
                setScratchValue(value)
            }
            NoteEntry.TEXT -> {
                // append to end
                val newLine = if (textValue.isEmpty()) { "" } else { "\n" }
                setTextValue(textValue + newLine + stripBlank(value))
            }
            NoteEntry.ANAGRAM_SOURCE -> {
                // only add as many characters as will fit
                val anagSrc = charArrayWithBlanks(
                    anagramSourceValue,
                    boxesLength,
                )
                val len = Math.min(value.length, anagSrc.size)
                var numRemaining = numRemainingAnagramCharacters

                for (i in 0 until len) {
                    if (numRemaining == 0)
                        break
                    if (isBlank(anagSrc[i]))
                        numRemaining -= 1
                    anagSrc[i] = value[i]
                }

                updateNote() { it.withAnagramSource(String(anagSrc)) }
            }
            NoteEntry.ANAGRAM_SOLUTION -> {
                // simulate playing each character except blank
                val targetValue = charArrayWithBlanks(
                    anagramSolutionValue,
                    boxesLength
                )
                val len = Math.min(value.length, targetValue.size)
                for (i in 0 until len) {
                    if (!isBlank(value[i])) {
                        val oldChar = targetValue[i]
                        val newChar = filterAnagramSolution(value[i])
                        if (newChar != 0.toChar()) {
                            targetValue[i] = newChar
                            updateAnagramSolution(i, oldChar, String(targetValue))
                        }
                    }
                }
            }
        }
    }

    private fun getNotesValue(source : NoteEntry) : String {
        return when (source) {
            NoteEntry.BOARD -> boardNotesValue
            NoteEntry.SCRATCH -> scratchValue
            NoteEntry.TEXT -> textValue
            NoteEntry.ANAGRAM_SOURCE -> anagramSourceValue
            NoteEntry.ANAGRAM_SOLUTION -> anagramSolutionValue
        }
    }

    /**
     * Update anagram source prediction settings enabled
     */
    private fun predictAnagramChars() {
        if (notesClue == null) {
            setAnagramPrediction(null)
            return
        }

        val hint = notesClue?.getHint()
        if (hint == null) {
            setAnagramPrediction(null)
            return
        }

        val leadChars = getLeadingStringIfJustified(anagramSourceValue)
        if (
            leadChars == null
            || leadChars.isEmpty()
            || leadChars.length == boxesLength
            // don't predict if there are characters in the solution
            || leadChars.length != boxesLength - numRemainingAnagramCharacters
        ) {
            setAnagramPrediction(null)
            return
        }

        // strip HTML and spaces
        val strippedHint = hint.replace(
            utils.getFilterClueToAlphabeticRegex().toRegex(), ""
        )

        val sourceIdx = strippedHint.indexOf(leadChars, ignoreCase = true)
        if (sourceIdx < 0) {
            setAnagramPrediction(null)
            return
        }

        val prediction = getUpperLetters(
            strippedHint, sourceIdx, boxesLength
        );

        setAnagramPrediction(prediction)
    }

    /**
     * Get all characters up to first blank if left justified
     *
     * That is, no non-blank characters after the first blank
     */
    private fun getLeadingStringIfJustified(s : String) : String? {
        var firstBlank = -1
        for (i in 0 until s.length) {
            if (firstBlank < 0) {
                // still looking for blank
                if (isBlank(s[i]))
                    firstBlank = i
            } else {
                // checking no non-blank
                if (!isBlank(s[i]))
                    return null
            }
        }
        if (firstBlank >= 0)
            return s.substring(0, firstBlank)
        else
            return s
    }

    /**
     * Update state with prediction, null means clear
     */
    private fun setAnagramPrediction(prediction : String?) {
        _notesUIState.value = notesUIState.value?.copy(
            anagramSourcePredictionValue = prediction,
        )
    }

    /**
     * Get alphabetic chars from string in uppercase
     *
     * @param source string to get from
     * @param start char to start from
     * @param length max num chars to get
     * @return string up to length chars only alphabetical or
     * number converted to upper case
     */
    private fun getUpperLetters(
        source : String,
        start : Int = 0,
        length : Int = source.length,
    ) : String {
        val sb = StringBuilder()
        val end = Math.min(source.length, start + length)
        for (i in start until end) {
            val c = source[i]
            if (c.isLetter())
                sb.append(c.uppercase())
        }
        return sb.toString();
    }
}
