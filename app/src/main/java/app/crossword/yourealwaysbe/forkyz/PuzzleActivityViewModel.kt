
package app.crossword.yourealwaysbe.forkyz

import java.io.IOException
import java.util.Locale
import java.util.Deque
import javax.inject.Inject
import kotlin.text.Regex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import android.app.Application
import android.content.Context
import android.net.Uri
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel

import org.jg.wordstonumbers.WordsToNumbersUtil

import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.CrosswordSolverData
import app.crossword.yourealwaysbe.forkyz.exttools.DuckDuckGoData
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalDictionaries
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.exttools.FifteenSquaredData
import app.crossword.yourealwaysbe.forkyz.exttools.SharePuzzleData
import app.crossword.yourealwaysbe.forkyz.exttools.ShareTextData
import app.crossword.yourealwaysbe.forkyz.exttools.askChatGPTForCurrentClue
import app.crossword.yourealwaysbe.forkyz.exttools.isChatGPTEnabled
import app.crossword.yourealwaysbe.forkyz.inttools.ClueListData
import app.crossword.yourealwaysbe.forkyz.inttools.ClueNotesData
import app.crossword.yourealwaysbe.forkyz.inttools.InternalToolData
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer
import app.crossword.yourealwaysbe.forkyz.util.KeyboardInfo
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManagerKt
import app.crossword.yourealwaysbe.forkyz.util.SingleLiveEvent
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared
import app.crossword.yourealwaysbe.forkyz.util.liveDataFanIn
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.forkyz.view.PlayboardTextRenderer
import app.crossword.yourealwaysbe.forkyz.view.PuzzleFinishedDialogViewModel
import app.crossword.yourealwaysbe.forkyz.view.PuzzleInfoDialogViewModel
import app.crossword.yourealwaysbe.forkyz.view.SpecialEntryDialogViewModel
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardListener
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle

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
    val hasChatGPT : Boolean = true,
    val hasCrosswordSolver : Boolean = true,
    val hasDuckDuckGo : Boolean = true,
    val hasFifteenSquared : Boolean = true,
    val hasExternalDictionary : Boolean = true,
) {
    val hasExternal : Boolean
        = hasChatGPT
        || hasCrosswordSolver
        || hasDuckDuckGo
        || hasFifteenSquared
        || hasExternalDictionary
}

data class ShowErrorsMenuState(
    val enabled : Boolean = true,
    val isShowingErrorsClue : Boolean = false,
    val isShowingErrorsCursor : Boolean = false,
    val isShowingErrorsGrid : Boolean = false,
) {
    val isShowing : Boolean
        get() = enabled && (
            isShowingErrorsGrid || isShowingErrorsCursor || isShowingErrorsClue
        )
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
    val volumeActivatesVoice : Boolean,
    val equalsAnnounceClue : Boolean,
    val showVoiceButton : Boolean,
    val showAnnounceButton : Boolean,
    val alwaysAnnounceBox : Boolean,
    val alwaysAnnounceClue : Boolean,
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

data class PuzzleActivityUIState(
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

// will lose this tag and become an open base class
@HiltViewModel
open class PuzzleActivityViewModel @Inject constructor(
    application : Application,
    val settings : ForkyzSettings,
    val currentPuzzleHolder : CurrentPuzzleHolder,
    val fileHandlerProvider : FileHandlerProvider,
    val utils : AndroidVersionUtils,
) : AndroidViewModel(application), PlayboardListener {
    val announceEvent = SingleLiveEvent<AnnounceData?>()
    val sendToastEvent = SingleLiveEvent<SendToast?>()
    val externalToolEvent = SingleLiveEvent<ExternalToolData?>()
    val internalToolEvent = SingleLiveEvent<InternalToolData?>()
    val backEvent = SingleLiveEvent<Boolean>()

    val voiceState : LiveData<VoiceState> = liveDataFanIn(
        {
            VoiceState(
                settings.liveVoiceVolumeActivatesVoice.value ?: false,
                settings.liveVoiceEqualsAnnounceClue.value ?: false,
                settings.liveVoiceButtonActivatesVoice.value ?: false,
                settings.liveVoiceButtonAnnounceClue.value ?: false,
                settings.liveVoiceAlwaysAnnounceBox.value ?: false,
                settings.liveVoiceAlwaysAnnounceClue.value ?: false,
            )
        },
        settings.liveVoiceVolumeActivatesVoice,
        settings.liveVoiceEqualsAnnounceClue,
        settings.liveVoiceButtonActivatesVoice,
        settings.liveVoiceButtonAnnounceClue,
        settings.liveVoiceAlwaysAnnounceBox,
        settings.liveVoiceAlwaysAnnounceClue,
    )

    private val menuExpandStack : MutableList<PuzzleSubMenu> = mutableListOf()
    private val _menuState : MutableLiveData<PuzzleMenuState> = liveDataFanIn(
        this::getMenuState,
        settings.liveExternalToolSettings,
        settings.livePlayScratchMode,
        settings.livePlayShowErrorsClue,
        settings.livePlayShowErrorsCursor,
        settings.livePlayShowErrorsGrid,
    )
    val menuState : LiveData<PuzzleMenuState> = _menuState

    private val _uiState : MutableLiveData<PuzzleActivityUIState>
        = liveDataFanIn(
            {
                val puzHasSolution = getPuzzle()?.hasSolution() ?: false
                val indicateShowErrors =
                    puzHasSolution
                    && settings.livePlayIndicateShowErrors.value ?: false
                    && (
                        settings.livePlayShowErrorsClue.value ?: false
                        || settings.livePlayShowErrorsCursor.value ?: false
                        || settings.livePlayShowErrorsGrid.value ?: false
                    )
                PuzzleActivityUIState(
                    settings.livePlayShowTimer.value ?: false,
                    indicateShowErrors,
                )
            },
            settings.livePlayShowTimer,
            settings.livePlayIndicateShowErrors,
            settings.livePlayShowErrorsClue,
            settings.livePlayShowErrorsCursor,
            settings.livePlayShowErrorsGrid,
        )
    val uiState : LiveData<PuzzleActivityUIState> = _uiState

    private val _keyboardState = liveDataFanIn(
        { getUpdatedKeyboardState() },
        settings.liveKeyboardSettings
    )
    val keyboardState : LiveData<KeyboardState> = _keyboardState

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
        get() = getPuzzle()?.getTime() == 0.toLong()
    val canResume : Boolean
        get() { return getPuzzle() != null }
    val isTimerRunning : Boolean
        get() { return timer != null }
    val supportURL : String?
        get() { return getPuzzle()?.getSupportUrl() }
    val shareURL : String?
        get() { return getPuzzle()?.getShareUrl() }

    private val voiceCommandDispatcher = VoiceCommands()

    // needed public to send to non-compose views
    fun getBoard() : Playboard? = currentPuzzleHolder.board
    fun getPuzzle() : Puzzle? = getBoard()?.getPuzzle()

    fun saveBoard() {
        currentPuzzleHolder.saveBoard()
    }

    init {
        // keep listener active so model is up to date with changes that happen
        // while paused
        getBoard()?.addListener(this)
    }

    fun markKeyboardRefreshed() {
        _keyboardState.value = keyboardState.value?.copy(
            needsRefresh = false,
        )
    }

    fun startTimer() {
        val puz = getPuzzle()
        if (puz != null && puz.getPercentComplete() != 100) {
                val time = puz.getTime()
                timer = ImaginaryTimer(time);
                timer?.start();
        }
    }

    // need to do default arguments the Java way while
    // PuzzleActivity.java still used
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
                if (forceOverwriteTime || puz.getPercentComplete() != 100) {
                    puz.setTime(timer.getElapsed());
                }
            }
        }
        timer = null
    }

    fun toggleShowErrorsClue() {
        getBoard()?.let { board ->
            settings.setPlayShowErrorsClue(!board.isShowErrorsClue())
        }
    }

    fun toggleClueFlag() {
        getBoard()?.getClue()?.let { clue ->
            if (clue.isFlagged())
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
                board.getHighlightLetter()?.let { pos ->
                    puz.checkedGetBox(pos)?.let { box ->
                        if (box.isFlagged())
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

    fun toggleShowErrorsCursor() {
        getBoard()?.let { board ->
            settings.setPlayShowErrorsCursor(!board.isShowErrorsCursor())
        }
    }

    fun toggleShowErrorsGrid() {
        getBoard()?.let { board ->
            settings.setPlayShowErrorsGrid(!board.isShowErrorsGrid())
        }
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
        val app : Application = getApplication()

        if (!utils.hasNetworkConnection(getApplication())) {
            sendToast(app.getString(R.string.help_query_but_no_active_network))
        } else {
            getBoard()?.let { board ->
                askChatGPTForCurrentClue(
                    app,
                    settings,
                    board,
                ) { response ->
                    val app : Application = getApplication()
                    sendDialogMessage(
                        app.getString(R.string.help_query_response_title),
                        response,
                    )
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
        getBoard()?.let { board ->
            board.getClue()?.let { clue ->
                ShareTextData.buildShareClue(
                    getApplication(),
                    settings,
                    board,
                    clue,
                    withResponse,
                    externalToolEvent::postValue,
                )
            }
        }
    }

    fun sharePuzzle(writeBlank : Boolean, omitExtensions : Boolean) {
        getPuzzle()?.let { puz ->
            SharePuzzleData.build(
                getApplication(),
                puz,
                writeBlank,
                omitExtensions,
                externalToolEvent::postValue,
            )
        }
    }

    fun shareCompletion() {
        getPuzzle()?.let { puz ->
            externalToolEvent.value = ShareTextData.buildShareCompletion(
                getApplication(),
                puz,
            )
        }
    }

    /**
     * Handles user request to edit currently selected clue
     */
    fun editClue() {
        getBoard()?.getClueID()?.let { editClue(it) }
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

    fun launchCurrentClueNotes() {
        getBoard()?.let { board ->
            launchClueNotes(board.getClueID())
        }
    }

    fun launchClueNotes(clue : Clue?) {
        launchClueNotes(clue?.getClueID())
    }

    fun launchClueNotes(cid : ClueID?) {
        if (cid == null) {
            launchPuzzleNotes()
        } else {
            getBoard()?.jumpToClue(cid)
            internalToolEvent.postValue(
                ClueNotesData.buildClueNotes(cid)
            )
        }
    }

    fun launchPuzzleNotes() {
        internalToolEvent.postValue(ClueNotesData.buildPuzzleNotes())
    }

    fun launchClueList() {
        internalToolEvent.postValue(ClueListData.build())
    }

    fun doBack() {
        backEvent.postValue(true)
    }

    fun clearBackEvent() {
        backEvent.postValue(false)
    }

    fun dispatchVoiceCommand(text : List<String>?) {
        text?.let { voiceCommandDispatcher.dispatch(it) }
    }

    // public for now to give a route through puzzle activity to
    // register voice commands
    fun registerVoiceCommand(command : VoiceCommand) {
        voiceCommandDispatcher.registerVoiceCommand(command)
    }

    protected fun registerVoiceCommandBack() {
        val app : Application = getApplication()
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_back),
                { _ -> doBack() }
            )
        )
    }

    /**
     * Voice command launch notes current clue
     */
    protected fun registerVoiceCommandNotes() {
        val app : Application = getApplication()
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_notes),
            { launchCurrentClueNotes() },
        ))
    }

    /**
     * Prepared command for inputting word answers
     */
    // public for now to give a route through puzzle activity
    fun registerVoiceCommandAnswer() {
        val app : Application = getApplication()
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_answer),
                app.getString(R.string.command_answer_alt),
                { answer ->
                    // remove non-word as not usually entered into grids
                    val prepped = answer.replace("\\W+".toRegex(), "")
                        .uppercase(Locale.getDefault())
                    getBoard()?.playAnswer(prepped)
                }
            )
        )
    }

    fun callCrosswordSolver() {
        val zone = getBoard()?.getCurrentZone()
        if (zone == null)
            return

        val puz = getPuzzle()
        if (puz == null)
            return

        externalToolEvent.postValue(
            CrosswordSolverData.buildMissingLetters(puz, zone)
        )
    }

    fun searchDuckDuckGo() {
        getBoard()?.getClue()
            ?.let { DuckDuckGoData.build(getApplication(), it) }
            ?.let { externalToolEvent.postValue(it) }
    }

    fun searchFifteenSquared() {
        getPuzzle()?.let {
            FifteenSquaredData.build(getApplication(), it)
        }?.let { externalToolEvent.postValue(it) }
    }

    fun callExternalDictionary() {
        getBoard()?.let { board ->
            ExternalDictionaries.build(
                settings,
                board,
                externalToolEvent::postValue
            )
        }
    }

    fun announceClue(onlyIfAccessibilityService : Boolean = false) {
        getBoard()?.let { board ->
            settings.getPlayShowCount() { showCount ->
                PlayboardTextRenderer.getAccessibleCurrentClueWord(
                    getApplication(), board, showCount
                )?.let {
                    announce(AnnounceData(it), onlyIfAccessibilityService)
                }
            }
        }
    }

   fun announceBox(onlyIfAccessibilityService : Boolean) {
        getBoard()?.let { board ->
            PlayboardTextRenderer.getAccessibleCurrentBox(
                getApplication(),
                board,
            )?.let {
                announce(AnnounceData(it), onlyIfAccessibilityService)
            }
        }
    }

    fun expandMenu(menu : PuzzleSubMenu) {
        menuExpandStack.add(menu)
        val state = menuState.value ?: getMenuState()
        _menuState.postValue(state.copy(expanded = menu))
    }

    /**
     * Close most recently opened menu and return to previous
     */
    fun closeMenu() {
         val state = menuState.value ?: getMenuState()
        if (menuExpandStack.isEmpty()) {
            _menuState.postValue(state.copy(expanded = PuzzleSubMenu.NONE))
        } else {
            menuExpandStack.removeAt(menuExpandStack.size - 1)
            _menuState.postValue(state.copy(expanded = menuExpandStack.last()))
        }
    }

    /**
     * Dismiss menu completely
     */
    fun dismissMenu() {
        menuExpandStack.clear()
         val state = menuState.value ?: getMenuState()
        _menuState.postValue(state.copy(expanded = PuzzleSubMenu.NONE))
    }

    fun clearAnnounceEvent() {
        announceEvent.postValue(null)
    }

    fun clearInternalToolEvent() {
        internalToolEvent.postValue(null)
    }

    fun clearExternalToolEvent() {
        externalToolEvent.postValue(null)
    }

    fun clearSendToast() {
        sendToastEvent.postValue(null)
    }

    fun selectClue(clue : Clue?) {
        selectClue(clue?.clueID)
    }

    fun selectClue(cid : ClueID?) {
        getBoard()?.let { board ->
            cid?.let { cid ->
                if (cid != board.getClueID())
                    board.jumpToClue(cid)
            }
        }
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        handleChangeTimer()
        handleChangeAccessibility(changes)
        handleChangeChatGPT(changes)
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
            settings,
            currentPuzzleHolder,
        )
    }

    fun closePuzzleFinishedDialog() {
        _finishedDialogViewModel.value = null
    }

    fun doSpecialEntry() {
        _specialEntryDialogViewModel.value = SpecialEntryDialogViewModel(
            settings,
            currentPuzzleHolder,
        )
    }

    fun clearSpecialEntry() {
        _specialEntryDialogViewModel.value = null
    }

    /**
     * Prepared command for inputting letters
     */
    protected fun registerVoiceCommandLetter() {
        val app : Application = getApplication()
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_letter),
                { letter ->
                    if (letter != null && !letter.isEmpty()) {
                        getBoard()?.playLetter(
                            Character.toUpperCase(letter[0])
                        )
                    }
                },
            )
        )
    }

    /**
     * Prepared command for jumping to clue number
     */
    protected fun registerVoiceCommandNumber() {
        val app : Application = getApplication()
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_number),
                { textNumber ->
                    val prepped = WordsToNumbersUtil
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

    /**
     * Prepared command for clearing current word
     */
    protected fun registerVoiceCommandClear() {
        val app : Application = getApplication()
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_clear),
                { getBoard()?.clearWord() },
            )
        )
    }

    /**
     * Prepared command for announcing current clue
     */
    protected fun registerVoiceCommandAnnounceClue() {
        val app : Application = getApplication()
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_announce_clue),
                { announceClue(false) },
            )
        )
    }

    /**
     * Prepared command for requesting help
     */
    protected fun registerVoiceCommandClueHelp() {
        val app : Application = getApplication()
        registerVoiceCommand(
            VoiceCommand(
                app.getString(R.string.command_current_clue_help),
                { requestHelpForCurrentClue() }
            )
        )
    }

    protected fun getLongClueText(clue : Clue, cb : (String) -> Unit) {
        settings.getPlayShowCount() { showCount ->
            val text = PlayboardTextRenderer.getLongClueText(
                getApplication(),
                clue,
                showCount
            )
            cb(text)
        }
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
        val state = keyboardState.value ?: KeyboardState()
        _keyboardState.value = state.copy(
            visibility = visibility,
            needsRefresh = true,
            hideOnBack = info.hideOnBack,
            showHideButton = info.showHideButton,
        )
    }

    private fun keyboardManagerHide() {
        val state = keyboardState.value ?: KeyboardState()
        _keyboardState.value = state.copy(
            visibility = KeyboardVisibility.NONE,
            needsRefresh = true,
            hideOnBack = false,
        )
    }

    private fun sendToast(text : String) {
        sendToastEvent.postValue(SendToast(text))
    }

    private fun getMenuState() : PuzzleMenuState {
        val board = getBoard()
        val puz = getPuzzle()

        val hasShareURL = puz?.getShareUrl() != null
        val hasSupportURL = puz?.getSupportUrl() != null
        val canReveal = puz?.hasSolution() ?: false

        val box = board?.getCurrentBox();

        val canRevealInitialLetters = puz?.hasInitialValueCells() ?: false
        val canRevealInitialBoxLetter
            = !Box.isBlock(box) && box?.hasInitialValue() ?: false

        val extSettings = settings.liveExternalToolSettings.value
        val externalToolsState = ExternalToolsMenuState(
            extSettings?.hasChatGPT ?: false,
            extSettings?.crosswordSolverEnabled ?: false,
            extSettings?.duckDuckGoEnabled ?: false,
            extSettings?.fifteenSquaredEnabled ?: false,
            extSettings?.hasExternalDictionary ?: false,
        )

        val isScratchMode = settings.livePlayScratchMode.value

        val isShowingErrorsClue
            = settings.livePlayShowErrorsClue.value ?: false
        val isShowingErrorsCursor
            = settings.livePlayShowErrorsCursor.value ?: false
        val isShowingErrorsGrid
            = settings.livePlayShowErrorsGrid.value ?: false
        val showErrorsState = ShowErrorsMenuState(
            canReveal,
            isShowingErrorsClue,
            isShowingErrorsCursor,
            isShowingErrorsGrid,
        )

        val expanded = menuState.value?.expanded ?: PuzzleSubMenu.NONE

        return PuzzleMenuState(
            hasShareURL,
            hasSupportURL,
            externalToolsState,
            isScratchMode!!,
            showErrorsState,
            canReveal,
            canRevealInitialBoxLetter,
            canRevealInitialLetters,
            expanded,
        )
    }

    private fun handleChangeTimer() {
        val puz = getPuzzle()
        if (puz != null && puz.getPercentComplete() == 100) {
            if (isTimerRunning) {
                stopTimer(true)
                showFinishedDialog()
            }
        }
    }

    private fun isAccessibilityServiceRunning() : Boolean {
        val app : Application = getApplication()
        val manager = app.getSystemService(
            Context.ACCESSIBILITY_SERVICE
        ) as AccessibilityManager?;
        return manager != null && manager.isEnabled();
    }

    private fun announce(
        data : AnnounceData,
        onlyIfAccessibilityService : Boolean,
    ) {
        if (!onlyIfAccessibilityService || isAccessibilityServiceRunning()) {
            announceEvent.postValue(data)
        }
    }

    private fun handleChangeAccessibility(changes : PlayboardChanges) {
        val accessibilityRunning = isAccessibilityServiceRunning()
        settings.getVoiceAlwaysAnnounceBox() { announceBoxSetting ->
        settings.getVoiceAlwaysAnnounceClue() { announceClueSetting ->
            val announceBox = accessibilityRunning || announceBoxSetting
            val announceClue = accessibilityRunning || announceClueSetting
            val board = getBoard()

            if (board != null && (announceClue || announceBox)) {
                val newPos = board.getHighlightLetter()
                val isNewWord
                    = changes.getPreviousWord() != changes.getCurrentWord()
                val isNewPosition = changes.getPreviousPosition() != newPos

                if (isNewWord && announceClue)
                    announceClue(!announceClueSetting)
                else if (isNewPosition && announceBox)
                    announceBox(!announceBoxSetting)
            }
        }}
    }

    private fun handleChangeChatGPT(changes : PlayboardChanges) {
        isChatGPTEnabled(settings) { isEnabled ->
            if (isEnabled) {
                val newWord = changes.previousWord != changes.currentWord
                if (newWord)
                    _menuState.postValue(getMenuState())
            }
        }
    }

    private fun getUpdatedKeyboardState() : KeyboardState {
        val state = keyboardState.value ?: KeyboardState()
        return settings.liveKeyboardSettings.value?.let { ks ->
            state.copy(
                compact = ks.compact,
                hapticFeedback = ks.haptic,
                layout = ks.layout,
                repeatDelay = ks.repeatDelay.toLong(),
                repeatInterval = ks.repeatInterval.toLong(),
            )
        } ?: state
    }

    private fun showDialog(dialog : PuzzleDialog) {
        _uiState.value = uiState.value?.copy(
            showDialog = dialog,
        )
    }

    private fun clearDialog(dialog : PuzzleDialog) {
        if (uiState.value?.showDialog == dialog) {
            _uiState.value = uiState.value?.copy(
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
}

