
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Set;

public record DownloadersSettings(
    boolean downloadDeStandaard,
    boolean downloadDeTelegraaf,
    boolean downloadGuardianDailyCryptic,
    boolean downloadHamAbend,
    boolean downloadIndependentDailyCryptic,
    boolean downloadJonesin,
    boolean downloadJoseph,
    boolean download20Minutes,
    boolean downloadLeParisienF1,
    boolean downloadLeParisienF2,
    boolean downloadLeParisienF3,
    boolean downloadLeParisienF4,
    boolean downloadMetroCryptic,
    boolean downloadNewsday,
    boolean downloadNewYorkTimesSyndicated,
    boolean downloadPremier,
    boolean downloadSheffer,
    boolean downloadUniversal,
    boolean downloadUSAToday,
    boolean downloadWaPoSunday,
    boolean downloadWsj,
    boolean scrapeCru,
    boolean scrapeEveryman,
    boolean scrapeGuardianQuick,
    boolean scrapeKegler,
    boolean scrapePrivateEye,
    boolean scrapePrzekroj,
    boolean downloadCustomDaily,
    String customDailyTitle,
    String customDailyUrl,
    boolean suppressSummaryNotifications,
    boolean suppressIndividualNotifications,
    Set<String> autoDownloaders,
    int downloadTimeout,
    boolean downloadOnStartup
) {
    public boolean isNotificationPermissionNeeded() {
        return !(
            suppressSummaryNotifications()
            && suppressIndividualNotifications()
        );
    }
}

