
package app.crossword.yourealwaysbe.forkyz.util

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint

/**
 * Convert key event to response string
 *
 * E.g. key_a to A, or null if not printable char
 */
fun keyEventToResponse(event : KeyEvent) : String? {
    val codePoint = event.utf16CodePoint
    val type = Character.getType(codePoint)
    val valid = (
        !Character.isISOControl(codePoint) &&
        type != Character.UNASSIGNED.toInt() &&
        type != Character.SURROGATE.toInt()
    )
    return if (valid)
        String(Character.toChars(codePoint)).uppercase()
    else
        null
}



