
package app.crossword.yourealwaysbe.forkyz.util

expect class View
expect class EditorInfo
expect interface InputConnection

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
interface InputConnectionMediator {
    val actionUp : Int
    val actionDown : Int
    val keyA : Int
    val keyB : Int
    val keyC : Int
    val keyD : Int
    val keyE : Int
    val keyF : Int
    val keyG : Int
    val keyH : Int
    val keyI : Int
    val keyJ : Int
    val keyK : Int
    val keyL : Int
    val keyM : Int
    val keyN : Int
    val keyO : Int
    val keyP : Int
    val keyQ : Int
    val keyR : Int
    val keyS : Int
    val keyT : Int
    val keyU : Int
    val keyV : Int
    val keyW : Int
    val keyX : Int
    val keyY : Int
    val keyZ : Int
    val keySpace : Int
    val keyDelete : Int
    val keyUp : Int
    val keyDown : Int
    val keyLeft : Int
    val keyRight : Int

    /**
     * Will refresh if receiver is new and keyboard set
     */
    fun registerReceiver(receiver : KeyboardManageableView?)
    fun clearReceiver()
    fun focusReceiver()
    fun showNative(forceCaps : Boolean)
    fun hideNative()
    fun sendForkyzKey(keyCode : Int, action : Int)
}

