
package app.crossword.yourealwaysbe.forkyz.util

import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

import android.content.Context
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

import app.crossword.yourealwaysbe.forkyz.view.getForkyzKeyboardEditorInfo

interface KeyboardManageableView {
    /**
     * Indicates view should be in native input mode
     *
     * @param nativeInput if native input
     * @param forceCaps whether to convert all native input into uppercase
     * @return true if this is a change from previous mode
     */
    fun setNativeInput(nativeInput : Boolean, forceCaps : Boolean) : Boolean

    /**
     * The view that is manageable (usually this)
     */
    fun getView() : View

    /**
     * Create an input connection for the built-in Forkyz keyboard
     */
    fun onCreateForkyzInputConnection(ei : EditorInfo) : InputConnection
}

/**
 * For connecting input methods to views in Compose
 *
 * Currently use old views setup.
 *
 * Provide the ForkyzKeyboard and the ManageableView that wants to be
 * connected. Once has both, with refresh connection on any change. Holds the
 * live input connection between the view and ForkyzKeyboard if appropriate.
 *
 * Also provides helper functions for connecting to native keyboard.
 */
class InputConnectionMediator() {
    private var forkyzKeyboardEditorInfo : EditorInfo? = null
    private var receiver : KeyboardManageableView? = null
    var forkyzKeyboardInputConnection : InputConnection? = null
        private set

    /**
     * Will refresh if receiver is new and keyboard set
     */
    fun registerReceiver(receiver : KeyboardManageableView?) {
        if (this.receiver !== receiver) {
            this.receiver = receiver
            refreshInputConnection()
        }
    }

    fun clearReceiver() {
        hideNative()
        registerReceiver(null)
    }

    fun focusReceiver() {
        receiver?.let { it.getView().requestFocus() }
    }

    fun showNative(forceCaps : Boolean) {
        // Would prefer the following, but doesn't seem to work
        // (with Android Views rather than Compose)?
        // focusRequester.requestFocus()
        // LocalSoftwareKeyboardController.current?.show()/.hide()
        receiver?.let { receiver ->
            receiver.getView()?.let { view ->
                val nativeChanged = receiver.setNativeInput(true, forceCaps)
                if (view.requestFocus()) {
                    getInputMethodManager()?.let { imm ->
                        if (nativeChanged)
                            imm.restartInput(view)
                        val res = imm.showSoftInput(view, 0)
                    }
                }
            }
        }
    }

    fun hideNative() {
        receiver?.let { receiver ->
            receiver.getView()?.let { view ->
                receiver.setNativeInput(false, false);
                getInputMethodManager()
                    ?.hideSoftInputFromWindow(view.getWindowToken(), 0)
            }
        }
    }

    private fun refreshInputConnection() {
        if (forkyzKeyboardEditorInfo == null)
            forkyzKeyboardEditorInfo = getForkyzKeyboardEditorInfo()

        forkyzKeyboardEditorInfo?.let { editorInfo ->
            receiver?.onCreateForkyzInputConnection(editorInfo)
                ?.let { inputConnection ->
                    forkyzKeyboardInputConnection = inputConnection
                }
        }
    }

    private fun getInputMethodManager() : InputMethodManager? {
        return receiver
            ?.getView()
            ?.getContext()
            ?.getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager
    }
}
