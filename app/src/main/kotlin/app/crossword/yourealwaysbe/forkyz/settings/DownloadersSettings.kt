
package app.crossword.yourealwaysbe.forkyz.settings

data class DownloadersSettings(
    val downloadDeStandaard : Boolean,
    val downloadDeTelegraaf : Boolean,
    val downloadElPaisExperto : Boolean,
    val downloadGuardianDailyCryptic : Boolean,
    val downloadGuardianWeeklyQuiptic : Boolean,
    val downloadHamAbend : Boolean,
    val downloadIndependentDailyCryptic : Boolean,
    val downloadIrishNewsCryptic : Boolean,
    val downloadJonesin : Boolean,
    val downloadJoseph : Boolean,
    val download20Minutes : Boolean,
    val downloadLATimes: Boolean,
    val downloadLeParisienF1 : Boolean,
    val downloadLeParisienF2 : Boolean,
    val downloadLeParisienF3 : Boolean,
    val downloadLeParisienF4 : Boolean,
    val downloadMetroCryptic : Boolean,
    val downloadMetroQuick : Boolean,
    val downloadNewsday : Boolean,
    val downloadNewYorkTimesSyndicated : Boolean,
    val downloadNRCInHetMidden : Boolean,
    val downloadNRCScrypto : Boolean,
    val downloadPremier : Boolean,
    val downloadSheffer : Boolean,
    val downloadSudOuestMotsCroises : Boolean,
    val downloadSudOuestMotsFleches : Boolean,
    val downloadTF1MotsCroises : Boolean,
    val downloadTF1MotsFleches : Boolean,
    val downloadUniversal : Boolean,
    val downloadUSAToday : Boolean,
    val downloadWaPoSunday : Boolean,
    val downloadWsj : Boolean,
    val scrapeCru : Boolean,
    val scrapeGuardianQuick : Boolean,
    val scrapeKegler : Boolean,
    val scrapePrivateEye : Boolean,
    val scrapePrzekroj : Boolean,
    val downloadCustomDaily : Boolean,
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
}
