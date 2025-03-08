
package app.crossword.yourealwaysbe.forkyz

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.lifecycle.ViewModelProvider

import app.crossword.yourealwaysbe.forkyz.util.ColorUtils
import app.crossword.yourealwaysbe.forkyz.view.BoardEditText
import app.crossword.yourealwaysbe.forkyz.view.BoardEditText.BoardEditFilter
import app.crossword.yourealwaysbe.forkyz.view.BoardEditView.BoardClickListener
import app.crossword.yourealwaysbe.forkyz.view.BoardWordEditView
import app.crossword.yourealwaysbe.forkyz.view.ScrollingImageView
import app.crossword.yourealwaysbe.forkyz.view.ScrollingImageView.Point
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position

private val VERTICAL_BOARD_PADDING = 6.dp

class NotesActivity() : PuzzleActivityKt() {
    companion object {
        // TODO: JvmField can be removed when PlayActivity is Compose
        @JvmField
        val CLUE_NOTE_LISTNAME = "clueNoteListName"
        @JvmField
        val CLUE_NOTE_INDEX = "clueNoteIndex"
    }

    override protected lateinit var viewModel : NotesActivityViewModel

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

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun TopAppBar() {
        val state = viewModel.notesUIState.observeAsState()
        themeHelper.ForkyzTopAppBar(
            title = {
                if (state.value?.isPuzzleNotes ?: false) {
                    Text(stringResource(R.string.menu_notes_puzzle))
                } else {
                    ClueText(state.value?.clueText)
                }
            },
            onBack = { finish() },
            actions = { OverflowMenu() },
        )
    }

    @Composable
    private fun ActivityBody(modifier : Modifier) {
        Column(
            modifier = modifier.fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .height(IntrinsicSize.Max),
        ) {
            val state = viewModel.notesUIState.observeAsState()
            val isPuzzleNotes = state.value?.isPuzzleNotes ?: false

            if (!isPuzzleNotes) {
                MiniboardRow()
            }
            ScratchRow()
            TextRow(modifier = Modifier.weight(1.0F))
            FlagRow()
            AnagramSourceRow()
            AnagramSolutionRow()
            VoiceButtons()
            Keyboard()
        }
    }

    @Composable
    private fun MiniboardRow() {
        val state = viewModel.notesUIState.observeAsState()

        BoardLabel(R.string.board_lab)
        // TODO: getsuppressnoteslist
        Miniboard(
            modifier = Modifier.padding(
                vertical = VERTICAL_BOARD_PADDING
            ),
            onClick = { _, _ -> viewModel.showKeyboard() },
            onLongClick = { /* TODO */ },
            longClickAccessibilityAction
                = R.string.transfer_board_to_focussed_view,
            ignoreScratchMode = true,
            useSuppressNotesList = state.value != null,
            suppressNotesList = state.value?.suppressNotesList,
        )
    }

    @Composable
    private fun ScratchRow() {
        val state = viewModel.notesUIState.observeAsState()

        BoardLabel(R.string.scratch_lab)
        BoardEdit(
            value = state.value?.scratchValue ?: "",
            contentDescription = R.string.scratch_lab,
            onClick = {
                // TODO: show keyboard for view!
                viewModel.showKeyboard()
            },
            onLongClick = {
                // TODO
                // int id = getCurrentFocus(R.id.miniboard)
                // if (id == R.id.notes_box) {
                //     moveBoardEditTextToNotes(binding.scratchMiniboard)
                // } else if (id == R.id.anagram_source) {
                //     executeTransferResponseRequest(
                //         TransferResponseRequest.SCRATCH_TO_ANAGRAM_SOURCE,
                //         true
                //     )
                // } else if (id == R.id.anagram_solution) {
                //     executeTransferResponseRequest(
                //         TransferResponseRequest.SCRATCH_TO_ANAGRAM_SOL,
                //         true
                //     )
                // } else {
                //     if (isClueOnBoard()) {
                //         executeTransferResponseRequest(
                //             TransferResponseRequest.SCRATCH_TO_BOARD, true
                //         )
                //     }
                // }
            },
            postChange = { _, _, _, value ->
                viewModel.setScratchValue(value)
            },
            longClickAccessibilityAction
                = R.string.transfer_scratch_to_focussed_view,
        )
    }

    @Composable
    private fun TextRow(modifier : Modifier) {
        val state = viewModel.notesUIState.observeAsState()
        val textValue = state.value?.textValue ?: ""
        val hintStringID = if (state.value?.isPuzzleNotes ?: false)
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
                .onFocusChanged({
                    viewModel.onFocusNativeView(it.hasFocus)
                }),
            textStyle = textStyle,
            singleLine = false,
            minLines = 2,
            value = textValue,
            onValueChange = viewModel::setTextValue,
            placeholder = { Text(stringResource(hintStringID)) },
        )
    }

    @Composable
    private fun FlagRow() {
        val state = viewModel.notesUIState.observeAsState()
        state.value?.let { stateValue ->
            if (!stateValue.isPuzzleNotes) {
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
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val clueFlagColor = stateValue.clueFlagColor
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
                            checked = stateValue.clueFlagged,
                            // needs to be null so parent gets long click
                            onCheckedChange = null,
                            colors = checkColors,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun AnagramSourceRow() {
        //val state = viewModel.notesUIState.observeAsState()

        BoardLabel(R.string.anagram_source_lab)
        BoardEdit(
            value = "",
            contentDescription = R.string.anagram_source_lab,
            onClick = {
                // TODO: show keyboard for view!
                viewModel.showKeyboard()
            },
            onLongClick = {
                // TODO
            },
            postChange = { _, _, _, _ ->
                // TODO
            },
            longClickAccessibilityAction
                = R.string.shuffle_anagram_source,
        )
    }

    @Composable
    private fun AnagramSolutionRow() {
        //val state = viewModel.notesUIState.observeAsState()

        BoardLabel(R.string.anagram_solution_lab)
        BoardEdit(
            value = "",
            contentDescription = R.string.anagram_source_lab,
            onClick = {
                // TODO: show keyboard for view!
                viewModel.showKeyboard()
            },
            onLongClick = {
                // TODO
            },
            postChange = { _, _, _, _ ->
                // TODO
            },
            longClickAccessibilityAction
                = R.string.transfer_anagram_solution_to_focussed_view,
        )
    }

    @Composable
    private fun BoardLabel(stringID : Int) {
        Text(stringResource(stringID))
    }

    @Composable
    private fun OverflowMenu() {
        val state by viewModel.menuState.observeAsState()

        IconButton(onClick = { viewModel.expandMenu(SubMenu.MAIN) }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.overflow),
            )
        }
        DropdownMenu(
            modifier = BASE_MENU_MODIFIER,
            expanded = state?.expanded == SubMenu.MAIN,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuClueFlagToggle()
            MenuCellFlagToggle()
            MenuSpecialEntry()
            MenuShowErrors()
            MenuReveal()
            MenuExternalTools()
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
    private fun MenuClueFlagToggle() {
        DropdownMenuItem(
            text = {
                MenuText(stringResource(R.string.menu_flag_clue_toggle))
            },
            onClick = viewModel::toggleClueFlag,
        )
    }

    @Composable
    private fun MenuCellFlagToggle() {
        DropdownMenuItem(
            text = {
                MenuText(stringResource(R.string.menu_flag_cell_toggle))
            },
            onClick = viewModel::toggleCellFlag,
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
     */
    @Composable
    protected fun BoardEdit(
        value : String,
        contentDescription : Int,
        onClick : () -> Unit,
        onLongClick : () -> Unit,
        longClickAccessibilityAction : Int,
        delete : ((Char, Int) -> Boolean)? = null,
        filter : ((Char, Char, Int) -> Char)? = null,
        postChange : ((Int, Char, Boolean, String) -> Unit)? = null,
        //incognitoMode : Boolean = false,
        //ignoreScratchMode : Boolean = false,
    ) {
        val state = viewModel.notesUIState.observeAsState()

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
                            if (delete == null)
                                return true
                            else
                                return delete(oldChar, pos)
                        }

                        override fun filter(
                            oldChar : Char,
                            newChar : Char,
                            pos : Int,
                        ) : Char {
                            if (filter == null)
                                return newChar
                            else
                                return filter(oldChar, newChar, pos)
                        }

                        override fun postChange(
                            pos : Int,
                            oldChar : Char,
                            isDelete : Boolean,
                        ) {
                            if (postChange != null) {
                                postChange(
                                    pos,
                                    oldChar,
                                    isDelete,
                                    view.toString(),
                                )
                            }
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
                })

                view
            },
            update = { view ->
                view.apply {
                    setLength(state.value?.boxesLength ?: 0)
                    setFromString(value)
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
}
