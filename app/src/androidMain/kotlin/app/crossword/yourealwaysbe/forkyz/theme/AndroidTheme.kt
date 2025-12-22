
package app.crossword.yourealwaysbe.forkyz.theme

import android.app.Activity
import androidx.compose.material3.ColorScheme
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
actual fun getDynamicTheme(darkMode : Boolean) : ColorScheme {
    val context = LocalContext.current

    return if (DynamicColors.isDynamicColorAvailable()) {
        if (darkMode) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    } else {
        if (darkMode) darkScheme else lightScheme
    }
}

@Composable
actual fun AppTheme(
    appTheme : Theme,
    darkMode : Boolean,
    content : @Composable() () -> Unit,
) {
    val view = LocalView.current
    val colorScheme = getColorScheme(appTheme, darkMode)

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

