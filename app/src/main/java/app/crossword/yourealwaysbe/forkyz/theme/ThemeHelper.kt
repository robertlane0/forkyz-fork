
package app.crossword.yourealwaysbe.forkyz.theme

import javax.inject.Inject
import javax.inject.Singleton

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.appbar.AppBarLayout

import app.crossword.yourealwaysbe.forkyz.ForkyzActivity
import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.Theme

@Singleton
class ThemeHelper @Inject constructor(val settings : ForkyzSettings) {
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
        title : @Composable () -> Unit,
        actions : @Composable RowScope.() -> Unit = { },
        onBack : (() -> Unit)? = null,
        scrollBehavior : TopAppBarScrollBehavior? = null,
    ) {
        val dayNightMode by getAppDayNightModeState()
        val appTheme by getAppThemeState()
        ForkyzTopAppBar(
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
            else -> false
        }
    }

    @Composable
    private fun getAppDayNightModeState() : State<DayNightMode> =
        settings.liveAppDayNightMode.collectAsStateWithLifecycle(
            DayNightMode.DNM_SYSTEM,
            ProcessLifecycleOwner.get(),
        )

    @Composable
    private fun getAppThemeState() : State<Theme> =
        settings.liveAppTheme.collectAsStateWithLifecycle(
            Theme.T_STANDARD,
            ProcessLifecycleOwner.get(),
        )
}
