
package app.crossword.yourealwaysbe.forkyz.settings

data class LLMAgentSettings(
    val llmAgent : LLMAgent,
    val apiKey : String,
) {
    val isEnabled : Boolean = llmAgent != LLMAgent.LA_NONE
}

data class ExternalToolSettings(
    val llmAgentSettings : LLMAgentSettings,
    val crosswordSolverEnabled : Boolean,
    val duckDuckGoEnabled : Boolean,
    val externalDictionary : ExternalDictionarySetting,
    val fifteenSquaredEnabled : Boolean,
) {
    val hasExternal
        get() = (
            hasLLMAgent
            || crosswordSolverEnabled
            || duckDuckGoEnabled
            || hasExternalDictionary
            || fifteenSquaredEnabled
        )

    val hasLLMAgent
        get() = llmAgentSettings.isEnabled

    val hasExternalDictionary
        get() = externalDictionary != ExternalDictionarySetting.EDS_NONE
}
