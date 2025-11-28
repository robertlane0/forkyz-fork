
package app.crossword.yourealwaysbe.forkyz.settings

data class BackgroundDownloadSettings(
    val requireUnmetered : Boolean = false,
    val allowRoaming : Boolean = false,
    val requireCharging : Boolean = false,
    val hourly : Boolean = false,
    val days : Set<String> = setOf(),
    val daysTime : Int = 8,
) { }
