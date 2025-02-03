
package app.crossword.yourealwaysbe.forkyz.settings

class BackgroundDownloadSettings(
    val requireUnmetered : Boolean,
    val allowRoaming : Boolean,
    val requireCharging : Boolean,
    val hourly : Boolean,
    val days : Set<String>,
    val daysTime : Int,
) { }
