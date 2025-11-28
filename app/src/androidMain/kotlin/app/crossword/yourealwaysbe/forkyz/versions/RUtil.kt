
package app.crossword.yourealwaysbe.forkyz.versions

import android.os.Build
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.R)
open class RUtil  : QUtil() {
    public override fun setFullScreen(window : Window) {
        val insetsController = window.getInsetsController()
        if (insetsController != null) {
            insetsController.hide(WindowInsets.Type.statusBars())
        }
    }
}

