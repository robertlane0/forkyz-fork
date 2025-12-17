
package app.crossword.yourealwaysbe.forkyz.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

import app.crossword.yourealwaysbe.forkyz.settings.Theme

@Composable
actual fun AppTheme(
    appTheme : Theme,
    darkMode : Boolean,
    content : @Composable() () -> Unit,
) {
    val colorScheme = if (darkMode) darkScheme else lightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

