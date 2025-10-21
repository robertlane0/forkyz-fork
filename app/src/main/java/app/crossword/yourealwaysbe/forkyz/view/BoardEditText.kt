
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Dp

import app.crossword.yourealwaysbe.forkyz.theme.BoardColorScheme
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.letterOrDigitKeyToChar

// to avoid using Box.BLANK to keep data away from UI :(
private val BLANK : Char = ' '

class BoardEditTextState private constructor(initSelected : Int) {
    constructor() : this(initSelected = -1)

    var selected by mutableIntStateOf(initSelected)
    internal var boxInputState = BoxInputState()

    fun requestFocus() {
        boxInputState?.requestFocus()
    }

    fun setResponse(response : Char) {
        boxInputState?.setResponse(response.toString())
    }

    companion object {
        internal fun Saver() = Saver<BoardEditTextState, Int>(
            save = { it.selected },
            restore = { BoardEditTextState(initSelected = it) },
        )
    }
}

@Composable
fun rememberBoardEditTextState() : BoardEditTextState {
    return rememberSaveable(saver = BoardEditTextState.Saver()) {
        BoardEditTextState()
    }
}

/**
 * A board-like text field of given length
 *
 * @param textMeasurer shared text measurer
 * @param maxWidth the maximum width to display boxes to
 * @param inputConnectionMediator for mediating input to the box
 * @param colors colors to display the board
 * @param state box input state for controlling focus
 * @param length the total length of the boxes
 * @param value the value of the field (up to length)
 * @param onDelete callback of delete(pos) position being deleted.
 * Does not change value, that parameter should be updated by caller as
 * result of callback.
 * @param onChange is onChange(pos, newChar) means set newChar at pos,
 * returns new value (full string) or null if change should be blocked.
 * Used for updated cursor position. value parameter must also be
 * updated to change display.
 * @param skipFilled skip completed characters on text entry
 * @param separators list of separators to display between box index and
 * index -1 or null if no separators
 * @param shadow value to show "behind" actual value (in blanks, grey)
 * @param onLongPress call back when board long pressed
 * @param onFocusChanged call back when focus on input changes
 */
@Composable
fun BoardEditText(
    modifier : Modifier = Modifier,
    textMeasurer : TextMeasurer,
    maxWidth : Dp,
    inputConnectionMediator : InputConnectionMediator,
    colors : BoardColorScheme,
    state : BoardEditTextState,
    length : Int,
    value : String,
    onDelete : (Int) -> Unit = { null },
    onChange : (Int, Char) -> String? = { _, _ -> null },
    skipFilled : Boolean = false,
    separators : ImmutableList<String?> = persistentListOf(),
    shadow : String? = null,
    onTap : () -> Unit,
    onLongPress : () -> Unit,
    onFocusChanged : (Boolean) -> Unit,
) {
    fun isSelectedBlank(s : String) : Boolean {
        return (s.getOrElse(state.selected) { BLANK }) == BLANK
    }

    fun isAcceptableCharacterResponse(c : Char) : Boolean {
        return !Character.isISOControl(c) && !Character.isSurrogate(c)
    }

    fun onNewResponse(response : String) {
        response.getOrNull(0)?.let { newChar ->
            if (isAcceptableCharacterResponse(newChar)) {
                onChange(state.selected, newChar)?.let { newValue ->
                    state.selected = Math.min(length - 1, state.selected + 1)
                    while (
                        !isSelectedBlank(newValue)
                        && state.selected < (length-1)
                    ) {
                        state.selected += 1
                    }
                }
            }
        }
    }

    fun onDeleteResponse() {
        state.selected = if (isSelectedBlank(value))
            Math.max(0, state.selected - 1)
        else
            state.selected
        onDelete(state.selected)
    }

    fun onKeyEvent(event : KeyEvent) : Boolean {
        when (event.key) {
            Key.DirectionLeft -> {
                if (event.type == KeyEventType.KeyUp)
                    state.selected = Math.max(state.selected - 1, 0)
                return true
            }
            Key.DirectionRight -> {
                if (event.type == KeyEventType.KeyUp)
                    state.selected = Math.min(state.selected + 1, length - 1)
                return true
            }
            Key.Delete, Key.Backspace -> {
                if (event.type == KeyEventType.KeyUp)
                    onDeleteResponse()
                return true
            }
            Key.Spacebar -> {
                if (event.type == KeyEventType.KeyUp)
                    onNewResponse(" ")
                return true
            }
        }

        val keyChar = letterOrDigitKeyToChar(event.key)
        if (keyChar != null) {
            if (event.type == KeyEventType.KeyUp)
                onNewResponse(keyChar.toString())
            return true
        }

        return false
    }

    BoxInput(
        modifier = modifier.onFocusChanged {
            if (!it.hasFocus)
                state.selected = -1
            else if (state.selected < 0)
                state.selected = 0
            onFocusChanged(it.hasFocus)
        }.onKeyEvent(::onKeyEvent),
        state = state.boxInputState,
        inputConnectionMediator = inputConnectionMediator,
        onNewResponse = ::onNewResponse,
        onDeleteResponse = ::onDeleteResponse,
    ) {
        val boxes = remember(
            length,
            value,
            state.selected,
            separators,
            shadow,
        ) {
            (0 until length).map { col ->
                val response = value.getOrElse(col) { BLANK }
                val scratchState = ScratchState(
                    across = shadow?.getOrNull(col)?.let { it.toString() },
                )
                BoxState(
                    row = 1,
                    col = col,
                    isBlock = false,
                    isBlank = response == BLANK,
                    response = response.toString(),
                    highlighted = col == state.selected,
                    separators = Separators(
                        left = if (col == 0) separators?.getOrNull(0) else null,
                        right = separators?.getOrNull(col + 1),
                    ),
                    scratchState = scratchState,
                )
            }.toImmutableList()
        }

        BoxesEditNoInput(
            textMeasurer = textMeasurer,
            maxWidth = maxWidth,
            colors = colors,
            boxes = boxes,
            onTap = { _, col ->
                state.selected = col
                onTap()
            },
            onLongPress = { _, _ -> onLongPress() },
        )

        LaunchedEffect(value, state.selected) {
            if (state.selected >= 0) {
                val selectedChar = value.getOrElse(state.selected) { BLANK }
                state.setResponse(selectedChar)
            }
        }
    }
}
