
package app.crossword.yourealwaysbe.forkyz

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolLauncher
import app.crossword.yourealwaysbe.forkyz.inttools.InternalToolData
import app.crossword.yourealwaysbe.forkyz.inttools.InternalToolLauncher
import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.view.ClueTabs
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.MovementStrategy

class ClueListActivity() : PuzzleActivity() {

    override protected lateinit var viewModel : ClueListActivityViewModel

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)
            .get(ClueListActivityViewModel::class.java)

        setContent {
            AppTheme() {
                ClueListPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    inputConnectionMediator = inputConnectionMediator,
                    displayWidth = displayWidth,
                    displayHeight = displayHeight,
                    isPortrait = isPortrait,
                    openHTMLPage = this::openHTMLPage,
                    openSettingsPage = this::openSettingsPage,
                    openURI = this::openURI,
                    onFinish = this::finish,
                    onBack = this::doBackAction,
                    setBackEnabled = this::setBackEnabled,
                    toast = this::toast,
                    handleInternalToolEvent = this::handleInternalToolEvent,
                    handleExternalToolEvent = this::handleExternalToolEvent,
                    setStatusBarColor = this::legacySetStatusBarColor,
                    launchVoiceInput = this::launchVoiceInput,
                    announce = { data, view -> announce(data, view) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClueListPage(
    viewModel : ClueListActivityViewModel,
    themeHelper : ThemeHelper,
    inputConnectionMediator : InputConnectionMediator,
    displayWidth : Int,
    displayHeight : Int,
    isPortrait : Boolean,
    openHTMLPage : (String) -> Unit,
    openSettingsPage : () -> Unit,
    openURI : (Uri) -> Unit,
    onFinish : () -> Unit,
    onBack : () -> Unit,
    setBackEnabled : (Boolean) -> Unit,
    toast : (String) -> Unit,
    handleInternalToolEvent : (InternalToolData) -> Unit,
    handleExternalToolEvent : (ExternalToolData) -> Unit,
    setStatusBarColor : (Color) -> Unit,
    launchVoiceInput : () -> Unit,
    announce : (AnnounceData, View) -> Unit,
) {
    PuzzlePage(
        viewModel = viewModel,
        themeHelper = themeHelper,
        inputConnectionMediator = inputConnectionMediator,
        displayWidth = displayWidth,
        displayHeight = displayHeight,
        isPortrait = isPortrait,
        openHTMLPage = openHTMLPage,
        openSettingsPage = openSettingsPage,
        openURI = openURI,
        onFinish = onFinish,
        onBack = onBack,
        setBackEnabled = setBackEnabled,
        toast = toast,
        handleInternalToolEvent = handleInternalToolEvent,
        handleExternalToolEvent = handleExternalToolEvent,
        setStatusBarColor = setStatusBarColor,
        launchVoiceInput = launchVoiceInput,
        announce = announce,
    ) {
        fun onKeyEvent(event : KeyEvent) : Boolean {
            when (event.key) {
                Key.Escape -> {
                    if (event.type == KeyEventType.KeyUp)
                        onBack()
                    return true
                }
                Key.DirectionLeft -> {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.moveLeft()
                    return true
                }
                Key.DirectionRight -> {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.moveRight()
                    return true
                }
                Key.DirectionUp -> {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.moveUp();
                    return true
                }
                Key.DirectionDown -> {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.moveDown()
                    return true
                }
                Key.Delete, Key.Backspace -> {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.deleteLetter()
                    return true
                }
                Key.Spacebar -> {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.playSpace()
                    return true
                }
            }

            val keyChar = letterOrDigitKeyToChar(event.key)
            if (keyChar != null) {
                if (event.type == KeyEventType.KeyUp)
                    viewModel.playLetter(keyChar)
                return true
            } else {
                return onKeyEventPuzzlePage(event)
            }
        }

        @Composable
        fun MenuShowAllWords() {
            val showAllWords by viewModel.showAllWords.collectAsStateWithLifecycle()

            MenuCheckedItem(
                checked = showAllWords,
                text = {
                    MenuText(stringResource(R.string.show_all_words_clue_list))
                },
                onClick = {
                    viewModel.toggleShowAllWords()
                    viewModel.dismissMenu()
                },
            )
        }

        @Composable
        fun OverflowMenu() {
            val state by viewModel.menuState.collectAsStateWithLifecycle()

            IconButton(onClick = { viewModel.expandMenu(PuzzleSubMenu.MAIN) }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.overflow),
                )
            }
            DropdownMenu(
                modifier = BaseMenuModifier,
                expanded = state.expanded == PuzzleSubMenu.MAIN,
                onDismissRequest = viewModel::dismissMenu,
            ) {
                MenuNotes()
                MenuScratchMode()
                MenuSpecialEntry()
                MenuShowErrors()
                MenuReveal()
                MenuExternalTools()
                MenuShowAllWords()
                MenuEditClue()
                MenuPuzzleInfo()
                MenuSupportPuzzleSource()
                MenuShare()
                MenuHelp()
                MenuSettings()
            }
            MenuNotesSub()
            MenuShowErrorsSub()
            MenuRevealSub()
            MenuExternalToolsSub()
            MenuShareSub()
        }

        @Composable
        fun TopAppBar() {
            themeHelper.ForkyzTopAppBar(
                title = { Text(stringResource(R.string.clues_list_label)) },
                onBack = { onFinish() },
                actions = { OverflowMenu() },
            )
        }

        @Composable
        fun ClueLists(modifier : Modifier, showAllWords : Boolean) {
            val boardColorScheme = themeHelper.getBoardColorScheme()
            ClueTabs(
                modifier = modifier,
                viewModel = viewModel.clueTabsViewModel,
                isPortrait = isPortrait,
                onClueClick = viewModel::clueClick,
                onClueClickDescription = stringResource(R.string.select_clue),
                onClueLongClick = { cid ->
                    viewModel.selectClue(cid)
                    viewModel.launchClueNotes(cid)
                },
                onClueLongClickDescription
                    = stringResource(R.string.open_clue_notes),
                onClueBoardClick = { cid, _ -> viewModel.clueClick(cid) },
                onClueBoardClickDescription = stringResource(R.string.select_clue),
                showAllWords = showAllWords,
                boardColorScheme = boardColorScheme,
            )
        }

        @Composable
        fun ActivityBody(modifier : Modifier) {
            Column(modifier = modifier) {
                val showAllWords
                    by viewModel.showAllWords.collectAsStateWithLifecycle()
                val movementStrategy
                    by viewModel.movementStrategy.collectAsStateWithLifecycle()

                Miniboard(
                    onClick = { _, _ -> viewModel.showKeyboard() },
                    onLongClick = { viewModel.launchCurrentClueNotes() },
                    longClickAccessibilityAction = R.string.open_clue_notes,
                    incognitoMode = showAllWords,
                    movementStrategy = movementStrategy,
                )
                ClueLists(
                    Modifier.fillMaxWidth().weight(1.0F),
                    showAllWords,
                )
                VoiceButtons()
                Keyboard()
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
