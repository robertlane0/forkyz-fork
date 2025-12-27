
package app.crossword.yourealwaysbe.forkyz.net

import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cru_puzzle_workshop
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.custom_daily_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.de_standaard
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.de_telegraaf
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.elpais_experto
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_quick
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.guardian_quiptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.hamburger_abendblatt_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.independent_daily
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.irish_news_cryptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.jonesin_crosswords
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.joseph_crossword
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keglars_cryptics
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.la_croix
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.la_times
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f1
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f2
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f3
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.le_parisien_daily_f4
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.metro_cryptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.metro_quick
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.new_york_times_syndicated
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.newsday
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.nrc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.nrc_in_het_midden
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.nrc_in_het_midden_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.nrc_scrypto
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.nrc_scrypto_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.premier_crossword
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.private_eye
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.przekroj
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.sheffer_crossword
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.sud_ouest_mots_croises
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.sud_ouest_mots_fleches
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.tf1_mots_croises
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.tf1_mots_fleches
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.uclick_copyright
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.universal_crossword
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.usa_today
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.vingminutes
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.wall_street_journal
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.washington_post_sunday
import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.puz.io.BrainsOnlyIO
import app.crossword.yourealwaysbe.puz.io.FortyTwoPuzzlesIO
import app.crossword.yourealwaysbe.puz.io.GuardianJSONIO
import app.crossword.yourealwaysbe.puz.io.IO
import app.crossword.yourealwaysbe.puz.io.JPZIO
import app.crossword.yourealwaysbe.puz.io.NRCInHetMiddenIO
import app.crossword.yourealwaysbe.puz.io.PMLIO
import app.crossword.yourealwaysbe.puz.io.PrzekrojIO
import app.crossword.yourealwaysbe.puz.io.RCIJeuxMFJIO

private val TAG = "ForkyzDownloaders"
private val NUM_DOWNLOAD_THREADS = 3

interface DownloadersListener {
    /**
     * Called if no internet connection for download
     */
    suspend fun onNoConnection() { }

    suspend fun onPuzzleDownloadStarted(name : String) { }

    suspend fun onPuzzleDownloadFinished(
        name : String,
        result : Downloader.DownloadResult,
    ) { }

    suspend fun onDownloadsFinished(
        someDownloaded : Boolean,
        someFailed : Boolean,
    ) { }
}

class Downloaders(
    private val utils : NativeBackendUtils,
    private val fileHandler : FileHandler,
    private val settings : DownloadersSettings,
    private val downloadersListener : DownloadersListener? = null,
) {
    // In the main download method, lock all other downloaders so that
    // the list of existing files does not change externally. I.e. two
    // separate downloaders don't cause duplicate downloads.
    private val puzzleDirLock = Semaphore(1)

    suspend fun getDownloaders(date : LocalDate) : List<Downloader> {
        return getDownloaders().filter { it.isAvailable(date) }
    }

    suspend fun download(date : LocalDate) {
        download(date, getDownloaders(date))
    }

    // Downloads the latest puzzles newer/equal to than the given date
    // for the given set of downloaders.
    //
    // If downloaders is null, then the full list of downloaders will be
    // used.
    suspend fun downloadLatestInRange(
        from : LocalDate,
        to : LocalDate,
        givenDownloaders : List<Downloader>?,
    ) {
        val downloaders = givenDownloaders ?: getDownloaders()

        val puzzlesToDownload = HashMap<Downloader, LocalDate>()
        for (d in downloaders) {
            val latestDate = d.getLatestDate(to)
            if (latestDate != null && !latestDate.isBefore(from)) {
                AppLogger.i(
                    TAG,
                    "Will try to download puzzle " + d + " @ " + latestDate
                )
                puzzlesToDownload.put(d, latestDate)
            }
        }

        if (!puzzlesToDownload.isEmpty()) {
            download(puzzlesToDownload)
        }
    }

    suspend fun download(
        date : LocalDate,
        givenDownloaders : List<Downloader>?,
    ) {
        AppLogger.i(TAG, "Download $date and $givenDownloaders")
        val downloaders = givenDownloaders ?: getDownloaders(date)

        val puzzlesToDownload = downloaders.filter {
            it.isAvailable(date)
        }.map { it to date }.toMap()

        download(puzzlesToDownload)
    }

    suspend fun getAutoDownloaders() : List<Downloader> {
        return getDownloaders().filter {
            settings.autoDownloaders.contains(it.internalName)
        }
    }

    private suspend fun download(
        puzzlesToDownload : Map<Downloader, LocalDate>,
    ) {
        if (!utils.hasNetworkConnection()) {
            if (downloadersListener != null)
                downloadersListener.onNoConnection()
            return
        }

        try {
            acquirePuzzleDirLock()

            val fileNames = fileHandler.getPuzzleNames()

            val downloadExecutor
                = Executors.newFixedThreadPool(NUM_DOWNLOAD_THREADS)
            val somethingDownloaded = AtomicBoolean(false)
            val somethingFailed = AtomicBoolean(false)

            puzzlesToDownload.forEach { puzzle ->
                val downloader = puzzle.key
                val date = puzzle.value
                downloadExecutor.submit {
                    val result = downloadPuzzle(
                        downloader,
                        date,
                        fileNames,
                    )
                    if (result.isSuccess)
                        somethingDownloaded.set(true)
                    else if (result.isFailed)
                        somethingFailed.set(true)
                }
            }

            downloadExecutor.shutdown()

            try {
                downloadExecutor.awaitTermination(
                    Long.MAX_VALUE, TimeUnit.MILLISECONDS
                )
            } catch (e : InterruptedException) {
                // Oh well
            }

            if (downloadersListener != null) {
                downloadersListener.onDownloadsFinished(
                    somethingDownloaded.get(),
                    somethingFailed.get()
                )
            }
        } finally {
            releasePuzzleDirLock()
        }
    }

    private fun acquirePuzzleDirLock() {
        try {
            puzzleDirLock.acquire()
        } catch (e : InterruptedException) {
            // oh well
        }
    }

    private fun releasePuzzleDirLock() {
        puzzleDirLock.release()
    }

    /**
     * Download and save the puzzle from the downloader
     *
     * Only saves if we don't already have it. Only call if
     * puzzleDirLock is acquired (so existingFileNames doesn't expire).
     */
    private fun downloadPuzzle(
        d : Downloader,
        date : LocalDate,
        existingFileNames : Set<String>,
    ) : Downloader.DownloadResult {
        // failed unless proven otherwise!
        var result = Downloader.DownloadResult.FAILED

        AppLogger.i(TAG, "Downloading " + d.toString())
        runBlocking {
            downloadersListener?.onPuzzleDownloadStarted(d.name)
        }

        try {
            result = d.download(date, existingFileNames)

            if (result.isSuccess) {
                result.fileName?.let { fileName ->
                    if (!existingFileNames.contains(fileName)) {
                        result.puzzle?.let { puz ->
                            fileHandler.saveNewPuzzle(
                                puz,
                                fileName,
                            )
                        }
                    }
                }
            }
        } catch (e : Exception) {
            AppLogger.w(TAG, "Failed to download "+d.name, e)
        }

        runBlocking {
            downloadersListener?.onPuzzleDownloadFinished(
                d.name,
                result,
            )
        }

        return result
    }

    suspend fun getDownloaders() : List<Downloader> {
        val downloaders : MutableList<Downloader> = mutableListOf()

        if (settings.downloadDeStandaard) {
            downloaders.add(KeesingDownloader(
                "destandaard",
                utils.getString(Res.string.de_standaard),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(-1), // midnight, tested
                "https://aboshop.standaard.be/",
                "'https://www.standaard.be/kruiswoordraadsel'",
                "hetnieuwsbladpremium",
                "crossword_today_hetnieuwsbladpremium"
            ))
        }

        if (settings.downloadDeTelegraaf) {
            downloaders.add(AbstractDateDownloader(
                "detelegraaf",
                utils.getString(Res.string.de_telegraaf),
                AbstractDateDownloader.DAYS_SATURDAY,
                Duration.ofHours(-1), // midnight, tested
                "https://www.telegraaf.nl/abonnement/telegraaf/abonnement-bestellen/",
                BrainsOnlyIO(),
                "'https://pzzl.net/servlet/MH_kruis/netcrossword?date='"
                    + "yyMMdd",
                "'https://www.telegraaf.nl/puzzels/kruiswoord'",
            ))
        }

        if (settings.downloadElPaisExperto) {
            downloaders.add(ElPaisExpertoDownloader(
                "elpaisexperto",
                utils.getString(Res.string.elpais_experto),
            ))
        }

        if (settings.downloadGuardianDailyCryptic) {
            downloaders.add(GuardianDailyCrypticDownloader(
                "guardian",
                utils.getString(Res.string.guardian_daily),
            ))
        }

        if (settings.downloadGuardianWeeklyQuiptic) {
            downloaders.add(GuardianWeeklyQuipticDownloader(
                "guardianQuiptic",
                utils.getString(Res.string.guardian_quiptic),
            ))
        }

        if (settings.downloadHamAbend) {
            downloaders.add(RaetselZentraleSchwedenDownloader(
                "hamabend",
                utils.getString(Res.string.hamburger_abendblatt_daily),
                "hhab",
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1), // midnight Germany (verified)
                "https://www.abendblatt.de/plus",
                "'https://www.abendblatt.de/ratgeber/wissen/"
                    + "article106560367/Spielen-Sie-hier-taeglich"
                    + "-das-kostenlose-Kreuzwortraetsel.html'",
            ))
        }

        if (settings.downloadIndependentDailyCryptic) {
            downloaders.add(AbstractDateDownloader(
                "independent",
                utils.getString(Res.string.independent_daily),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ZERO, // midnight UK (actually further in advance)
                "https://www.independent.co.uk/donations",
                JPZIO(),
                "'https://ams.cdn.arkadiumhosted.com/assets/gamesfeed/"
                    + "independent/daily-crossword/c_'yyMMdd'.xml'",
                "'https://puzzles.independent.co.uk/games/"
                    + "cryptic-crossword-independent'",
            ))
        }

        if (settings.downloadIrishNewsCryptic) {
            downloaders.add(AbstractDateDownloader(
                "irishnewscryptic",
                utils.getString(Res.string.irish_news_cryptic),
                AbstractDateDownloader.DAYS_WEEKDAY,
                Duration.ZERO,
                "https://www.irishnews.com/",
                PAPuzzlesStreamScraper(),
                "'https://www.irishnews.com/puzzles/cryptic-crossword/'",
                "'https://www.irishnews.com/puzzles/cryptic-crossword/'",
                ZonedDateTime.now(ZoneId.of("Europe/Belfast")).toLocalDate(),
             ))
        }

        if (settings.downloadJonesin) {
            downloaders.add(AbstractDateDownloader(
                "jonesin",
                utils.getString(Res.string.jonesin_crosswords),
                AbstractDateDownloader.DAYS_THURSDAY,
                Duration.ofDays(-2), // by experiment
                "https://crosswordnexus.com/jonesin/",
                IO(),
                "'https://herbach.dnsalias.com/Jonesin/jz'yyMMdd'.puz'",
                "'https://crosswordnexus.com/solve/?"
                    + "puzzle=/downloads/jonesin/jonesin'yyMMdd'.puz'",
            ))
        }

        if (settings.downloadJoseph) {
            val arkadiumUrl = "https://www.arkadium.com/games/" +
                "joseph-crossword-kingsfeatures/"
            downloaders.add(KingDigitalLoginDownloader(
                "Joseph",
                "Joseph",
                utils.getString(Res.string.joseph_crossword),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ofDays(-20), // by experiment
                "https://puzzles.kingdigital.com",
                arkadiumUrl,
                arkadiumUrl,
                "KFS+Arkadium$",
                "Puzzles&Games",
                "",
                "JCC01",
            ))
        }

        if (settings.download20Minutes) {
            downloaders.add(AbstractDateDownloader(
                "20minutes",
                utils.getString(Res.string.vingminutes),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1), // by experiment
                "https://www.20minutes.fr",
                RCIJeuxMFJIO(),
                "'https://www.rcijeux.fr/drupal_game/20minutes/grids/'ddMMyy'.mfj'",
                "'https://www.20minutes.fr/services/mots-fleches'",
            ))
        }

        if (settings.downloadLaCroix) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMCJ(
                "lacroix",
                utils.getString(Res.string.la_croix),
                AbstractDateDownloader.DAYS_WEEKDAY,
                Duration.ofHours(1),
                "https://abonnement.la-croix.com/",
                "https://www.rcijeux.fr/drupal_game/lacroix/mcroises1/"
                    +"grids/gary%d.mcj",
                "'https://www.la-croix.com/mots-croises'",
                8787,
                LocalDate.of(2025, 12, 19),
                1,
            ))
        }

        if (settings.downloadLATimes) {
            downloaders.add(LATimesDownloader(
                "latimes",
                utils.getString(Res.string.la_times),
            ))
        }

        if (settings.downloadLeParisienF1) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "leparisienf1",
                utils.getString((Res.string.le_parisien_daily_f1)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://abonnement.leparisien.fr",
                "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
                        + "mfleches_1_%d.mfj",
                "'https://www.leparisien.fr/jeux/mots-fleches/'",
                2536,
                LocalDate.of(2022, 6, 21),
                1,
            ))
        }

        if (settings.downloadLeParisienF2) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "leparisienf2",
                utils.getString((Res.string.le_parisien_daily_f2)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://abonnement.leparisien.fr",
                "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
                        + "mfleches_2_%d.mfj",
                "'https://www.leparisien.fr/jeux/mots-fleches/force-2/'",
                2536,
                LocalDate.of(2022, 6, 21),
                1,
            ))
        }

        if (settings.downloadLeParisienF3) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "leparisienf3",
                utils.getString((Res.string.le_parisien_daily_f3)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://abonnement.leparisien.fr",
                "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
                        + "mfleches_3_%d.mfj",
                "'https://www.leparisien.fr/jeux/mots-fleches/force-3/'",
                300,
                LocalDate.of(2023, 3, 4),
                1,
            ))
        }

        if (settings.downloadLeParisienF4) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "leparisienf4",
                utils.getString((Res.string.le_parisien_daily_f4)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://abonnement.leparisien.fr",
                "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/grids/"
                        + "mfleches_4_%d.mfj",
                "'https://www.leparisien.fr/jeux/mots-fleches/force-4/'",
                300,
                LocalDate.of(2023, 3, 4),
                1,
            ))
        }

        if (settings.downloadMetroCryptic) {
            downloaders.add(AbstractDateDownloader(
                "metrocryptic",
                utils.getString(Res.string.metro_cryptic),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ZERO,
                "https://metro.co.uk/",
                PMLIO(),
                "'https://metro.co.uk/puzzles/cryptic-crossword'",
                "'https://metro.co.uk/puzzles/cryptic-crossword'",
                ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDate(),
            ))
        }

        if (settings.downloadMetroQuick) {
            downloaders.add(AbstractDateDownloader(
                "metroquick",
                utils.getString(Res.string.metro_quick),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ZERO,
                "https://metro.co.uk/",
                PMLIO(),
                "'https://metro.co.uk/puzzles/quick-crossword'",
                "'https://metro.co.uk/puzzles/quick-crossword'",
                ZonedDateTime.now(ZoneId.of("Europe/London")).toLocalDate(),
            ))
        }

        if (settings.downloadNewsday) {
            downloaders.add(AbstractDateDownloader(
                "newsday",
                utils.getString(Res.string.newsday),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(5), // midnight US? (by experiment)
                // i can't browse this site for a more specific URL
                // (GDPR)
                "https://www.newsday.com",
                BrainsOnlyIO(),
                "'https://brainsonly.com/servlets-newsday-crossword/"
                    + "newsdaycrossword?date='yyMMdd",
                "'https://www.newsday.com'",
            ))
        }

        if (settings.downloadNewYorkTimesSyndicated) {
            downloaders.add(AbstractDateDownloader(
                "nytsyndicated",
                utils.getString(Res.string.new_york_times_syndicated),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(5), // guess midnight NY
                "https://www.seattletimes.com/games-nytimes-crossword/",
                BrainsOnlyIO(),
                "'https://nytsyn.pzzl.com/nytsyn-crossword-mh/"
                    + "nytsyncrossword?date='"
                    + "yyMMdd",
                "'https://nytsyn.pzzl.com/cwd_seattle/#/s/'"
                    + "yyMMdd",
                LocalDate.now().plusDays(-7),
            ))
        }

        if (settings.downloadNRCInHetMidden) {
            downloaders.add(WithMetaDataDownloader(
                "nrc_inhetmidden",
                utils.getString(Res.string.nrc_in_het_midden),
                AbstractDateDownloader.DAYS_WEEKDAY,
                Duration.ofHours(1), // Wild guess
                "https://abonnementen.nrc.nl/",
                NRCInHetMiddenIO(),
                "'https://api.42puzzles.com/player/nrc/rrFksRq7v6pSweNyhBXk"
                    + "?date='yyyy-MM-dd",
                "'https://www.nrc.nl/puzzels/inhetmidden/'",
                utils.getString(Res.string.nrc_in_het_midden_title),
                utils.getString(Res.string.nrc),
            ))
        }

        if (settings.downloadNRCScrypto) {
            downloaders.add(WithMetaDataDownloader(
                "nrc_scrypto",
                utils.getString(Res.string.nrc_scrypto),
                AbstractDateDownloader.DAYS_SATURDAY,
                Duration.ofHours(1), // Wild guess
                "https://abonnementen.nrc.nl/",
                FortyTwoPuzzlesIO(),
                "'https://api.42puzzles.com/player/nrc/AIbjYuckKZdc4pUpBKjW"
                    + "?date='yyyy-MM-dd",
                "'https://www.nrc.nl/puzzels/scrypto/'",
                utils.getString(Res.string.nrc_scrypto_title),
                utils.getString(Res.string.nrc),
            ))
        }

        if (settings.downloadPremier) {
            val arkadiumUrl = "https://www.arkadium.com/games/" +
                "premier-crossword-kingsfeatures/"
            downloaders.add(KingDigitalLoginDownloader(
                "Premier",
                "Premier",
                utils.getString(Res.string.premier_crossword),
                AbstractDateDownloader.DAYS_SUNDAY,
                Duration.ofHours(0), // guess midnight NY
                "https://puzzles.kingdigital.com",
                arkadiumUrl,
                arkadiumUrl,
                "KFS+Arkadium$",
                "Puzzles&Games",
                arkadiumUrl,
                "PCC01",
            ))
        }

        if (settings.downloadSheffer) {
            val arkadiumUrl = "'https://www.arkadium.com/games/" +
                "sheffer-crossword-kingsfeatures/'"
            downloaders.add(KingDigitalLoginDownloader(
                "Sheffer",
                "Sheffer",
                utils.getString(Res.string.sheffer_crossword),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ofDays(-20), // from full puzzle list during login
                "https://puzzles.kingdigital.com",
                arkadiumUrl,
                arkadiumUrl,
                "KFS+Arkadium$",
                "Puzzles&Games",
                "",
                "SHCC01",
            ))
        }

        if (settings.downloadSudOuestMotsCroises) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMCJ(
                "sudouestmotcroises",
                utils.getString((Res.string.sud_ouest_mots_croises)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://abonnement.sudouest.fr/",
                "https://www.rcijeux.fr/drupal_game/gso/mcroises/grids/%d.mcj",
                "'https://www.sudouest.fr/jeux/mots-croises/'",
                1992,
                LocalDate.of(2025, 10, 26),
                1,
            ))
        }

        if (settings.downloadSudOuestMotsFleches) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "sudouestmotfleches",
                utils.getString((Res.string.sud_ouest_mots_fleches)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://abonnement.sudouest.fr/",
                "https://www.rcijeux.fr/drupal_game/gso/mfleches/grids/%d.mfj",
                "'https://www.sudouest.fr/jeux/mots-fleches/'",
                1988,
                LocalDate.of(2025, 10, 22),
                1,
            ))
        }

        if (settings.downloadTF1MotsCroises) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMCJ(
                "tf1motcroises",
                utils.getString((Res.string.tf1_mots_croises)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://www.tf1info.fr/",
                "https://www.rcijeux.fr/drupal_game/lci/mcroises/grids/%d.mcj",
                "'https://www.tf1info.fr/jeux/mots-croises/'",
                4739,
                LocalDate.of(2025, 10, 26),
                1,
            ))
        }

        if (settings.downloadTF1MotsFleches) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                "tf1motfleches",
                utils.getString((Res.string.tf1_mots_fleches)),
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofHours(1),
                "https://www.tf1info.fr/",
                "https://www.rcijeux.fr/drupal_game/lci/mfleches/grids/%d.mfj",
                "'https://www.tf1info.fr/jeux/mots-fleches/'",
                5395,
                LocalDate.of(2025, 10, 26),
                1,
            ))
        }

        if (settings.downloadUniversal) {
            downloaders.add(UclickDownloader(
                "universal",
                "fcx",
                utils.getString(Res.string.universal_crossword),
                utils.getString(Res.string.uclick_copyright),
                "http://www.uclick.com/client/spi/fcx/",
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofMinutes(6 * 60 + 30), // 06:30 by experiment
                null,
            ))
        }

        if (settings.downloadUSAToday) {
            downloaders.add(UclickDownloader(
                "usatoday",
                "usaon",
                utils.getString(Res.string.usa_today),
                utils.getString(Res.string.usa_today),
                "https://subscribe.usatoday.com",
                AbstractDateDownloader.DAYS_DAILY,
                Duration.ofMinutes(6 * 60 + 30), // 06:30 by experiment
                "'https://games.usatoday.com/en/games/uclick-crossword'",
            ))
        }

        if (settings.downloadWaPoSunday) {
            downloaders.add(AbstractDateDownloader(
                "waposunday",
                utils.getString(Res.string.washington_post_sunday),
                AbstractDateDownloader.DAYS_SUNDAY,
                Duration.ofHours(-1), // by experiment
                "https://subscribe.wsj.com",
                IO(),
                "'https://herbach.dnsalias.com/Wapo/wp'yyMMdd'.puz'",
                "'https://subscribe.washingtonpost.com'",
            ))
        }

        if (settings.downloadWsj) {
            downloaders.add(AbstractDateDownloader(
                "wsj",
                utils.getString(Res.string.wall_street_journal),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ofHours(-3), // by experiment
                "https://subscribe.wsj.com",
                IO(),
                "'https://herbach.dnsalias.com/wsj/wsj'yyMMdd'.puz'",
                "'https://www.wsj.com/news/puzzle'",
            ))
        }

        addCustomDownloaders(downloaders)

        if (settings.scrapeCru) {
            downloaders.add(PageScraper.Puz(
                // certificate doesn't seem to work for me
                // "https://theworld.com/~wij/puzzles/cru/index.html",
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html",
                "crypticcru",
                utils.getString(Res.string.cru_puzzle_workshop),
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html",
            ))
        }

        if (settings.scrapeKegler) {
            downloaders.add(PageScraper.Puz(
                "https://kegler.gitlab.io/Block_style/index.html",
                "keglar",
                utils.getString(Res.string.keglars_cryptics),
                "https://kegler.gitlab.io/",
            ))
        }

        if (settings.scrapePrivateEye) {
            downloaders.add(PageScraper.Puz(
                "https://www.private-eye.co.uk/pictures/crossword/download/",
                "privateeye",
                utils.getString(Res.string.private_eye),
                "https://shop.private-eye.co.uk",
                true, // download from end of page
            ))
        }

        if (settings.scrapePrzekroj) {
            downloaders.add(PageScraper(
                "https://przekroj.org/krzyzowki/.*",
                PrzekrojIO(),
                "https://przekroj.org/humor-rozmaitosci/krzyzowki/",
                "przekroj",
                utils.getString(Res.string.przekroj),
                "https://przekroj.pl/sklep",
                true, // share file url
                false, // read top down
            ))
        }

        if (settings.scrapeGuardianQuick) {
            downloaders.add(PageScraper(
                "https://www.theguardian.com/crosswords/quick/\\d*",
                GuardianJSONIO(),
                "https://www.theguardian.com/crosswords/series/quick",
                "guardianQuick",
                utils.getString(Res.string.guardian_quick),
                "https://support.theguardian.com",
                true, // share file url
                false, // read top down
            ))
        }

        downloaders.forEach { it.timeout = settings.downloadTimeout }

        return downloaders
    }

    private suspend fun addCustomDownloaders(
        downloaders : MutableList<Downloader>,
    ) {
        if (settings.downloadCustomDaily) {
            var title = settings.customDailyTitle
            if (title.trim().isEmpty())
                title = utils.getString(Res.string.custom_daily_title)

            val urlDateFormatPattern = settings.customDailyUrl

            if (!urlDateFormatPattern.trim().isEmpty()) {
                downloaders.add(
                    CustomDailyDownloader("custom", title, urlDateFormatPattern),
                )
            }
        }
    }
}

