/**
 * Attempt to make a usable version of SwipeToDismissBox
 *
 * The default is too prone to accidental flings, even when you're
 * trying to be really careful to put the thing in the right position.
 *
 * This one has a buffer zone given by LeaningStart and LeaningEnd, so
 * it will snap to these rather than the action points StartToEnd or
 * EndToStart. If it settles on a leaning state, it resets back to
 * Settled.
 *
 * TODO: there are some Material bugs that mean flingBehavior can't be
 * configured. Once resolved, there might be a nicer implementation.
 * That e.g. does not animate to the leaning position before snapping
 * back to settled.
 *
 * https://issuetracker.google.com/issues/367660226
 */

package app.crossword.yourealwaysbe.forkyz.view

import kotlin.math.roundToInt

import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

enum class SwipeToDismissBoxValue {
    Settled,
    LeaningEnd,
    StartToEnd,
    LeaningStart,
    EndToStart,
}

class SwipeToDismissBoxState(
    initialValue : SwipeToDismissBoxValue = SwipeToDismissBoxValue.Settled,
) {
    companion object {
        fun Saver() = Saver<SwipeToDismissBoxState, SwipeToDismissBoxValue>(
            save = { it.anchorState.currentValue },
            restore = { SwipeToDismissBoxState(it) },
        )
    }

    val anchorState = AnchoredDraggableState(initialValue)

    val settledValue : SwipeToDismissBoxValue
        get() { return anchorState.settledValue }
    val targetValue : SwipeToDismissBoxValue
        get() { return anchorState.targetValue }
    val currentValue : SwipeToDismissBoxValue
        get() { return anchorState.currentValue }
    val lastVelocity : Float
        get() { return anchorState.lastVelocity }

    suspend fun snapTo(value : SwipeToDismissBoxValue) {
        anchorState.snapTo(value)
    }

    suspend fun animateTo(value : SwipeToDismissBoxValue) {
        anchorState.animateTo(value)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun rememberSwipeToDismissBoxState() : SwipeToDismissBoxState {
    val state = rememberSaveable(saver = SwipeToDismissBoxState.Saver()) {
        SwipeToDismissBoxState()
    }

    // update anchors as side effect (as depends on density)
    val density = LocalDensity.current
    SideEffect {
        state.anchorState.updateAnchors(
            with(density) {
                DraggableAnchors {
                    SwipeToDismissBoxValue.EndToStart at -80.dp.toPx()
                    SwipeToDismissBoxValue.LeaningStart at -70.dp.toPx()
                    SwipeToDismissBoxValue.Settled at 0f
                    SwipeToDismissBoxValue.LeaningEnd at 70.dp.toPx()
                    SwipeToDismissBoxValue.StartToEnd at 80.dp.toPx()
                }
            }
        )
    }

    // bounce back to settled unless fully committed
    LaunchedEffect(state.settledValue) {
        when (state.settledValue) {
            SwipeToDismissBoxValue.LeaningEnd,
            SwipeToDismissBoxValue.LeaningStart
                -> state.animateTo(SwipeToDismissBoxValue.Settled)
            else -> { }
        }
    }

    return state
}

@Composable
fun SwipeToDismissBox(
    modifier : Modifier = Modifier,
    state : SwipeToDismissBoxState,
    enabled : Boolean = true,
    backgroundContent : @Composable () -> Unit = { },
    body : @Composable () -> Unit = { },
) {
    Box(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .anchoredDraggable(
                state = state.anchorState,
                enabled = enabled,
                orientation = Orientation.Horizontal,
                flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                    state = state.anchorState,
                    positionalThreshold = { d -> d },
                ),
            )
    ) {
        backgroundContent()
        Box(
            modifier = Modifier.offset {
                IntOffset(
                    x = state.anchorState.requireOffset().roundToInt(),
                    y = 0,
                )
            }
        ) {
            body()
        }
    }
}
