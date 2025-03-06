
package app.crossword.yourealwaysbe.forkyz

import java.io.IOException
import java.util.Objects
import java.util.function.Consumer
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityViewCommand
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.exttools.ChatGPTHelpData
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolLauncher
import app.crossword.yourealwaysbe.forkyz.inttools.InternalToolLauncher
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.SpeechContract
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.view.ForkyzKeyboard
import app.crossword.yourealwaysbe.forkyz.view.PlayboardTextRenderer
import app.crossword.yourealwaysbe.forkyz.view.PuzzleInfoDialogs
import app.crossword.yourealwaysbe.forkyz.view.RevealPuzzleDialog
import app.crossword.yourealwaysbe.forkyz.view.SpecialEntryDialog
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.Zone

@AndroidEntryPoint
abstract class PuzzleActivityKt() : ForkyzActivityKt() {
    private val LOG = Logger.getLogger(PuzzleActivity::class.toString())

    protected val BASE_MENU_MODIFIER
        = Modifier.defaultMinSize(minWidth = 200.dp)

    /**
     * Get currently known value of volume activates voice
     *
     * Needs to be non-blocking so that onKeyEvent can return the
     * right value immediately, so use a live value that may be unknown
     * (defaults to false)
     */
    protected val isVolumeDownActivatesVoicePref : Boolean
        get() {
            return viewModel.voiceState.value?.volumeActivatesVoice ?: false
        }

    protected val isAnnounceClueEquals : Boolean
        get() {
            return viewModel.voiceState.value?.equalsAnnounceClue ?: false
        }

    protected val inputConnectionMediator = InputConnectionMediator()
    private var ttsService : TextToSpeech? = null
    private var ttsReady : Boolean = false

    abstract protected val viewModel : PuzzleActivityViewModel
    private var appBarLayout : AppBarLayout? = null

    private val voiceInputLauncher : ActivityResultLauncher<String> =
        registerForActivityResult(SpeechContract()) { text ->
            viewModel.dispatchVoiceCommand(text)
        }

    private val isAccessibilityServiceRunning : Boolean
        get() {
            val manager = getSystemService(
                Context.ACCESSIBILITY_SERVICE
            ) as AccessibilityManager?
            return manager?.isEnabled() ?: false
        }

    // only enabled when non-native keyboard is showing and back will cause it
    // to hide
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            viewModel.hideKeyboard(true)
        }
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, backCallback)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.keyboardState.collect { state ->
                    backCallback.isEnabled = state.hideOnBack
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()

        viewModel.stopTimer()
        viewModel.saveBoard()
        viewModel.unlistenBoard()

        ttsService?.let {
            it.shutdown()
            ttsService = null
            ttsReady = false
        }
    }

    override fun onResume() {
        super.onResume()

        if (!viewModel.canResume) {
            LOG.info("No puzzle board, puzzle activity finishing.")
            finish()
            return
        }

        viewModel.startTimer()
        viewModel.listenBoard()
    }

    /**
     * Use if you want to update your UI based on the timer
     *
     * Callback is a launched effect.
     */
    @Composable
    protected fun OnTimerUpdate(onTimerUpdate : () -> Unit) {
        val state by viewModel.uiState.observeAsState()
        val showTimer = state?.showTimer ?: false
        LaunchedEffect(showTimer) {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (showTimer) {
                    delay(1000L)
                    onTimerUpdate()
                }
            }
        }
    }

    @Composable
    protected fun Modifier.puzzleStatusBarColor() : Modifier {
        val state by viewModel.menuState.observeAsState()

        // always return same padding
        // not returning status bar padding means the app freezes if
        // imepadding is added afterwards.
        // potentially just a material bug that might be fixed in
        // future
        if (state?.showErrorsState?.isShowing ?: false)
            return this.background(themeHelper.cheatedColor)
                .statusBarsPadding()
        else
            return this.statusBarsPadding()
    }

    protected open fun onKeyEvent(event : KeyEvent) : Boolean {

        return when (event.key) {
            Key.Equals -> {
                if (isAnnounceClueEquals) {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.announceClue(false)
                    true
                } else false
            }
            Key.VolumeDown -> {
                if (isVolumeDownActivatesVoicePref) {
                    if (event.type == KeyEventType.KeyUp)
                        launchVoiceInput()
                    true
                } else false
            }
            else -> false
        }
    }

    /**
     * Convert letter/digit key event to uppercase char
     *
     * Or null if not a char key
     */
    protected fun letterOrDigitKeyToChar(key : Key) : Char?
        = when (key) {
            // must be a better way..
            Key.A -> 'A'
            Key.B -> 'B'
            Key.C -> 'C'
            Key.D -> 'D'
            Key.E -> 'E'
            Key.F -> 'F'
            Key.G -> 'G'
            Key.H -> 'H'
            Key.I -> 'I'
            Key.J -> 'J'
            Key.K -> 'K'
            Key.L -> 'L'
            Key.M -> 'M'
            Key.N -> 'N'
            Key.O -> 'O'
            Key.P -> 'P'
            Key.Q -> 'Q'
            Key.R -> 'R'
            Key.S -> 'S'
            Key.T -> 'T'
            Key.U -> 'U'
            Key.V -> 'V'
            Key.W -> 'W'
            Key.X -> 'X'
            Key.Y -> 'Y'
            Key.Z -> 'Z'
            Key.Zero -> '0'
            Key.One -> '1'
            Key.Two -> '2'
            Key.Three -> '3'
            Key.Four -> '4'
            Key.Five -> '5'
            Key.Six -> '6'
            Key.Seven -> '7'
            Key.Eight -> '8'
            Key.Nine -> '9'
            else -> null
        }

    protected fun doBackAction() {
        if (backCallback.isEnabled)
            backCallback.handleOnBackPressed()
        else
            this.finish()
    }

    /**
     * Handle events and things that need to be launched because of
     *
     * If overriding, call super
     */
    @Composable
    protected open fun Launchers() {
        val announcement by viewModel.announceEvent.observeAsState()
        LaunchedEffect(announcement) {
            announcement?.let { data ->
                announce(data)
                viewModel.clearAnnounceEvent()
            }
        }
        val internalTool by viewModel.internalToolEvent.observeAsState()
        LaunchedEffect(internalTool) {
            internalTool?.let { tool ->
                tool.accept(InternalToolLauncher(this@PuzzleActivityKt))
                viewModel.clearInternalToolEvent()
            }
        }
        val externalTool by viewModel.externalToolEvent.observeAsState()
        LaunchedEffect(externalTool) {
            externalTool?.let { tool ->
                tool.accept(ExternalToolLauncher(this@PuzzleActivityKt))
                viewModel.clearExternalToolEvent()
            }
        }
        val sendToast by viewModel.sendToastEvent.observeAsState()
        LaunchedEffect(sendToast) {
            sendToast?.let { data ->
                Toast.makeText(
                    this@PuzzleActivityKt,
                    data.text,
                    Toast.LENGTH_LONG,
                ).show()
                viewModel.clearSendToast()
            }
        }
        val puzzleFinished by viewModel.puzzleFinishedEvent.observeAsState()
        LaunchedEffect(puzzleFinished) {
            if (puzzleFinished ?: false) {
                val dialog = PuzzleInfoDialogs.Finished()
                dialog.show(
                    getSupportFragmentManager(),
                  "PuzzleInfoDialogs.Finished"
                )
                viewModel.clearPuzzleFinishedEvent()
            }
        }
        val backEvent by viewModel.backEvent.observeAsState()
        LaunchedEffect(backEvent) {
            if (backEvent ?: false) {
                doBackAction()
                viewModel.clearBackEvent()
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    protected fun VoiceButtons() {
        val state by viewModel.voiceState.observeAsState()

        val showVoiceButton = state?.showVoiceButton ?: false
        val showAnnounceButton = state?.showAnnounceButton ?: false

        if (showVoiceButton || showAnnounceButton) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (showVoiceButton) {
                    FilledIconButton(
                        modifier = Modifier.padding(horizontal=10.dp)
                            .width(50.dp),
                        onClick = { launchVoiceInput() },
                    ) {
                        Icon(
                            modifier = Modifier.width(50.dp),
                            painter = painterResource(R.drawable.ic_voice),
                            contentDescription
                                = stringResource(R.string.voice_command),
                        )
                    }
                }
                if (showAnnounceButton) {
                    FilledIconButton(
                        modifier = Modifier.padding(horizontal=10.dp)
                            .width(50.dp),
                        onClick = { viewModel.announceClue() },
                    ) {
                        Icon(
                            modifier = Modifier.width(50.dp),
                            painter = painterResource(R.drawable.ic_announce),
                            contentDescription = stringResource(
                                R.string.announce_clue_label,
                            ),
                        )
                    }
                }
            }
        }
    }

    @Composable
    protected fun Keyboard() {
        val state by viewModel.keyboardState.collectAsStateWithLifecycle()

        LaunchedEffect(state.needsNativeRefresh) {
            if (state.needsNativeRefresh) {
                if (state.visibility == KeyboardVisibility.NATIVE) {
                    inputConnectionMediator.showNative(state.forceCaps)
                } else {
                    inputConnectionMediator.hideNative()
                }
                viewModel.markNativeRefreshed()
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                val keyboard = ForkyzKeyboard(context)
                inputConnectionMediator.registerForkyzKeyboard(keyboard)
                keyboard
            },
            update = { view ->
                val visibility
                    = if (state.visibility == KeyboardVisibility.FORKYZ)
                        View.VISIBLE
                    else
                        View.GONE
                view.setVisibility(visibility)
                view.setShowHideButton(
                    state.showHideButton,
                    { viewModel.hideKeyboard(true) },
                )
            },
        )
    }

    @Composable
    protected fun MenuCheckedItem(
        checked : Boolean,
        text : @Composable () -> Unit,
        onClick : () -> Unit,
    ) {
        DropdownMenuItem(
            text = text,
            onClick = onClick,
            trailingIcon = {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onClick() },
                )
            }
        )
    }

    @Composable
    protected fun MenuText(text : String) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }

    @Composable
    protected fun MenuNotes() {
        MenuSubEntry(
            text = { MenuText(stringResource(R.string.menu_notes)) },
            onClick = { viewModel.expandMenu(SubMenu.NOTES) },
        )
    }

    @Composable
    protected fun MenuNotesSub() {
        val state by viewModel.menuState.observeAsState()

        DropdownMenu(
            modifier = BASE_MENU_MODIFIER,
            expanded = state?.expanded == SubMenu.NOTES,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.menu_notes,
                onClick = viewModel::closeMenu,
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.menu_notes_clue)) },
                onClick = {
                    viewModel.launchCurrentClueNotes()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(R.string.menu_notes_puzzle))
                },
                onClick = {
                    viewModel.launchPuzzleNotes()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(R.string.menu_flag_clue_toggle))
                },
                onClick = {
                    viewModel.toggleClueFlag()
                    viewModel.dismissMenu()
                }
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(R.string.menu_flag_cell_toggle))
                },
                onClick = {
                    viewModel.toggleCellFlag()
                    viewModel.dismissMenu()
                }
            )
        }
    }

    @Composable
    protected fun MenuScratchMode() {
        val state by viewModel.menuState.observeAsState()
        MenuCheckedItem(
            checked = state?.isScratchMode ?: false,
            text = { MenuText(stringResource(R.string.scratch_mode)) },
            onClick = {
                viewModel.toggleScratchMode()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    protected fun MenuSpecialEntry() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.special_entry)) },
            onClick = {
                specialEntry()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    protected fun MenuShowErrors() {
        val state by viewModel.menuState.observeAsState()
        if (!(state?.showErrorsState?.enabled ?: false))
            return

        MenuSubEntry(
            text = {
                if (state?.showErrorsState?.isShowing ?: false)
                    MenuText(stringResource(R.string.showing_errors))
                else
                    MenuText(stringResource(R.string.show_errors))
            },
            onClick = { viewModel.expandMenu(SubMenu.SHOW_ERRORS) },
        )
    }

    @Composable
    protected fun MenuShowErrorsSub() {
        val state by viewModel.menuState.observeAsState()
        if (!(state?.showErrorsState?.enabled ?: false))
            return

        DropdownMenu(
            modifier = BASE_MENU_MODIFIER,
            expanded = state?.expanded == SubMenu.SHOW_ERRORS,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.show_errors,
                onClick = viewModel::closeMenu,
            )
            MenuCheckedItem(
                checked
                    = state?.showErrorsState?.isShowingErrorsCursor ?: false,
                text = {
                    MenuText(stringResource(R.string.show_errors_cursor))
                },
                onClick = {
                    viewModel.toggleShowErrorsCursor()
                    viewModel.dismissMenu()
                }
            )
            MenuCheckedItem(
                checked = state?.showErrorsState?.isShowingErrorsClue ?: false,
                text = { MenuText(stringResource(R.string.show_errors_clue)) },
                onClick = {
                    viewModel.toggleShowErrorsClue()
                    viewModel.dismissMenu()
                }
            )
            MenuCheckedItem(
                checked = state?.showErrorsState?.isShowingErrorsGrid ?: false,
                text = {
                    MenuText(stringResource(R.string.show_errors_full_grid))
                },
                onClick = {
                    viewModel.toggleShowErrorsGrid()
                    viewModel.dismissMenu()
                }
            )
        }
    }

    @Composable
    protected fun MenuReveal() {
        val state by viewModel.menuState.observeAsState()
        if (!(state?.canReveal ?: false))
            return

        MenuSubEntry(
            text = { MenuText(stringResource(R.string.reveal)) },
            onClick = { viewModel.expandMenu(SubMenu.REVEAL) },
        )
    }

    @Composable
    protected fun MenuRevealSub() {
        val state by viewModel.menuState.observeAsState()
        if (!(state?.canReveal ?: false))
            return

        DropdownMenu(
            modifier = BASE_MENU_MODIFIER,
            expanded = state?.expanded == SubMenu.REVEAL,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.reveal,
                onClick = viewModel::closeMenu,
            )
            if (state?.canRevealInitialBoxLetter ?: false) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(R.string.initial_letter))
                    },
                    onClick = {
                        viewModel.revealInitialLetter()
                        viewModel.dismissMenu()
                    },
                )
            }
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.letter)) },
                onClick = {
                    viewModel.revealLetter()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.word)) },
                onClick = {
                    viewModel.revealWord()
                    viewModel.dismissMenu()
                },
            )
            if (state?.canRevealInitialLetters ?: false) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(R.string.initial_letters))
                    },
                    onClick = {
                        viewModel.revealInitialLetters()
                        viewModel.dismissMenu()
                    },
                )
            }
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.errors)) },
                onClick = {
                    viewModel.revealErrors()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.puzzle)) },
                onClick = {
                    showRevealPuzzleDialog()
                    viewModel.dismissMenu()
                },
            )
        }
    }

    @Composable
    protected fun MenuExternalTools() {
        val state by viewModel.menuState.observeAsState()
        if (!(state?.externalToolsState?.hasExternal ?: false))
            return

        MenuSubEntry(
            text = { MenuText(stringResource(R.string.external_tools)) },
            onClick = { viewModel.expandMenu(SubMenu.EXTERNAL_TOOLS) },
        )
    }

    @Composable
    protected fun MenuExternalToolsSub() {
        val state by viewModel.menuState.observeAsState()
        if (!(state?.externalToolsState?.hasExternal ?: false))
            return

        DropdownMenu(
            modifier = BASE_MENU_MODIFIER,
            expanded = state?.expanded == SubMenu.EXTERNAL_TOOLS,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.external_tools,
                onClick = viewModel::closeMenu,
            )
            if (state?.externalToolsState?.hasExternalDictionary ?: false) {
                DropdownMenuItem(
                    text = {
                        MenuText(
                            stringResource(R.string.help_external_dictionary)
                        )
                    },
                    onClick = {
                        viewModel.callExternalDictionary()
                        viewModel.dismissMenu()
                    },
                )
            }
            if (state?.externalToolsState?.hasCrosswordSolver ?: false) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(R.string.help_crossword_solver))
                    },
                    onClick = {
                        viewModel.callCrosswordSolver()
                        viewModel.dismissMenu()
                    },
                )
            }
            if (state?.externalToolsState?.hasChatGPT ?: false) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(R.string.help_query_chat_gpt))
                    },
                    onClick = {
                        viewModel.requestHelpForCurrentClue()
                        viewModel.dismissMenu()
                    },
                )
            }
            if (state?.externalToolsState?.hasDuckDuckGo ?: false) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(R.string.help_duckduckgo))
                    },
                    onClick = {
                        viewModel.searchDuckDuckGo()
                        viewModel.dismissMenu()
                    },
                )
            }
            if (state?.externalToolsState?.hasFifteenSquared ?: false) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(R.string.help_fifteen_squared))
                    },
                    onClick = {
                        viewModel.searchFifteenSquared()
                        viewModel.dismissMenu()
                    },
                )
            }
        }
    }

    @Composable
    protected fun MenuShare() {
        MenuSubEntry(
            text = { MenuText(stringResource(R.string.share)) },
            onClick = { viewModel.expandMenu(SubMenu.SHARE) },
        )
    }

    @Composable
    protected fun MenuShareSub() {
        val state by viewModel.menuState.observeAsState()

        DropdownMenu(
            modifier = BASE_MENU_MODIFIER,
            expanded = state?.expanded == SubMenu.SHARE,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.share,
                onClick = viewModel::closeMenu,
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.share_clue)) },
                onClick = {
                    viewModel.shareClue(false)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(R.string.share_clue_response))
                },
                onClick = {
                    viewModel.shareClue(true)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(R.string.share_puzzle_full))
                },
                onClick = {
                    viewModel.sharePuzzle(false, false)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(
                        stringResource(R.string.share_puzzle_without_extensions)
                    )
                },
                onClick = {
                    viewModel.sharePuzzle(false, true)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.share_puzzle_orig)) },
                onClick = {
                    viewModel.sharePuzzle(true, true)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(
                        stringResource(R.string.share_puzzle_open_share_url)
                    )
                },
                onClick = {
                    openShareUrl()
                    viewModel.dismissMenu()
                },
            )
        }
    }

    @Composable
    protected fun MenuEditClue() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.edit_clue)) },
            onClick = {
                viewModel.editClue()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    protected fun MenuHelp() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.help)) },
            onClick = {
                actionHelp()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    protected fun MenuSettings() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.settings)) },
            onClick = {
                actionSettings()
                viewModel.dismissMenu()
            }
        )
    }
    @Composable
    protected fun MenuPuzzleInfo() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.info)) },
            onClick = {
                showInfoDialog()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    protected fun MenuSupportPuzzleSource() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.support_puzzle_source)) },
            onClick = {
                actionSupportSource()
                viewModel.dismissMenu()
            }
        )
    }

    private fun showRevealPuzzleDialog() {
        RevealPuzzleDialog().show(supportFragmentManager, "RevealPuzzleDialog")
    }

    private fun showInfoDialog() {
        PuzzleInfoDialogs.Info()
            .show(supportFragmentManager, "PuzzleInfoDialogs.Info")
    }

    private fun actionSupportSource() {
        viewModel.supportURL?.let { url ->
            val i = Intent(Intent.ACTION_VIEW)
            i.setData(Uri.parse(url))
            startActivity(i)
        }
    }

    private fun specialEntry() {
        SpecialEntryDialog().show(
            getSupportFragmentManager(),
            "SpecialEntryDialog",
        )
    }

    private fun openShareUrl() {
        viewModel.shareURL?.let { url ->
            if (!url.isEmpty()) {
                val i = Intent(Intent.ACTION_VIEW)
                i.setData(Uri.parse(url))
                startActivity(i)
            }
        }
    }

    /**
     * Add available accessibility actions to view
     *
     * Just announce clue for now. Only used by views not yet converted
     * to Compose. Will be replaced by Compose way in future.
     */
    protected fun addAccessibilityActions(view : View) {
        ViewCompat.addAccessibilityAction(
            view,
            getString(R.string.announce_clue_label),
            { _, _ ->
                viewModel.announceClue(true)
                true
            },
        )
    }

    /**
     * Announce text with accessibility if running or tts
     */
    private fun announce(data : AnnounceData) {
        val text = data.message
        if (isAccessibilityServiceRunning) {
            //val content = findViewById(android.R.id.content)
            //if (content != null && text != null)
            //    content.announceForAccessibility(text)
        } else {
            if (ttsService == null) {
                ttsService = TextToSpeech(
                    applicationContext,
                    { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            ttsReady = true
                            announce(data)
                        }
                    }
                )
            } else if (!ttsReady) {
                // hopefully rare occasion where tts being prepared but not
                // ready yet
                Toast.makeText(
                    this,
                    R.string.speech_not_ready,
                    Toast.LENGTH_SHORT,
                ).show()
            } else {
                utils.speak(ttsService, text)
            }
        }
    }

    private fun actionHelp() {
        val helpIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("play.html"),
            this,
            HTMLActivity::class.java,
        )
        this.startActivity(helpIntent)
    }

    private fun actionSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    @Composable
    private fun MenuSubEntry(
        text : @Composable () -> Unit,
        onClick : () -> Unit,
    ) {
        DropdownMenuItem(
            text = text,
            onClick = onClick,
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_submenu),
                    contentDescription = null,
                )
            }

        )
    }

    @Composable
    private fun MenuDropdownHeading(
        titleId : Int,
        onClick : () -> Unit,
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(titleId),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            },
            onClick = onClick,
        )
    }

    private fun launchVoiceInput() {
        try {
            voiceInputLauncher.launch(
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        } catch (e : ActivityNotFoundException) {
            Toast.makeText(
                this,
                R.string.no_speech_recognition_available,
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
