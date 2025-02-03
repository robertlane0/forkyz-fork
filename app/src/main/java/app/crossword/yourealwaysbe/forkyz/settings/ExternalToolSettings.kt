
package app.crossword.yourealwaysbe.forkyz.settings

import app.crossword.yourealwaysbe.forkyz.tools.ChatGPTHelp
import app.crossword.yourealwaysbe.forkyz.tools.ExternalDictionary

class ExternalToolSettings(
    val chatGPTAPIKey : String,
    val crosswordSolverEnabled : Boolean,
    val duckDuckGoEnabled : Boolean,
    val externalDictionary : ExternalDictionary?,
    val fifteenSquaredEnabled : Boolean,
) {
    val hasExternal
        get() = (
            ChatGPTHelp.isEnabled(this)
            || crosswordSolverEnabled
            || duckDuckGoEnabled
            || externalDictionary != null
            || fifteenSquaredEnabled
        )
}
