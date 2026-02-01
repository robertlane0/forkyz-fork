
package app.crossword.yourealwaysbe.forkyz.util

import android.content.Context
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager

actual typealias View = android.view.View
actual typealias EditorInfo = android.view.inputmethod.EditorInfo
actual typealias InputConnection = android.view.inputmethod.InputConnection

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
class AndroidInputConnectionMediator() : InputConnectionMediator {
    override val actionUp : Int = KeyEvent.ACTION_UP
    override val actionDown : Int = KeyEvent.ACTION_DOWN
    override val keyA : Int = KeyEvent.KEYCODE_A
    override val keyB : Int = KeyEvent.KEYCODE_B
    override val keyC : Int = KeyEvent.KEYCODE_C
    override val keyD : Int = KeyEvent.KEYCODE_D
    override val keyE : Int = KeyEvent.KEYCODE_E
    override val keyF : Int = KeyEvent.KEYCODE_F
    override val keyG : Int = KeyEvent.KEYCODE_G
    override val keyH : Int = KeyEvent.KEYCODE_H
    override val keyI : Int = KeyEvent.KEYCODE_I
    override val keyJ : Int = KeyEvent.KEYCODE_J
    override val keyK : Int = KeyEvent.KEYCODE_K
    override val keyL : Int = KeyEvent.KEYCODE_L
    override val keyM : Int = KeyEvent.KEYCODE_M
    override val keyN : Int = KeyEvent.KEYCODE_N
    override val keyO : Int = KeyEvent.KEYCODE_O
    override val keyP : Int = KeyEvent.KEYCODE_P
    override val keyQ : Int = KeyEvent.KEYCODE_Q
    override val keyR : Int = KeyEvent.KEYCODE_R
    override val keyS : Int = KeyEvent.KEYCODE_S
    override val keyT : Int = KeyEvent.KEYCODE_T
    override val keyU : Int = KeyEvent.KEYCODE_U
    override val keyV : Int = KeyEvent.KEYCODE_V
    override val keyW : Int = KeyEvent.KEYCODE_W
    override val keyX : Int = KeyEvent.KEYCODE_X
    override val keyY : Int = KeyEvent.KEYCODE_Y
    override val keyZ : Int = KeyEvent.KEYCODE_Z
    override val keySpace : Int = KeyEvent.KEYCODE_SPACE
    override val keyDelete : Int = KeyEvent.KEYCODE_DEL
    override val keyUp : Int = KeyEvent.KEYCODE_DPAD_UP
    override val keyDown : Int = KeyEvent.KEYCODE_DPAD_DOWN
    override val keyLeft : Int = KeyEvent.KEYCODE_DPAD_LEFT
    override val keyRight : Int = KeyEvent.KEYCODE_DPAD_RIGHT

    private var forkyzKeyboardEditorInfo : EditorInfo? = null
    private var receiver : KeyboardManageableView? = null
    private var forkyzKeyboardInputConnection : InputConnection? = null

    /**
     * Will refresh if receiver is new and keyboard set
     */
    override fun registerReceiver(receiver : KeyboardManageableView?) {
        if (this.receiver !== receiver) {
            this.receiver = receiver
            refreshInputConnection()
        }
    }

    override fun clearReceiver() {
        hideNative()
        registerReceiver(null)
    }

    override fun focusReceiver() {
        receiver?.let { it.getView().requestFocus() }
    }

    override fun showNative(forceCaps : Boolean) {
        // Would prefer the following, but doesn't seem to work
        // (with Android Views rather than Compose)?
        // focusRequester.requestFocus()
        // LocalSoftwareKeyboardController.current?.show()/.hide()
        receiver?.let { receiver ->
            receiver.getView().let { view ->
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

    override fun hideNative() {
        receiver?.let { receiver ->
            receiver.getView().let { view ->
                receiver.setNativeInput(false, false);
                getInputMethodManager()
                    ?.hideSoftInputFromWindow(view.getWindowToken(), 0)
            }
        }
    }

    override fun sendForkyzKey(keyCode : Int, action : Int) {
        val event = KeyEvent(action, keyCode)
        val flagged = KeyEvent.changeFlags(
            event,
            KeyEvent.FLAG_KEEP_TOUCH_MODE or KeyEvent.FLAG_SOFT_KEYBOARD,
        )
        forkyzKeyboardInputConnection?.sendKeyEvent(flagged)
    }

    private fun refreshInputConnection() {
        if (forkyzKeyboardEditorInfo == null)
            forkyzKeyboardEditorInfo = EditorInfo()

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

