
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

import android.app.Application
import androidx.core.content.ContextCompat

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ClueHighlight
import app.crossword.yourealwaysbe.forkyz.settings.ClueTabsDouble
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.addAlpha
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardListener
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position

/**
 * Indicates that panel should skip to clue on page
 */
data class SnapToClueEvent(
    val paneIndex : Int,
    val pageNum : Int,
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
 * @param renderSettings what to use to draw boards
 * @param scratchMode whether is scratch mode
 */
data class ClueTabsState(
    val doubleMode : ClueTabsDouble = ClueTabsDouble.CTD_NEVER,
    val pages : List<ClueList> = listOf(),
    val panePages : List<Int> = listOf(0, 1),
    val highlightSelectedRadio : Boolean = true,
    val highlightSelectedBackground : Boolean = false,
    val snapToClueEvent : SnapToClueEvent? = null,
    val renderSettings : RenderSettings = RenderSettings(),
    val scratchMode : Boolean = false,
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

private data class ClueTabsDisplaySettings(
    val clueTabsDouble : ClueTabsDouble,
    val showWords : Boolean,
    val clueHighlight : ClueHighlight,
    val renderSettings : RenderSettings,
    val showCount : Boolean,
)

private data class ClueTabsSettings(
    val displaySettings : ClueTabsDisplaySettings,
    val scratchMode : Boolean,
)

class ClueTabsViewModel(
    private val application : Application,
    private val viewModelScope : CoroutineScope,
    private val settings : ForkyzSettings,
    val board : Playboard?,
    val playLetter : (String) -> Unit,
    val deleteLetter : () -> Unit,
) : PlayboardListener {
    private val mediatedUIState
        = MediatedStateWithFlow<ClueTabsState, ClueTabsSettings>(
            viewModelScope,
            getInitialUIState(),
            this::getUpdatedUIState,
            combine(
                combine(
                    settings.liveClueListClueTabsDouble,
                    settings.liveClueListShowWords,
                    settings.livePlayClueHighlight,
                    settings.livePlayRenderSettings,
                    settings.livePlayShowCount,
                    ::ClueTabsDisplaySettings,
                ),
                settings.livePlayScratchMode,
                ::ClueTabsSettings,
            ),
        )
    val uiState : StateFlow<ClueTabsState> = mediatedUIState.stateFlow

    // remembered from latest state update
    private var isShowCount : Boolean = false
    private val wordEditViewModels : MutableMap<ClueID, WordEditViewModel?>
        = mutableMapOf()

    val numPages : Int
        get() { return currentUIState.numPages }

    val pages : List<ClueList>
        get() { return currentUIState.pages }

    val historyPageNum : Int
        get() { return currentUIState.historyPageNum }

    /**
     * View should set this to track if it is showing both panes
     *
     * Is used during snap to clue behaviour to decide which pane to
     * update.
     */
    var isDouble : Boolean = false
        set(value) {
            field = value
            snapToClueIfEnabled()
        }

    init {
        board?.addListener(this)
    }

    fun getWordEditViewModel(clueID : ClueID) : WordEditViewModel? {
        return wordEditViewModels.getOrPut(clueID) {
            board?.getClueWord(clueID)?.let { word ->
                WordEditViewModel(
                    viewModelScope,
                    settings,
                    board,
                    playLetter,
                    deleteLetter,
                    word,
                )
            }
        }
    }

    fun onBoxClick(row : Int, col : Int) {
        board?.setHighlightLetter(Position(row, col))
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        currentUIState = currentUIState.copy(pages = getStatePages())
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
        return currentUIState.isHistoryPage(pageNum)
    }

    /**
     * Set page for pane
     *
     * @param index the index of the pane (0, 1)
     * @param page the page number (0 to num pages)
     */
    fun setPage(index : Int, page : Int) {
        val state = currentUIState
        if (0 <= page && page < state.numPages) {
            currentUIState = state.copy(
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

        val historyPageNum = currentUIState.historyPageNum
        val historyIndex = currentUIState.getHistoryIndex(cid) ?: -1

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

        val curPage0 = currentUIState.panePages.get(0)
        val curPage1 = currentUIState.panePages.get(1)

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

        currentUIState = currentUIState.copy(
            snapToClueEvent = SnapToClueEvent(
                paneIndex = if (listShown0) 0 else 1,
                pageNum = pageNum,
                clueIndex = if (isHistory) historyIndex else cid.index,
            ),
        )

        return pageNum
    }

    fun clearSnapToClueEvent() {
        currentUIState = currentUIState.copy(
            snapToClueEvent = null,
        )
    }

    private fun getInitialUIState() : ClueTabsState {
        return ClueTabsState(pages = getStatePages())
    }

    private fun getUpdatedUIState(
        state : ClueTabsState,
        clueTabsSettings : ClueTabsSettings,
    ) : ClueTabsState {
        val highlight = clueTabsSettings.displaySettings.clueHighlight
        val highlightSelectedRadio
            = highlight == ClueHighlight.CH_RADIO_BUTTON
                || highlight == ClueHighlight.CH_BOTH
        val highlightSelectedBackground
            = highlight == ClueHighlight.CH_BACKGROUND
                || highlight == ClueHighlight.CH_BOTH

        val showCount = clueTabsSettings.displaySettings.showCount
        val pages = if (isShowCount != showCount)
            getStatePages()
        else
            state.pages
        isShowCount = showCount

        return state.copy(
            pages = pages,
            doubleMode = clueTabsSettings.displaySettings.clueTabsDouble,
            highlightSelectedRadio = highlightSelectedRadio,
            highlightSelectedBackground = highlightSelectedBackground,
            renderSettings = clueTabsSettings.displaySettings.renderSettings,
            scratchMode = clueTabsSettings.scratchMode,
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
            addAlpha(clue.flagColor)
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
        val listName = getShortListName(clue)
        val displayNum = clue.displayNumber
        val hint = clue.hint
        val hasCount = clue.hasZone()
        val count = if (hasCount) clue.getZone().size() else -1

        if (showDirection) {
            if (hasCount && isShowCount) {
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
            if (hasCount && isShowCount) {
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
        val state = currentUIState
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

    private var currentUIState : ClueTabsState
        get() { return mediatedUIState.current }
        set(value) { mediatedUIState.current = value }
}
