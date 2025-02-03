
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
        get() = settings.liveAppDayNightMode.value
            ?: settings.getAppDayNightModeSync()

    private val appTheme : Theme
        get() = settings.liveAppTheme.value
            ?: settings.getAppThemeSync()

    @Composable
    fun isDarkMode() : Boolean = when (appDayNightMode) {
        DayNightMode.DNM_DAY -> false
        DayNightMode.DNM_NIGHT -> true
        DayNightMode.DNM_SYSTEM -> isSystemInDarkTheme()
        else -> false
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

    fun setupStatusBar(
        activity : ForkyzActivity,
        appBarLayout : AppBarLayout,
    ) {
        val nightModeFlags = activity
            .getResources()
            .getConfiguration().uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (
            appTheme == Theme.T_LEGACY_LIKE
            && nightModeFlags != Configuration.UI_MODE_NIGHT_YES
        ) {
            activity.setStatusBarColor(
                appBarLayout,
                ContextCompat.getColor(activity, R.color.primary),
            )
        } else {
            activity.setStatusBarElevation(appBarLayout)
        }
    }
}
