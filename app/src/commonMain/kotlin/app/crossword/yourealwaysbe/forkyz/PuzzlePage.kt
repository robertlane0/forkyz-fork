
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_clue_label
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.are_you_sure
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.completed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.edit_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.edit_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.errors
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_app_install_msg
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_app_not_installed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_tools
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_tools
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fit_to_screen
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_crossword_solver
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_crossword_solver_anagram
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_duckduckgo
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_external_dictionary
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_fifteen_squared
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_query_llm_agent
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_announce
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_voice
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.info
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.initial_letter
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.initial_letters
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.letter
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_flag_cell_toggle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_flag_clue_toggle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_notes_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_notes_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ok
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.reveal
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.reveal
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.reveal_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scratch_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_clue_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_full
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_open_share_url
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_orig
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_without_extensions
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors_cursor
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors_full_grid
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.showing_errors
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.special_entry
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.support_puzzle_source
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.unknown_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.voice_command
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.website
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.word
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.zoom
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.zoom
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.zoom_in
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.zoom_in_max
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.zoom_out
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.zoom_reset
import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuDropdownHeading
import app.crossword.yourealwaysbe.forkyz.menu.MenuHelp
import app.crossword.yourealwaysbe.forkyz.menu.MenuSettings
import app.crossword.yourealwaysbe.forkyz.menu.MenuSubEntry
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout
import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings
import app.crossword.yourealwaysbe.forkyz.theme.BoardColorScheme
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toUri
import app.crossword.yourealwaysbe.forkyz.view.ChooseFlagColorDialog
import app.crossword.yourealwaysbe.forkyz.view.ForkyzKeyboard
import app.crossword.yourealwaysbe.forkyz.view.OKCancelDialog
import app.crossword.yourealwaysbe.forkyz.view.OKDialog
import app.crossword.yourealwaysbe.forkyz.view.PlayboardTextRenderer
import app.crossword.yourealwaysbe.forkyz.view.PuzzleFinishedDialog
import app.crossword.yourealwaysbe.forkyz.view.PuzzleInfoDialog
import app.crossword.yourealwaysbe.forkyz.view.SpecialEntryDialog
import app.crossword.yourealwaysbe.forkyz.view.SpecialKey
import app.crossword.yourealwaysbe.forkyz.view.StringDialog
import app.crossword.yourealwaysbe.forkyz.view.TwoButtonDialog
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.MovementStrategy
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.Zone

private val TAG = "ForkyzPuzzlePage"
private val CLUE_TEXT_SIZE = 17.sp

class PuzzlePageScope(
    private val viewModel : PuzzlePageViewModel,
    private val themeHelper : ThemeHelper,
    private val utils : NativeFrontendUtils,
    private val pageOpener : PageOpener,
) {
    val isVolumeDownActivatesVoicePref : Boolean
        get() {
            return viewModel.voiceState.value.volumeActivatesVoice
        }

    val isAnnounceClueEquals : Boolean
        get() {
            return viewModel.voiceState.value.equalsAnnounceClue
        }

    /**
     * Use if you want to update your UI based on the timer
     *
     * Callback is a launched effect.
     */
    @Composable
    fun OnTimerUpdate(onTimerUpdate : () -> Unit) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val showTimer by remember {
            derivedStateOf { state.showTimer }
        }
        LifecycleResumeEffect(showTimer) {
            scope.launch {
                while (showTimer) {
                    delay(1000L)
                    onTimerUpdate()
                }
            }
            onPauseOrDispose {
                // TODO: this is bad if scope not recreated on resume!
                scope.cancel()
            }
        }
    }

    @Composable
    fun Modifier.puzzleStatusBarColor() : Modifier {
        val state by viewModel.menuState.collectAsStateWithLifecycle()

        // always return same padding
        // not returning status bar padding means the app freezes if
        // imepadding is added afterwards.
        // potentially just a material bug that might be fixed in
        // future

        val isShowing by remember {
            derivedStateOf { state.showErrorsState.isShowing }
        }
        val color = if (isShowing)
            themeHelper.cheatedColor
        else
            themeHelper.statusBarColor

        // for older devices where Compose doesn't draw over status bar
        // for some reason
        LaunchedEffect(color) {
            utils.setStatusBarColor(color)
        }

        return this.background(color).statusBarsPadding()
    }

    /**
     * Clue text for top app bar
     *
     * If null, then displays a placeholder
     */
    @Composable
    fun ClueText(
        modifier : Modifier = Modifier,
        clueText : String? = null,
        onClick : () -> Unit = { },
        onClickDescription : String = "",
        onLongClick : () -> Unit = { },
        onLongClickDescription : String = "",
    ) {
        val fullModifier = modifier.combinedClickable(
            onClick = onClick,
            onClickLabel = onClickDescription,
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickDescription,
        )
        val text = clueText ?: stringResource(Res.string.unknown_hint)

        // Text doesn't support autoSize yet, so rebuild BasicText
        // from (or close enough)
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material/material/src/commonMain/kotlin/androidx/compose/material/Text.kt
        val color = LocalContentColor.current
        BasicText(
            modifier = fullModifier,
            text = remember(text) { htmlToAnnotatedString(text) },
            autoSize =  TextAutoSize.StepBased(
                minFontSize = 5.sp,
                maxFontSize = CLUE_TEXT_SIZE,
                stepSize = 1.sp,
            ),
            color = { color },
        )
    }

    fun onKeyEventPuzzlePage(event : KeyEvent) : Boolean {
        val isDown = event.type == KeyEventType.KeyDown
        return when (event.key) {
            Key.Escape -> {
                if (isDown)
                    pageOpener.onBack()
                return true
            }
            Key.Equals -> {
                if (isAnnounceClueEquals) {
                    if (isDown)
                        viewModel.announceClue()
                    true
                } else false
            }
            Key.VolumeDown -> {
                if (isVolumeDownActivatesVoicePref) {
                    if (isDown)
                        utils.launchVoiceInput()
                    true
                } else false
            }
            else -> false
        }
    }

    fun onSpecialKeyDownPuzzlePage(key : SpecialKey) {
        return when (key) {
            SpecialKey.KEY_SPECIAL_ENTRY -> {
                viewModel.doSpecialEntry()
            }
            else -> { /* ignore */ }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun VoiceButtons() {
        val state by viewModel.voiceState.collectAsStateWithLifecycle()

        val showVoiceButton by remember {
            derivedStateOf { state.showVoiceButton }
        }
        val showAnnounceButton by remember {
            derivedStateOf { state.showAnnounceButton }
        }

        if (showVoiceButton || showAnnounceButton) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 5.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                if (showVoiceButton) {
                    FilledIconButton(
                        modifier = Modifier.padding(horizontal=10.dp)
                            .width(50.dp),
                        onClick = { utils.launchVoiceInput() },
                    ) {
                        Icon(
                            modifier = Modifier.width(50.dp),
                            painter = painterResource(Res.drawable.ic_voice),
                            contentDescription
                                = stringResource(Res.string.voice_command),
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
                            painter = painterResource(Res.drawable.ic_announce),
                            contentDescription = stringResource(
                                Res.string.announce_clue_label,
                            ),
                        )
                    }
                }
            }
        }
    }

    /**
     * The built-in keyboard
     *
     * If providing own onSpecialKeyDown, call through to
     * onSpecialKeyDownPuzzlePage for anything not handled.
     */
    @Composable
    fun Keyboard(
        showSpecialKeys : Boolean = false,
        onSpecialKeyDown : (SpecialKey) -> Unit = ::onSpecialKeyDownPuzzlePage,
        onSpecialKeyUp : (SpecialKey) -> Unit = { },
    ) {
        val state by viewModel.keyboardState.collectAsStateWithLifecycle()

        LaunchedEffect(state.needsRefresh) {
            if (state.needsRefresh) {
                if (state.visibility == KeyboardVisibility.NATIVE) {
                    utils.inputConnectionMediator.showNative(state.forceCaps)
                } else {
                    utils.inputConnectionMediator.hideNative()
                }
                if (state.visibility == KeyboardVisibility.FORKYZ) {
                    utils.inputConnectionMediator.focusReceiver()
                }
                viewModel.markKeyboardRefreshed()
            }
        }

        if (state.visibility == KeyboardVisibility.FORKYZ) {
            ForkyzKeyboard(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                inputConnectionMediator = utils.inputConnectionMediator,
                onHideClick = { viewModel.hideKeyboard(true) },
                onSpecialKeyDown = onSpecialKeyDown,
                onSpecialKeyUp = onSpecialKeyUp,
                compact = state.compact,
                hapticFeedback = state.hapticFeedback,
                showHideButton = state.showHideButton,
                showSpecialKeys = showSpecialKeys,
                layout = state.layout,
                repeatDelay = state.repeatDelay,
                repeatInterval = state.repeatInterval,
            )
        }
    }

    @Composable
    fun MenuCheckedItem(
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
    fun MenuNotes() {
        MenuSubEntry(
            text = { MenuText(stringResource(Res.string.menu_notes)) },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.NOTES) },
        )
    }

    @Composable
    fun MenuNotesSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val expanded by remember {
            derivedStateOf { state.expanded == PuzzleSubMenu.NOTES }
        }

        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = expanded,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                Res.string.menu_notes,
                onClick = viewModel::closeMenu,
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.menu_notes_clue)) },
                onClick = {
                    viewModel.launchCurrentClueNotes()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.menu_notes_puzzle))
                },
                onClick = {
                    viewModel.launchPuzzleNotes()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.menu_flag_clue_toggle))
                },
                onClick = {
                    viewModel.toggleClueFlag()
                    viewModel.dismissMenu()
                }
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.menu_flag_cell_toggle))
                },
                onClick = {
                    viewModel.toggleCellFlag()
                    viewModel.dismissMenu()
                }
            )
        }
    }

    @Composable
    fun MenuScratchMode() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val isScratchMode by remember {
            derivedStateOf { state.isScratchMode }
        }
        MenuCheckedItem(
            checked = isScratchMode,
            text = { MenuText(stringResource(Res.string.scratch_mode)) },
            onClick = {
                viewModel.toggleScratchMode()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    fun MenuSpecialEntry() {
        DropdownMenuItem(
            text = { MenuText(stringResource(Res.string.special_entry)) },
            onClick = {
                viewModel.doSpecialEntry()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    fun MenuShowErrors() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val enabled by remember {
            derivedStateOf { state.showErrorsState.enabled }
        }
        if (!enabled)
            return


        val isShowing by remember {
            derivedStateOf { state.showErrorsState.isShowing }
        }
        MenuSubEntry(
            text = {
                if (isShowing)
                    MenuText(stringResource(Res.string.showing_errors))
                else
                    MenuText(stringResource(Res.string.show_errors))
            },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.SHOW_ERRORS) },
        )
    }

    @Composable
    fun MenuShowErrorsSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val showErrorsState by remember {
            derivedStateOf { state.showErrorsState }
        }
        if (!showErrorsState.enabled)
            return

        val expanded by remember {
            derivedStateOf { state.expanded == PuzzleSubMenu.SHOW_ERRORS }
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = expanded,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                Res.string.show_errors,
                onClick = viewModel::closeMenu,
            )
            MenuCheckedItem(
                checked = showErrorsState.isShowingErrorsCursor,
                text = {
                    MenuText(stringResource(Res.string.show_errors_cursor))
                },
                onClick = {
                    viewModel.toggleShowErrorsCursor()
                    viewModel.dismissMenu()
                }
            )
            MenuCheckedItem(
                checked = showErrorsState.isShowingErrorsClue,
                text = { MenuText(stringResource(Res.string.show_errors_clue)) },
                onClick = {
                    viewModel.toggleShowErrorsClue()
                    viewModel.dismissMenu()
                }
            )
            MenuCheckedItem(
                checked = showErrorsState.isShowingErrorsGrid,
                text = {
                    MenuText(stringResource(Res.string.show_errors_full_grid))
                },
                onClick = {
                    viewModel.toggleShowErrorsGrid()
                    viewModel.dismissMenu()
                }
            )
        }
    }

    @Composable
    fun MenuReveal() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val canReveal by remember { derivedStateOf { state.canReveal } }
        if (!canReveal)
            return

        MenuSubEntry(
            text = { MenuText(stringResource(Res.string.reveal)) },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.REVEAL) },
        )
    }

    @Composable
    fun MenuRevealSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val canReveal by remember { derivedStateOf { state.canReveal } }
        if (!canReveal)
            return

        val expanded by remember {
            derivedStateOf { state.expanded == PuzzleSubMenu.REVEAL }
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = expanded,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                Res.string.reveal,
                onClick = viewModel::closeMenu,
            )
            val canRevealInitialBoxLetter by remember {
                derivedStateOf { state.canRevealInitialBoxLetter }
            }
            if (canRevealInitialBoxLetter) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(Res.string.initial_letter))
                    },
                    onClick = {
                        viewModel.revealInitialLetter()
                        viewModel.dismissMenu()
                    },
                )
            }
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.letter)) },
                onClick = {
                    viewModel.revealLetter()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.word)) },
                onClick = {
                    viewModel.revealWord()
                    viewModel.dismissMenu()
                },
            )
            val canRevealInitialLetters by remember {
                derivedStateOf { state.canRevealInitialLetters }
            }
            if (canRevealInitialLetters) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(Res.string.initial_letters))
                    },
                    onClick = {
                        viewModel.revealInitialLetters()
                        viewModel.dismissMenu()
                    },
                )
            }
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.errors)) },
                onClick = {
                    viewModel.revealErrors()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.puzzle)) },
                onClick = {
                    viewModel.revealPuzzle()
                    viewModel.dismissMenu()
                },
            )
        }
    }

    @Composable
    fun MenuZoom() {
        MenuSubEntry(
            text = { MenuText(stringResource(Res.string.zoom)) },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.ZOOM) },
        )
    }

    @Composable
    fun MenuZoomSub(
        onZoomIn : () -> Unit,
        onZoomInMax : () -> Unit,
        onZoomOut : () -> Unit,
        onZoomFit : () -> Unit,
        onZoomReset : () -> Unit,
    ) {
        val state by viewModel.menuState.collectAsStateWithLifecycle()

        val expanded by remember {
            derivedStateOf { state.expanded == PuzzleSubMenu.ZOOM }
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = expanded,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                Res.string.zoom,
                onClick = viewModel::closeMenu,
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.zoom_in)) },
                onClick = {
                    onZoomIn()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.zoom_in_max)) },
                onClick = {
                    onZoomInMax()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.zoom_out))
                },
                onClick = {
                    onZoomOut()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.fit_to_screen)) },
                onClick = {
                    onZoomFit()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.zoom_reset)) },
                onClick = {
                    onZoomReset()
                    viewModel.dismissMenu()
                },
            )
        }
    }

    @Composable
    fun MenuExternalTools() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val hasExternal by remember {
            derivedStateOf { state.externalToolsState.hasExternal }
        }
        if (!hasExternal)
            return

        MenuSubEntry(
            text = { MenuText(stringResource(Res.string.external_tools)) },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.EXTERNAL_TOOLS) },
        )
    }

    /**
     * External tools sub menu
     *
     * @param onCallExternalDictionary call back for when external dictionary
     * called
     * @param onCallCrosswordSolver call back when crossword solver
     * called
     * @param showAnagramSolver whether to show the anagram solver if enabled
     * in settings.
     * @param onCallAnagramSolver call back when anagram solver called (does
     * nothing if null)
     */
    @Composable
    fun MenuExternalToolsSub(
        onCallExternalDictionary : () -> Unit
            = viewModel::callExternalDictionary,
        onCallCrosswordSolver : () -> Unit = viewModel::callCrosswordSolver,
        showAnagramSolver : Boolean = false,
        onCallAnagramSolver : () -> Unit = { },
    ) {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val externalToolsState by remember {
            derivedStateOf { state.externalToolsState }
        }
        if (!externalToolsState.hasExternal)
            return

        val expanded by remember {
            derivedStateOf { state.expanded == PuzzleSubMenu.EXTERNAL_TOOLS }
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = expanded,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                Res.string.external_tools,
                onClick = viewModel::closeMenu,
            )
            if (externalToolsState.hasExternalDictionary) {
                DropdownMenuItem(
                    text = {
                        MenuText(
                            stringResource(Res.string.help_external_dictionary)
                        )
                    },
                    onClick = {
                        viewModel.dismissMenu()
                        onCallExternalDictionary()
                    },
                )
            }
            if (externalToolsState.hasCrosswordSolver) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(Res.string.help_crossword_solver))
                    },
                    onClick = {
                        viewModel.dismissMenu()
                        onCallCrosswordSolver()
                    },
                )
                if (showAnagramSolver) {
                    DropdownMenuItem(
                        text = {
                            MenuText(
                                stringResource(
                                    Res.string.help_crossword_solver_anagram
                                )
                            )
                        },
                        onClick = {
                            viewModel.dismissMenu()
                            onCallAnagramSolver()
                        },
                    )

                }
            }
            if (externalToolsState.hasLLMAgent) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(Res.string.help_query_llm_agent))
                    },
                    onClick = {
                        viewModel.requestHelpForCurrentClue()
                        viewModel.dismissMenu()
                    },
                )
            }
            if (externalToolsState.hasDuckDuckGo) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(Res.string.help_duckduckgo))
                    },
                    onClick = {
                        viewModel.searchDuckDuckGo()
                        viewModel.dismissMenu()
                    },
                )
            }
            if (externalToolsState.hasFifteenSquared) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(Res.string.help_fifteen_squared))
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
    fun MenuShare() {
        MenuSubEntry(
            text = { MenuText(stringResource(Res.string.share)) },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.SHARE) },
        )
    }

    @Composable
    fun MenuShareSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()

        val expanded by remember {
            derivedStateOf { state.expanded == PuzzleSubMenu.SHARE }
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = expanded,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                Res.string.share,
                onClick = viewModel::closeMenu,
            )

            if ((viewModel.getPuzzle()?.percentComplete ?: 0) == 100) {
                DropdownMenuItem(
                    text = { MenuText(stringResource(Res.string.completed)) },
                    onClick = {
                        viewModel.shareCompletion()
                        viewModel.dismissMenu()
                    },
                )
            }

            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.share_clue)) },
                onClick = {
                    viewModel.shareClue(false)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.share_clue_response))
                },
                onClick = {
                    viewModel.shareClue(true)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.share_puzzle_full))
                },
                onClick = {
                    viewModel.sharePuzzle(false, false)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(
                        stringResource(Res.string.share_puzzle_without_extensions)
                    )
                },
                onClick = {
                    viewModel.sharePuzzle(false, true)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(Res.string.share_puzzle_orig)) },
                onClick = {
                    viewModel.sharePuzzle(true, true)
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(
                        stringResource(Res.string.share_puzzle_open_share_url)
                    )
                },
                onClick = {
                    openShareUrl()
                    viewModel.dismissMenu()
                },
            )
        }
    }

    /**
     * Edit clue menu entry
     *
     * @param onEditClue override for custom on edit behavior
     */
    @Composable
    fun MenuEditClue(
        onEditClue : () -> Unit = viewModel::editClue,
    ) {
        DropdownMenuItem(
            text = { MenuText(stringResource(Res.string.edit_clue)) },
            onClick = {
                viewModel.dismissMenu()
                onEditClue()
            }
        )
    }

    @Composable
    fun MenuHelp() {
        MenuHelp(
            openHelp = { pageOpener.openHTMLPage(HTMLFile.PLAY_HELP) },
            onDismiss = viewModel::dismissMenu,
        )
    }

    @Composable
    fun MenuSettings() {
        MenuSettings(
            openSettings = { pageOpener.openSettingsPage() },
            onDismiss = viewModel::dismissMenu,
        )
    }

    @Composable
    fun MenuPuzzleInfo() {
        DropdownMenuItem(
            text = { MenuText(stringResource(Res.string.info)) },
            onClick = {
                viewModel.showInfoDialog()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    fun MenuSupportPuzzleSource() {
        DropdownMenuItem(
            text = { MenuText(stringResource(Res.string.support_puzzle_source)) },
            onClick = {
                actionSupportSource()
                viewModel.dismissMenu()
            }
        )
    }

    fun doExitPage() {
        utils.inputConnectionMediator.clearReceiver()
        pageOpener.onBack()
    }

    private fun actionSupportSource() {
        viewModel.supportURL?.let { url ->
            pageOpener.openURI(url.toUri())
        }
    }

    private fun openShareUrl() {
        viewModel.shareURL?.let { url ->
            if (!url.isEmpty()) {
                pageOpener.openURI(url.toUri())
            }
        }
    }
}

@Composable
fun PuzzlePage(
    viewModel : PuzzlePageViewModel,
    themeHelper : ThemeHelper,
    utils : NativeFrontendUtils,
    pageOpener : PageOpener,
    body : @Composable PuzzlePageScope.() -> Unit,
) {
    /**
     * Handle events and things that need to be launched because of
     */
    @Composable
    fun Launchers() {
        val announcement
            by viewModel.announceEvent.collectAsStateWithLifecycle()
        LaunchedEffect(announcement) {
            announcement?.let { data ->
                utils.announce(data.message)
                viewModel.clearAnnounceEvent()
            }
        }
        val openClueList
            by viewModel.openClueListEvent.collectAsStateWithLifecycle()
        LaunchedEffect(openClueList) {
            if (openClueList) {
                pageOpener.openClueListPage()
                viewModel.clearOpenClueListEvent()
            }
        }
        val openNotes
            by viewModel.openNotesEvent.collectAsStateWithLifecycle()
        LaunchedEffect(openNotes) {
            openNotes?.let { openNotes ->
                pageOpener.openNotesPage(openNotes.clueID)
                viewModel.clearOpenNotesEvent()
            }
        }
        val externalTool
            by viewModel.externalToolEvent.collectAsStateWithLifecycle()
        LaunchedEffect(externalTool) {
            externalTool?.let { tool ->
                utils.handleExternalToolEvent(tool)
                viewModel.clearExternalToolEvent()
            }
        }
        val sendToast by viewModel.sendToastEvent.collectAsStateWithLifecycle()
        LaunchedEffect(sendToast) {
            sendToast?.let {
                utils.toast(it.text)
                viewModel.clearSendToast()
            }
        }
        val backEvent by viewModel.backEvent.collectAsStateWithLifecycle()
        LaunchedEffect(backEvent) {
            if (backEvent) {
                pageOpener.onBack()
                viewModel.clearBackEvent()
            }
        }
    }

    /**
     * Always call to display dialogs
     */
    @Composable
    fun PuzzleDialogs() {
        val infoDialogViewModel
            by viewModel.infoDialogViewModel.collectAsStateWithLifecycle()
        infoDialogViewModel?.let { infoDialogViewModel ->
            PuzzleInfoDialog(
                viewModel = infoDialogViewModel,
                onClose = viewModel::closePuzzleInfoDialog,
            )
        }

        val finishedDialogViewModel
            by viewModel.finishedDialogViewModel.collectAsStateWithLifecycle()
        finishedDialogViewModel?.let { finishedDialogViewModel ->
            PuzzleFinishedDialog(
                viewModel = finishedDialogViewModel,
                onDone = viewModel::closePuzzleFinishedDialog,
                onShare = viewModel::shareCompletion,
            )
        }

        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val showDialog by remember { derivedStateOf { state.showDialog } }
        when (showDialog) {
            PuzzleDialog.REVEAL_ALL -> {
                OKCancelDialog(
                    title = Res.string.reveal_puzzle,
                    summary = Res.string.are_you_sure,
                    onCancel = viewModel::clearRevealDialog,
                    onOK = viewModel::confirmRevealPuzzle,
                )
            }
            else -> { /* show nothing */ }
        }

        val flagClueDialogState by viewModel
            .flagClueDialogState
            .collectAsStateWithLifecycle()
        flagClueDialogState?.let { flagClueDialogState ->
            ChooseFlagColorDialog(
                selectedColor = flagClueDialogState.flagColor,
                onCancel = viewModel::clearFlagClueDialog,
                onChoose = { color ->
                    viewModel.flagClue(
                        flagClueDialogState.clueID,
                        color,
                    )
                },
                boardColorScheme = themeHelper.getBoardColorScheme(),
            )
        }

        val flagCellDialogState by viewModel
            .flagCellDialogState
            .collectAsStateWithLifecycle()
        flagCellDialogState?.let { flagCellDialogState ->
            ChooseFlagColorDialog(
                selectedColor = flagCellDialogState.flagColor,
                onCancel = viewModel::clearFlagCellDialog,
                onChoose = { color ->
                    viewModel.flagCell(
                        flagCellDialogState.position,
                        color,
                    )
                },
                boardColorScheme = themeHelper.getBoardColorScheme(),
            )
        }

        val editClueDialogState by viewModel
            .editClueDialogState
            .collectAsStateWithLifecycle()
        editClueDialogState?.let { editClueDialogState ->
            StringDialog(
                title = Res.string.edit_clue,
                hint = Res.string.clue_hint,
                value = editClueDialogState.hint,
                onValueChange = { hint ->
                    viewModel.confirmEditClue(
                        editClueDialogState.clueID,
                        hint,
                    )
                },
                onDismissRequest = viewModel::clearEditClueDialog,
            )
        }

        val dialogMessage by viewModel
            .dialogMessage
            .collectAsStateWithLifecycle()
        dialogMessage?.let { dm ->
            OKDialog(
                title = dm.title,
                summary = dm.message,
                onClose = viewModel::clearDialogMessage,
            )
        }

        val appNotFoundDialogState by viewModel
            .appNotFoundDialogState
            .collectAsStateWithLifecycle()
        appNotFoundDialogState?.let { appNotFoundDialogState ->
            TwoButtonDialog(
                title = stringResource(
                    Res.string.external_app_not_installed,
                    appNotFoundDialogState.appName,
                ),
                summary = stringResource(
                    Res.string.external_app_install_msg,
                    appNotFoundDialogState.appName,
                ),
                negativeText= stringResource(Res.string.website),
                onNegative = {
                    viewModel.clearAppNotFoundDialog()
                    pageOpener.openURI(appNotFoundDialogState.appURL.toUri())
                },
                positiveText = stringResource(Res.string.ok),
                onPositive = viewModel::clearAppNotFoundDialog,
            )
        }

        val specialEntryDialogViewModel by viewModel
            .specialEntryDialogViewModel
            .collectAsStateWithLifecycle()
        specialEntryDialogViewModel?.let { specialEntryDialogViewModel ->
            SpecialEntryDialog(
                viewModel = specialEntryDialogViewModel,
                onClose = viewModel::clearSpecialEntry,
            )
        }
    }

    fun doBackAction() {
        if (viewModel.keyboardState.value.hideOnBack)
            viewModel.hideKeyboard(true)
        else
            pageOpener.onBack()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun HandleBack() {
        val keyboardState
            by viewModel.keyboardState.collectAsStateWithLifecycle()
        val hideOnBack by remember {
            derivedStateOf { keyboardState.hideOnBack }
        }
        val backState = rememberNavigationEventState(
            currentInfo = NavigationEventInfo.None,
        )
        NavigationBackHandler(
            state = backState,
            isBackEnabled = hideOnBack,
            onBackCompleted = ::doBackAction,
        )
    }

    val scope = remember {
        PuzzlePageScope(
            viewModel = viewModel,
            themeHelper = themeHelper,
            utils = utils,
            pageOpener = pageOpener,
        )
    }

    DisposableEffect(Unit) {
        utils.setOnVoiceCommandCallback(viewModel::dispatchVoiceCommand)
        utils.setOnAppNotFoundCallback(viewModel::handleAppNotFoundException)
        onDispose {
            utils.setOnVoiceCommandCallback(null)
            utils.setOnAppNotFoundCallback(null)
        }
    }

    LifecycleResumeEffect(Unit) {
        if (!viewModel.canResume) {
            AppLogger.i(TAG, "No puzzle board, puzzle activity finishing.")
            pageOpener.onBack()
        }
        viewModel.startTimer()

        onPauseOrDispose {
            viewModel.stopTimer()
            viewModel.saveBoard()
        }
    }

    HandleBack()

    Launchers()
    body(scope)
    PuzzleDialogs()
}

