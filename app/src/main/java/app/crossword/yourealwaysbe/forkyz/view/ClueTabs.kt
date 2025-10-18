
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ClueTabsDouble
import app.crossword.yourealwaysbe.forkyz.theme.BoardColorScheme
import app.crossword.yourealwaysbe.forkyz.view.BoardWordEdit
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position

// from Android docs
private val DP_PER_INCH = 160
private val DOUBLE_UP_WIDTH_DP = (3 * DP_PER_INCH).dp
private val FLAG_WIDTH = 2.sp
private val CLUE_VERTICAL_PADDING = 4.sp
private val CLUE_HORIZONTAL_PADDING = 16.dp
private val CLUE_MIN_HEIGHT = 14.sp
private val FILLED_CLUE_ALPHA = 0.6F
private val UNFILLED_CLUE_ALPHA = 1.0F

/**
 * Clue tabs view
 *
 * @param modifier modifier
 * @param textMeasurer shared text measurer for rendering board words
 * @param viewModel a ClueTabsViewModel to get data from backend
 * @param isPortrait true if the current display is portrait
 * @param boardColorScheme colors for board rendering
 * @param showAllWords show miniboard beneath each clue
 * @param maxWordScale maximum scale of miniboard (else fits space)
 * @param onClueClick callback when clue clicked
 * @param onClueClickDescription accessibility action text for click
 * action
 * @param onClueLongClick callback when a clue is long clicked, takes
 * clue argument
 * @param onClueLongClickDescription accessibility action text for long
 * click
 * @param onClueBoardClick callback when the miniboard for a clue is
 * clicked, takes clue clicked
 * * @param onClueBoardClickDescription accessibility action text for long
 * click on miniboard
 * @param onBarLongClick when tab bar long clicked
 * @param onBarLongClickDescription accessibility action text for long
 * click on tabs bar
 * @param onPageChange callback when pane x changes to page y
 */
@Composable
fun ClueTabs(
    modifier : Modifier = Modifier,
    textMeasurer : TextMeasurer,
    viewModel : ClueTabsViewModel,
    isPortrait : Boolean,
    boardColorScheme : BoardColorScheme,
    showAllWords : Boolean = false,
    onClueClick : (ClueID) -> Unit = { },
    onClueClickDescription : String? = null, /* TODO */
    onClueLongClick : (ClueID) -> Unit = { },
    onClueLongClickDescription : String? = null,
    onClueBoardClick : (ClueID) -> Unit = { },
    onClueBoardClickDescription : String? = null,
    onBarLongClick : () -> Unit = { },
    onBarLongClickDescription : String? = null,
    maxWordScale : Float? = null,
    onPageChange : (Int, Int) -> Unit = { _, _ -> },
) {
    @Composable
    fun Modifier.clueItemBackground(clueSelected : Boolean) : Modifier {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        if (state.highlightSelectedBackground && clueSelected)
            return this.background(MaterialTheme.colorScheme.secondaryContainer)
        else
            return this
    }

    @Composable
    fun clueItemForeground(clueSelected : Boolean) : Color {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        if (state.highlightSelectedBackground && clueSelected)
            return MaterialTheme.colorScheme.onSecondaryContainer
        else
            return MaterialTheme.colorScheme.onSurface
    }

    @Composable
    fun ClueItem(clue : ClueItem, maxWidth : Dp) {
        // padding is a bit all over the place depending on what bits
        // are visible..
        Column(
            modifier = Modifier.fillMaxWidth()
                .combinedClickable(
                    onClick = { onClueClick(clue.cid) },
                    onClickLabel = onClueClickDescription,
                    onLongClick = { onClueLongClick(clue.cid) },
                    onLongClickLabel = onClueLongClickDescription,
                ).clueItemBackground(clue.selected)
        ) {
            with (LocalDensity.current) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .heightIn(min = CLUE_MIN_HEIGHT.toDp()),
                ) {
                    val flagColor = if (clue.isFlagged) {
                        clue.flagColor?.let { Color(it) }
                            ?: boardColorScheme.flagColor
                    } else {
                        Color.Transparent
                    }
                    Box(
                        modifier = Modifier.width(FLAG_WIDTH.toDp())
                            .fillMaxHeight()
                            .background(flagColor)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(
                                top = CLUE_VERTICAL_PADDING.toDp(),
                                bottom = (
                                    if (showAllWords) 0.toDp()
                                    else CLUE_VERTICAL_PADDING.toDp()
                                ),
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = modifier.weight(1.0F)
                                .padding(start = CLUE_HORIZONTAL_PADDING)
                                .alpha(
                                    if (clue.filled)
                                        FILLED_CLUE_ALPHA
                                    else
                                        UNFILLED_CLUE_ALPHA
                                ),
                            text = AnnotatedString.fromHtml(clue.text),
                            color = clueItemForeground(clue.selected),
                        )

                        val state by viewModel.uiState
                            .collectAsStateWithLifecycle()
                        if (state.highlightSelectedRadio) {
                            RadioButton(
                                selected = clue.selected,
                                onClick = { onClueClick(clue.cid) },
                            )
                        }
                    }
                }

                if (showAllWords) {
                    val wordEditWidth = maxWidth
                    viewModel.getWordEditViewModel(clue.cid)?.let {
                        wordEditViewModel->
                            WordEditNoInput(
                                modifier = Modifier.padding(
                                    vertical = CLUE_VERTICAL_PADDING.toDp(),
                                    horizontal = CLUE_HORIZONTAL_PADDING,
                                ),
                                textMeasurer = textMeasurer,
                                maxWidth
                                    = maxWidth - CLUE_HORIZONTAL_PADDING * 2,
                                colors = boardColorScheme,
                                viewModel = wordEditViewModel,
                                onTap = { row, col ->
                                    viewModel.onBoxClick(row, col)
                                    onClueBoardClick(clue.cid)
                                },
                                onLongPress = { _, _->
                                    onClueLongClick(clue.cid)
                                },
                            )
                    }
                }
            }
        }
        HorizontalDivider()
    }

    @Composable
    fun Page(paneIndex : Int, page : Int, maxWidth : Dp) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(state.snapToClueEvent) {
            state.snapToClueEvent?.let { event ->
                if (event.paneIndex == paneIndex && event.pageNum == page) {
                    coroutineScope.launch {
                        listState.scrollToItem(event.clueIndex)
                    }
                    viewModel.clearSnapToClueEvent()
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            state = listState,
        ) {
            items(state.pages.get(page).clues) {
                key(it.cid.toString()) {
                    ClueItem(it, maxWidth)
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun Pane(
        modifier : Modifier = Modifier,
        paneIndex : Int,
        maxWidth : Dp,
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val numPages = state.numPages
        val pages = state.pages

        if (numPages > 0) {
            val coroutineScope = rememberCoroutineScope()
            val uiStatePage = Math.min(
                state.panePages.get(paneIndex),
                numPages - 1,
            )
            val pagerState = rememberPagerState(
                pageCount = { numPages },
                initialPage = uiStatePage,
            )

            LaunchedEffect(pagerState) {
                // don't update viewModel for initial page (not set by
                // view model)
                var hadFirst = false
                snapshotFlow {
                    pagerState.currentPage
                }.distinctUntilChanged().collect { page ->
                    if (!hadFirst) {
                        hadFirst = true
                    } else if (uiStatePage != page) {
                        viewModel.setPage(paneIndex, page)
                        // report driven by pager
                        onPageChange(paneIndex, page)
                    }
                }
            }

            LaunchedEffect(uiStatePage) {
                if (uiStatePage != pagerState.currentPage) {
                    coroutineScope.launch {
                        pagerState.scrollToPage(uiStatePage)
                    }
                    // report driven by ui state
                    onPageChange(paneIndex, uiStatePage)
                }
            }

            Column(modifier = modifier) {
                SecondaryTabRow(
                    modifier = Modifier.combinedClickable(
                        onClick = { /* handled in Tab */ },
                        onLongClick = onBarLongClick,
                        onLongClickLabel = onBarLongClickDescription,
                    ),
                    selectedTabIndex = pagerState.currentPage,
                ) {
                    pages.forEachIndexed { idx, page ->
                        Tab(
                            selected = idx == pagerState.currentPage,
                            onClick = { viewModel.setPage(paneIndex, idx) },
                            text = { Text(page.listName) },
                        )
                    }
                }
                HorizontalPager(state = pagerState) { page ->
                    Page(paneIndex, page, maxWidth)
                }
            }
        }
    }

    fun isDouble(
        doubleMode : ClueTabsDouble,
        maxWidth : Dp,
        isPortrait : Boolean,
    ) : Boolean {
        return when(doubleMode) {
            ClueTabsDouble.CTD_ALWAYS -> true
            ClueTabsDouble.CTD_LANDSCAPE -> !isPortrait
            ClueTabsDouble.CTD_WIDE -> maxWidth >= DOUBLE_UP_WIDTH_DP
            else -> false
        }
    }

    BoxWithConstraints(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
    ) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val isDouble = isDouble(state.doubleMode, maxWidth, isPortrait)
        LaunchedEffect(isDouble) {
            viewModel.isDouble = isDouble
        }

        val paneWidth = maxWidth / (if (isDouble) 2 else 1)

        Row() {
            Pane(
                modifier = Modifier.weight(1.0F),
                paneIndex = 0,
                maxWidth = paneWidth
            )
            if (isDouble) {
                Pane(
                    modifier = Modifier.weight(1.0F),
                    paneIndex = 1,
                    maxWidth = paneWidth,
                )
            }
        }
    }
}

