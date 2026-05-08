
package app.crossword.yourealwaysbe.forkyz.view

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import app.crossword.yourealwaysbe.forkyz.util.BoxInputConnection
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManageableView
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.forkyz.view.OrdinaryKey

/**
 * A View class that is blank except it establishes input connections
 */
class BoxInputView(
    context : Context,
) : View(context, null),
    BoxInputConnection.BoxInputListener,
    KeyboardManageableView,
    KoinComponent {

    private val utils : AndroidVersionUtils by inject()

    private var currentInputConnection : BoxInputConnection? = null
    private var nativeInput : Boolean = false
    private var forceCaps : Boolean = true
    var onNewResponseCallback : ((String) -> Unit)? = null
    var onDeleteResponseCallback : (() -> Unit)? = null
    var onOrdinaryKeyDownCallback : ((OrdinaryKey) -> Unit)? = null
    var onOrdinaryKeyUpCallback : ((OrdinaryKey) -> Unit)? = null

    override fun requestComposeFocus() {
        requestFocus()
    }

    var response : String? = null
        set(value) {
            field = value
            currentInputConnection?.setResponse(response)
        }

    override fun onMeasure(widthMeasureSpec : Int, heightMeasureSpec : Int) {
        setMeasuredDimension(1, 1)
    }

    override fun onNewResponse(response : String?) {
        response?.let { response ->
            onNewResponseCallback?.let { it(response) }
        }
    }

    override fun onDeleteResponse() {
        onDeleteResponseCallback?.let { it() }
    }

    override fun setNativeInput(
        nativeInput : Boolean,
        forceCaps : Boolean,
    ) : Boolean {
        val changed = (
            this.nativeInput != nativeInput
            || this.forceCaps != forceCaps
        )
        this.nativeInput = nativeInput
        this.forceCaps = forceCaps
        return changed
    }

    override fun getView() : View {
        return this;
    }

    override fun onCheckIsTextEditor() : Boolean {
        return nativeInput
    }

    override fun onCreateInputConnection(
        outAttrs : EditorInfo,
    ) : InputConnection? {
        if (onCheckIsTextEditor()) {
            currentInputConnection = BoxInputConnection(
                this,
                utils,
                response ?: "",
                this,
                forceCaps,
            ).apply {
                setOutAttrs(outAttrs)
            }
            return currentInputConnection
        } else {
            return null
        }
    }

    override fun onStringValueKeyDown(value : String) {
        onNewResponse(value)
    }

    override fun onStringValueKeyUp(value : String) {
        // ignore
    }

    override fun onOrdinaryKeyDown(key : OrdinaryKey) {
        if (key == OrdinaryKey.KEY_DELETE)
            onDeleteResponse()
        else
            onOrdinaryKeyDownCallback?.let { it(key) }
    }

    override fun onOrdinaryKeyUp(key : OrdinaryKey) {
        if (key != OrdinaryKey.KEY_DELETE)
            onOrdinaryKeyUpCallback?.let { it(key) }
    }
}

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
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.size(1.dp, 1.dp),
            factory = { context ->
                val boxInputView = BoxInputView(context).apply {
                    setFocusable(true)
                    setFocusableInTouchMode(true)
                }
                state.requestFocusAction = {
                    inputConnectionMediator.registerReceiver(boxInputView)
                    inputConnectionMediator.focusReceiver()
                }
                state.setResponseAction = { boxInputView.response = it }
                boxInputView
            },
            update = { view ->
                view.apply {
                    onNewResponseCallback = onNewResponse
                    onDeleteResponseCallback = onDeleteResponse
                    onOrdinaryKeyDownCallback = onOrdinaryKeyDown
                    onOrdinaryKeyUpCallback = onOrdinaryKeyUp
                }
            },
        )

        body()
    }
}

