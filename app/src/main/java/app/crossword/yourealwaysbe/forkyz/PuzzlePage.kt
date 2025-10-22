
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import android.net.Uri
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
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
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
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

class PuzzlePageScope(
    private val viewModel : PuzzlePageViewModel,
    private val themeHelper : ThemeHelper,
    private val inputConnectionMediator : InputConnectionMediator,
    private val displayWidth : Int,
    private val displayHeight : Int,
    private val isPortrait : Boolean,
    private val openHTMLPage : (Int) -> Unit,
    private val openSettingsPage : () -> Unit,
    private val openURI : (Uri) -> Unit,
    private val onBack : () -> Unit,
    private val setStatusBarColor : (Color) -> Unit,
    private val launchVoiceInput : () -> Unit,
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
        LifecycleResumeEffect(state.showTimer) {
            scope.launch {
                while (state.showTimer) {
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

        val color = if (state.showErrorsState.isShowing)
            themeHelper.cheatedColor
        else
            themeHelper.statusBarColor

        // for older devices where Compose doesn't draw over status bar
        // for some reason
        LaunchedEffect(color) {
            setStatusBarColor(color)
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
        val text = clueText ?: stringResource(R.string.unknown_hint)

        // Text doesn't support autoSize yet, so rebuild BasicText
        // from (or close enough)
        // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material/material/src/commonMain/kotlin/androidx/compose/material/Text.kt
        val color = LocalContentColor.current
        val clueSize = integerResource(R.integer.clue_text_size)
        BasicText(
            modifier = fullModifier,
            text = AnnotatedString.fromHtml(text),
            autoSize =  TextAutoSize.StepBased(
                minFontSize = 5.sp,
                maxFontSize = clueSize.sp,
                stepSize = 1.sp,
            ),
            color = { color },
        )
    }

    fun onKeyEventPuzzlePage(event : KeyEvent) : Boolean {
        return when (event.key) {
            Key.Escape -> {
                if (event.type == KeyEventType.KeyUp)
                    onBack()
                return true
            }
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

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun VoiceButtons() {
        val state by viewModel.voiceState.collectAsStateWithLifecycle()

        val showVoiceButton = state.showVoiceButton
        val showAnnounceButton = state.showAnnounceButton

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
    fun Keyboard(
        showSpecialKeys : Boolean = false,
        onSpecialKeyDown : (SpecialKey) -> Unit = { },
        onSpecialKeyUp : (SpecialKey) -> Unit = { },
    ) {
        val state by viewModel.keyboardState.collectAsStateWithLifecycle()

        LaunchedEffect(state.needsRefresh) {
            if (state.needsRefresh) {
                if (state.visibility == KeyboardVisibility.NATIVE) {
                    inputConnectionMediator.showNative(state.forceCaps)
                } else {
                    inputConnectionMediator.hideNative()
                }
                if (state.visibility == KeyboardVisibility.FORKYZ) {
                    inputConnectionMediator.focusReceiver()
                }
                viewModel.markKeyboardRefreshed()
            }
        }

        if (state.visibility == KeyboardVisibility.FORKYZ) {
            ForkyzKeyboard(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                inputConnection
                    = inputConnectionMediator.forkyzKeyboardInputConnection,
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
            text = { MenuText(stringResource(R.string.menu_notes)) },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.NOTES) },
        )
    }

    @Composable
    fun MenuNotesSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()

        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = state.expanded == PuzzleSubMenu.NOTES,
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
    fun MenuScratchMode() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        MenuCheckedItem(
            checked = state.isScratchMode,
            text = { MenuText(stringResource(R.string.scratch_mode)) },
            onClick = {
                viewModel.toggleScratchMode()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    fun MenuSpecialEntry() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.special_entry)) },
            onClick = {
                viewModel.doSpecialEntry()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    fun MenuShowErrors() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        if (!state.showErrorsState.enabled)
            return

        MenuSubEntry(
            text = {
                if (state.showErrorsState.isShowing)
                    MenuText(stringResource(R.string.showing_errors))
                else
                    MenuText(stringResource(R.string.show_errors))
            },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.SHOW_ERRORS) },
        )
    }

    @Composable
    fun MenuShowErrorsSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        if (!state.showErrorsState.enabled)
            return

        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = state.expanded == PuzzleSubMenu.SHOW_ERRORS,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.show_errors,
                onClick = viewModel::closeMenu,
            )
            MenuCheckedItem(
                checked = state.showErrorsState.isShowingErrorsCursor,
                text = {
                    MenuText(stringResource(R.string.show_errors_cursor))
                },
                onClick = {
                    viewModel.toggleShowErrorsCursor()
                    viewModel.dismissMenu()
                }
            )
            MenuCheckedItem(
                checked = state.showErrorsState.isShowingErrorsClue,
                text = { MenuText(stringResource(R.string.show_errors_clue)) },
                onClick = {
                    viewModel.toggleShowErrorsClue()
                    viewModel.dismissMenu()
                }
            )
            MenuCheckedItem(
                checked = state.showErrorsState.isShowingErrorsGrid,
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
    fun MenuReveal() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        if (!state.canReveal)
            return

        MenuSubEntry(
            text = { MenuText(stringResource(R.string.reveal)) },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.REVEAL) },
        )
    }

    @Composable
    fun MenuRevealSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()
        if (!state.canReveal)
            return

        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = state.expanded == PuzzleSubMenu.REVEAL,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.reveal,
                onClick = viewModel::closeMenu,
            )
            if (state.canRevealInitialBoxLetter) {
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
            if (state.canRevealInitialLetters) {
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
                    viewModel.revealPuzzle()
                    viewModel.dismissMenu()
                },
            )
        }
    }

    @Composable
    fun MenuZoom() {
        MenuSubEntry(
            text = { MenuText(stringResource(R.string.zoom)) },
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

        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = state.expanded == PuzzleSubMenu.ZOOM,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.zoom,
                onClick = viewModel::closeMenu,
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.zoom_in)) },
                onClick = {
                    onZoomIn()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.zoom_in_max)) },
                onClick = {
                    onZoomInMax()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(R.string.zoom_out))
                },
                onClick = {
                    onZoomOut()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.fit_to_screen)) },
                onClick = {
                    onZoomFit()
                    viewModel.dismissMenu()
                },
            )
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.zoom_reset)) },
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
        if (!state.externalToolsState.hasExternal)
            return

        MenuSubEntry(
            text = { MenuText(stringResource(R.string.external_tools)) },
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
        if (!state.externalToolsState.hasExternal)
            return

        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = state.expanded == PuzzleSubMenu.EXTERNAL_TOOLS,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.external_tools,
                onClick = viewModel::closeMenu,
            )
            if (state.externalToolsState.hasExternalDictionary) {
                DropdownMenuItem(
                    text = {
                        MenuText(
                            stringResource(R.string.help_external_dictionary)
                        )
                    },
                    onClick = {
                        viewModel.dismissMenu()
                        onCallExternalDictionary()
                    },
                )
            }
            if (state.externalToolsState.hasCrosswordSolver) {
                DropdownMenuItem(
                    text = {
                        MenuText(stringResource(R.string.help_crossword_solver))
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
                                    R.string.help_crossword_solver_anagram
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
            if (state.externalToolsState.hasChatGPT) {
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
            if (state.externalToolsState.hasDuckDuckGo) {
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
            if (state.externalToolsState.hasFifteenSquared) {
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
    fun MenuShare() {
        MenuSubEntry(
            text = { MenuText(stringResource(R.string.share)) },
            onClick = { viewModel.expandMenu(PuzzleSubMenu.SHARE) },
        )
    }

    @Composable
    fun MenuShareSub() {
        val state by viewModel.menuState.collectAsStateWithLifecycle()

        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = state.expanded == PuzzleSubMenu.SHARE,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuDropdownHeading(
                R.string.share,
                onClick = viewModel::closeMenu,
            )

            if ((viewModel.getPuzzle()?.percentComplete ?: 0) == 100) {
                DropdownMenuItem(
                    text = { MenuText(stringResource(R.string.completed)) },
                    onClick = {
                        viewModel.shareCompletion()
                        viewModel.dismissMenu()
                    },
                )
            }

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
            text = { MenuText(stringResource(R.string.edit_clue)) },
            onClick = {
                viewModel.dismissMenu()
                onEditClue()
            }
        )
    }

    @Composable
    fun MenuHelp() {
        MenuHelp(
            openHelp = { openHTMLPage(R.raw.play) },
            onDismiss = viewModel::dismissMenu,
        )
    }

    @Composable
    fun MenuSettings() {
        MenuSettings(
            openSettings = openSettingsPage,
            onDismiss = viewModel::dismissMenu,
        )
    }

    @Composable
    fun MenuPuzzleInfo() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.info)) },
            onClick = {
                viewModel.showInfoDialog()
                viewModel.dismissMenu()
            }
        )
    }

    @Composable
    fun MenuSupportPuzzleSource() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.support_puzzle_source)) },
            onClick = {
                actionSupportSource()
                viewModel.dismissMenu()
            }
        )
    }

    fun doExitPage() {
        inputConnectionMediator.clearReceiver()
        onBack()
    }

    private fun actionSupportSource() {
        viewModel.supportURL?.let { url ->
            openURI(url.toUri())
        }
    }

    private fun openShareUrl() {
        viewModel.shareURL?.let { url ->
            if (!url.isEmpty()) {
                openURI(url.toUri())
            }
        }
    }
}

@Composable
fun PuzzlePage(
    viewModel : PuzzlePageViewModel,
    themeHelper : ThemeHelper,
    inputConnectionMediator : InputConnectionMediator,
    displayWidth : Int,
    displayHeight : Int,
    isPortrait : Boolean,
    openClueListPage : () -> Unit,
    openNotesPage : (ClueID?) -> Unit,
    openHTMLPage : (Int) -> Unit,
    openSettingsPage : () -> Unit,
    openURI : (Uri) -> Unit,
    onFinish : () -> Unit,
    onBack : () -> Unit,
    toast : (String) -> Unit,
    handleExternalToolEvent : (ExternalToolData) -> Unit,
    setOnAppNotFoundCallback : (((AppNotFoundException) -> Unit)?) -> Unit,
    setStatusBarColor : (Color) -> Unit,
    launchVoiceInput : () -> Unit,
    setOnVoiceCommandCallback : (((List<String>?) -> Unit)?) -> Unit,
    // TODO: remove need for View
    announce : (AnnounceData, View) -> Unit,
    body : @Composable PuzzlePageScope.() -> Unit,
) {
    /**
     * Handle events and things that need to be launched because of
     */
    @Composable
    fun Launchers() {
        val announcement
            by viewModel.announceEvent.collectAsStateWithLifecycle()
        val view = LocalView.current
        LaunchedEffect(announcement) {
            announcement?.let { data ->
                announce(data, view)
                viewModel.clearAnnounceEvent()
            }
        }
        val openClueList
            by viewModel.openClueListEvent.collectAsStateWithLifecycle()
        LaunchedEffect(openClueList) {
            if (openClueList) {
                openClueListPage()
                viewModel.clearOpenClueListEvent()
            }
        }
        val openNotes
            by viewModel.openNotesEvent.collectAsStateWithLifecycle()
        LaunchedEffect(openNotes) {
            openNotes?.let { openNotes ->
                openNotesPage(openNotes.clueID)
                viewModel.clearOpenNotesEvent()
            }
        }
        val externalTool
            by viewModel.externalToolEvent.collectAsStateWithLifecycle()
        LaunchedEffect(externalTool) {
            externalTool?.let { tool ->
                handleExternalToolEvent(tool)
                viewModel.clearExternalToolEvent()
            }
        }
        val sendToast by viewModel.sendToastEvent.collectAsStateWithLifecycle()
        LaunchedEffect(sendToast) {
            sendToast?.let {
                toast(it.text)
                viewModel.clearSendToast()
            }
        }
        val backEvent by viewModel.backEvent.collectAsStateWithLifecycle()
        LaunchedEffect(backEvent) {
            if (backEvent) {
                onBack()
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
        when (state.showDialog) {
            PuzzleDialog.REVEAL_ALL -> {
                OKCancelDialog(
                    title = R.string.reveal_puzzle,
                    summary = R.string.are_you_sure,
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
                title = R.string.edit_clue,
                hint = R.string.clue_hint,
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
                    R.string.external_app_not_installed,
                    appNotFoundDialogState.appName,
                ),
                summary = stringResource(
                    R.string.external_app_install_msg,
                    appNotFoundDialogState.appName,
                ),
                negativeText= stringResource(R.string.website),
                onNegative = {
                    viewModel.clearAppNotFoundDialog()
                    openURI(appNotFoundDialogState.appURL.toUri())
                },
                positiveText = stringResource(android.R.string.ok),
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
            onBack()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun HandleBack() {
        val keyboardState
            by viewModel.keyboardState.collectAsStateWithLifecycle()
        BackHandler(
            enabled = keyboardState.hideOnBack,
            onBack = ::doBackAction,
        )
    }

    val scope = remember {
        PuzzlePageScope(
            viewModel = viewModel,
            themeHelper = themeHelper,
            inputConnectionMediator = inputConnectionMediator,
            displayWidth = displayWidth,
            displayHeight = displayHeight,
            isPortrait = isPortrait,
            openHTMLPage = openHTMLPage,
            openSettingsPage = openSettingsPage,
            openURI = openURI,
            onBack = ::doBackAction,
            setStatusBarColor = setStatusBarColor,
            launchVoiceInput = launchVoiceInput,
        )
    }

    DisposableEffect(Unit) {
        setOnVoiceCommandCallback(viewModel::dispatchVoiceCommand)
        setOnAppNotFoundCallback(viewModel::handleAppNotFoundException)
        onDispose {
            setOnVoiceCommandCallback(null)
            setOnAppNotFoundCallback(null)
        }
    }

    LifecycleResumeEffect(Unit) {
        if (!viewModel.canResume) {
            Log.i(TAG, "No puzzle board, puzzle activity finishing.")
            onFinish()
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

