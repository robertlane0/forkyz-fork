
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlinx.coroutines.flow.first

import app.crossword.yourealwaysbe.forkyz.settings.ExternalToolSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.LLMAgent
import app.crossword.yourealwaysbe.forkyz.settings.LLMAgentSettings

/**
 * Base interface for LLM services that can respond to text queries
 */
interface LLMService {
    /**
     * Makes a request to the LLM service with the given query
     *
     * @param settings the settings object containing configuration
     * @param query the text query to send to the LLM
     * @return the response text, or null if the request failed
     */
    suspend fun makeRequest(
        settings : ForkyzSettings,
        query : String
    ) : String?
}

suspend fun getLLMService(settings : ForkyzSettings) : LLMService? {
    val extToolSettings = settings.liveExternalToolSettings.first()
    return getLLMService(extToolSettings)
}

fun getLLMService(settings : ExternalToolSettings) : LLMService? {
    return when (settings.llmAgentSettings.llmAgent) {
        LLMAgent.LA_CHATGPT -> ChatGPTService
        LLMAgent.LA_CLAUDE -> ClaudeService
        else -> null
    }
}

