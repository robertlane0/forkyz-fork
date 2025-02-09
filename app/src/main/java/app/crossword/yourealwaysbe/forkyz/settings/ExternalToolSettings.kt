
package app.crossword.yourealwaysbe.forkyz.settings

import app.crossword.yourealwaysbe.forkyz.tools.ChatGPTHelp

class ExternalToolSettings(
    val chatGPTAPIKey : String,
    val crosswordSolverEnabled : Boolean,
    val duckDuckGoEnabled : Boolean,
    val externalDictionary : ExternalDictionarySetting,
    val fifteenSquaredEnabled : Boolean,
) {
    val hasExternal
        get() = (
            ChatGPTHelp.isEnabled(this)
            || crosswordSolverEnabled
            || duckDuckGoEnabled
            || hasExternalDictionary
            || fifteenSquaredEnabled
        )

    val hasExternalDictionary
        get() = externalDictionary != ExternalDictionarySetting.EDS_NONE
}
