
package app.crossword.yourealwaysbe.forkyz

//import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.Locale
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.text.Regex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

import androidx.lifecycle.asLiveData
import androidx.lifecycle.LiveData
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel

import org.jg.wordstonumbers.WordsToNumbersUtil

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.tools.ChatGPTHelpData
import app.crossword.yourealwaysbe.forkyz.tools.CrosswordSolverData
import app.crossword.yourealwaysbe.forkyz.tools.DuckDuckGoData
import app.crossword.yourealwaysbe.forkyz.tools.ExternalDictionaries
import app.crossword.yourealwaysbe.forkyz.tools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.tools.FifteenSquaredData
import app.crossword.yourealwaysbe.forkyz.tools.ShareClueData
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.SingleLiveEvent
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared
import app.crossword.yourealwaysbe.forkyz.util.liveDataFanIn
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Puzzle

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
)

data class MenuState(
    val hasShareURL : Boolean = true,
    val hasSupportURL : Boolean = true,
    val externalToolsState : ExternalToolsMenuState = ExternalToolsMenuState(),
    val isScratchMode : Boolean = false,
    val showErrorsState : ShowErrorsMenuState = ShowErrorsMenuState(),
    val canReveal : Boolean = true,
    val canRevealInitialBoxLetter : Boolean = true,
    val canRevealInitialLetters : Boolean = true,
)

// will lose this tag and become an open base class
@HiltViewModel
class PuzzleActivityViewModel @Inject constructor(
    application : Application,
    val settings : ForkyzSettings,
    val currentPuzzleHolder : CurrentPuzzleHolder,
    val fileHandlerProvider : FileHandlerProvider,
    val utils : AndroidVersionUtils,
) : AndroidViewModel(application) {
    private val LOG = Logger.getLogger(
        PuzzleActivityViewModel::class.toString()
    )

    val shareStreamEvent = SingleLiveEvent<ShareStream>()
    val announceClueEvent = SingleLiveEvent<Void>()
    val sendToastEvent = SingleLiveEvent<SendToast>()
    val externalToolEvent = SingleLiveEvent<ExternalToolData>()

    val menuState : LiveData<MenuState> = liveDataFanIn(
        this::getMenuState,
        settings.liveExternalToolSettings,
        settings.livePlayScratchMode,
        settings.livePlayShowErrorsClue,
        settings.livePlayShowErrorsCursor,
        settings.livePlayShowErrorsGrid,
    )

    private val voiceCommandDispatcher = VoiceCommands()

    protected fun getBoard() : Playboard? = currentPuzzleHolder.board
    protected fun getPuzzle() : Puzzle? = getBoard()?.getPuzzle()

    fun saveBoard() {
        currentPuzzleHolder.saveBoard()
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

        val app : Application = getApplication()
        FileHandlerShared.getShareUri(
            app,
            puz,
            writeBlank,
            omitExtensions
        ) { puzUri ->
            val mimeType = FileHandlerShared.getShareUriMimeType()
            shareStreamEvent.postValue(
                ShareStream(
                    app.getString(R.string.share_puzzle_title),
                    mimeType,
                    puzUri,
                )
            )
        }
    }

    fun dispatchVoiceCommand(text : List<String>) {
        voiceCommandDispatcher.dispatch(text)
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
                { announceClueEvent.postCall() },
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

        return MenuState(
            hasShareURL,
            hasSupportURL,
            externalToolsState,
            isScratchMode!!,
            showErrorsState,
            canReveal = canReveal,
            canRevealInitialBoxLetter = canRevealInitialBoxLetter,
            canRevealInitialLetters = canRevealInitialLetters,
        )
    }
}

