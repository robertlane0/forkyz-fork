
package app.crossword.yourealwaysbe.forkyz.settings

/**
 * Keyboard settings
 *
 * @param compact whether built-in keyboard should be compact
 * @param forceCaps force native input into upper case
 * @param haptic haptic feedback on built-in keyboard
 * @param hideButton show hide button on built-in keyboard
 * @param layout which layout to use on built-in keyboard
 * @param mode the show/hide mode of the keyboard
 * @param repeatDelay how long to hold before key repeat on built-in
 * keyboard
 * @param repeatInterval how long between repeat presses on built-in
 * keyboard
 * @param useNative whether the use the native system keyboard rather
 * than the built-in keyboard
 */
data class KeyboardSettings(
    val compact : Boolean,
    val forceCaps : Boolean,
    val haptic : Boolean,
    val hideButton : Boolean,
    val layout : KeyboardLayout,
    val mode : KeyboardMode,
    val repeatDelay : Int,
    val repeatInterval : Int,
    val useNative : Boolean,
)

