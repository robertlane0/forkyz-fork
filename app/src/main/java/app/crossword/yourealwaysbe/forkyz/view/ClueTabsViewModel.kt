
package app.crossword.yourealwaysbe.forkyz.view

import android.app.Application
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ClueHighlight
import app.crossword.yourealwaysbe.forkyz.settings.ClueTabsDouble
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.ColorUtils
import app.crossword.yourealwaysbe.forkyz.util.liveDataFanIn
import app.crossword.yourealwaysbe.forkyz.util.observeOnceNonNull
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardListener
import app.crossword.yourealwaysbe.puz.Playboard.Word

/**
 * Indicates that panel should skip to clue on page
 */
data class SnapToClueEvent(
    val paneIndex : Int,
    val clueIndex : Int,
)

/**
 * Data for a clue item
 *
 * @param isFlagged if the clue if flagged at all
 * @param flagColor non-null if flagged and non-default color
 */
data class ClueItem(
    val cid : ClueID,
    val text : String,
    val isFlagged : Boolean,
    val flagColor : Int?,
    val filled : Boolean,
    val selected : Boolean,
    val word : Word?,
)

data class ClueList(
    val listName : String,
    val clues : List<ClueItem>,
)

/**
 * ClueTabsState
 *
 * @param doubleMode when to show two panes
 * @param listName the list of clue list names
 * @param panePages list of current pages of the two panes
 * @param highlightSelectedRadio use radio buttons to highlight selected
 * clue
 * @param highlightSelectedHighlight use background highlight to highlight
 * selected clue
 * @param snapToClueEvent whether needs to snap to a clue
 * clearSnapToClue event once done
 */
data class ClueTabsState(
    val doubleMode : ClueTabsDouble = ClueTabsDouble.CTD_NEVER,
    val pages : List<ClueList> = listOf(),
    val panePages : List<Int> = listOf(0, 1),
    val highlightSelectedRadio : Boolean = true,
    val highlightSelectedBackground : Boolean = false,
    val snapToClueEvent : SnapToClueEvent? = null,
) {
    val numPages : Int
        get() { return pages.size }

    fun isHistoryPage(pageNum : Int) : Boolean {
        return pageNum == historyPageNum
    }

    val historyPageNum : Int = numPages - 1

    /**
     * -1 if not in list
     */
    fun getHistoryIndex(cid : ClueID) : Int {
        return pages[historyPageNum].clues.indexOfFirst { clue ->
            clue.cid == cid
        }
    }
}

class ClueTabsViewModel(
    private val application : Application,
    private val settings : ForkyzSettings,
    val board : Playboard?,
) : PlayboardListener {
    private val _uiState : MutableLiveData<ClueTabsState> = liveDataFanIn(
        { getUpdatedUIState(refreshClueList = false) } ,
        settings.liveClueListClueTabsDouble,
        settings.liveClueListShowWords,
        settings.livePlayClueHighlight,
        // snap only really here to make the live data value accurate via an
        // observer
        settings.liveClueListSnapToClue,
    )
    val uiState : LiveData<ClueTabsState> = _uiState

    val numPages : Int
        get() { return uiState.value?.numPages ?: 0 }

    val pages : List<ClueList>
        get() { return uiState.value?.pages ?: listOf() }

    val historyPageNum : Int
        get() { return uiState.value?.historyPageNum ?: 0 }

    /**
     * View should set this to track if it is showing both panes
     *
     * Is used during snap to clue behaviour to decide which pane to
     * update.
     */
    var isDouble : Boolean = false
        set(value) {
            field = value
            // now we know double status, do a snap to clue, but wait until
            // state first initialised
            uiState.observeOnceNonNull { state ->
                snapToClueIfEnabled()
            }
        }

    init {
        board?.addListener(this)
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        _uiState.value = getUpdatedUIState(refreshClueList = true)
        if (changes.previousWord != board?.currentWord)
            snapToClueIfEnabled()
    }

    /**
     * Page num of page with listName or -1
     */
    fun getPageNum(listName : String) : Int {
        return pages.indexOfFirst { page -> page.listName == listName }
    }

    /**
     * If page num is history page
     */
    fun isHistoryPage(pageNum : Int) : Boolean {
        return uiState.value?.isHistoryPage(pageNum) ?: false
    }

    /**
     * Set page for pane
     *
     * @param index the index of the pane (0, 1)
     * @param page the page number (0 to num pages)
     */
    fun setPage(index : Int, page : Int) {
        val state = uiState.value ?: getUpdatedUIState(refreshClueList = true)
        if (0 <= page && page < state.numPages) {
            _uiState.value = state.copy(
                panePages = state.panePages.mapIndexed { i, v ->
                    if (i == index) page else v
                }
            )
        }
    }

    /**
     * Snap to currently selected clue
     *
     * Ensure that showPageNum is show (if >= 0), replacing replacePageNum if
     * specified. E.g. if showPageNum is the history page it will snap on the
     * history page rather than the main list containing the clue.
     *
     * If not in the history list, will always use page of clue's list.
     *
     * @return page number snapped to or -1 if no change
     */
    fun snapToClue(showPageNum : Int = -1, replacePageNum : Int = -1) : Int {
        val cid = board?.clueID
        if (cid == null)
            return -1

        val listName = cid.listName
        if (listName == null)
            return -1

        val historyPageNum = uiState.value?.historyPageNum
        val historyIndex = uiState.value?.getHistoryIndex(cid) ?: -1

        val pageNum = if (showPageNum == historyPageNum  && historyIndex >= 0)
            historyPageNum
        else
            getPageNum(listName)

        if (showPageNum >= 0)
            ensurePageShown(showPageNum, replacePageNum)
        else if (pageNum >= 0)
            ensurePageShown(pageNum, replacePageNum)
        else
            return -1

        val curPage0 = uiState.value?.panePages?.get(0)
        val curPage1 = uiState.value?.panePages?.get(1)

        var listShown0
            = curPage0 == pageNum || curPage0 == historyPageNum
        val listShown1
            = isDouble && (curPage1 == pageNum || curPage1 == historyPageNum)

        var isHistory = curPage0 == historyPageNum || curPage1 == historyPageNum

        if (!listShown0 && !listShown1) {
            setPage(0, pageNum)
            listShown0 = true
            isHistory = pageNum == historyPageNum
        }

        _uiState.value = uiState.value?.copy(
            snapToClueEvent = SnapToClueEvent(
                if (listShown0) 0 else 1,
                if (isHistory) historyIndex else cid.index,
            ),
        )

        return pageNum
    }

    fun clearSnapToClueEvent() {
        _uiState.value = uiState.value?.copy(
            snapToClueEvent = null,
        )
    }

    private fun getUpdatedUIState(refreshClueList : Boolean) : ClueTabsState {
        val state = uiState.value ?: ClueTabsState()

        val pages = if (refreshClueList || state.pages.isEmpty()) {
            getStatePages()
        } else {
            state.pages
        }

        val highlight = settings.livePlayClueHighlight.value
            ?: ClueHighlight.CH_RADIO_BUTTON
        val highlightSelectedRadio
            = highlight == ClueHighlight.CH_RADIO_BUTTON
                || highlight == ClueHighlight.CH_BOTH
        val highlightSelectedBackground
            = highlight == ClueHighlight.CH_BACKGROUND
                || highlight == ClueHighlight.CH_BOTH

        return state.copy(
            doubleMode = settings.liveClueListClueTabsDouble.value
                ?: state.doubleMode,
            pages = pages,
            highlightSelectedRadio = highlightSelectedRadio,
            highlightSelectedBackground = highlightSelectedBackground,
        )
    }

    private fun getStatePages() : List<ClueList> {
        val listPages = (board?.puzzle?.clueListNames ?: listOf())
            .sorted()
            .map { listName ->
                ClueList(
                    listName,
                    board?.puzzle?.getClues(listName)?.map {
                        getClueItem(
                            clue = it,
                            showDirection = false,
                        )
                    } ?: listOf(),
                )
            }

        val historyPage = ClueList(
            application.getString(R.string.clue_tab_history),
            (board?.puzzle?.history ?: listOf())
                .map {
                    getClueItem(
                        cid = it,
                        showDirection = true,
                    )
                },
        )

        return listPages + historyPage
    }

    private fun getClueItem(cid : ClueID, showDirection : Boolean) : ClueItem {
        return board?.puzzle?.getClue(cid)?.let {
            getClueItem(it, showDirection)
        } ?: ClueItem(
            cid = cid,
            text = "",
            isFlagged = false,
            flagColor = null,
            filled = false,
            selected = false,
            word = null,
        )
    }

    private fun getClueItem(clue : Clue, showDirection : Boolean) : ClueItem {
        val flagColor = if (
            clue.isFlagged && !clue.isDefaultFlagColor()
        ) {
            ColorUtils.addAlpha(clue.flagColor)
        } else {
            null
        }
        return ClueItem(
            cid = clue.clueID,
            text = getClueText(
                clue = clue,
                showDirection = showDirection,
            ),
            isFlagged = clue.isFlagged,
            flagColor = flagColor,
            filled = board?.isFilledClueID(clue.clueID) ?: false,
            selected = clue.clueID == board?.puzzle?.currentClueID,
            word = board?.getClueWord(clue.clueID),
        )
    }

    private fun getClueText(
        clue : Clue,
        showDirection : Boolean,
    ) : String {
        val showCount = settings.livePlayShowCount.value ?: false

        val listName = getShortListName(clue)
        val displayNum = clue.displayNumber
        val hint = clue.hint
        val hasCount = clue.hasZone()
        val count = if (hasCount) clue.getZone().size() else -1

        if (showDirection) {
            if (hasCount && showCount) {
                if (displayNum != null) {
                    return application.getString(
                        R.string.clue_format_short_with_count,
                        displayNum, listName, hint, count
                    )
                } else {
                    return application.getString(
                        R.string.clue_format_short_no_num_with_count,
                        listName, hint, count
                    )
                }
            } else {
                if (displayNum != null) {
                    return application.getString(
                        R.string.clue_format_short,
                        displayNum, listName, hint
                    )
                } else {
                    return application.getString(
                        R.string.clue_format_short_no_num,
                        listName, hint
                    )
                }
            }
        } else {
            if (hasCount && showCount) {
                if (displayNum != null) {
                    return application.getString(
                        R.string.clue_format_short_no_dir_with_count,
                        displayNum, hint, count
                    )
                } else {
                    return application.getString(
                        R.string.clue_format_short_no_num_no_dir_with_count,
                        hint, count
                    )
                }
            } else {
                if (displayNum != null) {
                    return application.getString(
                        R.string.clue_format_short_no_dir,
                        displayNum, hint
                    )
                } else {
                    return application.getString(
                        R.string.clue_format_short_no_num_no_dir,
                        hint
                    )
                }
            }
        }
    }

    private fun getShortListName(clue : Clue) : String {
        val listName = clue.clueID.listName
        if (listName == null || listName.isEmpty())
            return "";
        else
            return listName.substring(0,1).lowercase()
    }

    /**
     * Ensure page is shown
     *
     * If the pageNum-th is not shown, then update the panel showing
     * replacePageNum with pageNum (or the first panel if neither).
     * replacePageNum can be -1 if no replace.
     *
     * Call after isDouble is at least first set, since this information is
     * only known after first layout
     */
    private fun ensurePageShown(pageNum : Int, replacePageNum : Int) {
        val state = uiState.value ?: ClueTabsState()
        if (pageNum == state.panePages[0])
            return;

        if (isDouble) {
            if (pageNum == state.panePages[1])
                return

            if (replacePageNum == state.panePages[1]) {
                setPage(1, pageNum);
                return
            }
        }

        // change if replacePageNum matches or not
        setPage(0, pageNum)
    }

    /**
     * Snap to clue if settings ask for it
     */
    private fun snapToClueIfEnabled() {
        settings.getClueListSnapToClue { snapToClue ->
            if (snapToClue)
                snapToClue()
        }
    }
}
