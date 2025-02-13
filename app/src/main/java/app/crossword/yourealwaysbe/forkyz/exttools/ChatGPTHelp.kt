
package app.crossword.yourealwaysbe.forkyz.exttools

import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.annotation.MainThread
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.settings.ExternalToolSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.JSONUtils
import app.crossword.yourealwaysbe.puz.Playboard

private val CHAT_GPT_API_URL = "https://api.openai.com/v1/completions"
private val CHAT_GPT_MODEL = "gpt-3.5-turbo-instruct"
private val CHAT_GPT_RESPONSE_CHOICES = "choices"
private val CHAT_GPT_RESPONSE_ERROR = "error"
private val CHAT_GPT_RESPONSE_MESSAGE = "message"
private val CHAT_GPT_RESPONSE_TEXT = "text"
private val CHAT_GPT_TEMPERATURE = 1.0
private val CHAT_GPT_MAX_TOKENS = 500
private val HTTP_OK_RESPONSE = 200
private val QUERY_TIMEOUT = 30000

private val HELP_RESPONSE_TEXT = "helpResponse"

class ChatGPTHelpData(
    val response : String,
) : ExternalToolData() {
    companion object {
        private val ioScope = CoroutineScope(Dispatchers.IO)

        /**
         * Makes request and calls back with response data
         *
         * May not call back if Chat GPT not configured or no clue to
         * ask about.
         */
        fun buildForCurrentClue(
            context : Context,
            settings : ForkyzSettings,
            board : Playboard,
            callback : Consumer<ChatGPTHelpData>,
        ) {
            settings.getExtChatGPTAPIKey() { apiKey ->
                apiKey?.let { apiKey ->
                    makeQuery(context, board)?.let { query ->
                        makeRequest(apiKey, query) { response ->
                            callback.accept(ChatGPTHelpData(response))
                        }
                    }
                }
            }
        }

        // method should become redundant when all Kotlin/Compose
        @MainThread
        @JvmStatic
        fun isEnabled(settings : ForkyzSettings, cb : Consumer<Boolean>) {
            settings.getExtChatGPTAPIKey() { apiKey ->
                cb.accept(isEnabled(apiKey))
            }
        }

        @JvmStatic // remove when all Kotlin
        fun isEnabled(extSettings : ExternalToolSettings) : Boolean
            = isEnabled(extSettings.chatGPTAPIKey)

        private fun makeRequest(
            apiKey : String,
            query : String,
            callback : Consumer<String>,
        ) {
            ioScope.launch {
                try {
                    val conn = URL(CHAT_GPT_API_URL).openConnection()
                        as HttpURLConnection
                    with (conn) {
                        setConnectTimeout(QUERY_TIMEOUT)
                        setReadTimeout(QUERY_TIMEOUT)
                        setRequestMethod("POST")
                        setRequestProperty(
                            "Content-Type", "application/json",
                        )
                        setRequestProperty(
                            "Authorization", "Bearer " + apiKey,
                        )
                        setDoOutput(true)
                    }

                    sendQueryData(conn, query)

                    callback.accept(getQueryResponse(conn))
                } catch (e : IOException) {
                    // ignore bad response?
                } catch (e : JSONException) {
                    // ignore bad response?
                }
            }
        }

        private fun sendQueryData(
            conn : HttpURLConnection,
            query : String,
        ) {
            val data = JSONObject()
            with (data) {
                put("model", CHAT_GPT_MODEL)
                put("prompt", query)
                put("max_tokens", CHAT_GPT_MAX_TOKENS)
                put("temperature", CHAT_GPT_TEMPERATURE)
            }

            BufferedOutputStream(conn.getOutputStream()).use { os ->
                val bytes = data.toString().toByteArray()
                os.write(bytes, 0, bytes.size)
            }
        }

        private fun getQueryResponse(conn : HttpURLConnection) : String {
            if (conn.getResponseCode() != HTTP_OK_RESPONSE) {
                return conn.getErrorStream().use { inputStream ->
                    val json = JSONUtils.streamToJSON(inputStream)
                    json.getJSONObject(CHAT_GPT_RESPONSE_ERROR)
                        .getString(CHAT_GPT_RESPONSE_MESSAGE)
                }
            }

            return conn.getInputStream().use { inputStream ->
                val response = JSONUtils.streamToJSON(inputStream)
                val choices = response.getJSONArray(CHAT_GPT_RESPONSE_CHOICES)
                val first = choices.getJSONObject(0)
                first.getString(CHAT_GPT_RESPONSE_TEXT).trim()
            }
        }

        private fun makeQuery(context : Context, board : Playboard) : String? {
            val blank = context.getString(R.string.share_clue_blank_box)

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
                return context.getString(
                    R.string.help_query_solution_and_response,
                    hint, response, solution
                )
            } else if (response != null) {
                return context.getString(
                    R.string.help_query_just_response,
                    hint, response
                )
            } else if (solution != null) {
                return context.getString(
                    R.string.help_query_just_solution,
                    hint, solution
                )
            } else {
                return context.getString(R.string.help_query_just_clue, clue)
            }
        }

        private fun isEnabled(apiKey : String?) : Boolean
            = apiKey != null && !apiKey.trim().isEmpty()
    }

    override fun accept(launcher : ExternalToolLauncher) {
        launcher.visit(this)
    }
}

fun ExternalToolLauncher.visit(data : ChatGPTHelpData) {
    val dialog = HelpResponseDialog()
    val args = Bundle()
    args.putString(HELP_RESPONSE_TEXT, data.response)
    dialog.setArguments(args)
    dialog.show(this.activity.getSupportFragmentManager(), "HelpResponseDialog")
}

class HelpResponseDialog() : DialogFragment() {
    override fun onCreateDialog(savedInstanceState : Bundle?) : Dialog {
        val activity = getActivity()

        var response = getArguments()?.getString(HELP_RESPONSE_TEXT)
        if (response == null)
            response = activity?.getString(R.string.help_query_failed)

        val builder = MaterialAlertDialogBuilder(activity!!)
            .setTitle(
                activity.getString(R.string.help_query_response_title)
            ).setMessage(response)
            .setPositiveButton(R.string.ok, null)

        return builder.create()
    }
}

