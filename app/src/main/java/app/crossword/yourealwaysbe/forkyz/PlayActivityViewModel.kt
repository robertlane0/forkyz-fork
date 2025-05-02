
package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.GridRatio
import app.crossword.yourealwaysbe.forkyz.settings.FitToScreenMode
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.liveDataFanIn
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import kotlin.random.Random

/**
 * UI State for play
 *
 * @param clueText the clue text to display with the clue
 * @param puzzleTitle the title of the current puzzle
 * @param isClueBelowGrid whether to show clue beneath grid or in title
 * @param showClueTabs whether to show clues below grid
 * @param clueTabsPage0 which page to show on first clue tabs
 * @param clueTabsPage1 which page to show on second clue tabs
 * @param portraitGridRatio grid ratio in portrait
 * @param landscapeGridRatio grid ratio in portrait
 * @param puzzleAspectRatio the aspect ratio of the current puzzle
 * @param isRandomClueOnShake whether shakes should jump to random clue
 * @param fitToScreenMode the fit to screen mode
 * @param isAcrostic if the puzzle is an acrostic one
 * @param isFullScreen if the display should be full screen
 */
data class PlayActivityUIState(
    val clueText : String? = null,
    val puzzleTitle : String = "",
    val isClueBelowGrid : Boolean = false,
    val showClueTabs : Boolean = false,
    val clueTabsPage0 : Int = 0,
    val clueTabsPage1 : Int = 1,
    val portraitGridRatio : GridRatio = GridRatio.GR_PUZZLE_SHAPE,
    val landscapeGridRatio : GridRatio = GridRatio.GR_PUZZLE_SHAPE,
    val puzzleAspectRatio : Float = 1.0F,
    val isRandomClueOnShake : Boolean = false,
    val fitToScreenMode : FitToScreenMode = FitToScreenMode.FTSM_NEVER,
    val isAcrostic : Boolean = false,
    val isFullScreen : Boolean = false,
    val firstPlayMessage : String? = null,
)

@HiltViewModel
class PlayActivityViewModel @Inject constructor(
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
    private var _playUIState : MutableLiveData<PlayActivityUIState>
    public var playUIState : LiveData<PlayActivityUIState>
        private set

    // indicates if first run message was shown and is now cleared
    // will stay false if this is not the first run of the puzzle
    private var firstPlayCleared = false

    public val isAcrostic : Boolean
        get() {
            return getPuzzle()?.let { it.kind == Puzzle.Kind.ACROSTIC } ?: false
        }
    init {
        setupVoiceCommands()

        _playUIState = liveDataFanIn(
            {
                val curState = playUIState.value ?: PlayActivityUIState()
                val showClueTabs = isAcrostic
                    || (settings.livePlayShowClueTabs.value ?: false)
                curState.copy(
                    isClueBelowGrid
                        = settings.livePlayClueBelowGrid.value ?: false,
                    showClueTabs = showClueTabs,
                    clueTabsPage0
                        = settings.livePlayClueTabsPage(0).value ?: 0,
                    clueTabsPage1
                        = settings.livePlayClueTabsPage(1).value ?: 1,
                    portraitGridRatio
                        = settings.livePlayGridRatioPortrait.value
                            ?: GridRatio.GR_PUZZLE_SHAPE,
                    landscapeGridRatio
                        = settings.livePlayGridRatioLandscape.value
                            ?: GridRatio.GR_PUZZLE_SHAPE,
                    isRandomClueOnShake
                        = settings.livePlayRandomClueOnShake.value ?: false,
                    fitToScreenMode
                        = settings.livePlayFitToScreenMode.value
                            ?: FitToScreenMode.FTSM_NEVER,
                    isFullScreen = settings.livePlayFullScreen.value ?: false,
                )
            },
            settings.livePlayClueBelowGrid,
            settings.livePlayShowClueTabs,
            settings.livePlayClueTabsPage(0),
            settings.livePlayClueTabsPage(1),
            settings.livePlayGridRatioPortrait,
            settings.livePlayGridRatioLandscape,
            settings.livePlayRandomClueOnShake,
            settings.livePlayFitToScreenMode,
            settings.livePlayFullScreen,
        )
        playUIState = _playUIState

        updateState()
    }

    /**
     * Get value of scale saved in settings
     *
     * Not part of playUIState currently as scale is driven by the
     * BoardFullEditView widget, putting it in the state causes race
     * conditions with e.g. zoomToFit.
     */
    fun getSavedScale(cb : (Float) -> Unit) {
        settings.getPlayScale(cb)
    }

    fun saveScale(scale : Float) {
        settings.setPlayScale(scale)
    }

    fun prevClue() {
        getBoard()?.previousWord()
    }

    fun nextClue() {
        getBoard()?.nextWord()
    }

    fun clickBoard(previousWord : Word?) {
        // only show keyboard if double click a word
        // hide if it's a new word
        getBoard()?.let { board ->
            val newPos = board.highlightLetter
            if (previousWord?.checkInWord(newPos.row, newPos.col) ?: false) {
                showKeyboard()
            } else {
                hideKeyboard()
            }
        }
    }

    fun longClickBoard(position : Position?) {
        getBoard()?.let { board ->
            position?.let { position ->
                board.setHighlightLetter(position)
                launchClueNotes(board.getClueID())
            }
        }
    }

    fun showClueTabs() {
        settings.setPlayShowClueTabs(true)
    }

    /**
     * Hide the clue tabs
     *
     * Needs to know which page they were on to save
     */
    fun hideClueTabs(page0 : Int?, page1 : Int?) {
        saveClueTabsPages(page0, page1)
        if (!isAcrostic)
            settings.setPlayShowClueTabs(false)
    }

    fun clickClue(clue : Clue?, previousWord : Word? = null) {
        if (clue == null)
            return

        getBoard()?.let { board ->
            val word = previousWord ?: board.currentWord
            if (clue.hasZone() && clue.clueID != board.clueID)
                board.jumpToClue(clue)
            displayKeyboard(word)
        }
    }

    fun saveClueTabsPages(page0 : Int?, page1 : Int?) {
        page0?.let { settings.setPlayClueTabsPage(0, it) }
        page1?.let { settings.setPlayClueTabsPage(1, it) }
    }

    fun deleteLetter() {
        getBoard()?.let { board ->
            settings.getPlayScratchMode { scratchMode ->
                if (scratchMode)
                    board.deleteScratchLetter();
                else
                    board.deleteOrUndoLetter();
            }
        }
    }

    fun playLetter(c : Char) {
        getBoard()?.let { board ->
            settings.getPlayScratchMode { scratchMode ->
                if (scratchMode)
                    board.playScratchLetter(c)
                else
                    board.playLetter(c)
            }
        }
    }

    fun playSpace() {
        settings.getPlaySpaceChangesDirection { changesDir ->
            if (changesDir)
                getBoard()?.toggleSelection()
            else
                playLetter(' ')
        }
    }

    fun playEnter() {
        settings.getPlayEnterChangesDirection { changesDir ->
            if (changesDir)
                getBoard()?.toggleSelection()
            else
                nextClue()
        }
    }

    fun moveLeft() {
        getBoard()?.moveLeft()
    }

    fun moveRight() {
        getBoard()?.moveRight()
    }

    fun moveUp() {
        getBoard()?.moveUp()
    }

    fun moveDown() {
        getBoard()?.moveDown()
    }

    fun toggleSelection() {
        getBoard()?.toggleSelection()
    }

    fun pickRandomUnfilledClue() {
        val board = getBoard()
        val puz = getPuzzle()
        if (board == null || puz == null)
            return

        val currentID = board.getClueID();

        val unfilledClues = puz.getAllClues().filter { clue ->
            val cid = clue.getClueID()
            cid != currentID && !board.isFilledClueID(clue.clueID)
        }

        if (!unfilledClues.isEmpty()) {
            // bit inefficient, but saves a field
            val idx = Random.nextInt(unfilledClues.size);
            board.jumpToClue(unfilledClues[idx]);
        }
    }

    fun clearFirstPlayMessage() {
        firstPlayCleared = true
        _playUIState.value = playUIState.value?.copy(
            firstPlayMessage = null,
        )
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        super.onPlayboardChange(changes)
        updateState()
    }

    private fun setupVoiceCommands() {
        registerVoiceCommandAnswer()
        registerVoiceCommandLetter()
        registerVoiceCommandNumber()
        registerVoiceCommandClear()
        registerVoiceCommandAnnounceClue()
        registerVoiceCommandClueHelp()
        registerVoiceCommandBack()
        registerVoiceCommandNotes()

        val app : Application = getApplication()

        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_delete),
            { deleteLetter() },
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_toggle),
            { toggleSelection() },
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_next),
            { nextClue() }
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_previous),
            { prevClue() },
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_left),
            { moveLeft() },
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_right),
            { moveRight() },
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_up),
            { moveUp() },
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_down),
            { moveDown() },
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_clues),
            { launchClueList() },
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_jump_random),
            { pickRandomUnfilledClue() }
        ))
    }

    private fun getClueText(cb : (String?) -> Unit) {
        val clue = getBoard()?.clue
        if (clue != null) {
            getLongClueText(clue, cb)
        } else {
            cb(null)
        }
    }

    private fun updateState() {
        getPuzzle()?.let { puz ->
            getClueText { clueText ->
                val state = playUIState.value ?: PlayActivityUIState()

                val aspectRatio = if (puz.height == 0)
                    1F
                else
                    puz.width.toFloat() / puz.height

                val firstPlayMessage = if (
                    isFirstPlay && !firstPlayCleared && puz.hasIntroMessage()
                ) {
                    puz.getIntroMessage()
                } else {
                    null
                }

                _playUIState.value = state.copy(
                    puzzleTitle = puz.title,
                    clueText = clueText,
                    puzzleAspectRatio = aspectRatio,
                    isAcrostic = isAcrostic,
                    firstPlayMessage = firstPlayMessage,
                )
            }
        }
    }

    /**
     * Change keyboard display if the same word has been selected twice
     */
    private fun displayKeyboard(previous : Word) {
        // only show keyboard if double click a word
        // hide if it's a new word
        getBoard()?.let { board ->
            val newPos = board.getHighlightLetter()
            if (previous.checkInWord(newPos.row, newPos.col)) {
                showKeyboard()
            } else {
                hideKeyboard()
            }
        }
    }
}

