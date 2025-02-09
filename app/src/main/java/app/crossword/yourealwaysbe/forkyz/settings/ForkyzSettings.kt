
package app.crossword.yourealwaysbe.forkyz.settings

import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import androidx.annotation.WorkerThread
import androidx.datastore.core.DataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import app.crossword.yourealwaysbe.forkyz.net.Downloaders
import app.crossword.yourealwaysbe.forkyz.util.JSONUtils
import app.crossword.yourealwaysbe.forkyz.util.files.Accessor
import app.crossword.yourealwaysbe.forkyz.util.observeOnce
import app.crossword.yourealwaysbe.puz.MovementStrategy
import app.crossword.yourealwaysbe.puz.Playboard.DeleteCrossingMode

class ForkyzSettings(
    val settingsStore: DataStore<Settings>
) : CoroutineScope {
    private val WRITE_CHARSET = Charset.forName("UTF-8")

    private val job = Job()
    // serialise updates in single thread
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override val coroutineContext
        = Dispatchers.IO.limitedParallelism(1) + job

    private val mainScope = CoroutineScope(Dispatchers.Main)

    val settingsFlow: Flow<Settings> = settingsStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(Settings.getDefaultInstance())
            } else {
                throw exception
            }
        }

    val liveVoiceAlwaysAnnounceBox : LiveData<Boolean>
        = settingsFlow.map {
            it.alwaysAnnounceBox
        }.distinctUntilChanged().asLiveData()

    fun getVoiceAlwaysAnnounceBox(cb : Consumer<Boolean>)
        = liveVoiceAlwaysAnnounceBox.observeOnce(cb)

    fun setVoiceAlwaysAnnounceBox(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setAlwaysAnnounceBox(value).build()
            }
        }
    }

    val liveVoiceAlwaysAnnounceClue : LiveData<Boolean>
        = settingsFlow.map {
            it.alwaysAnnounceClue
        }.distinctUntilChanged().asLiveData()

    fun getVoiceAlwaysAnnounceClue(cb : Consumer<Boolean>)
        = liveVoiceAlwaysAnnounceClue.observeOnce(cb)

    fun setVoiceAlwaysAnnounceClue(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setAlwaysAnnounceClue(value).build()
            }
        }
    }

    val liveAppTheme : LiveData<Theme>
        = settingsFlow.map {
            it.applicationTheme
        }.distinctUntilChanged().asLiveData()

    fun getAppTheme(cb : Consumer<Theme>)
        = liveAppTheme.observeOnce(cb)

    fun getAppThemeSync() : Theme
        = runBlocking { settingsFlow.first().applicationTheme }

    fun setAppTheme(value : Theme) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setApplicationTheme(value).build()
            }
        }
    }

    fun setAppThemeSync(value : Theme) {
        runBlocking {
            settingsStore.updateData { settings ->
                settings.toBuilder().setApplicationTheme(value).build()
            }
        }
    }

    val liveBackgroundDownloadSettings : LiveData<BackgroundDownloadSettings>
        = settingsFlow.map {
            BackgroundDownloadSettings(
                it.backgroundDownloadRequireUnmetered,
                it.backgroundDownloadAllowRoaming,
                it.backgroundDownloadRequireCharging,
                it.backgroundDownloadHourly,
                it.backgroundDownloadDaysList.toSet(),
                it.backgroundDownloadDaysTime
            )
        }.distinctUntilChanged().asLiveData()

    fun getBackgroundDownloadSettings(cb : Consumer<BackgroundDownloadSettings>)
        = liveBackgroundDownloadSettings.observeOnce(cb)

    val liveBrowseCleanupAgeArchive : LiveData<Int>
        = settingsFlow.map {
            it.archiveCleanupAge
        }.distinctUntilChanged().asLiveData()

    fun getBrowseCleanupAgeArchive(cb : Consumer<Int>)
        = liveBrowseCleanupAgeArchive.observeOnce(cb)

    fun setBrowseCleanupAgeArchive(value : Int) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setArchiveCleanupAge(value).build()
            }
        }
    }

    val liveDownloadersSettings : LiveData<DownloadersSettings>
        = settingsFlow.map {
            DownloadersSettings(
                it.downloadDeStandaard,
                it.downloadDeTelegraaf,
                it.downloadGuardianDailyCryptic,
                it.downloadGuardianWeeklyQuiptic,
                it.downloadHamAbend,
                it.downloadIndependentDailyCryptic,
                it.downloadIrishNewsCryptic,
                it.downloadJonesin,
                it.downloadJoseph,
                it.download20Minutes,
                it.downloadLeParisienF1,
                it.downloadLeParisienF2,
                it.downloadLeParisienF3,
                it.downloadLeParisienF4,
                it.downloadMetroCryptic,
                it.downloadMetroQuick,
                it.downloadNewsday,
                it.downloadNewYorkTimesSyndicated,
                it.downloadPremier,
                it.downloadSheffer,
                it.downloadUniversal,
                it.downloadUSAToday,
                it.downloadWaPoSunday,
                it.downloadWsj,
                it.scrapeCru,
                it.scrapeEveryman,
                it.scrapeGuardianQuick,
                it.scrapeKegler,
                it.scrapePrivateEye,
                it.scrapePrzekroj,
                it.downloadCustomDaily,
                it.customDailyTitle,
                it.customDailyUrl,
                it.suppressSummaryMessages,
                it.suppressMessages,
                it.autoDownloadersList.toSet(),
                it.downloadTimeout,
                it.dlOnStartup,
            )
        }.distinctUntilChanged().asLiveData()

    fun getDownloadersSettings(cb : Consumer<DownloadersSettings>)
        = liveDownloadersSettings.observeOnce(cb)

    val liveDownloadAllowRoaming : LiveData<Boolean>
        = settingsFlow.map {
            it.backgroundDownloadAllowRoaming
        }.distinctUntilChanged().asLiveData()

    fun getDownloadAllowRoaming(cb : Consumer<Boolean>)
        = liveDownloadAllowRoaming.observeOnce(cb)

    fun setDownloadAllowRoaming(value : Boolean, cb : Runnable? = null) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setBackgroundDownloadAllowRoaming(value)
                    .build()
            }
            runMain(cb)
        }
    }

    val liveDownloadAutoDownloaders : LiveData<Set<String>>
        = settingsFlow.map {
            it.autoDownloadersList.toSet()
        }.distinctUntilChanged().asLiveData()

    fun getDownloadAutoDownloaders(cb : Consumer<Set<String>>)
        = liveDownloadAutoDownloaders.observeOnce(cb)

    fun setDownloadAutoDownloaders(value : Set<String>) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .clearAutoDownloaders()
                    .addAllAutoDownloaders(value)
                    .build()
            }
        }
    }

    val liveDownloadDays : LiveData<Set<String>>
        = settingsFlow.map {
            it.backgroundDownloadDaysList.toSet()
        }.distinctUntilChanged().asLiveData()

    fun getDownloadDays(cb : Consumer<Set<String>>)
        = liveDownloadDays.observeOnce(cb)

    fun setDownloadDays(value : Set<String>, cb : Runnable? = null) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .clearBackgroundDownloadDays()
                    .addAllBackgroundDownloadDays(value)
                    .build()
            }
            runMain(cb)
        }
    }

    val liveDownloadDaysTime : LiveData<Int>
        = settingsFlow.map {
            it.backgroundDownloadDaysTime
        }.distinctUntilChanged().asLiveData()

    fun getDownloadDaysTime(cb : Consumer<Int>)
        = liveDownloadDaysTime.observeOnce(cb)

    fun setDownloadDaysTime(value : Int, cb : Runnable? = null) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setBackgroundDownloadDaysTime(value)
                    .build()
            }
            runMain(cb)
        }
    }

    val liveDownloadHourly : LiveData<Boolean>
        = settingsFlow.map {
            it.backgroundDownloadHourly
        }.distinctUntilChanged().asLiveData()

    fun getDownloadHourly(cb : Consumer<Boolean>)
        = liveDownloadHourly.observeOnce(cb)

    fun setDownloadHourly(value : Boolean, cb : Runnable? = null) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setBackgroundDownloadHourly(value).build()
            }
            runMain(cb)
        }
    }

    val liveDownloadRequireCharging : LiveData<Boolean>
        = settingsFlow.map {
            it.backgroundDownloadRequireCharging
        }.distinctUntilChanged().asLiveData()

    fun getDownloadRequireCharging(cb : Consumer<Boolean>)
        = liveDownloadRequireCharging.observeOnce(cb)

    fun setDownloadRequireCharging(
        value : Boolean,
        cb : Runnable? = null,
    ) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setBackgroundDownloadRequireCharging(value)
                    .build()
            }
            runMain(cb)
        }
    }

    val liveDownloadRequireUnmetered : LiveData<Boolean>
        = settingsFlow.map {
            it.backgroundDownloadRequireUnmetered
        }.distinctUntilChanged().asLiveData()

    fun getDownloadRequireUnmetered(cb : Consumer<Boolean>)
        = liveDownloadRequireUnmetered.observeOnce(cb)

    fun setDownloadRequireUnmetered(
        value : Boolean,
        cb : Runnable? = null,
    ) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setBackgroundDownloadRequireUnmetered(value)
                    .build()
            }
            runMain(cb)
        }
    }

    val liveBrowseAlwaysShowRating : LiveData<Boolean>
        = settingsFlow.map {
            it.browseAlwaysShowRating
        }.distinctUntilChanged().asLiveData()

    fun getBrowseAlwaysShowRating(cb : Consumer<Boolean>)
        = liveBrowseAlwaysShowRating.observeOnce(cb)

    fun setBrowseAlwaysShowRating(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setBrowseAlwaysShowRating(value).build()
            }
        }
    }

    val liveBrowseIndicateIfSolution : LiveData<Boolean>
        = settingsFlow.map {
            it.browseIndicateIfSolution
        }.distinctUntilChanged().asLiveData()

    fun getBrowseIndicateIfSolution(cb : Consumer<Boolean>)
        = liveBrowseIndicateIfSolution.observeOnce(cb)

    fun setBrowseIndicateIfSolution(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setBrowseIndicateIfSolution(value).build()
            }
        }
    }

    val liveBrowseNewPuzzle : LiveData<Boolean>
        = settingsFlow.map {
            it.browseNewPuzzle
        }.distinctUntilChanged().asLiveData()

    fun getBrowseNewPuzzle(cb : Consumer<Boolean>)
        = liveBrowseNewPuzzle.observeOnce(cb)

    fun setBrowseNewPuzzle(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setBrowseNewPuzzle(value).build()
            }
        }
    }

    val liveBrowseShowPercentageCorrect : LiveData<Boolean>
        = settingsFlow.map {
            it.browseShowPercentageCorrect
        }.distinctUntilChanged().asLiveData()

    fun getBrowseShowPercentageCorrect(cb : Consumer<Boolean>)
        = liveBrowseShowPercentageCorrect.observeOnce(cb)

    fun setBrowseShowPercentageCorrect(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setBrowseShowPercentageCorrect(value)
                    .build()
            }
        }
    }

    val liveVoiceButtonActivatesVoice : LiveData<Boolean>
        = settingsFlow.map {
            it.buttonActivatesVoice
        }.distinctUntilChanged().asLiveData()

    fun getVoiceButtonActivatesVoice(cb : Consumer<Boolean>)
        = liveVoiceButtonActivatesVoice.observeOnce(cb)

    fun setVoiceButtonActivatesVoice(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setButtonActivatesVoice(value).build()
            }
        }
    }

    val liveVoiceButtonAnnounceClue : LiveData<Boolean>
        = settingsFlow.map {
            it.buttonAnnounceClue
        }.distinctUntilChanged().asLiveData()

    fun getVoiceButtonAnnounceClue(cb : Consumer<Boolean>)
        = liveVoiceButtonAnnounceClue.observeOnce(cb)

    fun setVoiceButtonAnnounceClue(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setButtonAnnounceClue(value).build()
            }
        }
    }

    val liveExtChatGPTAPIKey : LiveData<String>
        = settingsFlow.map {
            it.chatGPTAPIKey
        }.distinctUntilChanged().asLiveData()

    fun getExtChatGPTAPIKey(cb : Consumer<String>)
        = liveExtChatGPTAPIKey.observeOnce(cb)

    fun setExtChatGPTAPIKey(value : String) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setChatGPTAPIKey(value).build()
            }
        }
    }

    val liveBrowseCleanupAge : LiveData<Int>
        = settingsFlow.map {
            it.cleanupAge
        }.distinctUntilChanged().asLiveData()

    fun getBrowseCleanupAge(cb : Consumer<Int>)
        = liveBrowseCleanupAge.observeOnce(cb)

    fun setBrowseCleanupAge(value : Int) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setCleanupAge(value).build()
            }
        }
    }

    val livePlayClueBelowGrid : LiveData<Boolean>
        = settingsFlow.map {
            it.clueBelowGrid
        }.distinctUntilChanged().asLiveData()

    fun getPlayClueBelowGrid(cb : Consumer<Boolean>)
        = livePlayClueBelowGrid.observeOnce(cb)

    fun setPlayClueBelowGrid(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setClueBelowGrid(value).build()
            }
        }
    }

    val livePlayClueHighlight : LiveData<ClueHighlight>
        = settingsFlow.map {
            it.clueHighlight
        }.distinctUntilChanged().asLiveData()

    fun getPlayClueHighlight(cb : Consumer<ClueHighlight>)
        = livePlayClueHighlight.observeOnce(cb)

    fun setPlayClueHighlight(value : ClueHighlight) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setClueHighlight(value).build()
            }
        }
    }

    val liveClueListClueTabsDouble : LiveData<ClueTabsDouble>
        = settingsFlow.map {
            it.clueTabsDouble
        }.distinctUntilChanged().asLiveData()

    fun getClueListClueTabsDouble(cb : Consumer<ClueTabsDouble>)
        = liveClueListClueTabsDouble.observeOnce(cb)

    fun setClueListClueTabsDouble(value : ClueTabsDouble) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setClueTabsDouble(value).build()
            }
        }
    }

    /**
     * For testing -- shutdown the settings and wait for jobs to end
     *
     * Useful to wait for all updates to have passed.
     */
    internal suspend fun shutdown() {
        job.complete()
        job.join()
    }

    private val livePlayActivityClueTabsPage : LiveData<Int>
        = settingsFlow.map {
            it.playActivityClueTabsPage
        }.distinctUntilChanged().asLiveData()

    private val livePlayActivityClueTabsPage1 : LiveData<Int>
        = settingsFlow.map {
            it.playActivityClueTabsPage1
        }.distinctUntilChanged().asLiveData()

    fun getPlayClueTabsPage(viewIdx : Int, cb : Consumer<Int>) {
        if (viewIdx == 0)
            livePlayActivityClueTabsPage.observeOnce(cb)
        else
            livePlayActivityClueTabsPage1.observeOnce(cb)
    }

    fun setPlayClueTabsPage(viewIdx : Int, pageNo : Int) {
        if (pageNo >= 0) {
            if (viewIdx == 0) {
                launch {
                    settingsStore.updateData { settings ->
                        settings.toBuilder()
                            .setPlayActivityClueTabsPage(pageNo)
                            .build()
                    }
                }
            } else if (viewIdx == 1) {
                launch {
                    settingsStore.updateData { settings ->
                        settings.toBuilder()
                            .setPlayActivityClueTabsPage1(pageNo)
                            .build()
                    }
                }
            }
        }
    }

    val liveExtCrosswordSolverEnabled : LiveData<Boolean>
        = settingsFlow.map {
            it.crosswordSolverEnabled
        }.distinctUntilChanged().asLiveData()

    fun getExtCrosswordSolverEnabled(cb : Consumer<Boolean>)
        = liveExtCrosswordSolverEnabled.observeOnce(cb)

    fun setExtCrosswordSolverEnabled(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setCrosswordSolverEnabled(value).build()
            }
        }
    }

    val liveDownloadCustomDailyTitle : LiveData<String>
        = settingsFlow.map {
            it.customDailyTitle
        }.distinctUntilChanged().asLiveData()

    fun getDownloadCustomDailyTitle(cb : Consumer<String>)
        = liveDownloadCustomDailyTitle.observeOnce(cb)

    fun setDownloadCustomDailyTitle(value : String) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setCustomDailyTitle(value).build()
            }
        }
    }

    val liveDownloadCustomDailyURL : LiveData<String>
        = settingsFlow.map {
            it.customDailyUrl
        }.distinctUntilChanged().asLiveData()

    fun getDownloadCustomDailyURL(cb : Consumer<String>)
        = liveDownloadCustomDailyURL.observeOnce(cb)

    fun setDownloadCustomDailyURL(value : String) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setCustomDailyUrl(value).build()
            }
        }
    }

    val livePlayCycleUnfilledMode : LiveData<CycleUnfilledMode>
        = settingsFlow.map {
            it.cycleUnfilledMode
        }.distinctUntilChanged().asLiveData()

    fun getPlayCycleUnfilledMode(cb : Consumer<CycleUnfilledMode>)
        = livePlayCycleUnfilledMode.observeOnce(cb)

    fun setPlayCycleUnfilledMode(value : CycleUnfilledMode) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setCycleUnfilledMode(value).build()
            }
        }
    }

    val livePlayDeleteCrossingMode : LiveData<DeleteCrossingMode>
        = settingsFlow.map {
            when (it.deleteCrossingMode) {
                DeleteCrossingModeSetting.DCMS_DELETE
                    -> DeleteCrossingMode.DELETE
                DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_CELLS
                    -> DeleteCrossingMode.PRESERVE_FILLED_CELLS
                DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_WORDS
                    -> DeleteCrossingMode.PRESERVE_FILLED_WORDS
                else -> DeleteCrossingMode.DELETE
            }
        }.distinctUntilChanged().asLiveData()

    fun getPlayDeleteCrossingMode(cb : Consumer<DeleteCrossingMode>)
        = livePlayDeleteCrossingMode.observeOnce(cb)

    val livePlayDeleteCrossingModeSetting : LiveData<DeleteCrossingModeSetting>
        = settingsFlow.map {
            it.deleteCrossingMode
        }.distinctUntilChanged().asLiveData()

    fun getPlayDeleteCrossingModeSetting(
        cb : Consumer<DeleteCrossingModeSetting>
    ) = livePlayDeleteCrossingModeSetting.observeOnce(cb)

    fun setPlayDeleteCrossingModeSetting(value : DeleteCrossingModeSetting) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDeleteCrossingMode(value).build()
            }
        }
    }

    val liveBrowseDeleteOnCleanup : LiveData<Boolean>
        = settingsFlow.map {
            it.deleteOnCleanup
        }.distinctUntilChanged().asLiveData()

    fun getBrowseDeleteOnCleanup(cb : Consumer<Boolean>)
        = liveBrowseDeleteOnCleanup.observeOnce(cb)

    fun setBrowseDeleteOnCleanup(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDeleteOnCleanup(value).build()
            }
        }
    }

    val liveRatingsDisableRatings : LiveData<Boolean>
        = settingsFlow.map {
            it.disableRatings
        }.distinctUntilChanged().asLiveData()

    fun getRatingsDisableRatings(cb : Consumer<Boolean>)
        = liveRatingsDisableRatings.observeOnce(cb)

    fun setRatingsDisableRatings(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDisableRatings(value).build()
            }
        }
    }

    val liveBrowseDisableSwipe : LiveData<Boolean>
        = settingsFlow.map {
            it.disableSwipe
        }.distinctUntilChanged().asLiveData()

    fun getBrowseDisableSwipe(cb : Consumer<Boolean>)
        = liveBrowseDisableSwipe.observeOnce(cb)

    fun setBrowseDisableSwipe(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDisableSwipe(value).build()
            }
        }
    }

    val livePlayScratchDisplay : LiveData<Boolean>
        = settingsFlow.map {
            it.displayScratch
        }.distinctUntilChanged().asLiveData()

    fun getPlayScratchDisplay(cb : Consumer<Boolean>)
        = livePlayScratchDisplay.observeOnce(cb)

    fun setPlayScratchDisplay(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDisplayScratch(value).build()
            }
        }
    }

    val livePlayDisplaySeparators : LiveData<DisplaySeparators>
        = settingsFlow.map {
            it.displaySeparators
        }.distinctUntilChanged().asLiveData()

    fun getPlayDisplaySeparators(cb : Consumer<DisplaySeparators>)
        = livePlayDisplaySeparators.observeOnce(cb)

    fun setPlayDisplaySeparators(value : DisplaySeparators) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDisplaySeparators(value).build()
            }
        }
    }

    val liveBrowseLastDownload : LiveData<Long>
        = settingsFlow.map {
            it.dlLast
        }.distinctUntilChanged().asLiveData()

    fun getBrowseLastDownload(cb : Consumer<Long>)
        = liveBrowseLastDownload.observeOnce(cb)

    fun getBrowseLastDownloadSync() : Long
        = runBlocking { settingsFlow.first().dlLast }

    fun setBrowseLastDownload(value : Long) {
        if (0 <= value && value <= System.currentTimeMillis()) {
            launch {
                settingsStore.updateData { settings ->
                    settings.toBuilder().setDlLast(value).build()
                }
            }
        }
    }

    val liveDownloadOnStartUp : LiveData<Boolean>
        = settingsFlow.map {
            it.dlOnStartup
        }.distinctUntilChanged().asLiveData()

    fun getDownloadOnStartUp(cb : Consumer<Boolean>)
        = liveDownloadOnStartUp.observeOnce(cb)

    fun setDownloadOnStartUp(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDlOnStartup(value).build()
            }
        }
    }

    val livePlayDoubleTapFitBoard : LiveData<Boolean>
        = settingsFlow.map {
            it.doubleTap
        }.distinctUntilChanged().asLiveData()

    fun getPlayDoubleTapFitBoard(cb : Consumer<Boolean>)
        = livePlayDoubleTapFitBoard.observeOnce(cb)

    fun setPlayDoubleTapFitBoard(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDoubleTap(value).build()
            }
        }
    }

    val liveDownload20Minutes : LiveData<Boolean>
        = settingsFlow.map {
            it.download20Minutes
        }.distinctUntilChanged().asLiveData()

    fun getDownload20Minutes(cb : Consumer<Boolean>)
        = liveDownload20Minutes.observeOnce(cb)

    fun setDownload20Minutes(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownload20Minutes(value).build()
            }
        }
    }

    val liveDownloadCustomDaily : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadCustomDaily
        }.distinctUntilChanged().asLiveData()

    fun getDownloadCustomDaily(cb : Consumer<Boolean>)
        = liveDownloadCustomDaily.observeOnce(cb)

    fun setDownloadCustomDaily(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadCustomDaily(value).build()
            }
        }
    }

    val liveDownloadDeStandaard : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadDeStandaard
        }.distinctUntilChanged().asLiveData()

    fun getDownloadDeStandaard(cb : Consumer<Boolean>)
        = liveDownloadDeStandaard.observeOnce(cb)

    fun setDownloadDeStandaard(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadDeStandaard(value).build()
            }
        }
    }

    val liveDownloadDeTelegraaf : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadDeTelegraaf
        }.distinctUntilChanged().asLiveData()

    fun getDownloadDeTelegraaf(cb : Consumer<Boolean>)
        = liveDownloadDeTelegraaf.observeOnce(cb)

    fun setDownloadDeTelegraaf(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadDeTelegraaf(value).build()
            }
        }
    }

    val liveDownloadGuardianDailyCryptic : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadGuardianDailyCryptic
        }.distinctUntilChanged().asLiveData()

    fun getDownloadGuardianDailyCryptic(cb : Consumer<Boolean>)
        = liveDownloadGuardianDailyCryptic.observeOnce(cb)

    fun setDownloadGuardianDailyCryptic(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setDownloadGuardianDailyCryptic(value)
                    .build()
            }
        }
    }

    val liveDownloadGuardianWeeklyQuiptic : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadGuardianWeeklyQuiptic
        }.distinctUntilChanged().asLiveData()

    fun getDownloadGuardianWeeklyQuiptic(cb : Consumer<Boolean>)
        = liveDownloadGuardianWeeklyQuiptic.observeOnce(cb)

    fun setDownloadGuardianWeeklyQuiptic(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setDownloadGuardianWeeklyQuiptic(value)
                    .build()
            }
        }
    }

    val liveDownloadHamAbend : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadHamAbend
        }.distinctUntilChanged().asLiveData()

    fun getDownloadHamAbend(cb : Consumer<Boolean>)
        = liveDownloadHamAbend.observeOnce(cb)

    fun setDownloadHamAbend(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadHamAbend(value).build()
            }
        }
    }

    val liveDownloadIndependentDailyCryptic : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadIndependentDailyCryptic
        }.distinctUntilChanged().asLiveData()

    fun getDownloadIndependentDailyCryptic(cb : Consumer<Boolean>)
        = liveDownloadIndependentDailyCryptic.observeOnce(cb)

    fun setDownloadIndependentDailyCryptic(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setDownloadIndependentDailyCryptic(value)
                    .build()
            }
        }
    }

    val liveDownloadIrishNewsCryptic : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadIrishNewsCryptic
        }.distinctUntilChanged().asLiveData()

    fun getDownloadIrishNewsCryptic(cb : Consumer<Boolean>)
        = liveDownloadIrishNewsCryptic.observeOnce(cb)

    fun setDownloadIrishNewsCryptic(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadIrishNewsCryptic(value).build()
            }
        }
    }

    val liveDownloadJonesin : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadJonesin
        }.distinctUntilChanged().asLiveData()

    fun getDownloadJonesin(cb : Consumer<Boolean>)
        = liveDownloadJonesin.observeOnce(cb)

    fun setDownloadJonesin(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadJonesin(value).build()
            }
        }
    }

    val liveDownloadJoseph : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadJoseph
        }.distinctUntilChanged().asLiveData()

    fun getDownloadJoseph(cb : Consumer<Boolean>)
        = liveDownloadJoseph.observeOnce(cb)

    fun setDownloadJoseph(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadJoseph(value).build()
            }
        }
    }

    val liveDownloadLeParisienF1 : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadLeParisienF1
        }.distinctUntilChanged().asLiveData()

    fun getDownloadLeParisienF1(cb : Consumer<Boolean>)
        = liveDownloadLeParisienF1.observeOnce(cb)

    fun setDownloadLeParisienF1(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadLeParisienF1(value).build()
            }
        }
    }

    val liveDownloadLeParisienF2 : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadLeParisienF2
        }.distinctUntilChanged().asLiveData()

    fun getDownloadLeParisienF2(cb : Consumer<Boolean>)
        = liveDownloadLeParisienF2.observeOnce(cb)

    fun setDownloadLeParisienF2(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadLeParisienF2(value).build()
            }
        }
    }

    val liveDownloadLeParisienF3 : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadLeParisienF3
        }.distinctUntilChanged().asLiveData()

    fun getDownloadLeParisienF3(cb : Consumer<Boolean>)
        = liveDownloadLeParisienF3.observeOnce(cb)

    fun setDownloadLeParisienF3(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadLeParisienF3(value).build()
            }
        }
    }

    val liveDownloadLeParisienF4 : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadLeParisienF4
        }.distinctUntilChanged().asLiveData()

    fun getDownloadLeParisienF4(cb : Consumer<Boolean>)
        = liveDownloadLeParisienF4.observeOnce(cb)

    fun setDownloadLeParisienF4(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadLeParisienF4(value).build()
            }
        }
    }

    val liveDownloadMetroCryptic : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadMetroCryptic
        }.distinctUntilChanged().asLiveData()

    fun getDownloadMetroCryptic(cb : Consumer<Boolean>)
        = liveDownloadMetroCryptic.observeOnce(cb)

    fun setDownloadMetroCryptic(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadMetroCryptic(value).build()
            }
        }
    }

    val liveDownloadMetroQuick : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadMetroQuick
        }.distinctUntilChanged().asLiveData()

    fun getDownloadMetroQuick(cb : Consumer<Boolean>)
        = liveDownloadMetroQuick.observeOnce(cb)

    fun setDownloadMetroQuick(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadMetroQuick(value).build()
            }
        }
    }

    val liveDownloadNewYorkTimesSyndicated : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadNewYorkTimesSyndicated
        }.distinctUntilChanged().asLiveData()

    fun getDownloadNewYorkTimesSyndicated(cb : Consumer<Boolean>)
        = liveDownloadNewYorkTimesSyndicated.observeOnce(cb)

    fun setDownloadNewYorkTimesSyndicated(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setDownloadNewYorkTimesSyndicated(value)
                    .build()
            }
        }
    }

    val liveDownloadNewsday : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadNewsday
        }.distinctUntilChanged().asLiveData()

    fun getDownloadNewsday(cb : Consumer<Boolean>)
        = liveDownloadNewsday.observeOnce(cb)

    fun setDownloadNewsday(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadNewsday(value).build()
            }
        }
    }

    val liveDownloadPremier : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadPremier
        }.distinctUntilChanged().asLiveData()

    fun getDownloadPremier(cb : Consumer<Boolean>)
        = liveDownloadPremier.observeOnce(cb)

    fun setDownloadPremier(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadPremier(value).build()
            }
        }
    }

    val liveDownloadSheffer : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadSheffer
        }.distinctUntilChanged().asLiveData()

    fun getDownloadSheffer(cb : Consumer<Boolean>)
        = liveDownloadSheffer.observeOnce(cb)

    fun setDownloadSheffer(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadSheffer(value).build()
            }
        }
    }

    val liveDownloadTimeout : LiveData<Int>
        = settingsFlow.map {
            it.downloadTimeout
        }.distinctUntilChanged().asLiveData()

    fun getDownloadTimeout(cb : Consumer<Int>)
        = liveDownloadTimeout.observeOnce(cb)

    fun setDownloadTimeout(value : Int) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadTimeout(value).build()
            }
        }
    }

    val liveDownloadUSAToday : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadUSAToday
        }.distinctUntilChanged().asLiveData()

    fun getDownloadUSAToday(cb : Consumer<Boolean>)
        = liveDownloadUSAToday.observeOnce(cb)

    fun setDownloadUSAToday(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadUSAToday(value).build()
            }
        }
    }

    val liveDownloadUniversal : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadUniversal
        }.distinctUntilChanged().asLiveData()

    fun getDownloadUniversal(cb : Consumer<Boolean>)
        = liveDownloadUniversal.observeOnce(cb)

    fun setDownloadUniversal(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadUniversal(value).build()
            }
        }
    }

    val liveDownloadWaPoSunday : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadWaPoSunday
        }.distinctUntilChanged().asLiveData()

    fun getDownloadWaPoSunday(cb : Consumer<Boolean>)
        = liveDownloadWaPoSunday.observeOnce(cb)

    fun setDownloadWaPoSunday(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadWaPoSunday(value).build()
            }
        }
    }

    val liveDownloadWsj : LiveData<Boolean>
        = settingsFlow.map {
            it.downloadWsj
        }.distinctUntilChanged().asLiveData()

    fun getDownloadWsj(cb : Consumer<Boolean>)
        = liveDownloadWsj.observeOnce(cb)

    fun setDownloadWsj(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDownloadWsj(value).build()
            }
        }
    }

    val liveExtDuckDuckGoEnabled : LiveData<Boolean>
        = settingsFlow.map {
            it.duckDuckGoEnabled
        }.distinctUntilChanged().asLiveData()

    fun getExtDuckDuckGoEnabled(cb : Consumer<Boolean>)
        = liveExtDuckDuckGoEnabled.observeOnce(cb)

    fun setExtDuckDuckGoEnabled(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setDuckDuckGoEnabled(value).build()
            }
        }
    }

    val livePlayEnsureVisible : LiveData<Boolean>
        = settingsFlow.map {
            it.ensureVisible
        }.distinctUntilChanged().asLiveData()

    fun getPlayEnsureVisible(cb : Consumer<Boolean>)
        = livePlayEnsureVisible.observeOnce(cb)

    fun setPlayEnsureVisible(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setEnsureVisible(value).build()
            }
        }
    }

    val livePlayEnterChangesDirection : LiveData<Boolean>
        = settingsFlow.map {
            it.enterChangesDirection
        }.distinctUntilChanged().asLiveData()

    fun getPlayEnterChangesDirection(cb : Consumer<Boolean>)
        = livePlayEnterChangesDirection.observeOnce(cb)

    fun setPlayEnterChangesDirection(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setEnterChangesDirection(value).build()
            }
        }
    }

    val liveVoiceEqualsAnnounceClue : LiveData<Boolean>
        = settingsFlow.map {
            it.equalsAnnounceClue
        }.distinctUntilChanged().asLiveData()

    fun getVoiceEqualsAnnounceClue(cb : Consumer<Boolean>)
        = liveVoiceEqualsAnnounceClue.observeOnce(cb)

    fun setVoiceEqualsAnnounceClue(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setEqualsAnnounceClue(value).build()
            }
        }
    }

    val liveExtDictionarySetting : LiveData<ExternalDictionarySetting>
        = settingsFlow.map {
            it.externalDictionary
        }.distinctUntilChanged().asLiveData()

    fun getExtDictionarySetting(cb : Consumer<ExternalDictionarySetting>)
        = liveExtDictionarySetting.observeOnce(cb)

    fun setExtDictionarySetting(value : ExternalDictionarySetting) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setExternalDictionary(value).build()
            }
        }
    }

    val liveExternalToolSettings : LiveData<ExternalToolSettings>
        = settingsFlow.map {
            ExternalToolSettings(
                it.chatGPTAPIKey,
                it.crosswordSolverEnabled,
                it.duckDuckGoEnabled,
                it.externalDictionary,
                it.fifteenSquaredEnabled,
            );
        }.distinctUntilChanged().asLiveData()

    fun getExternalToolSettings(cb : Consumer<ExternalToolSettings>)
        = liveExternalToolSettings.observeOnce(cb)

    val liveExtFifteenSquaredEnabled : LiveData<Boolean>
        = settingsFlow.map {
            it.fifteenSquaredEnabled
        }.distinctUntilChanged().asLiveData()

    fun getExtFifteenSquaredEnabled(cb : Consumer<Boolean>)
        = liveExtFifteenSquaredEnabled.observeOnce(cb)

    fun setExtFifteenSquaredEnabled(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setFifteenSquaredEnabled(value).build()
            }
        }
    }

    val liveFileHandlerSettings : LiveData<FileHandlerSettings>
        = settingsFlow.map {
            FileHandlerSettings(
                it.storageLocation,
                it.safRootUri,
                it.safCrosswordsFolderUri,
                it.safArchiveFolderUri,
                it.safToImportFolderUri,
                it.safToImportDoneFolderUri,
                it.safToImportFailedFolderUri,
            )
        }.distinctUntilChanged().asLiveData()

    fun getFileHandlerSettings(cb : Consumer<FileHandlerSettings>)
        = liveFileHandlerSettings.observeOnce(cb)

    fun setFileHandlerSettings(
        settings : FileHandlerSettings,
        cb : Runnable? = null,
    ) {
        launch {
            settingsStore.updateData {
                it.toBuilder()
                    .setStorageLocation(settings.storageLocation)
                    .setSafRootUri(settings.safRootURI)
                    .setSafCrosswordsFolderUri(settings.safCrosswordsURI)
                    .setSafArchiveFolderUri(settings.safArchiveURI)
                    .setSafToImportFolderUri(settings.safToImportURI)
                    .setSafToImportDoneFolderUri(settings.safToImportDoneURI)
                    .setSafToImportFailedFolderUri(
                        settings.safToImportFailedURI
                    ).build()
            }
            runMain(cb)
        }
    }

    fun setFileHandlerSettingsSync(
        settings : FileHandlerSettings,
        cb : Runnable? = null,
    ) {
        runBlocking {
            settingsStore.updateData {
                it.toBuilder()
                    .setStorageLocation(settings.storageLocation)
                    .setSafRootUri(settings.safRootURI)
                    .setSafCrosswordsFolderUri(settings.safCrosswordsURI)
                    .setSafArchiveFolderUri(settings.safArchiveURI)
                    .setSafToImportFolderUri(settings.safToImportURI)
                    .setSafToImportDoneFolderUri(settings.safToImportDoneURI)
                    .setSafToImportFailedFolderUri(
                        settings.safToImportFailedURI
                    ).build()
            }
            runMain(cb)
        }
    }

    val liveFileHandlerStorageLocation : LiveData<StorageLocation>
        = settingsFlow.map {
            it.storageLocation
        }.distinctUntilChanged().asLiveData()

    val livePlayFitToScreenMode : LiveData<FitToScreenMode>
        = settingsFlow.map {
            it.fitToScreenMode
        }.distinctUntilChanged().asLiveData()

    fun getPlayFitToScreenMode(cb : Consumer<FitToScreenMode>)
        = livePlayFitToScreenMode.observeOnce(cb)

    fun setPlayFitToScreenMode(value : FitToScreenMode) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setFitToScreenMode(value).build()
            }
        }
    }

    val livePlayFullScreen : LiveData<Boolean>
        = settingsFlow.map {
            it.fullScreen
        }.distinctUntilChanged().asLiveData()

    fun getPlayFullScreen(cb : Consumer<Boolean>)
        = livePlayFullScreen.observeOnce(cb)

    fun setPlayFullScreen(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setFullScreen(value).build()
            }
        }
    }

    val livePlayGridRatioPortrait : LiveData<GridRatio>
        = settingsFlow.map {
            it.gridRatio
        }.distinctUntilChanged().asLiveData()

    fun getPlayGridRatioPortrait(cb : Consumer<GridRatio>)
        = livePlayGridRatioPortrait.observeOnce(cb)

    fun setPlayGridRatioPortrait(value : GridRatio) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setGridRatio(value).build()
            }
        }
    }

    val livePlayGridRatioLandscape : LiveData<GridRatio>
        = settingsFlow.map {
            it.gridRatioLand
        }.distinctUntilChanged().asLiveData()

    fun getPlayGridRatioLandscape(cb : Consumer<GridRatio>)
        = livePlayGridRatioLandscape.observeOnce(cb)

    fun setPlayGridRatioLandscape(value : GridRatio) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setGridRatioLand(value).build()
            }
        }
    }

    val livePlayIndicateShowErrors : LiveData<Boolean>
        = settingsFlow.map {
            it.indicateShowErrors
        }.distinctUntilChanged().asLiveData()

    fun getPlayIndicateShowErrors(cb : Consumer<Boolean>)
        = livePlayIndicateShowErrors.observeOnce(cb)

    fun setPlayIndicateShowErrors(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setIndicateShowErrors(value).build()
            }
        }
    }

    val livePlayInferSeparators : LiveData<Boolean>
        = settingsFlow.map {
            it.inferSeparators
        }.distinctUntilChanged().asLiveData()

    fun getPlayInferSeparators(cb : Consumer<Boolean>)
        = livePlayInferSeparators.observeOnce(cb)

    fun setPlayInferSeparators(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setInferSeparators(value).build()
            }
        }
    }

    val liveKeyboardCompact : LiveData<Boolean>
        = settingsFlow.map {
            it.keyboardCompact
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardCompact(cb : Consumer<Boolean>)
        = liveKeyboardCompact.observeOnce(cb)

    fun setKeyboardCompact(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setKeyboardCompact(value).build()
            }
        }
    }

    val liveKeyboardForceCaps : LiveData<Boolean>
        = settingsFlow.map {
            it.keyboardForceCaps
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardForceCaps(cb : Consumer<Boolean>)
        = liveKeyboardForceCaps.observeOnce(cb)

    fun setKeyboardForceCaps(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setKeyboardForceCaps(value).build()
            }
        }
    }

    val liveKeyboardHaptic : LiveData<Boolean>
        = settingsFlow.map {
            it.keyboardHaptic
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardHaptic(cb : Consumer<Boolean>)
        = liveKeyboardHaptic.observeOnce(cb)

    fun setKeyboardHaptic(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setKeyboardHaptic(value).build()
            }
        }
    }

    val liveKeyboardHideButton : LiveData<Boolean>
        = settingsFlow.map {
            it.keyboardHideButton
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardHideButton(cb : Consumer<Boolean>)
        = liveKeyboardHideButton.observeOnce(cb)

    fun setKeyboardHideButton(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setKeyboardHideButton(value).build()
            }
        }
    }

    val liveKeyboardLayout : LiveData<KeyboardLayout>
        = settingsFlow.map {
            it.keyboardLayout
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardLayout(cb : Consumer<KeyboardLayout>)
        = liveKeyboardLayout.observeOnce(cb)

    fun setKeyboardLayout(value : KeyboardLayout) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setKeyboardLayout(value).build()
            }
        }
    }

    val liveKeyboardMode : LiveData<KeyboardMode>
        = settingsFlow.map {
            it.keyboardShowHide
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardMode(cb : Consumer<KeyboardMode>)
        = liveKeyboardMode.observeOnce(cb)

    fun setKeyboardMode(value : KeyboardMode) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setKeyboardShowHide(value).build()
            }
        }
    }

    val liveKeyboardRepeatDelay : LiveData<Int>
        = settingsFlow.map {
            it.keyboardRepeatDelay
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardRepeatDelay(cb : Consumer<Int>)
        = liveKeyboardRepeatDelay.observeOnce(cb)

    fun setKeyboardRepeatDelay(value : Int) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setKeyboardRepeatDelay(value).build()
            }
        }
    }

    val liveKeyboardRepeatInterval : LiveData<Int>
        = settingsFlow.map {
            it.keyboardRepeatInterval
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardRepeatInterval(cb : Consumer<Int>)
        = liveKeyboardRepeatInterval.observeOnce(cb)

    fun setKeyboardRepeatInterval(value : Int) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setKeyboardRepeatInterval(value).build()
            }
        }
    }

    val liveKeyboardSettings : LiveData<KeyboardSettings>
        = settingsFlow.map {
            KeyboardSettings(
                it.keyboardCompact,
                it.keyboardForceCaps,
                it.keyboardHaptic,
                it.keyboardHideButton,
                it.keyboardLayout,
                it.keyboardShowHide,
                it.keyboardRepeatDelay,
                it.keyboardRepeatInterval,
                it.useNativeKeyboard,
            )
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardSettings(cb : Consumer<KeyboardSettings>)
        = liveKeyboardSettings.observeOnce(cb)

    val liveKeyboardUseNative : LiveData<Boolean>
        = settingsFlow.map {
            it.useNativeKeyboard
        }.distinctUntilChanged().asLiveData()

    fun getKeyboardUseNative(cb : Consumer<Boolean>)
        = liveKeyboardUseNative.observeOnce(cb)

    fun setKeyboardUseNative(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setUseNativeKeyboard(value).build()
            }
        }
    }

    val liveBrowseLastSeenVersion : LiveData<String>
        = settingsFlow.map {
            it.lastSeenVersion
        }.distinctUntilChanged().asLiveData()

    fun getBrowseLastSeenVersion(cb : Consumer<String>)
        = liveBrowseLastSeenVersion.observeOnce(cb)

    fun setBrowseLastSeenVersion(value : String) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setLastSeenVersion(value).build()
            }
        }
    }

    val livePlayMovementStrategy : LiveData<MovementStrategy>
        = settingsFlow.map { settings ->
            val cycleForwards
                = settings.cycleUnfilledMode != CycleUnfilledMode.CU_NEVER
            val cycleBackwards
                = settings.cycleUnfilledMode == CycleUnfilledMode.CU_ALWAYS

            val strategy = when (settings.movementStrategy) {
                MovementStrategySetting.MSS_MOVE_NEXT_ON_AXIS
                    -> MovementStrategy.MOVE_NEXT_ON_AXIS
                MovementStrategySetting.MSS_STOP_ON_END
                    -> MovementStrategy.STOP_ON_END
                MovementStrategySetting.MSS_MOVE_NEXT_CLUE
                    -> MovementStrategy.MOVE_NEXT_CLUE
                MovementStrategySetting.MSS_MOVE_PARALLEL_WORD
                    -> MovementStrategy.MOVE_PARALLEL_WORD
                else -> MovementStrategy.MOVE_NEXT_ON_AXIS
            }

            if (cycleForwards || cycleBackwards) {
                MovementStrategy.CycleUnfilled(
                    strategy, cycleForwards, cycleBackwards
                )
            } else {
                strategy
            }
        }.distinctUntilChanged().asLiveData()

    fun getPlayMovementStrategy(cb : Consumer<MovementStrategy>)
        = livePlayMovementStrategy.observeOnce(cb)

    val livePlayMovementStrategySetting : LiveData<MovementStrategySetting>
        = settingsFlow.map {
            it.movementStrategy
        }.distinctUntilChanged().asLiveData()

    fun getPlayMovementStrategySetting(cb : Consumer<MovementStrategySetting>)
        = livePlayMovementStrategySetting.observeOnce(cb)

    fun setPlayMovementStrategySetting(value : MovementStrategySetting) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setMovementStrategy(value).build()
            }
        }
    }

    val liveAppOrientationLock : LiveData<Orientation>
        = settingsFlow.map {
            it.orientationLock
        }.distinctUntilChanged().asLiveData()

    fun getAppOrientationLock(cb : Consumer<Orientation>)
        = liveAppOrientationLock.observeOnce(cb)

    fun setAppOrientationLock(value : Orientation) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setOrientationLock(value).build()
            }
        }
    }

    val livePlayPlayLetterUndoEnabled : LiveData<Boolean>
        = settingsFlow.map {
            it.playLetterUndoEnabled
        }.distinctUntilChanged().asLiveData()

    fun getPlayPlayLetterUndoEnabled(cb : Consumer<Boolean>)
        = livePlayPlayLetterUndoEnabled.observeOnce(cb)

    fun setPlayPlayLetterUndoEnabled(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setPlayLetterUndoEnabled(value).build()
            }
        }
    }

    val livePlayPredictAnagramChars : LiveData<Boolean>
        = settingsFlow.map {
            it.predictAnagramChars
        }.distinctUntilChanged().asLiveData()

    fun getPlayPredictAnagramChars(cb : Consumer<Boolean>)
        = livePlayPredictAnagramChars.observeOnce(cb)

    fun setPlayPredictAnagramChars(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setPredictAnagramChars(value).build()
            }
        }
    }

    val livePlayPreserveCorrectLettersInShowErrors : LiveData<Boolean>
        = settingsFlow.map {
            it.preserveCorrectLettersInShowErrors
        }.distinctUntilChanged().asLiveData()

    fun getPlayPreserveCorrectLettersInShowErrors(cb : Consumer<Boolean>)
        = livePlayPreserveCorrectLettersInShowErrors.observeOnce(cb)

    fun setPlayPreserveCorrectLettersInShowErrors(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder()
                    .setPreserveCorrectLettersInShowErrors(value)
                    .build()
            }
        }
    }

    val livePlayRandomClueOnShake : LiveData<Boolean>
        = settingsFlow.map {
            it.randomClueOnShake
        }.distinctUntilChanged().asLiveData()

    fun getPlayRandomClueOnShake(cb : Consumer<Boolean>)
        = livePlayRandomClueOnShake.observeOnce(cb)

    fun setPlayRandomClueOnShake(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setRandomClueOnShake(value).build()
            }
        }
    }

    val liveFileHandlerSafArchive : LiveData<String>
        = settingsFlow.map {
            it.safArchiveFolderUri
        }.distinctUntilChanged().asLiveData()

    fun getFileHandlerSafArchive(cb : Consumer<String>)
        = liveFileHandlerSafArchive.observeOnce(cb)

    val liveFileHandlerSafCrosswords : LiveData<String>
        = settingsFlow.map {
            it.safCrosswordsFolderUri
        }.distinctUntilChanged().asLiveData()

    fun getFileHandlerSafCrosswords(cb : Consumer<String>)
        = liveFileHandlerSafCrosswords.observeOnce(cb)

    val liveFileHandlerSafRoot : LiveData<String>
        = settingsFlow.map {
            it.safRootUri
        }.distinctUntilChanged().asLiveData()

    fun getFileHandlerSafRoot(cb : Consumer<String>)
        = liveFileHandlerSafRoot.observeOnce(cb)

    val liveFileHandlerSafToImportDone : LiveData<String>
        = settingsFlow.map {
            it.safToImportDoneFolderUri
        }.distinctUntilChanged().asLiveData()

    fun getFileHandlerSafToImportDone(cb : Consumer<String>)
        = liveFileHandlerSafToImportDone.observeOnce(cb)

    val liveFileHandlerSafToImportFailed : LiveData<String>
        = settingsFlow.map {
            it.safToImportFailedFolderUri
        }.distinctUntilChanged().asLiveData()

    fun getFileHandlerSafToImportFailed(cb : Consumer<String>)
        = liveFileHandlerSafToImportFailed.observeOnce(cb)

    val liveFileHandlerSafToImport : LiveData<String>
        = settingsFlow.map {
            it.safToImportFolderUri
        }.distinctUntilChanged().asLiveData()

    fun getFileHandlerSafToImport(cb : Consumer<String>)
        = liveFileHandlerSafToImport.observeOnce(cb)

    val livePlayRenderSettings : LiveData<RenderSettings>
        = settingsFlow.map {
            RenderSettings(
                it.displayScratch,
                it.suppressHints,
                it.displaySeparators,
                it.inferSeparators,
            )
        }.distinctUntilChanged().asLiveData()

    fun getPlayRenderSettings(cb : Consumer<RenderSettings>)
        = livePlayRenderSettings.observeOnce(cb)

    val livePlayScale : LiveData<Float>
        = settingsFlow.map {
            it.scale
        }.distinctUntilChanged().asLiveData()

    fun getPlayScale(cb : Consumer<Float>)
        = livePlayScale.observeOnce(cb)

    fun setPlayScale(value : Float) {
        if (0 <= value) {
            launch {
                settingsStore.updateData { settings ->
                    settings.toBuilder().setScale(value).build()
                }
            }
        }
    }

    val liveScrapeCru : LiveData<Boolean>
        = settingsFlow.map {
            it.scrapeCru
        }.distinctUntilChanged().asLiveData()

    fun getScrapeCru(cb : Consumer<Boolean>)
        = liveScrapeCru.observeOnce(cb)

    fun setScrapeCru(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setScrapeCru(value).build()
            }
        }
    }

    val liveScrapeEveryman : LiveData<Boolean>
        = settingsFlow.map {
            it.scrapeEveryman
        }.distinctUntilChanged().asLiveData()

    fun getScrapeEveryman(cb : Consumer<Boolean>)
        = liveScrapeEveryman.observeOnce(cb)

    fun setScrapeEveryman(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setScrapeEveryman(value).build()
            }
        }
    }

    val liveScrapeGuardianQuick : LiveData<Boolean>
        = settingsFlow.map {
            it.scrapeGuardianQuick
        }.distinctUntilChanged().asLiveData()

    fun getScrapeGuardianQuick(cb : Consumer<Boolean>)
        = liveScrapeGuardianQuick.observeOnce(cb)

    fun setScrapeGuardianQuick(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setScrapeGuardianQuick(value).build()
            }
        }
    }

    val liveScrapeKegler : LiveData<Boolean>
        = settingsFlow.map {
            it.scrapeKegler
        }.distinctUntilChanged().asLiveData()

    fun getScrapeKegler(cb : Consumer<Boolean>)
        = liveScrapeKegler.observeOnce(cb)

    fun setScrapeKegler(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setScrapeKegler(value).build()
            }
        }
    }

    val liveScrapePrivateEye : LiveData<Boolean>
        = settingsFlow.map {
            it.scrapePrivateEye
        }.distinctUntilChanged().asLiveData()

    fun getScrapePrivateEye(cb : Consumer<Boolean>)
        = liveScrapePrivateEye.observeOnce(cb)

    fun setScrapePrivateEye(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setScrapePrivateEye(value).build()
            }
        }
    }

    val liveScrapePrzekroj : LiveData<Boolean>
        = settingsFlow.map {
            it.scrapePrzekroj
        }.distinctUntilChanged().asLiveData()

    fun getScrapePrzekroj(cb : Consumer<Boolean>)
        = liveScrapePrzekroj.observeOnce(cb)

    fun setScrapePrzekroj(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setScrapePrzekroj(value).build()
            }
        }
    }

    val livePlayScratchMode : LiveData<Boolean>
        = settingsFlow.map {
            it.scratchMode
        }.distinctUntilChanged().asLiveData()

    fun getPlayScratchMode(cb : Consumer<Boolean>)
        = livePlayScratchMode.observeOnce(cb)

    fun setPlayScratchMode(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setScratchMode(value).build()
            }
        }
    }

    val livePlayShowCluesTab : LiveData<Boolean>
        = settingsFlow.map {
            it.showCluesOnPlayScreen
        }.distinctUntilChanged().asLiveData()

    fun getPlayShowCluesTab(cb : Consumer<Boolean>)
        = livePlayShowCluesTab.observeOnce(cb)

    fun setPlayShowCluesTab(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setShowCluesOnPlayScreen(value).build()
            }
        }
    }

    val livePlayShowCount : LiveData<Boolean>
        = settingsFlow.map {
            it.showCount
        }.distinctUntilChanged().asLiveData()

    fun getPlayShowCount(cb : Consumer<Boolean>)
        = livePlayShowCount.observeOnce(cb)

    fun setPlayShowCount(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setShowCount(value).build()
            }
        }
    }

    val livePlayShowErrorsGrid : LiveData<Boolean>
        = settingsFlow.map {
            it.showErrors
        }.distinctUntilChanged().asLiveData()

    fun getPlayShowErrorsGrid(cb : Consumer<Boolean>)
        = livePlayShowErrorsGrid.observeOnce(cb)

    fun setPlayShowErrorsGrid(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setShowErrors(value).build()
            }
        }
    }

    val livePlayShowErrorsClue : LiveData<Boolean>
        = settingsFlow.map {
            it.showErrorsClue
        }.distinctUntilChanged().asLiveData()

    fun getPlayShowErrorsClue(cb : Consumer<Boolean>)
        = livePlayShowErrorsClue.observeOnce(cb)

    fun setPlayShowErrorsClue(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setShowErrorsClue(value).build()
            }
        }
    }

    val livePlayShowErrorsCursor : LiveData<Boolean>
        = settingsFlow.map {
            it.showErrorsCursor
        }.distinctUntilChanged().asLiveData()

    fun getPlayShowErrorsCursor(cb : Consumer<Boolean>)
        = livePlayShowErrorsCursor.observeOnce(cb)

    fun setPlayShowErrorsCursor(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setShowErrorsCursor(value).build()
            }
        }
    }

    val livePlayShowTimer : LiveData<Boolean>
        = settingsFlow.map {
            it.showTimer
        }.distinctUntilChanged().asLiveData()

    fun getPlayShowTimer(cb : Consumer<Boolean>)
        = livePlayShowTimer.observeOnce(cb)

    fun setPlayShowTimer(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setShowTimer(value).build()
            }
        }
    }

    val liveClueListShowWords : LiveData<Boolean>
        = settingsFlow.map {
            it.showWordsInClueList
        }.distinctUntilChanged().asLiveData()

    fun getClueListShowWords(cb : Consumer<Boolean>)
        = liveClueListShowWords.observeOnce(cb)

    fun setClueListShowWords(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setShowWordsInClueList(value).build()
            }
        }
    }

    val livePlaySkipFilled : LiveData<Boolean>
        = settingsFlow.map {
            it.skipFilled
        }.distinctUntilChanged().asLiveData()

    fun getPlaySkipFilled(cb : Consumer<Boolean>)
        = livePlaySkipFilled.observeOnce(cb)

    fun setPlaySkipFilled(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setSkipFilled(value).build()
            }
        }
    }

    val liveRatingsSettings : LiveData<RatingsSettings>
        = settingsFlow.map {
            RatingsSettings(
                it.disableRatings,
                it.browseAlwaysShowRating,
            )
        }.distinctUntilChanged().asLiveData()

    val liveClueListSnapToClue : LiveData<Boolean>
        = settingsFlow.map {
            it.snapClue
        }.distinctUntilChanged().asLiveData()

    fun getClueListSnapToClue(cb : Consumer<Boolean>)
        = liveClueListSnapToClue.observeOnce(cb)

    fun setClueListSnapToClue(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setSnapClue(value).build()
            }
        }
    }

    val liveBrowseSort : LiveData<Accessor>
        = settingsFlow.map {
            when (it.sort) {
                AccessorSetting.AS_SOURCE -> Accessor.SOURCE
                AccessorSetting.AS_DATE_ASC -> Accessor.DATE_ASC
                else -> Accessor.DATE_DESC
            }
        }.distinctUntilChanged().asLiveData()

    fun getBrowseSort(cb : Consumer<Accessor>)
        = liveBrowseSort.observeOnce(cb)

    fun setBrowseSort(value : Accessor)
        = setBrowseSortSetting(convertAccessor(value))

    fun setBrowseSortSetting(value : AccessorSetting) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setSort(value).build()
            }
        }
    }

    private fun convertAccessor(value : Accessor) : AccessorSetting
        = if (value == Accessor.SOURCE)
            AccessorSetting.AS_SOURCE
        else if (value == Accessor.DATE_ASC)
            AccessorSetting.AS_DATE_ASC
        else /* DATE_DESC */
            AccessorSetting.AS_DATE_DESC

    val livePlaySpaceChangesDirection : LiveData<Boolean>
        = settingsFlow.map {
            it.spaceChangesDirection
        }.distinctUntilChanged().asLiveData()

    fun getPlaySpaceChangesDirection(cb : Consumer<Boolean>)
        = livePlaySpaceChangesDirection.observeOnce(cb)

    fun setPlaySpaceChangesDirection(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setSpaceChangesDirection(value).build()
            }
        }
    }

    val livePlaySuppressHintHighlighting : LiveData<Boolean>
        = settingsFlow.map {
            it.suppressHints
        }.distinctUntilChanged().asLiveData()

    fun getPlaySuppressHintHighlighting(cb : Consumer<Boolean>)
        = livePlaySuppressHintHighlighting.observeOnce(cb)

    fun setPlaySuppressHintHighlighting(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setSuppressHints(value).build()
            }
        }
    }

    val liveDownloadSuppressMessages : LiveData<Boolean>
        = settingsFlow.map {
            it.suppressMessages
        }.distinctUntilChanged().asLiveData()

    fun getDownloadSuppressIndividualNotifications(cb : Consumer<Boolean>)
        = liveDownloadSuppressMessages.observeOnce(cb)

    fun setDownloadSuppressMessages(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setSuppressMessages(value).build()
            }
        }
    }

    val liveDownloadSuppressSummaryMessages : LiveData<Boolean>
        = settingsFlow.map {
            it.suppressSummaryMessages
        }.distinctUntilChanged().asLiveData()

    fun getDownloadSuppressSummaryMessages(cb : Consumer<Boolean>)
        = liveDownloadSuppressSummaryMessages.observeOnce(cb)

    fun setDownloadSuppressSummaryMessages(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setSuppressSummaryMessages(value).build()
            }
        }
    }

    fun disableNotifications() {
        setDownloadSuppressMessages(true)
        setDownloadSuppressSummaryMessages(true)
    }

    val liveBrowseSwipeAction : LiveData<BrowseSwipeAction>
        = settingsFlow.map {
            it.swipeAction
        }.distinctUntilChanged().asLiveData()

    fun getBrowseSwipeAction(cb : Consumer<BrowseSwipeAction>)
        = liveBrowseSwipeAction.observeOnce(cb)

    fun setBrowseSwipeAction(value : BrowseSwipeAction) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setSwipeAction(value).build()
            }
        }
    }

    val livePlayToggleBeforeMove : LiveData<Boolean>
        = settingsFlow.map {
            it.toggleBeforeMove
        }.distinctUntilChanged().asLiveData()

    fun getPlayToggleBeforeMove(cb : Consumer<Boolean>)
        = livePlayToggleBeforeMove.observeOnce(cb)

    fun setPlayToggleBeforeMove(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setToggleBeforeMove(value).build()
            }
        }
    }

    val liveAppDayNightMode : LiveData<DayNightMode>
        = settingsFlow.map {
            it.uiTheme
        }.distinctUntilChanged().asLiveData()

    fun getAppDayNightMode(cb : Consumer<DayNightMode>)
        = liveAppDayNightMode.observeOnce(cb)

    fun getAppDayNightModeSync() : DayNightMode
        = runBlocking { settingsFlow.first().uiTheme }

    fun setAppDayNightMode(value : DayNightMode, cb : Runnable? = null) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setUiTheme(value).build()
            }
            runMain(cb)
        }
    }

    fun setAppDayNightModeSync(value : DayNightMode, cb : Runnable? = null) {
        runBlocking {
            settingsStore.updateData { settings ->
                settings.toBuilder().setUiTheme(value).build()
            }
            cb?.run()
        }
    }

    val liveVoiceVolumeActivatesVoice : LiveData<Boolean>
        = settingsFlow.map {
            it.volumeActivatesVoice
        }.distinctUntilChanged().asLiveData()

    fun getVoiceVolumeActivatesVoice(cb : Consumer<Boolean>)
        = liveVoiceVolumeActivatesVoice.observeOnce(cb)

    fun setVoiceVolumeActivatesVoice(value : Boolean) {
        launch {
            settingsStore.updateData { settings ->
                settings.toBuilder().setVolumeActivatesVoice(value).build()
            }
        }
    }

    @WorkerThread
    fun exportSettings(outputStream : OutputStream?) {
        if (outputStream == null)
            return

        try {
            val settings = runBlocking { settingsFlow.first() }
            val json = JSONObject()

            doExportImport(json, settings)

            // don't close but flush, it's the caller's job on the
            // outputStream
            val writer = PrintWriter(
                BufferedWriter(
                    OutputStreamWriter(
                        outputStream, WRITE_CHARSET
                    )
                )
            )
            writer.print(json.toString())
            writer.flush()
        } catch (e : JSONException) {
            throw IOException("JSON error exporting settings", e)
        }
    }

    @WorkerThread
    fun importSettings(inputStream : InputStream?) {
        if (inputStream == null)
            return

        try {
            val json = JSONUtils.streamToJSON(inputStream)
            doExportImport(json)
        } catch (e : JSONException) {
            throw IOException("JSON error importing settings", e)
        }
    }

    fun migrateAutoDownloaders(downloaders : Downloaders) {
        // TODO
    }

    fun migrateThemePreferences() {
        // TODO
    }

    fun migrateDontDeleteCrossing() {
        // TODO
    }

    fun migrateCycleUnfilled() {
        // TODO
    }

    fun migrateFitToScreen() {
        // TODO
    }

    fun migrateLegacyBackgroundDownloads(cb : Consumer<Boolean>) {
        // TODO
    }

    private fun runMain(runnable : Runnable?) {
        runnable?.let { mainScope.launch { it.run() } }
    }

    private fun addListToJSON(
        json : JSONObject, key : String, items : List<*>
    ) {
        val array = JSONArray()
        items.forEach { array.put(it) }
        json.put(key, array)
    }

    private fun getStringSetFromJSON(
        json : JSONObject,
        key : String,
        setFun : (Set<String>) -> Unit,
    ) {
        json.optJSONArray(key)?.let { arr ->
            var items = mutableSetOf<String>()
            for (i in 0..(arr.length() - 1))
                arr.optString(i)?.let { items.add(it) }
            setFun(items)
        }
    }

    /**
     * Export or import settings to/from json
     *
     * Combined to avoid key constants.
     *
     * @param settings non-null if export wanted
     */
    private fun doExportImport(
        json : JSONObject,
        settings : Settings? = null,
    ) {
        val run : (
            String,
            (Settings, String) -> Unit,
            (String) -> Unit
        ) -> Unit = { key, exportFun, importFun ->
            if (settings != null)
                exportFun(settings, key)
            else
                importFun(key)
        }

        run(
            "alwaysAnnounceBox",
            { s, key -> json.put(key, s.alwaysAnnounceBox) },
            { key ->
                json.optBoolean(key)?.let {setVoiceAlwaysAnnounceBox(it) }
            },
        )
        run(
            "alwaysAnnounceClue",
            { s, key -> json.put(key, s.alwaysAnnounceClue) },
            { key ->
                json.optBoolean(key)?.let { setVoiceAlwaysAnnounceClue(it) }
            },
        )
        run(
            "applicationTheme",
            { s, key -> json.put(key, s.applicationTheme.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setAppTheme(Theme.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "archiveCleanupAge",
            { s, key -> json.put(key, s.archiveCleanupAge) },
            { key ->
                json.optInt(key)?.let { setBrowseCleanupAgeArchive(it) }
            },
        )
        run(
            "autoDownloaders",
            { s, key -> addListToJSON(json, key, s.autoDownloadersList) },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadAutoDownloaders,
                )
            },
        )
        run(
            "backgroundDownloadAllowRoaming",
            { s, key -> json.put(key, s.backgroundDownloadAllowRoaming) },
            { key ->
                json.optBoolean(key)?.let { setDownloadAllowRoaming(it) }
            },
        )
        run(
            "backgroundDownloadDays",
            { s, key ->
                addListToJSON(json, key, s.backgroundDownloadDaysList)
            },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadDays,
                )
            },
        )
        run(
            "backgroundDownloadDaysTime",
            { s, key -> json.put(key, s.backgroundDownloadDaysTime) },
            { key -> json.optInt(key)?.let { setDownloadDaysTime(it) } },
        )
        run(
            "backgroundDownloadHourly",
            { s, key -> json.put(key, s.backgroundDownloadHourly) },
            { key -> json.optBoolean(key)?.let { setDownloadHourly(it) } },
        )
        run(
            "backgroundDownloadRequireCharging",
            { s, key -> json.put(key, s.backgroundDownloadRequireCharging) },
            { key ->
                json.optBoolean(key)?.let { setDownloadRequireCharging(it) }
            },
        )
        run(
            "backgroundDownloadRequireUnmetered",
            { s, key -> json.put(key, s.backgroundDownloadRequireUnmetered) },
            { key ->
                json.optBoolean(key)?.let { setDownloadRequireUnmetered(it) }
            },
        )
        run(
            "browseAlwaysShowRating",
            { s, key -> json.put(key, s.browseAlwaysShowRating) },
            { key ->
                json.optBoolean(key)?.let { setBrowseAlwaysShowRating(it) }
            },
        )
        run(
            "browseIndicateIfSolution",
            { s, key -> json.put(key, s.browseIndicateIfSolution) },
            { key ->
                json.optBoolean(key)?.let { setBrowseIndicateIfSolution(it) }
            },
        )
        run(
            "browseNewPuzzle",
            { s, key -> json.put(key, s.browseNewPuzzle) },
            { key -> json.optBoolean(key)?.let { setBrowseNewPuzzle(it) } },
        )
        run(
            "browseShowPercentageCorrect",
            { s, key -> json.put(key, s.browseShowPercentageCorrect) },
            { key ->
                json.optBoolean(key)?.let { setBrowseShowPercentageCorrect(it) }
            },
        )
        run(
            "buttonActivatesVoice",
            { s, key -> json.put(key, s.buttonActivatesVoice) },
            { key ->
                json.optBoolean(key)?.let { setVoiceButtonActivatesVoice(it) }
            },
        )
        run(
            "buttonAnnounceClue",
            { s, key -> json.put(key, s.buttonAnnounceClue) },
            { key ->
                json.optBoolean(key)?.let { setVoiceButtonAnnounceClue(it) }
            },
        )
        run(
            "chatGPTAPIKey",
            { s, key -> json.put(key, s.chatGPTAPIKey) },
            { key -> json.optString(key)?.let { setExtChatGPTAPIKey(it) } },
        )
        run(
            "cleanupAge",
            { s, key -> json.put(key, s.cleanupAge) },
            { key -> json.optInt(key)?.let { setBrowseCleanupAge(it) } },
        )
        run(
            "clueBelowGrid",
            { s, key -> json.put(key, s.clueBelowGrid) },
            { key -> json.optBoolean(key)?.let { setPlayClueBelowGrid(it) } },
        )
        run(
            "clueHighlight",
            { s, key -> json.put(key, s.clueHighlight.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setPlayClueHighlight(ClueHighlight.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "clueTabsDouble",
            { s, key -> json.put(key, s.clueTabsDouble.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setClueListClueTabsDouble(ClueTabsDouble.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "crosswordSolverEnabled",
            { s, key -> json.put(key, s.crosswordSolverEnabled) },
            { key ->
                json.optBoolean(key)?.let { setExtCrosswordSolverEnabled(it) }
            },
        )
        run(
            "customDailyTitle",
            { s, key -> json.put(key, s.customDailyTitle) },
            { key ->
                json.optString(key)?.let { setDownloadCustomDailyTitle(it) }
            },
        )
        run(
            "customDailyUrl",
            { s, key -> json.put(key, s.customDailyUrl) },
            { key ->
                json.optString(key)?.let { setDownloadCustomDailyURL(it) }
            },
        )
        run(
            "cycleUnfilledMode",
            { s, key -> json.put(key, s.cycleUnfilledMode.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setPlayCycleUnfilledMode(CycleUnfilledMode.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "deleteCrossingMode",
            { s, key -> json.put(key, s.deleteCrossingMode.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setPlayDeleteCrossingModeSetting(
                           DeleteCrossingModeSetting.valueOf(it)
                       )
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "deleteOnCleanup",
            { s, key -> json.put(key, s.deleteOnCleanup) },
            { key ->
                json.optBoolean(key)?.let { setBrowseDeleteOnCleanup(it) }
            },
        )
        run(
            "disableRatings",
            { s, key -> json.put(key, s.disableRatings) },
            { key ->
                json.optBoolean(key)?.let { setRatingsDisableRatings(it) }
            },
        )
        run(
            "disableSwipe",
            { s, key -> json.put(key, s.disableSwipe) },
            { key -> json.optBoolean(key)?.let { setBrowseDisableSwipe(it) } },
        )
        run(
            "displayScratch",
            { s, key -> json.put(key, s.displayScratch) },
            { key -> json.optBoolean(key)?.let { setPlayScratchDisplay(it) } },
        )
        run(
            "displaySeparators",
            { s, key -> json.put(key, s.displaySeparators.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setPlayDisplaySeparators(DisplaySeparators.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "dlLast",
            { s, key -> json.put(key, s.dlLast) },
            { key -> json.optLong(key)?.let { setBrowseLastDownload(it) } },
        )
        run(
            "dlOnStartup",
            { s, key -> json.put(key, s.dlOnStartup) },
            { key -> json.optBoolean(key)?.let { setDownloadOnStartUp(it) } },
        )
        run(
            "doubleTap",
            { s, key -> json.put(key, s.doubleTap) },
            { key ->
                json.optBoolean(key)?.let { setPlayDoubleTapFitBoard(it) }
            },
        )
        run(
            "download20Minutes",
            { s, key -> json.put(key, s.download20Minutes) },
            { key -> json.optBoolean(key)?.let { setDownload20Minutes(it) } },
        )
        run(
            "downloadCustomDaily",
            { s, key -> json.put(key, s.downloadCustomDaily) },
            { key -> json.optBoolean(key)?.let { setDownloadCustomDaily(it) } },
        )
        run(
            "downloadDeStandaard",
            { s, key -> json.put(key, s.downloadDeStandaard) },
            { key -> json.optBoolean(key)?.let { setDownloadDeStandaard(it) } },
        )
        run(
            "downloadDeTelegraaf",
            { s, key -> json.put(key, s.downloadDeTelegraaf) },
            { key -> json.optBoolean(key)?.let { setDownloadDeTelegraaf(it) } },
        )
        run(
            "downloadGuardianDailyCryptic",
            { s, key -> json.put(key, s.downloadGuardianDailyCryptic) },
            { key ->
                json.optBoolean(key)?.let {
                    setDownloadGuardianDailyCryptic(it)
                }
            },
        )
        run(
            "downloadGuardianWeeklyQuiptic",
            { s, key -> json.put(key, s.downloadGuardianWeeklyQuiptic) },
            { key ->
                json.optBoolean(key)?.let {
                    setDownloadGuardianWeeklyQuiptic(it)
                }
            },
        )
        run(
            "downloadHamAbend",
            { s, key -> json.put(key, s.downloadHamAbend) },
            { key -> json.optBoolean(key)?.let { setDownloadHamAbend(it) } },
        )
        run(
            "downloadIndependentDailyCryptic",
            { s, key -> json.put(key, s.downloadIndependentDailyCryptic) },
            { key ->
                json.optBoolean(key)?.let {
                    setDownloadIndependentDailyCryptic(it)
                }
            },
        )
        run(
            "downloadIrishNewsCryptic",
            { s, key -> json.put(key, s.downloadIrishNewsCryptic) },
            { key ->
                json.optBoolean(key)?.let { setDownloadIrishNewsCryptic(it) }
            },
        )
        run(
            "downloadJonesin",
            { s, key -> json.put(key, s.downloadJonesin) },
            { key -> json.optBoolean(key)?.let { setDownloadJonesin(it) } },
        )
        run(
            "downloadJoseph",
            { s, key -> json.put(key, s.downloadJoseph) },
            { key -> json.optBoolean(key)?.let { setDownloadJoseph(it) } },
        )
        run(
            "downloadLeParisienF1",
            { s, key -> json.put(key, s.downloadLeParisienF1) },
            { key ->
                json.optBoolean(key)?.let { setDownloadLeParisienF1(it) }
            },
        )
        run(
            "downloadLeParisienF2",
            { s, key -> json.put(key, s.downloadLeParisienF2) },
            { key ->
                json.optBoolean(key)?.let { setDownloadLeParisienF2(it) }
            },
        )
        run(
            "downloadLeParisienF3",
            { s, key -> json.put(key, s.downloadLeParisienF3) },
            { key ->
                json.optBoolean(key)?.let { setDownloadLeParisienF3(it) }
            },
        )
        run(
            "downloadLeParisienF4",
            { s, key -> json.put(key, s.downloadLeParisienF4) },
            { key ->
                json.optBoolean(key)?.let { setDownloadLeParisienF4(it) }
            },
        )
        run(
            "downloadMetroCryptic",
            { s, key -> json.put(key, s.downloadMetroCryptic) },
            { key ->
                json.optBoolean(key)?.let { setDownloadMetroCryptic(it) }
            },
        )
        run(
            "downloadMetroQuick",
            { s, key -> json.put(key, s.downloadMetroQuick) },
            { key -> json.optBoolean(key)?.let { setDownloadMetroQuick(it) } },
        )
        run(
            "downloadNewYorkTimesSyndicated",
            { s, key -> json.put(key, s.downloadNewYorkTimesSyndicated) },
            { key ->
                json.optBoolean(key)?.let {
                    setDownloadNewYorkTimesSyndicated(it)
                }
            },
        )
        run(
            "downloadNewsday",
            { s, key -> json.put(key, s.downloadNewsday) },
            { key -> json.optBoolean(key)?.let { setDownloadNewsday(it) } },
        )
        run(
            "downloadPremier",
            { s, key -> json.put(key, s.downloadPremier) },
            { key -> json.optBoolean(key)?.let { setDownloadPremier(it) } },
        )
        run(
            "downloadSheffer",
            { s, key -> json.put(key, s.downloadSheffer) },
            { key -> json.optBoolean(key)?.let { setDownloadSheffer(it) } },
        )
        run(
            "downloadTimeout",
            { s, key -> json.put(key, s.downloadTimeout) },
            { key -> json.optInt(key)?.let { setDownloadTimeout(it) } },
        )
        run(
            "downloadUSAToday",
            { s, key -> json.put(key, s.downloadUSAToday) },
            { key -> json.optBoolean(key)?.let { setDownloadUSAToday(it) } },
        )
        run(
            "downloadUniversal",
            { s, key -> json.put(key, s.downloadUniversal) },
            { key -> json.optBoolean(key)?.let { setDownloadUniversal(it) } },
        )
        run(
            "downloadWaPoSunday",
            { s, key -> json.put(key, s.downloadWaPoSunday) },
            { key -> json.optBoolean(key)?.let { setDownloadWaPoSunday(it) } },
        )
        run(
            "downloadWsj",
            { s, key -> json.put(key, s.downloadWsj) },
            { key -> json.optBoolean(key)?.let { setDownloadWsj(it) } },
        )
        run(
            "duckDuckGoEnabled",
            { s, key -> json.put(key, s.duckDuckGoEnabled) },
            { key ->
                json.optBoolean(key)?.let { setExtDuckDuckGoEnabled(it) }
            },
        )
        run(
            "ensureVisible",
            { s, key -> json.put(key, s.ensureVisible) },
            { key -> json.optBoolean(key)?.let { setPlayEnsureVisible(it) } },
        )
        run(
            "enterChangesDirection",
            { s, key -> json.put(key, s.enterChangesDirection) },
            { key ->
                json.optBoolean(key)?.let { setPlayEnterChangesDirection(it) }
            },
        )
        run(
            "equalsAnnounceClue",
            { s, key -> json.put(key, s.equalsAnnounceClue) },
            { key ->
                json.optBoolean(key)?.let { setVoiceEqualsAnnounceClue(it) }
            },
        )
        run(
            "externalDictionary",
            { s, key -> json.put(key, s.externalDictionary.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setExtDictionarySetting(
                           ExternalDictionarySetting.valueOf(it)
                       )
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "fifteenSquaredEnabled",
            { s, key -> json.put(key, s.fifteenSquaredEnabled) },
            { key ->
                json.optBoolean(key)?.let { setExtFifteenSquaredEnabled(it) }
            },
        )
        run(
            "fitToScreenMode",
            { s, key -> json.put(key, s.fitToScreenMode.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setPlayFitToScreenMode(FitToScreenMode.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "fullScreen",
            { s, key -> json.put(key, s.fullScreen) },
            { key -> json.optBoolean(key)?.let { setPlayFullScreen(it) } },
        )
        run(
            "gridRatio",
            { s, key -> json.put(key, s.gridRatio.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setPlayGridRatioPortrait(GridRatio.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "gridRatioLand",
            { s, key -> json.put(key, s.gridRatioLand.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setPlayGridRatioLandscape(GridRatio.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "indicateShowErrors",
            { s, key -> json.put(key, s.indicateShowErrors) },
            { key ->
                json.optBoolean(key)?.let { setPlayIndicateShowErrors(it) }
            },
        )
        run(
            "inferSeparators",
            { s, key -> json.put(key, s.inferSeparators) },
            { key -> json.optBoolean(key)?.let { setPlayInferSeparators(it) } },
        )
        run(
            "keyboardCompact",
            { s, key -> json.put(key, s.keyboardCompact) },
            { key -> json.optBoolean(key)?.let { setKeyboardCompact(it) } },
        )
        run(
            "keyboardForceCaps",
            { s, key -> json.put(key, s.keyboardForceCaps) },
            { key -> json.optBoolean(key)?.let { setKeyboardForceCaps(it) } },
        )
        run(
            "keyboardHaptic",
            { s, key -> json.put(key, s.keyboardHaptic) },
            { key -> json.optBoolean(key)?.let { setKeyboardHaptic(it) } },
        )
        run(
            "keyboardHideButton",
            { s, key -> json.put(key, s.keyboardHideButton) },
            { key -> json.optBoolean(key)?.let { setKeyboardHideButton(it) } },
        )
        run(
            "keyboardLayout",
            { s, key -> json.put(key, s.keyboardLayout.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setKeyboardLayout(KeyboardLayout.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "keyboardRepeatDelay",
            { s, key -> json.put(key, s.keyboardRepeatDelay) },
            { key -> json.optInt(key)?.let { setKeyboardRepeatDelay(it) } },
        )
        run(
            "keyboardRepeatInterval",
            { s, key -> json.put(key, s.keyboardRepeatInterval) },
            { key -> json.optInt(key)?.let { setKeyboardRepeatInterval(it) } },
        )
        run(
            "keyboardShowHide",
            { s, key -> json.put(key, s.keyboardShowHide.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setKeyboardMode(KeyboardMode.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "lastSeenVersion",
            { s, key -> json.put(key, s.lastSeenVersion) },
            { key ->
                json.optString(key)?.let { setBrowseLastSeenVersion(it) }
            },
        )
        run(
            "movementStrategy",
            { s, key -> json.put(key, s.movementStrategy.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setPlayMovementStrategySetting(
                           MovementStrategySetting.valueOf(it)
                       )
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "orientationLock",
            { s, key -> json.put(key, s.orientationLock.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setAppOrientationLock(Orientation.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "playActivityClueTabsPage",
            { s, key -> json.put(key, s.playActivityClueTabsPage) },
            { key -> json.optInt(key)?.let { setPlayClueTabsPage(0, it) } },
        )
        run(
            "playActivityClueTabsPage1",
            { s, key -> json.put(key, s.playActivityClueTabsPage1) },
            { key -> json.optInt(key)?.let { setPlayClueTabsPage(1, it) } },
        )
        run(
            "playLetterUndoEnabled",
            { s, key -> json.put(key, s.playLetterUndoEnabled) },
            { key ->
                json.optBoolean(key)?.let { setPlayPlayLetterUndoEnabled(it) }
            },
        )
        run(
            "predictAnagramChars",
            { s, key -> json.put(key, s.predictAnagramChars) },
            { key ->
                json.optBoolean(key)?.let { setPlayPredictAnagramChars(it) }
            },
        )
        run(
            "preserveCorrectLettersInShowErrors",
            { s, key -> json.put(key, s.preserveCorrectLettersInShowErrors) },
            { key ->
                json.optBoolean(key)?.let {
                    setPlayPreserveCorrectLettersInShowErrors(it)
                }
            },
        )
        run(
            "randomClueOnShake",
            { s, key -> json.put(key, s.randomClueOnShake) },
            { key ->
                json.optBoolean(key)?.let { setPlayRandomClueOnShake(it) }
            },
        )
        run(
            "scale",
            { s, key -> json.put(key, s.scale) },
            { key -> json.optDouble(key)?.let { setPlayScale(it.toFloat()) } },
        )
        run(
            "scrapeCru",
            { s, key -> json.put(key, s.scrapeCru) },
            { key -> json.optBoolean(key)?.let { setScrapeCru(it) } },
        )
        run(
            "scrapeEveryman",
            { s, key -> json.put(key, s.scrapeEveryman) },
            { key -> json.optBoolean(key)?.let { setScrapeEveryman(it) } },
        )
        run(
            "scrapeGuardianQuick",
            { s, key -> json.put(key, s.scrapeGuardianQuick) },
            { key -> json.optBoolean(key)?.let { setScrapeGuardianQuick(it) } },
        )
        run(
            "scrapeKegler",
            { s, key -> json.put(key, s.scrapeKegler) },
            { key -> json.optBoolean(key)?.let { setScrapeKegler(it) } },
        )
        run(
            "scrapePrivateEye",
            { s, key -> json.put(key, s.scrapePrivateEye) },
            { key -> json.optBoolean(key)?.let { setScrapePrivateEye(it) } },
        )
        run(
            "scrapePrzekroj",
            { s, key -> json.put(key, s.scrapePrzekroj) },
            { key -> json.optBoolean(key)?.let { setScrapePrzekroj(it) } },
        )
        run(
            "scratchMode",
            { s, key -> json.put(key, s.scratchMode) },
            { key -> json.optBoolean(key)?.let { setPlayScratchMode(it) } },
        )
        run(
            "showCluesOnPlayScreen",
            { s, key -> json.put(key, s.showCluesOnPlayScreen) },
            { key -> json.optBoolean(key)?.let { setPlayShowCluesTab(it) } },
        )
        run(
            "showCount",
            { s, key -> json.put(key, s.showCount) },
            { key -> json.optBoolean(key)?.let { setPlayShowCount(it) } },
        )
        run(
            "showErrors",
            { s, key -> json.put(key, s.showErrors) },
            { key -> json.optBoolean(key)?.let { setPlayShowErrorsGrid(it) } },
        )
        run(
            "showErrorsClue",
            { s, key -> json.put(key, s.showErrorsClue) },
            { key -> json.optBoolean(key)?.let { setPlayShowErrorsClue(it) } },
        )
        run(
            "showErrorsCursor",
            { s, key -> json.put(key, s.showErrorsCursor) },
            { key ->
                json.optBoolean(key)?.let { setPlayShowErrorsCursor(it) }
            },
        )
        run(
            "showTimer",
            { s, key -> json.put(key, s.showTimer) },
            { key -> json.optBoolean(key)?.let { setPlayShowTimer(it) } },
        )
        run(
            "showWordsInClueList",
            { s, key -> json.put(key, s.showWordsInClueList) },
            { key -> json.optBoolean(key)?.let { setClueListShowWords(it) } },
        )
        run(
            "skipFilled",
            { s, key -> json.put(key, s.skipFilled) },
            { key -> json.optBoolean(key)?.let { setPlaySkipFilled(it) } },
        )
        run(
            "snapClue",
            { s, key -> json.put(key, s.snapClue) },
            { key -> json.optBoolean(key)?.let { setClueListSnapToClue(it) } },
        )
        run(
            "sort",
            { s, key -> json.put(key, s.sort) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setBrowseSortSetting(AccessorSetting.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "spaceChangesDirection",
            { s, key -> json.put(key, s.spaceChangesDirection) },
            { key ->
                json.optBoolean(key)?.let { setPlaySpaceChangesDirection(it) }
            },
        )
        run(
            "suppressHints",
            { s, key -> json.put(key, s.suppressHints) },
            { key ->
                json.optBoolean(key)?.let {
                    setPlaySuppressHintHighlighting(it)
                }
            },
        )
        run(
            "suppressMessages",
            { s, key -> json.put(key, s.suppressMessages) },
            { key ->
                json.optBoolean(key)?.let { setDownloadSuppressMessages(it) }
            },
        )
        run(
            "suppressSummaryMessages",
            { s, key -> json.put(key, s.suppressSummaryMessages) },
            { key ->
                json.optBoolean(key)?.let {
                    setDownloadSuppressSummaryMessages(it)
                }
            },
        )
        run(
            "swipeAction",
            { s, key -> json.put(key, s.swipeAction.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setBrowseSwipeAction(BrowseSwipeAction.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "toggleBeforeMove",
            { s, key -> json.put(key, s.toggleBeforeMove) },
            { key ->
                json.optBoolean(key)?.let { setPlayToggleBeforeMove(it) }
            },
        )
        run(
            "uiTheme",
            { s, key -> json.put(key, s.uiTheme.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setAppDayNightMode(DayNightMode.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "useNativeKeyboard",
            { s, key -> json.put(key, s.useNativeKeyboard) },
            { key -> json.optBoolean(key)?.let { setKeyboardUseNative(it) } },
        )
        run(
            "volumeActivatesVoice",
            { s, key -> json.put(key, s.volumeActivatesVoice) },
            { key ->
                json.optBoolean(key)?.let { setVoiceVolumeActivatesVoice(it) }
            },
        )

        // bit untidy for file handler settings
        if (settings == null) {
            json.optString("storageLocation")?.let { location ->
            json.optString("safRootUri")?.let { root ->
            json.optString("safCrosswordsFolderUri")?.let { crosswords ->
            json.optString("safArchiveFolderUri")?.let { archive ->
            json.optString("safToImportFolderUri")?.let { import ->
            json.optString("safToImportDoneFolderUri")?.let { importDone ->
            json.optString("safToImportFailedFolderUri")?.let { importFailed ->
                try {
                    setFileHandlerSettings(
                        FileHandlerSettings(
                            StorageLocation.valueOf(location),
                            root,
                            crosswords,
                            archive,
                            import,
                            importDone,
                            importFailed,
                        )
                    )
                } catch (e : IllegalArgumentException) {
                    // ignore
                }
            }}}}}}}
        } else {
            json.put("storageLocation", settings.storageLocation.toString())
            json.put("safArchiveFolderUri", settings.safArchiveFolderUri)
            json.put("safCrosswordsFolderUri", settings.safCrosswordsFolderUri)
            json.put("safRootUri", settings.safRootUri)
            json.put(
                "safToImportDoneFolderUri",
                settings.safToImportDoneFolderUri
            )
            json.put(
                "safToImportFailedFolderUri",
                settings.safToImportFailedFolderUri,
            )
            json.put("safToImportFolderUri", settings.safToImportFolderUri)
        }
    }
}
