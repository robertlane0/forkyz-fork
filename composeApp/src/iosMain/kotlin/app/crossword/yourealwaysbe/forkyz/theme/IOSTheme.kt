
package app.crossword.yourealwaysbe.forkyz.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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
    CompositionLocalProvider(
        LocalScaffoldContentWindowInsets provides WindowInsets(0, 0, 0, 0),
    ) {
        MaterialTheme(
            colorScheme = getColorScheme(appTheme, darkMode),
            content = content,
        )
    }
}
