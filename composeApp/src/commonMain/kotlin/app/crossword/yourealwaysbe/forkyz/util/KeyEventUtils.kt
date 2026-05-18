
package app.crossword.yourealwaysbe.forkyz.util

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint

// Returns the printable character for a key event, uppercased, or null.
fun keyEventToResponse(event : KeyEvent) : String? {
    val codePoint = event.utf16CodePoint

    // C0 controls (U+0000–U+001F), DEL (U+007F), C1 controls (U+0080–U+009F)
    if (codePoint in 0x00..0x1F || codePoint in 0x7F..0x9F)
        return null

    return if (codePoint <= 0xFFFF) {
        val char = codePoint.toChar()
        val cat = char.category
        if (cat == CharCategory.UNASSIGNED || cat == CharCategory.SURROGATE)
            null
        else
            char.toString().uppercase()
    } else {
        val shifted = codePoint - 0x10000
        val high = ((shifted shr 10) + 0xD800).toChar()
        val low = ((shifted and 0x3FF) + 0xDC00).toChar()
        charArrayOf(high, low).concatToString().uppercase()
    }
}
