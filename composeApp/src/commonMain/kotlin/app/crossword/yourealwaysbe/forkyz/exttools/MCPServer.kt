
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
import app.crossword.yourealwaysbe.puz.util.JSONObject
import app.crossword.yourealwaysbe.puz.util.asForkyzJSONObject
import app.crossword.yourealwaysbe.puz.util.buildJSONObject

private val TAG = "ForkyzMCPServer"

/**
 * A tool exposed by the MCP server.
 *
 * @param name stable identifier used in JSON-RPC tool names
 * @param description human-readable purpose of the tool
 * @param inputSchema JSON Schema describing the input parameters
 * @param execute called with the params JsonElement from the tool
 * call; returns text content for the response
 */
data class MCPTool(
    val name : String,
    val description : String,
    val inputSchema : JSONObject,
    val execute : suspend (params : JSONObject?) -> String,
)

/**
 * Minimal MCP (Model Context Protocol) server implementing JSON-RPC 2.0
 * over HTTP using ktor CIO.
 *
 * Call start() from a coroutine — it suspends until the server stops or
 * an error occurs.
 *
 * @param hostPort host and port to listen on, e.g. "localhost:8080"
 * @param tools the MCP tools to expose
 */
class MCPServer(
    val hostPort : String,
    private val tools : List<MCPTool>,
) {
    companion object {
        fun getUrlFromHostPort(hostPort : String) : String {
            return "http://$hostPort/mcp"
        }
    }

    // Use own scope to catch exceptions such as addr in use
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

    /**
     * Bind and serve until the server stops or an error occurs.
     */
    suspend fun start() {
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

    suspend fun stop() {
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

