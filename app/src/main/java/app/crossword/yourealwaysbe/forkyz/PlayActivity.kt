
package app.crossword.yourealwaysbe.forkyz

import android.content.Intent
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat
import androidx.lifecycle.ViewModelProvider

import com.squareup.seismic.ShakeDetector

import app.crossword.yourealwaysbe.forkyz.settings.FitToScreenMode
import app.crossword.yourealwaysbe.forkyz.settings.GridRatio
import app.crossword.yourealwaysbe.forkyz.view.BoardEditView.BoardClickListener
import app.crossword.yourealwaysbe.forkyz.view.BoardFullEditView
import app.crossword.yourealwaysbe.forkyz.view.ForkyzKeyboard
import app.crossword.yourealwaysbe.forkyz.view.OKDialog
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position

private val ACROSTIC_CLUE_TABS_WORD_SCALE = 0.7F
// min height of board in portrait / acrostic
private val ACROSTIC_BOARD_HEIGHT_RATIO_MIN = 0.25F
// max width of board in landscape / acrostic
private val ACROSTIC_BOARD_WIDTH_RATIO_MAX = 0.7F

class PlayActivity() : PuzzleActivity(), ShakeDetector.Listener {
    override protected lateinit var viewModel : PlayActivityViewModel

    private var clueTabsPageSetter = ClueTabsPageSetter()
    private var shakeDetector : ShakeDetector? = null
    private val boardViewZoomer = BoardViewZoomer()

    private val displayWidth : Int by lazy {
        getResources().getDisplayMetrics().widthPixels
    }
    private val displayHeight : Int by lazy {
        getResources().getDisplayMetrics().heightPixels
    }
    private val isPortrait : Boolean by lazy {
        displayWidth <= displayHeight
    }

    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)
            .get(PlayActivityViewModel::class.java)

        setContent {
            Launchers()

            AppTheme {
                Scaffold(
                    modifier = Modifier.onKeyEvent(this::onKeyEvent)
                        .imePadding()
                        .puzzleStatusBarColor(),
                    topBar = { TopAppBar() },
                ) { innerPadding ->
                    ActivityBody(Modifier.padding(innerPadding))
                }
            }
        }
    }

    override protected fun onResume() {
        startShakeDetector()
        super.onResume()
    }

    override protected fun onPause() {
        clueTabsPageSetter.getPage(0)?.let { viewModel.setClueTabsPage(0, it) }
        clueTabsPageSetter.getPage(1)?.let { viewModel.setClueTabsPage(1, it) }
        stopShakeDetector()
        super.onPause()
    }

    override protected fun onKeyEvent(event : KeyEvent) : Boolean {
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
            Key.Delete -> {
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
            return super.onKeyEvent(event)
    }

    override fun hearShake() {
        viewModel.pickRandomUnfilledClue()
    }

    @Composable
    override protected fun Launchers() {
        super.Launchers()

        val state by viewModel.playUIState.observeAsState()

        LaunchedEffect(state?.clueTabsPage) {
            state?.clueTabsPage?.let { page ->
                clueTabsPageSetter.setPage(0, page)
            }
        }
        LaunchedEffect(state?.clueTabsPage1) {
            state?.clueTabsPage?.let { page ->
                clueTabsPageSetter.setPage(1, page)
            }
        }
        LaunchedEffect(state?.isRandomClueOnShake) {
            if (state?.isRandomClueOnShake ?: false) {
                if (shakeDetector == null) {
                    shakeDetector = ShakeDetector(this@PlayActivity)
                    startShakeDetector()
                }
            } else {
                stopShakeDetector()
                shakeDetector = null
            }
        }
        LaunchedEffect(state?.fitToScreenMode) {
            boardViewZoomer.fitToScreenMode = state?.fitToScreenMode
        }
        LaunchedEffect(state?.isFullScreen) {
            if (state?.isFullScreen ?: false)
                utils.setFullScreen(getWindow())
        }

        // get the initial scale that was saved in settings
        // zoomer will use it if zoom to fit shouldn't happen
        LaunchedEffect(Unit) {
            viewModel.getSavedScale() { boardViewZoomer.initialScale = it }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun TopAppBar() {
        val state by viewModel.playUIState.observeAsState()

        themeHelper.ForkyzTopAppBar(
            title = {
                if (state?.isClueBelowGrid ?: false) {
                    val title = state?.puzzleTitle ?: ""
                    Text(AnnotatedString.fromHtml(title))
                } else {
                    ClueText(Modifier.fillMaxWidth())
                }
            },
            onBack = { finish() },
            actions = { OverflowMenu() },
        )
    }

    @Composable
    private fun ActivityBody(modifier : Modifier) {
        if (isPortrait)
            ActivityBodyPortrait(modifier)
        else
            ActivityBodyLandscape(modifier)

        IntroDialog()
    }

    @Composable
    private fun IntroDialog() {
        val state by viewModel.playUIState.observeAsState()

        state?.firstPlayMessage?.let { firstPlayMessage ->
            OKDialog(
                title = R.string.introduction,
                summary = firstPlayMessage,
                onClose = viewModel::clearFirstPlayMessage,
            )
        }
    }

    @Composable
    private fun ActivityBodyPortrait(modifier : Modifier) {
        Column(modifier = modifier.fillMaxHeight()) {
            Board(
                Modifier.boardHeightPortrait(
                    unrestrictedModifier = Modifier.weight(1.0F),
                ).fillMaxWidth(),
            )
            ClueBar(Modifier.fillMaxWidth())
            CluesList(Modifier.weight(1.0F).fillMaxWidth())
            VoiceButtons()
            Keyboard(
                showSpecialKeys = true,
                onSpecialKeyUp = this@PlayActivity::onSpecialKeyUp,
            )
        }
    }

    @Composable
    private fun ActivityBodyLandscape(modifier : Modifier) {
        Column(modifier.fillMaxHeight().fillMaxWidth()) {
            Row(Modifier.weight(1.0F).fillMaxWidth()) {
                val state by viewModel.playUIState.observeAsState()
                val showCluesTab = state?.showCluesTab ?: false

                val boardWidthModifier = if (showCluesTab)
                    Modifier.width(IntrinsicSize.Min)
                else
                    Modifier.fillMaxWidth()

                Column(boardWidthModifier.fillMaxHeight()) {
                    Board(
                        Modifier.boardWidthLandscape()
                            .weight(1.0F),
                    )
                    ClueBar(Modifier.fillMaxWidth())
                }

                if (showCluesTab) {
                    Column(Modifier.weight(1.0F).fillMaxHeight()) {
                        CluesList(Modifier.weight(1.0F).fillMaxWidth())
                        VoiceButtons()
                    }
                }
            }
            Keyboard(
                showSpecialKeys = true,
                onSpecialKeyUp = this@PlayActivity::onSpecialKeyUp,
            )
        }
    }

    @Composable
    private fun OverflowMenu() {
        val state by viewModel.menuState.observeAsState()
        val uiState by viewModel.playUIState.observeAsState()

        IconButton(onClick = { viewModel.expandMenu(SubMenu.MAIN) }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.overflow),
            )
        }
        DropdownMenu(
            modifier = BASE_MENU_MODIFIER,
            expanded = state?.expanded == SubMenu.MAIN,
            onDismissRequest = viewModel::dismissMenu,
        ) {
            MenuClues()
            MenuNotes()
            if (uiState?.fitToScreenMode != FitToScreenMode.FTSM_LOCKED)
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
        if (uiState?.fitToScreenMode != FitToScreenMode.FTSM_LOCKED)
            MenuZoomSub()
    }

    @Composable
    private fun MenuZoomSub() {
        MenuZoomSub(
            onZoomIn = boardViewZoomer::zoomIn,
            onZoomInMax = boardViewZoomer::zoomInMax,
            onZoomOut = boardViewZoomer::zoomOut,
            onZoomFit = boardViewZoomer::zoomFit,
            onZoomReset = boardViewZoomer::zoomReset,
        )
    }

    @Composable
    private fun MenuClues() {
        DropdownMenuItem(
            text = { MenuText(stringResource(R.string.clues)) },
            onClick = {
                viewModel.launchClueList()
                viewModel.dismissMenu()
            }
        )
    }

    /**
     * Below grid clue bar
     *
     * Modifier mainly needed for width, height is set internally
     */
    @Composable
    private fun ClueBar(modifier : Modifier) {
        val state by viewModel.playUIState.observeAsState()

        if (state?.isClueBelowGrid ?: false) {
            Row(
                modifier = modifier
                    .height(64.dp)
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
    }

    /**
     * Version of clue text with long-click actions for play &c.
     */
    @Composable
    private fun ClueText(modifier : Modifier) {
        val state by viewModel.playUIState.observeAsState()
        ClueText(
            modifier = modifier,
            clueText = state?.clueText,
            onClick = viewModel::toggleShowClueTabs,
            onClickDescription = stringResource(R.string.toggle_clue_tabs),
            onLongClick = viewModel::launchClueList,
            onLongClickDescription = stringResource(R.string.open_clue_list),
        )
    }

    @Composable
    private fun CluesList(modifier : Modifier) {
        val state by viewModel.playUIState.observeAsState()

        if (!(state?.showCluesTab ?: false))
            return

        val isAcrostic = state?.isAcrostic ?: false
        val showAllWords = isAcrostic
        val onBarLongClick : () -> Unit
            = if (isAcrostic) viewModel::toggleShowClueTabs
            else { { } }
        val onBarLongClickDescription
            = if (isAcrostic) stringResource(R.string.toggle_clue_tabs)
            else ""
        val onBarSwipeDown : () -> Unit
            = if (isAcrostic) viewModel::hideClueTabs else { { } }

        ClueTabsView(
            modifier = modifier,
            board = viewModel.getBoard(),
            onClueClick = viewModel::clickClue,
            onClueClickDescription = stringResource(R.string.select_clue),
            onClueLongClick = { viewModel.launchCurrentClueNotes() },
            onClueLongClickDescription
                = stringResource(R.string.open_clue_notes),
            onBoardClick = viewModel::clickClue,
            onBarLongClick = onBarLongClick,
            onBarLongClickDescription = onBarLongClickDescription,
            onBarSwipeDown = onBarSwipeDown,
            showAllWords = showAllWords,
            pageSetter = clueTabsPageSetter,
            maxWordScale = ACROSTIC_CLUE_TABS_WORD_SCALE,
        )
    }

    @Composable
    private fun Board(modifier : Modifier) {
        val state by viewModel.playUIState.observeAsState()
        val locked = state?.fitToScreenMode == FitToScreenMode.FTSM_LOCKED

        AndroidView(
            modifier = modifier.onSizeChanged { size ->
                boardViewZoomer.viewSize = size
            },
            factory = { context ->
                val view = BoardFullEditView(context).apply {
                    setBoard(viewModel.getBoard())
                    setFocusable(true)
                    setFocusableInTouchMode(true)
                    setBackground(
                        AppCompatResources.getDrawable(
                            context,
                            R.drawable.miniboard_no_focus_indicator_bg,
                        )
                    )
                    addBoardClickListener(object : BoardClickListener {
                        override fun onClick(
                            _position : Position,
                            previousWord : Word,
                        ) {
                            viewModel.clickBoard(previousWord)
                        }

                        override fun onLongClick(position : Position?) {
                            viewModel.longClickBoard(position)
                        }
                    })
                    setScaleListener(viewModel::saveScale)
                }

                ViewCompat.replaceAccessibilityAction(
                    view,
                    AccessibilityActionCompat.ACTION_LONG_CLICK,
                    context.getString(R.string.open_clue_notes),
                    null
                )

                // always this view gets input
                inputConnectionMediator.registerReceiver(view)
                view.requestFocusOnSized()

                boardViewZoomer.boardView = view

                view
            },
            update = { view ->
                view.apply {
                    setAllowOverScroll(!locked)
                    setAllowZoom(!locked)
                }
            },
        )
    }

    @Composable
    private fun Modifier.boardHeightPortrait(
        unrestrictedModifier : Modifier = Modifier,
    ) : Modifier {
        val state by viewModel.playUIState.observeAsState()
        val aspect = state?.puzzleAspectRatio ?: 1.0F

        if (state?.isAcrostic ?: false) {
            val minHeight = (
                ACROSTIC_BOARD_HEIGHT_RATIO_MIN * displayHeight
            ).toInt().dp
            return this.aspectRatio(aspect).heightIn(min=minHeight)
        } else if (state?.showCluesTab ?: false) {
            return when (state?.portraitGridRatio) {
                GridRatio.GR_ONE_TO_ONE -> this.aspectRatio(1.0F)
                GridRatio.GR_THIRTY_PCNT -> this.fillMaxHeight(0.3F)
                GridRatio.GR_FORTY_PCNT -> this.fillMaxHeight(0.4F)
                GridRatio.GR_FIFTY_PCNT -> this.fillMaxHeight(0.5F)
                GridRatio.GR_SIXTY_PCNT -> this.fillMaxHeight(0.6F)
                else -> this.aspectRatio(aspect)
            }
        } else {
            return this.then(unrestrictedModifier)
        }
    }

    @Composable
    private fun Modifier.boardWidthLandscape() : Modifier {
        val state by viewModel.playUIState.observeAsState()
        val aspect = state?.puzzleAspectRatio ?: 1.0F

        if (state?.isAcrostic ?: false) {
            val maxWidth = (
                ACROSTIC_BOARD_WIDTH_RATIO_MAX * displayWidth
            ).toInt().dp
            return this.aspectRatio(aspect).widthIn(max=maxWidth)
        } else if (state?.showCluesTab ?: false) {
            return when (state?.landscapeGridRatio) {
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

    private fun onSpecialKeyUp(key : ForkyzKeyboard.SpecialKey) {
        when (key) {
            ForkyzKeyboard.SpecialKey.KEY_CHANGE_CLUE_DIRECTION
                -> viewModel.toggleSelection()
            ForkyzKeyboard.SpecialKey.KEY_NEXT_CLUE
                -> viewModel.nextClue()
            ForkyzKeyboard.SpecialKey.KEY_PREVIOUS_CLUE
                -> viewModel.prevClue()
        }
    }

    private fun startShakeDetector() {
        shakeDetector?.let {
            it.start(
                getSystemService(SENSOR_SERVICE) as SensorManager,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    private fun stopShakeDetector() {
        shakeDetector?.stop()
    }
}

// TODO: make more mvvm-y - view shouldn't be driving
private class BoardViewZoomer {
    var boardView : BoardFullEditView? = null
        set(value : BoardFullEditView?) {
            field = value
            doSetScale()
        }

    var initialScale : Float? = null
        set(value : Float?) {
            field = value
            doSetScale()
        }

    var fitToScreenMode : FitToScreenMode? = null
        set(value : FitToScreenMode?) {
            field = value
            doSetScale()
        }

    var viewSize : IntSize? = null
        set(value : IntSize?) {
            field = value
            doSetScale()
        }

    private var hasSetInitialScaleMode : FitToScreenMode? = null

    fun zoomIn() { boardView?.zoomIn() }
    fun zoomInMax() { boardView?.zoomInMax() }
    fun zoomOut() { boardView?.zoomOut() }
    fun zoomFit() { boardView?.fitToView() }
    fun zoomReset() { boardView?.zoomReset() }

    private fun doSetScale() {
        val boardView = this.boardView
        val initialScale = this.initialScale
        val fitToScreenMode = this.fitToScreenMode
        val viewSize = this.viewSize

        if (
            boardView != null
            && initialScale != null
            && fitToScreenMode != null
            && viewSize != null
                && viewSize.width > 0 && viewSize.height > 0
        ) {
            when (fitToScreenMode) {
                FitToScreenMode.FTSM_START -> {
                    if (fitToScreenMode != hasSetInitialScaleMode)
                        zoomFit()
                }
                FitToScreenMode.FTSM_LOCKED -> {
                    zoomFit()
                }
                else -> {
                    if (fitToScreenMode != hasSetInitialScaleMode)
                        boardView.setCurrentScale(initialScale)
                }
            }
            hasSetInitialScaleMode = fitToScreenMode
        }
    }
}

