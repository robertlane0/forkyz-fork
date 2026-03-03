
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_tab_history
import app.crossword.yourealwaysbe.forkyz.settings.ClueHighlight
import app.crossword.yourealwaysbe.forkyz.settings.ClueTabsDouble
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.RenderSettings
import app.crossword.yourealwaysbe.forkyz.settings.SnapClueMode
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.addAlpha
import app.crossword.yourealwaysbe.puz.Clue
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardChanges
import app.crossword.yourealwaysbe.puz.Playboard.PlayboardListener
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position

/**
 * Tags to attach to Playboard actions to control ClueTab behaviour
 */
enum class ClueTabsPlayboardTag {
    /**
     * A change that should not result in snapping to a clue
     */
    NO_SNAP_TO_CLUE,
}

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
    val crossingSelected : Boolean,
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
    val highlightCrossingClue : Boolean = false,
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
    val highlightCrossingClue : Boolean,
    val renderSettings : RenderSettings,
    val showCount : Boolean,
)

private data class ClueTabsSettings(
    val displaySettings : ClueTabsDisplaySettings,
    val scratchMode : Boolean,
)

class ClueTabsViewModel(
    private val utils : NativeBackendUtils,
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
                    settings.liveClueListHighlightCrossingClue,
                    settings.livePlayRenderSettings,
                ) { double, words, highlight, crossingHL, render ->
                    ClueTabsDisplaySettings(
                        double, words, highlight, crossingHL, render,
                        false,
                    )
                },
                settings.livePlayShowCount,
                settings.livePlayScratchMode,
            ) { display, showCount, scratchMode ->
                ClueTabsSettings(
                    display.copy(showCount = showCount),
                    scratchMode,
                )
            },
        )
    val uiState : StateFlow<ClueTabsState> = mediatedUIState.stateFlow

    // remembered from latest state update
    private var isShowCount : Boolean = false
    private val wordEditViewModels : MutableMap<ClueID, WordEditViewModel?>
        = mutableMapOf()

    /**
     * Will disable playboard change updates while hidden
     */
    var isVisible : Boolean = true
        set (value) {
            field = value
            if (value) {
                viewModelScope.launch {
                    currentUIState = currentUIState.copy(
                        pages = getStatePages(),
                    )
                }
            }
        }

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

    fun onCleared() {
        board?.removeListener(this)
        wordEditViewModels.forEach { _, vm -> vm?.onCleared() }
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

    fun onBoxClick(row : Int, col : Int, clueID : ClueID) {
        board?.setHighlightLetter(Position(row, col), clueID)
    }

    override fun onPlayboardChange(changes : PlayboardChanges) {
        viewModelScope.launch {
            currentUIState = currentUIState.copy(
                pages = getStatePages(
                    playboardChanges = changes,
                    currentPages = currentUIState.pages,
                )
            )
            val positionChanged
                = changes.previousPosition != board?.highlightLetter
            val isSnap = (
                (changes.previousWord != board?.currentWord || positionChanged)
                && ClueTabsPlayboardTag.NO_SNAP_TO_CLUE !in changes.tags
            )
            if (isSnap)
                snapToClueIfEnabled()
        }
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
        return snapToClueId(
            board?.clueID ?: return -1,
            showPageNum,
            replacePageNum,
        )
    }

    /**
     * Snap to a specific clue by ID
     *
     * @return page number snapped to or -1 if no change
     */
    private fun snapToClueId(
        cid : ClueID,
        showPageNum : Int = -1,
        replacePageNum : Int = -1,
    ) : Int {
        val listName = cid.listName
        if (listName == null)
            return -1

        val historyPageNum = currentUIState.historyPageNum
        val historyIndex = currentUIState.getHistoryIndex(cid)

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

        var isHistory = historyIndex >= 0
            && (curPage0 == historyPageNum || curPage1 == historyPageNum)

        if (!listShown0 && !listShown1) {
            setPage(0, pageNum)
            listShown0 = true
            isHistory = historyIndex >= 0 && pageNum == historyPageNum
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
        return ClueTabsState(pages = runBlocking { getStatePages() })
    }

    private suspend fun getUpdatedUIState(
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
        val needsUpdate = (isShowCount != showCount)
        isShowCount = showCount
        val pages = if (needsUpdate)
            getStatePages()
        else
            state.pages

        return state.copy(
            pages = pages,
            doubleMode = clueTabsSettings.displaySettings.clueTabsDouble,
            highlightSelectedRadio = highlightSelectedRadio,
            highlightSelectedBackground = highlightSelectedBackground,
            highlightCrossingClue
                = clueTabsSettings.displaySettings.highlightCrossingClue,
            renderSettings = clueTabsSettings.displaySettings.renderSettings,
            scratchMode = clueTabsSettings.scratchMode,
        )
    }

    private suspend fun getStatePages(
        playboardChanges : PlayboardChanges? = null,
        currentPages : List<ClueList>? = null,
    ) : List<ClueList> {
        if (playboardChanges != null && currentPages != null) {
            return getStatePagesOptimized(playboardChanges, currentPages)
        } else {
            val listPages = (board?.puzzle?.clueListNames?.toList() ?: listOf())
                .sorted()
                .map { listName ->
                    ClueList(
                        listName,
                        board?.puzzle?.getClues(listName)?.toList()?.map {
                            getClueItem(
                                clue = it,
                                showDirection = false,
                            )
                        } ?: listOf(),
                    )
                }

            val historyPage = ClueList(
                utils.getString(Res.string.clue_tab_history),
                (board?.puzzle?.history?.toList() ?: listOf())
                    .map {
                        getClueItem(
                            cid = it,
                            showDirection = true,
                        )
                    },
            )

            return listPages + historyPage
        }
    }

    private suspend fun getStatePagesOptimized(
        changes : PlayboardChanges,
        currentPages : List<ClueList>,
    ) : List<ClueList> {
        val historyChange = changes.isHistoryChange()
        val historyPageNum = currentPages.size - 1
        // need to calculate this as some clues might be filled due to
        // box changes
        val changedClueIDs = (
            (changes.changedClueIDs?.toSet() ?: setOf())
            + (changes.cellChanges?.flatMap {
                board?.puzzle?.checkedGetBox(it)?.getIsPartOfClues() ?: setOf()
            }?.toSet() ?: setOf())
        )

        return currentPages.mapIndexed { pageIndex, page ->
            val isHistoryPage = pageIndex == historyPageNum

            val updatedClues = if (isHistoryPage && historyChange) {
                // Reorder history list according to lastHistoryIndex
                val currentHistory
                    = board?.puzzle?.history?.toList() ?: listOf()
                currentHistory.map { cid ->
                    // Try to find existing clue item
                    val existingItem = page.clues.find { it.cid == cid }

                    if (existingItem != null) {
                        // Update selected field if needed
                        updateClueItemIfNeeded(
                            cid = cid,
                            existingItem = existingItem,
                            changes = changes,
                            changedClueIDs = changedClueIDs,
                            showDirection = true,
                        )
                    } else {
                        // New item in history, calculate it
                        getClueItem(cid = cid, showDirection = true)
                    }
                }
            } else {
                // For non-history pages or history without reordering,
                // just update existing items
                page.clues.map { clueItem ->
                    val cid = clueItem.cid
                    updateClueItemIfNeeded(
                        cid = cid,
                        existingItem = clueItem,
                        changes = changes,
                        changedClueIDs = changedClueIDs,
                        showDirection = isHistoryPage,
                    )
                }
            }

            page.copy(clues = updatedClues)
        }
    }

    /**
     * Get the crossing clue ID for the current board position
     *
     * Returns the first clue at the current cell that is not the
     * currently selected clue, or null if there is no crossing clue.
     */
    private fun getCrossingClueID() : ClueID? {
        val board = board ?: return null
        val currentCid = board.clueID ?: return null
        val box = board.currentBox ?: return null
        return box.getIsPartOfClues()
            .firstOrNull { it != currentCid }
    }

    private suspend fun updateClueItemIfNeeded(
        cid: ClueID,
        existingItem : ClueItem,
        changes : PlayboardChanges,
        changedClueIDs : Set<ClueID>,
        showDirection : Boolean,
    ): ClueItem {
        val currentWordCid = changes.currentWord?.clueID
        val previousWordCid = changes.previousWord?.clueID
        val crossingCid = getCrossingClueID()
        val needsSelectedUpdate
            = cid == currentWordCid || cid == previousWordCid
        val needsCrossingUpdate
            = cid == crossingCid || existingItem.crossingSelected
        val needsRecalculation = changedClueIDs.contains(cid)

        return if (needsRecalculation) {
            getClueItem(cid = cid, showDirection = showDirection)
        } else if (needsSelectedUpdate || needsCrossingUpdate) {
            existingItem.copy(
                selected = cid == currentWordCid,
                crossingSelected = cid == crossingCid,
            )
        } else {
            existingItem
        }
    }

    private suspend fun getClueItem(
        cid : ClueID,
        showDirection : Boolean,
    ) : ClueItem {
        return board?.puzzle?.getClue(cid)?.let {
            getClueItem(it, showDirection)
        } ?: ClueItem(
            cid = cid,
            text = "",
            isFlagged = false,
            flagColor = null,
            filled = false,
            selected = false,
            crossingSelected = false,
            word = null,
        )
    }

    private suspend fun getClueItem(
        clue : Clue,
        showDirection : Boolean,
    ) : ClueItem {
        val flagColor = if (
            clue.isFlagged && !clue.isDefaultFlagColor()
        ) {
            addAlpha(clue.flagColor)
        } else {
            null
        }
        return ClueItem(
            cid = clue.clueID,
            text = PlayboardTextRenderer.getShortClueText(
                utils,
                clue,
                true,
                showDirection,
                isShowCount,
            ),
            isFlagged = clue.isFlagged,
            flagColor = flagColor,
            filled = board?.isFilledClueID(clue.clueID) ?: false,
            selected = clue.clueID == board?.puzzle?.currentClueID,
            crossingSelected = clue.clueID == getCrossingClueID(),
            word = board?.getClueWord(clue.clueID),
        )
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
        settings.getClueListSnapClueMode { mode ->
            when (mode) {
                SnapClueMode.SCM_OFF -> { }
                SnapClueMode.SCM_CLUE -> snapToClue()
                SnapClueMode.SCM_CROSSING -> {
                    val crossingCid = getCrossingClueID()
                    if (crossingCid != null) {
                        snapToClueId(crossingCid)
                    } else {
                        snapToClue()
                    }
                }
            }
        }
    }

    private var currentUIState : ClueTabsState
        get() { return mediatedUIState.current }
        set(value) { mediatedUIState.current = value }
}
