
package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.theme.AppTheme
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.OrientationHelper
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

@AndroidEntryPoint
open class ForkyzActivityKt : ComponentActivity() {

    @Inject
    lateinit var themeHelper : ThemeHelper

    @Inject
    lateinit var orientationHelper : OrientationHelper

    @Inject
    lateinit var utils : AndroidVersionUtils

    override fun onCreate(bundle : Bundle?) {
        super.onCreate(bundle)
        orientationHelper.applyOrientation(this)
    }

    @Composable
    protected fun AppTheme(content : @Composable () -> Unit)
        = themeHelper.AppTheme(content)
}
