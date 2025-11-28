
package app.crossword.yourealwaysbe.forkyz.settings

data class RenderSettings(
    val displayScratch : Boolean = false,
    val suppressHintHighlighting : Boolean = false,
    val displaySeparators : DisplaySeparators = DisplaySeparators.DS_NEVER,
    val inferSeparators : Boolean = false,
)
