
package app.crossword.yourealwaysbe.forkyz.settings

import app.crossword.yourealwaysbe.forkyz.net.DownloaderID

data class DownloadersSettings(
    val disabledDownloaders : Set<String>,
    val customDailyTitle : String,
    val customDailyUrl : String,
    val suppressSummaryNotifications : Boolean,
    val suppressIndividualNotifications : Boolean,
    val autoDownloaders : Set<String>,
    val downloadTimeout : Int,
    val downloadOnStartUp : Boolean,
) {
    val isNotificationPermissionNeeded
        get() = !(
            suppressSummaryNotifications
            && suppressIndividualNotifications
        )

    fun enabled(downloaderID: DownloaderID): Boolean {
        return downloaderID.id !in disabledDownloaders
    }

    fun enabledAuto(downloaderID: DownloaderID): Boolean {
        return downloaderID.id in autoDownloaders
    }
}
