
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator

@Composable
actual fun BoxInput(
    modifier : Modifier,
    state : BoxInputState,
    inputConnectionMediator : InputConnectionMediator,
    onNewResponse : (String) -> Unit,
    onDeleteResponse : () -> Unit,
    body : @Composable () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    state.requestFocusAction = { focusRequester.requestFocus() }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.size(1.dp, 1.dp)
                .focusRequester(focusRequester)
                .focusable()
        )
        body()
    }
}

