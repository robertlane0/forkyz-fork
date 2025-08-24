
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat

import app.crossword.yourealwaysbe.forkyz.view.BoardEditView.BoardClickListener
import app.crossword.yourealwaysbe.forkyz.view.BoardWordEditView
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Playboard.Word
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.MovementStrategy

/**
 * Miniboard current word view
 *
 * Shows current word on board. Assumes should take immediate focus
 * and setup as view targeted by keyboard.
 *
 * @param board the playboard used by the view
 * @param contentDescription content description of board view
 * @param clueID if non-null show this word not current
 * @param onClick onClick method for BoardClickListener
 * @param onLongClick onLongClick method for BoardClickListener
 * @param clickAccessibilityAction click action description
 * @param longClickAccessibilityAction long click action description
 * @param inputConnectionMediator if non-null, set as view for input
 * connection and request focus
 * @param incognitoMode whether to make the view invisible and take
 * no space (ok, 1 pixel)
 * @param useSuppressNotesList whether to use the given
 * suppressNotesList or let BoardWordEditView use its default
 * @param suppressNotesList for PlayboardRenderer.drawBox
 * @param maxWordScale maximum scale of word (else fits space)
 * @param renderWithExpectedWidth precalculate miniboard size using
 * BoxWidthConstraints to pre-calculate width (and therefore height).
 * Useful for preventing UI resizing effects, but prevents IntrinsicSize
 * measurements in parents.
 * @param totalPadding how much padding will be applied to the view for
 * use when rendering with expected width
 * @param focusable whether view should be focusable including touch mode
 * @param movementStrategy what movement strategy to use
 */
@Composable
fun BoardWordEdit(
    modifier : Modifier = Modifier,
    board : Playboard,
    contentDescription : String,
    word : Word? = null,
    onClick : (Position?, Word?) -> Unit = { _, _ -> },
    onLongClick : (Position?) -> Unit = { },
    clickAccessibilityAction : String? = null,
    longClickAccessibilityAction : String? = null,
    inputConnectionMediator : InputConnectionMediator? = null,
    incognitoMode : Boolean = false,
    ignoreScratchMode : Boolean = false,
    useSuppressNotesList : Boolean = false,
    suppressNotesList : Set<String>? = null,
    onFocusChanged : (Boolean) -> Unit = { },
    maxWordScale : Float? = null,
    renderWithExpectedWidth : Boolean = false,
    totalPadding : Dp = 0.dp,
    focusable : Boolean = true,
    movementStrategy : MovementStrategy = MovementStrategy.MOVE_NEXT_CLUE,
) {
    @Composable
    fun View(width : Int? = null) {
        AndroidView(
            modifier = modifier.fillMaxWidth(),
            factory = { context ->
                val view = BoardWordEditView(context).apply {
                    setFocusable(focusable)
                    setFocusableInTouchMode(focusable)
                    setContentDescription(contentDescription)
                    setAllowOverScroll(false)
                    width?.let { setExpectedWidth(it) }
                    maxWordScale?.let { setMaxScale(it) }

                    if (useSuppressNotesList) {
                        setBoard(board, suppressNotesList)
                        word?.let { setWord(it, suppressNotesList) }
                    } else {
                        setBoard(board)
                        word?.let { setWord(it) }
                    }

                    addBoardClickListener(object : BoardClickListener {
                        override fun onClick(
                            position : Position?,
                            previousWord : Word?,
                        ) {
                            onClick(position, previousWord)
                        }

                        override fun onLongClick(position : Position?) {
                            onLongClick(position)
                        }
                    })
                }

                // always this view gets input
                inputConnectionMediator?.let { inputConnectionMediator ->
                    inputConnectionMediator.registerReceiver(view)
                    view.setOnFocusChangeListener({ _, gainFocus ->
                        if (gainFocus)
                            inputConnectionMediator.registerReceiver(view)
                        onFocusChanged(gainFocus)
                    })
                    view.requestFocusOnSized()
                }

                view
            },
            update = { view ->
                view.apply {
                    setIncognitoMode(incognitoMode)
                    setIgnoreScratchMode(ignoreScratchMode)
                    if (useSuppressNotesList)
                        setSuppressNotesList(suppressNotesList)
                    else
                        clearSuppressNotesList()
                    setMovementStrategy(movementStrategy)
                }
                clickAccessibilityAction?.let {
                    ViewCompat.replaceAccessibilityAction(
                        view,
                        AccessibilityActionCompat.ACTION_CLICK,
                        it,
                        null,
                    )
                }
                longClickAccessibilityAction?.let {
                    ViewCompat.replaceAccessibilityAction(
                        view,
                        AccessibilityActionCompat.ACTION_LONG_CLICK,
                        it,
                        null,
                    )
                }
            },
            onRelease = { it.detach() },
        )
    }

    if (renderWithExpectedWidth) {
        BoxWithConstraints() {
            val width = with(LocalDensity.current) {
                maxWidth.toPx().toInt() - totalPadding.toPx().toInt()
            }
            View(width)
        }
    } else {
        View()
    }
}

