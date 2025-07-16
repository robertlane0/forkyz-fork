
package app.crossword.yourealwaysbe.forkyz.settings

import app.crossword.yourealwaysbe.forkyz.exttools.isChatGPTEnabled

data class ExternalToolSettings(
    val chatGPTAPIKey : String,
    val crosswordSolverEnabled : Boolean,
    val duckDuckGoEnabled : Boolean,
    val externalDictionary : ExternalDictionarySetting,
    val fifteenSquaredEnabled : Boolean,
) {
    val hasExternal
        get() = (
            hasChatGPT
            || crosswordSolverEnabled
            || duckDuckGoEnabled
            || hasExternalDictionary
            || fifteenSquaredEnabled
        )

    val hasChatGPT
        get() = isChatGPTEnabled(this)

    val hasExternalDictionary
        get() = externalDictionary != ExternalDictionarySetting.EDS_NONE
}
