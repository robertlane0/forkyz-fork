
package app.crossword.yourealwaysbe.forkyz.exttools

import app.crossword.yourealwaysbe.forkyz.settings.ExternalToolSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings

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

    /**
     * Checks if the service is enabled based on settings
     *
     * @param settings the settings object to check
     * @return true if the service is properly configured and enabled
     */
    suspend fun isEnabled(settings : ForkyzSettings) : Boolean

    /**
     * Checks if the service is enabled based on settings
     *
     * @param settings the settings object to check
     * @return true if the service is properly configured and enabled
     */
    fun isEnabled(settings : ExternalToolSettings) : Boolean
}
