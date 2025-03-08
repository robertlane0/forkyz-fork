
package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import android.app.Application
import androidx.lifecycle.LiveData

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.settings.CycleUnfilledMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.VoiceCommands.VoiceCommand
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.getStopOnEndStrategy
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.MovementStrategy
import app.crossword.yourealwaysbe.puz.Playboard.Word

/**
 * The state of the clue tabs widget
 *
 * We are tracking a "primary" list number. Although the clue tabs may
 * display two lists, only one is the main one that is used as the
 * reference point for left/right actions and other key events.
 *
 * Not ideal at the moment that this has to keep in sync with ClueTabs,
 * but will become ClueTabs ViewModel when ClueTabs is Compose.
 *
 * @param numPages how many pages there are (number of clue lists +
 * history)
 * @param currentPageNum the current primary list number
 */
data class ClueTabsState(
    val currentPageNum : Int = 0,
    val listNames : List<String> = listOf(),
) {
    // one for each list plus history
    val numPages : Int
        get() { return listNames.size + 1 }

    // last page is always history
    val isCurrentPageHistory : Boolean
        get() { return isHistoryPage(currentPageNum) }

    fun isHistoryPage(pageNum : Int) : Boolean {
        return pageNum == numPages - 1;
    }
}

/**
 * Data for changing page during snap to clue events
 *
 * @param showPageNum ensure this page is shown
 * @param replacePageNum replace this page when ensuring if shown
 */
data class PageChangeData(
    val showPageNum : Int,
    val replacePageNum : Int,
)

/**
 * Data for snap to clue
 *
 * @param pageChange if not null, follow page change data during snap
 */
data class SnapToClueEvent(
    val pageChangeData : PageChangeData? = null,
)

@HiltViewModel
class ClueListActivityViewModel @Inject constructor(
    application : Application,
    settings : ForkyzSettings,
    currentPuzzleHolder : CurrentPuzzleHolder,
    fileHandlerProvider : FileHandlerProvider,
    utils : AndroidVersionUtils,
) : PuzzleActivityViewModel(
    application,
    settings,
    currentPuzzleHolder,
    fileHandlerProvider,
    utils,
) {
    init {
        setupVoiceCommands()
    }

    private val _snapToClueEvent = MutableStateFlow<SnapToClueEvent?>(null)
    private val _clueTabsState
        = MutableStateFlow<ClueTabsState>(
            ClueTabsState(
                listNames = getPuzzle()
                    ?.getClueListNames()
                    ?.sorted()
                    ?: listOf()
            )
        )

    val snapToClueEvent : StateFlow<SnapToClueEvent?> = _snapToClueEvent
    val showAllWords : LiveData<Boolean> = settings.liveClueListShowWords
    val clueTabsState : StateFlow<ClueTabsState> = _clueTabsState

    fun toggleShowAllWords() {
        val current = showAllWords.value ?: false
        settings.setClueListShowWords(!current)
    }

    /**
     * General method for any part of a clue tab click
     */
    fun clueClick(clue : Clue?) {
        getBoard()?.let { board ->
            clue?.let { clue ->
                if (clue.hasZone()) {
                    val oldWord = board.getCurrentWord()
                    if (clue.getClueID() != board.getClueID())
                        board.jumpToClue(clue)
                    displayKeyboard(oldWord)
                }
            }
        }
    }

    fun deleteLetter() {
        getBoard()?.let { board ->
            settings.getPlayScratchMode() { scratchMode ->
                val movementStrategy = board.getMovementStrategy()
                getStopOnEndStrategy(settings) { stopOnEnd ->
                    board.setMovementStrategy(stopOnEnd)
                    if (scratchMode)
                        board.deleteScratchLetter()
                    else
                        board.deleteOrUndoLetter()
                    board.setMovementStrategy(movementStrategy)
                }
            }
        }
    }

    fun playSpace() {
        settings.getPlaySpaceChangesDirection() { spaceChangesDirection ->
            if (!spaceChangesDirection) {
                playLetter(' ')
            }
        }
    }

    fun playLetter(c : Char) {
        getBoard()?.let { board ->
            settings.getPlayScratchMode() { scratchMode ->
                val movementStrategy = board.getMovementStrategy()
                getStopOnEndStrategy(settings) { stopOnEnd ->
                    board.setMovementStrategy(stopOnEnd)
                    if (scratchMode)
                        board.playScratchLetter(c)
                    else
                        board.playLetter(c)
                    board.setMovementStrategy(movementStrategy)
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
                        selectFirstSelectableClue(
                            currentPageNum,
                            false,
                        )
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
                        selectFirstSelectableClue(
                            currentPageNum,
                            true,
                        )
                    }
                }
            }
        }
    }

    fun clearSnapToClueEvent() {
        _snapToClueEvent.value = null
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
        registerVoiceCommand(VoiceCommand(
            app.getString(R.string.command_notes),
            { _ -> launchCurrentClueNotes() },
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
        if (pageNum < clueTabsState.value.numPages - 1) {
            return pageNum + 1
        } else {
            return null
        }
    }

    /**
     * Return prev page num if don't fall off end
     */
    private fun getPrevPageNum() : Int? {
        return getPrevPageNum(currentPageNum)
    }

    private fun getPrevPageNum(pageNum : Int) : Int? {
        if (pageNum > 0) {
            return pageNum - 1
        } else {
            return null
        }
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

    private fun snapToClue(
        pageChangeData : PageChangeData? = null
    ) {
        _snapToClueEvent.value = SnapToClueEvent(pageChangeData)

        if (pageChangeData == null) {
            // change current page state
            if (isCurrentPageHistory) {
                // no need to change current page
            } else {
                // switch to page of current clue
                val board = getBoard()
                val puz = getPuzzle()
                if (board == null || puz == null)
                    return

                val cid = board.getClueID()
                val curList = cid?.getListName()
                val newPageNum = clueTabsState.value.listNames.indexOf(curList)
                if (newPageNum >= 0) {
                    _clueTabsState.value = clueTabsState.value.copy(
                        currentPageNum = newPageNum
                    )
                }
            }
        } else {
            _clueTabsState.value = clueTabsState.value.copy(
                currentPageNum = pageChangeData.showPageNum
            )
        }
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

        val pageChangeData = if (pageNum == currentPageNum)
            null
        else
            PageChangeData(pageNum, currentPageNum)

        if (clueTabsState.value.isHistoryPage(pageNum)) {
            for (cid in puz.getHistory()) {
                val clue = puz.getClue(cid)
                if (clue?.hasZone() ?: false) {
                    board.jumpToClue(clue)
                    snapToClue(pageChangeData)
                    return true
                }
            }
        } else {
            val listNames = clueTabsState.value.listNames
            if (0 <= pageNum && pageNum <= listNames.size - 1) {
                val listName = listNames[pageNum]
                val firstClue = puz.getClues(listName).getFirstZonedIndex()
                if (firstClue >= 0) {
                    board.jumpToClue(ClueID(listName, firstClue))
                    snapToClue(pageChangeData)
                    return true
                }
            }
        }
        return false
    }

    private val isCurrentPageHistory : Boolean
        get() { return clueTabsState.value.isCurrentPageHistory }

    private val currentPageNum : Int
        get() { return clueTabsState.value.currentPageNum }
}
