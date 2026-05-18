
package app.crossword.yourealwaysbe.forkyz.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

import app.crossword.yourealwaysbe.forkyz.settings.Theme

@Composable
actual fun getDynamicTheme(darkMode : Boolean) : ColorScheme {
    return if (darkMode) darkScheme else lightScheme
}

@Composable
actual fun AppTheme(
    appTheme : Theme,
    darkMode : Boolean,
    content : @Composable() () -> Unit,
) {
    MaterialTheme(
        colorScheme = getColorScheme(appTheme, darkMode),
        content = content,
    )
}
