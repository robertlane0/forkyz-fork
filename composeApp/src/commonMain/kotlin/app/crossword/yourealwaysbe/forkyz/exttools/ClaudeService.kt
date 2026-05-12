
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

import app.crossword.yourealwaysbe.forkyz.puz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.puz.util.buildJSONArray
import app.crossword.yourealwaysbe.forkyz.puz.util.buildJSONObject
import app.crossword.yourealwaysbe.forkyz.settings.ExternalToolSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.getURLJSON

private const val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"
private const val CLAUDE_MODEL = "claude-sonnet-4-5"
private const val CLAUDE_API_VERSION = "2023-06-01"
private const val CLAUDE_RESPONSE_CONTENT = "content"
private const val CLAUDE_RESPONSE_ERROR = "error"
private const val CLAUDE_RESPONSE_MESSAGE = "message"
private const val CLAUDE_RESPONSE_TEXT = "text"
private const val CLAUDE_TEMPERATURE = 1.0
private const val CLAUDE_MAX_TOKENS = 500
private const val QUERY_TIMEOUT = 30000

/**
 * Claude implementation of LLMService
 */
object ClaudeService : LLMService {
    override suspend fun makeRequest(
        settings : ForkyzSettings,
        query : String,
    ) : String? {
        val apiKey = settings.liveExtLLMAgentAPIKey.first()
        return withContext(Dispatchers.IO) {
            try {
                val json = getURLJSON(
                    url = CLAUDE_API_URL,
                    timeout = QUERY_TIMEOUT,
                    headers = mapOf(
                        "x-api-key" to apiKey,
                        "anthropic-version" to CLAUDE_API_VERSION
                    ),
                    data = getQueryData(query),
                )
                getQueryResponse(json)
            } catch (e : IOException) {
                null
            } catch (e : JSONException) {
                null
            }
        }
    }

    private fun getQueryData(query : String) : JSONObject {
        val data = buildJSONObject {
            put("model", CLAUDE_MODEL)
            put("max_tokens", CLAUDE_MAX_TOKENS)
            put("temperature", CLAUDE_TEMPERATURE)

            val messages = buildJSONArray {
                val message = buildJSONObject {
                    put("role", "user")
                    put("content", query)
                }
                add(message)
            }

            put("messages", messages)
        }
        return data
    }

    private fun getQueryResponse(json : JSONObject) : String {
        if (json.has(CLAUDE_RESPONSE_ERROR)) {
            return json.getJSONObject(CLAUDE_RESPONSE_ERROR)
                .getString(CLAUDE_RESPONSE_MESSAGE)
        } else {
            val content = json.getJSONArray(CLAUDE_RESPONSE_CONTENT)
            val first = content.getJSONObject(0)
            return first.getString(CLAUDE_RESPONSE_TEXT).trim()
        }
    }
}

