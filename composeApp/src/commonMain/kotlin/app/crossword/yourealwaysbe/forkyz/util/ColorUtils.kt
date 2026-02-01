
package app.crossword.yourealwaysbe.forkyz.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private val ADD_ALPHA : Int = 0xff000000.toInt()
private val DEL_ALPHA : Int = 0x00ffffff

/**
 * Add ff alpha channel
 */
fun addAlpha(color : Int) : Int {
    return color or ADD_ALPHA
}

/**
 * Remove ff alpha channel
 */
fun delAlpha(color : Int) : Int {
    return color and DEL_ALPHA
}

/**
 * For Kotlin-Java interface
 */
fun colorToInt(color : Long) : Int {
    return Color(color.toULong()).toArgb()
}

/**
 * Adds alpha ff and converts
 */
fun colorFromInt(color : Int) : Color {
    return Color(addAlpha(color))
}

