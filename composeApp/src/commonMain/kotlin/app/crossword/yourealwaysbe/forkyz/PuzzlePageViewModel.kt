
package app.crossword.yourealwaysbe.forkyz

import kotlin.text.Regex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO // needed on IOS, not JVM or Android
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_announce_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_answer
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_answer_alt
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_back
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_clear
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_current_clue_help
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_letter
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.command_number
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.feature_not_implemented
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_query_response_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.llm_query_but_no_active_network
import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.CrosswordSolverData
import app.crossword.yourealwaysbe.forkyz.exttools.DuckDuckGoData
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalDictionaries
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.exttools.FifteenSquaredData
import app.crossword.yourealwaysbe.forkyz.exttools.MCPBoardManager
import app.crossword.yourealwaysbe.forkyz.exttools.SharePuzzleData
import app.crossword.yourealwaysbe.forkyz.exttools.ShareTextData
import app.crossword.yourealwaysbe.forkyz.exttools.askLLMAgentForCurrentClue
import app.crossword.yourealwaysbe.forkyz.exttools.isLLMAgentEnabled
import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Playboard
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.PlayboardListener
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.settings.ClueListClueLine
import app.crossword.yourealwaysbe.forkyz.settings.ExternalToolSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardSettings
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.ShowErrorsMode
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer
import app.crossword.yourealwaysbe.forkyz.util.KeyboardInfo
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManagerKt
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.WordsToNumbers
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.mediate
import app.crossword.yourealwaysbe.forkyz.view.PlayboardTextRenderer
import app.crossword.yourealwaysbe.forkyz.view.PuzzleFinishedDialogViewModel
import app.crossword.yourealwaysbe.forkyz.view.PuzzleInfoDialogViewModel
import app.crossword.yourealwaysbe.forkyz.view.SpecialEntryDialogViewModel

enum class PuzzleSubMenu {
    NONE,
    MAIN,
    NOTES,
    SHOW_ERRORS,
    REVEAL,
    EXTERNAL_TOOLS,
    SHARE,
    ZOOM,
}

class ShareStream(
    val title : String,
    val mimeType : String,
    val uri : Uri,
)

class SendToast(val text : String)

data class ExternalToolsMenuState(
    val hasLLMAgent : Boolean = true,
    val hasCrosswordSolver : Boolean = true,
    val hasDuckDuckGo : Boolean = true,
    val hasFifteenSquared : Boolean = true,
    val hasExternalDictionary : Boolean = true,
    val hasMCPEnabled : Boolean = true,
    val isMCPRunning : Boolean = true,
) {
    val hasExternal : Boolean
        = hasLLMAgent
        || hasCrosswordSolver
        || hasDuckDuckGo
        || hasFifteenSquared
        || hasExternalDictionary
}

data class ShowErrorsMenuState(
    val enabled : Boolean = true,
    val mode : ShowErrorsMode = ShowErrorsMode.NONE,
) {
    val isShowing : Boolean
        get() = enabled && mode != ShowErrorsMode.NONE
    val isShowingErrorsCursor : Boolean
        get() = mode == ShowErrorsMode.CURSOR
    val isShowingErrorsClue : Boolean
        get() = mode == ShowErrorsMode.CLUE
    val isShowingErrorsGrid : Boolean
        get() = mode == ShowErrorsMode.GRID
    val isShowingErrorsWholeWord : Boolean
        get() = mode == ShowErrorsMode.WHOLE_WORD
}

data class PuzzleMenuState(
    val hasShareURL : Boolean = true,
    val hasSupportURL : Boolean = true,
    val externalToolsState : ExternalToolsMenuState = ExternalToolsMenuState(),
    val isScratchMode : Boolean = false,
    val showErrorsState : ShowErrorsMenuState = ShowErrorsMenuState(),
    val canReveal : Boolean = true,
    val canRevealInitialBoxLetter : Boolean = true,
    val canRevealInitialLetters : Boolean = true,
    val expanded : PuzzleSubMenu = PuzzleSubMenu.NONE,
)

data class VoiceState(
    val volumeActivatesVoice : Boolean = false,
    val equalsAnnounceClue : Boolean = false,
    val showVoiceButton : Boolean = false,
    val showAnnounceButton : Boolean = false,
    val alwaysAnnounceBox : Boolean = false,
    val alwaysAnnounceClue : Boolean = false,
)

data class AnnounceData(val message : CharSequence)

enum class PuzzleDialog {
    NONE,
    REVEAL_ALL,
}

data class FlagClueDialogState(
    val clueID : ClueID,
    val flagColor : Int,
)

data class FlagCellDialogState(
    val position : Position,
    val flagColor : Int,
)

data class EditClueDialogState(
    val clueID : ClueID,
    val hint : String,
)

data class DialogMessage(
    val title : String,
    val message : String,
)

data class AppNotFoundDialogState(
    val appName : String,
    val appURL : String,
)

data class PuzzlePageUIState(
    val showTimer : Boolean = false,
    val indicateShowErrors : Boolean = false,
    val showDialog : PuzzleDialog = PuzzleDialog.NONE,
)

enum class KeyboardVisibility {
    NONE,
    FORKYZ,
    NATIVE,
}

/**
 * State of keyboard
 *
 * @param visibility whether to show Forkyz, Native, or no keyboard
 * @param needsRefresh indicates if state has just changed,
 * so keyboard needs showing or hiding. Call markKeyboardRefreshed() once
 * launched.
 * @param hideOnBack whether the (Forkyz) keyboard should intepret a back gesture
 * as "close"
 * @param showHideButton whether the Forkyz keyboard should show a hide
 * button
 * @param forceCaps whether the native keyboard should convert to caps
 * @param compact whether keyboard should be compact
 * @param hapticFeedback whether to use haptic feedback on key press
 * @param layout keyboard layout to use
 * @param repeatDelay how long to hold before key repeat happens (0
 * means never)
 * @param repeatInterval how long between key repeats (0 means never)
 */
data class KeyboardState(
    val visibility : KeyboardVisibility = KeyboardVisibility.NONE,
    val needsRefresh : Boolean = false,
    val hideOnBack : Boolean = false,
    val showHideButton : Boolean = false,
    val forceCaps : Boolean = false,
    val compact : Boolean = false,
    val hapticFeedback : Boolean = false,
    val layout : KeyboardLayout = KeyboardLayout.KL_QWERTY,
    val repeatDelay : Long = 300,
    val repeatInterval : Long = 75,
)

data class ClueNotesData(
    val clueID : ClueID?
)

private data class VoiceActivateSettings(
    val volumeActivatesVoice : Boolean,
    val equalsAnnounceClue : Boolean,
    val buttonActivatesVoice : Boolean,
    val buttonAnnounceClue : Boolean,
)

private data class VoiceAlwaysSettings(
    val alwaysAnnounceBox : Boolean,
    val alwaysAnnounceClue : Boolean,
)

private data class VoiceSettings(
    val activateSettings : VoiceActivateSettings,
    val alwaysSettings : VoiceAlwaysSettings,
)

private data class PuzzleMenuSettings(
    val externalToolSettings : ExternalToolSettings,
    val scratchMode : Boolean,
    val showErrorsMode : ShowErrorsMode,
)

private data class PuzzleMenuStateParams(
    val settings : PuzzleMenuSettings,
    val isMCPRunning : Boolean,
)

private data class UIStateSettings(
    val showTimer : Boolean,
    val showErrorsMode : ShowErrorsMode,
    val indicateShowErrors : Boolean,
)

open class PuzzlePageViewModel(
    val settings : ForkyzSettings,
    val utils : NativeBackendUtils,
    val currentPuzzleHolder : CurrentPuzzleHolder,
    val fileHandlerProvider : FileHandlerProvider,
    val mcpBoardManager : MCPBoardManager,
) : ViewModel(), PlayboardListener {
    private val _announceEvent = MutableStateFlow<AnnounceData?>(null)
    val announceEvent : StateFlow<AnnounceData?> = _announceEvent

    protected val _sendToastEvent = MutableStateFlow<SendToast?>(null)
    val sendToastEvent : StateFlow<SendToast?> = _sendToastEvent

    protected val _externalToolEvent = MutableStateFlow<ExternalToolData?>(null)
    val externalToolEvent : StateFlow<ExternalToolData?> = _externalToolEvent

    protected val _openClueListEvent = MutableStateFlow<Boolean>(false)
    val openClueListEvent : StateFlow<Boolean> = _openClueListEvent

    protected val _openNotesEvent = MutableStateFlow<ClueNotesData?>(null)
    val openNotesEvent : StateFlow<ClueNotesData?> = _openNotesEvent

    protected val _backEvent = MutableStateFlow<Boolean>(false)
    val backEvent : StateFlow<Boolean> = _backEvent

    val voiceState : StateFlow<VoiceState> = mediate(
        viewModelScope,
        VoiceState(),
        { voiceSettings ->
            VoiceState(
                voiceSettings.activateSettings.volumeActivatesVoice,
                voiceSettings.activateSettings.equalsAnnounceClue,
                voiceSettings.activateSettings.buttonActivatesVoice,
                voiceSettings.activateSettings.buttonAnnounceClue,
                voiceSettings.alwaysSettings.alwaysAnnounceBox,
                voiceSettings.alwaysSettings.alwaysAnnounceClue,
            )
        },
        combine(
            combine(
                settings.liveVoiceVolumeActivatesVoice,
                settings.liveVoiceEqualsAnnounceClue,
                settings.liveVoiceButtonActivatesVoice,
                settings.liveVoiceButtonAnnounceClue,
                ::VoiceActivateSettings,
            ),
            combine(
                settings.liveVoiceAlwaysAnnounceBox,
                settings.liveVoiceAlwaysAnnounceClue,
                ::VoiceAlwaysSettings,
            ),
            ::VoiceSettings,
        ),
    )

    private val menuExpandStack : MutableList<PuzzleSubMenu> = mutableListOf()

    private val mediatedMenuState
        = MediatedStateWithFlow<PuzzleMenuState, PuzzleMenuStateParams>(
            viewModelScope,
                PuzzleMenuState(),
                this::getUpdatedMenuState,
                combine(
                    combine(
                        settings.liveExternalToolSettings,
                        settings.livePlayScratchMode,
                        settings.livePlayShowErrorsMode,
                        ::PuzzleMenuSettings,
                    ),
                    settings.liveExtMCPRunning,
                    ::PuzzleMenuStateParams,
                )
            )
    val menuState : StateFlow<PuzzleMenuState> = mediatedMenuState.stateFlow

    private val mediatedUIState
        = MediatedStateWithFlow<PuzzlePageUIState, UIStateSettings>(
            viewModelScope,
            PuzzlePageUIState(),
            { state, uiSettings ->
                val puzHasSolution = getPuzzle()?.hasSolution() ?: false
                val indicateShowErrors =
                    puzHasSolution
                    && uiSettings.indicateShowErrors
                    && uiSettings.showErrorsMode != ShowErrorsMode.NONE
                state.copy(
                    showTimer = uiSettings.showTimer,
                    indicateShowErrors = indicateShowErrors,
                )
            },
            combine(
                settings.livePlayShowTimer,
                settings.livePlayShowErrorsMode,
                settings.livePlayIndicateShowErrors,
                ::UIStateSettings,
            ),
        )
    val uiState : StateFlow<PuzzlePageUIState> = mediatedUIState.stateFlow

    private val mediatedKeyboardState
        = MediatedStateWithFlow<KeyboardState, KeyboardSettings>(
            viewModelScope,
            KeyboardState(),
            this::getUpdatedKeyboardState,
            settings.liveKeyboardSettings
        )
    val keyboardState : StateFlow<KeyboardState>
        = mediatedKeyboardState.stateFlow

    /**
     * When not null show info dialog with view model
     */
    private val _infoDialogViewModel
        = MutableStateFlow<PuzzleInfoDialogViewModel?>(null)
    val infoDialogViewModel : StateFlow<PuzzleInfoDialogViewModel?>
        = _infoDialogViewModel

    private val _finishedDialogViewModel
        = MutableStateFlow<PuzzleFinishedDialogViewModel?>(null)
    val finishedDialogViewModel : StateFlow<PuzzleFinishedDialogViewModel?>
        = _finishedDialogViewModel

    private val _flagClueDialogState
        = MutableStateFlow<FlagClueDialogState?>(null)
    val flagClueDialogState : StateFlow<FlagClueDialogState?>
        = _flagClueDialogState

    private val _editClueDialogState
        = MutableStateFlow<EditClueDialogState?>(null)
    val editClueDialogState : StateFlow<EditClueDialogState?>
        = _editClueDialogState

    private val _flagCellDialogState
        = MutableStateFlow<FlagCellDialogState?>(null)
    val flagCellDialogState : StateFlow<FlagCellDialogState?>
        = _flagCellDialogState

    private val _dialogMessage
        = MutableStateFlow<DialogMessage?>(null)
    val dialogMessage : StateFlow<DialogMessage?>
        = _dialogMessage

    private val _appNotFoundDialogState
        = MutableStateFlow<AppNotFoundDialogState?>(null)
    val appNotFoundDialogState : StateFlow<AppNotFoundDialogState?>
        = _appNotFoundDialogState

    private val _specialEntryDialogViewModel
        = MutableStateFlow<SpecialEntryDialogViewModel?>(null)
    val specialEntryDialogViewModel : StateFlow<SpecialEntryDialogViewModel?>
        = _specialEntryDialogViewModel

    private val keyboardManager = KeyboardManagerKt(
        settings,
        this::keyboardManagerShow,
        this::keyboardManagerHide,
    )

    var timer : ImaginaryTimer? = null
        private set
    val puzzleHasInitialValues : Boolean by lazy {
        getPuzzle()?.hasInitialValueCells() ?: false
    }
    val isFirstPlay : Boolean
        get() = getPuzzle()?.time == 0.toLong()
    val canResume : Boolean
        get() { return getPuzzle() != null }
    val isTimerRunning : Boolean
        get() { return timer != null }
    val supportURL : String?
        get() { return getPuzzle()?.supportUrl }
    val shareURL : String?
        get() { return getPuzzle()?.shareUrl }

    private val voiceCommandDispatcher = VoiceCommands()

    // needed public to send to non-compose views
    fun getBoard() : Playboard? = currentPuzzleHolder.board
    fun getPuzzle() : Puzzle? = getBoard()?.puzzle

    fun saveBoard() {
        currentPuzzleHolder.saveBoard()
    }

    init {
        // keep listener active so model is up to date with changes that happen
        // while paused
        getBoard()?.addListener(this)
    }

    fun markKeyboardRefreshed() {
        currentKeyboardState = currentKeyboardState.copy(
            needsRefresh = false,
        )
    }

    fun startTimer() {
        val puz = getPuzzle()
        if (puz != null && puz.percentComplete != 100) {
                val time = puz.time
                timer = ImaginaryTimer(time);
                timer?.start();
        }
    }

    // need to do default arguments the Java way while
    // PuzzlePage.java still used
    fun stopTimer() {
        stopTimer(false)
    }

    /**
     * Stop the timer running
     *
     * Updates puzzle time if it wasn't already finished or if
     * forceOverwriteTime is true (used when 100% hit for first time).
     */
    fun stopTimer(forceOverwriteTime : Boolean) {
        getPuzzle()?.let { puz ->
            timer?.let { timer ->
                timer.stop();
                if (forceOverwriteTime || puz.percentComplete != 100) {
                    puz.time = timer.getElapsed()
                }
            }
        }
        timer = null
    }

    fun toggleShowErrorsMode(mode : ShowErrorsMode) {
        getBoard()?.let { board ->
            val newMode = if (board.getShowErrorsMode() == mode)
                ShowErrorsMode.NONE
            else
                mode
            settings.setPlayShowErrorsMode(newMode)
        }
    }

    fun toggleClueFlag() {
        getBoard()?.clue?.let { clue ->
            if (clue.isFlagged)
                getBoard()?.flagClue(clue, false);
            else
                showFlagClueDialog(clue.clueID)
        }
    }

    fun flagClue(clueID : ClueID, color : Int) {
        getBoard()?.let { board ->
            board.flagClue(clueID, true)
            board.setFlagColor(clueID, color)
        }
        clearFlagClueDialog()
    }

    fun clearFlagClueDialog() {
        _flagClueDialogState.value = null
    }

    fun toggleCellFlag() {
        getBoard()?.let { board ->
            getPuzzle()?.let { puz ->
                board.highlightLetter.let { pos ->
                    puz.checkedGetBox(pos)?.let { box ->
                        if (box.isFlagged)
                            board.flagPosition(pos, false)
                        else
                            showFlagCellDialog(pos)
                    }
                }
            }
        }
    }

    fun flagCell(position : Position, color : Int) {
        getBoard()?.let { board ->
            board.flagPosition(position, true)
            board.setFlagColor(position, color)
        }
        clearFlagCellDialog()
    }

    fun clearFlagCellDialog() {
        _flagCellDialogState.value = null
    }


    fun revealInitialLetter() {
        getBoard()?.revealInitialLetter()
    }

    fun revealLetter() {
        getBoard()?.revealLetter()
    }

    fun revealWord() {
        getBoard()?.revealWord()
    }

    fun revealInitialLetters() {
        getBoard()?.revealInitialLetters()
    }

    fun revealErrors() {
        getBoard()?.revealErrors()
    }

    fun revealPuzzle() {
        showDialog(PuzzleDialog.REVEAL_ALL)
    }

    fun confirmRevealPuzzle() {
        clearRevealDialog()
        getBoard()?.revealPuzzle()
    }

    fun clearRevealDialog() {
        clearDialog(PuzzleDialog.REVEAL_ALL)
    }

    fun toggleScratchMode() {
        settings.getPlayScratchMode() { settings.setPlayScratchMode(!it); }
    }

    fun requestHelpForCurrentClue() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!utils.hasNetworkConnection()) {
                sendToast(
                    utils.getString(Res.string.llm_query_but_no_active_network)
                )
            } else {
                getBoard()?.let { board ->
                    val responseTitle = utils.getString(
                        Res.string.help_query_response_title,
                    )
                    val response = askLLMAgentForCurrentClue(
                        utils,
                        settings,
                        board,
                    )
                    sendDialogMessage(responseTitle, response)
                }
            }
        }
    }

    fun clearDialogMessage() {
        _dialogMessage.value = null
    }

    fun handleAppNotFoundException(e : AppNotFoundException) {
        _appNotFoundDialogState.value = AppNotFoundDialogState(
            e.appName,
            e.appURL,
        )
    }

    fun clearAppNotFoundDialog() {
        _appNotFoundDialogState.value = null
    }

    fun shareClue(withResponse : Boolean) {
        viewModelScope.launch {
            getBoard()?.let { board ->
                board.clue?.let { clue ->
                    ShareTextData.buildShareClue(
                        utils,
                        settings,
                        board,
                        clue,
                        withResponse,
                    )?.let { _externalToolEvent.value = it }
                }
            }
        }
    }

    fun sharePuzzle(writeBlank : Boolean, omitExtensions : Boolean) {
        viewModelScope.launch {
            if (utils.isShareFileAvailable) {
                getPuzzle()?.let { puz ->
                    SharePuzzleData.build(
                        viewModelScope,
                        utils,
                        puz,
                        writeBlank,
                        omitExtensions,
                        { _externalToolEvent.value = it },
                    )
                }
            } else {
                sendToast(utils.getString(Res.string.feature_not_implemented))
            }
        }
    }

    fun shareCompletion() {
        viewModelScope.launch {
            getPuzzle()?.let { puz ->
                _externalToolEvent.value = ShareTextData.buildShareCompletion(
                    utils,
                    puz,
                )
            }
        }
    }

    /**
     * Handles user request to edit currently selected clue
     */
    fun editClue() {
        getBoard()?.clueID?.let { editClue(it) }
    }

    fun editClue(cid : ClueID?) {
        cid?.let { cid ->
            getPuzzle()?.getClue(cid)?.let { clue ->
                _editClueDialogState.value = EditClueDialogState(cid, clue.hint)
            }
        }
    }

    fun confirmEditClue(cid : ClueID, hint : String) {
        clearEditClueDialog()
        getBoard()?.let { it.editClue(cid, hint) }
    }

    fun clearEditClueDialog() {
        _editClueDialogState.value = null
    }

    fun launchClueList() {
        _openClueListEvent.value = true
    }

    fun clearOpenClueListEvent() {
        _openClueListEvent.value = false
    }

    fun launchCurrentClueNotes() {
        getBoard()?.let { board ->
            launchClueNotes(board.clueID)
        }
    }

    fun launchClueNotes(clue : Clue?) {
        launchClueNotes(clue?.clueID)
    }

    fun launchClueNotes(cid : ClueID?) {
        if (cid == null) {
            launchPuzzleNotes()
        } else {
            getBoard()?.let { board ->
                if (cid != board.clueID)
                    board.jumpToClue(cid)
            }
            _openNotesEvent.value = ClueNotesData(cid)
        }
    }

    fun launchPuzzleNotes() {
        _openNotesEvent.value = ClueNotesData(null)
    }

    fun doBack() {
        _backEvent.value = true
    }

    fun clearBackEvent() {
        _backEvent.value = false
    }

    fun dispatchVoiceCommand(text : List<String>?) {
        viewModelScope.launch {
            val useLLM = settings.liveVoiceLLMVoiceCommands.first()
            if (useLLM && !utils.hasNetworkConnection()) {
                sendToast(
                    utils.getString(Res.string.llm_query_but_no_active_network)
                )
            }
            // try anyway, falls back to basic version
            getPuzzle()?.let { puz ->
                text?.let { commandAlternatives ->
                    viewModelScope.launch(Dispatchers.IO) {
                        voiceCommandDispatcher.dispatch(
                            settings,
                            utils,
                            commandAlternatives,
                            puz,
                        )
                    }
                }
            }
        }
    }

    // public for now to give a route through puzzle activity to
    // register voice commands
    fun registerVoiceCommand(command : VoiceCommand) {
        voiceCommandDispatcher.registerVoiceCommand(command)
    }

    /**
     * General click board action
     *
     * @param fixClue don't allow current clue to change because of
     * click
     */
    fun clickBoard(row : Int, col : Int, fixClue : Boolean = false) {
        // only show keyboard if double click a word
        // hide if it's a new word
        getBoard()?.let { board ->
            val newPos = Position(row, col)
            val clueID = if (fixClue) board.clueID else null
            val previousWord = board.setHighlightLetter(newPos, clueID)
            if (previousWord.checkInWord(newPos.row, newPos.col)) {
                showKeyboard()
            } else {
                hideKeyboard()
            }
        }
    }

    protected fun registerVoiceCommandBack() {
        viewModelScope.launch {
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(Res.string.command_back),
                    { _ -> doBack() }
                )
            )
        }
    }

    /**
     * Voice command launch notes current clue
     */
    protected fun registerVoiceCommandNotes() {
        viewModelScope.launch {
            registerVoiceCommand(VoiceCommand(
                utils.getString(Res.string.command_notes),
                { launchCurrentClueNotes() },
            ))
        }
    }

    /**
     * Prepared command for inputting word answers
     */
    // public for now to give a route through puzzle activity
    fun registerVoiceCommandAnswer() {
        viewModelScope.launch {
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(Res.string.command_answer),
                    utils.getString(Res.string.command_answer_alt),
                    { answer ->
                        // remove non-word as not usually entered into grids
                        val prepped = answer.replace("\\W+".toRegex(), "")
                            .uppercase()
                        getBoard()?.playAnswer(prepped)
                    }
                )
            )
        }
    }

    fun callCrosswordSolver() {
        val zone = getBoard()?.currentZone
        if (zone == null)
            return

        val puz = getPuzzle()
        if (puz == null)
            return

        _externalToolEvent.value = CrosswordSolverData.buildMissingLetters(
            puz,
            zone,
        )
    }

    fun searchDuckDuckGo() {
        viewModelScope.launch {
            getBoard()?.clue
                ?.let { DuckDuckGoData.build(utils, it) }
                ?.let { _externalToolEvent.value = it }
            }
    }

    fun searchFifteenSquared() {
        viewModelScope.launch {
            getPuzzle()?.let {
                FifteenSquaredData.build(utils, it)
            }?.let { _externalToolEvent.value = it }
        }
    }

    fun callExternalDictionary() {
        getBoard()?.let { board ->
            ExternalDictionaries.build(
                settings,
                board,
                { _externalToolEvent.value = it },
            )
        }
    }

    fun announceClue() {
        viewModelScope.launch {
            getBoard()?.let { board ->
                val showCount = settings.livePlayShowCount.first()
                PlayboardTextRenderer.getAccessibleCurrentClueWord(
                    utils, board, showCount
                )?.let {
                    announce(AnnounceData(it))
                }
            }
        }
    }

   fun announceBox() {
        viewModelScope.launch {
            getBoard()?.let { board ->
                PlayboardTextRenderer.getAccessibleCurrentBox(
                    utils,
                    board,
                )?.let {
                    announce(AnnounceData(it))
                }
            }
        }
    }

    /**
     * Returns a text description of the current clue and word state,
     * or null if there is no current clue.
     */
    suspend fun getCurrentClueInfo() : String? {
        val board = getBoard() ?: return null
        val showCount = settings.livePlayShowCount.first()
        return PlayboardTextRenderer.getAccessibleCurrentClueWord(
            utils,
            board,
            showCount,
        )?.toString()
    }

    fun expandMenu(menu : PuzzleSubMenu) {
        menuExpandStack.add(menu)
        currentMenuState = currentMenuState.copy(expanded = menu)
    }

    /**
     * Close most recently opened menu and return to previous
     */
    fun closeMenu() {
        val state = currentMenuState
        if (menuExpandStack.isEmpty()) {
            currentMenuState = state.copy(expanded = PuzzleSubMenu.NONE)
        } else {
            menuExpandStack.removeAt(menuExpandStack.size - 1)
            currentMenuState = state.copy(expanded = menuExpandStack.last())
        }
    }

    /**
     * Dismiss menu completely
     */
    fun dismissMenu() {
        menuExpandStack.clear()
        currentMenuState = currentMenuState.copy(
            expanded = PuzzleSubMenu.NONE,
        )
    }

    fun clearAnnounceEvent() {
        _announceEvent.value = null
    }

    fun clearOpenNotesEvent() {
        _openNotesEvent.value = null
    }

    fun clearExternalToolEvent() {
        _externalToolEvent.value = null
    }

    fun clearSendToast() {
        _sendToastEvent.value = null
    }

    fun selectClue(clue : Clue?) {
        selectClue(clue?.clueID)
    }

    fun selectClue(cid : ClueID?) {
        getBoard()?.let { board ->
            cid?.let { cid ->
                if (cid != board.clueID)
                    board.jumpToClue(cid)
            }
        }
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        handleChangeTimer()
        handleChangeAccessibility(changes)
        handleChangeMenu(changes)
    }

    override protected fun onCleared() {
        getBoard()?.removeListener(this)
    }

    fun keyboardPushBlockHide() {
        keyboardManager.pushBlockHide()
    }

    fun keyboardPopBlockHide() {
        keyboardManager.popBlockHide()
    }

    fun showKeyboard() {
        keyboardManager.showKeyboard(this::keyboardManagerShow)
    }

    fun hideKeyboard(force : Boolean = false) {
        keyboardManager.hideKeyboard(
            this::keyboardManagerHide,
            force,
        )
    }

    fun onFocusNativeView(gainFocus : Boolean) {
        keyboardManager.onFocusNativeView(
            gainFocus,
            this::keyboardManagerHide,
        )
    }

    fun showInfoDialog() {
        _infoDialogViewModel.value = PuzzleInfoDialogViewModel(
            viewModelScope,
            settings,
            currentPuzzleHolder,
            fileHandlerProvider,
            timer,
        )
    }

    fun closePuzzleInfoDialog() {
        _infoDialogViewModel.value = null
    }

    fun showFinishedDialog() {
        _finishedDialogViewModel.value = PuzzleFinishedDialogViewModel(
            viewModelScope,
            settings,
            currentPuzzleHolder,
        )
    }

    fun closePuzzleFinishedDialog() {
        _finishedDialogViewModel.value = null
    }

    fun doSpecialEntry() {
        _specialEntryDialogViewModel.value = SpecialEntryDialogViewModel(
            viewModelScope,
            settings,
            currentPuzzleHolder,
        )
    }

    fun clearSpecialEntry() {
        _specialEntryDialogViewModel.value = null
    }

    fun startMCP() {
        mcpBoardManager.start()
    }

    fun stopMCP() {
        mcpBoardManager.stop()
    }

    /**
     * Prepared command for inputting letters
     */
    protected fun registerVoiceCommandLetter() {
        viewModelScope.launch {
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(Res.string.command_letter),
                    { letter ->
                        if (!letter.isEmpty()) {
                            getBoard()?.playLetter(letter[0].uppercase())
                        }
                    },
                )
            )
        }
    }

    /**
     * Prepared command for jumping to clue number
     */
    protected fun registerVoiceCommandNumber() {
        viewModelScope.launch {
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(Res.string.command_number),
                    { textNumber ->
                        val prepped = WordsToNumbers
                            .convertTextualNumbersInDocument(textNumber)
                        try {
                            val number = prepped.toInt()
                            getBoard()?.jumpToClue(number.toString())
                        } catch (e : NumberFormatException) {
                            getBoard()?.jumpToClue(textNumber)
                        }
                    },
                )
            )
        }
    }

    /**
     * Prepared command for clearing current word
     */
    protected fun registerVoiceCommandClear() {
        viewModelScope.launch {
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(Res.string.command_clear),
                    { getBoard()?.clearWord() },
                )
            )
        }
    }

    /**
     * Prepared command for announcing current clue
     */
    protected fun registerVoiceCommandAnnounceClue() {
        viewModelScope.launch {
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(Res.string.command_announce_clue),
                    { announceClue() },
                )
            )
        }
    }

    /**
     * Prepared command for requesting help
     */
    protected fun registerVoiceCommandClueHelp() {
        viewModelScope.launch {
            registerVoiceCommand(
                VoiceCommand(
                    utils.getString(Res.string.command_current_clue_help),
                    { requestHelpForCurrentClue() }
                )
            )
        }
    }

    protected suspend fun getClueLineClueText(clue : Clue) : String {
        val showCount = settings.livePlayShowCount.first()
        val clueListClueLine
            = settings.livePlayClueListNameInClueLine.first()
        val text = when (clueListClueLine) {
            ClueListClueLine.CLCL_ABBREVIATED -> {
                PlayboardTextRenderer.getShortClueText(
                    utils,
                    clue,
                    true,
                    true,
                    showCount
                )
            }
            ClueListClueLine.CLCL_NONE -> {
                PlayboardTextRenderer.getShortClueText(
                    utils,
                    clue,
                    false,
                    false,
                    showCount
                )
            }
            else -> {
                PlayboardTextRenderer.getLongClueText(
                    utils,
                    clue,
                    showCount
                )
            }
        }
        return text
    }

    protected fun showFlagClueDialog(clueID : ClueID) {
        getBoard()?.let { board ->
            board.puzzle.getClue(clueID)?.let { clue ->
                _flagClueDialogState.value = FlagClueDialogState(
                    clueID,
                    clue.flagColor,
                )
            }
        }
    }

    protected fun showFlagCellDialog(position : Position) {
        getPuzzle()?.checkedGetBox(position)?.let { box ->
            _flagCellDialogState.value = FlagCellDialogState(
                position,
                box.flagColor,
            )
        }
    }

    private fun keyboardManagerShow(info : KeyboardInfo) {
        val visibility = if (info.useNative)
            KeyboardVisibility.NATIVE
        else
            KeyboardVisibility.FORKYZ
        currentKeyboardState = currentKeyboardState.copy(
            visibility = visibility,
            needsRefresh = true,
            hideOnBack = info.hideOnBack,
            showHideButton = info.showHideButton,
        )
    }

    private fun keyboardManagerHide() {
        currentKeyboardState = currentKeyboardState.copy(
            visibility = KeyboardVisibility.NONE,
            needsRefresh = true,
            hideOnBack = false,
        )
    }

    private fun sendToast(text : String) {
        _sendToastEvent.value = SendToast(text)
    }

    private fun getUpdatedMenuState(
        state : PuzzleMenuState,
        params : PuzzleMenuStateParams,
    ) : PuzzleMenuState {
        val board = getBoard()
        val puz = getPuzzle()

        val menuSettings = params.settings

        val hasShareURL = puz?.shareUrl != null
        val hasSupportURL = puz?.supportUrl != null
        val canReveal = puz?.hasSolution() ?: false

        val box = board?.currentBox

        val canRevealInitialLetters = puz?.hasInitialValueCells() ?: false
        val canRevealInitialBoxLetter
            = !Box.isBlock(box) && box?.hasInitialValue() ?: false

        val extSettings = menuSettings.externalToolSettings
        val externalToolsState = ExternalToolsMenuState(
            extSettings.hasLLMAgent,
            extSettings.crosswordSolverEnabled,
            extSettings.duckDuckGoEnabled,
            extSettings.fifteenSquaredEnabled,
            extSettings.hasExternalDictionary,
            extSettings.mcpHostPort.isNotEmpty(),
            params.isMCPRunning,
        )

        val isScratchMode = menuSettings.scratchMode

        val showErrorsState = ShowErrorsMenuState(
            enabled = canReveal,
            mode = menuSettings.showErrorsMode,
        )

        val expanded = state.expanded

        return PuzzleMenuState(
            hasShareURL,
            hasSupportURL,
            externalToolsState,
            isScratchMode,
            showErrorsState,
            canReveal,
            canRevealInitialBoxLetter,
            canRevealInitialLetters,
            expanded,
        )
    }

    private fun handleChangeTimer() {
        val puz = getPuzzle()
        if (puz != null && puz.percentComplete == 100) {
            if (isTimerRunning) {
                stopTimer(true)
                showFinishedDialog()
            }
        }
    }

    private fun announce(data : AnnounceData) {
        _announceEvent.value = data
    }

    private fun handleChangeAccessibility(changes : PlayboardChanges) {
        settings.getVoiceAlwaysAnnounceBox() { announceBox ->
        settings.getVoiceAlwaysAnnounceClue() { announceClue ->
            val board = getBoard()

            if (board != null && (announceClue || announceBox)) {
                val newPos = board.highlightLetter
                val isNewWord
                    = changes.previousWord != changes.currentWord
                val isNewPosition = changes.previousPosition != newPos

                if (isNewWord && announceClue)
                    announceClue()
                else if (isNewPosition && announceBox)
                    announceBox()
            }
        }}
    }

    private fun handleChangeMenu(changes : PlayboardChanges) {
        getBoard()?.let { board ->
            val box = board.currentBox
            val canRevealInitialBoxLetter
                = !Box.isBlock(box) && box?.hasInitialValue() ?: false

            currentMenuState = currentMenuState.copy(
                canRevealInitialBoxLetter = canRevealInitialBoxLetter,
            )
        }
    }

    private fun getUpdatedKeyboardState(
        state : KeyboardState,
        ks : KeyboardSettings,
    ) : KeyboardState {
        return state.copy(
            forceCaps = ks.forceCaps,
            compact = ks.compact,
            hapticFeedback = ks.haptic,
            layout = ks.layout,
            repeatDelay = ks.repeatDelay.toLong(),
            repeatInterval = ks.repeatInterval.toLong(),
        )
    }

    private fun showDialog(dialog : PuzzleDialog) {
        currentUIState = currentUIState.copy(
            showDialog = dialog,
        )
    }

    private fun clearDialog(dialog : PuzzleDialog) {
        if (currentUIState.showDialog == dialog) {
            currentUIState = currentUIState.copy(
                showDialog = PuzzleDialog.NONE,
            )
        }
    }

    private fun sendDialogMessage(
        title : String,
        message : String,
    ) {
        _dialogMessage.value = DialogMessage(title, message)
    }

    private var currentMenuState : PuzzleMenuState
        get() { return mediatedMenuState.current }
        set(value) { mediatedMenuState.current = value }

    private var currentUIState : PuzzlePageUIState
        get() { return mediatedUIState.current }
        set(value) { mediatedUIState.current = value }

    private var currentKeyboardState : KeyboardState
        get() { return mediatedKeyboardState.current }
        set(value) { mediatedKeyboardState.current = value }
}

