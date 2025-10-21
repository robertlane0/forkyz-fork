
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.collections.immutable.ImmutableList
import kotlinx.parcelize.Parcelize

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import app.crossword.yourealwaysbe.forkyz.settings.FitToScreenMode
import app.crossword.yourealwaysbe.forkyz.theme.BoardColorScheme
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator

private val MAX_BOX_SIZE_DP : Float = 160f
private val MIN_DP_PER_BOX : Float = 10f
private val ZOOM_FACTOR : Float = 1.25f
private val MAX_SCALE : Float = MAX_BOX_SIZE_DP / BASE_BOX_SIZE_DP
private val MIN_SCALE : Float = MIN_DP_PER_BOX / BASE_BOX_SIZE_DP
private val MAX_SCALE_WORD : Float = 1f
private val MIN_SCALE_WORD : Float = 0.6f
// how close to screen border to scroll to it
private val STICKY_PAN_THRESHOLD_DP : Float = 10f
private val DOUBLE_CLICK_INTERVAL : Int = 300 // ms

@Parcelize
internal class BoardEditStateSaveable(
    val x : Float,
    val y : Float,
    val scale : Float,
    val fitToScreenMode : FitToScreenMode?,
    val doneFirstFit : Boolean,
    val pendingFit : Boolean,
    val runningScale : Float,
) : Parcelable

/**
 * For controlling/saving the pan/zoom of the board
 */
class BoardEditState private constructor(
    initX : Float,
    initY : Float,
    initScale : Float,
    initFitToScreenMode : FitToScreenMode?,
    initDoneFirstFit : Boolean,
    initPendingFit : Boolean,
    initRunningScale : Float,
) {
    constructor() : this(
        initX = 0f,
        initY = 0f,
        initScale = 1f,
        initFitToScreenMode = null,
        initDoneFirstFit = false,
        initPendingFit = false,
        initRunningScale = 1f,
    )

    var x by mutableFloatStateOf(initX)
        private set
    var y by mutableFloatStateOf(initY)
        private set
    var scale by mutableFloatStateOf(initScale)
        private set
    var runningScale by mutableFloatStateOf(initRunningScale)
        private set
    var doneFirstFit by mutableStateOf<Boolean>(initDoneFirstFit)
        private set

    // for zoomFit
    var viewWidthPx : Float = 0f
        set(value) {
            if (field != value) {
                field = value
                calcFitZoom()
            }
        }
    var viewHeightPx : Float = 0f
        set(value) {
            if (field != value) {
                field = value
                calcFitZoom()
            }
        }
    var boardWidthBoxes : Int = 0
        set(value) {
            if (field != value) {
                field = value
                calcFitZoom()
            }
        }
    var boardHeightBoxes : Int = 0
        set(value) {
            if (field != value) {
                field = value
                calcFitZoom()
            }
        }
    /**
     * DP to Px factor
     */
    var density : Float = 0f
        set(value) {
            if (field != value) {
                field = value
                calcFitZoom()
            }
        }

    internal var boxInputState = BoxInputState()

    private var fitScale : Float = 0f
        private set

    private var pendingFit : Boolean = initPendingFit
    private var fitToScreenMode : FitToScreenMode? = initFitToScreenMode

    companion object {
        internal fun Saver() = Saver<BoardEditState, BoardEditStateSaveable>(
            save = {
                BoardEditStateSaveable(
                    it.x,
                    it.y,
                    it.scale,
                    it.fitToScreenMode,
                    it.doneFirstFit,
                    it.pendingFit,
                    it.runningScale,
                )
            },
            restore = {
                BoardEditState(
                    initX = it.x,
                    initY = it.y,
                    initScale = it.scale,
                    initFitToScreenMode = it.fitToScreenMode,
                    initDoneFirstFit = it.doneFirstFit,
                    initPendingFit = it.pendingFit,
                    initRunningScale = it.runningScale,
                )
            },
        )
    }

    fun requestFocus() {
        boxInputState?.requestFocus()
    }

    fun zoomIn() { zoomBy(ZOOM_FACTOR) }
    fun zoomInMax() { scale = checkScale(MAX_SCALE) }
    fun zoomOut() { zoomBy(1 / ZOOM_FACTOR) }
    fun zoomReset() { scale = checkScale(1f) }
    fun moveX(delta : Float) {
        if (!locked)
            x += delta
    }
    fun moveY(delta : Float) {
        if (!locked)
            y += delta
    }

    /**
     * Returns amount actually zoomed (if limits hit)
     */
    fun zoomBy(factor : Float) : Float {
        val oldScale = scale
        scale = checkScale(scale * factor)
        return scale / oldScale
    }

    /**
     * Returns amount actually zoomed (if limits hit)
     */
    fun runningZoomBy(factor : Float) : Float {
        val oldScale = scale * runningScale
        val newScale = checkScale(oldScale * factor)
        runningScale = newScale / scale
        return newScale / oldScale
    }

    fun commitRunningScale() {
        scale = runningScale * scale
        runningScale = 1f
    }

    fun zoomFit() {
        if (fitScale > 0) {
            x = 0f
            y = 0f
            scale = checkScale(fitScale)
        } else {
            pendingFit = true
        }
    }

    /**
     * Move x but snap to grid edges
     */
    fun stickyMoveX(delta : Float) {
        x = stickyMove(x, delta, viewWidthPx, boardWidthBoxes)
    }

    /**
     * Move y but snap to grid edges
     */
    fun stickyMoveY(delta : Float) {
        y = stickyMove(y, delta, viewHeightPx, boardHeightBoxes)
    }

    /**
     * Set initial fit to screen mode with saved scale
     *
     * Will do nothing if has been called before
     */
    fun setInitialFitToScreen(
        fitToScreenMode : FitToScreenMode,
        savedScale : Float,
    ) {
        if (doneFirstFit)
            return

        doneFirstFit = true
        this.fitToScreenMode = fitToScreenMode

        when (fitToScreenMode) {
            FitToScreenMode.FTSM_START -> { zoomFit() }
            FitToScreenMode.FTSM_LOCKED -> { zoomFit() }
            else -> { scale = savedScale }
        }
    }

    /**
     * Call when changed will implement lock if needed
     */
    fun updateFitToScreenMode(fitToScreenMode : FitToScreenMode) {
        this.fitToScreenMode = fitToScreenMode
        when (fitToScreenMode) {
            FitToScreenMode.FTSM_LOCKED -> { zoomFit() }
            else -> { /* do nothing */ }
        }
    }

    /**
     * Ensure current clue is visible
     */
    fun ensureVisible(seeX : Float, seeY : Float) {
        val minX = -x
        val minY = -y
        val maxX = viewWidthPx - x
        val maxY = viewHeightPx - y

        if (seeX < minX)
            x = -seeX
        else if (seeX > maxX)
            x = -(seeX - viewWidthPx)

        if (seeY < minY)
            y = -seeY
        else if (seeY > maxY)
            y = -(seeY - viewHeightPx)
    }

    /**
     * Move x by delta but stick to edges of screen
     *
     * Edge of screen calculated by sizeBoxes * current box size and
     * sizeDp is the total space available.
     *
     * Returns snapped/stuck value. Or no move if locked.
     */
    private fun stickyMove(
        x : Float,
        delta : Float,
        sizePx : Float,
        sizeBoxes : Int,
    ) : Float {
        if (locked)
            return x

        val newX = x + delta
        if (sizePx > 0 && sizeBoxes > 0 && density > 0) {
            val threshold = STICKY_PAN_THRESHOLD_DP * density
            if (Math.abs(newX) < threshold) {
                return 0f
            } else {
                val boardSize = BASE_BOX_SIZE_DP * density * sizeBoxes * scale
                val farX = sizePx - boardSize
                if (Math.abs(farX - newX) < threshold) {
                    return farX
                } else {
                    return newX
                }
            }
        } else {
            return newX
        }
    }

    private fun calcFitZoom() {
        if (
            viewWidthPx <= 0
            || viewHeightPx <= 0
            || boardWidthBoxes <= 0
            || boardHeightBoxes <=0
            || density <= 0
        )
            return

        val boxSizePx = BASE_BOX_SIZE_DP * density
        val hScale = viewWidthPx / (boxSizePx * boardWidthBoxes)
        val vScale = viewHeightPx / (boxSizePx * boardHeightBoxes)

        fitScale = Math.min(hScale, vScale)

        if (pendingFit || locked) {
            pendingFit = false
            zoomFit()
        }
    }

    /**
     * Return trimmed version of potential new scale
     */
    fun checkScale(newScale : Float) : Float {
        if (!locked || newScale == fitScale)
            return Math.max(Math.min(newScale, MAX_SCALE), MIN_SCALE)
        else
            return scale
    }

    private val locked : Boolean
        get() { return fitToScreenMode == FitToScreenMode.FTSM_LOCKED }
}

@Composable
fun rememberBoardEditState() : BoardEditState {
    return rememberSaveable(saver = BoardEditState.Saver()) {
        BoardEditState()
    }
}

// hacky method to translate pixel click to box
private fun offsetToBox(
    viewModel : BoardEditViewModel,
    state : BoardEditState,
    density : Float,
    offset : Offset,
) : BoxState? {
    // annoying repeat of boxProfile
    val boxSize = BASE_BOX_SIZE_DP * state.scale * density
    val row = ((offset.y - state.y) / boxSize).toInt()
    val col = ((offset.x - state.x) / boxSize).toInt()

    if (row < 0 || col < 0) {
        return null
    } else if (row < viewModel.height && col < viewModel.width) {
        return viewModel.boardState
            .value
            .boxes
            .getOrNull(row)
            ?.getOrNull(col)
    } else if (col < viewModel.width) {
        // annoying use of layout knowledge from below
        return viewModel.boardState
            .value
            .pinnedBoxes
            ?.getOrNull(row - viewModel.height - 1)
            ?.getOrNull(col)
    } else {
        return null
    }
}

/**
 * Full interactive board view
 *
 * @param onTap gets board row, col of tap
 * @param onLongPress gets board row, col of long press
 * @param body passes a focusReceiver callback for giving focus to the
 * input connection
 */
@Composable
fun BoardEdit(
    modifier : Modifier = Modifier,
    inputConnectionMediator : InputConnectionMediator,
    state : BoardEditState,
    colors : BoardColorScheme,
    viewModel : BoardEditViewModel,
    doubleTapFitBoard : Boolean,
    onTap : (Int, Int) -> Unit,
    onLongPress : (Int, Int) -> Unit,
) {
    BoxInput(
        modifier = modifier,
        state = state.boxInputState,
        inputConnectionMediator = inputConnectionMediator,
        onNewResponse = viewModel.playLetter,
        onDeleteResponse = viewModel.deleteLetter,
    ) {
        BoardFullBody(
            state = state,
            colors = colors,
            viewModel = viewModel,
            doubleTapFitBoard = doubleTapFitBoard,
            onTap = onTap,
            onLongPress = onLongPress,
        )

        val newResponse by viewModel
            .newResponseEvent
            .collectAsStateWithLifecycle()
        LaunchedEffect(newResponse) {
            newResponse?.let(state.boxInputState::setResponse)
            viewModel.clearNewResponseEvent()
        }
    }
}

@Composable
private fun BoardFullBody(
    modifier : Modifier = Modifier,
    state : BoardEditState,
    colors : BoardColorScheme,
    viewModel : BoardEditViewModel,
    doubleTapFitBoard : Boolean,
    onTap : (Int, Int) -> Unit,
    onLongPress : (Int, Int) -> Unit,
) {
    state.boardWidthBoxes = viewModel.width
    // more hacking of remembering that pinned has 1 box padding
    state.boardHeightBoxes = (
        viewModel.height
        + viewModel.pinnedHeight
        + if (viewModel.pinnedHeight > 0) 1 else 0
    )

    val localDensity = LocalDensity.current
    var lastTapTime = remember { 0L }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(true) {
                detectTapGestures(
                    onTap = { offset ->
                        // don't use onDoubleTap to avoid delay of onTap
                        val now = System.currentTimeMillis()
                        val isDouble = (
                            now - lastTapTime < DOUBLE_CLICK_INTERVAL
                        )
                        lastTapTime = now
                        if (isDouble && doubleTapFitBoard) {
                            state.zoomFit()
                        } else {
                            offsetToBox(
                                viewModel,
                                state,
                                localDensity.density,
                                offset,
                            )?.let { box -> onTap(box.row, box.col) }
                        }
                    },
                    onLongPress = { offset ->
                        offsetToBox(
                            viewModel,
                            state,
                            localDensity.density,
                            offset,
                        )?.let { box -> onLongPress(box.row, box.col) }
                    },
                )
            }.pointerInput(true) {
                detectTransformGestures(
                    onGesture = { centroid, pan, zoom, _ ->
                        // correct offset to zoom into point
                        val fullX = centroid.x - state.x
                        val fullY = centroid.y - state.y
                        val trueZoom = state.runningZoomBy(zoom)
                        state.moveX(-(fullX * (trueZoom - 1)))
                        state.moveY(-(fullY * (trueZoom - 1)))
                        state.stickyMoveX(pan.x)
                        state.stickyMoveY(pan.y)
                    },
                )
            }.pointerInput(true) {
                // thanks
                //https://stackoverflow.com/questions/68686117/how-to-detect-the-end-of-transform-gesture-in-jetpack-compose
                awaitPointerEventScope {
                    while (true) {
                        do {
                            val event = awaitPointerEvent()
                            val canceled = event.changes.any { it.isConsumed }
                        } while (!canceled && event.changes.any { it.pressed })
                        state.commitRunningScale()
                    }
                }
            },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.blockColor),
        ) {
            // only draw once scale know to avoid jank
            if (state.doneFirstFit) {
                val density = localDensity.density
                with (localDensity) {
                    state.viewWidthPx = minWidth.toPx()
                    state.viewHeightPx = minHeight.toPx()
                    state.density = density
                }

                // needed to keep alignment topstart when grid bigger
                // see https://issuetracker.google.com/issues/216163501
                Box(
                    modifier = Modifier.wrapContentSize(
                        Alignment.TopStart,
                        unbounded = true,
                    ),
                ) {
                    val localTextStyle = LocalTextStyle.current
                    var boxProfile = remember(
                        localDensity,
                        colors,
                        state.scale,
                        localTextStyle,
                    ) {
                        BoxProfile.build(
                            localDensity,
                            colors,
                            state.scale,
                            localTextStyle,
                        )
                    }
                    val board by viewModel.boardState
                        .collectAsStateWithLifecycle()

                    Column(
                        modifier = Modifier.graphicsLayer {
                            // bit of a hack to calculate this here but
                            // need to adjust the fact that scale
                            // expands around the center of the view, so
                            // need to calc view size
                            val widthPx = with (localDensity) {
                                boxProfile.boxSize.toPx() *
                                    state.boardWidthBoxes
                            }
                            val heightPx = with (localDensity) {
                                boxProfile.boxSize.toPx() *
                                    state.boardHeightBoxes
                            }
                            val runningOffX
                                = 0.5f * (state.runningScale - 1) * widthPx
                            val runningOffY
                                = 0.5f * (state.runningScale - 1) * heightPx

                            scaleX = state.runningScale
                            scaleY = state.runningScale
                            translationX = state.x + runningOffX
                            translationY = state.y + runningOffY
                        }
                    ) {
                        Board(
                            width = viewModel.width,
                            height = viewModel.height,
                            boxes = board.boxes,
                            images = board.images,
                            profile = boxProfile,
                        )

                        board.pinnedBoxes?.let { pinnedBoxes ->
                            Board(
                                modifier = Modifier.padding(
                                    top = boxProfile.boxSize,
                                ),
                                width = viewModel.width,
                                height = pinnedBoxes.size,
                                boxes = pinnedBoxes,
                                profile = boxProfile,
                            )
                        }
                    }

                    fun ensureVisible(row : Int, col : Int) {
                        val x = col * boxProfile.boxSizePx
                        val y = row * boxProfile.boxSizePx
                        state.ensureVisible(x, y)
                    }

                    val scrollToClue by viewModel
                        .scrollToClueEvent
                        .collectAsStateWithLifecycle()
                    LaunchedEffect(scrollToClue) {
                        scrollToClue?.let { pos ->
                            // +1 to get bottom corner
                            ensureVisible(
                                pos.currentWordBottomRow + 1,
                                pos.currentWordRightCol + 1,
                            )
                            ensureVisible(
                                pos.currentWordTopRow,
                                pos.currentWordLeftCol,
                            )
                            ensureVisible(
                                pos.cursorRow + 1,
                                pos.cursorCol + 1,
                            )
                            ensureVisible(
                                pos.cursorRow,
                                pos.cursorCol,
                            )
                            viewModel.clearScrollToClueEvent()
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full interactive board view
 *
 * Max width is an argument rather than using BoxWithConstraints so it
 * can be used with intrinsic measurements.
 *
 * @param maxWidth max width in dp to render board
 * @param onTap gets board row, col of tap
 * @param onLongPress gets board row, col of long press
 * @param body passes a focusReceiver callback for giving focus to the
 * input connection
 */
@Composable
fun WordEdit(
    modifier : Modifier = Modifier,
    textMeasurer : TextMeasurer,
    maxWidth : Dp,
    state : BoxInputState,
    inputConnectionMediator : InputConnectionMediator,
    colors : BoardColorScheme,
    viewModel : WordEditViewModel,
    onTap : (Int, Int) -> Unit,
    onLongPress : (Int, Int) -> Unit,
    incognitoMode : Boolean = false,
) {
    val inputModifier = if (incognitoMode) Modifier else modifier
    val boardModifier = if (incognitoMode) modifier else Modifier
    BoxInput(
        modifier = inputModifier,
        state = state,
        inputConnectionMediator = inputConnectionMediator,
        onNewResponse = viewModel.playLetter,
        onDeleteResponse = viewModel.deleteLetter,
    ) {
        if (!incognitoMode) {
            val boardState by viewModel.boardState.collectAsStateWithLifecycle()
            BoxesEditNoInput(
                modifier = boardModifier,
                textMeasurer = textMeasurer,
                maxWidth = maxWidth,
                colors = colors,
                boxes = boardState.boxes,
                onTap = onTap,
                onLongPress = onLongPress,
            )
        }

        val newResponse by viewModel
            .newResponseEvent
            .collectAsStateWithLifecycle()
        LaunchedEffect(newResponse) {
            newResponse?.let(state::setResponse)
            viewModel.clearNewResponseEvent()
        }
    }
}

private fun offsetToBoxWord(
    density : Float,
    boxSize : Float,
    widthBoxes : Int,
    offset : Offset,
    boxes : ImmutableList<BoxState?>,
) : BoxState? {
    val row = (offset.y / boxSize).toInt()
    val col = (offset.x / boxSize).toInt()
    val index = widthBoxes * row + col
    return boxes.getOrNull(index)
}

@Composable
fun WordEditNoInput(
    modifier : Modifier = Modifier,
    textMeasurer : TextMeasurer,
    maxWidth : Dp,
    colors : BoardColorScheme,
    viewModel : WordEditViewModel,
    onTap : (Int, Int) -> Unit,
    onLongPress : (Int, Int) -> Unit,
) {
    val boardState by viewModel.boardState.collectAsStateWithLifecycle()
    BoxesEditNoInput(
        modifier = modifier,
        textMeasurer = textMeasurer,
        maxWidth = maxWidth,
        colors = colors,
        boxes = boardState.boxes,
        onTap = onTap,
        onLongPress = onLongPress,
    )
}

@Composable
fun BoxesEditNoInput(
    modifier : Modifier = Modifier,
    textMeasurer : TextMeasurer,
    maxWidth : Dp,
    colors : BoardColorScheme,
    boxes : ImmutableList<BoxState?>,
    onTap : (Int, Int) -> Unit,
    onLongPress : (Int, Int) -> Unit,
) {
    val localDensity = LocalDensity.current
    val displayWidth = with (localDensity) { maxWidth.toPx() }
    val localTextStyle = LocalTextStyle.current
    val length = boxes.size
    val boxProfile
        = remember(localDensity, colors, length, localTextStyle) {
            val baseBoxSizePx = BASE_BOX_SIZE_DP * localDensity.density
            val fillScale = displayWidth / (baseBoxSizePx * length)
            val scale = Math.max(
                MIN_SCALE_WORD,
                Math.min(MAX_SCALE_WORD, fillScale),
            )
            BoxProfile.build(localDensity, colors, scale, localTextStyle)
        }
    val widthBoxes = (displayWidth / boxProfile.boxSizePx).toInt()

    BoardWord(
        modifier = modifier.pointerInput(true) {
            detectTapGestures(
                onTap = { offset ->
                    offsetToBoxWord(
                        localDensity.density,
                        boxProfile.boxSizePx,
                        widthBoxes,
                        offset,
                        boxes,
                    )?.let { box -> onTap(box.row, box.col) }
                },
                onLongPress = { offset ->
                    offsetToBoxWord(
                        localDensity.density,
                        boxProfile.boxSizePx,
                        widthBoxes,
                        offset,
                        boxes,
                    )?.let { box -> onLongPress(box.row, box.col) }
                },
            )
        },
        textMeasurer = textMeasurer,
        profile = boxProfile,
        boxes = boxes,
        widthBoxes = widthBoxes,
    )
}

