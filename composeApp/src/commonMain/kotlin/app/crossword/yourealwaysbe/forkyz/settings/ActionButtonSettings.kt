
package app.crossword.yourealwaysbe.forkyz.settings

data class ActionButtonSettings(
    val announceClue : Boolean = false,
    val voiceInput : Boolean = false,
) {
    val hasButtons : Boolean = (
        announceClue
        || voiceInput
    )
}

