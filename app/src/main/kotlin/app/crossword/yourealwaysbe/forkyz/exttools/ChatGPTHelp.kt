
package app.crossword.yourealwaysbe.forkyz.exttools

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import android.content.Context
import android.os.Bundle
import androidx.annotation.MainThread

import org.json.JSONException
import org.json.JSONObject

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ExternalToolSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.getURLJSON
import app.crossword.yourealwaysbe.puz.Playboard

private val CHAT_GPT_API_URL = "https://api.openai.com/v1/completions"
private val CHAT_GPT_MODEL = "gpt-3.5-turbo-instruct"
private val CHAT_GPT_RESPONSE_CHOICES = "choices"
private val CHAT_GPT_RESPONSE_ERROR = "error"
private val CHAT_GPT_RESPONSE_MESSAGE = "message"
private val CHAT_GPT_RESPONSE_TEXT = "text"
private val CHAT_GPT_TEMPERATURE = 1.0
private val CHAT_GPT_MAX_TOKENS = 500
private val QUERY_TIMEOUT = 30000

/**
 * Makes request and calls back with response data
 *
 * May not call back if Chat GPT not configured or no clue to
 * ask about.
 *
 * @param callback delivers text response for OK dialog
 */
suspend fun askChatGPTForCurrentClue(
    utils : NativeBackendUtils,
    settings : ForkyzSettings,
    board : Playboard,
    callback : (String) -> Unit,
) {
    settings.liveExtChatGPTAPIKey.first()?.let { apiKey ->
        val query = makeQuery(utils, board)
        if (query == null) {
            callback(utils.getString(R.string.help_no_clue_to_query))
        } else {
            makeRequest(apiKey, query) { response ->
                if (response == null)
                    callback(utils.getString(R.string.help_query_failed))
                else
                    callback(response)
            }
        }
    }
}

// method should become redundant when all Kotlin/Compose
@MainThread
fun isChatGPTEnabled(settings : ForkyzSettings, cb : (Boolean) -> Unit) {
    settings.getExtChatGPTAPIKey() { apiKey ->
        cb(isChatGPTEnabled(apiKey))
    }
}

fun isChatGPTEnabled(extSettings : ExternalToolSettings) : Boolean
    = isChatGPTEnabled(extSettings.chatGPTAPIKey)

private suspend fun makeRequest(
    apiKey : String,
    query : String,
    callback : suspend (String?) -> Unit,
) {
    val entryContext = currentCoroutineContext()
    coroutineScope {
        launch(Dispatchers.IO) {
            try {
                val json = getURLJSON(
                    url = CHAT_GPT_API_URL,
                    timeout = QUERY_TIMEOUT,
                    headers = mapOf("Authorization" to "Bearer " + apiKey),
                    data = getQueryData(query),
                )
                launch(entryContext) {
                    callback(getQueryResponse(json))
                }
            } catch (e : IOException) {
                launch(entryContext) { callback(null) }
            } catch (e : JSONException) {
                launch(entryContext) { callback(null) }
            }
        }
    }
}

private fun getQueryData(query : String) : JSONObject {
    val data = JSONObject()
    with (data) {
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

private suspend fun makeQuery(
    utils : NativeBackendUtils,
    board : Playboard,
) : String? {
    val blank = utils.getString(R.string.share_clue_blank_box)

    val puz = board.getPuzzle()
    if (puz == null)
        return null

    val clue = board?.getClue()
    if (clue == null)
        return null

    val hint = clue.getHint()
    var response : String? = null
    var solution : String? = null

    val zone = clue.getZone()
    if (zone != null) {
        var hasResponse = false
        val responseBuilder = StringBuilder()
        var hasSolution = false
        val solutionBuilder = StringBuilder()

        for (pos in zone) {
            val box = puz.checkedGetBox(pos)
            if (box != null) {
                if (!box.isBlank()) {
                    hasResponse = true
                    responseBuilder.append(box.getResponse())
                } else {
                    responseBuilder.append(blank)
                }

                if (box.hasSolution()) {
                    hasSolution = true
                    solutionBuilder.append(box.getSolution())
                } else {
                    solutionBuilder.append(blank)
                }
            }
        }

        if (hasSolution)
            solution = solutionBuilder.toString()
        if (hasResponse)
            response = responseBuilder.toString()
    }

    if (response != null && solution != null) {
        return utils.getString(
            R.string.help_query_solution_and_response,
            hint, response, solution
        )
    } else if (response != null) {
        return utils.getString(
            R.string.help_query_just_response,
            hint, response
        )
    } else if (solution != null) {
        return utils.getString(
            R.string.help_query_just_solution,
            hint, solution
        )
    } else {
        return utils.getString(R.string.help_query_just_clue, clue)
    }
}

private fun isChatGPTEnabled(apiKey : String?) : Boolean
    = apiKey != null && !apiKey.trim().isEmpty()

