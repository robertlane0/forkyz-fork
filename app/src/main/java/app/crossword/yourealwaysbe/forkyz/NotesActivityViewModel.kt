
package app.crossword.yourealwaysbe.forkyz

import java.util.Locale
import javax.inject.Inject

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Note
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges

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
    val clueText : String? = null,
    val boxesLength : Int = 0,
    val suppressNotesList : Set<String>? = null,
    val scratchValue : String = "",
    val textValue : String = "",
    val clueFlagged : Boolean = false,
    val clueFlagColor : Int = 0,
) {
    val isPuzzleNotes : Boolean
        get() = notesClueID == null
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

    init {
        setupVoiceCommands()

        _notesUIState.addSource(settings.livePlayScratchDisplay) {
            _notesUIState.value = notesUIState.value?.copy(
                suppressNotesList = getSuppressNotesList(notesClueID)
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
        get() { return getPuzzle()?.getClue(notesClueID) }

    val isPuzzleNotes : Boolean
        get() { return notesUIState.value?.isPuzzleNotes ?: false }

    val boxesLength : Int
        get() { return notesUIState.value?.boxesLength ?: 0 }

    /**
     * Only null if no puzzle
     */
    val note : Note?
        get() {
            return getPuzzle()?.let { puz ->
                if (isPuzzleNotes) {
                    return puz.playerNote ?: Note(boxesLength)
                } else {
                    return puz.getNote(notesClueID) ?: Note(boxesLength)
                }
            }
        }

    fun setScratchValue(value : String) {
        updateNote() { note -> note.withScratch(value) }
    }

    fun setTextValue(value : String) {
        updateNote() { note -> note.withText(value) }
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
                    // // remove non-word as not usually entered into grids
                    // val prepped = text.replaceAll("\\W+", "")
                    //     .toUpperCase(Locale.getDefault())

                    // // means you can't enter "clear" into scratch, but oh well
                    // val clear = app.getString(R.string.command_clear)
                    // if (clear.equalsIgnoreCase(text)) {
                    //     binding.scratchMiniboard.clear()
                    // } else {
                    //     int len = Math.min(
                    //         prepped.length(), binding.scratchMiniboard.getLength()
                    //     )
                    //     for (int i = 0 i < len i++) {
                    //         binding.scratchMiniboard
                    //             .setResponse(i, prepped.charAt(i))
                    //     }
                    // }

                    // // need to save since is called between an onPause and an
                    // // onResume
                    // saveNoteToBoard()
                }
            )
        )
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_flag),
                { _ ->
                    // binding.flagClue.setChecked(true)

                    // // need to save since is called between an onPause and an
                    // // onResume
                    // saveNoteToBoard()
                }
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
                clueText = null,
                boxesLength = maxPuzzleDimension,
                suppressNotesList = suppressNotesList,
                scratchValue = note?.scratch ?: "",
                textValue = note?.text ?: "",
                clueFlagged = false,
                clueFlagColor = 0,
            )
        } else {
            val note = puz?.getNote(cid)
            val clue = puz?.let { it.getClue(cid) }
            if (clue == null) {
                _notesUIState.value = notesUIState.value?.copy(
                    notesClueID = cid,
                    clueText = null,
                    boxesLength = maxPuzzleDimension,
                    suppressNotesList = suppressNotesList,
                    scratchValue = note?.scratch ?: "",
                    textValue = note?.text ?: "",
                    clueFlagged = false,
                    clueFlagColor = 0,
                )
            } else {
                val boxesLength = if (clue.hasZone())
                    clue.zone.size()
                else
                    maxPuzzleDimension

                getLongClueText(clue) { text ->
                    _notesUIState.value = notesUIState.value?.copy(
                        notesClueID = cid,
                        clueText = text,
                        boxesLength = boxesLength,
                        suppressNotesList = suppressNotesList,
                        scratchValue = note?.scratch ?: "",
                        textValue = note?.text ?: "",
                        clueFlagged = clue.isFlagged(),
                        clueFlagColor = clue.flagColor,
                    )
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

        note?.let { note ->
            val newNote = transformer(note)
            if (isPuzzleNotes) {
                puz.setPlayerNote(newNote)
            } else {
                puz.setNote(notesClueID, newNote)
            }
            updateState()
        }
    }
}
