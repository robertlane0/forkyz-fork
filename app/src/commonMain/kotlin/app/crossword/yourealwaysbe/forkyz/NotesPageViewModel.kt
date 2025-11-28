
package app.crossword.yourealwaysbe.forkyz

import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.exttools.CrosswordSolverData
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalDictionaries
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.getOnce
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed
import app.crossword.yourealwaysbe.forkyz.view.WordEditViewModel
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.MovementStrategy
import app.crossword.yourealwaysbe.puz.Note
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.Word

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
        return padBlank(s, pos) + c
    else
        return s.substring(0, pos) + c + s.substring(pos + 1)
}

/**
 * Gets char or blank if out of s bounds
 */
private fun getChar(s : String, pos : Int) : Char {
    return s.getOrElse(pos) { BLANK_CHAR }
}

/**
 * Delete char at pos from s
 */
private fun deleteChar(s : String, pos : Int) : String {
    return if (s.length > pos)
        setChar(s, pos, BLANK_CHAR)
    else
        s
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
 * Used for doing internal anagram updates
 */
private data class AnagramValues(
    val source : String,
    val solution : String,
)

/**
 * UI State for notes
 *
 * @param notesClueID the ID of the clue if clue notes else null (puzzle
 * notes)
 * @param clueText the clue text to display with the clue, or null if
 * this is the zone of cells with no clue attached
 */
data class NotesPageUIState(
    val notesClueID : ClueID? = null,
    val clueText : String? = null,
    val boxesLength : Int = 0,
    val scratchValue : String = "",
    val textValue : String = "",
    val anagramSourceValue : String = "",
    val anagramSourcePredictionValue : String? = null,
    val anagramSolutionValue : String = "",
    val anagramPrediction : String? = null,
    val clueFlagged : Boolean = false,
    val clueFlagColor : Int = 0,
    val confirmTransferRequest : TransferRequest? = null,
    val renderSettings : RenderSettings = RenderSettings(),
    val skipFilled : Boolean = false,
    val expandableClueLine : Boolean = false,
    val clueSeparators : ImmutableList<String?> = persistentListOf(),
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

private data class DisplaySettings(
    val inferSeparators : Boolean,
    val renderSettings : RenderSettings,
    val expandableClueLine : Boolean,
)

private data class InteractionSettings(
    val skipFilled : Boolean,
)

private data class NotesSettings(
    val display : DisplaySettings,
    val interaction : InteractionSettings,
)

class NotesPageViewModel(
    val notesClueID : ClueID?,
    settings : ForkyzSettings,
    utils : NativeBackendUtils,
    currentPuzzleHolder : CurrentPuzzleHolder,
    fileHandlerProvider : FileHandlerProvider,
) : PuzzlePageViewModel(
    settings,
    utils,
    currentPuzzleHolder,
    fileHandlerProvider,
) {
    private val mediatedNotesUIState
        = MediatedStateWithFlow<NotesPageUIState, NotesSettings>(
            viewModelScope,
            NotesPageUIState(),
            { state, notesSettings ->
                state.copy(
                    renderSettings = notesSettings.display.renderSettings,
                    skipFilled = notesSettings.interaction.skipFilled,
                    expandableClueLine
                        = notesSettings.display.expandableClueLine,
                    clueSeparators = getSeparators(
                        notesClueID,
                        notesSettings.display.inferSeparators,
                    ),
                )
            },
            combine(
                combine(
                    settings.livePlayInferSeparators,
                    settings.livePlayRenderSettings,
                    settings.livePlayExpandableClueLine,
                    ::DisplaySettings,
                ),
                settings.livePlaySkipFilled.map(::InteractionSettings),
                ::NotesSettings,
            ),
        )
    val notesUIState : StateFlow<NotesPageUIState>
        = mediatedNotesUIState.stateFlow

    private val _acceptedPredictionEvent = MutableStateFlow<Int?>(null)
    /**
     * The length of a prediction if just accepted or null
     */
    val acceptedPredictionEvent : StateFlow<Int?>
        = _acceptedPredictionEvent

    val wordEditViewModel = WordEditViewModel(
        viewModelScope,
        settings,
        getBoard(),
        this::playLetterToBoard,
        this::deleteLetterFromBoard,
        notesClueID?.let { getBoard()?.getClueWord(it) },
        showOwnScratch = false,
    )

    init {
        setupVoiceCommands()
        updateState()
    }

    val notesClue : Clue?
        get() { return getBoard()?.puzzle?.getClue(notesClueID) }

    val isPuzzleNotes : Boolean
        get() { return notesClueID == null }

    val boxesLength : Int
        get() { return currentNotesUIState.boxesLength }

    val numRemainingAnagramCharacters : Int
        get() { return currentNotesUIState.numRemainingAnagramCharacters }

    /**
     * The value of the current word, but as a string not a string array
     *
     * For use when copying from board to notes, where notes are
     * represented as strings with one char per box
     */
    val boardNotesValue : String
        get() {
            return getBoard()?.let { board ->
                val zone = notesClue?.getZone() ?: board.currentWord.zone
                return String(
                    zone.map { pos ->
                        val box = board.puzzle?.checkedGetBox(pos)
                        if (box?.isBlank() ?: true)
                            BLANK_CHAR
                        else
                            box.response[0]
                    }.toCharArray()
                )
            } ?: ""
        }

    val scratchValue : String
        get() { return currentNotesUIState.scratchValue }

    val textValue : String
        get() { return currentNotesUIState.textValue }

    val anagramSourceValue : String
        get() { return currentNotesUIState.anagramSourceValue }

    val anagramSourcePredictionValue : String?
        get() { return currentNotesUIState.anagramSourcePredictionValue }

    val anagramSolutionValue : String
        get() { return currentNotesUIState.anagramSolutionValue }

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
        playLetterToBoard(c.toString())
    }

    fun playLetterToBoard(c : String) {
        getBoard()?.let { board ->
            val oldMovementStrategy = board.getMovementStrategy()
            movementStrategyFlow.getOnce { movementStrategy ->
                board.setMovementStrategy(movementStrategy)
                board.playLetter(c)
                board.setMovementStrategy(oldMovementStrategy)
            }
        }
    }

    fun deleteLetterFromBoard() {
        getBoard()?.let { board ->
            movementStrategyFlow.getOnce { movementStrategy ->
                val oldMovementStrategy = board.getMovementStrategy()
                board.setMovementStrategy(movementStrategy)
                board.deleteOrUndoLetter()
                board.setMovementStrategy(oldMovementStrategy)
            }
        }
    }

    fun deleteScratch(pos : Int) {
        val newValue = deleteChar(scratchValue, pos)
        updateNote() { it.withScratch(newValue) }
    }

    fun changeScratch(pos : Int, newChar : Char) : String? {
        val newValue = setChar(scratchValue, pos, newChar)
        updateNote() { it.withScratch(newValue) }
        return newValue
    }

    fun setScratchValue(value : String) {
        updateNote() { it.withScratch(value) }
    }

    fun setTextValue(value : String) {
        updateNote() { it.withText(value) }
    }

    fun deleteAnagramSource(pos : Int) {
        val newValue = deleteChar(anagramSourceValue, pos)
        updateNote() { it.withAnagramSource(newValue) }
    }

    fun changeAnagramSource(pos : Int, newChar : Char) : String? {
        val oldChar = getChar(anagramSourceValue, pos)

        val canChange = (
            isBlank(newChar)
            || !isBlank(oldChar)
            || numRemainingAnagramCharacters > 0
        )

        if (!canChange)
            return null

        if (
            isBlank(oldChar)
            && isBlank(newChar)
            && anagramSourcePredictionValue != null
        ) {
            updateNote() {
                it.withAnagramSource(anagramSourcePredictionValue)
            }
            _acceptedPredictionEvent.value =
                anagramSourcePredictionValue?.length

            return anagramSourcePredictionValue
        } else {
            val newValue = setChar(anagramSourceValue, pos, newChar)
            updateNote() { it.withAnagramSource(newValue) }
            return newValue
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

    fun deleteAnagramSolution(pos : Int) {
        changeAnagramSolution(pos, BLANK_CHAR)
    }

    fun changeAnagramSolution(pos : Int, newChar : Char) : String? {
        if (!canAddToAnagramSolutionValues(currentAnagramValues, newChar))
            return null

        val oldChar = getChar(anagramSolutionValue, pos)
        val newValue = setChar(anagramSolutionValue, pos, newChar)
        val newValues = updateAnagramSolutionValues(
            currentAnagramValues,
            pos,
            oldChar,
            newValue,
        )
        updateAnagramValues(newValues)
        return currentAnagramValues.solution
    }

    fun transferEntries(source : NoteEntry, target : NoteEntry) {
        if (source == target) {
            // nothing to do
        } else if (!hasCopyConflict(source, target)) {
            executeTransfer(source, target)
        } else {
            currentNotesUIState = currentNotesUIState.copy(
                confirmTransferRequest = TransferRequest(source, target),
            )
        }
    }

    fun confirmTransferRequest(execute : Boolean) {
        currentNotesUIState.confirmTransferRequest?.let { request ->
            currentNotesUIState = currentNotesUIState.copy(
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
            showFlagClueDialog(cid)
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
        _externalToolEvent.value = CrosswordSolverData.buildAnagram(
            sourceLetters + solutionLetters
        )
    }

    fun callExternalDictionary(entry : NoteEntry) {
        ExternalDictionaries.build(
            settings,
            getNotesValue(entry),
            { _externalToolEvent.value = it },
        )
    }

    fun callCrosswordSolver(entry : NoteEntry) {
        val request = getNotesValue(entry)
        _externalToolEvent.value = CrosswordSolverData.buildMissingLetters(
            request,
        )
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        super.onPlayboardChange(changes)
        updateState()
    }

    private val maxPuzzleDimension : Int
        get() {
            return getPuzzle()?.let { puz ->
                Math.max(puz.width, puz.height)
            } ?: 0
        }

    private fun setupVoiceCommands() {
        registerVoiceCommandAnswer()
        registerVoiceCommandLetter()
        registerVoiceCommandClear()
        registerVoiceCommandAnnounceClue()
        registerVoiceCommandClueHelp()
        registerVoiceCommandBack()

        viewModelScope.launch {
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(R.string.command_left),
                    { _ -> moveLeft() }
                )
            )
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(R.string.command_right),
                    { _ -> moveRight() }
                )
            )
            val clear = utils.getString(R.string.command_clear)
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(R.string.command_scratch),
                    { text ->
                        // remove non-word as not usually entered into grids
                        val prepped = text.replace("\\W+", "")
                            .uppercase(Locale.getDefault())

                        // means you can't enter "clear" into scratch, but oh well
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
                    utils.getString(R.string.command_flag),
                    { _ -> toggleNotesClueFlag() }
                )
            )
        }
    }

    /**
     * Update state for given clue ID
     */
    private fun updateState() {
        val cid = notesClueID
        val puz = getPuzzle()
        if (cid == null) {
            val note = puz?.playerNote
            currentNotesUIState = currentNotesUIState.copy(
                notesClueID = null,
                clueText = null,
                boxesLength = maxPuzzleDimension,
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
                currentNotesUIState = currentNotesUIState.copy(
                    notesClueID = cid,
                    clueText = null,
                    boxesLength = maxPuzzleDimension,
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

                viewModelScope.launch {
                    val text = getClueLineClueText(clue)
                    val oldAnagramSource
                        = currentNotesUIState.anagramSourceValue
                    val newAnagramSource = note?.anagramSource ?: ""

                    currentNotesUIState = currentNotesUIState.copy(
                        notesClueID = cid,
                        clueText = text,
                        boxesLength = boxesLength,
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
     * Get separators for given clue
     */
    private fun getSeparators(
        cid : ClueID?,
        inferSeparators : Boolean,
    ) : ImmutableList<String?> {
        val clue = getBoard()?.puzzle?.getClue(cid)
        if (isPuzzleNotes || !(clue?.hasZone() ?: false))
            return persistentListOf()

        val len = clue?.zone?.size() ?: -1
        return (0..len).map { i ->
            clue?.getSeparator(i, inferSeparators)
        }.toImmutableList()
    }

    private fun updateNote(transformer : (Note) -> Note) {
        val board = getBoard()
        if (board == null)
            return

        val newNote = transformer(note)
        if (isPuzzleNotes) {
            board.setPlayerNote(newNote)
        } else {
            board.setClueNote(notesClueID, newNote)
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
            var values = currentAnagramValues
            val anagSol = values.solution.toCharArray()
            for (i in 0 until anagSol.size) {
                val oldChar = anagSol[i]
                anagSol[i] = BLANK_CHAR
                values = updateAnagramSolutionValues(
                    values,
                    i,
                    oldChar,
                    String(anagSol),
                )
            }
            updateAnagramValues(values)
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
                var values = currentAnagramValues
                val len = Math.min(value.length, boxesLength)
                for (i in 0 until len) {
                    if (!isBlank(value[i])) {
                        val solution = padBlank(values.solution, boxesLength)
                        val oldChar = solution[i]
                        val newChar = value[i]
                        if (canAddToAnagramSolutionValues(values, newChar)) {
                            values = updateAnagramSolutionValues(
                                values,
                                i,
                                oldChar,
                                setChar(solution, i, newChar)
                            )
                        }
                    }
                }
                updateAnagramValues(values)
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
        currentNotesUIState = currentNotesUIState.copy(
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

    /**
     * Set a new character in anagram solution
     *
     * Swaps between solution and source, and blocks if not possible.
     * Returns updated value.
     */
    private fun updateAnagramSolutionValues(
        anagramValues : AnagramValues,
        pos : Int,
        oldChar : Char,
        newValue : String,
    ) : AnagramValues {
        if (pos < 0 || boxesLength <= pos)
            return anagramValues

        var source = padBlank(anagramValues.source, boxesLength)
        var solution = padBlank(newValue, boxesLength)
        val newChar = solution[pos]

        val sourcePos = source.indexOf(newChar)
        if (sourcePos >= 0) {
            source = setChar(source, sourcePos, oldChar)
        } else {
            // use old solution value to find a solution char to swap
            // with (will never be looking for blank here, so don't need
            // to extend solution with blanks)
            val solPos = anagramValues.solution.indexOf(newChar)
            if (solPos >= 0)
                solution = setChar(solution, solPos, oldChar)
        }

        return AnagramValues(source, solution)
    }

    private fun updateAnagramValues(values : AnagramValues) {
        if (values != currentAnagramValues)
            updateNote() { it.withAnagram(values.source, values.solution) }
    }

    /**
     * If it's possible to add newChar to the given anagram values
     */
    private fun canAddToAnagramSolutionValues(
        values : AnagramValues,
        newChar : Char,
    ) : Boolean {
        return (
            values.source.contains(newChar)
            || values.solution.contains(newChar)
        )
    }

    private val movementStrategyFlow : StateFlow<MovementStrategy>
        = settings.livePlayStopOnEndStrategy.stateInSubscribed(
            viewModelScope,
            MovementStrategy.STOP_ON_END,
        )
    init {
        movementStrategyFlow.getOnce {
            // Because the flow is not permanently subscribed, an initial get
            // is needed to prime it with the right value. It will be one
            // keypress behind forevermore.
        }
    }

    private val currentAnagramValues : AnagramValues
        get() { return AnagramValues(anagramSourceValue, anagramSolutionValue) }

    private var currentNotesUIState : NotesPageUIState
        get() { return mediatedNotesUIState.current }
        set(value) { mediatedNotesUIState.current = value }


}
