
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

class IOSMCPBoardManager(
    settings : ForkyzSettings,
    utils : NativeBackendUtils,
    currentPuzzleHolder : CurrentPuzzleHolder,
) : MCPBoardManager(settings, utils, currentPuzzleHolder) {
    override protected fun doStart(hostPort : String) {
        // MCP board server not supported on iOS
    }

    override protected fun doStop() {
        // MCP board server not supported on iOS
    }
}
