
package app.crossword.yourealwaysbe.forkyz.util

import java.util.logging.Logger

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardMode
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardSettings

/**
 * Info about the soft keyboard
 *
 * @param hideOnBack whether keyboard should be hidden if back gesture
 * fired
 * @param showHideButton whether to show the keyboard hide button
 * @param forceCaps whether to force caps on input
 */
data class KeyboardInfo(
    val useNative : Boolean = false,
    val hideOnBack : Boolean = false,
    val showHideButton : Boolean = false,
    val forceCaps : Boolean = false,
)

/**
 * Create a new manager to handle the keyboard
 *
 * To use, pass on calls to the implemented methods below.
 *
 * @param settings the settings instance for the app
 * @param showCB callback during init whenever the keyboard needs to be
 * really shown (activity/viewmodel have to handle that).
 * @param hideCB callback during init when the keyboard needs to be
 * really hidden.
 */
class KeyboardManagerKt(
    val settings : ForkyzSettings,
    showCB : (KeyboardInfo) -> Unit,
    hideCB : () -> Unit,
) {
    private val LOGGER = Logger.getLogger(
        KeyboardManagerKt::class.toString()
    )

    private var blockHideDepth : Int = 0

    init {
        settings.getKeyboardSettings() { ks ->
            if (ks.mode == KeyboardMode.KM_ALWAYS_SHOW) {
                showKeyboard(showCB)
            } else {
                hideKeyboard(hideCB)
            }
        }
    }

    /**
     * Show keyboard
     *
     * @param showCB as in constructor
     */
    fun showKeyboard(showCB : (KeyboardInfo) -> Unit) {
        settings.getKeyboardSettings() { ks ->
            showKeyboard(ks, showCB)
        }
    }

    fun hideKeyboard(hideCB : () -> Unit) {
        hideKeyboard(hideCB, false)
    }

    /**
     * Hide the keyboard unless the user always wants it
     *
     * Will not hide if the user is currently pressing a key
     *
     * @param force force hide the keyboard, even if user has set always
     * show
     */
    fun hideKeyboard(hideCB : () -> Unit, force : Boolean) {
        settings.getKeyboardSettings() { ks ->
            hideKeyboard(ks, hideCB, force)
        }
    }

    /**
     * Add a block hide request
     *
     * hideKeyboard will only have an effect if there are no block hide
     * requests (or force was passed to hideKeyboard)
     */
    fun pushBlockHide() { blockHideDepth++ }

    /**
     * Remove a block hide request
     */
    fun popBlockHide() { blockHideDepth-- }

    private val isBlockHide : Boolean
        get() = blockHideDepth > 0

    /**
     * Hides keyboard according to settings
     */
    private fun hideKeyboard(
        ks : KeyboardSettings,
        hideCB : () -> Unit,
        force : Boolean,
    ) {
        val prefHide =
            ks.mode != KeyboardMode.KM_ALWAYS_SHOW
                && ks.mode != KeyboardMode.KM_HIDE_MANUAL
        val softHide =
            prefHide && !isBlockHide // && !keyboardView.hasKeysDown()
        val doHide = force || softHide

        if (doHide)
            hideCB()
    }

    private fun showKeyboard(
        ks : KeyboardSettings,
        showCB : (KeyboardInfo) -> Unit,
    ) {
        if (ks.mode != KeyboardMode.KM_NEVER_SHOW) {
            val hideOnBack = !ks.useNative
                && !ks.hideButton
                && ks.mode != KeyboardMode.KM_ALWAYS_SHOW
            val showHideButton = ks.hideButton
                && (
                    ks.mode == KeyboardMode.KM_HIDE_MANUAL
                    || ks.mode == KeyboardMode.KM_SHOW_SPARINGLY
                )

            showCB(
                KeyboardInfo(
                    ks.useNative,
                    hideOnBack,
                    showHideButton,
                    ks.forceCaps,
                )
            )
        }
    }
}
