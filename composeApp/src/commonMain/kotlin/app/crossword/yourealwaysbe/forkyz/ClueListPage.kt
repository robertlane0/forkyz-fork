
package app.crossword.yourealwaysbe.forkyz

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clues_list_label
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.current_word
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.open_clue_list
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.open_clue_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.overflow
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.select_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.show_all_words_clue_list
import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuExit
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.MovementStrategy
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.keyEventToResponse
import app.crossword.yourealwaysbe.forkyz.view.BoxInputState
import app.crossword.yourealwaysbe.forkyz.view.ClueTabs
import app.crossword.yourealwaysbe.forkyz.view.OrdinaryKey
import app.crossword.yourealwaysbe.forkyz.view.WordEdit
import app.crossword.yourealwaysbe.forkyz.view.rememberBoardTextMeasurer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClueListPage(
    viewModel : ClueListPageViewModel,
    themeHelper : ThemeHelper,
    utils : NativeFrontendUtils,
    pageOpener : PageOpener,
    isTop : Boolean,
) {
    PuzzlePage(
        viewModel = viewModel,
        themeHelper = themeHelper,
        utils = utils,
        pageOpener = pageOpener,
    ) {
        val textMeasurer = rememberBoardTextMeasurer()
        // the top board that will always have focus (hidden out of view
        // but still focused in show all words)
        val boxInputState = remember { BoxInputState() }

        fun onKeyEvent(event : KeyEvent) : Boolean {
            val isDown = event.type == KeyEventType.KeyDown

            val handled = onKeyEventKeyboardBindingsPuzzlePage(
                event = event,
                onClueNotesDown = viewModel::launchCurrentClueNotes,
                onInsertSpecialDown = viewModel::doSpecialEntry,
                onNextClueDown = viewModel::moveDown,
                onPrevClueDown = viewModel::moveUp,
                onPuzzleNotesDown = viewModel::launchPuzzleNotes,
                onRandomClueDown = viewModel::pickRandomUnfilledClue,
                onToggleScratchModeDown = viewModel::toggleScratchMode,
            )
            if (handled)
                return true

            when (event.key) {
                Key.DirectionLeft -> {
                    if (isDown)
                        viewModel.moveLeft()
                    return true
                }
                Key.DirectionRight -> {
                    if (isDown)
                        viewModel.moveRight()
                    return true
                }
                Key.DirectionUp -> {
                    if (isDown)
                        viewModel.moveUp();
                    return true
                }
                Key.DirectionDown -> {
                    if (isDown)
                        viewModel.moveDown()
                    return true
                }
                Key.Delete, Key.Backspace -> {
                    if (isDown)
                        viewModel.deleteResponse()
                    return true
                }
                Key.Spacebar -> {
                    if (isDown)
                        viewModel.playSpace()
                    return true
                }
            }

            val response = keyEventToResponse(event)
            if (response != null) {
                if (isDown)
                    viewModel.playResponse(response)
                return true
            } else {
                return onKeyEventPuzzlePage(event)
            }
        }

        fun onOrdinaryKeyDown(key : OrdinaryKey) {
            when (key) {
                OrdinaryKey.KEY_DELETE -> viewModel.deleteResponse()
                OrdinaryKey.KEY_DOWN -> viewModel.moveDown()
                OrdinaryKey.KEY_LEFT -> viewModel.moveLeft()
                OrdinaryKey.KEY_RIGHT -> viewModel.moveRight()
                OrdinaryKey.KEY_SPACE -> viewModel.playSpace()
                OrdinaryKey.KEY_UP -> viewModel.moveUp()
            }
        }

        @Composable
        fun MenuShowAllWords() {
            val showAllWords by viewModel.showAllWords.collectAsStateWithLifecycle()

            MenuCheckedItem(
                checked = showAllWords,
                text = {
                    MenuText(stringResource(Res.string.show_all_words_clue_list))
                },
                onClick = {
                    viewModel.toggleShowAllWords()
                    viewModel.dismissMenu()
                },
            )
        }

        @Composable
        fun OverflowMenu() {
            IconButton(onClick = { viewModel.expandMenu(PuzzleSubMenu.MAIN) }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.overflow),
                )
            }


            val state by viewModel.menuState.collectAsStateWithLifecycle()
            val expanded by remember {
                derivedStateOf {
                    state.expanded == PuzzleSubMenu.MAIN
                }
            }
            DropdownMenu(
                modifier = BaseMenuModifier,
                expanded = expanded,
                onDismissRequest = viewModel::dismissMenu,
            ) {
                MenuNotes()
                MenuScratchMode()
                MenuSpecialEntry()
                MenuHint()
                MenuExternalTools()
                MenuShowAllWords()
                MenuEditClue()
                MenuPuzzleInfo()
                MenuSupportPuzzleSource()
                MenuShare()
                MenuHelp()
                MenuSettings()
                MenuExit(utils)
            }
            MenuNotesSub()
            MenuHintSub()
            MenuExternalToolsSub()
            MenuShareSub()
        }

        @Composable
        fun TopAppBar() {
            themeHelper.ForkyzTopAppBar(
                title = { Text(stringResource(Res.string.clues_list_label)) },
                onBack = { doExitPage() },
                actions = { OverflowMenu() },
            )
        }

        @Composable
        fun ClueLists(modifier : Modifier, showAllWords : Boolean) {
            val boardColorScheme = themeHelper.getBoardColorScheme()
            ClueTabs(
                modifier = modifier,
                textMeasurer = textMeasurer,
                viewModel = viewModel.clueTabsViewModel,
                isPortrait = utils.isPortrait,
                onClueClick = { cid ->
                    boxInputState.requestFocus()
                    viewModel.clickClueTabs(cid)
                },
                onClueClickDescription = stringResource(Res.string.select_clue),
                onClueLongClick = { cid ->
                    viewModel.selectClue(cid)
                    viewModel.launchClueNotes(cid)
                },
                onClueLongClickDescription
                    = stringResource(Res.string.open_clue_notes),
                onClueBoardClick = { cid ->
                    boxInputState.requestFocus()
                    viewModel.clickClueTabs(cid)
                },
                onClueBoardClickDescription
                    = stringResource(Res.string.select_clue),
                showAllWords = showAllWords,
                boardColorScheme = boardColorScheme,
            )
        }

        @Composable
        fun ActivityBody(modifier : Modifier) {
            BoxWithConstraints(modifier = modifier) {
                val boardMaxWidth = maxWidth
                Column() {
                    val showAllWords
                        by viewModel.showAllWords.collectAsStateWithLifecycle()

                    WordEdit(
                        textMeasurer = textMeasurer,
                        maxWidth = boardMaxWidth,
                        state = boxInputState,
                        inputConnectionMediator = utils.inputConnectionMediator,
                        colors = themeHelper.getBoardColorScheme(),
                        viewModel = viewModel.wordEditViewModel,
                        onTap = { row, col ->
                            boxInputState.requestFocus()
                            viewModel.clickBoard(row, col, fixClue = true)
                        },
                        onLongPress = { _, _ ->
                            viewModel.launchCurrentClueNotes()
                        },
                        onLongPressDescription
                            = stringResource(Res.string.open_clue_list),
                        incognitoMode = showAllWords,
                        contentDescription
                            = stringResource(Res.string.current_word),
                        onOrdinaryKeyDown = ::onOrdinaryKeyDown,
                    )
                    ClueLists(
                        Modifier.fillMaxWidth().weight(1.0F),
                        showAllWords,
                    )
                    ActionButtons()
                    Keyboard()

                    LaunchedEffect(isTop) {
                        if (isTop)
                            boxInputState.requestFocus()
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier.onKeyEvent { event -> onKeyEvent(event) }
                .imePadding()
                .puzzleStatusBarColor(),
            topBar = { TopAppBar() },
        ) { innerPadding ->
            ActivityBody(Modifier.padding(innerPadding))
        }
    }
}

