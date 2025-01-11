
package app.crossword.yourealwaysbe.forkyz.theme

import javax.inject.Inject
import javax.inject.Singleton

import android.app.Activity
import android.app.Application
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import com.google.android.material.color.DynamicColors

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.Theme

@Singleton
class ThemeHelper @Inject constructor(val settings : ForkyzSettings) {
    private val appDayNightMode : DayNightMode
        get() = settings.liveAppDayNightMode().value
            ?: settings.getAppDayNightModeSync()

    val appTheme : Theme = settings.getAppThemeSync()

    @Composable
    fun isDarkMode() : Boolean = when (appDayNightMode) {
        DayNightMode.DAY -> false
        DayNightMode.NIGHT -> true
        DayNightMode.SYSTEM -> isSystemInDarkTheme()
    }

    @Composable
    fun AppTheme(content : @Composable() () -> Unit) {
        AppTheme(
            appTheme = appTheme,
            darkMode = isDarkMode(),
            content = content,
        )
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun ForkyzTopAppBar(
        title : @Composable () -> Unit,
        actions : @Composable RowScope.() -> Unit,
        onBack : () -> Unit,
        scrollBehavior : TopAppBarScrollBehavior,
    ) {
        ForkyzTopAppBar(
            appTheme = appTheme,
            darkMode = isDarkMode(),
            title = title,
            actions = actions,
            onBack = onBack,
            scrollBehavior = scrollBehavior,
        )
    }

    /**
     * To theme non-compose parts of the app
     */
    fun themeApplication(app : Application) {
        // has to be blocking: a late theme change won't get picked up
        if (appTheme == Theme.DYNAMIC)
            DynamicColors.applyToActivitiesIfAvailable(app);
    }

    /**
     * To theme non-compose parts of the app
     */
    fun themeActivity(activity : Activity) {
        // needs to be blocking so can be done before end of onCreate in
        // ForkyzActivity (else toolbar not themed)
        when (appTheme) {
            Theme.STANDARD -> activity.setTheme(R.style.Theme_Forkyz_Standard)
            Theme.LEGACY_LIKE -> {
                activity.setTheme(R.style.Theme_Forkyz_LegacyLike)
            }
            else -> { /* keep main theme */ }
        }
    }
}
