
package app.crossword.yourealwaysbe.forkyz.settings

data class ActionButtonSettings(
    val announceClue : Boolean = false,
    val toggleScratchMode : Boolean = false,
    val toggleShowErrorsGrid : Boolean = false,
    val voiceInput : Boolean = false,
) {
    val hasButtons : Boolean = (
        announceClue
        || toggleScratchMode
        || toggleShowErrorsGrid
        || voiceInput
    )
}

