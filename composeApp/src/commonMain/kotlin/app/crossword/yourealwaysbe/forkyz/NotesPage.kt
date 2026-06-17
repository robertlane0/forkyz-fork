
package app.crossword.yourealwaysbe.forkyz

import kotlinx.collections.immutable.persistentListOf

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.anagram_solution_lab
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.anagram_source_lab
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.board_lab
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.choose_flag_color
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.copy_conflict
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.flag_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.general_clue_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.general_puzzle_notes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_flag_cell_toggle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_flag_clue_toggle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.menu_notes_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.notes_lab
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.overflow
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.scratch_lab
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.shuffle_anagram_source
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.transfer_anagram_solution_to_focussed_view
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.transfer_board_to_focussed_view
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.transfer_overwrite_warning
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.transfer_scratch_to_focussed_view
import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuExit
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.Word
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.addAlpha
import app.crossword.yourealwaysbe.forkyz.util.keyEventToResponse
import app.crossword.yourealwaysbe.forkyz.view.BoardEditText
import app.crossword.yourealwaysbe.forkyz.view.BoardEditTextState
import app.crossword.yourealwaysbe.forkyz.view.BoxInputState
import app.crossword.yourealwaysbe.forkyz.view.OKCancelDialog
import app.crossword.yourealwaysbe.forkyz.view.OrdinaryKey
import app.crossword.yourealwaysbe.forkyz.view.WordEdit
import app.crossword.yourealwaysbe.forkyz.view.rememberBoardEditTextState
import app.crossword.yourealwaysbe.forkyz.view.rememberBoardTextMeasurer

private val VERTICAL_BOARD_PADDING = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesPage(
    viewModel : NotesPageViewModel,
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

        var focussedEntry : NoteEntry?
            by rememberSaveable { mutableStateOf(null) }

        fun getDefaultEntry() : NoteEntry {
            return if (viewModel.isPuzzleNotes)
                NoteEntry.SCRATCH
            else
                NoteEntry.BOARD
        }

        /**
         * Call when focus on entry items changes
         *
         * Keeps track of which entry if any has focus
         */
        fun setFocussedEntry(entry : NoteEntry, focussed : Boolean) {
            if (focussed)
                focussedEntry = entry
            else if (focussedEntry == entry)
                focussedEntry = null
        }

        /**
         * Transfer source to focussed view
         *
         * Use default if focussed entry is null or the same as source
         */
        fun transferToFocussed(
            source : NoteEntry,
            default : NoteEntry? = null
        ) {
            val target = if (focussedEntry == null || focussedEntry == source)
                default
            else
                focussedEntry
            target?.let { viewModel.transferEntries(source, it) }
        }

        fun callExternalDictionary() {
            viewModel.callExternalDictionary(focussedEntry ?: getDefaultEntry())
        }

        fun callCrosswordSolver() {
            viewModel.callCrosswordSolver(focussedEntry ?: getDefaultEntry())
        }

        fun onKeyEvent(event : KeyEvent) : Boolean {
            val handled = onKeyEventKeyboardBindings(
                event = event,
                onInsertSpecialDown = {
                    if (focussedEntry == NoteEntry.BOARD)
                        viewModel.doSpecialEntry()
                },
                onShuffleAnagramDown = viewModel::shuffleAnagramSource,
            )
            if (handled)
                return true

            return onKeyEventPuzzlePage(event)
        }

        fun onMiniboardKeyEvent(event : KeyEvent) : Boolean {
            val isDown = event.type == KeyEventType.KeyDown
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
                Key.Delete, Key.Backspace -> {
                    if (isDown)
                        viewModel.deleteLetterFromBoard()
                    return true
                }
                Key.Spacebar -> {
                    if (isDown)
                        viewModel.playLetterToBoard(' ')
                    return true
                }
            }

            val response = keyEventToResponse(event)
            if (response != null) {
                if (isDown)
                    viewModel.playLetterToBoard(response)
                return true
            } else {
                return onKeyEventPuzzlePage(event)
            }
        }

        @Composable
        fun MenuClueFlagToggle() {
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.menu_flag_clue_toggle))
                },
                onClick = {
                    viewModel.toggleClueFlag()
                    viewModel.dismissMenu()
                },
            )
        }

        @Composable
        fun MenuCellFlagToggle() {
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(Res.string.menu_flag_cell_toggle))
                },
                onClick = {
                    viewModel.toggleCellFlag()
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
                MenuClueFlagToggle()
                MenuCellFlagToggle()
                if (focussedEntry == NoteEntry.BOARD)
                    MenuSpecialEntry()
                MenuShowErrors()
                MenuReveal()
                MenuExternalTools()
                val uiState
                    by viewModel.notesUIState.collectAsStateWithLifecycle()
                val isPuzzleNotes by remember {
                    derivedStateOf { uiState.isPuzzleNotes }
                }
                if (!isPuzzleNotes) {
                    MenuEditClue() {
                        viewModel.editClue(viewModel.notesClueID)
                    }
                }
                MenuPuzzleInfo()
                MenuSupportPuzzleSource()
                MenuShare()
                MenuHelp()
                MenuSettings()
                MenuExit(utils)
            }
            MenuNotesSub()
            MenuShowErrorsSub()
            MenuRevealSub()
            MenuExternalToolsSub(
                onCallExternalDictionary = ::callExternalDictionary,
                onCallCrosswordSolver = ::callCrosswordSolver,
                showAnagramSolver = true,
                onCallAnagramSolver = viewModel::callAnagramSolver
            )
            MenuShareSub()
        }

        @Composable
        fun TopAppBar() {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val expandableClueLine by remember {
                derivedStateOf { state.expandableClueLine }
            }
            val modifier = if (expandableClueLine)
                Modifier
            else
                Modifier.height(TopAppBarDefaults.TopAppBarExpandedHeight)

            themeHelper.ForkyzTopAppBar(
                modifier = modifier,
                title = {
                    val isPuzzleNotes by remember {
                        derivedStateOf { state.isPuzzleNotes }
                    }
                    val clueText by remember {
                        derivedStateOf { state.clueText }
                    }

                    if (isPuzzleNotes) {
                        Text(stringResource(Res.string.menu_notes_puzzle))
                    } else {
                        ClueText(
                            modifier = Modifier.fillMaxWidth(),
                            clueText = state.clueText,
                        )
                    }
                },
                onBack = { doExitPage() },
                actions = { OverflowMenu() },
            )
        }

        @Composable
        fun BoardLabel(stringID : StringResource) {
            Text(stringResource(stringID))
        }

        @Composable
        fun MiniboardRow(maxWidth : Dp) {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val showBoard by remember { derivedStateOf { state.showBoard } }
            if (!showBoard)
                return

            val boardColorScheme = themeHelper.getBoardColorScheme()
            val boxInputState = remember { BoxInputState() }

            BoardLabel(Res.string.board_lab)
            WordEdit(
                modifier = Modifier.onKeyEvent(::onMiniboardKeyEvent)
                    .padding(vertical = VERTICAL_BOARD_PADDING),
                textMeasurer = textMeasurer,
                maxWidth = maxWidth,
                state = boxInputState,
                inputConnectionMediator = utils.inputConnectionMediator,
                colors = boardColorScheme,
                viewModel = viewModel.wordEditViewModel,
                onTap = { row, col ->
                    boxInputState.requestFocus()
                    setFocussedEntry(NoteEntry.BOARD, true)
                    viewModel.clickBoard(row, col, fixClue = true)
                    viewModel.showKeyboard()
                },
                onLongPress = { _, _ ->
                    transferToFocussed(NoteEntry.BOARD)
                },
                onLongPressDescription
                    = stringResource(Res.string.transfer_board_to_focussed_view),
            )

            LaunchedEffect(isTop) {
                if (isTop) {
                    boxInputState.requestFocus()
                    setFocussedEntry(NoteEntry.BOARD, true)
                }
            }
        }

        /**
         * BoardEditText for notes page.
         *
         * Provides some arguments of BoardEditText directly
         */
        @Composable
        fun NotesBoardEditText(
            maxWidth : Dp,
            boardEditTextState : BoardEditTextState,
            value : String,
            onDelete : (Int) -> Unit = { null },
            onChange : (Int, Char) -> String? = { _, _ -> null },
            separators : Boolean = false,
            shadow : String? = null,
            onTap : () -> Unit,
            onLongPress : () -> Unit,
            onLongPressDescription : String,
            onFocusChanged : (Boolean) -> Unit,
            contentDescription : String,
        ) {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val boardColorScheme = themeHelper.getBoardColorScheme()

            val separatorsList by remember(separators) {
                derivedStateOf {
                    if (separators)
                        state.clueSeparators
                    else
                        persistentListOf()
                }
            }
            val skipFilled by remember { derivedStateOf { state.skipFilled } }
            val splitWords by remember { derivedStateOf { state.splitWords} }

            BoardEditText(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = VERTICAL_BOARD_PADDING),
                textMeasurer = textMeasurer,
                maxWidth = maxWidth,
                inputConnectionMediator = utils.inputConnectionMediator,
                colors = boardColorScheme,
                state = boardEditTextState,
                length = state.boxesLength,
                value = value,
                onDelete = onDelete,
                onChange = onChange,
                skipFilled = skipFilled,
                splitWords = splitWords,
                separators = separatorsList,
                shadow = shadow,
                onTap = onTap,
                onLongPress = onLongPress,
                onLongPressDescription = onLongPressDescription,
                onFocusChanged = onFocusChanged,
                contentDescription = contentDescription,
            )
        }

        @Composable
        fun ScratchRow(maxWidth : Dp) {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val scratchValue by remember {
                derivedStateOf { state.scratchValue }
            }
            val boardEditTextState = rememberBoardEditTextState()

            BoardLabel(Res.string.scratch_lab)
            NotesBoardEditText(
                maxWidth = maxWidth,
                boardEditTextState = boardEditTextState,
                value = scratchValue,
                onDelete = viewModel::deleteScratch,
                onChange = viewModel::changeScratch,
                separators = true,
                shadow = null,
                onTap = {
                    boardEditTextState.requestFocus()
                    viewModel.showKeyboard()
                },
                onLongPress = {
                    transferToFocussed(NoteEntry.SCRATCH, NoteEntry.BOARD)
                },
                onLongPressDescription
                    = stringResource(Res.string.transfer_scratch_to_focussed_view),
                onFocusChanged = { focussed ->
                    setFocussedEntry(NoteEntry.SCRATCH, focussed)
                },
                contentDescription = stringResource(Res.string.scratch_lab),
            )
        }

        @Composable
        fun TextRow(modifier : Modifier) {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val isPuzzleNotes by remember {
                derivedStateOf { state.isPuzzleNotes }
            }
            val hintStringID = if (isPuzzleNotes)
                Res.string.general_puzzle_notes
            else
                Res.string.general_clue_notes
            val textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace
            )

            // needs immediate updates:
            // https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5
            val textValue by remember { derivedStateOf { state.textValue } }
            var localText by remember(textValue) {
                mutableStateOf(textValue)
            }

            BoardLabel(Res.string.notes_lab)
            TextField(
                modifier = modifier.fillMaxWidth()
                    .padding(vertical = VERTICAL_BOARD_PADDING)
                    .onFocusChanged {
                        setFocussedEntry(NoteEntry.TEXT, it.hasFocus)
                        viewModel.onFocusNativeView(it.hasFocus)
                    },
                textStyle = textStyle,
                singleLine = false,
                minLines = 2,
                value = localText,
                onValueChange = {
                    localText = it
                    viewModel.setTextValue(it)
                },
                placeholder = { Text(stringResource(hintStringID)) },
            )
        }

        @Composable
        fun FlagRow() {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val isPuzzleNotes by remember {
                derivedStateOf { state.isPuzzleNotes }
            }
            if (!isPuzzleNotes) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.combinedClickable(
                            onLongClick = viewModel::longClickNotesClueFlag,
                            onClick = viewModel::toggleNotesClueFlag,
                            onLongClickLabel = stringResource(
                                Res.string.choose_flag_color
                            ),
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val clueFlagColor by remember {
                            derivedStateOf { state.clueFlagColor }
                        }
                        var checkColors = CheckboxDefaults.colors()
                        if (!Clue.isDefaultFlagColor(clueFlagColor)) {
                            val color = Color(addAlpha(clueFlagColor))
                            checkColors = CheckboxDefaults.colors(
                                checkedColor = color,
                                uncheckedColor = color,
                            )
                        }

                        Text(stringResource(Res.string.flag_clue))
                        val checked by remember {
                            derivedStateOf { state.clueFlagged }
                        }
                        Checkbox(
                            modifier = Modifier.padding(10.dp),
                            checked = checked,
                            // needs to be null so parent gets long click
                            onCheckedChange = null,
                            colors = checkColors,
                        )
                    }
                }
            }
        }

        @Composable
        fun AnagramSourceRow(maxWidth : Dp) {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val acceptedPredictionEvent
                by viewModel.acceptedPredictionEvent.collectAsStateWithLifecycle()
            val boardEditTextState = rememberBoardEditTextState()

            LaunchedEffect(acceptedPredictionEvent) {
                acceptedPredictionEvent?.let {
                    // to end if filled boxes, else to first free space
                    if (it >= state.boxesLength)
                        it - 1
                    else if (it > 0)
                        it
                    else
                        null
                }?.let { newSelected ->
                    boardEditTextState.selected = newSelected
                }
                if (acceptedPredictionEvent != null)
                    viewModel.clearAcceptedPredictionEvent()
            }

            BoardLabel(Res.string.anagram_source_lab)
            val anagramSourceValue by remember {
                derivedStateOf { state.anagramSourceValue }
            }
            val anagramSourcePredictionValue by remember {
                derivedStateOf { state.anagramSourcePredictionValue }
            }
            NotesBoardEditText(
                maxWidth = maxWidth,
                boardEditTextState = boardEditTextState,
                value = anagramSourceValue,
                onDelete = viewModel::deleteAnagramSource,
                onChange = viewModel::changeAnagramSource,
                separators = false,
                shadow = anagramSourcePredictionValue,
                onTap = {
                    boardEditTextState.requestFocus()
                    viewModel.showKeyboard()
                },
                onLongPress = viewModel::shuffleAnagramSource,
                onLongPressDescription
                    = stringResource(Res.string.shuffle_anagram_source),
                onFocusChanged = { focussed ->
                    setFocussedEntry(NoteEntry.ANAGRAM_SOURCE, focussed)
                },
                contentDescription
                    = stringResource(Res.string.anagram_source_lab),
            )
        }

        @Composable
        fun AnagramSolutionRow(maxWidth : Dp) {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val boardEditTextState = rememberBoardEditTextState()

            BoardLabel(Res.string.anagram_solution_lab)
            val anagramSolutionValue by remember {
                derivedStateOf { state.anagramSolutionValue }
            }
            NotesBoardEditText(
                maxWidth = maxWidth,
                boardEditTextState = boardEditTextState,
                value = anagramSolutionValue,
                onDelete = viewModel::deleteAnagramSolution,
                onChange = viewModel::changeAnagramSolution,
                separators = true,
                shadow = null,
                onTap = {
                    boardEditTextState.requestFocus()
                    viewModel.showKeyboard()
                },
                onLongPress = {
                    transferToFocussed(
                        NoteEntry.ANAGRAM_SOLUTION,
                        NoteEntry.BOARD,
                    )
                },
                onLongPressDescription = stringResource(
                    Res.string.transfer_anagram_solution_to_focussed_view
                ),
                onFocusChanged = { focussed ->
                    setFocussedEntry(NoteEntry.ANAGRAM_SOLUTION, focussed)
                },
                contentDescription
                    = stringResource(Res.string.anagram_source_lab),
            )
        }

        @Composable
        fun TransferConfirmationDialog() {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val confirmTransferRequest by remember {
                derivedStateOf { state.confirmTransferRequest }
            }
            if (confirmTransferRequest != null) {
                OKCancelDialog(
                    title = Res.string.copy_conflict,
                    summary = Res.string.transfer_overwrite_warning,
                    onOK = { viewModel.confirmTransferRequest(true) },
                    onCancel = { viewModel.confirmTransferRequest(false) },
                )
            }
        }

        @Composable
        fun ActivityBody(modifier : Modifier) {
            BoxWithConstraints(modifier.fillMaxSize()) {
                val miniboardMaxWidth = maxWidth
                Column() {
                    Column(
                        modifier = Modifier.weight(1f)
                            .verticalScroll(rememberScrollState())
                            .height(IntrinsicSize.Max),
                    ) {
                        MiniboardRow(maxWidth = miniboardMaxWidth)
                        ScratchRow(maxWidth = miniboardMaxWidth)
                        TextRow(modifier = Modifier.weight(1.0F))
                        FlagRow()
                        AnagramSourceRow(maxWidth = miniboardMaxWidth)
                        AnagramSolutionRow(maxWidth = miniboardMaxWidth)
                        VoiceButtons()
                    }
                    Keyboard()
                }
            }
            TransferConfirmationDialog()
        }

        Scaffold(
            modifier = Modifier.onKeyEvent(::onKeyEvent)
                .imePadding()
                .puzzleStatusBarColor(),
            topBar = { TopAppBar() },
        ) { innerPadding ->
            ActivityBody(Modifier.padding(innerPadding))
        }
    }
}
