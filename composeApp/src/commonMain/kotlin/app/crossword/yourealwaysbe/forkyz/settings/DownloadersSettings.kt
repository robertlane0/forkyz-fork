
package app.crossword.yourealwaysbe.forkyz.settings

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

import app.crossword.yourealwaysbe.forkyz.net.DownloaderID

data class DownloadersSettings(
    val disabledDownloaders : Set<String>,
    val customDailyTitle : String,
    val customDailyUrl : String,
    val suppressSummaryNotifications : Boolean,
    val suppressIndividualNotifications : Boolean,
    val downloadTimeout : Int,
    val downloadOnStartUp : Boolean,
    val autoDownloadersBlockMon : Set<String>,
    val autoDownloadersBlockTue : Set<String>,
    val autoDownloadersBlockWed : Set<String>,
    val autoDownloadersBlockThu : Set<String>,
    val autoDownloadersBlockFri : Set<String>,
    val autoDownloadersBlockSat : Set<String>,
    val autoDownloadersBlockSun : Set<String>,
) {
    val isNotificationPermissionNeeded
        get() = !(
            suppressSummaryNotifications
            && suppressIndividualNotifications
        )

    fun enabled(downloaderID: DownloaderID): Boolean {
        return downloaderID.id !in disabledDownloaders
    }

    fun enabledAuto(
        downloaderID: DownloaderID,
        date : LocalDate,
    ): Boolean {
        if (!enabled(downloaderID)) {
            return false
        } else {
            return when (date.dayOfWeek) {
                DayOfWeek.MONDAY
                    -> downloaderID.id !in autoDownloadersBlockMon
                DayOfWeek.TUESDAY
                    -> downloaderID.id !in autoDownloadersBlockTue
                DayOfWeek.WEDNESDAY
                    -> downloaderID.id !in autoDownloadersBlockWed
                DayOfWeek.THURSDAY
                    -> downloaderID.id !in autoDownloadersBlockThu
                DayOfWeek.FRIDAY
                    -> downloaderID.id !in autoDownloadersBlockFri
                DayOfWeek.SATURDAY
                    -> downloaderID.id !in autoDownloadersBlockSat
                DayOfWeek.SUNDAY
                    -> downloaderID.id !in autoDownloadersBlockSun
            }
        }
    }
}
