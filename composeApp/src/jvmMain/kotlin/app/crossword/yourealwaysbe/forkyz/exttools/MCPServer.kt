
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema

import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.asForkyzJSONObject
import app.crossword.yourealwaysbe.forkyz.util.buildJSONObject

private val TAG = "ForkyzMCPServer"

actual class MCPServer actual constructor(
    actual val hostPort : String,
    private val tools : List<MCPTool>,
) {
    actual companion object {
        actual fun getUrlFromHostPort(hostPort : String) : String {
            return "http://$hostPort/mcp"
        }
    }

    private val serverScope = CoroutineScope(
        Dispatchers.IO
            + SupervisorJob()
            + CoroutineExceptionHandler { _, e ->
                AppLogger.e(TAG, "MCP server error", e)
            }
    )

    private var server : EmbeddedServer<
        CIOApplicationEngine,
        CIOApplicationEngine.Configuration,
    >? = null

    actual suspend fun start() {
        AppLogger.d(TAG, "MCP server starting on $hostPort")
        withContext(Dispatchers.IO) {
            try {
                server = buildServer()
                server?.start(wait = true)
            } catch (e : Exception) {
                AppLogger.e(TAG, "MCP server failed to start on $hostPort", e)
            }
        }
    }

    actual suspend fun stop() {
        withContext(Dispatchers.IO) {
            server?.stop(
                gracePeriodMillis = 0,
                timeoutMillis = 500,
            )
        }
        serverScope.cancel()
        AppLogger.d(TAG, "MCP server stopped")
    }

    private fun buildServer() : EmbeddedServer<
        CIOApplicationEngine,
        CIOApplicationEngine.Configuration,
    >? {
        try {
            val colonIdx = hostPort.lastIndexOf(':')

            if (colonIdx < 0) {
                AppLogger.w(
                    TAG,
                    "No : in $hostPort, will not create MCP server",
                )
                return null
            }

            val host = hostPort.substring(0, colonIdx)
            val port = hostPort.substring(colonIdx + 1).toIntOrNull()

            if (port == null) {
                AppLogger.w(
                    TAG,
                    "Port $port is not valid, will not create MCP server",
                )
                return null
            }

            val mcpServer = Server(
                serverInfo = Implementation(
                    name = "forkyz-mcp-server",
                    version = "1.0.0",
                ),
                options = ServerOptions(
                    capabilities = ServerCapabilities(
                        tools = ServerCapabilities.Tools(listChanged = true),
                    ),
                )
            )

            for (tool in tools) {
                mcpServer.addTool(
                    name = tool.name,
                    description = tool.description,
                    inputSchema = ToolSchema(
                        properties = buildJSONObject {
                            put("input", tool.inputSchema)
                        }.toKotlinJSON,
                    )
                ) { request ->
                    val args = request
                        .arguments
                        ?.asForkyzJSONObject
                        ?.getJSONObject("input")
                    val result = tool.execute(args)
                    CallToolResult(content = listOf(TextContent(result)))
                }
            }

            return with(serverScope) {
                embeddedServer(CIO, host = host, port = port) {
                    mcpStreamableHttp { mcpServer }
                }
            }
        } catch (e : Exception) {
            AppLogger.e(TAG, "Could not create MCP Server", e)
            return null
        }
    }
}
