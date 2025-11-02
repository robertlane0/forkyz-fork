package app.crossword.yourealwaysbe.forkyz.net;

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
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.util.Log;

import app.crossword.yourealwaysbe.forkyz.ForkyzActivity;
import app.crossword.yourealwaysbe.forkyz.ForkyzApplication;
import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings;
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils;
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler;
import app.crossword.yourealwaysbe.puz.io.BrainsOnlyIO;
import app.crossword.yourealwaysbe.puz.io.GuardianJSONIO;
import app.crossword.yourealwaysbe.puz.io.IO;
import app.crossword.yourealwaysbe.puz.io.JPZIO;
import app.crossword.yourealwaysbe.puz.io.PMLIO;
import app.crossword.yourealwaysbe.puz.io.PrzekrojIO;
import app.crossword.yourealwaysbe.puz.io.RCIJeuxMFJIO;

public class Downloaders {
    private static final String TAG = "ForkyzDownloaders";
    private static final int NUM_DOWNLOAD_THREADS = 3;

    public static interface DownloadersListener {
        /**
         * Called if no internet connection for download
         */
        default void onNoConnection() { }

        default void onPuzzleDownloadStarted(String name) { }

        default void onPuzzleDownloadFinished(
            String name,
            Downloader.DownloadResult result
        ) { }

        default void onDownloadsFinished(
            boolean someDownloaded,
            boolean someFailed
        ) { }
    }

    // In the main download method, lock all other downloaders so that
    // the list of existing files does not change externally. I.e. two
    // separate downloaders don't cause duplicate downloads.
    private static final Semaphore puzzleDirLock = new Semaphore(1);

    private DownloadersSettings settings;
    private FileHandler fileHandler;
    private NativeBackendUtils utils;
    private DownloadersListener downloadersListener;

    /**
     * Create a downloader instance that does not notify
     */
    public Downloaders(
        NativeBackendUtils utils,
        FileHandler fileHandler,
        DownloadersSettings settings
    ) {
        this(utils, fileHandler, settings, null);
    }

    /**
     * Create a downloader instance
     *
     * Can pass a null fileHandler if no download methods will be called.
     */
    public Downloaders(
        NativeBackendUtils utils,
        FileHandler fileHandler,
        DownloadersSettings settings,
        DownloadersListener downloadersListener
    ) {
        this.utils = utils;
        this.fileHandler = fileHandler;
        this.settings = settings;
        this.downloadersListener = downloadersListener;
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
                Log.i(
                    TAG,
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
        if (downloaders == null) {
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

        boolean hasConnection = utils.hasNetworkConnection();
        if (!hasConnection) {
            if (downloadersListener != null)
                downloadersListener.onNoConnection();
            return;
        }

        try {
            acquirePuzzleDirLock();

            Set<String> fileNames = fileHandler.getPuzzleNames();

            ExecutorService downloadExecutor
                = Executors.newFixedThreadPool(NUM_DOWNLOAD_THREADS);
            final AtomicBoolean somethingDownloaded = new AtomicBoolean(false);
            final AtomicBoolean somethingFailed = new AtomicBoolean(false);

            for (
                Map.Entry<Downloader, LocalDate> puzzle
                    : puzzlesToDownload.entrySet()
            ) {
                Downloader downloader = puzzle.getKey();
                LocalDate date = puzzle.getValue();

                downloadExecutor.submit(() -> {
                    Downloader.DownloadResult result = downloadPuzzle(
                        downloader,
                        date,
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

            if (downloadersListener != null) {
                downloadersListener.onDownloadsFinished(
                    somethingDownloaded.get(),
                    somethingFailed.get()
                );
            }
        } finally {
            releasePuzzleDirLock();
        }
    }

    private void acquirePuzzleDirLock() {
        try {
            puzzleDirLock.acquire();
        } catch (InterruptedException e) {
            // oh well
        }
    }

    private void releasePuzzleDirLock() {
        puzzleDirLock.release();
    }

    /**
     * Download and save the puzzle from the downloader
     *
     * Only saves if we don't already have it. Only call if
     * puzzleDirLock is acquired (so existingFileNames doesn't expire).
     */
    private Downloader.DownloadResult downloadPuzzle(
        Downloader d,
        LocalDate date,
        Set<String> existingFileNames
    ) {
        // failed unless proven otherwise!
        Downloader.DownloadResult result = Downloader.DownloadResult.FAILED;

        if (fileHandler == null)
            return result;

        Log.i(TAG, "Downloading " + d.toString());
        if (downloadersListener != null)
            downloadersListener.onPuzzleDownloadStarted(d.getName());

        try {
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
            Log.w(TAG, "Failed to download "+d.getName(), e);
        }

        if (downloadersListener != null) {
            downloadersListener.onPuzzleDownloadFinished(
                d.getName(),
                result
            );
        }

        return result;
    }

    public List<Downloader> getDownloaders() {
        List<Downloader> downloaders = new LinkedList<>();

        if (settings.getDownloadDeStandaard()) {
            downloaders.add(new KeesingDownloader(
                "destandaard",
                utils.getString(R.string.de_standaard),
                AbstractDateDownloader.DAYS_DAILY,
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
                utils.getString(R.string.de_telegraaf),
                AbstractDateDownloader.DAYS_SATURDAY,
                Duration.ofHours(-1), // midnight, tested
                "https://www.telegraaf.nl/abonnement/telegraaf/abonnement-bestellen/",
                new BrainsOnlyIO(),
                "'https://pzzl.net/servlet/MH_kruis/netcrossword?date='"
                    + "yyMMdd",
                "'https://www.telegraaf.nl/puzzels/kruiswoord'"
            ));
        }

        if (settings.getDownloadElPaisExperto()) {
            downloaders.add(new ElPaisExpertoDownloader(
                "elpaisexperto",
                utils.getString(R.string.elpais_experto)
            ));
        }

        if (settings.getDownloadGuardianDailyCryptic()) {
            downloaders.add(new GuardianDailyCrypticDownloader(
                "guardian", utils.getString(R.string.guardian_daily)
            ));
        }

        if (settings.getDownloadGuardianWeeklyQuiptic()) {
            downloaders.add(new GuardianWeeklyQuipticDownloader(
                "guardianQuiptic", utils.getString(R.string.guardian_quiptic)
            ));
        }

        if (settings.getDownloadHamAbend()) {
            downloaders.add(new RaetselZentraleSchwedenDownloader(
                "hamabend",
                utils.getString(R.string.hamburger_abendblatt_daily),
                "hhab",
                AbstractDateDownloader.DAYS_DAILY,
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
                utils.getString(R.string.independent_daily),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ZERO, // midnight UK (actually further in advance)
                "https://www.independent.co.uk/donations",
                new JPZIO(),
                "'https://ams.cdn.arkadiumhosted.com/assets/gamesfeed/"
                    + "independent/daily-crossword/c_'yyMMdd'.xml'",
                "'https://puzzles.independent.co.uk/games/"
                    + "cryptic-crossword-independent'"
            ));
        }

        if (settings.getDownloadIrishNewsCryptic()) {
            downloaders.add(new AbstractDateDownloader(
                "irishnewscryptic",
                utils.getString(R.string.irish_news_cryptic),
                AbstractDateDownloader.DAYS_WEEKDAY,
                Duration.ZERO,
                "https://www.irishnews.com/",
                new PAPuzzlesStreamScraper(),
                "'https://www.irishnews.com/puzzles/cryptic-crossword/'",
                "'https://www.irishnews.com/puzzles/cryptic-crossword/'",
                ZonedDateTime.now(ZoneId.of("Europe/Belfast")).toLocalDate()
             ));
        }

        if (settings.getDownloadJonesin()) {
            downloaders.add(new AbstractDateDownloader(
                "jonesin",
                utils.getString(R.string.jonesin_crosswords),
                AbstractDateDownloader.DAYS_THURSDAY,
                Duration.ofDays(-2), // by experiment
                "https://crosswordnexus.com/jonesin/",
                new IO(),
                "'https://herbach.dnsalias.com/Jonesin/jz'yyMMdd'.puz'",
                "'https://crosswordnexus.com/solve/?"
                    + "puzzle=/downloads/jonesin/jonesin'yyMMdd'.puz'"
            ));
        }

        if (settings.getDownloadJoseph()) {
            String arkadiumUrl = "https://www.arkadium.com/games/"
                + "joseph-crossword-kingsfeatures/";
            downloaders.add(new KingDigitalLoginDownloader(
                "Joseph",
                "Joseph",
                utils.getString(R.string.joseph_crossword),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ofDays(-20), // by experiment
                "https://puzzles.kingdigital.com",
                arkadiumUrl,
                arkadiumUrl,
                "KFS+Arkadium$",
                "Puzzles&Games",
                "",
                "JCC01"
            ));
        }

        if (settings.getDownload20Minutes()) {
            downloaders.add(new AbstractDateDownloader(
                "20minutes",
                utils.getString(R.string.vingminutes),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1), // by experiment
                "https://www.20minutes.fr",
                new RCIJeuxMFJIO(),
                "'https://www.rcijeux.fr/drupal_game/20minutes/grids/'ddMMyy'.mfj'",
                "'https://www.20minutes.fr/services/mots-fleches'"
            ));
        }

        if (settings.getDownloadLATimes()) {
            downloaders.add(new LATimesDownloader(
                "latimes",
                utils.getString(R.string.la_times)
            ));
        }

        if (settings.getDownloadLeParisienF1()) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "leparisienf1",
                utils.getString((R.string.le_parisien_daily_f1)),
                AbstractDateDownloader.DAYS_DAILY,
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
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "leparisienf2",
                utils.getString((R.string.le_parisien_daily_f2)),
                AbstractDateDownloader.DAYS_DAILY,
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
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "leparisienf3",
                utils.getString((R.string.le_parisien_daily_f3)),
                AbstractDateDownloader.DAYS_DAILY,
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
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "leparisienf4",
                utils.getString((R.string.le_parisien_daily_f4)),
                AbstractDateDownloader.DAYS_DAILY,
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
                utils.getString(R.string.metro_cryptic),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ZERO,
                "https://metro.co.uk/",
                new PMLIO(),
                "'https://metro.co.uk/puzzles/cryptic-crossword'",
                "'https://metro.co.uk/puzzles/cryptic-crossword'",
                ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDate()
            ));
        }

        if (settings.getDownloadMetroQuick()) {
            downloaders.add(new AbstractDateDownloader(
                "metroquick",
                utils.getString(R.string.metro_quick),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ZERO,
                "https://metro.co.uk/",
                new PMLIO(),
                "'https://metro.co.uk/puzzles/quick-crossword'",
                "'https://metro.co.uk/puzzles/quick-crossword'",
                ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDate()
            ));
        }

        if (settings.getDownloadNewsday()) {
            downloaders.add(new AbstractDateDownloader(
                "newsday",
                utils.getString(R.string.newsday),
                AbstractDateDownloader.DAYS_DAILY,
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
                utils.getString(R.string.new_york_times_syndicated),
                AbstractDateDownloader.DAYS_DAILY,
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
            String arkadiumUrl = "https://www.arkadium.com/games/"
                + "premier-crossword-kingsfeatures/";
            downloaders.add(new KingDigitalLoginDownloader(
                "Premier",
                "Premier",
                utils.getString(R.string.premier_crossword),
                AbstractDateDownloader.DAYS_SUNDAY,
                Duration.ofHours(0), // guess midnight NY
                "https://puzzles.kingdigital.com",
                arkadiumUrl,
                arkadiumUrl,
                "KFS+Arkadium$",
                "Puzzles&Games",
                arkadiumUrl,
                "PCC01"
            ));
        }

        if (settings.getDownloadSheffer()) {
            String arkadiumUrl = "'https://www.arkadium.com/games/"
                + "sheffer-crossword-kingsfeatures/'";
            downloaders.add(new KingDigitalLoginDownloader(
                "Sheffer",
                "Sheffer",
                utils.getString(R.string.sheffer_crossword),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ofDays(-20), // from full puzzle list during login
                "https://puzzles.kingdigital.com",
                arkadiumUrl,
                arkadiumUrl,
                "KFS+Arkadium$",
                "Puzzles&Games",
                "",
                "SHCC01"
            ));
        }

        if (settings.getDownloadSudOuestMotsCroises()) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMCJ(
                "sudouestmotcroises",
                utils.getString((R.string.sud_ouest_mots_croises)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://abonnement.sudouest.fr/",
                "https://www.rcijeux.fr/drupal_game/gso/mcroises/grids/%d.mcj",
                "'https://www.sudouest.fr/jeux/mots-croises/'",
                1992,
                LocalDate.of(2025, 10, 26),
                1
            ));
        }

        if (settings.getDownloadSudOuestMotsFleches()) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "sudouestmotfleches",
                utils.getString((R.string.sud_ouest_mots_fleches)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://abonnement.sudouest.fr/",
                "https://www.rcijeux.fr/drupal_game/gso/mfleches/grids/%d.mfj",
                "'https://www.sudouest.fr/jeux/mots-fleches/'",
                1988,
                LocalDate.of(2025, 10, 22),
                1
            ));
        }

        if (settings.getDownloadTF1MotsCroises()) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMCJ(
                "tf1motcroises",
                utils.getString((R.string.tf1_mots_croises)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://www.tf1info.fr/",
                "https://www.rcijeux.fr/drupal_game/lci/mcroises/grids/%d.mcj",
                "'https://www.tf1info.fr/jeux/mots-croises/'",
                4739,
                LocalDate.of(2025, 10, 26),
                1
            ));
        }

        if (settings.getDownloadTF1MotsFleches()) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "tf1motfleches",
                utils.getString((R.string.tf1_mots_fleches)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://www.tf1info.fr/",
                "https://www.rcijeux.fr/drupal_game/lci/mfleches/grids/%d.mfj",
                "'https://www.tf1info.fr/jeux/mots-fleches/'",
                5395,
                LocalDate.of(2025, 10, 26),
                1
            ));
        }

        if (settings.getDownloadUniversal()) {
            downloaders.add(new UclickDownloader(
                "universal",
                "fcx",
                utils.getString(R.string.universal_crossword),
                utils.getString(R.string.uclick_copyright),
                "http://www.uclick.com/client/spi/fcx/",
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofMinutes(6 * 60 + 30), // 06:30 by experiment
                null
            ));
        }

        if (settings.getDownloadUSAToday()) {
            downloaders.add(new UclickDownloader(
                "usatoday",
                "usaon",
                utils.getString(R.string.usa_today),
                utils.getString(R.string.usa_today),
                "https://subscribe.usatoday.com",
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofMinutes(6 * 60 + 30), // 06:30 by experiment
                "'https://games.usatoday.com/en/games/uclick-crossword'"
            ));
        }

        if (settings.getDownloadWaPoSunday()) {
            downloaders.add(new AbstractDateDownloader(
                "waposunday",
                utils.getString(R.string.washington_post_sunday),
                AbstractDateDownloader.DAYS_SUNDAY,
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
                utils.getString(R.string.wall_street_journal),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
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
                utils.getString(R.string.cru_puzzle_workshop),
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html"
            ));
        }

        if (settings.getScrapeKegler()) {
            downloaders.add(new PageScraper.Puz(
                "https://kegler.gitlab.io/Block_style/index.html",
                "keglar",
                utils.getString(R.string.keglars_cryptics),
                "https://kegler.gitlab.io/"
            ));
        }

        if (settings.getScrapePrivateEye()) {
            downloaders.add(new PageScraper.Puz(
                "https://www.private-eye.co.uk/pictures/crossword/download/",
                "privateeye",
                utils.getString(R.string.private_eye),
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
                utils.getString(R.string.przekroj),
                "https://przekroj.pl/sklep",
                true, // share file url
                false // read top down
            ));
        }

        if (settings.getScrapeGuardianQuick()) {
            downloaders.add(new PageScraper(
                "https://www.theguardian.com/crosswords/quick/\\d*",
                new GuardianJSONIO(),
                "https://www.theguardian.com/crosswords/series/quick",
                "guardianQuick",
                utils.getString(R.string.guardian_quick),
                "https://support.theguardian.com",
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
                title = utils.getString(R.string.custom_daily_title);

            String urlDateFormatPattern = settings.getCustomDailyUrl();

            if (
                urlDateFormatPattern != null
                && !urlDateFormatPattern.trim().isEmpty()
            ) {
                downloaders.add(
                    new CustomDailyDownloader(
                        "custom", title, urlDateFormatPattern
                    )
                );
            }
        }
    }
}
