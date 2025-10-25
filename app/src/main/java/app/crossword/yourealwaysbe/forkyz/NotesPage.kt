
package app.crossword.yourealwaysbe.forkyz

import kotlinx.collections.immutable.persistentListOf

import android.net.Uri
import androidx.activity.compose.setContent
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.addAlpha
import app.crossword.yourealwaysbe.forkyz.util.letterOrDigitKeyToChar
import app.crossword.yourealwaysbe.forkyz.view.BoardEditText
import app.crossword.yourealwaysbe.forkyz.view.BoardEditTextState
import app.crossword.yourealwaysbe.forkyz.view.BoxInputState
import app.crossword.yourealwaysbe.forkyz.view.OKCancelDialog
import app.crossword.yourealwaysbe.forkyz.view.WordEdit
import app.crossword.yourealwaysbe.forkyz.view.rememberBoardEditTextState
import app.crossword.yourealwaysbe.forkyz.view.rememberBoardTextMeasurer
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.MovementStrategy
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position

private val VERTICAL_BOARD_PADDING = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesPage(
    viewModel : NotesPageViewModel,
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
    setOnVoiceCommandCallback : (((List<String>?) -> Unit)?) -> Unit,
    launchVoiceInput : () -> Unit,
    announce : (AnnounceData) -> Unit,
) {
    PuzzlePage(
        viewModel = viewModel,
        themeHelper = themeHelper,
        inputConnectionMediator = inputConnectionMediator,
        displayWidth = displayWidth,
        displayHeight = displayHeight,
        isPortrait = isPortrait,
        openClueListPage = openClueListPage,
        openNotesPage = openNotesPage,
        openHTMLPage = openHTMLPage,
        openSettingsPage = openSettingsPage,
        openURI = openURI,
        onFinish = onFinish,
        onBack = onBack,
        toast = toast,
        handleExternalToolEvent = handleExternalToolEvent,
        setOnAppNotFoundCallback = setOnAppNotFoundCallback,
        setStatusBarColor = setStatusBarColor,
        setOnVoiceCommandCallback = setOnVoiceCommandCallback,
        launchVoiceInput = launchVoiceInput,
        announce = announce,

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
            return onKeyEventPuzzlePage(event)
        }

        fun onMiniboardKeyEvent(event : KeyEvent) : Boolean {
            when (event.key) {
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
                Key.Delete, Key.Backspace -> {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.deleteLetterFromBoard()
                    return true
                }
                Key.Spacebar -> {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.playLetterToBoard(' ')
                    return true
                }
            }

            val keyChar = letterOrDigitKeyToChar(event.key)
            if (keyChar != null) {
                if (event.type == KeyEventType.KeyUp)
                    viewModel.playLetterToBoard(keyChar)
                return true
            } else {
                return onKeyEventPuzzlePage(event)
            }
        }

        @Composable
        fun MenuClueFlagToggle() {
            DropdownMenuItem(
                text = {
                    MenuText(stringResource(R.string.menu_flag_clue_toggle))
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
                    MenuText(stringResource(R.string.menu_flag_cell_toggle))
                },
                onClick = {
                    viewModel.toggleCellFlag()
                    viewModel.dismissMenu()
                },
            )
        }

        @Composable
        fun OverflowMenu() {
            val state by viewModel.menuState.collectAsStateWithLifecycle()
            val uiState by viewModel.notesUIState.collectAsStateWithLifecycle()

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
                MenuClueFlagToggle()
                MenuCellFlagToggle()
                if (focussedEntry == NoteEntry.BOARD)
                    MenuSpecialEntry()
                MenuShowErrors()
                MenuReveal()
                MenuExternalTools()
                if (!uiState.isPuzzleNotes) {
                    MenuEditClue() {
                        viewModel.editClue(viewModel.notesClueID)
                    }
                }
                MenuPuzzleInfo()
                MenuSupportPuzzleSource()
                MenuShare()
                MenuHelp()
                MenuSettings()
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
            val modifier = if (state.expandableClueLine)
                Modifier
            else
                Modifier.height(TopAppBarDefaults.TopAppBarExpandedHeight)

            themeHelper.ForkyzTopAppBar(
                modifier = modifier,
                title = {
                    if (state.isPuzzleNotes) {
                        Text(stringResource(R.string.menu_notes_puzzle))
                    } else {
                        ClueText(
                            modifier = Modifier.fillMaxWidth(),
                            clueText = state?.clueText,
                        )
                    }
                },
                onBack = { doExitPage() },
                actions = { OverflowMenu() },
            )
        }

        @Composable
        fun BoardLabel(stringID : Int) {
            Text(stringResource(stringID))
        }

        @Composable
        fun MiniboardRow(maxWidth : Dp) {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            if (state.isPuzzleNotes)
                return

            val boardColorScheme = themeHelper.getBoardColorScheme()
            val boxInputState = remember { BoxInputState() }

            BoardLabel(R.string.board_lab)
            WordEdit(
                modifier = Modifier.onKeyEvent(::onMiniboardKeyEvent)
                    .padding(vertical = VERTICAL_BOARD_PADDING),
                textMeasurer = textMeasurer,
                maxWidth = maxWidth,
                state = boxInputState,
                inputConnectionMediator = inputConnectionMediator,
                colors = boardColorScheme,
                viewModel = viewModel.wordEditViewModel,
                onTap = { row, col ->
                    boxInputState.requestFocus()
                    setFocussedEntry(NoteEntry.BOARD, true)
                    viewModel.showKeyboard()
                },
                onLongPress = { _, _ ->
                    transferToFocussed(NoteEntry.BOARD)
                },
                onLongPressDescription
                    = stringResource(R.string.transfer_board_to_focussed_view),
            )

            LaunchedEffect(Unit) {
                boxInputState.requestFocus()
                setFocussedEntry(NoteEntry.BOARD, true)
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

            val separatorsList = if (separators)
                state.clueSeparators
            else
                persistentListOf()

            BoardEditText(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = VERTICAL_BOARD_PADDING),
                textMeasurer = textMeasurer,
                maxWidth = maxWidth,
                inputConnectionMediator = inputConnectionMediator,
                colors = boardColorScheme,
                state = boardEditTextState,
                length = state.boxesLength,
                value = value,
                onDelete = onDelete,
                onChange = onChange,
                skipFilled = state.skipFilled,
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
            val boardEditTextState = rememberBoardEditTextState()

            BoardLabel(R.string.scratch_lab)
            NotesBoardEditText(
                maxWidth = maxWidth,
                boardEditTextState = boardEditTextState,
                value = state.scratchValue,
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
                    = stringResource(R.string.transfer_scratch_to_focussed_view),
                onFocusChanged = { focussed ->
                    setFocussedEntry(NoteEntry.SCRATCH, focussed)
                },
                contentDescription = stringResource(R.string.scratch_lab),
            )
        }

        @Composable
        fun TextRow(modifier : Modifier) {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val hintStringID = if (state.isPuzzleNotes)
                R.string.general_puzzle_notes
            else
                R.string.general_clue_notes
            val textStyle = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace
            )

            // needs immediate updates:
            // https://medium.com/androiddevelopers/effective-state-management-for-textfield-in-compose-d6e5b070fbe5
            var localText by remember(state.textValue) {
                mutableStateOf(state.textValue)
            }

            BoardLabel(R.string.notes_lab)
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
            if (!state.isPuzzleNotes) {
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
                                R.string.choose_flag_color
                            ),
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val clueFlagColor = state.clueFlagColor
                        var checkColors = CheckboxDefaults.colors()
                        if (!Clue.isDefaultFlagColor(clueFlagColor)) {
                            val color = Color(addAlpha(clueFlagColor))
                            checkColors = CheckboxDefaults.colors(
                                checkedColor = color,
                                uncheckedColor = color,
                            )
                        }

                        Text(stringResource(R.string.flag_clue))
                        Checkbox(
                            modifier = Modifier.padding(10.dp),
                            checked = state.clueFlagged,
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

            BoardLabel(R.string.anagram_source_lab)
            NotesBoardEditText(
                maxWidth = maxWidth,
                boardEditTextState = boardEditTextState,
                value = state.anagramSourceValue,
                onDelete = viewModel::deleteAnagramSource,
                onChange = viewModel::changeAnagramSource,
                separators = false,
                shadow = state.anagramSourcePredictionValue,
                onTap = {
                    boardEditTextState.requestFocus()
                    viewModel.showKeyboard()
                },
                onLongPress = viewModel::shuffleAnagramSource,
                onLongPressDescription
                    = stringResource(R.string.shuffle_anagram_source),
                onFocusChanged = { focussed ->
                    setFocussedEntry(NoteEntry.ANAGRAM_SOURCE, focussed)
                },
                contentDescription
                    = stringResource(R.string.anagram_source_lab),
            )
        }

        @Composable
        fun AnagramSolutionRow(maxWidth : Dp) {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()
            val boardEditTextState = rememberBoardEditTextState()

            BoardLabel(R.string.anagram_solution_lab)
            NotesBoardEditText(
                maxWidth = maxWidth,
                boardEditTextState = boardEditTextState,
                value = state.anagramSolutionValue,
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
                    R.string.transfer_anagram_solution_to_focussed_view
                ),
                onFocusChanged = { focussed ->
                    setFocussedEntry(NoteEntry.ANAGRAM_SOLUTION, focussed)
                },
                contentDescription
                    = stringResource(R.string.anagram_source_lab),
            )
        }

        @Composable
        fun TransferConfirmationDialog() {
            val state by viewModel.notesUIState.collectAsStateWithLifecycle()

            if (state.confirmTransferRequest != null) {
                OKCancelDialog(
                    title = R.string.copy_conflict,
                    summary = R.string.transfer_overwrite_warning,
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
