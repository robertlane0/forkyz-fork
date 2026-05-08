
package app.crossword.yourealwaysbe.forkyz.util

import app.crossword.yourealwaysbe.forkyz.view.OrdinaryKey

expect class View

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
     * Restore Compose focus to this view
     */
    fun requestComposeFocus() { }

    /**
     * When a string value key goes down on ForkyzKeyboard
     */
    fun onStringValueKeyDown(value : String)

    /**
     * When a string value key goes up on Forkyz Keyboard
     */
    fun onStringValueKeyUp(value : String)

    /**
     * When a OrdinaryKey goes down on Forkyz Keyboard
     */
    fun onOrdinaryKeyDown(key : OrdinaryKey)

    /**
     * When an OrdinaryKey goes up on Forkyz Keyboard
     */
    fun onOrdinaryKeyUp(key : OrdinaryKey)
}

/**
 * For connecting input methods to views in Compose
 *
 * Currently use old views setup.
 *
 * Provide the ManageableView that wants to be connected. Will refresh
 * connection on any change. Maintains input connection between the view
 * and native keyboard if appropriate.
 */
abstract class InputConnectionMediator {
    protected var receiver : KeyboardManageableView? = null
        private set

    fun registerReceiver(receiver : KeyboardManageableView?) {
        if (this.receiver !== receiver) {
            this.receiver = receiver
        }
    }

    fun clearReceiver() {
        hideNative()
        registerReceiver(null)
    }

    fun sendStringValueKeyDown(value : String) {
        receiver?.onStringValueKeyDown(value)
    }

    fun sendStringValueKeyUp(value : String) {
        receiver?.onStringValueKeyUp(value)
    }

    fun sendOrdinaryKeyDown(key : OrdinaryKey) {
        receiver?.onOrdinaryKeyDown(key)
    }

    fun sendOrdinaryKeyUp(key : OrdinaryKey) {
        receiver?.onOrdinaryKeyUp(key)
    }

    fun focusReceiver() {
        receiver?.requestComposeFocus()
    }

    abstract fun showNative(forceCaps : Boolean)
    abstract fun hideNative()
}

