
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

import app.crossword.yourealwaysbe.forkyz.puz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.puz.util.buildJSONObject
import app.crossword.yourealwaysbe.forkyz.settings.ExternalToolSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.getURLJSON

private const val CHAT_GPT_API_URL = "https://api.openai.com/v1/completions"
private const val CHAT_GPT_MODEL = "gpt-3.5-turbo-instruct"
private const val CHAT_GPT_RESPONSE_CHOICES = "choices"
private const val CHAT_GPT_RESPONSE_ERROR = "error"
private const val CHAT_GPT_RESPONSE_MESSAGE = "message"
private const val CHAT_GPT_RESPONSE_TEXT = "text"
private const val CHAT_GPT_TEMPERATURE = 1.0
private const val CHAT_GPT_MAX_TOKENS = 500
private const val QUERY_TIMEOUT = 30000

/**
 * ChatGPT implementation of LLMService
 */
object ChatGPTService : LLMService {
    override suspend fun makeRequest(
        settings : ForkyzSettings,
        query : String
    ) : String? {
        val apiKey = settings.liveExtLLMAgentAPIKey.first()
        return withContext(Dispatchers.IO) {
            try {
                val json = getURLJSON(
                    url = CHAT_GPT_API_URL,
                    timeout = QUERY_TIMEOUT,
                    headers = mapOf("Authorization" to "Bearer $apiKey"),
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
            put("model", CHAT_GPT_MODEL)
            put("prompt", query)
            put("max_tokens", CHAT_GPT_MAX_TOKENS)
            put("temperature", CHAT_GPT_TEMPERATURE)
        }
        return data
    }

    private fun getQueryResponse(json : JSONObject) : String {
        if (json.has(CHAT_GPT_RESPONSE_ERROR)) {
            return json.getJSONObject(CHAT_GPT_RESPONSE_ERROR)
                .getString(CHAT_GPT_RESPONSE_MESSAGE)
        } else {
            val choices = json.getJSONArray(CHAT_GPT_RESPONSE_CHOICES)
            val first = choices.getJSONObject(0)
            return first.getString(CHAT_GPT_RESPONSE_TEXT).trim()
        }
    }
}

