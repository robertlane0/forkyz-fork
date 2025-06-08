
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.color.DynamicColors

import app.crossword.yourealwaysbe.forkyz.ForkyzActivity
import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.Theme

@Singleton
class ThemeHelper @Inject constructor(val settings : ForkyzSettings) {
    private val appDayNightMode : DayNightMode
        get() {
            return settings.liveAppDayNightMode.value
                ?: settings.getAppDayNightModeSync()
        }

    private val appTheme : Theme
        get() {
            return settings.liveAppTheme.value
                ?: settings.getAppThemeSync()
        }

    val cheatedColor : Color
        @Composable
        get() {
            if (isDarkMode(appDayNightMode))
                return cheatedColorDark
            else
                return cheatedColorLight
        }

    val statusBarColor : Color
        @Composable
        get() {
            return statusBarColor(appTheme, isDarkMode(appDayNightMode))
        }

    @Composable
    fun AppTheme(content : @Composable() () -> Unit) {
        val dayNightMode by settings.liveAppDayNightMode.observeAsState()
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
        val dayNightMode by settings.liveAppDayNightMode.observeAsState()
        ForkyzTopAppBar(
            appTheme = appTheme,
            darkMode = isDarkMode(dayNightMode),
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
        if (appTheme == Theme.T_DYNAMIC)
            DynamicColors.applyToActivitiesIfAvailable(app);
    }

    /**
     * To theme non-compose parts of the app
     */
    fun themeActivity(activity : Activity) {
        // needs to be blocking so can be done before end of onCreate in
        // ForkyzActivity (else toolbar not themed)
        when (appTheme) {
            Theme.T_STANDARD -> activity.setTheme(R.style.Theme_Forkyz_Standard)
            Theme.T_LEGACY_LIKE -> {
                activity.setTheme(R.style.Theme_Forkyz_LegacyLike)
            }
            else -> { /* keep main theme */ }
        }
    }

    @Composable
    private fun isDarkMode(dayNightMode : DayNightMode?) : Boolean {
        return when (appDayNightMode) {
            DayNightMode.DNM_DAY -> false
            DayNightMode.DNM_NIGHT -> true
            DayNightMode.DNM_SYSTEM -> isSystemInDarkTheme()
            else -> false
        }
    }
}
