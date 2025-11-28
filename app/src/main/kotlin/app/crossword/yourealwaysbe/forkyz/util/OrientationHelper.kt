
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.Orientation

class OrientationHelper(val settings : ForkyzSettings) {
    @SuppressWarnings("SourceLockedOrientationActivity")
    fun <T> applyOrientation(activity : T)
    where T : Activity, T : LifecycleOwner {
        settings.liveAppOrientationLock.onEach { orientation ->
            try {
                when (orientation) {
                    Orientation.O_PORTRAIT -> activity.setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    )
                    Orientation.O_LANDSCAPE -> activity.setRequestedOrientation(
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
        }.launchIn(activity.lifecycleScope)
    }
}
