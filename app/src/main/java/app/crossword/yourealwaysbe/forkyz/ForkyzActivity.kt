
package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.theme.AppTheme
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.NightModeHelper
import app.crossword.yourealwaysbe.forkyz.util.OrientationHelper
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

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
    // TODO: remove with NightModeHelper once all Compose
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
}
