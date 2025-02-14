
package app.crossword.yourealwaysbe.forkyz.util

import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

import app.crossword.yourealwaysbe.forkyz.util.KeyboardManager.ManageableView
import app.crossword.yourealwaysbe.forkyz.view.ForkyzKeyboard

/**
 * For connecting input methods to views in Compose
 *
 * Currently use old views setup.
 *
 * Provide the ForkyzKeyboard and the ManageableView that wants to be
 * connected. Once has both, with refresh connection on any change.
 */
class InputConnectionMediator() {
    private var forkyzKeyboard : ForkyzKeyboard? = null
    private var receiver : ManageableView? = null

    /**
     * Will refresh if keyboard is new and receiver set
     */
    fun registerForkyzKeyboard(forkyzKeyboard : ForkyzKeyboard?) {
        if (this.forkyzKeyboard !== forkyzKeyboard) {
            this.forkyzKeyboard = forkyzKeyboard
            refreshInputConnection()
        }
    }

    /**
     * Will refresh if receiver is new and keyboard set
     */
    fun registerReceiver(receiver : ManageableView?) {
        if (this.receiver !== receiver) {
            this.receiver = receiver
            refreshInputConnection()
        }
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
                        imm.showSoftInput(view, 0)
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
        forkyzKeyboard?.getEditorInfo()?.let { editorInfo ->
            receiver?.onCreateForkyzInputConnection(editorInfo)
                ?.let { inputConnection ->
                    forkyzKeyboard?.setInputConnection(inputConnection)
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
