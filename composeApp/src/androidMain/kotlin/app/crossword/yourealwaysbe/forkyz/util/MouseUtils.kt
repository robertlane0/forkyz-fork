
package app.crossword.yourealwaysbe.forkyz.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

actual fun Modifier.onMiddleClick(handler : (Offset) -> Unit) : Modifier {
    // no right click on Android
    return this
}

