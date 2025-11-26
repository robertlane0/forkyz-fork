
package app.crossword.yourealwaysbe.forkyz

import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.squareup.seismic.ShakeDetector

import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.settings.FitToScreenMode
import app.crossword.yourealwaysbe.forkyz.settings.GridRatio
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.letterOrDigitKeyToChar
import app.crossword.yourealwaysbe.forkyz.view.BoardEdit
import app.crossword.yourealwaysbe.forkyz.view.ClueTabs
import app.crossword.yourealwaysbe.forkyz.view.OKDialog
import app.crossword.yourealwaysbe.forkyz.view.SpecialKey
import app.crossword.yourealwaysbe.forkyz.view.rememberBoardEditState
import app.crossword.yourealwaysbe.forkyz.view.rememberBoardTextMeasurer
import app.crossword.yourealwaysbe.puz.ClueID

private val ACROSTIC_CLUE_TABS_WORD_SCALE = 0.7F
// min height of board in portrait / acrostic
private val ACROSTIC_BOARD_HEIGHT_RATIO_MIN = 0.25F
// max width of board in landscape / acrostic
private val ACROSTIC_BOARD_WIDTH_RATIO_MAX = 0.7F

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayPage(
    viewModel : PlayPageViewModel,
    themeHelper : ThemeHelper,
    utils : NativeFrontendUtils,
    pageOpener : PageOpener,
) {
    val boardEditState = rememberBoardEditState()

    PuzzlePage(
        viewModel = viewModel,
        themeHelper = themeHelper,
        utils = utils,
        pageOpener = pageOpener,
    ) {
        fun toggleShowClueTabs() {
            val isShowing = viewModel.playUIState.value?.showClueTabs ?: false
            if (isShowing)
                viewModel.hideClueTabs()
            else
                viewModel.showClueTabs()
        }

        @Composable
        fun PlayLaunchers() {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()

            val isRandomClueOnShake by remember {
                derivedStateOf { state.isRandomClueOnShake }
            }
            LaunchedEffect(isRandomClueOnShake) {
                if (state.isRandomClueOnShake) {
                    utils.startShakeDetector {
                        viewModel.pickRandomUnfilledClue()
                    }
                } else {
                    utils.stopShakeDetector()
                }
            }

            LaunchedEffect(Unit) {
                viewModel.getInitialSizeInfo { fitToScreenMode, scale ->
                    boardEditState.setInitialFitToScreen(fitToScreenMode, scale)
                }
            }

            val fitToScreenMode by remember {
                derivedStateOf { state.fitToScreenMode }
            }
            LaunchedEffect(fitToScreenMode) {
                boardEditState.updateFitToScreenMode(fitToScreenMode)
            }

            val isFullScreen by remember {
                derivedStateOf { state.isFullScreen }
            }
            LaunchedEffect(isFullScreen) {
                if (isFullScreen)
                    utils.onSetFullScreen()
            }
        }

        @Composable
        fun MenuZoomSub() {
            MenuZoomSub(
                onZoomIn = boardEditState::zoomIn,
                onZoomInMax = boardEditState::zoomInMax,
                onZoomOut = boardEditState::zoomOut,
                onZoomFit = boardEditState::zoomFit,
                onZoomReset = boardEditState::zoomReset,
            )
        }

        @Composable
        fun MenuClues() {
            DropdownMenuItem(
                text = { MenuText(stringResource(R.string.clues)) },
                onClick = {
                    viewModel.launchClueList()
                    viewModel.dismissMenu()
                }
            )
        }

        @Composable
        fun OverflowMenu() {
            val state by viewModel.menuState.collectAsStateWithLifecycle()
            val expanded by remember {
                derivedStateOf { state.expanded == PuzzleSubMenu.MAIN }
            }
            val uiState by viewModel.playUIState.collectAsStateWithLifecycle()
            val fitLocked by remember {
                derivedStateOf {
                    uiState.fitToScreenMode == FitToScreenMode.FTSM_LOCKED
                }
            }

            IconButton(onClick = { viewModel.expandMenu(PuzzleSubMenu.MAIN) }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.overflow),
                )
            }
            DropdownMenu(
                modifier = BaseMenuModifier,
                expanded = expanded,
                onDismissRequest = viewModel::dismissMenu,
            ) {
                MenuClues()
                MenuNotes()
                if (!fitLocked)
                    MenuZoom()
                MenuScratchMode()
                MenuSpecialEntry()
                MenuShowErrors()
                MenuReveal()
                MenuExternalTools()
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
            if (!fitLocked)
                MenuZoomSub()
        }

        /**
         * Version of clue text with long-click actions for play &c.
         */
        @Composable
        fun ClueText(modifier : Modifier) {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            val clueText by remember {
                derivedStateOf { state.clueText }
            }
            ClueText(
                modifier = modifier,
                clueText = clueText,
                onClick = ::toggleShowClueTabs,
                onClickDescription = stringResource(R.string.toggle_clue_tabs),
                onLongClick = viewModel::launchClueList,
                onLongClickDescription = stringResource(R.string.open_clue_list),
            )
        }

        @Composable
        fun Modifier.clueBarHeight() : Modifier {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            val expandableClueLine by remember {
                derivedStateOf { state.expandableClueLine }
            }
            if (expandableClueLine) {
                return this.heightIn(
                    min = TopAppBarDefaults.TopAppBarExpandedHeight,
                )
            } else {
                return this.height(TopAppBarDefaults.TopAppBarExpandedHeight)
            }
        }

        @Composable
        fun TopAppBar() {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            val isClueBelowGrid by remember {
                derivedStateOf { state.isClueBelowGrid }
            }
            val puzzleTitle by remember {
                derivedStateOf { state.puzzleTitle }
            }

            themeHelper.ForkyzTopAppBar(
                modifier = Modifier.clueBarHeight(),
                title = {
                    if (isClueBelowGrid) {
                        val title = puzzleTitle
                            ?: stringResource(R.string.app_name)
                        Text(
                            text = AnnotatedString.fromHtml(title),
                            minLines = 1,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        ClueText(modifier = Modifier.fillMaxWidth())
                    }
                },
                onBack = { doExitPage() },
                actions = { OverflowMenu() },
            )
        }

        @Composable
        fun IntroDialog() {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            val firstPlayMessage by remember {
                derivedStateOf { state.firstPlayMessage }
            }

            firstPlayMessage?.let { firstPlayMessage ->
                OKDialog(
                    title = R.string.introduction,
                    summary = firstPlayMessage,
                    onClose = viewModel::clearFirstPlayMessage,
                )
            }
        }

        /**
         * Below grid clue bar
         *
         * Modifier mainly needed for width, height is set internally
         */
        @Composable
        fun ClueBar(modifier : Modifier) {
            Row(
                modifier = modifier
                    .clueBarHeight()
                    .background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = viewModel::prevClue) {
                    Icon(
                        painter = painterResource(R.drawable.ic_prev_clue),
                        contentDescription = stringResource(R.string.prev_clue),
                    )
                }
                ClueText(Modifier.weight(1.0F))
                IconButton(onClick = viewModel::nextClue) {
                    Icon(
                        painter = painterResource(R.drawable.ic_next_clue),
                        contentDescription = stringResource(R.string.next_clue),
                    )
                }
            }
        }

        @Composable
        fun CluesList(modifier : Modifier) {
            val textMeasurer = rememberBoardTextMeasurer()

            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            val showClueTabs by remember {
                derivedStateOf { state.showClueTabs }
            }

            if (!showClueTabs)
                return

            val isAcrostic by remember {
                derivedStateOf { state.isAcrostic }
            }
            val showAllWords = isAcrostic
            val onBarLongClick : () -> Unit
                = if (isAcrostic) ::toggleShowClueTabs
                else { { } }
            val onBarLongClickDescription
                = if (isAcrostic) stringResource(R.string.toggle_clue_tabs)
                else ""
            val boardColorScheme = themeHelper.getBoardColorScheme()

            ClueTabs(
                modifier = modifier,
                textMeasurer = textMeasurer,
                viewModel = viewModel.clueTabsViewModel,
                isPortrait = utils.isPortrait,
                showAllWords = showAllWords,
                onClueClick = viewModel::clickClue,
                onClueClickDescription = stringResource(R.string.select_clue),
                onClueLongClick = viewModel::launchClueNotes,
                onClueLongClickDescription
                    = stringResource(R.string.open_clue_notes),
                onClueBoardClick = viewModel::clickClue,
                onClueBoardClickDescription = stringResource(R.string.select_clue),
                onBarLongClick = onBarLongClick,
                onBarLongClickDescription = onBarLongClickDescription,
                maxWordScale = ACROSTIC_CLUE_TABS_WORD_SCALE,
                onPageChange = viewModel::saveClueTabsPage,
                boardColorScheme = boardColorScheme,
            )
        }

        @Composable
        fun Board(modifier : Modifier) {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            val doubleTapFitBoard by remember {
                derivedStateOf { state.doubleTapFitBoard }
            }
            val dontSnapBoardToBorders by remember {
                derivedStateOf { state.dontSnapBoardToBorders }
            }
            val boardColorScheme = themeHelper.getBoardColorScheme()

            BoardEdit(
                modifier = modifier,
                inputConnectionMediator = utils.inputConnectionMediator,
                state = boardEditState,
                viewModel = viewModel.boardEditViewModel,
                colors = boardColorScheme,
                doubleTapFitBoard = doubleTapFitBoard,
                dontSnapBoardToBorders = dontSnapBoardToBorders,
                onTap = { row, col ->
                    boardEditState.requestFocus()
                    viewModel.clickBoard(row, col)
                },
                onLongPress = viewModel::longClickBoard,
                onLongPressDescription
                    = stringResource(R.string.open_clue_notes),
            )

            LaunchedEffect(Unit) {
                boardEditState.requestFocus()
            }
        }

        fun min(x : Dp, y : Dp) : Dp {
            return if (x < y) x else y
        }

        fun max(x : Dp, y : Dp) : Dp {
            return if (x > y) x else y
        }

        fun fracDp(frac : Double, fracOf : Int) : Dp {
            return (frac * fracOf).toInt().dp
        }

        @Composable
        fun Modifier.boardHeightPortrait(
            width : Dp,
            maxHeight : Dp,
        ) : Modifier {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            val aspect by remember {
                derivedStateOf { state.puzzleAspectRatio }
            }
            val isAcrostic by remember {
                derivedStateOf { state.isAcrostic }
            }
            val showClueTabs by remember {
                derivedStateOf { state.showClueTabs }
            }

            val desiredHeight = if (isAcrostic) {
                val minHeight
                    = ACROSTIC_BOARD_HEIGHT_RATIO_MIN * utils.displayHeight
                val aspectHeight = width / aspect
                max(minHeight.dp, aspectHeight)
            } else if (showClueTabs) {
                val portraitGridRatio by remember {
                    derivedStateOf { state.portraitGridRatio }
                }
                when (portraitGridRatio) {
                    GridRatio.GR_ONE_TO_ONE -> width
                    GridRatio.GR_THIRTY_PCNT -> fracDp(0.3, utils.displayHeight)
                    GridRatio.GR_FORTY_PCNT -> fracDp(0.4, utils.displayHeight)
                    GridRatio.GR_FIFTY_PCNT -> fracDp(0.5, utils.displayHeight)
                    GridRatio.GR_SIXTY_PCNT -> fracDp(0.6, utils.displayHeight)
                    else -> width / aspect
                }
            } else {
                maxHeight
            }

            return this.height(min(maxHeight, desiredHeight))
        }

        @Composable
        fun Modifier.boardWidthLandscape() : Modifier {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            val aspect by remember {
                derivedStateOf { state.puzzleAspectRatio }
            }
            val isAcrostic by remember {
                derivedStateOf { state.isAcrostic }
            }
            val showClueTabs by remember {
                derivedStateOf { state.showClueTabs }
            }

            if (isAcrostic) {
                val maxWidth = (
                    ACROSTIC_BOARD_WIDTH_RATIO_MAX * utils.displayWidth
                ).toInt().dp
                return this.aspectRatio(aspect).widthIn(max=maxWidth)
            } else if (showClueTabs) {
                val landscapeGridRatio by remember {
                    derivedStateOf { state.landscapeGridRatio }
                }
                return when (landscapeGridRatio) {
                    GridRatio.GR_ONE_TO_ONE -> this.aspectRatio(1.0F)
                    GridRatio.GR_THIRTY_PCNT -> this.fillMaxWidth(0.3F)
                    GridRatio.GR_FORTY_PCNT -> this.fillMaxWidth(0.4F)
                    GridRatio.GR_FIFTY_PCNT -> this.fillMaxWidth(0.5F)
                    GridRatio.GR_SIXTY_PCNT -> this.fillMaxWidth(0.6F)
                    else -> this.aspectRatio(aspect)
                }
            } else {
                return this.fillMaxWidth()
            }
        }

        fun onSpecialKeyUp(key : SpecialKey) {
            when (key) {
                SpecialKey.KEY_CHANGE_CLUE_DIRECTION ->
                    viewModel.toggleSelection()
                SpecialKey.KEY_NEXT_CLUE -> viewModel.nextClue()
                SpecialKey.KEY_PREVIOUS_CLUE -> viewModel.prevClue()
                else -> onSpecialKeyUpPuzzlePage(key)
            }
        }

        fun onKeyEvent(event : KeyEvent) : Boolean {
            val isUp = event.type == KeyEventType.KeyUp
            var handled = false

            viewModel.keyboardPushBlockHide()

            when (event.key) {
                Key.Search -> {
                    if (isUp)
                        viewModel.nextClue()
                    handled = true
                }
                Key.DirectionDown -> {
                    if (isUp)
                        viewModel.moveDown()
                    handled = true
                }
                Key.DirectionUp -> {
                    if (isUp)
                        viewModel.moveUp()
                    handled = true
                }
                Key.DirectionLeft -> {
                    if (isUp)
                        viewModel.moveLeft()
                    handled = true
                }
                Key.DirectionRight -> {
                    if (isUp)
                        viewModel.moveRight()
                    handled = true
                }
                Key.DirectionCenter -> {
                    if (isUp)
                        viewModel.toggleSelection()
                    handled = true
                }
                Key.Spacebar -> {
                    if (isUp)
                        viewModel.playSpace()
                    handled = true
                }
                Key.Enter -> {
                    if (isUp)
                        viewModel.playEnter()
                    handled = true
                }
                Key.Delete, Key.Backspace -> {
                    if (isUp)
                        viewModel.deleteLetter()
                    handled = true
                }
            }

            if (!handled) {
                val keyChar = letterOrDigitKeyToChar(event.key)
                if (keyChar != null) {
                    if (event.type == KeyEventType.KeyUp)
                        viewModel.playLetter(keyChar)
                    handled = true
                }
            }

            viewModel.keyboardPopBlockHide();

            if (handled)
                return true
            else
                return onKeyEventPuzzlePage(event)
        }

        @Composable
        fun ActivityBodyPortrait(modifier : Modifier) {
            Column(modifier = modifier.fillMaxHeight()) {
                BoxWithConstraints(Modifier.weight(1.0F)) {
                    val state by viewModel
                        .playUIState
                        .collectAsStateWithLifecycle()
                    val showClueBar by remember {
                        derivedStateOf { state.isClueBelowGrid }
                    }
                    val clueBarHeight = if (showClueBar)
                        TopAppBarDefaults.TopAppBarExpandedHeight
                    else
                        0.dp
                    val maxBoardHeight = maxHeight - clueBarHeight
                    val maxBoardWidth = maxWidth

                    Column(Modifier.fillMaxHeight().fillMaxWidth()) {
                        Board(
                            Modifier.boardHeightPortrait(
                                width = maxBoardWidth,
                                maxHeight = maxBoardHeight,
                            ).fillMaxWidth(),
                        )
                        if (showClueBar)
                            ClueBar(Modifier.fillMaxWidth())
                        CluesList(Modifier.weight(1.0F).fillMaxWidth())
                    }
                }
                VoiceButtons()
                Keyboard(
                    showSpecialKeys = true,
                    onSpecialKeyUp = ::onSpecialKeyUp,
                )
            }
        }

        @Composable
        fun ActivityBodyLandscape(modifier : Modifier) {
            Column(modifier.fillMaxHeight().fillMaxWidth()) {
                Row(Modifier.weight(1.0F).fillMaxWidth()) {
                    val state
                        by viewModel.playUIState.collectAsStateWithLifecycle()
                    val showClueTabs by remember {
                        derivedStateOf { state.showClueTabs }
                    }

                    val boardWidthModifier = if (showClueTabs)
                        Modifier.width(IntrinsicSize.Min)
                    else
                        Modifier.fillMaxWidth()

                    Column(boardWidthModifier.fillMaxHeight()) {
                        Board(
                            Modifier.boardWidthLandscape()
                                .weight(1.0F),
                        )
                        val isClueBelowGrid by remember {
                            derivedStateOf { state.isClueBelowGrid }
                        }
                        if (isClueBelowGrid)
                            ClueBar(Modifier.fillMaxWidth())
                    }

                    if (showClueTabs) {
                        Column(Modifier.weight(1.0F).fillMaxHeight()) {
                            CluesList(Modifier.weight(1.0F).fillMaxWidth())
                            VoiceButtons()
                        }
                    }
                }
                Keyboard(
                    showSpecialKeys = true,
                    onSpecialKeyUp = ::onSpecialKeyUp,
                )
            }
        }

        @Composable
        fun ActivityBody(modifier : Modifier) {
            if (utils.isPortrait)
                ActivityBodyPortrait(modifier)
            else
                ActivityBodyLandscape(modifier)

            IntroDialog()
        }

        LifecycleResumeEffect(Unit) {
            utils.resumeShakeDetector()
            onPauseOrDispose {
                utils.pauseShakeDetector()
                viewModel.saveScale(boardEditState.scale)
            }
        }

        PlayLaunchers()
        Scaffold(
            modifier = Modifier.onKeyEvent(::onKeyEvent)
                .imePadding()
                .puzzleStatusBarColor(),
            topBar = { TopAppBar() },
        ) { innerPadding ->
            ActivityBody(Modifier.padding(innerPadding))
        }
    }
}


