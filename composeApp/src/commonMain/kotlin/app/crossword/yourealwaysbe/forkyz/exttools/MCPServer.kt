
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.util.JSONObject

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
 * over HTTP.
 *
 * Call start() from a coroutine — it suspends until the server stops or
 * an error occurs.
 *
 * @param hostPort host and port to listen on, e.g. "localhost:8080"
 * @param tools the MCP tools to expose
 */
expect class MCPServer(hostPort : String, tools : List<MCPTool>) {
    val hostPort : String

    companion object {
        fun getUrlFromHostPort(hostPort : String) : String
    }

    suspend fun start()
    suspend fun stop()
}
