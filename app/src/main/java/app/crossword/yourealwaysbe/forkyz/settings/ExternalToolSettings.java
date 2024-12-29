
package app.crossword.yourealwaysbe.forkyz.settings;

import app.crossword.yourealwaysbe.forkyz.tools.ChatGPTHelp;
import app.crossword.yourealwaysbe.forkyz.tools.ExternalDictionary;

public record ExternalToolSettings(
    String chatGPTAPIKey,
    boolean crosswordSolverEnabled,
    boolean duckDuckGoEnabled,
    ExternalDictionary externalDictionary,
    boolean fifteenSquaredEnabled
) {

    public boolean hasExternal() {
        return ChatGPTHelp.isEnabled(this)
            || crosswordSolverEnabled
            || duckDuckGoEnabled
            || externalDictionary != null
            || fifteenSquaredEnabled;
    }
}
