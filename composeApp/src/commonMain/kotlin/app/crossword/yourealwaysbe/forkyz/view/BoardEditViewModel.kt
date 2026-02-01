
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.Color

import app.crossword.yourealwaysbe.forkyz.settings.DisplaySeparators
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.colorFromInt
import app.crossword.yourealwaysbe.forkyz.util.getOnce
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardListener
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.PuzImage
import app.crossword.yourealwaysbe.puz.Zone

enum class Bar {
    NONE, SOLID, DASHED, DOTTED
}

enum class Shape {
    CIRCLE, ARROW_LEFT, ARROW_RIGHT, ARROW_UP, ARROW_DOWN, TRIANGLE_LEFT,
    TRIANGLE_RIGHT, TRIANGLE_UP, TRIANGLE_DOWN, DIAMOND, CLUB,
    HEART, SPADE, STAR, SQUARE, RHOMBUS, FORWARD_SLASH, BACK_SLASH,
    X
}

/**
 * Image, data in boxes
 */
data class Image(
    val row : Int,
    val col : Int,
    val width : Int,
    val height : Int,
    val url : String,
)

data class FlagsState(
    val flagAcross : Boolean = false,
    val flagAcrossColor : Color? = null,
    val flagDown : Boolean = false,
    val flagDownColor : Color? = null,
    val flagCell : Boolean = false,
    val flagCellColor : Color? = null,
)

data class ScratchState(
    val across : String? = null,
    val down : String? = null,
)

data class BarState(
    val top : Bar = Bar.NONE,
    val bottom : Bar = Bar.NONE,
    val left : Bar = Bar.NONE,
    val right : Bar = Bar.NONE,
    val color : Color? = null,
)

data class Separators(
    val top : String? = null,
    val bottom : String? = null,
    val left : String? = null,
    val right : String? = null,
)

/**
 * State of a cell for rendering
 *
 * @param highlighted is the currently selected cell
 * @param selected is part of the currently selected word
 */
data class BoxState(
    val row : Int,
    val col : Int,
    val isBlock : Boolean = true,
    val isBlank : Boolean = false,
    val response : String = Box.BLANK,
    val clueNumber : String? = null,
    val boxColor : Color? = null,
    val barState : BarState? = null,
    val highlighted : Boolean = false,
    val selected : Boolean = false,
    val cheated : Boolean = false,
    val marks : ImmutableList<ImmutableList<String?>>? = null,
    val flagsState : FlagsState = FlagsState(),
    val scratchState : ScratchState = ScratchState(),
    val textColor : Color? = null,
    val shape : Shape? = null,
    val isError : Boolean = false,
    val separators : Separators = Separators(),
)

/**
 * Information about the currently selected word/box
 */
data class CurrentPositionState(
    val cursorRow : Int,
    val cursorCol : Int,
    val currentWordTopRow : Int,
    val currentWordBottomRow : Int,
    val currentWordLeftCol : Int,
    val currentWordRightCol : Int,
)

/**
 * States of the boxes that need rendering
 *
 * Null BoxState means uninteresting block (black square)
 *
 * @param currentPosition only given if may need to snap to clue
 */
data class BoardState(
    val boxes : ImmutableList<ImmutableList<BoxState?>> = persistentListOf(),
    val pinnedBoxes : ImmutableList<ImmutableList<BoxState?>>? = null,
    val currentPosition : CurrentPositionState? = null,
    val images : ImmutableList<Image> = persistentListOf(),
)

data class WordState(
    val boxes : ImmutableList<BoxState?> = persistentListOf(),
)

data class BoardEditSettings(
    val renderSettings : RenderSettings = RenderSettings(),
    val ensureVisible : Boolean = false,
)

abstract class BaseBoardEditViewModel<BaseBoardState>(
    private val viewModelScope : CoroutineScope,
    protected val settings : ForkyzSettings,
    protected val board : Playboard?,
    val playLetter : (String) -> Unit,
    val deleteLetter : () -> Unit,
) : PlayboardListener {
    protected val settingsFlow : Flow<BoardEditSettings>
        = combine(
            settings.livePlayRenderSettings,
            settings.livePlayEnsureVisible,
            ::BoardEditSettings,
        )
    private lateinit var mediatedBoardState
        : MediatedStateWithFlow<BaseBoardState, BoardEditSettings>
    lateinit var boardState : StateFlow<BaseBoardState>
        private set

    val _newResponseEvent = MutableStateFlow<String?>(null)
    val newResponseEvent : StateFlow<String?> = _newResponseEvent

    /**
     * Must be called during init of derived class
     */
    protected fun initBoardState() {
        mediatedBoardState = MediatedStateWithFlow<
            BaseBoardState, BoardEditSettings
        >(
            viewModelScope,
            getBoardState(BoardEditSettings()),
            { state, boardEditSettings -> getBoardState(boardEditSettings) },
            settingsFlow,
        )
        boardState = mediatedBoardState.stateFlow
    }

    init {
        board?.addListener(this)
        board?.currentResponse?.let { _newResponseEvent.value = it }
    }

    fun onCleared() {
        board?.removeListener(this)
    }

    fun clearNewResponseEvent() {
        _newResponseEvent.value = null
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        // TODO: ignore non-board changes
        updateBoardState(changes)
        updateResponse()
    }

    protected fun updateBoardState(changes : PlayboardChanges) {
        viewModelScope.launch {
            val boardEditSettings = settingsFlow.first()
            mediatedBoardState.current = getBoardState(
                boardEditSettings,
                changes,
                mediatedBoardState.current,
            )
        }
    }

    private fun updateResponse() {
        board?.currentResponse?.let { _newResponseEvent.value = it }
    }

    /**
     * Get fresh board state for current board
     */
    protected abstract fun getBoardState(
        boardEditSettings : BoardEditSettings,
        changes : PlayboardChanges? = null,
        prevState : BaseBoardState? = null,
    ) : BaseBoardState

    /**
     * Get state of a box
     *
     * @param selectedPositions to be highlighted as selected (not
     * highlighted)
     * @param clueID treat as only showing clueID in a horizontal row if
     * not null
     * @param showOwnScratch whether to show scratch (if enabled) of the
     * current displayed
     */
    protected fun getBoxState(
        boardEditSettings : BoardEditSettings,
        row : Int,
        col : Int,
        box : Box?,
        selectedPositions : Set<Position> = setOf(),
        clueID : ClueID? = null,
        showOwnScratch : Boolean = true,
    ) : BoxState? {
        val puz = board?.puzzle
        if (box == null || puz == null)
            return null

        val position = Position(row, col)

        val highlighted = (position == board.highlightLetter)
        val selected = selectedPositions.contains(position)
        val isError = getIsError(boardEditSettings, box, highlighted, selected)
        if (isError)
            box.setCheated(true)
        val cheated = (
            box.isCheated
            && !boardEditSettings.renderSettings.suppressHintHighlighting
        )
        val marks = if (box.hasMarks()) {
            box.marks.map { it.toImmutableList() }.toImmutableList()
        } else null

        return BoxState(
            row = row,
            col = col,
            isBlock = Box.isBlock(box),
            isBlank = box.isBlank(),
            clueNumber = if (box.hasClueNumber()) box.clueNumber else null,
            response = box.response ?: Box.BLANK,
            boxColor = if (box.hasColor()) colorFromInt(box.color) else null,
            // bars don't make sense if showing a horizontal clue
            barState = if (clueID == null) getBarState(box) else null,
            highlighted = highlighted,
            selected = selected,
            cheated = cheated,
            marks = marks,
            flagsState = getFlagsState(box),
            scratchState = getScratchState(
                boardEditSettings,
                box,
                clueID,
                showOwnScratch,
            ),
            textColor
                = if (box.hasTextColor()) colorFromInt(box.textColor) else null,
            shape = if (box.hasShape()) getShape(box.shape) else null,
            isError = isError,
            separators = getSeparators(boardEditSettings, position, clueID),
        )
    }

    private fun getBar(bar : Box.Bar) : Bar {
        return when (bar) {
            Box.Bar.SOLID -> Bar.SOLID
            Box.Bar.DASHED -> Bar.DASHED
            Box.Bar.DOTTED -> Bar.DOTTED
            else -> Bar.NONE
        }
    }

    private fun getBarState(box : Box) : BarState {
        return BarState(
            top = getBar(box.getBarTop()),
            bottom = getBar(box.getBarBottom()),
            left = getBar(box.getBarLeft()),
            right = getBar(box.getBarRight()),
            color = if (box.hasBarColor()) colorFromInt(box.barColor) else null,
        )
    }

    /**
     * Estimate general direction of clue
     *
     * Bias towards across if unsure
     */
    private fun isClueProbablyAcross(cid : ClueID) : Boolean {
        val puz = board?.puzzle
        if (puz == null)
            return true

        val clue = puz.getClue(cid)
        val zone = clue?.getZone();
        if (zone == null || zone.size() <= 1)
            return true

        val pos0 = zone.getPosition(0);
        val pos1 = zone.getPosition(1);

        return pos1.col > pos0.col
    }

    private fun getScratchState(
        boardEditSettings : BoardEditSettings,
        box : Box,
        clueID : ClueID?,
        showOwnScratch : Boolean,
    ) : ScratchState {
        val puz = board?.puzzle
        if (puz == null || !boardEditSettings.renderSettings.displayScratch)
            return ScratchState()

        var scratchAcross : String? = null
        var scratchDown : String? = null

        for (cid in box.getIsPartOfClues()) {
            if (cid == clueID && !showOwnScratch)
                continue
            puz.getNote(cid)?.scratch?.let { scratch ->
                val pos = box.getCluePosition(cid)
                scratch.getOrNull(pos)?.let { scratchChar ->
                    if (!scratchChar.isWhitespace()) {
                        if (isClueProbablyAcross(cid))
                            scratchAcross = scratchChar.toString()
                        else
                            scratchDown = scratchChar.toString()
                    }
                }
            }
            if (scratchAcross != null && scratchDown != null)
                break
        }

        return ScratchState(
            across = scratchAcross,
            down = scratchDown,
        )
    }

    private fun getFlagsState(box : Box) : FlagsState {
        val puz = board?.puzzle
        if (puz == null)
            return FlagsState()

        var flagAcross = false
        var flagAcrossColor : Color? = null
        var flagDown = false
        var flagDownColor : Color? = null

        for (cid in box.getIsPartOfClues()) {
            if (box.isStartOf(cid)) {
                puz.getClue(cid)?.let { clue ->
                    if (clue.isFlagged()) {
                        val color = if (clue.isDefaultFlagColor())
                            null
                        else
                            colorFromInt(clue.flagColor)

                        if (isClueProbablyAcross(cid)) {
                            flagAcross = true
                            flagAcrossColor = color
                        } else {
                            flagDown = true
                            flagDownColor = color
                        }
                    }
                }
            }
            if (flagAcross && flagDown)
                break
        }

        val flagCell = box.isFlagged()
        val flagCellColor = if (box.isDefaultFlagColor())
            null
        else
            colorFromInt(box.flagColor)

        return FlagsState(
            flagAcross = flagAcross,
            flagAcrossColor = flagAcrossColor,
            flagDown = flagDown,
            flagDownColor = flagDownColor,
            flagCell = flagCell,
            flagCellColor = flagCellColor,
        )
    }

    private fun getShape(shape : Box.Shape) : Shape {
        return when (shape) {
            Box.Shape.CIRCLE -> Shape.CIRCLE
            Box.Shape.ARROW_LEFT -> Shape.ARROW_LEFT
            Box.Shape.ARROW_RIGHT -> Shape.ARROW_RIGHT
            Box.Shape.ARROW_UP -> Shape.ARROW_UP
            Box.Shape.ARROW_DOWN -> Shape.ARROW_DOWN
            Box.Shape.TRIANGLE_LEFT -> Shape.TRIANGLE_LEFT
            Box.Shape.TRIANGLE_RIGHT -> Shape.TRIANGLE_RIGHT
            Box.Shape.TRIANGLE_UP -> Shape.TRIANGLE_UP
            Box.Shape.TRIANGLE_DOWN -> Shape.TRIANGLE_DOWN
            Box.Shape.DIAMOND -> Shape.DIAMOND
            Box.Shape.CLUB -> Shape.CLUB
            Box.Shape.HEART -> Shape.HEART
            Box.Shape.SPADE -> Shape.SPADE
            Box.Shape.STAR -> Shape.STAR
            Box.Shape.SQUARE -> Shape.SQUARE
            Box.Shape.RHOMBUS -> Shape.RHOMBUS
            Box.Shape.FORWARD_SLASH -> Shape.FORWARD_SLASH
            Box.Shape.BACK_SLASH -> Shape.BACK_SLASH
            Box.Shape.X -> Shape.X
        }
    }

    private fun getIsError(
        boardEditSettings : BoardEditSettings,
        box : Box,
        highlighted : Boolean,
        selected : Boolean,
    ) : Boolean {
        if (board == null || box.isBlank() || !box.hasSolution())
            return false

        val correct = box.getSolution() == box.getResponse()
        if (correct)
            return false;

        // it's wrong, so when do we highlight?
        if (board.isShowErrorsGrid()) {
            return true
        } else if (board.isShowErrorsCursor() && highlighted) {
            return true
        } else if (board.isShowErrorsClue()) {
            val cid = board.clueID
            return selected && board.isFilledClueID(cid)
        } else {
            return false
        }
    }

    /**
     * Set separator for box and clue at particular offset
     *
     * Takes from offset+1. It looks cleaner on the board to have the
     * separator on e.g. the right instead of left, as it doesn't clash
     * with numbers so much.
     */
    private fun addSeparator(
        clue : Clue,
        inferSeparators : Boolean,
        offset : Int,
        separators : Separators,
        allAcross : Boolean,
    ) : Separators {
        clue.getSeparator(offset + 1, inferSeparators)?.let { separator ->
            val cid = clue.clueID
            val zone = clue.zone
            if (!(zone?.isEmpty() ?: false)) {

                var dir = Zone.Direction.RIGHT

                if (!allAcross) {
                    // try to figure out where to put the separator, default
                    // to right
                    dir = zone.getDirection(offset)
                    if (dir == Zone.Direction.INCONCLUSIVE)
                        dir = zone.getDirection(offset - 1)
                    if (dir == Zone.Direction.INCONCLUSIVE)
                        dir = Zone.Direction.RIGHT
                }

                // on opposite site if coming from start (-1)
                if (offset < 0)
                    dir = dir.reverse()

                return when (dir) {
                    Zone.Direction.UP -> separators.copy(top = separator)
                    Zone.Direction.DOWN -> separators.copy(bottom = separator)
                    Zone.Direction.LEFT -> separators.copy(left = separator)
                    Zone.Direction.RIGHT -> separators.copy(right = separator)
                    else -> separators
                }
            }
        }
        return separators
    }

    /**
     * Add all separators for clue for box
     *
     * @param allAcross whether all separators should be treated as
     * across (right) (useful if only displaying a word in a row)
     */
    private fun addSeparators(
        clue : Clue,
        inferSeparators : Boolean,
        position : Position,
        separators : Separators,
        allAcross : Boolean = false,
    ) : Separators {
        var result = separators
        if (clue.hasSeparators(inferSeparators)) {
            val offset = clue.getZone().indexOf(position)
            result = addSeparator(
                clue,
                inferSeparators,
                offset,
                result,
                allAcross,
            )
            // add start sep if needed
            if (offset == 0)
                result = addSeparator(
                    clue,
                    inferSeparators,
                    -1,
                    result,
                    allAcross,
                )
        }
        return result
    }

    /**
     * Get the separators for the box on the full board
     *
     * May return null if no separators
     */
    private fun getSeparators(
        boardEditSettings : BoardEditSettings,
        position : Position,
        clueID : ClueID?,
    ) : Separators {
        var separators = Separators()

        val renderSettings = boardEditSettings.renderSettings
        val displaySeparators = renderSettings.displaySeparators
        val inferSeparators = renderSettings.inferSeparators

        if (displaySeparators == DisplaySeparators.DS_NEVER)
            return separators

        val puz = board?.puzzle
        val box = puz?.checkedGetBox(position);
        if (puz == null || box == null)
            return separators

        // do other clues first if they should be there
        if (clueID == null) {
            if (displaySeparators == DisplaySeparators.DS_ALWAYS) {
                box.getIsPartOfClues().forEach { cid ->
                    separators = addSeparators(
                        clue = puz.getClue(cid),
                        inferSeparators = inferSeparators,
                        position = position,
                        separators = separators,
                    )
                }
            }
        }

        // current clue takes precedence
        val currentCID = clueID ?: board.currentWord?.clueID
        if (currentCID != null && box.isPartOf(currentCID)) {
            separators = addSeparators(
                clue = puz.getClue(currentCID),
                inferSeparators = inferSeparators,
                position = position,
                separators = separators,
                allAcross = (clueID != null),
            )
        }

        return separators
    }
}

class BoardEditViewModel(
    viewModelScope : CoroutineScope,
    settings : ForkyzSettings,
    board : Playboard?,
    playLetter : (String) -> Unit,
    deleteLetter : () -> Unit,
) : BaseBoardEditViewModel<BoardState>(
    viewModelScope,
    settings,
    board,
    playLetter,
    deleteLetter,
) {
    val width : Int
        get() { return board?.puzzle?.width ?: 0 }
    val height : Int
        get() { return board?.puzzle?.height ?: 0 }
    val pinnedHeight : Int
        get() {
            return if (board?.puzzle?.hasPinnedClueID() ?: false) {
                1 + (
                    (board.puzzle?.pinnedClue?.zone?.size() ?: 0) / width
                )
            } else 0
        }

    private val _scrollToClueEvent
        = MutableStateFlow<CurrentPositionState?>(null)
    val scrollToClueEvent : StateFlow<CurrentPositionState?>
        = _scrollToClueEvent

    init {
        initBoardState()
        scrollToClueIfEnabled()
    }

    fun clearScrollToClueEvent() {
        _scrollToClueEvent.value = null
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        super.onPlayboardChange(changes)
        updateScrollToClue(changes)
    }

    private fun updateScrollToClue(changes : PlayboardChanges) {
        if (changes.currentWord != changes.previousWord)
            scrollToClueIfEnabled()
    }

    private fun scrollToClueIfEnabled() {
        settings.getPlayEnsureVisible { ensureVisible ->
            if (ensureVisible) {
                settingsFlow.getOnce { boardEditSettings ->
                    _scrollToClueEvent.value = getCurrentPositionState(
                        boardEditSettings,
                    )
                }
            }
        }
    }

    /**
     * Get fresh board state for current board
     */
    override protected fun getBoardState(
        boardEditSettings : BoardEditSettings,
        changes : PlayboardChanges?,
        prevState : BoardState?,
    ) : BoardState {
        val puz = board?.puzzle
        if (board == null || puz == null)
            return BoardState()

        val selectedPositions = board.currentWord.zone.toSet()

        val images = board.puzzle?.images?.map {
            Image(
                row = it.row,
                col = it.col,
                width = it.width,
                height = it.height,
                url = it.url,
            )
        }?.toImmutableList() ?: persistentListOf()

        return BoardState(
            boxes = getBoxes(
                boardEditSettings,
                selectedPositions,
                changes,
                prevState,
            ),
            pinnedBoxes = getPinnedBoxes(boardEditSettings, selectedPositions),
            images = images,
        )
    }

    private fun getBoxes(
        boardEditSettings : BoardEditSettings,
        selectedPositions : Set<Position>,
        changes : PlayboardChanges? = null,
        prevState : BoardState? = null,
    ) : ImmutableList<ImmutableList<BoxState?>> {
        val puz = board?.puzzle
        if (puz == null)
            return persistentListOf()

        return (0 until height).map { row ->
            (0 until width).map { col ->
                val box = puz.checkedGetBox(row, col)
                val changedBox = (
                    changes
                        ?.cellChanges
                        ?.contains(Position(row, col))
                        ?: true
                ) || (
                    changes
                        .changedClueIDs
                        ?.any { box?.isPartOf(it) ?: false }
                        ?: true
                )
                if (!changedBox && prevState != null) {
                    prevState.boxes.getOrNull(row)?.getOrNull(col)
                } else {
                    getBoxState(
                        boardEditSettings,
                        row,
                        col,
                        box,
                        selectedPositions,
                    )
                }
            }.toImmutableList()
        }.toImmutableList()
    }

    private fun getPinnedBoxes(
        boardEditSettings : BoardEditSettings,
        selectedPositions : Set<Position>,
    ) : ImmutableList<ImmutableList<BoxState?>>? {
        val puz = board?.puzzle
        if (puz == null)
            return null

        return if (puz.hasPinnedClueID()) {
            puz.pinnedClue?.zone?.let { zone ->
                if (zone.size() <= width) {
                    val padding = List((width - zone.size()) / 2) { col ->
                        BoxState(height, col)
                    }
                    val pinned = zone.map { pos ->
                        val box = puz.checkedGetBox(pos)
                        getBoxState(
                            boardEditSettings,
                            pos.row,
                            pos.col,
                            box,
                            selectedPositions,
                        )
                    }.toImmutableList()
                    persistentListOf((padding + pinned).toImmutableList())
                } else {
                    val rows = zone.size() / width
                    (0 until rows).map { row ->
                        (0 until width).map { col ->
                            val pos = zone.getPosition(row * width + col)
                            val box = puz.checkedGetBox(pos)
                            getBoxState(
                                boardEditSettings,
                                pos.row,
                                pos.col,
                                box,
                                selectedPositions,
                            )
                        }.toImmutableList()
                    }.toImmutableList()
                }
            }
        } else null
    }

    /**
     * Returns null if scroll to clue not enabled
     */
    private fun getCurrentPositionState(
        boardEditSettings : BoardEditSettings,
    ) : CurrentPositionState? {
        if (board == null || !boardEditSettings.ensureVisible)
            return null

        val cursor = board.highlightLetter
        if (cursor == null)
            return null

        val selectedPositions = board.currentWord.zone
        if (selectedPositions.isEmpty)
            return null

        val topRow = selectedPositions.map { it.row }.min()
        val bottomRow = selectedPositions.map { it.row }.max()
        val leftCol = selectedPositions.map { it.col }.min()
        val rightCol = selectedPositions.map { it.col }.max()

        return CurrentPositionState(
            cursorRow = cursor.row,
            cursorCol = cursor.col,
            currentWordTopRow = topRow,
            currentWordBottomRow = bottomRow,
            currentWordLeftCol = leftCol,
            currentWordRightCol = rightCol,
        )
    }
}

/**
 * Word edit view model for word
 *
 * Or current word if word is null
 *
 * @param showOwnScratch whether to show scratch for current word being
 * displayed if scratch view enabled
 */
class WordEditViewModel(
    viewModelScope : CoroutineScope,
    settings : ForkyzSettings,
    board : Playboard?,
    playLetter : (String) -> Unit,
    deleteLetter : () -> Unit,
    private val word : Word? = null,
    private val showOwnScratch : Boolean = true,
) : BaseBoardEditViewModel<WordState>(
    viewModelScope,
    settings,
    board,
    playLetter,
    deleteLetter,
) {
    init { initBoardState() }

    /**
     * Get fresh board state for current board
     */
    override protected fun getBoardState(
        boardEditSettings : BoardEditSettings,
        changes : PlayboardChanges?,
        prevState : WordState?,
    ) : WordState {
        // TODO: use changes
        val puz = board?.puzzle
        val shownWord = word ?: board?.currentWord
        val zone = shownWord?.zone
        if (board == null || puz == null || zone == null)
            return WordState()

        return WordState(
            boxes = zone.map { pos ->
                if (pos == null) {
                    null
                } else {
                    getBoxState(
                        boardEditSettings,
                        pos.row,
                        pos.col,
                        puz.checkedGetBox(pos),
                        setOf(),
                        shownWord.clueID,
                        showOwnScratch,
                    )
                }
            }.toImmutableList(),
        )
    }
}
