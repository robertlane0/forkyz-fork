
package app.crossword.yourealwaysbe.forkyz.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.Theme
import app.crossword.yourealwaysbe.forkyz.util.collectAsStateWithProcessLifecycle

class ThemeHelper(val settings : ForkyzSettings) {
    val cheatedColor : Color
        @Composable
        get() {
            val appDayNightMode by getAppDayNightModeState()
            if (isDarkMode(appDayNightMode))
                return cheatedColorDark
            else
                return cheatedColorLight
        }

    val statusBarColor : Color
        @Composable
        get() {
            val appDayNightMode by getAppDayNightModeState()
            val appTheme by getAppThemeState()
            return statusBarColor(appTheme, isDarkMode(appDayNightMode))
        }

    @Composable
    fun LegacyStatusBarColor(setStatusBarColor : (Color) -> Unit) {
        val appDayNightMode by getAppDayNightModeState()
        val appTheme by getAppThemeState()
        val color = statusBarColor(appTheme, isDarkMode(appDayNightMode))
        LaunchedEffect(color) {
            setStatusBarColor(color)
        }
    }

    @Composable
    fun AppTheme(content : @Composable() () -> Unit) {
        val dayNightMode by getAppDayNightModeState()
        val appTheme by getAppThemeState()
        AppTheme(
            appTheme = appTheme,
            darkMode = isDarkMode(dayNightMode),
            content = content,
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun ForkyzTopAppBar(
        modifier : Modifier = Modifier,
        title : @Composable () -> Unit,
        actions : @Composable RowScope.() -> Unit = { },
        onBack : (() -> Unit)? = null,
        scrollBehavior : TopAppBarScrollBehavior? = null,
    ) {
        val dayNightMode by getAppDayNightModeState()
        val appTheme by getAppThemeState()
        ForkyzTopAppBar(
            modifier = modifier,
            appTheme = appTheme,
            darkMode = isDarkMode(dayNightMode),
            title = title,
            actions = actions,
            onBack = onBack,
            scrollBehavior = scrollBehavior,
        )
    }

    @Composable
    fun getBoardColorScheme() : BoardColorScheme {
        val dayNightMode by getAppDayNightModeState()
        return getBoardColorScheme(isDarkMode(dayNightMode))
    }

    @Composable
    fun getCircleProgressBarColorScheme() : CircleProgressBarColorScheme {
        val dayNightMode by getAppDayNightModeState()
        return getCircleProgressBarColorScheme(isDarkMode(dayNightMode))
    }

    @Composable
    private fun isDarkMode(appDayNightMode : DayNightMode) : Boolean {
        return when (appDayNightMode) {
            DayNightMode.DNM_DAY -> false
            DayNightMode.DNM_NIGHT -> true
            DayNightMode.DNM_SYSTEM -> isSystemInDarkTheme()
        }
    }

    @Composable
    private fun getAppDayNightModeState() : State<DayNightMode> =
        settings.liveAppDayNightMode.collectAsStateWithProcessLifecycle(
            DayNightMode.DNM_SYSTEM,
        )

    @Composable
    private fun getAppThemeState() : State<Theme> =
        settings.liveAppTheme.collectAsStateWithProcessLifecycle(
            Theme.T_STANDARD,
        )
}
