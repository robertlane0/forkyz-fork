
package app.crossword.yourealwaysbe.forkyz

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.util.ColorUtils
import app.crossword.yourealwaysbe.forkyz.view.BoardEditText
import app.crossword.yourealwaysbe.forkyz.view.BoardEditText.BoardEditFilter
import app.crossword.yourealwaysbe.forkyz.view.BoardEditView.BoardClickListener
import app.crossword.yourealwaysbe.forkyz.view.BoardWordEditView
import app.crossword.yourealwaysbe.forkyz.view.OKCancelDialog
import app.crossword.yourealwaysbe.forkyz.view.ScrollingImageView
import app.crossword.yourealwaysbe.forkyz.view.ScrollingImageView.Point
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.MovementStrategy
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position

private val VERTICAL_BOARD_PADDING = 6.dp

class NotesActivity() : PuzzleActivity() {
    companion object {
        // TODO: JvmField can be removed when PlayActivity is Compose
        @JvmField
        val CLUE_NOTE_LISTNAME = "clueNoteListName"
        @JvmField
        val CLUE_NOTE_INDEX = "clueNoteIndex"
    }

    override protected lateinit var viewModel : NotesActivityViewModel

    private var focussedEntry : NoteEntry? = null
    private val defaultEntry : NoteEntry
        get() {
            return if (viewModel.isPuzzleNotes)
                NoteEntry.SCRATCH
            else
                NoteEntry.BOARD
        }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)
            .get(NotesActivityViewModel::class.java)

        viewModel.notesClueID = getNotesClueID()

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
        }
        return super.onKeyEvent(event)
    }

    private fun onMiniboardKeyEvent(event : KeyEvent) : Boolean {
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
            return super.onKeyEvent(event)
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun TopAppBar() {
        val state by viewModel.notesUIState.collectAsStateWithLifecycle()
        themeHelper.ForkyzTopAppBar(
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
            onBack = { finish() },
            actions = { OverflowMenu() },
        )
    }

    @Composable
    private fun ActivityBody(modifier : Modifier) {
        Column(modifier.fillMaxHeight()) {
            Column(
                modifier = Modifier.weight(1.0F)
                    .verticalScroll(rememberScrollState())
                    .height(IntrinsicSize.Max),
            ) {
                val state by viewModel.notesUIState.collectAsStateWithLifecycle()
                if (!state.isPuzzleNotes) {
                    MiniboardRow()
                }
                ScratchRow()
                TextRow(modifier = Modifier.weight(1.0F))
                FlagRow()
                AnagramSourceRow()
                AnagramSolutionRow()
                VoiceButtons()
            }
            Keyboard()
        }
        TransferConfirmationDialog()
        PuzzleDialogs()
    }

    @Composable
    private fun MiniboardRow() {
        val state by viewModel.notesUIState.collectAsStateWithLifecycle()

        BoardLabel(R.string.board_lab)
        Miniboard(
            word = state.word,
            modifier = Modifier.onKeyEvent(this::onMiniboardKeyEvent)
                .padding(
                    vertical = VERTICAL_BOARD_PADDING
                ),
            onClick = { _, _ -> viewModel.showKeyboard() },
            onLongClick = {
                transferToFocussed(NoteEntry.BOARD)
            },
            longClickAccessibilityAction
                = R.string.transfer_board_to_focussed_view,
            scratchMode = false,
            useSuppressNotesList = true,
            suppressNotesList = state.suppressNotesList,
            onFocusChanged = { focussed ->
                setFocussedEntry(NoteEntry.BOARD, focussed)
            },
            movementStrategy = state.movementStrategy,
            renderSettings = state.renderSettings,
        )
    }

    @Composable
    private fun ScratchRow() {
        val state by viewModel.notesUIState.collectAsStateWithLifecycle()

        BoardLabel(R.string.scratch_lab)
        BoardEdit(
            value = state.scratchValue,
            contentDescription = R.string.scratch_lab,
            onClick = viewModel::showKeyboard,
            onLongClick = {
                transferToFocussed(NoteEntry.SCRATCH, NoteEntry.BOARD)
            },
            postChange = { _, _, _, value ->
                viewModel.setScratchValue(value)
            },
            longClickAccessibilityAction
                = R.string.transfer_scratch_to_focussed_view,
            onFocusChanged = { focussed ->
                setFocussedEntry(NoteEntry.SCRATCH, focussed)
            },
            separatorsClue = state.notesClue,
            inferSeparators = state.inferSeparators,
        )
    }

    @Composable
    private fun TextRow(modifier : Modifier) {
        val state by viewModel.notesUIState.collectAsStateWithLifecycle()
        val hintStringID = if (state.isPuzzleNotes)
            R.string.general_puzzle_notes
        else
            R.string.general_clue_notes
        val textStyle = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace
        )

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
            value = state.textValue,
            onValueChange = viewModel::setTextValue,
            placeholder = { Text(stringResource(hintStringID)) },
        )
    }

    @Composable
    private fun FlagRow() {
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
                        val color = Color(
                            ColorUtils.addAlpha(clueFlagColor)
                        )
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
    private fun AnagramSourceRow() {
        val state by viewModel.notesUIState.collectAsStateWithLifecycle()
        val acceptedPredictionEvent
            by viewModel.acceptedPredictionEvent.collectAsStateWithLifecycle()

        var selectionPosition : Int? = acceptedPredictionEvent?.let {
            // to end if filled boxes, else to first free space
            if (it >= state.boxesLength)
                it - 1
            else if (it > 0)
                it
            else
                null
        }
        if (acceptedPredictionEvent != null)
            viewModel.clearAcceptedPredictionEvent()

        BoardLabel(R.string.anagram_source_lab)
        BoardEdit(
            value = state.anagramSourceValue,
            shadowValue = state.anagramSourcePredictionValue,
            contentDescription = R.string.anagram_source_lab,
            onClick = {
                // TODO: show keyboard for view!
                viewModel.showKeyboard()
            },
            onLongClick = viewModel::shuffleAnagramSource,
            filter = { oldChar, newChar, _ ->
                viewModel.filterAnagramSource(oldChar, newChar)
            },
            postChange = viewModel::updateAnagramSource,
            longClickAccessibilityAction
                = R.string.shuffle_anagram_source,
            onFocusChanged = { focussed ->
                setFocussedEntry(NoteEntry.ANAGRAM_SOURCE, focussed)
            },
            selectionPosition = selectionPosition,
        )
    }

    @Composable
    private fun AnagramSolutionRow() {
        val state by viewModel.notesUIState.collectAsStateWithLifecycle()

        BoardLabel(R.string.anagram_solution_lab)
        BoardEdit(
            value = state.anagramSolutionValue,
            contentDescription = R.string.anagram_source_lab,
            onClick = { viewModel.showKeyboard() },
            onLongClick = {
                transferToFocussed(NoteEntry.ANAGRAM_SOLUTION, NoteEntry.BOARD)
            },
            filter = { _, newChar, _ ->
                viewModel.filterAnagramSolution(newChar)
            },
            postChange = { pos, oldChar, _, newValue ->
                viewModel.updateAnagramSolution(pos, oldChar, newValue)
            },
            longClickAccessibilityAction
                = R.string.transfer_anagram_solution_to_focussed_view,
            onFocusChanged = { focussed ->
                setFocussedEntry(NoteEntry.ANAGRAM_SOLUTION, focussed)
            },
            separatorsClue = state.notesClue,
            inferSeparators = state.inferSeparators,
        )
    }

    @Composable
    private fun BoardLabel(stringID : Int) {
        Text(stringResource(stringID))
    }

    @Composable
    private fun TransferConfirmationDialog() {
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
    private fun OverflowMenu() {
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
            onCallExternalDictionary = this::callExternalDictionary,
            onCallCrosswordSolver = this::callCrosswordSolver,
            showAnagramSolver = true,
            onCallAnagramSolver = viewModel::callAnagramSolver
        )
        MenuShareSub()
    }

    @Composable
    private fun MenuClueFlagToggle() {
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
    private fun MenuCellFlagToggle() {
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

    /**
     * BoardEditView
     *
     * @param the value that should occupy the view
     * @param contentDescription id of content description for widget
     * @param onClick on click callback
     * @param onLongClick on long click callback
     * @param longClickAccessibilityAction the accessibility action
     * description for long click
     * @param delete see BoardEditFilter
     * @param filter see BoardEditFilter
     * @param postChange see BoardEditFilter with extra full value arg
     * @param onFocusChanged call back for gain/lose focus
     * @param shadowValue the shadow value to use
     * @param separatorsClue clue to copy separators from or null
     * @param inferSeparators whether to infer separators from clue
     * @param selectionPosition if non-null and in range, select this cel
     */
    @Composable
    protected fun BoardEdit(
        value : String,
        contentDescription : Int,
        onClick : () -> Unit,
        onLongClick : () -> Unit,
        longClickAccessibilityAction : Int,
        delete : (Char, Int) -> Boolean = { _, _ -> true },
        filter : (Char, Char, Int) -> Char = { _, newChar, _ -> newChar },
        postChange : (Int, Char, Boolean, String) -> Unit = { _, _, _, _ -> },
        onFocusChanged : (Boolean) -> Unit = { },
        shadowValue : String? = "",
        separatorsClue : Clue? = null,
        inferSeparators : Boolean = false,
        selectionPosition : Int? = null,
    ) {
        val state by viewModel.notesUIState.collectAsStateWithLifecycle()

        AndroidView(
            modifier = Modifier.fillMaxWidth()
                .padding(vertical = VERTICAL_BOARD_PADDING),
            factory = { context ->
                val view = BoardEditText(context).apply {
                    setFocusable(true)
                    setFocusableInTouchMode(true)
                    setContentDescription(
                        context.getString(contentDescription),
                    )
                    setContextMenuListener(
                        object : ScrollingImageView.ClickListener {
                            override fun onTap(point : Point?) {
                                onClick()
                            }
                            override fun onContextMenu(point : Point) {
                                onLongClick()
                            }
                        }
                    )
                    setFilter(object : BoardEditFilter {
                        override fun delete(
                            oldChar : Char,
                            pos : Int,
                        ) : Boolean {
                            return delete(oldChar, pos)
                        }

                        override fun filter(
                            oldChar : Char,
                            newChar : Char,
                            pos : Int,
                        ) : Char {
                            return filter(oldChar, newChar, pos)
                        }

                        override fun postChange(
                            pos : Int,
                            oldChar : Char,
                            isDelete : Boolean,
                        ) {
                            // getView() for BoardEditText just returns
                            // "this", but toString() on its own uses
                            // the toString() of NotesActivity (i think)
                            postChange(
                                pos,
                                oldChar,
                                isDelete,
                                getView().toString(),
                            )
                        }
                    })
                }
                ViewCompat.replaceAccessibilityAction(
                    view,
                    AccessibilityActionCompat.ACTION_LONG_CLICK,
                    getText(longClickAccessibilityAction),
                    null
                )
                view.setOnFocusChangeListener({ _, gainFocus ->
                    if (gainFocus)
                        inputConnectionMediator.registerReceiver(view)
                    onFocusChanged(gainFocus)
                })

                view
            },
            update = { view ->
                view.apply {
                    setLength(state.boxesLength)
                    setFromString(value)
                    setShadow(shadowValue)
                    if (separatorsClue != null)
                        setSeparatorsFromClue(separatorsClue, inferSeparators)
                    if (selectionPosition != null)
                        setSelectedCol(selectionPosition)
                    setRenderSettings(state.renderSettings)
                    setSkipFilled(state.skipFilled)
                }
            },
        )
    }

    /**
     * Get ClueID activity was called for or null if puzzle notes
     */
    private fun getNotesClueID() : ClueID? {
        val intent = getIntent()
        val listName = intent.getStringExtra(CLUE_NOTE_LISTNAME)
        val index = intent.getIntExtra(CLUE_NOTE_INDEX, -1)

        if (listName == null || index < 0)
            return null

        return ClueID(listName, index)
    }

    /**
     * Call when focus on entry items changes
     *
     * Keeps track of which entry if any has focus
     */
    private fun setFocussedEntry(entry : NoteEntry, focussed : Boolean) {
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
    private fun transferToFocussed(
        source : NoteEntry,
        default : NoteEntry? = null
    ) {
        val target = if (focussedEntry == null || focussedEntry == source)
            default
        else
            focussedEntry
        target?.let { viewModel.transferEntries(source, it) }
    }

    private fun callExternalDictionary() {
        viewModel.callExternalDictionary(focussedEntry ?: defaultEntry)
    }

    private fun callCrosswordSolver() {
        viewModel.callCrosswordSolver(focussedEntry ?: defaultEntry)
    }
}
