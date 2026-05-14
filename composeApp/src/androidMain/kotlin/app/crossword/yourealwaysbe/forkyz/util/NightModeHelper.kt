
package app.crossword.yourealwaysbe.forkyz.util

import java.lang.ref.WeakReference
import java.util.function.Consumer
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.AppLogger

/**
 * Heavily adapted from sources below, description no longer relevant so
 * removed.
 *
 * Original source: https://gist.github.com/slightfoot/c508cdc8828a478572e0
 * Adapted for Shortyz by Simon Lightfoot <simon></simon>@demondevelopers.com>
 */
class NightModeHelper private constructor(
    activity : AppCompatActivity,
    private val settings : ForkyzSettings,
) {
    private val mActivity = WeakReference<AppCompatActivity>(activity)

    fun restoreNightMode() {
        mActivity.get()?.let { activity ->
            getCurrentMode { currentMode ->
                when (currentMode) {
                    DayNightMode.DNM_DAY ->
                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_NO,
                        )
                    DayNightMode.DNM_NIGHT ->
                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_YES,
                        )
                    else ->
                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                        )
                }
            }
        }
    }

    fun getCurrentMode(cb : (DayNightMode) -> Unit) {
        settings.getAppDayNightMode(cb)
    }

    companion object {
        private val TAG = "ForkyzNightModeHelper"

        fun bind(
            activity : AppCompatActivity,
            settings : ForkyzSettings,
        ) : NightModeHelper {
            val helper = NightModeHelper(activity, settings)
            helper.restoreNightMode()

            // This may seem pointless but it forces the Theme to be reloaded
            // with new styles that would change due to new Configuration.
            val activityTheme = getTheme(activity)
            activity.setTheme(activityTheme)

            return helper
        }

        /**
         * Helper method to get the theme resource id. Warning, accessing
         * non-public methods is a no-no and there is no guarantee this will
         * work.
         *
         * @param context the context you want to extract the
         * theme-resource-id from
         * @return The themeId associated w/ the context
         */
        private fun getTheme(context : Context) : Int {
            try {
                val wrapper : Class<*> = Context::class.java
                val method = wrapper.getMethod("getThemeResId")
                method.setAccessible(true)
                return (method.invoke(context) as Int?)!!
            } catch (e : Exception) {
                AppLogger.w(TAG, "Exception getting theme", e)
            }
            return 0
        }
    }
}
