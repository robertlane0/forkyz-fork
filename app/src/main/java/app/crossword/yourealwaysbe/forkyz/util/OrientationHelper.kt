
package app.crossword.yourealwaysbe.forkyz.util

import javax.inject.Inject
import javax.inject.Singleton

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.Orientation

@Singleton
class OrientationHelper @Inject constructor(val settings : ForkyzSettings) {
    @SuppressWarnings("SourceLockedOrientationActivity")
    fun <T> applyOrientation(activity : T)
    where T : Activity, T : LifecycleOwner {
        settings.liveAppOrientationLock().observe(activity) { orientation ->
            try {
                when (orientation) {
                    Orientation.PORTRAIT -> activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    )
                    Orientation.LANDSCAPE -> activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    )
                    else -> activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    )
                }
            } catch(e : RuntimeException) {
                Toast.makeText(
                    activity,
                    R.string.no_orientation_lock,
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
