
package app.crossword.yourealwaysbe.forkyz

import android.os.Bundle
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.puz.Clue

class ClueListActivity() : PuzzleActivity() {

    override protected lateinit var viewModel : ClueListActivityViewModel

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)
            .get(ClueListActivityViewModel::class.java)

        setContent {
            Launchers()

            AppTheme() {
                Scaffold(
                    modifier = Modifier.onKeyEvent(this::onKeyEvent)
                        .imePadding()
                        .puzzleStatusBarColor(),
                    topBar = { TopAppBar() },
                ) { innerPadding ->
                    ActivityBody(Modifier.padding(innerPadding))
                }
            }
        }
    }

    override protected fun onKeyEvent(event : KeyEvent) : Boolean {
        when (event.key) {
            Key.Escape -> {
                if (event.type == KeyEventType.KeyUp)
                    doBackAction()
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
            return super.onKeyEvent(event)
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun TopAppBar() {
        themeHelper.ForkyzTopAppBar(
            title = { Text(stringResource(R.string.clues_list_label)) },
            onBack = { finish() },
            actions = { OverflowMenu() },
        )
    }

    @Composable
    private fun ActivityBody(modifier : Modifier) {
        Column(modifier = modifier) {
            val showAllWordsState by viewModel.showAllWords.observeAsState()
            val showAllWords = showAllWordsState ?: false

            Miniboard(
                onClick = { _, _ -> viewModel.showKeyboard() },
                onLongClick = { viewModel.launchCurrentClueNotes() },
                longClickAccessibilityAction = R.string.open_clue_notes,
                incognitoMode = showAllWords,
            )
            ClueLists(
                Modifier.fillMaxWidth().weight(1.0F),
                showAllWords,
            )
            VoiceButtons()
            Keyboard()
        }
    }

    @Composable
    private fun ClueLists(modifier : Modifier, showAllWords : Boolean) {
        val snapToClue
            by viewModel.snapToClueEvent.collectAsStateWithLifecycle()

        ClueTabsView(
            modifier = modifier,
            board = viewModel.getBoard(),
            onClueClick = viewModel::clueClick,
            onClueClickDescription = stringResource(R.string.select_clue),
            onClueLongClick = { clue ->
                clue?.let { clue ->
                    viewModel.selectClue(clue)
                    viewModel.launchClueNotes(clue)
                }
            },
            onClueLongClickDescription
                = stringResource(R.string.open_clue_notes),
            onBoardClick = { clue, _ -> viewModel.clueClick(clue) },
            snapToClue = snapToClue != null,
            snapClueShowPageNum = snapToClue?.pageChangeData?.showPageNum,
            snapClueReplacePageNum = snapToClue?.pageChangeData?.replacePageNum,
            showAllWords = showAllWords,
        )

        LaunchedEffect(snapToClue) {
            viewModel.clearSnapToClueEvent()
        }
    }

    @Composable
    private fun OverflowMenu() {
        val state by viewModel.menuState.observeAsState()

        IconButton(onClick = { viewModel.expandMenu(PuzzleSubMenu.MAIN) }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.overflow),
            )
        }
        DropdownMenu(
            modifier = BaseMenuModifier,
            expanded = state?.expanded == PuzzleSubMenu.MAIN,
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
    private fun MenuShowAllWords() {
        val showAllWords by viewModel.showAllWords.observeAsState()

        MenuCheckedItem(
            checked = showAllWords ?: false,
            text = {
                MenuText(stringResource(R.string.show_all_words_clue_list))
            },
            onClick = {
                viewModel.toggleShowAllWords()
                viewModel.dismissMenu()
            },
        )
    }
}
