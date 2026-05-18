
package app.crossword.yourealwaysbe.forkyz.exttools

actual class MCPServer actual constructor(
    actual val hostPort : String,
    private val tools : List<MCPTool>,
) {
    actual companion object {
        actual fun getUrlFromHostPort(hostPort : String) : String {
            return "http://$hostPort/mcp"
        }
    }

    actual suspend fun start() {}
    actual suspend fun stop() {}
}
