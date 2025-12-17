
package app.crossword.yourealwaysbe.forkyz.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.android.material.color.DynamicColors

import app.crossword.yourealwaysbe.forkyz.settings.Theme

@Composable
actual fun AppTheme(
    appTheme : Theme,
    darkMode : Boolean,
    content : @Composable() () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current

    val colorScheme = when (appTheme) {
        // only diff is app bar, see ForkyzTopAppBar
        Theme.T_LEGACY_LIKE -> if (darkMode) darkScheme else lightScheme
        Theme.T_DYNAMIC ->
            if (DynamicColors.isDynamicColorAvailable()) {
                if (darkMode) dynamicDarkColorScheme(context)
                else dynamicLightColorScheme(context)
            } else {
                if (darkMode) darkScheme else lightScheme
            }
        // Theme.T_STANDARD
        else -> if (darkMode) darkScheme else lightScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val isLight = !darkMode && appTheme != Theme.T_LEGACY_LIKE
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

