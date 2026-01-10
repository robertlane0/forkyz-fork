
package app.crossword.yourealwaysbe.forkyz

import kotlin.random.Random
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_clues
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_delete
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_down
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_jump_random
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_left
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_next
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_previous
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_right
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_toggle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_up
import app.crossword.yourealwaysbe.forkyz.settings.FitToScreenMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.GridRatio
import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.view.BoardEditViewModel
import app.crossword.yourealwaysbe.forkyz.view.ClueTabsViewModel
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle

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
 * @param firstPlayMessage message to display on first play
 * @param renderSettings rendering settings for board view
 * @param scratchMode whether is scratch mode
 * @param ensureVisible whether to jump to current clue on play board
 */
data class PlayPageUIState(
    val clueText : String? = null,
    val puzzleTitle : String? = null,
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
    val renderSettings : RenderSettings = RenderSettings(),
    val scratchMode : Boolean = false,
    val doubleTapFitBoard : Boolean = false,
    val ensureVisible : Boolean = false,
    val expandableClueLine : Boolean = false,
    val dontSnapBoardToBorders : Boolean = false,
)

private data class PlayClueTabsSettings(
    val showClueTabs : Boolean,
    val clueTabsPage0 : Int,
    val clueTabsPage1 : Int,
    val gridRatioPortrait : GridRatio,
    val gridRatioLandscape : GridRatio,
)

private data class PlayDisplaySettings(
    val clueBelowGrid : Boolean,
    val fitToScreenMode : FitToScreenMode,
    val fullScreen : Boolean,
    val renderSettings : RenderSettings,
    val expandableClueLine : Boolean,
)

private data class PlayInteractionSettings(
    val randomClueOnShake : Boolean,
    val scratchMode : Boolean,
    val doubleTapFitBoard : Boolean,
    val ensureVisible : Boolean,
    val dontSnapBoardToBorders : Boolean,
)

private data class PlaySettings(
    val clueTabsSettings : PlayClueTabsSettings,
    val displaySettings : PlayDisplaySettings,
    val interactionSettings : PlayInteractionSettings,
)

class PlayPageViewModel(
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
    // so we don't interleave updates and end up with out of date clue
    // lines
    private val updateStateLock = Mutex()

    private val mediatedPlayUIState
        = MediatedStateWithFlow<PlayPageUIState, PlaySettings>(
            viewModelScope,
            PlayPageUIState(),
            { state, playSettings ->
                val showClueTabs
                    = isAcrostic || playSettings.clueTabsSettings.showClueTabs
                state.copy(
                    isClueBelowGrid
                        = playSettings.displaySettings.clueBelowGrid,
                    showClueTabs = showClueTabs,
                    clueTabsPage0 = playSettings.clueTabsSettings.clueTabsPage0,
                    clueTabsPage1 = playSettings.clueTabsSettings.clueTabsPage1,
                    portraitGridRatio
                        = playSettings.clueTabsSettings.gridRatioPortrait,
                    landscapeGridRatio
                        = playSettings.clueTabsSettings.gridRatioLandscape,
                    isRandomClueOnShake
                        = playSettings.interactionSettings.randomClueOnShake,
                    fitToScreenMode
                        = playSettings.displaySettings.fitToScreenMode,
                    isFullScreen = playSettings.displaySettings.fullScreen,
                    renderSettings
                        = playSettings.displaySettings.renderSettings,
                    scratchMode = playSettings.interactionSettings.scratchMode,
                    doubleTapFitBoard
                        = playSettings.interactionSettings.doubleTapFitBoard,
                    ensureVisible
                        = playSettings.interactionSettings.ensureVisible,
                    expandableClueLine
                        = playSettings.displaySettings.expandableClueLine,
                    dontSnapBoardToBorders = playSettings
                        .interactionSettings
                        .dontSnapBoardToBorders,
                )
            },
            combine(
                combine(
                    settings.livePlayShowClueTabs,
                    settings.livePlayClueTabsPage(0),
                    settings.livePlayClueTabsPage(1),
                    settings.livePlayGridRatioPortrait,
                    settings.livePlayGridRatioLandscape,
                    ::PlayClueTabsSettings,
                ),
                combine(
                    settings.livePlayClueBelowGrid,
                    settings.livePlayFitToScreenMode,
                    settings.livePlayFullScreen,
                    settings.livePlayRenderSettings,
                    settings.livePlayExpandableClueLine,
                    ::PlayDisplaySettings,
                ),
                combine(
                    settings.livePlayRandomClueOnShake,
                    settings.livePlayScratchMode,
                    settings.livePlayDoubleTapFitBoard,
                    settings.livePlayEnsureVisible,
                    settings.livePlayDontSnapBoardToBorders,
                    ::PlayInteractionSettings,
                ),
                ::PlaySettings,
            ),
        )
    val playUIState : StateFlow<PlayPageUIState>
        = mediatedPlayUIState.stateFlow

    val boardEditViewModel = BoardEditViewModel(
        viewModelScope,
        settings,
        getBoard(),
        this::playResponse,
        this::deleteResponse,
    )

    val clueTabsViewModel = ClueTabsViewModel(
        utils,
        viewModelScope,
        settings,
        getBoard(),
        this::playResponse,
        this::deleteResponse,
    )
    init {
        settings.getPlayClueTabsPage(0) { clueTabsViewModel.setPage(0, it) }
        settings.getPlayClueTabsPage(1) { clueTabsViewModel.setPage(1, it) }
    }

    // indicates if first run message was shown and is now cleared
    // will stay false if this is not the first run of the puzzle
    private var firstPlayCleared = false

    public val isAcrostic : Boolean
        get() {
            return getPuzzle()?.let { it.kind == Puzzle.Kind.ACROSTIC } ?: false
        }

    init {
        setupVoiceCommands()
        updateState()
    }

    /**
     * Get value of fitToScreenMode and scale saved in settings
     *
     * Separate from state as state starts with an initial default value
     * that causes issues doing the first "true" fit.
     */
    fun getInitialSizeInfo(cb : (FitToScreenMode, Float) -> Unit) {
        settings.getPlayFitToScreenMode { fitToScreenMode ->
            settings.getPlayScale { scale ->
                cb(fitToScreenMode, scale)
            }
        }
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

    fun longClickBoard(row : Int, col : Int) {
        getBoard()?.let { board ->
            val position = Position(row, col)
            board.setHighlightLetter(position)
            launchClueNotes(board.getClueID())
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
    fun hideClueTabs() {
        if (!isAcrostic)
            settings.setPlayShowClueTabs(false)
    }

    fun clickClue(cid : ClueID?, previousWord : Word? = null) {
        if (cid == null)
            return

        getBoard()?.let { board ->
            board.puzzle.getClue(cid)?.let { clue ->
                val word = previousWord ?: board.currentWord
                if (clue.hasZone() && clue.clueID != board.clueID)
                    board.jumpToClue(clue)
                displayKeyboard(word)
            }
        }
    }

    fun saveClueTabsPage(index : Int, page : Int) {
        settings.setPlayClueTabsPage(index, page)
    }

    fun deleteResponse() {
        getBoard()?.let { board ->
            board.deleteOrUndoResponse()
        }
    }

    fun playResponse(c : Char) {
        playResponse(c.toString())
    }

    fun playResponse(c : String) {
        getBoard()?.let { board ->
            board.playResponse(c)
        }
    }

    fun playSpace() {
        settings.getPlaySpaceChangesDirection { changesDir ->
            if (changesDir)
                getBoard()?.toggleSelection()
            else
                playResponse(' ')
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

        val currentID = board.getClueID()

        val unfilledClues = puz.getAllClues().filter { clue ->
            val cid = clue.getClueID()
            cid != currentID && !board.isFilledClueID(clue.clueID)
        }

        if (!unfilledClues.isEmpty()) {
            // bit inefficient, but saves a field
            val idx = Random.nextInt(unfilledClues.size)
            val oldWord = board.currentWord
            board.jumpToClue(unfilledClues[idx])
            displayKeyboard(oldWord)
        }
    }

    fun clearFirstPlayMessage() {
        firstPlayCleared = true
        currentUIState = currentUIState.copy(firstPlayMessage = null)
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

        viewModelScope.launch {
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_delete),
                { deleteResponse() },
            ))
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_toggle),
                { toggleSelection() },
            ))
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_next),
                { nextClue() }
            ))
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_previous),
                { prevClue() },
            ))
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_left),
                { moveLeft() },
            ))
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_right),
                { moveRight() },
            ))
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_up),
                { moveUp() },
            ))
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_down),
                { moveDown() },
            ))
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_clues),
                { launchClueList() },
            ))
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_jump_random),
                { pickRandomUnfilledClue() }
            ))
        }
    }

    private suspend fun getClueText() : String? {
        return getBoard()?.clue?.let {
            getClueLineClueText(it)
        }
    }

    private fun updateState() {
        viewModelScope.launch {
            getPuzzle()?.let { puz ->
                updateStateLock.withLock {
                    val clueText = getClueText()
                    val aspectRatio = if (puz.height == 0)
                        1F
                    else
                        puz.width.toFloat() / puz.height

                    val firstPlayMessage = if (
                        isFirstPlay
                        && !firstPlayCleared
                        && puz.hasIntroMessage()
                    ) {
                        puz.getIntroMessage()
                    } else {
                        null
                    }

                    currentUIState = currentUIState.copy(
                        puzzleTitle = puz.title,
                        clueText = clueText,
                        puzzleAspectRatio = aspectRatio,
                        isAcrostic = isAcrostic,
                        firstPlayMessage = firstPlayMessage,
                    )
                }
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

    private var currentUIState : PlayPageUIState
        get() { return mediatedPlayUIState.current }
        set(value) { mediatedPlayUIState.current = value }
}

