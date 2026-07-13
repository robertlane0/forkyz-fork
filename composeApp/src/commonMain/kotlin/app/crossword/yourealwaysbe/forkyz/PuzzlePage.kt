
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_copied
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.completed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.copy_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.edit_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.errors
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_app_install_msg
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_app_not_installed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_tools
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.fit_to_screen
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_crossword_solver
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_crossword_solver_anagram
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_duckduckgo
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_external_dictionary
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_fifteen_squared
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.help_query_llm_agent
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_announce
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_scratch_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ic_voice
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.info
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.initial_letter
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.initial_letters
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.letter
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.mcp_start
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.mcp_stop
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_flag_cell_toggle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_flag_clue_toggle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_hinting
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_notes_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_notes_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.ok
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.promote_scratch_grid
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.promote_scratch_word
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.reveal
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.reveal_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scratch_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_clue_response
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_full
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_open_share_url
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_orig
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.share_puzzle_without_extensions
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors_cursor
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors_filled_word_cells
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors_filled_word_word
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_errors_full_grid
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.showing_errors
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.special_entry
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.support_puzzle_source
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.toggle_scratch_mode
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.unknown_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.voice_command
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.website
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.word
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
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.MovementStrategy
import app.crossword.yourealwaysbe.forkyz.puz.Playboard
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.ShowErrorsMode
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.Word
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.Zone
import app.crossword.yourealwaysbe.forkyz.settings.ActionButtonsMode
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardLayout
import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings
import app.crossword.yourealwaysbe.forkyz.theme.BoardColorScheme
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.WIDE_SCREEN_WIDTH_DP
import app.crossword.yourealwaysbe.forkyz.util.onKeyEventKeyboardBindings
import app.crossword.yourealwaysbe.forkyz.util.onMiddleClick
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
        val fullModifier = modifier
            .onMiddleClick { onLongClick() }
            .combinedClickable(
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

    /**
     * Checks event against configured keyboard shortcuts
     *
     * Calls back on action, returns true if handled. Always assumed
     * handled if matches action for which handler given as arg.
     */
    fun onKeyEventKeyboardBindingsPuzzlePage(
        event : KeyEvent,
        onAnnounceBoxDown : (() -> Unit)? = viewModel::announceBox,
        onAnnounceClueDown : (() -> Unit)? = viewModel::announceClue,
        onBackDown : (() -> Unit)? = ::doExitPage,
        onClueListDown : (() -> Unit)? = null,
        onClueNotesDown : (() -> Unit)? = null,
        onCopyClueDown : (() -> Unit)? = viewModel::copyClue,
        onExitDown : (() -> Unit)? = utils::onExit,
        onFitToScreenDown : (() -> Unit)? = null,
        onHelpDown : (() -> Unit)? = ::openHelp,
        onInsertSpecialDown : (() -> Unit)? = null,
        onNextClueDown : (() -> Unit)? = null,
        onOpenCrosswordSolverDown : (() -> Unit)?
            = viewModel::callCrosswordSolver,
        onOpenDictionaryDown : (() -> Unit)?
            = viewModel::callExternalDictionary,
        onOpenInfoDown : (() -> Unit)? = viewModel::showInfoDialog,
        onOpenShareUrlDown : (() -> Unit)? = ::openShareUrl,
        onPrevClueDown : (() -> Unit)? = null,
        onPuzzleNotesDown : (() -> Unit)? = null,
        onRandomClueDown : (() -> Unit)? = null,
        onRevealErrorsDown : (() -> Unit)? = viewModel::revealErrors,
        onRevealLetterDown : (() -> Unit)? = viewModel::revealLetter,
        onRevealPuzzleDown : (() -> Unit)? = viewModel::revealPuzzle,
        onRevealWordDown : (() -> Unit)? = viewModel::revealWord,
        onSearchDuckduckgoDown : (() -> Unit)? = viewModel::searchDuckDuckGo,
        onSearchFifteenSquaredDown : (() -> Unit)?
            = viewModel::searchFifteenSquared,
        onSettingsDown : (() -> Unit)? = { pageOpener.openSettingsPage() },
        onShareClueDown : (() -> Unit)? = { viewModel.shareClue(false) },
        onShareClueWithBoardDown : (() -> Unit)?
            = { viewModel.shareClue(true) },
        onSharePuzzleDown : (() -> Unit)?
            = { viewModel.sharePuzzle(false, false) },
        onSharePuzzleBlankDown : (() -> Unit)?
            = { viewModel.sharePuzzle(true, false) },
        onSharePuzzleNoIpuzDown : (() -> Unit)?
            = { viewModel.sharePuzzle(false, true) },
        onSharePuzzleOriginalDown : (() -> Unit)?
            = { viewModel.sharePuzzle(true, true) },
        onShuffleAnagramDown : (() -> Unit)? = null,
        onToggleCellFlagDown : (() -> Unit)? = viewModel::toggleCellFlag,
        onToggleClueFlagDown : (() -> Unit)? = viewModel::toggleClueFlag,
        onToggleScratchModeDown : (() -> Unit)? = null,
        onToggleSelectionDown : (() -> Unit)? = null,
        onToggleShowErrorsCursorDown : (() -> Unit)?
            = { viewModel.toggleShowErrorsMode(ShowErrorsMode.CURSOR) },
        onToggleShowErrorsGridDown : (() -> Unit)?
            = { viewModel.toggleShowErrorsMode(ShowErrorsMode.GRID) },
        onToggleShowErrorsWordCellsDown : (() -> Unit)? = {
            viewModel.toggleShowErrorsMode(ShowErrorsMode.FILLED_WORD_CELLS)
        },
        onToggleShowErrorsWordWordDown : (() -> Unit)? = {
            viewModel.toggleShowErrorsMode(ShowErrorsMode.FILLED_WORD_WORD)
        },
        onVoiceInputDown : (() -> Unit)? = utils::launchVoiceInput,
        onZoomInDown : (() -> Unit)? = null,
        onZoomInMaxDown : (() -> Unit)? = null,
        onZoomOutDown : (() -> Unit)? = null,
        onZoomResetDown : (() -> Unit)? = null,
   ) : Boolean {
        return onKeyEventKeyboardBindings(
            event = event,
            binds = viewModel.keyboardBindings,
            onAnnounceBoxDown = onAnnounceBoxDown,
            onAnnounceClueDown = onAnnounceClueDown,
            onBackDown = onBackDown,
            onClueListDown = onClueListDown,
            onClueNotesDown = onClueNotesDown,
            onCopyClueDown = onCopyClueDown,
            onExitDown = onExitDown,
            onFitToScreenDown = onFitToScreenDown,
            onHelpDown = onHelpDown,
            onInsertSpecialDown = onInsertSpecialDown,
            onNextClueDown = onNextClueDown,
            onOpenCrosswordSolverDown = onOpenCrosswordSolverDown,
            onOpenDictionaryDown = onOpenDictionaryDown,
            onOpenInfoDown = onOpenInfoDown,
            onOpenShareUrlDown = onOpenShareUrlDown,
            onPrevClueDown = onPrevClueDown,
            onPuzzleNotesDown = onPuzzleNotesDown,
            onRandomClueDown = onRandomClueDown,
            onRevealErrorsDown = onRevealErrorsDown,
            onRevealLetterDown = onRevealLetterDown,
            onRevealPuzzleDown = onRevealPuzzleDown,
            onRevealWordDown = onRevealWordDown,
            onSearchDuckduckgoDown = onSearchDuckduckgoDown,
            onSearchFifteenSquaredDown = onSearchFifteenSquaredDown,
            onSettingsDown = onSettingsDown,
            onShareClueDown = onShareClueDown,
            onShareClueWithBoardDown = onShareClueWithBoardDown,
            onSharePuzzleDown = onSharePuzzleDown,
            onSharePuzzleBlankDown = onSharePuzzleBlankDown,
            onSharePuzzleNoIpuzDown = onSharePuzzleNoIpuzDown,
            onSharePuzzleOriginalDown = onSharePuzzleOriginalDown,
            onShuffleAnagramDown = onShuffleAnagramDown,
            onToggleCellFlagDown = onToggleCellFlagDown,
            onToggleClueFlagDown = onToggleClueFlagDown,
            onToggleScratchModeDown = onToggleScratchModeDown,
            onToggleSelectionDown = onToggleSelectionDown,
            onToggleShowErrorsCursorDown = onToggleShowErrorsCursorDown,
            onToggleShowErrorsGridDown = onToggleShowErrorsGridDown,
            onToggleShowErrorsWordCellsDown = onToggleShowErrorsWordCellsDown,
            onToggleShowErrorsWordWordDown = onToggleShowErrorsWordWordDown,
            onVoiceInputDown = onVoiceInputDown,
            onZoomInDown = onZoomInDown,
            onZoomInMaxDown = onZoomInMaxDown,
            onZoomOutDown = onZoomOutDown,
            onZoomResetDown = onZoomResetDown,
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
    fun ActionButtons(inTitleBar : Boolean) {
        val state by viewModel.actionButtonState.collectAsStateWithLifecycle()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val menuState by viewModel.menuState.collectAsStateWithLifecycle()

        val displayInTitleBar = when (uiState.actionButtonsMode) {
            ActionButtonsMode.ABM_BOTTOM -> false
            ActionButtonsMode.ABM_TOP_ALWAYS -> true
            ActionButtonsMode.ABM_TOP_WIDE -> {
                utils.displayWidth >= WIDE_SCREEN_WIDTH_DP
            }
        }
        if (displayInTitleBar != inTitleBar)
            return

        val plainButtonColors = if (inTitleBar) {
            IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Transparent,
            )
        } else {
            IconButtonDefaults.filledIconButtonColors()
        }

        val plainModifier = if (inTitleBar) {
            Modifier
        } else {
            Modifier.padding(horizontal=5.dp)
        }

        if (state.hasButtons) {
            val rowModifier = if (inTitleBar) {
                Modifier
            } else {
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 5.dp)
            }

            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (state.announceClue) {
                    FilledIconButton(
                        modifier = plainModifier.width(50.dp),
                        colors = plainButtonColors,
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
                if (state.toggleScratchMode) {
                    val isScratchMode by remember {
                        derivedStateOf { menuState.isScratchMode }
                    }
                    val colors = if (isScratchMode) {
                        IconButtonDefaults.filledIconButtonColors(
                            containerColor
                                = MaterialTheme.colorScheme.primaryContainer,
                        )
                    } else {
                        plainButtonColors
                    }

                    FilledIconButton(
                        modifier = plainModifier.width(50.dp),
                        colors = colors,
                        onClick = { viewModel.toggleScratchMode() },
                    ) {
                        Icon(
                            modifier = Modifier.width(50.dp),
                            painter
                                = painterResource(Res.drawable.ic_scratch_mode),
                            contentDescription = stringResource(
                                Res.string.toggle_scratch_mode,
                            ),
                        )
                    }
                }
                if (state.voiceInput) {
                    FilledIconButton(
                        modifier = plainModifier.width(50.dp),
                        colors = plainButtonColors,
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
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.promote_scratch_word))
                },
                onClick = {
                    viewModel.promoteScratchCurrentWord()
                    viewModel.dismissMenu()
                }
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.promote_scratch_grid))
                },
                onClick = {
                    viewModel.promoteScratchGrid()
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
    fun MenuHint() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val canHint by remember {
            derivedStateOf { state.canReveal || state.showErrorsState.enabled }
        }
        if (!canHint)
            return

        val isShowingErrors by remember {
            derivedStateOf { state.showErrorsState.isShowing }
        }
        MenuSubEntry(
            text = {
                if (isShowingErrors) {
                    MenuText(stringResource(Res.string.menu_hinting))
                } else {
                    MenuText(stringResource(Res.string.menu_hint))
                }
            },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.HINT) },
        )
    }

    @Composable
    fun MenuHintSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val canReveal by remember { derivedStateOf { state.canReveal } }
        if (!canReveal)
            return

        val expanded by remember {
            derivedStateOf { state.expanded == PuzzleSubMenu.HINT }
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = expanded,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            val showErrorsState by remember {
                derivedStateOf { state.showErrorsState }
            }
            val isShowingErrors by remember {
                derivedStateOf { showErrorsState.isShowing }
            }

            val menuHintHeading = if (isShowingErrors)
                Res.string.menu_hinting
            else
                Res.string.menu_hint

            MenuDropdownHeading(
                titleId = menuHintHeading,
                onClick = viewModel::closeMenu,
            )

            HorizontalDivider()

            val showErrorsHeading = if (isShowingErrors)
                Res.string.showing_errors
            else
                Res.string.show_errors

            MenuDropdownHeading(
                titleId = showErrorsHeading,
                withBack = false,
            )
            MenuCheckedItem(
                checked = showErrorsState.isShowingErrorsCursor,
                text = {
                    MenuText(stringResource(Res.string.show_errors_cursor))
                },
                onClick = {
                    viewModel.toggleShowErrorsMode(ShowErrorsMode.CURSOR)
                    viewModel.dismissMenu()
                }
            )
            MenuCheckedItem(
                checked = showErrorsState.isShowingErrorsFilledWordCells,
                text = {
                    MenuText(
                        stringResource(
                            Res.string.show_errors_filled_word_cells,
                        ),
                    )
                },
                onClick = {
                    viewModel.toggleShowErrorsMode(
                        ShowErrorsMode.FILLED_WORD_CELLS,
                    )
                    viewModel.dismissMenu()
                }
            )
            MenuCheckedItem(
                checked = showErrorsState.isShowingErrorsFilledWordWord,
                text = {
                    MenuText(
                        stringResource(Res.string.show_errors_filled_word_word),
                    )
                },
                onClick = {
                    viewModel.toggleShowErrorsMode(
                        ShowErrorsMode.FILLED_WORD_WORD,
                    )
                    viewModel.dismissMenu()
                }
            )
            MenuCheckedItem(
                checked = showErrorsState.isShowingErrorsGrid,
                text = {
                    MenuText(stringResource(Res.string.show_errors_full_grid))
                },
                onClick = {
                    viewModel.toggleShowErrorsMode(ShowErrorsMode.GRID)
                    viewModel.dismissMenu()
                }
            )

            HorizontalDivider()

            MenuDropdownHeading(
                titleId = Res.string.reveal,
                withBack = false,
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
            if (externalToolsState.hasMCPEnabled) {
                val isMCPRunning = externalToolsState.isMCPRunning
                val text = if (isMCPRunning)
                    Res.string.mcp_stop
                else
                    Res.string.mcp_start

                DropdownMenuItem(
                    text = { MenuText(stringResource(text)) },
                    onClick = {
                        if (isMCPRunning)
                            viewModel.stopMCP()
                        else
                            viewModel.startMCP()
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
                text = { MenuText(stringResource(Res.string.copy_clue)) },
                onClick = {
                    viewModel.copyClue()
                    viewModel.dismissMenu()
                },
            )

            if (utils.canShareFiles) {
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
                            stringResource(
                                Res.string.share_puzzle_without_extensions,
                            )
                        )
                    },
                    onClick = {
                        viewModel.sharePuzzle(false, true)
                        viewModel.dismissMenu()
                    },
                )
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(Res.string.share_puzzle_orig))
                    },
                    onClick = {
                        viewModel.sharePuzzle(true, true)
                        viewModel.dismissMenu()
                    },
                )
                DropdownMenuItem(
                    text = {
                        MenuText(
                            stringResource(
                                Res.string.share_puzzle_open_share_url,
                            )
                        )
                    },
                    onClick = {
                        openShareUrl()
                        viewModel.dismissMenu()
                    },
                )
            }
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
            openHelp = ::openHelp,
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
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        val hasSupport by remember { derivedStateOf { state.hasSupportURL} }
        if (hasSupport) {
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.support_puzzle_source))
                },
                onClick = {
                    actionSupportSource()
                    viewModel.dismissMenu()
                }
            )
        }
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

    private fun openHelp() {
        pageOpener.openHTMLPage(HTMLFile.PLAY_HELP)
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
        val copyClueEvent
            by viewModel.copyClueEvent.collectAsStateWithLifecycle()
        val clueCopiedMsg = stringResource(Res.string.clue_copied)
        // Suppress deprecation because Clipboard does not have an KMP API
        // as of June 2026
        @Suppress("deprecation")
        val clipboardManager = LocalClipboardManager.current
        LaunchedEffect(copyClueEvent) {
            copyClueEvent?.let {
                clipboardManager.setText(htmlToAnnotatedString(it.text))
                viewModel.clearCopyClueEvent()
                utils.toast(clueCopiedMsg)
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
            utils.removeOnVoiceCommandCallback(viewModel::dispatchVoiceCommand)
            utils.removeOnAppNotFoundCallback(
                viewModel::handleAppNotFoundException,
            )
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

