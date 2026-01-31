
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator

/**
 * Class for doing some control of your input
 *
 * Set response and focus
 */
class BoxInputState {
    internal var requestFocusAction : (() -> Unit)? = null
    internal var setResponseAction : ((String) -> Unit)? = null

    fun requestFocus() {
        requestFocusAction?.let { it() }
    }

    fun setResponse(response : String) {
        setResponseAction?.let { it(response) }
    }
}

/**
 * Provides a box that can manage an input connection
 *
 * InputConnectionMediator needed to mediate input connections. Use
 * state.requestFocus to get the input focused. Use state.setResponse to
 * keep the input connection up to date with the current box contents.
 *
 * @param onNewResponse callback when new response typed, should return
 * the response for the box that is now highlighted.
 * @param onDeleteResponse call back when response deleted, should
 * return the response for the box that is now highlighted
 * @param body what goes in front of the input
 */
@Composable
expect fun BoxInput(
    modifier : Modifier = Modifier,
    state : BoxInputState,
    inputConnectionMediator : InputConnectionMediator,
    onNewResponse : (String) -> Unit = { },
    onDeleteResponse : () -> Unit = { },
    body : @Composable () -> Unit,
)

