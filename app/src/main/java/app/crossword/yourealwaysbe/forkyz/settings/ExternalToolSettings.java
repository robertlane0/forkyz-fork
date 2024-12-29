
package app.crossword.yourealwaysbe.forkyz.settings;

import app.crossword.yourealwaysbe.forkyz.tools.ChatGPTHelp;
import app.crossword.yourealwaysbe.forkyz.tools.ExternalDictionary;

public record ExternalToolSettings(
    ExternalDictionary externalDictionary,
    String chatGPTAPIKey,
    boolean crosswordSolverEnabled,
    boolean fifteenSquaredEnabled
) {

    public boolean hasExternal() {
        return externalDictionary != null
            || ChatGPTHelp.isEnabled(this)
            || crosswordSolverEnabled
            || fifteenSquaredEnabled;
    }
}
