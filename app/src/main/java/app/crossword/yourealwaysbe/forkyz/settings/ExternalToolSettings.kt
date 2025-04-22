
package app.crossword.yourealwaysbe.forkyz.settings

import app.crossword.yourealwaysbe.forkyz.exttools.ChatGPTHelpData

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
        get() = ChatGPTHelpData.isEnabled(this)

    val hasExternalDictionary
        get() = externalDictionary != ExternalDictionarySetting.EDS_NONE
}
