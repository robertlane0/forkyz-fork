
package app.crossword.yourealwaysbe.forkyz

//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.Locale
import java.util.logging.Logger
import javax.inject.Inject
import kotlin.text.Regex
import kotlinx.coroutines.launch

//import androidx.lifecycle.asLiveData
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel

import org.jg.wordstonumbers.WordsToNumbersUtil

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.tools.ChatGPTHelp
import app.crossword.yourealwaysbe.forkyz.tools.CrosswordSolverData
import app.crossword.yourealwaysbe.forkyz.tools.DuckDuckGoData
import app.crossword.yourealwaysbe.forkyz.tools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.SingleLiveEvent
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Puzzle

class ChatGPTResponse(val response : String)

class ShareMessage(
    val title : String,
    val message : String,
)

class ShareStream(
    val title : String,
    val mimeType : String,
    val uri : Uri,
)

class SendToast(val text : String)

// will lose this tag and become an open base class
@HiltViewModel
class PuzzleActivityViewModel @Inject constructor(
    application : Application,
) : AndroidViewModel(application) {
    private val LOG = Logger.getLogger(
        PuzzleActivityViewModel::class.qualifiedName
    )

    @Inject
    lateinit var settings : ForkyzSettings

    @Inject
    lateinit var currentPuzzleHolder : CurrentPuzzleHolder

    @Inject
    lateinit var fileHandlerProvider : FileHandlerProvider

    @Inject
    lateinit var utils : AndroidVersionUtils

    val shareMessageEvent = SingleLiveEvent<ShareMessage>()
    val shareStreamEvent = SingleLiveEvent<ShareStream>()
    val announceClueEvent = SingleLiveEvent<Void>()
    val requestHelpForCurrentClueEvent = SingleLiveEvent<Void>()
    val sendToastEvent = SingleLiveEvent<SendToast>()
    val chatGPTResponseEvent = SingleLiveEvent<ChatGPTResponse>()
    val externalToolEvent = SingleLiveEvent<ExternalToolData>()

    private val voiceCommandDispatcher = VoiceCommands()

    protected fun getBoard() : Playboard? = currentPuzzleHolder.getBoard()
    protected fun getPuzzle() : Puzzle? = getBoard()?.getPuzzle()

    fun saveBoard() {
        val puzHandle = currentPuzzleHolder.getPuzHandle()
        if (puzHandle == null) {
            LOG.severe("No puz handle to save puzzle to.")
            return
        }

        val puz = getPuzzle()
        if (puz == null) {
            LOG.severe("No puzzle associated to the board to save.")
            return
        }

        fileHandlerProvider.get() { fileHandler ->
            // TODO: save off main thread? May cause race conditions
            // with BrowseActivity refresh?
            try {
                fileHandler.save(puz, puzHandle)
            } catch (e : IOException) {
                LOG.severe("Error saving puzzle.")
                e.printStackTrace()
            }
        }
    }

    fun requestHelpForCurrentClue() {
        val app : Application = getApplication()

        if (!utils.hasNetworkConnection(getApplication())) {
            sendToast(app.getString(R.string.help_query_but_no_active_network))
        } else {
            canRequestHelpForCurrentClue() { canRequest ->
                if (canRequest) {
                    ChatGPTHelp(settings).requestHelpForCurrentClue(
                        app,
                        getBoard(),
                    ) { response ->
                        chatGPTResponseEvent.postValue(
                            ChatGPTResponse(response)
                        )
                    }
                }
            }
        }
    }

    fun shareClue(withResponse : Boolean) {
        val board = getBoard()
        val clue = board?.getClue()
        if (clue == null)
            return

        val puz = board.getPuzzle()
        val response = board.getCurrentWordBoxes()
        val app : Application = getApplication()

        getShareMessage(puz, clue, response, withResponse) { shareMessage ->
            shareMessageEvent.postValue(
                ShareMessage(
                    app.getString(R.string.share_clue_title),
                    shareMessage,
                )
            )
        }
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
            val puzzleDetails = getSharePuzzleDetails(puz)
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
                { requestHelpForCurrentClueEvent.postCall() },
            )
        )
    }

    private fun getShareMessage(
        puz : Puzzle,
        clue : Clue,
        response : Array<Box>,
        withResponse : Boolean,
        cb : (String) -> Unit,
    ) {
        getShareClueText(clue) { clueText ->
            val app : Application = getApplication()

            val responseText
                = if (withResponse) getShareResponseText(response)
                else null

            val puzzleDetails = getSharePuzzleDetails(puz)

            if (withResponse) {
                cb(
                    app.getString(
                        R.string.share_clue_response_text,
                        clueText,
                        responseText,
                        puzzleDetails,
                    )
                )
            } else {
                cb(
                    app.getString(
                        R.string.share_clue_text,
                        clueText,
                        puzzleDetails,
                    )
                )
            }
        }
    }

    private fun getShareResponseText(boxes : Array<Box>) : String {
        val app : Application = getApplication()
        val responseText = StringBuilder()
        for (box in boxes) {
            if (box.isBlank()) {
                responseText.append(
                    app.getString(R.string.share_clue_blank_box),
                )
            } else {
                responseText.append(box.getResponse())
            }
        }
        return responseText.toString()
    }

    private fun getShareClueText(clue : Clue?, cb : (String) -> Unit) {
        settings.getPlayShowCount() { showCount ->
            val app : Application = getApplication()

            if (clue == null) {
                cb(app.getString(R.string.unknown_hint))
            } else {
                val wordLen = if (clue.hasZone()) clue.getZone().size() else -1

                if (showCount && wordLen >= 0) {
                    cb(
                        app.getString(
                            R.string.clue_format_short_no_num_no_dir_with_count,
                            clue.getHint(),
                            wordLen,
                        )
                    )
                } else {
                    cb(clue.getHint())
                }
            }
        }
    }

    private fun getSharePuzzleDetails(puz : Puzzle) : String {
        var source = puz.getSource()
        var title = puz.getTitle()
        var author = puz.getAuthor()

        if (source == null)
            source = "";
        if (title == null)
            title = "";
        if (author != null) {
            // add author if not already in title or caption
            // case insensitive trick:
            // https://www.baeldung.com/java-case-insensitive-string-matching
            val regex = ("(?i).*" + Regex.escape(author) + ".*").toRegex()
            val removeAuthor = author.isEmpty()
                || title.matches(regex)
                || source.matches(regex)

            if (removeAuthor)
                author = null
        }

        val shareUrl = puz.getShareUrl();

        val app : Application = getApplication()
        if (shareUrl == null || shareUrl.isEmpty()) {
            return if (author != null) {
                app.getString(
                    R.string.share_puzzle_details_author_no_url,
                    source,
                    title,
                    author,
                )
            } else {
                app.getString(
                    R.string.share_puzzle_details_no_author_no_url,
                    source,
                    title,
                )
            }
        } else {
            return if (author != null) {
                app.getString(
                    R.string.share_puzzle_details_author_url,
                    source,
                    title,
                    author,
                    shareUrl,
                )
            } else {
                app.getString(
                    R.string.share_puzzle_details_no_author_url,
                    source,
                    title,
                    shareUrl,
                )
            }
        }
    }

    private fun canRequestHelpForCurrentClue(cb : (Boolean) -> Unit) {
        val clue = getBoard()?.getClue();
        if (clue == null)
            cb(false)
        else
            ChatGPTHelp.isEnabled(settings, cb)
    }

    private fun sendToast(text : String) {
        sendToastEvent.postValue(SendToast(text))
    }
}

