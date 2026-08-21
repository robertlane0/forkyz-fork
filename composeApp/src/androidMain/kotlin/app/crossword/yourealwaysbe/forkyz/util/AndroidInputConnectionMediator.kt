
package app.crossword.yourealwaysbe.forkyz.util

import android.content.Context
import android.view.inputmethod.InputMethodManager

actual typealias View = android.view.View

class AndroidInputConnectionMediator() : InputConnectionMediator() {
    override fun showNative(forceCaps : Boolean, removePunctuation : Boolean) {
        // Would prefer the following, but doesn't seem to work
        // (with Android Views rather than Compose)?
        // focusRequester.requestFocus()
        // LocalSoftwareKeyboardController.current?.show()/.hide()
        receiver?.let { receiver ->
            receiver.getView().let { view ->
                val nativeChanged = receiver.setNativeInput(
                    true,
                    forceCaps,
                    removePunctuation,
                )
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

    override fun hideNative() {
        receiver?.let { receiver ->
            receiver.getView().let { view ->
                receiver.setNativeInput(false, false, false);
                getInputMethodManager()
                    ?.hideSoftInputFromWindow(view.getWindowToken(), 0)
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

