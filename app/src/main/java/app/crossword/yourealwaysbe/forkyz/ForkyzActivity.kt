
package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.app.ShareCompat

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.theme.AppTheme
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.NightModeHelper
import app.crossword.yourealwaysbe.forkyz.util.OrientationHelper
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

/**
 * Data for sharing a file using Android share
 */
data class ShareFileData(
    val title : String,
    val uri : Uri,
    val mimeType : String,
)

@AndroidEntryPoint
open class ForkyzActivity : AppCompatActivity() {

    @Inject
    lateinit var themeHelper : ThemeHelper

    @Inject
    lateinit var orientationHelper : OrientationHelper

    @Inject
    lateinit var utils : AndroidVersionUtils

    // Only used for setting nightmode for non-compose views
    // breaking mvvm here -- rather than introduce a
    // ForkyzActivityViewModel for this legacy band-aid.
    // TODO: remove with NightModeHelper once all colors from Compose
    @Inject
    lateinit var settings : ForkyzSettings

    private var nightMode : NightModeHelper? = null

    override protected fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        orientationHelper.applyOrientation(this)
    }

    override protected fun onResume() {
        super.onResume()
        if (nightMode == null) {
            nightMode = NightModeHelper.bind(this, settings)
            nightMode?.restoreNightMode()
        }
    }

    @Composable
    protected fun AppTheme(content : @Composable () -> Unit)
        = themeHelper.AppTheme(content)

    /**
     * Share a file through Android share
     */
    protected fun shareFile(data : ShareFileData) {
        val shareIntent = ShareCompat.IntentBuilder(this)
            .setChooserTitle(data.title)
            .setStream(data.uri)
            .setType(data.mimeType)
            .createChooserIntent()
        startActivity(shareIntent)
    }
}
