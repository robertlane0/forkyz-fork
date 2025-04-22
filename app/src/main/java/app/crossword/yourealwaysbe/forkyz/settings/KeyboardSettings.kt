

package app.crossword.yourealwaysbe.forkyz.settings

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
