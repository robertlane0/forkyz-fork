
package app.crossword.yourealwaysbe.forkyz.view

import kotlin.math.min
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import org.jetbrains.compose.resources.stringResource

import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.clue_word
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Playboard.Word
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.settings.ClueTabsDouble
import app.crossword.yourealwaysbe.forkyz.theme.BoardColorScheme
import app.crossword.yourealwaysbe.forkyz.util.MATERIAL_MIN_CLICKABLE_DP
import app.crossword.yourealwaysbe.forkyz.util.onRightClick

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
    onClueClickDescription : String = "",
    onClueLongClick : (ClueID) -> Unit = { },
    onClueLongClickDescription : String = "",
    onClueBoardClick : (ClueID) -> Unit = { },
    onClueBoardClickDescription : String = "",
    onBarLongClick : () -> Unit = { },
    onBarLongClickDescription : String = "",
    maxWordScale : Float? = null,
    onPageChange : (Int, Int) -> Unit = { _, _ -> },
) {
    @Composable
    fun Modifier.clueItemBackground(clue : ClueItem) : Modifier {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val highlightSelectedBackground by remember {
            derivedStateOf { state.highlightSelectedBackground }
        }
        val highlightCrossingClue by remember {
            derivedStateOf { state.highlightCrossingClue }
        }
        if (highlightSelectedBackground && clue.selected) {
            return this.background(MaterialTheme.colorScheme.secondaryContainer)
        } else if (highlightCrossingClue && clue.crossingSelected) {
            return this.background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5F),
            )
        } else {
            return this
        }
    }

    @Composable
    fun clueItemForeground(clue : ClueItem) : Color {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val highlightSelectedBackground by remember {
            derivedStateOf { state.highlightSelectedBackground }
        }
        val highlightCrossingClue by remember {
            derivedStateOf { state.highlightCrossingClue }
        }
        val isCrossingHighlighted
            = highlightCrossingClue && clue.crossingSelected
        if (
            (highlightSelectedBackground && clue.selected)
            || isCrossingHighlighted
        ) {
            return MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            return MaterialTheme.colorScheme.onSurface
        }
    }

    @Composable
    fun ClueItem(clue : ClueItem, maxWidth : Dp) {
        // padding is a bit all over the place depending on what bits
        // are visible..
        Column(
            modifier = Modifier.fillMaxWidth()
                .heightIn(min = MATERIAL_MIN_CLICKABLE_DP)
                .onRightClick { onClueLongClick(clue.cid) }
                .combinedClickable(
                    onClick = { onClueClick(clue.cid) },
                    onClickLabel = onClueClickDescription,
                    onLongClick = { onClueLongClick(clue.cid) },
                    onLongClickLabel = onClueLongClickDescription,
                ).clueItemBackground(clue),
            verticalArrangement = Arrangement.Center,
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
                            text = remember(clue.text) {
                                htmlToAnnotatedString(clue.text)
                            },
                            color = clueItemForeground(clue)
                        )

                        val state by viewModel.uiState
                            .collectAsStateWithLifecycle()
                        val highlightSelectedRadio by remember {
                            derivedStateOf { state.highlightSelectedRadio }
                        }
                        if (highlightSelectedRadio) {
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
                                    viewModel.onBoxClick(row, col, clue.cid)
                                    onClueBoardClick(clue.cid)
                                },
                                onLongPress = { _, _->
                                    onClueLongClick(clue.cid)
                                },
                                onLongPressDescription
                                    = onClueLongClickDescription,
                                contentDescription
                                    = stringResource(Res.string.clue_word),
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
        val snapToClueEvent by remember {
            derivedStateOf { state.snapToClueEvent }
        }
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(snapToClueEvent) {
            snapToClueEvent?.let { event ->
                if (event.paneIndex == paneIndex && event.pageNum == page) {
                    coroutineScope.launch {
                        listState.scrollToItem(event.clueIndex)
                    }
                    viewModel.clearSnapToClueEvent()
                }
            }
        }

        val clues by remember { derivedStateOf { state.pages.get(page).clues } }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            state = listState,
        ) {
            items(clues) {
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
        val numPages by remember { derivedStateOf { state.numPages } }

        if (numPages > 0) {
            val coroutineScope = rememberCoroutineScope()
            val uiStatePage by remember(paneIndex) {
                derivedStateOf {
                    min(state.panePages.get(paneIndex), numPages - 1)
                }
            }
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
                val pages by remember {
                    derivedStateOf { state.pages }
                }
                if (pages.size > 0) {
                    SecondaryTabRow(
                        modifier = Modifier
                            .onRightClick { onBarLongClick() }
                            .combinedClickable(
                                onClick = { /* handled in Tab */ },
                                onLongClick = onBarLongClick,
                                onLongClickLabel = onBarLongClickDescription,
                            ),
                        selectedTabIndex = min(
                            pagerState.currentPage,
                            pages.size - 1,
                        ),
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
        val isDouble by remember(maxWidth, isPortrait) {
            derivedStateOf { isDouble(state.doubleMode, maxWidth, isPortrait) }
        }
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

