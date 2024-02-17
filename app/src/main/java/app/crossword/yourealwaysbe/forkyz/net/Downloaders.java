package app.crossword.yourealwaysbe.forkyz.net;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import app.crossword.yourealwaysbe.forkyz.BrowseActivity;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.puz.io.BrainsOnlyIO;
import app.crossword.yourealwaysbe.puz.io.IO;
import app.crossword.yourealwaysbe.puz.io.JPZIO;
import app.crossword.yourealwaysbe.puz.io.PrzekrojIO;
import app.crossword.yourealwaysbe.puz.io.RCIJeuxMFJIO;
import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler;
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Downloaders {
    private static final Logger LOG
        = Logger.getLogger("app.crossword.yourealwaysbe");
    private static final int NUM_DOWNLOAD_THREADS = 3;

    private Context context;
    private NotificationManagerCompat notificationManager;
    private DownloadersSettings settings;
    private FileHandler fileHandler;
    private AndroidVersionUtils utils;

    /**
     * Create a downloader instance that does not notify
     */
    public Downloaders(
        Context context,
        AndroidVersionUtils utils,
        FileHandler fileHandler,
        DownloadersSettings settings
    ) {
        this(context, utils, fileHandler, settings, null);
    }

    /**
     * Create a downloader instance
     *
     * Pass a null notification manager if notifications not needed.
     * Notifications only used if not disabled in settings.
     *
     * Can pass a null fileHandler if no download methods will be called.
     */
    public Downloaders(
        Context context,
        AndroidVersionUtils utils,
        FileHandler fileHandler,
        DownloadersSettings settings,
        NotificationManagerCompat notificationManager
    ) {
        this.context = context;
        this.utils = utils;
        this.fileHandler = fileHandler;
        this.settings = settings;
        this.notificationManager = notificationManager;
    }

    public List<Downloader> getDownloaders(LocalDate date) {
        List<Downloader> retVal = new LinkedList<Downloader>();
        for (Downloader d : getDownloaders()) {
            if (d.isAvailable(date))
                retVal.add(d);
        }
        return retVal;
    }

    public void download(LocalDate date) {
        download(date, getDownloaders(date));
    }

    // Downloads the latest puzzles newer/equal to than the given date for the given set of
    // downloaders.
    //
    // If downloaders is null, then the full list of downloaders will be used.
    public void downloadLatestInRange(
        LocalDate from, LocalDate to, List<Downloader> downloaders
    ) {
        if (downloaders == null) {
            downloaders = getDownloaders();
        }

        HashMap<Downloader, LocalDate> puzzlesToDownload
            = new HashMap<Downloader, LocalDate>();
        for (Downloader d : downloaders) {
            LocalDate latestDate = d.getLatestDate(to);
            if (latestDate != null && !latestDate.isBefore(from)) {
                LOG.info(
                    "Will try to download puzzle " + d + " @ " + latestDate
                );
                puzzlesToDownload.put(d, latestDate);
            }
        }

        if (!puzzlesToDownload.isEmpty()) {
            download(puzzlesToDownload);
        }
    }

    public void download(LocalDate date, List<Downloader> downloaders) {
        if ((downloaders == null) || (downloaders.size() == 0)) {
            downloaders = getDownloaders(date);
        }

        HashMap<Downloader, LocalDate> puzzlesToDownload = new HashMap<Downloader, LocalDate>();
        for (Downloader d : downloaders) {
            if (d.isAvailable(date))
                puzzlesToDownload.put(d, date);
        }

        download(puzzlesToDownload);
    }

    public List<Downloader> getAutoDownloaders() {
        List<Downloader> available = getDownloaders();

        List<Downloader> downloaders = new LinkedList<>();
        for (Downloader downloader : available) {
            boolean isAuto = settings
                .getAutoDownloaders()
                .contains(downloader.getInternalName());
            if (isAuto)
                downloaders.add(downloader);
        }

        return downloaders;
    }

    private void download(Map<Downloader, LocalDate> puzzlesToDownload) {
        if (fileHandler == null)
            return;

        boolean hasConnection = utils.hasNetworkConnection(context);

        if (!hasConnection) {
            notifyNoConnection();
            return;
        }

        NotificationCompat.Builder not =
                new NotificationCompat.Builder(context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(context.getString(
                            R.string.puzzles_downloading
                        ))
                        .setWhen(System.currentTimeMillis());

        int nextNotificationId = 1;
        Set<String> fileNames = fileHandler.getPuzzleNames();

        ExecutorService downloadExecutor
            = Executors.newFixedThreadPool(NUM_DOWNLOAD_THREADS);
        final AtomicBoolean somethingDownloaded = new AtomicBoolean(false);
        final AtomicBoolean somethingFailed = new AtomicBoolean(false);

        for (
            Map.Entry<Downloader, LocalDate> puzzle
                : puzzlesToDownload.entrySet()
        ) {
            int notificationId = nextNotificationId++;
            Downloader downloader = puzzle.getKey();
            LocalDate date = puzzle.getValue();

            downloadExecutor.submit(() -> {
                Downloader.DownloadResult result = downloadPuzzle(
                    downloader,
                    date,
                    not,
                    notificationId,
                    fileNames
                );
                if (result.isSuccess())
                    somethingDownloaded.set(true);
                else if (result.isFailed())
                    somethingFailed.set(true);
            });
        }

        downloadExecutor.shutdown();

        try {
            downloadExecutor.awaitTermination(
                Long.MAX_VALUE, TimeUnit.MILLISECONDS
            );
        } catch (InterruptedException e) {
            // Oh well
        }

        if (this.notificationManager != null) {
            this.notificationManager.cancel(0);
        }

        if (isNotifyingSummary()) {
            this.postDownloadedGeneral(
                somethingDownloaded.get(), somethingFailed.get()
            );
        }
    }

    /**
     * Download and save the puzzle from the downloader
     *
     * Only saves if we don't already have it.
     */
    @SuppressLint("MissingPermission") // canNotify does check
    private Downloader.DownloadResult downloadPuzzle(
        Downloader d,
        LocalDate date,
        NotificationCompat.Builder not,
        int notificationId,
        Set<String> existingFileNames
    ) {
        // failed unless proven otherwise!
        Downloader.DownloadResult result = Downloader.DownloadResult.FAILED;

        if (fileHandler == null)
            return result;

        LOG.info("Downloading " + d.toString());

        try {
            String contentText = context.getString(
                R.string.puzzles_downloading_from, d.getName()
            );
            not.setContentText(contentText)
                .setContentIntent(getContentIntent());

            if (isNotifyingIndividual()) {
                this.notificationManager.notify(0, not.build());
            }

            result = d.download(date, existingFileNames);

            if (result.isSuccess()) {
                String fileName = result.getFileName();
                if (!existingFileNames.contains(fileName)) {
                    fileHandler.saveNewPuzzle(
                        result.getPuzzle(), fileName
                    );
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to download "+d.getName(), e);
        }

        if (isNotifyingIndividual()) {
            this.postDownloadedNotification(
                notificationId, d.getName(), result
            );
        }

        return result;
    }

    @SuppressLint("MissingPermission") // canNotify does check
    private void postDownloadedGeneral(
        Boolean somethingDownloaded, Boolean somethingFailed
    ) {
        int messageId;
        if (somethingDownloaded && somethingFailed)
            messageId = R.string.puzzles_downloaded_some;
        else if (somethingDownloaded)
            messageId = R.string.puzzles_downloaded_all;
        else if (somethingFailed)
            messageId = R.string.puzzles_downloaded_none;
        else // nothing downloaded or failed
            return;
        Notification not = new NotificationCompat.Builder(
            context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
        ).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(messageId))
            .setContentIntent(getContentIntent())
            .setWhen(System.currentTimeMillis())
            .build();

        if (canNotify()) {
            this.notificationManager.notify(0, not);
        }
    }

    @SuppressLint("MissingPermission") // canNotify does check
    private void postDownloadedNotification(
        int i, String name, Downloader.DownloadResult result
    ) {
        // don't notify unless success or failure
        // notifications about existing puzzles would be annoying
        if (!(result.isSuccess() || result.isFailed()))
            return;

        int messageId = result.isSuccess()
            ? R.string.puzzle_downloaded
            : R.string.puzzle_download_failed;

        Notification not
            = new NotificationCompat.Builder(
            context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
        ).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(getContentIntent())
            .setContentTitle(context.getString(messageId, name))
            .setWhen(System.currentTimeMillis())
            .build();

        if (canNotify()) {
            this.notificationManager.notify(i, not);
        }
    }

    public List<Downloader> getDownloaders() {
        List<Downloader> downloaders = new LinkedList<>();

        if (settings.getDownloadDeStandaard()) {
            downloaders.add(new KeesingDownloader(
                "destandaard",
                context.getString(R.string.de_standaard),
                Downloader.DATE_DAILY,
                Duration.ofHours(-1), // midnight, tested
                "https://aboshop.standaard.be/",
                "'https://www.standaard.be/kruiswoordraadsel'",
                "hetnieuwsbladpremium",
                "crossword_today_hetnieuwsbladpremium"
            ));
        }

        if (settings.getDownloadDeTelegraaf()) {
            downloaders.add(new AbstractDateDownloader(
                "detelegraaf",
                context.getString(R.string.de_telegraaf),
                Downloader.DATE_SATURDAY,
                Duration.ofHours(-1), // midnight, tested
                "https://www.telegraaf.nl/abonnement/telegraaf/abonnement-bestellen/",
                new BrainsOnlyIO(),
                "'https://pzzl.net/servlet/MH_kruis/netcrossword?date='"
                    + "yyMMdd",
                "'https://www.telegraaf.nl/puzzels/kruiswoord'"
            ));
        }

        if (settings.getDownloadGuardianDailyCryptic()) {
            downloaders.add(new GuardianDailyCrypticDownloader(
                "guardian", context.getString(R.string.guardian_daily)
            ));
        }

        if (settings.getDownloadHamAbend()) {
            downloaders.add(new RaetselZentraleSchwedenDownloader(
                "hamabend",
                context.getString(R.string.hamburger_abendblatt_daily),
                "hhab",
                Downloader.DATE_DAILY,
                Duration.ofHours(1), // midnight Germany (verified)
                "https://www.abendblatt.de/plus",
                "'https://www.abendblatt.de/ratgeber/wissen/"
                    + "article106560367/Spielen-Sie-hier-taeglich"
                    + "-das-kostenlose-Kreuzwortraetsel.html'"
            ));
        }

        if (settings.getDownloadIndependentDailyCryptic()) {
            downloaders.add(new AbstractDateDownloader(
                "independent",
                context.getString(R.string.independent_daily),
                Downloader.DATE_DAILY,
                Duration.ZERO, // midnight UK (actually further in advance)
                "https://www.independent.co.uk/donations",
                new JPZIO(),
                "'https://ams.cdn.arkadiumhosted.com/assets/gamesfeed/"
                    + "independent/daily-crossword/c_'yyMMdd'.xml'",
                "'https://puzzles.independent.co.uk/games/"
                    + "cryptic-crossword-independent'"
            ));
        }

        if (settings.getDownloadJonesin()) {
            downloaders.add(new AbstractDateDownloader(
                "jonesin",
                context.getString(R.string.jonesin_crosswords),
                Downloader.DATE_THURSDAY,
                Duration.ofDays(-2), // by experiment
                "https://crosswordnexus.com/jonesin/",
                new IO(),
                "'https://herbach.dnsalias.com/Jonesin/jz'yyMMdd'.puz'",
                "'https://crosswordnexus.com/solve/?"
                    + "puzzle=/downloads/jonesin/jonesin'yyMMdd'.puz'"
            ));
        }

        if (settings.getDownloadJoseph()) {
            downloaders.add(new KingDigitalDownloader(
                "Joseph",
                "Joseph",
                context.getString(R.string.joseph_crossword),
                Downloader.DATE_NO_SUNDAY,
                Duration.ofDays(-7), // more than a week by experiment
                "https://puzzles.kingdigital.com",
                "'https://www.arkadium.com/games/"
                    + "joseph-crossword-kingsfeatures/'"
            ));
        }

        if (settings.getDownload20Minutes()) {
            downloaders.add(new AbstractDateDownloader(
                "20minutes",
                context.getString(R.string.vingminutes),
                Downloader.DATE_DAILY,
                Duration.ofHours(1), // by experiment
                "https://www.20minutes.fr",
                new RCIJeuxMFJIO(),
                "'https://www.rcijeux.fr/drupal_game/20minutes/grids/'ddMMyy'.mfj'",
                "'https://www.20minutes.fr/services/mots-fleches'"
            ));
        }

        if (settings.getDownloadLeParisienF1()) {
            downloaders.add(new AbstractRCIJeuxMFJDateDownloader(
                "leparisienf1",
                context.getString((R.string.le_parisien_daily_f1)),
                Downloader.DATE_DAILY,
                Duration.ofHours(1),
                "https://abonnement.leparisien.fr",
                "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
                        + "mfleches_1_%d.mfj",
                "'https://www.leparisien.fr/jeux/mots-fleches/'",
                2536,
                LocalDate.of(2022, 6, 21),
                1
            ));
        }

        if (settings.getDownloadLeParisienF2()) {
            downloaders.add(new AbstractRCIJeuxMFJDateDownloader(
                "leparisienf2",
                context.getString((R.string.le_parisien_daily_f2)),
                Downloader.DATE_DAILY,
                Duration.ofHours(1),
                "https://abonnement.leparisien.fr",
                "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
                        + "mfleches_2_%d.mfj",
                "'https://www.leparisien.fr/jeux/mots-fleches/force-2/'",
                2536,
                LocalDate.of(2022, 6, 21),
                1
            ));
        }

        if (settings.getDownloadLeParisienF3()) {
            downloaders.add(new AbstractRCIJeuxMFJDateDownloader(
                "leparisienf3",
                context.getString((R.string.le_parisien_daily_f3)),
                Downloader.DATE_DAILY,
                Duration.ofHours(1),
                "https://abonnement.leparisien.fr",
                "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
                        + "mfleches_3_%d.mfj",
                "'https://www.leparisien.fr/jeux/mots-fleches/force-3/'",
                300,
                LocalDate.of(2023, 3, 4),
                1
            ));
        }

        if (settings.getDownloadLeParisienF4()) {
            downloaders.add(new AbstractRCIJeuxMFJDateDownloader(
                "leparisienf4",
                context.getString((R.string.le_parisien_daily_f4)),
                Downloader.DATE_DAILY,
                Duration.ofHours(1),
                "https://abonnement.leparisien.fr",
                "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
                        + "mfleches_4_%d.mfj",
                "'https://www.leparisien.fr/jeux/mots-fleches/force-4/'",
                300,
                LocalDate.of(2023, 3, 4),
                1
            ));
        }

        if (settings.getDownloadMetroCryptic()) {
            downloaders.add(new AbstractDateDownloader(
                "metrocryptic",
                context.getString(R.string.metro_cryptic),
                Downloader.DATE_WEEKDAY,
                Duration.ZERO,
                "https://metro.co.uk/",
                new PAPuzzlesStreamScraper(),
                "'https://metro.co.uk/puzzles/"
                    + "cryptic-crosswords-online-free-daily-word-puzzle/'",
                "'https://metro.co.uk/puzzles/"
                    + "cryptic-crosswords-online-free-daily-word-puzzle/'",
                ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDate()
            ));
        }

        if (settings.getDownloadNewsday()) {
            downloaders.add(new AbstractDateDownloader(
                "newsday",
                context.getString(R.string.newsday),
                Downloader.DATE_DAILY,
                Duration.ofHours(5), // midnight US? (by experiment)
                // i can't browse this site for a more specific URL
                // (GDPR)
                "https://www.newsday.com",
                new BrainsOnlyIO(),
                "'https://brainsonly.com/servlets-newsday-crossword/"
                    + "newsdaycrossword?date='yyMMdd",
                "'https://www.newsday.com'"
            ));
        }

        if (settings.getDownloadNewYorkTimesSyndicated()) {
            downloaders.add(new AbstractDateDownloader(
                "nytsyndicated",
                context.getString(R.string.new_york_times_syndicated),
                Downloader.DATE_DAILY,
                Duration.ofHours(5), // guess midnight NY
                "https://www.seattletimes.com/games-nytimes-crossword/",
                new BrainsOnlyIO(),
                "'https://nytsyn.pzzl.com/nytsyn-crossword-mh/"
                    + "nytsyncrossword?date='"
                    + "yyMMdd",
                "'https://nytsyn.pzzl.com/cwd_seattle/#/s/'"
                    + "yyMMdd",
                LocalDate.now().plusDays(-7)
            ));
        }

        if (settings.getDownloadPremier()) {
            downloaders.add(new KingDigitalDownloader(
                "Premier",
                "Premier",
                context.getString(R.string.premier_crossword),
                Downloader.DATE_SUNDAY,
                Duration.ofDays(-7), // more than a week by experiment
                "https://puzzles.kingdigital.com",
                "'https://www.arkadium.com/games/"
                    + "premier-crossword-kingsfeatures/'"
            ));
        }

        if (settings.getDownloadSheffer()) {
            downloaders.add(new KingDigitalDownloader(
                "Sheffer",
                "Sheffer",
                context.getString(R.string.sheffer_crossword),
                Downloader.DATE_NO_SUNDAY,
                Duration.ofDays(-7), // more than a week by experiment
                "https://puzzles.kingdigital.com",
                "'https://www.arkadium.com/games/"
                    + "sheffer-crossword-kingsfeatures/'"
            ));
        }

        if (settings.getDownloadUniversal()) {
            downloaders.add(new UclickDownloader(
                "universal",
                "fcx",
                context.getString(R.string.universal_crossword),
                context.getString(R.string.uclick_copyright),
                "http://www.uclick.com/client/spi/fcx/",
                Downloader.DATE_DAILY,
                Duration.ofMinutes(6 * 60 + 30), // 06:30 by experiment
                null
            ));
        }

        if (settings.getDownloadUSAToday()) {
            downloaders.add(new UclickDownloader(
                "usatoday",
                "usaon",
                context.getString(R.string.usa_today),
                context.getString(R.string.usa_today),
                "https://subscribe.usatoday.com",
                Downloader.DATE_DAILY,
                Duration.ofMinutes(6 * 60 + 30), // 06:30 by experiment
                "'https://games.usatoday.com/en/games/uclick-crossword'"
            ));
        }

        if (settings.getDownloadWaPoSunday()) {
            downloaders.add(new AbstractDateDownloader(
                "waposunday",
                context.getString(R.string.washington_post_sunday),
                Downloader.DATE_SUNDAY,
                Duration.ofHours(-1), // by experiment
                "https://subscribe.wsj.com",
                new IO(),
                "'https://herbach.dnsalias.com/Wapo/wp'yyMMdd'.puz'",
                "'https://subscribe.washingtonpost.com'"
            ));
        }

        if (settings.getDownloadWsj()) {
            downloaders.add(new AbstractDateDownloader(
                "wsj",
                context.getString(R.string.wall_street_journal),
                Downloader.DATE_NO_SUNDAY,
                Duration.ofHours(-3), // by experiment
                "https://subscribe.wsj.com",
                new IO(),
                "'https://herbach.dnsalias.com/wsj/wsj'yyMMdd'.puz'",
                "'https://www.wsj.com/news/puzzle'"
            ));
        }

        addCustomDownloaders(downloaders);

        if (settings.getScrapeCru()) {
            downloaders.add(new PageScraper.Puz(
                // certificate doesn't seem to work for me
                // "https://theworld.com/~wij/puzzles/cru/index.html",
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html",
                "crypticcru",
                context.getString(R.string.cru_puzzle_workshop),
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html"
            ));
        }

        if (settings.getScrapeKegler()) {
            downloaders.add(new PageScraper.Puz(
                "https://kegler.gitlab.io/Block_style/index.html",
                "keglar",
                context.getString(R.string.keglars_cryptics),
                "https://kegler.gitlab.io/"
            ));
        }

        if (settings.getScrapePrivateEye()) {
            downloaders.add(new PageScraper.Puz(
                "https://www.private-eye.co.uk/pictures/crossword/download/",
                "privateeye",
                context.getString(R.string.private_eye),
                "https://shop.private-eye.co.uk",
                true // download from end of page
            ));
        }

        if (settings.getScrapePrzekroj()) {
            downloaders.add(new PageScraper(
                "https://przekroj.org/krzyzowki/.*",
                new PrzekrojIO(),
                "https://przekroj.org/humor-rozmaitosci/krzyzowki/",
                "przekroj",
                context.getString(R.string.przekroj),
                "https://przekroj.pl/sklep",
                true, // share file url
                false // read top down
            ));
        }

        for (Downloader downloader : downloaders)
            downloader.setTimeout(settings.getDownloadTimeout());

        return downloaders;
    }

    private void addCustomDownloaders(List<Downloader> downloaders) {
        if (settings.getDownloadCustomDaily()) {
            String title = settings.getCustomDailyTitle();
            if (title == null || title.trim().isEmpty())
                title = context.getString(R.string.custom_daily_title);

            String urlDateFormatPattern = settings.getCustomDailyUrl();

            downloaders.add(
                new CustomDailyDownloader("custom", title, urlDateFormatPattern)
            );
        }
    }

    private boolean canNotify() {
        return notificationManager != null
            && utils.hasPostNotificationsPermission(context);
    }

    private boolean isNotifyingSummary() {
        return canNotify()
            && notificationManager.areNotificationsEnabled()
            && !settings.getSuppressSummaryNotifications();
    }

    private boolean isNotifyingIndividual() {
        return canNotify()
            && notificationManager.areNotificationsEnabled()
            && !settings.getSuppressIndividualNotifications();
    }

    @SuppressLint("MissingPermission") // canNotify does check
    private void notifyNoConnection() {
        if (!isNotifyingSummary())
            return;

        Notification not = new NotificationCompat.Builder(
            context, ForkyzApplication.PUZZLE_DOWNLOAD_CHANNEL_ID
        ).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(
                context.getString(R.string.puzzle_download_no_connection)
            )
            .setContentIntent(getContentIntent())
            .setWhen(System.currentTimeMillis())
            .build();

        if (canNotify()) {
            this.notificationManager.notify(0, not);
        }
    }

    /**
     * Make a content intent for a notification
     */
    private PendingIntent getContentIntent() {
        Intent notificationIntent = new Intent(context, BrowseActivity.class);
        return PendingIntent.getActivity(
            context, 0, notificationIntent, utils.immutablePendingIntentFlag()
        );
    }
}
