
package app.crossword.yourealwaysbe.forkyz.net

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO // needed on IOS, not JVM or Android
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.LocalDate

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
import app.crossword.yourealwaysbe.forkyz.puz.io.BrainsOnlyIO
import app.crossword.yourealwaysbe.forkyz.puz.io.FortyTwoPuzzlesIO
import app.crossword.yourealwaysbe.forkyz.puz.io.GuardianJSONIO
import app.crossword.yourealwaysbe.forkyz.puz.io.IO
import app.crossword.yourealwaysbe.forkyz.puz.io.JPZIO
import app.crossword.yourealwaysbe.forkyz.puz.io.NRCInHetMiddenIO
import app.crossword.yourealwaysbe.forkyz.puz.io.PMLIO
import app.crossword.yourealwaysbe.forkyz.puz.io.PrzekrojIO
import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler

private val TAG = "ForkyzDownloaders"
private val NUM_DOWNLOAD_THREADS = 3

enum class DownloaderID(val id : String) {
    DE_STANDAARD("destandaard"),
    DE_TELEGRAAF("detelegraaf"),
    EL_PAIS_EXPERTO("elpaisexperto"),
    GUARDIAN_DAILY_CRYPTIC("guardian"),
    GUARDIAN_WEEKLY_QUIPTIC("guardianQuiptic"),
    HAM_ABEND("hamabend"),
    INDEPENDENT_DAILY_CRYPTIC("independent"),
    IRISH_NEWS_CRYPTIC("irishnewscryptic"),
    JONESIN("jonesin"),
    JOSEPH("Joseph"),
    TWENTY_MINUTES("20minutes"),
    LA_CROIX("lacroix"),
    LA_TIMES("latimes"),
    LE_PARISIEN_F1("leparisienf1"),
    LE_PARISIEN_F2("leparisienf2"),
    LE_PARISIEN_F3("leparisienf3"),
    LE_PARISIEN_F4("leparisienf4"),
    METRO_CRYPTIC("metrocryptic"),
    METRO_QUICK("metroquick"),
    NEWSDAY("newsday"),
    NYT_SYNDICATED("nytsyndicated"),
    NRC_IN_HET_MIDDEN("nrc_inhetmidden"),
    NRC_SCRYPTO("nrc_scrypto"),
    PREMIER("Premier"),
    SHEFFER("Sheffer"),
    SUD_OUEST_MOTS_CROISES("sudouestmotcroises"),
    SUD_OUEST_MOTS_FLECHES("sudouestmotfleches"),
    TF1_MOTS_CROISES("tf1motcroises"),
    TF1_MOTS_FLECHES("tf1motfleches"),
    UNIVERSAL("universal"),
    USA_TODAY("usatoday"),
    WAPO_SUNDAY("waposunday"),
    WSJ("wsj"),
    CRYPTIC_CRU("crypticcru"),
    GUARDIAN_QUICK("guardianQuick"),
    KEGLAR("keglar"),
    PRIVATE_EYE("privateeye"),
    PRZEKROJ("przekroj"),
    CUSTOM("custom"),
}

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
    companion object {
        // In the main download method, lock all other downloaders so that
        // the list of existing files does not change externally. I.e. two
        // separate downloaders don't cause duplicate downloads.
        // needs to be static as there may be multiple Downloaders
        // instances
        private val puzzleDirLock = Mutex()
    }

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
            if (latestDate != null && latestDate.compareTo(from) >= 0) {
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

    suspend fun getAutoDownloaders(date : LocalDate) : List<Downloader> {
        return getDownloaders().filter {
            settings.enabledAuto(it.downloaderID, date)
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
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

            val somethingDownloaded = AtomicBoolean(false)
            val somethingFailed = AtomicBoolean(false)

            coroutineScope {
                val scope = this + Dispatchers.IO.limitedParallelism(
                    NUM_DOWNLOAD_THREADS,
                    "Puzzle Downloader",
                )

                puzzlesToDownload.forEach { puzzle ->
                    val downloader = puzzle.key
                    val date = puzzle.value
                    scope.launch {
                        val result = downloadPuzzle(
                            downloader,
                            date,
                            fileNames,
                        )
                        if (result.isSuccess)
                            somethingDownloaded.store(true)
                        else if (result.isFailed)
                            somethingFailed.store(true)
                    }
                }
            }

            if (downloadersListener != null) {
                downloadersListener.onDownloadsFinished(
                    somethingDownloaded.load(),
                    somethingFailed.load(),
                )
            }
        } finally {
            releasePuzzleDirLock()
        }
    }

    private suspend fun acquirePuzzleDirLock() {
        puzzleDirLock.lock()
    }

    private fun releasePuzzleDirLock() {
        puzzleDirLock.unlock()
    }

    /**
     * Download and save the puzzle from the downloader
     *
     * Only saves if we don't already have it. Only call if
     * puzzleDirLock is acquired (so existingFileNames doesn't expire).
     */
    private suspend fun downloadPuzzle(
        d : Downloader,
        date : LocalDate,
        existingFileNames : Set<String>,
    ) : Downloader.DownloadResult {
        // failed unless proven otherwise!
        var result = Downloader.DownloadResult.FAILED

        AppLogger.i(TAG, "Downloading " + d.toString())
        downloadersListener?.onPuzzleDownloadStarted(d.name)

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

        downloadersListener?.onPuzzleDownloadFinished(
            d.name,
            result,
        )

        return result
    }

    suspend fun getDownloaders() : List<Downloader> {
        val downloaders : MutableList<Downloader> = mutableListOf()

        if (settings.enabled(DownloaderID.DE_STANDAARD)) {
            downloaders.add(KeesingDownloader(
                DownloaderID.DE_STANDAARD,
                utils.getString(Res.string.de_standaard),
                AbstractDateDownloader.DAYS_DAILY,
                -1.hours, // midnight, tested
                "https://aboshop.standaard.be/",
                "'https://www.standaard.be/kruiswoordraadsel'",
                "hetnieuwsbladpremium",
                "crossword_today_hetnieuwsbladpremium"
            ))
        }

        if (settings.enabled(DownloaderID.DE_TELEGRAAF)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.DE_TELEGRAAF,
                utils.getString(Res.string.de_telegraaf),
                AbstractDateDownloader.DAYS_SUNDAY,
                -1.hours, // midnight, tested
                "https://abonnement.telegraaf.nl/abonnement-bestellen",
                FortyTwoPuzzlesIO(),
                "'https://api.42puzzles.com/player/mediahuis/"
                    +"dHuRm50UEnli8iLLiZPL?date='yyyy-MM-dd",
                "'https://www.telegraaf.nl/puzzels/kruiskop'",
            ))
        }

        if (settings.enabled(DownloaderID.EL_PAIS_EXPERTO)) {
            downloaders.add(ElPaisExpertoDownloader(
                DownloaderID.EL_PAIS_EXPERTO,
                utils.getString(Res.string.elpais_experto),
            ))
        }

        if (settings.enabled(DownloaderID.GUARDIAN_DAILY_CRYPTIC)) {
            downloaders.add(GuardianDailyCrypticDownloader(
                DownloaderID.GUARDIAN_DAILY_CRYPTIC,
                utils.getString(Res.string.guardian_daily),
            ))
        }

        if (settings.enabled(DownloaderID.GUARDIAN_WEEKLY_QUIPTIC)) {
            downloaders.add(GuardianWeeklyQuipticDownloader(
                DownloaderID.GUARDIAN_WEEKLY_QUIPTIC,
                utils.getString(Res.string.guardian_quiptic),
            ))
        }

        if (settings.enabled(DownloaderID.HAM_ABEND)) {
            downloaders.add(RaetselZentraleSchwedenDownloader(
                DownloaderID.HAM_ABEND,
                utils.getString(Res.string.hamburger_abendblatt_daily),
                "hhab",
                AbstractDateDownloader.DAYS_DAILY,
                1.hours, // midnight Germany (verified)
                "https://www.abendblatt.de/plus",
                "'https://www.abendblatt.de/ratgeber/wissen/"
                    + "article106560367/Spielen-Sie-hier-taeglich"
                    + "-das-kostenlose-Kreuzwortraetsel.html'",
            ))
        }

        if (settings.enabled(DownloaderID.INDEPENDENT_DAILY_CRYPTIC)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.INDEPENDENT_DAILY_CRYPTIC,
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

        if (settings.enabled(DownloaderID.IRISH_NEWS_CRYPTIC)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.IRISH_NEWS_CRYPTIC,
                utils.getString(Res.string.irish_news_cryptic),
                AbstractDateDownloader.DAYS_WEEKDAY,
                Duration.ZERO,
                "https://www.irishnews.com/",
                PAPuzzlesStreamScraper(),
                "'https://www.irishnews.com/puzzles/cryptic-crossword/'",
                "'https://www.irishnews.com/puzzles/cryptic-crossword/'",
                TimeUtils.todayIn("Europe/Belfast"),
             ))
        }

        if (settings.enabled(DownloaderID.JONESIN)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.JONESIN,
                utils.getString(Res.string.jonesin_crosswords),
                AbstractDateDownloader.DAYS_THURSDAY,
                -2.days, // by experiment
                "https://crosswordnexus.com/jonesin/",
                IO(),
                "'https://herbach.dnsalias.com/Jonesin/jz'yyMMdd'.puz'",
                "'https://crosswordnexus.com/solve/?"
                    + "puzzle=/downloads/jonesin/jonesin'yyMMdd'.puz'",
            ))
        }

        if (settings.enabled(DownloaderID.JOSEPH)) {
            val arkadiumUrl = "https://www.arkadium.com/games/" +
                "joseph-crossword-kingsfeatures/"
            downloaders.add(KingDigitalLoginDownloader(
                "Joseph",
                DownloaderID.JOSEPH,
                utils.getString(Res.string.joseph_crossword),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                -20.days, // by experiment
                "https://puzzles.kingdigital.com",
                arkadiumUrl,
                arkadiumUrl,
                "KFS+Arkadium$",
                "Puzzles&Games",
                "",
                "JCC01",
            ))
        }

        if (settings.enabled(DownloaderID.TWENTY_MINUTES)) {
            downloaders.add(KeesingDownloader(
                DownloaderID.TWENTY_MINUTES,
                utils.getString(Res.string.vingminutes),
                AbstractDateDownloader.DAYS_DAILY,
                0.hours, // guess-ish
                "https://www.20minutes.fr",
                "'https://www.20minutes.fr/services/mots-fleches'",
                "20minutes",
                "arrowword_plus_today_"
            ))
        }

        if (settings.enabled(DownloaderID.LA_CROIX)) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMCJ(
                DownloaderID.LA_CROIX,
                utils.getString(Res.string.la_croix),
                AbstractDateDownloader.DAYS_WEEKDAY,
                1.hours,
                "https://abonnement.la-croix.com/",
                {
                    "https://www.rcijeux.fr/drupal_game/lacroix/mcroises1/" +
                    "grids/gary$it.mcj"
                },
                "'https://www.la-croix.com/mots-croises'",
                8815,
                LocalDate(2026, 1, 30),
            ))
        }

        if (settings.enabled(DownloaderID.LA_TIMES)) {
            downloaders.add(LATimesDownloader(
                DownloaderID.LA_TIMES,
                utils.getString(Res.string.la_times),
            ))
        }

        if (settings.enabled(DownloaderID.LE_PARISIEN_F1)) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                DownloaderID.LE_PARISIEN_F1,
                utils.getString((Res.string.le_parisien_daily_f1)),
                AbstractDateDownloader.DAYS_DAILY,
                1.hours,
                "https://abonnement.leparisien.fr",
                {
                    "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1" +
                    "/grids/mfleches_1_$it.mfj"
                },
                "'https://www.leparisien.fr/jeux/mots-fleches/'",
                2536,
                LocalDate(2022, 6, 21),
            ))
        }

        if (settings.enabled(DownloaderID.LE_PARISIEN_F2)) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                DownloaderID.LE_PARISIEN_F2,
                utils.getString((Res.string.le_parisien_daily_f2)),
                AbstractDateDownloader.DAYS_DAILY,
                1.hours,
                "https://abonnement.leparisien.fr",
                {
                    "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/" +
                    "grids/mfleches_2_$it.mfj"
                },
                "'https://www.leparisien.fr/jeux/mots-fleches/force-2/'",
                2536,
                LocalDate(2022, 6, 21),
            ))
        }

        if (settings.enabled(DownloaderID.LE_PARISIEN_F3)) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                DownloaderID.LE_PARISIEN_F3,
                utils.getString((Res.string.le_parisien_daily_f3)),
                AbstractDateDownloader.DAYS_DAILY,
                1.hours,
                "https://abonnement.leparisien.fr",
                {
                    "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/" +
                    "grids/mfleches_3_$it.mfj"
                },
                "'https://www.leparisien.fr/jeux/mots-fleches/force-3/'",
                300,
                LocalDate(2023, 3, 4),
            ))
        }

        if (settings.enabled(DownloaderID.LE_PARISIEN_F4)) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                DownloaderID.LE_PARISIEN_F4,
                utils.getString((Res.string.le_parisien_daily_f4)),
                AbstractDateDownloader.DAYS_DAILY,
                1.hours,
                "https://abonnement.leparisien.fr",
                {
                    "https://www.rcijeux.fr/drupal_game/leparisien/mfleches1/" +
                    "grids/mfleches_4_$it.mfj"
                },
                "'https://www.leparisien.fr/jeux/mots-fleches/force-4/'",
                300,
                LocalDate(2023, 3, 4),
            ))
        }

        if (settings.enabled(DownloaderID.METRO_CRYPTIC)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.METRO_CRYPTIC,
                utils.getString(Res.string.metro_cryptic),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ZERO,
                "https://metro.co.uk/",
                PMLIO(),
                "'https://metro.co.uk/puzzles/cryptic-crossword'",
                "'https://metro.co.uk/puzzles/cryptic-crossword'",
                TimeUtils.todayIn("Europe/London"),
                overrideEmbeddedDate = false,
            ))
        }

        if (settings.enabled(DownloaderID.METRO_QUICK)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.METRO_QUICK,
                utils.getString(Res.string.metro_quick),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                Duration.ZERO,
                "https://metro.co.uk/",
                PMLIO(),
                "'https://metro.co.uk/puzzles/quick-crossword'",
                "'https://metro.co.uk/puzzles/quick-crossword'",
                TimeUtils.todayIn("Europe/London"),
                overrideEmbeddedDate = false,
            ))
        }

        if (settings.enabled(DownloaderID.NEWSDAY)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.NEWSDAY,
                utils.getString(Res.string.newsday),
                AbstractDateDownloader.DAYS_DAILY,
                5.hours, // midnight US? (by experiment)
                // i can't browse this site for a more specific URL
                // (GDPR)
                "https://www.newsday.com",
                BrainsOnlyIO(),
                "'https://brainsonly.com/servlets-newsday-crossword/"
                    + "newsdaycrossword?date='yyMMdd",
                "'https://www.newsday.com'",
            ))
        }

        if (settings.enabled(DownloaderID.NYT_SYNDICATED)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.NYT_SYNDICATED,
                utils.getString(Res.string.new_york_times_syndicated),
                AbstractDateDownloader.DAYS_DAILY,
                5.hours, // guess midnight NY
                "https://www.seattletimes.com/games-nytimes-crossword/",
                BrainsOnlyIO(),
                "'https://nytsyn.pzzl.com/nytsyn-crossword-mh/"
                    + "nytsyncrossword?date='"
                    + "yyMMdd",
                "'https://nytsyn.pzzl.com/cwd_seattle/#/s/'"
                    + "yyMMdd",
                TimeUtils.offsetTodayUTC(-7.days),
            ))
        }

        if (settings.enabled(DownloaderID.NRC_IN_HET_MIDDEN)) {
            downloaders.add(WithMetaDataDownloader(
                DownloaderID.NRC_IN_HET_MIDDEN,
                utils.getString(Res.string.nrc_in_het_midden),
                AbstractDateDownloader.DAYS_WEEKDAY,
                1.hours, // Wild guess
                "https://abonnementen.nrc.nl/",
                NRCInHetMiddenIO(),
                "'https://api.42puzzles.com/player/nrc/rrFksRq7v6pSweNyhBXk"
                    + "?date='yyyy-MM-dd",
                "'https://www.nrc.nl/puzzels/inhetmidden/'",
                utils.getString(Res.string.nrc_in_het_midden_title),
                utils.getString(Res.string.nrc),
            ))
        }

        if (settings.enabled(DownloaderID.NRC_SCRYPTO)) {
            downloaders.add(WithMetaDataDownloader(
                DownloaderID.NRC_SCRYPTO,
                utils.getString(Res.string.nrc_scrypto),
                AbstractDateDownloader.DAYS_SATURDAY,
                1.hours, // Wild guess
                "https://abonnementen.nrc.nl/",
                FortyTwoPuzzlesIO(),
                "'https://api.42puzzles.com/player/nrc/AIbjYuckKZdc4pUpBKjW"
                    + "?date='yyyy-MM-dd",
                "'https://www.nrc.nl/puzzels/scrypto/'",
                utils.getString(Res.string.nrc_scrypto_title),
                utils.getString(Res.string.nrc),
            ))
        }

        if (settings.enabled(DownloaderID.PREMIER)) {
            val arkadiumUrl = "https://www.arkadium.com/games/" +
                "premier-crossword-kingsfeatures/"
            downloaders.add(KingDigitalLoginDownloader(
                "Premier",
                DownloaderID.PREMIER,
                utils.getString(Res.string.premier_crossword),
                AbstractDateDownloader.DAYS_SUNDAY,
                0.hours, // guess midnight NY
                "https://puzzles.kingdigital.com",
                arkadiumUrl,
                arkadiumUrl,
                "KFS+Arkadium$",
                "Puzzles&Games",
                arkadiumUrl,
                "PCC01",
            ))
        }

        if (settings.enabled(DownloaderID.SHEFFER)) {
            val arkadiumUrl = "'https://www.arkadium.com/games/" +
                "sheffer-crossword-kingsfeatures/'"
            downloaders.add(KingDigitalLoginDownloader(
                "Sheffer",
                DownloaderID.SHEFFER,
                utils.getString(Res.string.sheffer_crossword),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                -20.days, // from full puzzle list during login
                "https://puzzles.kingdigital.com",
                arkadiumUrl,
                arkadiumUrl,
                "KFS+Arkadium$",
                "Puzzles&Games",
                "",
                "SHCC01",
            ))
        }

        if (settings.enabled(DownloaderID.SUD_OUEST_MOTS_CROISES)) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMCJ(
                DownloaderID.SUD_OUEST_MOTS_CROISES,
                utils.getString((Res.string.sud_ouest_mots_croises)),
                AbstractDateDownloader.DAYS_DAILY,
                1.hours,
                "https://abonnement.sudouest.fr/",
                {
                    "https://www.rcijeux.fr/drupal_game/gso/mcroises/" +
                    "grids/$it.mcj"
                },
                "'https://www.sudouest.fr/jeux/mots-croises/'",
                1992,
                LocalDate(2025, 10, 26),
            ))
        }

        if (settings.enabled(DownloaderID.SUD_OUEST_MOTS_FLECHES)) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                DownloaderID.SUD_OUEST_MOTS_FLECHES,
                utils.getString((Res.string.sud_ouest_mots_fleches)),
                AbstractDateDownloader.DAYS_DAILY,
                1.hours,
                "https://abonnement.sudouest.fr/",
                {
                    "https://www.rcijeux.fr/drupal_game/gso/mfleches/" +
                    "grids/$it.mfj"
                },
                "'https://www.sudouest.fr/jeux/mots-fleches/'",
                1988,
                LocalDate(2025, 10, 22),
            ))
        }

        if (settings.enabled(DownloaderID.TF1_MOTS_CROISES)) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMCJ(
                DownloaderID.TF1_MOTS_CROISES,
                utils.getString((Res.string.tf1_mots_croises)),
                AbstractDateDownloader.DAYS_DAILY,
                1.hours,
                "https://www.tf1info.fr/",
                {
                    "https://www.rcijeux.fr/drupal_game/lci/mcroises/" +
                    "grids/$it.mcj"
                },
                "'https://www.tf1info.fr/jeux/mots-croises/'",
                4739,
                LocalDate(2025, 10, 26),
            ))
        }

        if (settings.enabled(DownloaderID.TF1_MOTS_FLECHES)) {
            downloaders.add(AbstractRCIJeuxMXJDateDownloader.buildMFJ(
                DownloaderID.TF1_MOTS_FLECHES,
                utils.getString((Res.string.tf1_mots_fleches)),
                AbstractDateDownloader.DAYS_DAILY,
                1.hours,
                "https://www.tf1info.fr/",
                {
                    "https://www.rcijeux.fr/drupal_game/lci/mfleches/" +
                    "grids/$it.mfj"
                },
                "'https://www.tf1info.fr/jeux/mots-fleches/'",
                5395,
                LocalDate(2025, 10, 26),
            ))
        }

        if (settings.enabled(DownloaderID.UNIVERSAL)) {
            downloaders.add(UclickDownloader(
                DownloaderID.UNIVERSAL,
                "fcx",
                utils.getString(Res.string.universal_crossword),
                utils.getString(Res.string.uclick_copyright),
                "http://www.uclick.com/client/spi/fcx/",
                AbstractDateDownloader.DAYS_DAILY,
                6.hours + 30.minutes, // 06:30 by experiment
                null,
            ))
        }

        if (settings.enabled(DownloaderID.USA_TODAY)) {
            downloaders.add(USATodayDownloader(
                DownloaderID.USA_TODAY,
                utils.getString(Res.string.usa_today),
            ))
        }

        if (settings.enabled(DownloaderID.WAPO_SUNDAY)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.WAPO_SUNDAY,
                utils.getString(Res.string.washington_post_sunday),
                AbstractDateDownloader.DAYS_SUNDAY,
                -1.hours, // by experiment
                "https://subscribe.wsj.com",
                IO(),
                "'https://herbach.dnsalias.com/Wapo/wp'yyMMdd'.puz'",
                "'https://subscribe.washingtonpost.com'",
            ))
        }

        if (settings.enabled(DownloaderID.WSJ)) {
            downloaders.add(AbstractDateDownloader(
                DownloaderID.WSJ,
                utils.getString(Res.string.wall_street_journal),
                AbstractDateDownloader.DAYS_NO_SUNDAY,
                -3.hours, // by experiment
                "https://subscribe.wsj.com",
                IO(),
                "'https://herbach.dnsalias.com/wsj/wsj'yyMMdd'.puz'",
                "'https://www.wsj.com/news/puzzle'",
            ))
        }

        addCustomDownloaders(downloaders)

        if (settings.enabled(DownloaderID.CRYPTIC_CRU)) {
            downloaders.add(PageScraper.Puz(
                // certificate doesn't seem to work for me
                // "https://theworld.com/~wij/puzzles/cru/index.html",
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html",
                DownloaderID.CRYPTIC_CRU,
                utils.getString(Res.string.cru_puzzle_workshop),
                "https://archive.nytimes.com/www.nytimes.com/premium/xword/cryptic-archive.html",
            ))
        }

        if (settings.enabled(DownloaderID.KEGLAR)) {
            downloaders.add(PageScraper.Puz(
                "https://kegler.gitlab.io/Block_style/index.html",
                DownloaderID.KEGLAR,
                utils.getString(Res.string.keglars_cryptics),
                "https://kegler.gitlab.io/",
            ))
        }

        if (settings.enabled(DownloaderID.PRIVATE_EYE)) {
            downloaders.add(PageScraper.Puz(
                "https://www.private-eye.co.uk/pictures/crossword/download/",
                DownloaderID.PRIVATE_EYE,
                utils.getString(Res.string.private_eye),
                "https://shop.private-eye.co.uk",
                true, // download from end of page
            ))
        }

        if (settings.enabled(DownloaderID.PRZEKROJ)) {
            downloaders.add(PageScraper(
                "https://przekroj.org/krzyzowki/.*",
                PrzekrojIO(),
                "https://przekroj.org/humor-rozmaitosci/krzyzowki/",
                DownloaderID.PRZEKROJ,
                utils.getString(Res.string.przekroj),
                "https://przekroj.pl/sklep",
                true, // share file url
                false, // read top down
            ))
        }

        if (settings.enabled(DownloaderID.GUARDIAN_QUICK)) {
            downloaders.add(PageScraper(
                "https://www.theguardian.com/crosswords/quick/\\d*",
                GuardianJSONIO(),
                "https://www.theguardian.com/crosswords/series/quick",
                DownloaderID.GUARDIAN_QUICK,
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
        if (settings.enabled(DownloaderID.CUSTOM)) {
            var title = settings.customDailyTitle
            if (title.trim().isEmpty())
                title = utils.getString(Res.string.custom_daily_title)

            val urlDateFormatPattern = settings.customDailyUrl

            if (!urlDateFormatPattern.trim().isEmpty()) {
                downloaders.add(
                    CustomDailyDownloader(
                        DownloaderID.CUSTOM,
                        title,
                        urlDateFormatPattern,
                    ),
                )
            }
        }
    }
}

