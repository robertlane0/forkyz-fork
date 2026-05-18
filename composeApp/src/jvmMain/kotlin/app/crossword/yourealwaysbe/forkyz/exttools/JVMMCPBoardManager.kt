
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

class JVMMCPBoardManager(
    settings : ForkyzSettings,
    utils : NativeBackendUtils,
    currentPuzzleHolder : CurrentPuzzleHolder,
) : MCPBoardManager(settings, utils, currentPuzzleHolder) {
    override protected fun doStart(hostPort : String) {
        startService(hostPort)
    }

    override protected fun doStop() {
        stopService()
    }
}
