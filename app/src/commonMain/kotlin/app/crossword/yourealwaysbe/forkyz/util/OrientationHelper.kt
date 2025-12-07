
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope

import org.jetbrains.compose.resources.getString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_orientation_lock
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
                activity.lifecycleScope.launch {
                    Toast.makeText(
                        activity,
                        getString(Res.string.no_orientation_lock),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.launchIn(activity.lifecycleScope)
    }
}
