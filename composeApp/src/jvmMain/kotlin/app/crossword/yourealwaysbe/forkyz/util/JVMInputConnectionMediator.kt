
package app.crossword.yourealwaysbe.forkyz.util

actual class View
actual class EditorInfo
actual interface InputConnection

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
class JVMInputConnectionMediator() : InputConnectionMediator {
    override val actionUp : Int = -1
    override val actionDown : Int = -1
    override val keyA : Int = -1
    override val keyB : Int = -1
    override val keyC : Int = -1
    override val keyD : Int = -1
    override val keyE : Int = -1
    override val keyF : Int = -1
    override val keyG : Int = -1
    override val keyH : Int = -1
    override val keyI : Int = -1
    override val keyJ : Int = -1
    override val keyK : Int = -1
    override val keyL : Int = -1
    override val keyM : Int = -1
    override val keyN : Int = -1
    override val keyO : Int = -1
    override val keyP : Int = -1
    override val keyQ : Int = -1
    override val keyR : Int = -1
    override val keyS : Int = -1
    override val keyT : Int = -1
    override val keyU : Int = -1
    override val keyV : Int = -1
    override val keyW : Int = -1
    override val keyX : Int = -1
    override val keyY : Int = -1
    override val keyZ : Int = -1
    override val keySpace : Int = -1
    override val keyDelete : Int = -1
    override val keyUp : Int = -1
    override val keyDown : Int = -1
    override val keyLeft : Int = -1
    override val keyRight : Int = -1

    /**
     * Will refresh if receiver is new and keyboard set
     */
    override fun registerReceiver(receiver : KeyboardManageableView?) {
        // TODO
    }

    override fun clearReceiver() {
        // TODO
    }

    override fun focusReceiver() {
        // TODO
    }

    override fun showNative(forceCaps : Boolean) {
        // TODO
    }

    override fun hideNative() {
        // TODO
    }

    override fun sendForkyzKey(keyCode : Int, action : Int) {
        // TODO
    }
}

