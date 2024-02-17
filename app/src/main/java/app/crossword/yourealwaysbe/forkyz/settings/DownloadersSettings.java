
package app.crossword.yourealwaysbe.forkyz.settings;

import java.util.Set;

// TODO: convert to record class when possible!
// https://issuetracker.google.com/issues/197081367
public class DownloadersSettings {
    private boolean downloadDeStandaard;
    private boolean downloadDeTelegraaf;
    private boolean downloadGuardianDailyCryptic;
    private boolean downloadHamAbend;
    private boolean downloadIndependentDailyCryptic;
    private boolean downloadJonesin;
    private boolean downloadJoseph;
    private boolean download20Minutes;
    private boolean downloadLeParisienF1;
    private boolean downloadLeParisienF2;
    private boolean downloadLeParisienF3;
    private boolean downloadLeParisienF4;
    private boolean downloadMetroCryptic;
    private boolean downloadNewsday;
    private boolean downloadNewYorkTimesSyndicated;
    private boolean downloadPremier;
    private boolean downloadSheffer;
    private boolean downloadUniversal;
    private boolean downloadUSAToday;
    private boolean downloadWaPoSunday;
    private boolean downloadWsj;
    private boolean scrapeCru;
    private boolean scrapeKegler;
    private boolean scrapePrivateEye;
    private boolean scrapePrzekroj;
    private boolean downloadCustomDaily;
    private String customDailyTitle;
    private String customDailyUrl;
    private boolean suppressSummaryNotifications;
    private boolean suppressIndividualNotifications;
    private Set<String> autoDownloaders;
    private int downloadTimeout;
    private boolean downloadOnStartup;

    public DownloadersSettings(
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
        this.downloadDeStandaard = downloadDeStandaard;
        this.downloadDeTelegraaf = downloadDeTelegraaf;
        this.downloadGuardianDailyCryptic = downloadGuardianDailyCryptic;
        this.downloadHamAbend = downloadHamAbend;
        this.downloadIndependentDailyCryptic
            = downloadIndependentDailyCryptic;
        this.downloadJonesin = downloadJonesin;
        this.downloadJoseph = downloadJoseph;
        this.download20Minutes = download20Minutes;
        this.downloadLeParisienF1 = downloadLeParisienF1;
        this.downloadLeParisienF2 = downloadLeParisienF2;
        this.downloadLeParisienF3 = downloadLeParisienF3;
        this.downloadLeParisienF4 = downloadLeParisienF4;
        this.downloadMetroCryptic = downloadMetroCryptic;
        this.downloadNewYorkTimesSyndicated = downloadNewYorkTimesSyndicated;
        this.downloadNewsday = downloadNewsday;
        this.downloadPremier = downloadPremier;
        this.downloadSheffer = downloadSheffer;
        this.downloadUniversal = downloadUniversal;
        this.downloadUSAToday = downloadUSAToday;
        this.downloadWaPoSunday = downloadWaPoSunday;
        this.downloadWsj = downloadWsj;
        this.scrapeCru = scrapeCru;
        this.scrapeKegler = scrapeKegler;
        this.scrapePrivateEye = scrapePrivateEye;
        this.scrapePrzekroj = scrapePrzekroj;
        this.downloadCustomDaily = downloadCustomDaily;
        this.customDailyTitle = customDailyTitle;
        this.customDailyUrl = customDailyUrl;
        this.suppressSummaryNotifications = suppressSummaryNotifications;
        this.suppressIndividualNotifications
            = suppressIndividualNotifications;
        this.autoDownloaders = autoDownloaders;
        this.downloadTimeout = downloadTimeout;
        this.downloadOnStartup = downloadOnStartup;
    }

    public boolean getDownloadDeStandaard() { return downloadDeStandaard; }
    public boolean getDownloadDeTelegraaf() { return downloadDeTelegraaf; }
    public boolean getDownloadGuardianDailyCryptic() {
        return downloadGuardianDailyCryptic;
    }
    public boolean getDownloadHamAbend() { return downloadHamAbend; }
    public boolean getDownloadIndependentDailyCryptic() {
        return downloadIndependentDailyCryptic;
    }
    public boolean getDownloadJonesin() { return downloadJonesin; }
    public boolean getDownloadJoseph() { return downloadJoseph; }
    public boolean getDownload20Minutes() { return download20Minutes; }
    public boolean getDownloadLeParisienF1() {
        return downloadLeParisienF1;
    }
    public boolean getDownloadLeParisienF2() {
        return downloadLeParisienF2;
    }
    public boolean getDownloadLeParisienF3() {
        return downloadLeParisienF3;
    }
    public boolean getDownloadLeParisienF4() {
        return downloadLeParisienF4;
    }
    public boolean getDownloadMetroCryptic() { return downloadMetroCryptic; }
    public boolean getDownloadNewsday() { return downloadNewsday; }
    public boolean getDownloadNewYorkTimesSyndicated() {
        return downloadNewYorkTimesSyndicated;
    }
    public boolean getDownloadPremier() { return downloadPremier; }
    public boolean getDownloadSheffer() { return downloadSheffer; }
    public boolean getDownloadUniversal() { return downloadUniversal; }
    public boolean getDownloadUSAToday() { return downloadUSAToday; }
    public boolean getDownloadWaPoSunday() { return downloadWaPoSunday; }
    public boolean getDownloadWsj() { return downloadWsj; }
    public boolean getScrapeCru() { return scrapeCru; }
    public boolean getScrapeKegler() { return scrapeKegler; }
    public boolean getScrapePrivateEye() { return scrapePrivateEye; }
    public boolean getScrapePrzekroj() { return scrapePrzekroj; }
    public boolean getDownloadCustomDaily() { return downloadCustomDaily; }
    public String getCustomDailyTitle() { return customDailyTitle; }
    public String getCustomDailyUrl() { return customDailyUrl; }
    public boolean getSuppressSummaryNotifications() {
        return suppressSummaryNotifications;
    }
    public boolean getSuppressIndividualNotifications() {
        return suppressIndividualNotifications;
    }
    public Set<String> getAutoDownloaders() { return autoDownloaders; }
    public int getDownloadTimeout() { return downloadTimeout; }
    public boolean getDownloadOnStartup() { return downloadOnStartup; }

    public boolean isNotificationPermissionNeeded() {
        return !(
            getSuppressSummaryNotifications()
            && getSuppressIndividualNotifications()
        );
    }
}

