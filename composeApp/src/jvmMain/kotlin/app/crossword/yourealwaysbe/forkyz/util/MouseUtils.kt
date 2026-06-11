
package app.crossword.yourealwaysbe.forkyz.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
actual fun Modifier.onRightClick(handler : (Offset) -> Unit) : Modifier {
    return this.pointerInput(true) {
        detectTapGestures(
            matcher = PointerMatcher.mouse(PointerButton.Secondary),
            onTap = handler,
        )
    }
}

