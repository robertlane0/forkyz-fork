
package app.crossword.yourealwaysbe.forkyz

//import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.Locale
import java.util.logging.Logger
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

import app.crossword.yourealwaysbe.forkyz.exttools.ChatGPTHelpData
import app.crossword.yourealwaysbe.forkyz.exttools.CrosswordSolverData
import app.crossword.yourealwaysbe.forkyz.exttools.DuckDuckGoData
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalDictionaries
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.exttools.FifteenSquaredData
import app.crossword.yourealwaysbe.forkyz.exttools.ShareClueData
import app.crossword.yourealwaysbe.forkyz.exttools.SharePuzzleData
import app.crossword.yourealwaysbe.forkyz.inttools.CellFlagData
import app.crossword.yourealwaysbe.forkyz.inttools.ClueFlagData
import app.crossword.yourealwaysbe.forkyz.inttools.EditClueData
import app.crossword.yourealwaysbe.forkyz.inttools.InternalToolData
import app.crossword.yourealwaysbe.forkyz.inttools.ClueNotesData
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
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
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardListener
import app.crossword.yourealwaysbe.puz.Puzzle

enum class SubMenu {
    NONE,
    MAIN,
    NOTES,
    SHOW_ERRORS,
    REVEAL,
    EXTERNAL_TOOLS,
    SHARE,
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

data class MenuState(
    val hasShareURL : Boolean = true,
    val hasSupportURL : Boolean = true,
    val externalToolsState : ExternalToolsMenuState = ExternalToolsMenuState(),
    val isScratchMode : Boolean = false,
    val showErrorsState : ShowErrorsMenuState = ShowErrorsMenuState(),
    val canReveal : Boolean = true,
    val canRevealInitialBoxLetter : Boolean = true,
    val canRevealInitialLetters : Boolean = true,
    val expanded : SubMenu = SubMenu.NONE,
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

data class PuzzleActivityUIState(
    val showTimer : Boolean = false,
    val indicateShowErrors : Boolean = false,
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
 * @param needsNativeRefresh indicates if native state has just changed,
 * so keyboard needs showing or hiding. Call markNativeRefreshed() once
 * launched.
 * @param hideOnBack whether the (Forkyz) keyboard should intepret a back gesture
 * as "close"
 * @param showHideButton whether the Forkyz keyboard should show a hide
 * button
 * @param forceCaps whether the native keyboard should convert to caps
 */
data class KeyboardState(
    val visibility : KeyboardVisibility = KeyboardVisibility.NONE,
    val needsNativeRefresh : Boolean = false,
    val hideOnBack : Boolean = false,
    val showHideButton : Boolean = false,
    val forceCaps : Boolean = false,
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
    private val LOG = Logger.getLogger(
        PuzzleActivityViewModel::class.toString()
    )

    val announceEvent = SingleLiveEvent<AnnounceData?>()
    val sendToastEvent = SingleLiveEvent<SendToast?>()
    val externalToolEvent = SingleLiveEvent<ExternalToolData?>()
    val internalToolEvent = SingleLiveEvent<InternalToolData?>()
    val puzzleFinishedEvent = SingleLiveEvent<Boolean>()
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

    private val menuExpandStack : MutableList<SubMenu> = mutableListOf()
    private val _menuState : MutableLiveData<MenuState> = liveDataFanIn(
        this::getMenuState,
        settings.liveExternalToolSettings,
        settings.livePlayScratchMode,
        settings.livePlayShowErrorsClue,
        settings.livePlayShowErrorsCursor,
        settings.livePlayShowErrorsGrid,
    )
    val menuState : LiveData<MenuState> = _menuState

    val uiState = liveDataFanIn(
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

    val _keyboardState = MutableStateFlow<KeyboardState>(KeyboardState())
    val keyboardState : StateFlow<KeyboardState> = _keyboardState

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
        get() = getPuzzle() != null
    val isTimerRunning : Boolean
        get() = timer != null
    val supportURL : String?
        get() = getPuzzle()?.getSupportUrl()
    val shareURL : String?
        get() = getPuzzle()?.getShareUrl()

    private val voiceCommandDispatcher = VoiceCommands()

    // needed public to send to non-compose views
    fun getBoard() : Playboard? = currentPuzzleHolder.board
    fun getPuzzle() : Puzzle? = getBoard()?.getPuzzle()

    fun saveBoard() {
        currentPuzzleHolder.saveBoard()
    }

    fun listenBoard() {
        getBoard()?.addListener(this)
    }

    fun unlistenBoard() {
        getBoard()?.removeListener(this)
    }

    fun markNativeRefreshed() {
        _keyboardState.value = keyboardState.value.copy(
            needsNativeRefresh = false,
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

    fun stopTimer() {
        getPuzzle()?.let { puz ->
            timer?.let { timer ->
                if (puz.getPercentComplete() != 100) {
                    timer.stop();
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
                internalToolEvent.postValue(ClueFlagData.build(clue.getClueID()))
        }
    }

    fun toggleCellFlag() {
        getBoard()?.let { board ->
            getPuzzle()?.let { puz ->
                board.getHighlightLetter()?.let { pos ->
                    puz.checkedGetBox(pos)?.let { box ->
                        if (box.isFlagged())
                            board.flagPosition(pos, false)
                        else
                            internalToolEvent.postValue(
                                CellFlagData.build(pos)
                            )
                    }
                }
            }
        }
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

    fun toggleScratchMode() {
        settings.getPlayScratchMode() { settings.setPlayScratchMode(!it); }
    }

    fun requestHelpForCurrentClue() {
        val app : Application = getApplication()

        if (!utils.hasNetworkConnection(getApplication())) {
            sendToast(app.getString(R.string.help_query_but_no_active_network))
        } else {
            getBoard()?.let { board ->
                ChatGPTHelpData.buildForCurrentClue(
                    app,
                    settings,
                    board,
                    externalToolEvent::postValue,
                )
            }
        }
    }

    fun shareClue(withResponse : Boolean) {
        val board = getBoard()
        val clue = board?.getClue()
        if (clue == null)
            return

        ShareClueData.build(
            getApplication(),
            settings,
            board,
            clue,
            withResponse,
            externalToolEvent::postValue,
        )
    }

    fun sharePuzzle(writeBlank : Boolean, omitExtensions : Boolean) {
        val puz = getPuzzle()
        if (puz == null)
            return

        SharePuzzleData.build(
            getApplication(),
            puz,
            writeBlank,
            omitExtensions,
            externalToolEvent::postValue,
        )
    }

    /**
     * Handles user request to edit currently selected clue
     */
    fun editClue() {
        getBoard()?.getClueID()?.let { editClue(it) }
    }

    fun editClue(cid : ClueID?) {
        cid?.let {
            internalToolEvent.postValue(EditClueData.build(it))
        }
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
        getBoard()?.let { board ->
            if (cid == null) {
                launchPuzzleNotes()
            } else {
                internalToolEvent.postValue(
                    ClueNotesData.buildClueNotes(board.getClueID())
                )
            }
        }
    }

    fun launchPuzzleNotes() {
        internalToolEvent.postValue(ClueNotesData.buildPuzzleNotes())
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

    fun expandMenu(menu : SubMenu) {
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
            _menuState.postValue(state.copy(expanded = SubMenu.NONE))
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
        _menuState.postValue(state.copy(expanded = SubMenu.NONE))
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

    fun clearPuzzleFinishedEvent() {
        puzzleFinishedEvent.postValue(false)
    }

    fun selectClue(clue : Clue?) {
        getBoard()?.let { board ->
            clue?.let { clue ->
                if (clue.getClueID() != board.getClueID())
                    board.jumpToClue(clue)
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

    fun showKeyboard() {
        keyboardManager.showKeyboard(this::keyboardManagerShow)
    }

    fun hideKeyboard(force : Boolean = false) {
        keyboardManager.hideKeyboard(
            this::keyboardManagerHide,
            force,
        )
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

    private fun keyboardManagerShow(info : KeyboardInfo) {
        val visibility = if (info.useNative)
            KeyboardVisibility.NATIVE
        else
            KeyboardVisibility.FORKYZ
        val needsNativeRefresh = info.useNative
        _keyboardState.value = KeyboardState(
            visibility,
            needsNativeRefresh,
            info.hideOnBack,
            info.showHideButton,
            info.forceCaps,
        )
    }

    private fun keyboardManagerHide() {
        val needsNativeRefresh
            = keyboardState.value.visibility == KeyboardVisibility.NATIVE
        _keyboardState.value = KeyboardState(
            visibility = KeyboardVisibility.NONE,
            needsNativeRefresh = needsNativeRefresh,
            hideOnBack = false,
        )
    }

    private fun sendToast(text : String) {
        sendToastEvent.postValue(SendToast(text))
    }

    private fun getMenuState() : MenuState {
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

        val expanded = menuState.value?.expanded ?: SubMenu.NONE

        return MenuState(
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
            if (isTimerRunning)
                puzzleFinishedEvent.postValue(true)
            stopTimer()
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
        ChatGPTHelpData.isEnabled(settings) { isEnabled ->
            if (isEnabled) {
                val newWord = changes.previousWord != changes.currentWord
                if (newWord)
                    _menuState.postValue(getMenuState())
            }
        }
    }
}

