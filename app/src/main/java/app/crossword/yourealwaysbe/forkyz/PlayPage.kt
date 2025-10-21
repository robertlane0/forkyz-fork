
package app.crossword.yourealwaysbe.forkyz

import android.net.Uri
import android.view.View
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
import androidx.compose.runtime.getValue
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

import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.menu.BaseMenuModifier
import app.crossword.yourealwaysbe.forkyz.menu.MenuText
import app.crossword.yourealwaysbe.forkyz.settings.FitToScreenMode
import app.crossword.yourealwaysbe.forkyz.settings.GridRatio
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
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

/**
 * Main play page
 *
 * @param startShakeDetector start shake detection with hear shake
 * callback
 * @param stopShakeDetector stop shake detection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayPage(
    viewModel : PlayPageViewModel,
    themeHelper : ThemeHelper,
    inputConnectionMediator : InputConnectionMediator,
    displayWidth : Int,
    displayHeight : Int,
    isPortrait : Boolean,
    openNotesPage : (ClueID?) -> Unit,
    openClueListPage : () -> Unit,
    openHTMLPage : (Int) -> Unit,
    openSettingsPage : () -> Unit,
    openURI : (Uri) -> Unit,
    onFinish : () -> Unit,
    onBack : () -> Unit,
    toast : (String) -> Unit,
    handleExternalToolEvent : (ExternalToolData) -> Unit,
    setOnAppNotFoundCallback : (((AppNotFoundException) -> Unit)?) -> Unit,
    setStatusBarColor : (Color) -> Unit,
    setOnVoiceCommandCallback : (((List<String>?) -> Unit)?) -> Unit,
    launchVoiceInput : () -> Unit,
    announce : (AnnounceData, View) -> Unit,
    onSetFullScreen : () -> Unit,
    startShakeDetector : (() -> Unit) -> Unit,
    stopShakeDetector : () -> Unit,
    resumeShakeDetector : () -> Unit,
    pauseShakeDetector : () -> Unit,
) {
    val boardEditState = rememberBoardEditState()

    PuzzlePage(
        viewModel = viewModel,
        themeHelper = themeHelper,
        inputConnectionMediator = inputConnectionMediator,
        displayWidth = displayWidth,
        displayHeight = displayHeight,
        isPortrait = isPortrait,
        openClueListPage = openClueListPage,
        openNotesPage = openNotesPage,
        openHTMLPage = openHTMLPage,
        openSettingsPage = openSettingsPage,
        openURI = openURI,
        onFinish = onFinish,
        onBack = onBack,
        toast = toast,
        handleExternalToolEvent = handleExternalToolEvent,
        setOnAppNotFoundCallback = setOnAppNotFoundCallback,
        setStatusBarColor = setStatusBarColor,
        setOnVoiceCommandCallback = setOnVoiceCommandCallback,
        launchVoiceInput = launchVoiceInput,
        announce = announce,
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

            LaunchedEffect(state.isRandomClueOnShake) {
                if (state.isRandomClueOnShake) {
                    startShakeDetector {
                        viewModel.pickRandomUnfilledClue()
                    }
                } else {
                    stopShakeDetector()
                }
            }
            LaunchedEffect(Unit) {
                viewModel.getInitialSizeInfo { fitToScreenMode, scale ->
                    boardEditState.setInitialFitToScreen(fitToScreenMode, scale)
                }
            }
            LaunchedEffect(state.fitToScreenMode) {
                boardEditState.updateFitToScreenMode(state.fitToScreenMode)
            }
            LaunchedEffect(state.isFullScreen) {
                if (state.isFullScreen)
                    onSetFullScreen()
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
            val uiState by viewModel.playUIState.collectAsStateWithLifecycle()

            IconButton(onClick = { viewModel.expandMenu(PuzzleSubMenu.MAIN) }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.overflow),
                )
            }
            DropdownMenu(
                modifier = BaseMenuModifier,
                expanded = state.expanded == PuzzleSubMenu.MAIN,
                onDismissRequest = viewModel::dismissMenu,
            ) {
                MenuClues()
                MenuNotes()
                if (uiState.fitToScreenMode != FitToScreenMode.FTSM_LOCKED)
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
            if (uiState.fitToScreenMode != FitToScreenMode.FTSM_LOCKED)
                MenuZoomSub()
        }

        /**
         * Version of clue text with long-click actions for play &c.
         */
        @Composable
        fun ClueText(modifier : Modifier) {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            ClueText(
                modifier = modifier,
                clueText = state.clueText,
                onClick = ::toggleShowClueTabs,
                onClickDescription = stringResource(R.string.toggle_clue_tabs),
                onLongClick = viewModel::launchClueList,
                onLongClickDescription = stringResource(R.string.open_clue_list),
            )
        }

        @Composable
        fun Modifier.clueBarHeight() : Modifier {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()
            if (state.expandableClueLine) {
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

            themeHelper.ForkyzTopAppBar(
                modifier = Modifier.clueBarHeight(),
                title = {
                    if (state.isClueBelowGrid) {
                        val title = state.puzzleTitle ?: stringResource(
                            R.string.app_name
                        )
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
                onBack = { onBack() },
                actions = { OverflowMenu() },
            )
        }

        @Composable
        fun IntroDialog() {
            val state by viewModel.playUIState.collectAsStateWithLifecycle()

            state.firstPlayMessage?.let { firstPlayMessage ->
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

            if (!state.showClueTabs)
                return

            val showAllWords = state.isAcrostic
            val onBarLongClick : () -> Unit
                = if (state.isAcrostic) ::toggleShowClueTabs
                else { { } }
            val onBarLongClickDescription
                = if (state.isAcrostic) stringResource(R.string.toggle_clue_tabs)
                else ""
            val boardColorScheme = themeHelper.getBoardColorScheme()

            ClueTabs(
                modifier = modifier,
                textMeasurer = textMeasurer,
                viewModel = viewModel.clueTabsViewModel,
                isPortrait = isPortrait,
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
            val locked = state.fitToScreenMode == FitToScreenMode.FTSM_LOCKED
            val boardColorScheme = themeHelper.getBoardColorScheme()

            BoardEdit(
                modifier = modifier,
                inputConnectionMediator = inputConnectionMediator,
                state = boardEditState,
                viewModel = viewModel.boardEditViewModel,
                colors = boardColorScheme,
                doubleTapFitBoard = state.doubleTapFitBoard,
                onTap = { row, col ->
                    boardEditState.requestFocus()
                    viewModel.clickBoard(row, col)
                },
                onLongPress = viewModel::longClickBoard,
            )

            LaunchedEffect(Unit) {
                // TODO: this is called over and over because the
                // Board keeps getting recomposed, maybe due to
                // boardSizePortrait modifier. Harmless, but
                // suggests an issue elsewhere
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
            val aspect = state.puzzleAspectRatio

            val desiredHeight = if (state.isAcrostic) {
                val minHeight = ACROSTIC_BOARD_HEIGHT_RATIO_MIN * displayHeight
                val aspectHeight = width / aspect
                max(minHeight.dp, aspectHeight)
            } else if (state.showClueTabs) {
                when (state.portraitGridRatio) {
                    GridRatio.GR_ONE_TO_ONE -> width
                    GridRatio.GR_THIRTY_PCNT -> fracDp(0.3, displayHeight)
                    GridRatio.GR_FORTY_PCNT -> fracDp(0.4, displayHeight)
                    GridRatio.GR_FIFTY_PCNT -> fracDp(0.5, displayHeight)
                    GridRatio.GR_SIXTY_PCNT -> fracDp(0.6, displayHeight)
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
            val aspect = state.puzzleAspectRatio

            if (state.isAcrostic) {
                val maxWidth = (
                    ACROSTIC_BOARD_WIDTH_RATIO_MAX * displayWidth
                ).toInt().dp
                return this.aspectRatio(aspect).widthIn(max=maxWidth)
            } else if (state.showClueTabs) {
                return when (state.landscapeGridRatio) {
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
                SpecialKey.KEY_CHANGE_CLUE_DIRECTION -> viewModel.toggleSelection()
                SpecialKey.KEY_NEXT_CLUE -> viewModel.nextClue()
                SpecialKey.KEY_PREVIOUS_CLUE -> viewModel.prevClue()
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
                    val showClueBar = state.isClueBelowGrid
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
                    val state by viewModel.playUIState.collectAsStateWithLifecycle()

                    val boardWidthModifier = if (state.showClueTabs)
                        Modifier.width(IntrinsicSize.Min)
                    else
                        Modifier.fillMaxWidth()

                    Column(boardWidthModifier.fillMaxHeight()) {
                        Board(
                            Modifier.boardWidthLandscape()
                                .weight(1.0F),
                        )
                        if (state.isClueBelowGrid)
                            ClueBar(Modifier.fillMaxWidth())
                    }

                    if (state.showClueTabs) {
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
            if (isPortrait)
                ActivityBodyPortrait(modifier)
            else
                ActivityBodyLandscape(modifier)

            IntroDialog()
        }

        LifecycleResumeEffect(Unit) {
            resumeShakeDetector()
            onPauseOrDispose {
                pauseShakeDetector()
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


