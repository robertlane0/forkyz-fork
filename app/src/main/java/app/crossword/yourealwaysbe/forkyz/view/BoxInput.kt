
package app.crossword.yourelawaysbe.forkyz.view

import javax.inject.Inject

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.util.BoxInputConnection
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.KeyboardManageableView
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

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
 * A View class that is blank except it establishes input connections
 */
@AndroidEntryPoint
class BoxInputView(
    context : Context,
) : View(context, null),
    BoxInputConnection.BoxInputListener,
    KeyboardManageableView {

    @Inject
    lateinit var utils : AndroidVersionUtils

    private var currentInputConnection : BoxInputConnection? = null
    private var nativeInput : Boolean = false
    private var forceCaps : Boolean = true
    var onNewResponseCallback : ((String) -> Unit)? = null
    var onDeleteResponseCallback : (() -> Unit)? = null

    var response : String? = null
        set(value) {
            field = value
            currentInputConnection?.setResponse(response)
        }

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
    }

    override fun onMeasure(widthMeasureSpec : Int, heightMeasureSpec : Int) {
        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(parentWidth, parentHeight)
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

    @Override
    override fun onCreateForkyzInputConnection(
        ei : EditorInfo,
    ) : InputConnection {
        val fic = BaseInputConnection(this, false)
        ei.inputType = InputType.TYPE_NULL
        return fic
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
fun BoxInput(
    modifier : Modifier = Modifier,
    state : BoxInputState,
    inputConnectionMediator : InputConnectionMediator,
    onNewResponse : (String) -> Unit = { },
    onDeleteResponse : () -> Unit = { },
    body : @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
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
                }
            },
        )

        body()
    }
}

