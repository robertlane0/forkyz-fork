
package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import android.app.Application
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.settings.CycleUnfilledMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.getOnce
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.forkyz.view.ClueTabsViewModel
import app.crossword.yourealwaysbe.forkyz.view.WordEditViewModel
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.MovementStrategy
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position

@HiltViewModel
class ClueListPageViewModel @Inject constructor(
    application : Application,
    settings : ForkyzSettings,
    currentPuzzleHolder : CurrentPuzzleHolder,
    fileHandlerProvider : FileHandlerProvider,
    utils : AndroidVersionUtils,
) : PuzzlePageViewModel(
    application,
    settings,
    currentPuzzleHolder,
    fileHandlerProvider,
    utils,
) {
    init {
        setupVoiceCommands()
    }

    /**
     * Keep a logical current page even if clue tabs displays two pages
     *
     * Maintain this always shown on some panel, use it to decide which
     * page is next/prev when changing.
     */
    private var currentPageNum : Int = 0

    // these should become a full state if too numerous!
    val showAllWords : StateFlow<Boolean> = stateIn(
        settings.liveClueListShowWords,
        false,
    )

    val renderSettings : StateFlow<RenderSettings> = stateIn(
        settings.livePlayRenderSettings,
        RenderSettings(),
    )

    val wordEditViewModel = WordEditViewModel(
        viewModelScope,
        settings,
        getBoard(),
        this::playLetter,
        this::deleteLetter,
    )

    val clueTabsViewModel = ClueTabsViewModel(
        application,
        viewModelScope,
        settings,
        getBoard(),
        this::playLetter,
        this::deleteLetter,
    )

    fun toggleShowAllWords() {
        val current = showAllWords.value ?: false
        settings.setClueListShowWords(!current)
    }

    /**
     * General method for any part of a clue tab click
     */
    fun clueClick(cid : ClueID) {
        getBoard()?.let { board ->
            board.puzzle.getClue(cid)?.let { clue ->
                if (clue.hasZone()) {
                    val oldWord = board.getCurrentWord()
                    if (cid != board.clueID)
                        board.jumpToClue(clue)
                    displayKeyboard(oldWord)
                }
            }
        }
    }

    fun deleteLetter() {
        getBoard()?.let { board ->
            settings.getPlayScratchMode { scratchMode ->
                movementStrategyFlow.getOnce { movementStrategy ->
                    val oldMovementStrategy = board.getMovementStrategy()
                    board.setMovementStrategy(movementStrategy)
                    if (scratchMode)
                        board.deleteScratchLetter()
                    else
                        board.deleteOrUndoLetter()
                    board.setMovementStrategy(oldMovementStrategy)
                }
            }
        }
    }

    fun playSpace() {
        playLetter(' ')
    }

    fun playLetter(c : Char) {
        playLetter(c.toString())
    }

    fun playLetter(c : String) {
        getBoard()?.let { board ->
            settings.getPlayScratchMode { scratchMode ->
                movementStrategyFlow.getOnce { movementStrategy ->
                    val oldMovementStrategy = board.getMovementStrategy()
                    board.setMovementStrategy(movementStrategy)
                    if (scratchMode)
                        board.playScratchLetter(c)
                    else
                        board.playLetter(c)
                    board.setMovementStrategy(oldMovementStrategy)
                }
            }
        }
    }

    fun moveLeft() {
        getBoard()?.let { board ->
            val first = getCurrentFirstPosition()
            if (!board.getHighlightLetter().equals(first)) {
                board.moveZoneBack(false)
                snapToClue()
            } else {
                movePrevPage()
            }
        }
    }

    fun moveRight() {
        getBoard()?.let { board ->
            val last = getCurrentLastPosition()
            if (!board.getHighlightLetter().equals(last)) {
                board.moveZoneForward(false)
                snapToClue()
            } else {
                moveNextPage()
            }
        }
    }

    fun moveUp() {
        if (!isCurrentPageHistory) {
            getBoard()?.let { board ->
                getPuzzle()?.let { puz ->
                    val cid = board.getClueID()
                    val curList = cid?.getListName()
                    val curClueIndex = cid?.getIndex() ?: -1

                    if (curList != null && curClueIndex >= 0) {
                        val prev = puz.getClues(curList)
                            .getPreviousZonedIndex(curClueIndex, true)
                        board.jumpToClue(ClueID(curList, prev))
                        snapToClue()
                    } else {
                        selectFirstSelectableClue(currentPageNum, false)
                    }
                }
            }
        }
    }

    fun moveDown() {
        if (isCurrentPageHistory) {
            getBoard()?.let { board ->
                getPuzzle()?.let { puz ->
                    val history = puz.getHistory()
                    if (history.size > 2) {
                        // always at top of history
                        board.jumpToClue(history.get(1))
                        snapToClue()
                    }
                }
            }
        } else {
            getBoard()?.let { board ->
                getPuzzle()?.let { puz ->
                    val cid = board.getClueID()
                    val curList = cid?.getListName()
                    val curClueIndex = cid?.getIndex() ?: -1

                    if (curList != null && curClueIndex >= 0) {
                        val next = puz.getClues(curList)
                            .getNextZonedIndex(curClueIndex, true)
                        board.jumpToClue(ClueID(curList, next))
                        snapToClue()
                    } else {
                        selectFirstSelectableClue(currentPageNum, true)
                    }
                }
            }
        }
    }

    private fun displayKeyboard(previousWord : Word?) {
        // only show keyboard if double click a word
        // else hide
        getBoard()?.let { board ->
            val newPos = board.getHighlightLetter()
            if (
                previousWord != null
                && previousWord.checkInWord(newPos.row, newPos.col)
            ) {
                showKeyboard()
            } else {
                hideKeyboard()
            }
        }
    }

    private fun setupVoiceCommands() {
        registerVoiceCommandAnswer()
        registerVoiceCommandLetter()
        registerVoiceCommandClear()
        registerVoiceCommandNumber()
        registerVoiceCommandAnnounceClue()
        registerVoiceCommandClueHelp()
        registerVoiceCommandBack()
        registerVoiceCommandNotes()

        val app : Application = getApplication()

        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_left),
            { _ -> moveLeft() }
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_right),
            { _ -> moveRight() }
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_up),
            { _ -> moveUp() }
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_down),
            { _ -> moveDown() }
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_next),
            { _ -> moveNextPage() },
        ))
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_previous),
            { _ -> movePrevPage() },
        ))
    }

    /**
     * Return next page num if don't fall off end
     */
    private fun getNextPageNum() : Int? {
        return getNextPageNum(currentPageNum)
    }

    /**
     * Return next page num if don't fall off end
     */
    private fun getNextPageNum(pageNum : Int) : Int? {
        return if (pageNum < clueTabsViewModel.numPages) pageNum + 1 else null
    }

    /**
     * Return prev page num if don't fall off end
     */
    private fun getPrevPageNum() : Int? {
        return getPrevPageNum(currentPageNum)
    }

    private fun getPrevPageNum(pageNum : Int) : Int? {
        return if (pageNum > 0) pageNum - 1 else null
    }

    private fun moveNextPage() {
        getNextPageNum()?.let { page ->
            selectFirstSelectableClue(page, true)
        }
    }

    private fun movePrevPage() {
        getPrevPageNum()?.let { page ->
            selectFirstSelectableClue(page, false)
        }
    }

    private fun snapToClue(showPageNum : Int? = null) {
        var newPageNum = -1
        if (showPageNum == null) {
            // change current page state
            if (isCurrentPageHistory) {
                newPageNum = clueTabsViewModel.snapToClue(
                    clueTabsViewModel.historyPageNum,
                )
            } else {
                // switch to page of current view
                getBoard()?.clueID?.listName?.let { curList ->
                    newPageNum = clueTabsViewModel.getPageNum(curList)
                    if (newPageNum >= 0) {
                        newPageNum = clueTabsViewModel.snapToClue(newPageNum)
                    }
                }
            }
        } else {
            newPageNum = clueTabsViewModel.snapToClue(showPageNum)
        }

        if (newPageNum >= 0)
            currentPageNum = newPageNum
    }

    /**
     * First position of current word or null
     */
    private fun getCurrentFirstPosition() : Position? {
        val zone = getBoard()?.getCurrentWord()?.getZone()
        if (zone != null && !zone.isEmpty())
            return zone.getPosition(0)
        else
            return null
    }

    /**
     * Last position of current word or null
     */
    private fun getCurrentLastPosition() : Position? {
        val zone = getBoard()?.getCurrentWord()?.getZone()
        if (zone != null && !zone.isEmpty())
            return zone.getPosition(zone.size() - 1)
        else
            return null
    }

    /**
     * Selects first selectable clue from startPageNum
     *
     * Starts flipping pages in search direction if needed. Does nothing
     * if none found in direction.
     */
    private fun selectFirstSelectableClue(
        startPageNum : Int,
        searchForwards : Boolean,
    ) {
        var selectedClue : Boolean
        var pageNum : Int? = startPageNum
        do {
            selectedClue = selectFirstClueOnPage(pageNum!!)
            if (!selectedClue) {
                if (searchForwards)
                    pageNum = getNextPageNum(pageNum)
                else
                    pageNum = getPrevPageNum(pageNum)
            }
        } while (pageNum != null && !selectedClue)
    }

    /**
     * Select first clue on pageNum
     *
     * @return true if a selectable clue found
     */
    private fun selectFirstClueOnPage(pageNum : Int) : Boolean {
        val board = getBoard()
        val puz = getPuzzle()
        if (board == null || puz == null)
            return false

        val showPageNum = if (pageNum == currentPageNum)
            null
        else
            pageNum

        if (clueTabsViewModel.isHistoryPage(pageNum)) {
            for (cid in puz.getHistory()) {
                val clue = puz.getClue(cid)
                if (clue?.hasZone() ?: false) {
                    board.jumpToClue(clue)
                    snapToClue(showPageNum)
                    return true
                }
            }
        } else {
            val pages = clueTabsViewModel.pages
            if (0 <= pageNum && pageNum <= pages.size - 1) {
                val listName = pages[pageNum].listName
                val firstClue = puz.getClues(listName).getFirstZonedIndex()
                if (firstClue >= 0) {
                    board.jumpToClue(ClueID(listName, firstClue))
                    snapToClue(showPageNum)
                    return true
                }
            }
        }
        return false
    }

    // last page is always history
    private val isCurrentPageHistory : Boolean
        get() {
            return clueTabsViewModel.isHistoryPage(currentPageNum)
        }

    private fun <T> stateIn(flow : Flow<T>, initialValue : T) : StateFlow<T> {
        return flow.stateInSubscribed(viewModelScope, initialValue)
    }

    private val movementStrategyFlow : StateFlow<MovementStrategy> = stateIn(
        settings.livePlayMovementStrategyClueList,
        MovementStrategy.MOVE_NEXT_CLUE,
    )
    init {
        movementStrategyFlow.getOnce {
            // Because the flow is only active when subscribed this value
            // will be behind by one keypress, so make sure it is correct at
            // first by an initial getOnce.
        }
    }
}
