
package app.crossword.yourealwaysbe.forkyz.exttools

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

private val TAG = "ForkyzAndroidMCPBoardManager"

class AndroidMCPBoardManager(
    private val context : Context,
    settings : ForkyzSettings,
    utils : NativeBackendUtils,
    currentPuzzleHolder : CurrentPuzzleHolder,
) : MCPBoardManager(settings, utils, currentPuzzleHolder) {
    override protected fun doStart(hostPort : String) {
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, MCPService::class.java)
                    .putExtra(MCPService.EXTRA_HOST_PORT, hostPort),
            )
        } catch (e : IllegalStateException) {
            AppLogger.w(TAG, "Could not start MCP service in foreground", e)
        }
    }

    override protected fun doStop() {
        context.stopService(Intent(context, MCPService::class.java))
    }
}
