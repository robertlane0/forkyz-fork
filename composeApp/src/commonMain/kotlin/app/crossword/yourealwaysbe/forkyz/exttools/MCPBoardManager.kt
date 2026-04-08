
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.observeForeverWithProcessLifecycle

private data class MCPSettings(
    val hostPort : String,
    val running : Boolean,
)

/**
 * Manages the MCP board server lifecycle.
 *
 * Observes the MCP host/port setting and starts/stops the server
 * automatically. Inject as singleton so it persists across
 * configuration changes.
 *
 * Platform-specific subclasses implement [start] and [stop]. Call back
 * to start/stopService to do the actual running on whatever coroutine
 * or service needed.
 */
abstract class MCPBoardManager(
    private val settings : ForkyzSettings,
    private val utils : NativeBackendUtils,
    private val currentPuzzleHolder : CurrentPuzzleHolder,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var board : MCPBoard? = null

    init {
        combine(
            settings.liveExtMCPHostPort,
            settings.liveExtMCPRunning,
            ::MCPSettings,
        ).observeForeverWithProcessLifecycle { mcpSettings ->
            if (!mcpSettings.running || mcpSettings.hostPort.isEmpty())
                doStop()
            else
                doStart(mcpSettings.hostPort)
        }
    }

    fun start() {
        settings.setExtMCPRunning(true)
    }

    fun stop() {
        settings.setExtMCPRunning(false)
    }

    abstract protected fun doStart(hostPort : String)

    abstract protected fun doStop()

    /**
     * Creates and starts MCPBoard directly
     *
     * Should only be used from service that should run the thing (e.g.
     * a service on Android). Launches in its own scope.
     */
    fun startService(hostPort : String) {
        scope.launch {
            board?.stop()
            board = MCPBoard(hostPort, { currentPuzzleHolder.board }, utils)
            board?.start()
        }
    }

    /**
     * Stop running (uses scope)
     */
    fun stopService() {
        scope.launch {
            board?.stop()
            board = null
        }
    }

    /**
     * Stop running (runBlocking)
     */
    fun stopServiceBlocking() {
        runBlocking {
            board?.stop()
            board = null
        }
    }
}

