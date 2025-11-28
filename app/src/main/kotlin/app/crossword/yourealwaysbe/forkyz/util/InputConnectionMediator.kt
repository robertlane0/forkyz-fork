
package app.crossword.yourealwaysbe.forkyz.util

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

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
    val actionUp : Int = KeyEvent.ACTION_UP
    val actionDown : Int = KeyEvent.ACTION_DOWN
    val keyA : Int = KeyEvent.KEYCODE_A
    val keyB : Int = KeyEvent.KEYCODE_B
    val keyC : Int = KeyEvent.KEYCODE_C
    val keyD : Int = KeyEvent.KEYCODE_D
    val keyE : Int = KeyEvent.KEYCODE_E
    val keyF : Int = KeyEvent.KEYCODE_F
    val keyG : Int = KeyEvent.KEYCODE_G
    val keyH : Int = KeyEvent.KEYCODE_H
    val keyI : Int = KeyEvent.KEYCODE_I
    val keyJ : Int = KeyEvent.KEYCODE_J
    val keyK : Int = KeyEvent.KEYCODE_K
    val keyL : Int = KeyEvent.KEYCODE_L
    val keyM : Int = KeyEvent.KEYCODE_M
    val keyN : Int = KeyEvent.KEYCODE_N
    val keyO : Int = KeyEvent.KEYCODE_O
    val keyP : Int = KeyEvent.KEYCODE_P
    val keyQ : Int = KeyEvent.KEYCODE_Q
    val keyR : Int = KeyEvent.KEYCODE_R
    val keyS : Int = KeyEvent.KEYCODE_S
    val keyT : Int = KeyEvent.KEYCODE_T
    val keyU : Int = KeyEvent.KEYCODE_U
    val keyV : Int = KeyEvent.KEYCODE_V
    val keyW : Int = KeyEvent.KEYCODE_W
    val keyX : Int = KeyEvent.KEYCODE_X
    val keyY : Int = KeyEvent.KEYCODE_Y
    val keyZ : Int = KeyEvent.KEYCODE_Z
    val keySpace : Int = KeyEvent.KEYCODE_SPACE
    val keyDelete : Int = KeyEvent.KEYCODE_DEL
    val keyUp : Int = KeyEvent.KEYCODE_DPAD_UP
    val keyDown : Int = KeyEvent.KEYCODE_DPAD_DOWN
    val keyLeft : Int = KeyEvent.KEYCODE_DPAD_LEFT
    val keyRight : Int = KeyEvent.KEYCODE_DPAD_RIGHT

    private var forkyzKeyboardEditorInfo : EditorInfo? = null
    private var receiver : KeyboardManageableView? = null
    private var forkyzKeyboardInputConnection : InputConnection? = null

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

    fun sendForkyzKey(keyCode : Int, action : Int) {
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

