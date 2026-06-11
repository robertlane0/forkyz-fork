
package app.crossword.yourealwaysbe.forkyz.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

expect fun Modifier.onRightClick(handler : (Offset) -> Unit) : Modifier

