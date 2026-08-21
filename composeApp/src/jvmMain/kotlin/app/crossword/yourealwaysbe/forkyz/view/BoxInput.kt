
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManageableView
import app.crossword.yourealwaysbe.forkyz.util.View

@Composable
actual fun BoxInput(
    modifier : Modifier,
    state : BoxInputState,
    inputConnectionMediator : InputConnectionMediator,
    onNewResponse : (String) -> Unit,
    onDeleteResponse : () -> Unit,
    onOrdinaryKeyDown : (OrdinaryKey) -> Unit,
    onOrdinaryKeyUp : (OrdinaryKey) -> Unit,
    body : @Composable () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val currentOnNewResponse by rememberUpdatedState(onNewResponse)
    val currentOnDeleteResponse by rememberUpdatedState(onDeleteResponse)
    val currentOnOrdinaryKeyDown by rememberUpdatedState(onOrdinaryKeyDown)
    val currentOnOrdinaryKeyUp by rememberUpdatedState(onOrdinaryKeyUp)
    val view = remember {
        object : KeyboardManageableView {
            override fun setNativeInput(
                nativeInput : Boolean,
                forceCaps : Boolean,
                removePunctuation : Boolean,
            ) : Boolean {
                return false
            }

            override fun getView() : View = View()

            override fun requestComposeFocus() {
                focusRequester.requestFocus()
            }

            override fun onStringValueKeyDown(value : String) {
                currentOnNewResponse(value)
            }

            override fun onStringValueKeyUp(value : String) {
                // ignore
            }

            override fun onOrdinaryKeyDown(key : OrdinaryKey) {
                if (key == OrdinaryKey.KEY_DELETE)
                    currentOnDeleteResponse()
                else
                    currentOnOrdinaryKeyDown(key)
            }

            override fun onOrdinaryKeyUp(key : OrdinaryKey) {
                if (key != OrdinaryKey.KEY_DELETE)
                    currentOnOrdinaryKeyUp(key)
            }
        }
    }
    state.requestFocusAction = {
        inputConnectionMediator.registerReceiver(view)
        focusRequester.requestFocus()
    }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.size(1.dp, 1.dp)
                .focusRequester(focusRequester)
                .focusable()
        )
        body()
    }
}

