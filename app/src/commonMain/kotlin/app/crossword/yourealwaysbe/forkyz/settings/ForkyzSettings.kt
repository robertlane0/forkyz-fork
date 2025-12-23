
package app.crossword.yourealwaysbe.forkyz.settings

import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.util.function.Consumer
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import androidx.annotation.WorkerThread
import androidx.datastore.core.DataStore

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import app.crossword.yourealwaysbe.forkyz.net.Downloaders
import app.crossword.yourealwaysbe.forkyz.util.JSONUtils
import app.crossword.yourealwaysbe.forkyz.util.getOnce
import app.crossword.yourealwaysbe.puz.MovementStrategy
import app.crossword.yourealwaysbe.puz.Playboard.DeleteCrossingMode

class ForkyzSettings(val settingsStore : DataStore<Settings>) {
    private val WRITE_CHARSET = Charset.forName("UTF-8")

    val settingsFlow : Flow<Settings>
        = settingsStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(Settings())
                } else {
                    throw exception
                }
            }

    val liveVoiceAlwaysAnnounceBox : Flow<Boolean>
        = settingsMap { it.alwaysAnnounceBox }

    fun getVoiceAlwaysAnnounceBox(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.alwaysAnnounceBox }

    fun setVoiceAlwaysAnnounceBox(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(alwaysAnnounceBox = value)
        }
    }

    val liveVoiceAlwaysAnnounceClue : Flow<Boolean>
        = settingsMap { it.alwaysAnnounceClue }

    fun getVoiceAlwaysAnnounceClue(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.alwaysAnnounceClue }

    fun setVoiceAlwaysAnnounceClue(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(alwaysAnnounceClue = value)
        }
    }

    val liveAppTheme : Flow<Theme>
        = settingsMap { it.applicationTheme }

    fun getAppTheme(cb : (Theme) -> Unit)
        = settingsOnce(cb) { it.applicationTheme }

    fun setAppTheme(value : Theme) {
        settingsUpdate { settings ->
            settings.copy(applicationTheme = value)
        }
    }

    fun setAppThemeSync(value : Theme) {
        runBlocking {
            settingsStore.updateData { settings ->
                settings.copy(applicationTheme = value)
            }
        }
    }

    val liveBackgroundDownloadSettings : Flow<BackgroundDownloadSettings>
        = settingsMap(this::makeBackgroundDownloadSettings)

    // Consumer for BackgroundDownloadManager.java
    fun getBackgroundDownloadSettings(
        cb : Consumer<BackgroundDownloadSettings>,
    ) = settingsOnce(
        cb::accept,
        this::makeBackgroundDownloadSettings,
    )

    val liveBrowseCleanupAgeArchive : Flow<Int>
        = settingsMap { it.archiveCleanupAge }

    fun getBrowseCleanupAgeArchive(cb : (Int) -> Unit)
        = settingsOnce(cb) { it.archiveCleanupAge }

    fun setBrowseCleanupAgeArchive(value : Int) {
        settingsUpdate { settings ->
            settings.copy(archiveCleanupAge = value)
        }
    }

    val liveBrowseDontConfirmCleanup : Flow<Boolean>
        = settingsMap { it.dontConfirmCleanup }

    fun getBrowseDontConfirmCleanup(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.dontConfirmCleanup }

    fun setBrowseDontConfirmCleanup(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(dontConfirmCleanup = value)
        }
    }

    val liveBrowseDontConfirmDelete : Flow<Boolean>
        = settingsMap { it.dontConfirmBrowseDelete }

    fun getBrowseDontConfirmDelete(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.dontConfirmBrowseDelete }

    fun setBrowseDontConfirmDelete(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(dontConfirmBrowseDelete = value)
        }
    }

    val liveDownloadersSettings : Flow<DownloadersSettings>
        = settingsMap(this::makeDownloadersSettings)

    fun getDownloadersSettings(cb : (DownloadersSettings) -> Unit)
        = settingsOnce(cb, this::makeDownloadersSettings)

    val liveDownloadAllowRoaming : Flow<Boolean>
        = settingsMap { it.backgroundDownloadAllowRoaming }

    fun getDownloadAllowRoaming(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.backgroundDownloadAllowRoaming }

    fun setDownloadAllowRoaming(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadAllowRoaming = value)
        }
    }

    val liveDownloadAutoDownloaders : Flow<Set<String>>
        = settingsMap { it.autoDownloaders.toSet() }

    fun getDownloadAutoDownloaders(cb : (Set<String>) -> Unit)
        = settingsOnce(cb) { it.autoDownloaders.toSet() }

    fun setDownloadAutoDownloaders(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(autoDownloaders = value.toList())
        }
    }

    val liveDownloadDays : Flow<Set<String>>
        = settingsMap { it.backgroundDownloadDays.toSet() }

    fun getDownloadDays(cb : (Set<String>) -> Unit)
        = settingsOnce(cb) { it.backgroundDownloadDays.toSet() }

    fun setDownloadDays(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadDays = value.toList())
        }
    }

    val liveDownloadDaysTime : Flow<Int>
        = settingsMap { it.backgroundDownloadDaysTime }

    fun getDownloadDaysTime(cb : (Int) -> Unit)
        = settingsOnce(cb) { it.backgroundDownloadDaysTime }

    fun setDownloadDaysTime(value : Int) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadDaysTime = value)
        }
    }

    val liveDownloadHourly : Flow<Boolean>
        = settingsMap { it.backgroundDownloadHourly }

    fun getDownloadHourly(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.backgroundDownloadHourly }

    fun setDownloadHourly(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadHourly = value)
        }
    }

    val liveDownloadRequireCharging : Flow<Boolean>
        = settingsMap { it.backgroundDownloadRequireCharging }

    fun getDownloadRequireCharging(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.backgroundDownloadRequireCharging }

    fun setDownloadRequireCharging(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadRequireCharging = value)
        }
    }

    val liveDownloadRequireUnmetered : Flow<Boolean>
        = settingsMap { it.backgroundDownloadRequireUnmetered }

    fun getDownloadRequireUnmetered(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.backgroundDownloadRequireUnmetered }

    fun setDownloadRequireUnmetered(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadRequireUnmetered = value)
        }
    }

    val liveBrowseAlwaysShowRating : Flow<Boolean>
        = settingsMap { it.browseAlwaysShowRating }

    fun getBrowseAlwaysShowRating(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.browseAlwaysShowRating }

    fun setBrowseAlwaysShowRating(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(browseAlwaysShowRating = value)
        }
    }

    val liveBrowseIndicateIfSolution : Flow<Boolean>
        = settingsMap { it.browseIndicateIfSolution }

    fun getBrowseIndicateIfSolution(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.browseIndicateIfSolution }

    fun setBrowseIndicateIfSolution(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(browseIndicateIfSolution = value)
        }
    }

    val liveBrowseNewPuzzle : Flow<Boolean>
        = settingsMap { it.browseNewPuzzle }

    fun getBrowseNewPuzzle(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.browseNewPuzzle }

    fun setBrowseNewPuzzle(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(browseNewPuzzle = value)
        }
    }

    val liveBrowseShowPercentageCorrect : Flow<Boolean>
        = settingsMap { it.browseShowPercentageCorrect }

    fun getBrowseShowPercentageCorrect(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.browseShowPercentageCorrect }

    fun setBrowseShowPercentageCorrect(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(browseShowPercentageCorrect = value)
        }
    }

    val liveVoiceButtonActivatesVoice : Flow<Boolean>
        = settingsMap { it.buttonActivatesVoice }

    fun getVoiceButtonActivatesVoice(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.buttonActivatesVoice }

    fun setVoiceButtonActivatesVoice(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(buttonActivatesVoice = value)
        }
    }

    val liveVoiceButtonAnnounceClue : Flow<Boolean>
        = settingsMap { it.buttonAnnounceClue }

    fun getVoiceButtonAnnounceClue(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.buttonAnnounceClue }

    fun setVoiceButtonAnnounceClue(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(buttonAnnounceClue = value)
        }
    }

    val liveExtLLMAgent : Flow<LLMAgent>
        = settingsMap { it.llmAgent }

    fun getExtLLMAgent(cb : (LLMAgent) -> Unit)
        = settingsOnce(cb) { it.llmAgent }

    fun setExtLLMAgent(value : LLMAgent) {
        settingsUpdate { settings ->
            settings.copy(llmAgent = value)
        }
    }

    val liveExtLLMAgentAPIKey : Flow<String>
        = settingsMap { it.llmAgentAPIKey }

    fun getExtLLMAgentAPIKey(cb : (String) -> Unit)
        = settingsOnce(cb) { it.llmAgentAPIKey }

    fun setExtLLMAgentAPIKey(value : String) {
        settingsUpdate { settings ->
            settings.copy(llmAgentAPIKey = value)
        }
    }

    val liveBrowseCleanupAge : Flow<Int>
        = settingsMap { it.cleanupAge }

    fun getBrowseCleanupAge(cb : (Int) -> Unit)
        = settingsOnce(cb) { it.cleanupAge }

    fun setBrowseCleanupAge(value : Int) {
        settingsUpdate { settings ->
            settings.copy(cleanupAge = value)
        }
    }

    val livePlayClueBelowGrid : Flow<Boolean>
        = settingsMap { it.clueBelowGrid }

    fun getPlayClueBelowGrid(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.clueBelowGrid }

    fun setPlayClueBelowGrid(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(clueBelowGrid = value)
        }
    }

    val livePlayClueHighlight : Flow<ClueHighlight>
        = settingsMap { it.clueHighlight }

    fun getPlayClueHighlight(cb : (ClueHighlight) -> Unit)
        = settingsOnce(cb) { it.clueHighlight }

    fun setPlayClueHighlight(value : ClueHighlight) {
        settingsUpdate { settings ->
            settings.copy(clueHighlight = value)
        }
    }

    val livePlayExpandableClueLine : Flow<Boolean>
        = settingsMap { it.expandableClueLine }

    fun getPlayExpandableClueLine(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.expandableClueLine }

    fun setPlayExpandableClueLine(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(expandableClueLine = value)
        }
    }

    val livePlayClueListNameInClueLine : Flow<ClueListClueLine>
        = settingsMap { it.clueListNameInClueLine }

    fun getPlayClueListNameInClueLine(cb : (ClueListClueLine) -> Unit)
        = settingsOnce(cb) { it.clueListNameInClueLine }

    fun setPlayClueListNameInClueLine(value : ClueListClueLine) {
        settingsUpdate { settings ->
            settings.copy(clueListNameInClueLine = value)
        }
    }

    val livePlayDontSnapBoardToBorders : Flow<Boolean>
        = settingsMap { it.dontSnapBoardToBorders }

    fun getPlayDontSnapBoardToBorders(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.dontSnapBoardToBorders }

    fun setPlayDontSnapBoardToBorders(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(dontSnapBoardToBorders = value)
        }
    }

    val liveClueListClueTabsDouble : Flow<ClueTabsDouble>
        = settingsMap { it.clueTabsDouble }

    fun getClueListClueTabsDouble(cb : (ClueTabsDouble) -> Unit)
        = settingsOnce(cb) { it.clueTabsDouble }

    fun setClueListClueTabsDouble(value : ClueTabsDouble) {
        settingsUpdate { settings ->
            settings.copy(clueTabsDouble = value)
        }
    }

    private fun makeBackgroundDownloadSettings(
        settings : Settings,
    ) : BackgroundDownloadSettings {
        return BackgroundDownloadSettings(
            settings.backgroundDownloadRequireUnmetered,
            settings.backgroundDownloadAllowRoaming,
            settings.backgroundDownloadRequireCharging,
            settings.backgroundDownloadHourly,
            settings.backgroundDownloadDays.toSet(),
            settings.backgroundDownloadDaysTime
        )
    }

    private fun makeDownloadersSettings(
        settings : Settings,
    ) : DownloadersSettings {
        return DownloadersSettings(
            settings.downloadDeStandaard,
            settings.downloadDeTelegraaf,
            settings.downloadElPaisExperto,
            settings.downloadGuardianDailyCryptic,
            settings.downloadGuardianWeeklyQuiptic,
            settings.downloadHamAbend,
            settings.downloadIndependentDailyCryptic,
            settings.downloadIrishNewsCryptic,
            settings.downloadJonesin,
            settings.downloadJoseph,
            settings.download20Minutes,
            settings.downloadLaCroix,
            settings.downloadLATimes,
            settings.downloadLeParisienF1,
            settings.downloadLeParisienF2,
            settings.downloadLeParisienF3,
            settings.downloadLeParisienF4,
            settings.downloadMetroCryptic,
            settings.downloadMetroQuick,
            settings.downloadNewsday,
            settings.downloadNewYorkTimesSyndicated,
            settings.downloadNRCInHetMidden,
            settings.downloadNRCScrypto,
            settings.downloadPremier,
            settings.downloadSheffer,
            settings.downloadSudOuestMotsCroises,
            settings.downloadSudOuestMotsFleches,
            settings.downloadTF1MotsCroises,
            settings.downloadTF1MotsFleches,
            settings.downloadUniversal,
            settings.downloadUSAToday,
            settings.downloadWaPoSunday,
            settings.downloadWsj,
            settings.scrapeCru,
            settings.scrapeGuardianQuick,
            settings.scrapeKegler,
            settings.scrapePrivateEye,
            settings.scrapePrzekroj,
            settings.downloadCustomDaily,
            settings.customDailyTitle,
            settings.customDailyUrl,
            settings.suppressSummaryMessages,
            settings.suppressMessages,
            settings.autoDownloaders.toSet(),
            settings.downloadTimeout,
            settings.dlOnStartup,
        )
    }

    private fun makeDeleteCrossingMode(
        settings : Settings,
    ) : DeleteCrossingMode {
        return when (settings.deleteCrossingMode) {
            DeleteCrossingModeSetting.DCMS_DELETE
                -> DeleteCrossingMode.DELETE
            DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_CELLS
                -> DeleteCrossingMode.PRESERVE_FILLED_CELLS
            DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_WORDS
                -> DeleteCrossingMode.PRESERVE_FILLED_WORDS
        }
    }

    private fun makeExternalToolSettings(
        settings : Settings,
    ) : ExternalToolSettings {
        return ExternalToolSettings(
            LLMAgentSettings(
                settings.llmAgent,
                settings.llmAgentAPIKey,
            ),
            settings.crosswordSolverEnabled,
            settings.duckDuckGoEnabled,
            settings.externalDictionary,
            settings.fifteenSquaredEnabled,
        );
    }

    private fun makeFileHandlerSettings(
        settings : Settings,
    ) : FileHandlerSettings {
        return FileHandlerSettings(
            settings.storageLocation,
            settings.safRootUri,
            settings.safCrosswordsFolderUri,
            settings.safArchiveFolderUri,
            settings.safToImportFolderUri,
            settings.safToImportDoneFolderUri,
            settings.safToImportFailedFolderUri,
        )
    }

    private fun makeKeyboardSettings(
        settings : Settings,
    ) : KeyboardSettings {
        return KeyboardSettings(
            settings.keyboardCompact,
            settings.keyboardForceCaps,
            settings.keyboardHaptic,
            settings.keyboardHideButton,
            settings.keyboardLayout,
            settings.keyboardShowHide,
            settings.keyboardRepeatDelay,
            settings.keyboardRepeatInterval,
            settings.useNativeKeyboard,
        )
    }

    private fun makePlayMovementStrategy(
        settings : Settings,
    ) : MovementStrategy {
        return getFullMovementStrategy(
            settings.movementStrategy,
            settings.cycleUnfilledMode,
        )
    }

    private fun makeStopOnEndStrategy(
        settings : Settings,
    ) : MovementStrategy {
        return getFullMovementStrategy(
            MovementStrategySetting.MSS_STOP_ON_END,
            settings.cycleUnfilledMode,
        )
    }

    private fun makeMovementStrategyClueList(
        settings : Settings,
    ) : MovementStrategy {
        return getFullMovementStrategy(
            coerceMovementStrategyClueList(settings.movementStrategyClueList),
            settings.cycleUnfilledMode,
        )
    }

    private fun makeMovementStrategyClueListSetting(
        settings : Settings,
    ) : MovementStrategySetting {
        return coerceMovementStrategyClueList(settings.movementStrategyClueList)
    }

    private fun makeRenderSettings(
        settings : Settings,
    ) : RenderSettings {
        return RenderSettings(
            settings.displayScratch,
            settings.suppressHints,
            settings.displaySeparators,
            settings.inferSeparators,
        )
    }

    private fun makeRatingSettings(
        settings : Settings,
    ) : RatingsSettings {
        return RatingsSettings(
            settings.disableRatings,
            settings.browseAlwaysShowRating,
        )
    }

    /**
     * Get a value from settings as a shared flow
     */
    private fun <T> settingsMap(
        getValue : (Settings) -> T,
    ) : Flow<T> {
        return settingsFlow.map(getValue).distinctUntilChanged()
    }

    /**
     * Get a value from settings once and return async on main via callback
     */
    private fun <T> settingsOnce(
        cb : (T) -> Unit,
        getValue : (Settings) -> T,
    ) {
        settingsFlow.getOnce { cb(getValue(it)) }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun settingsUpdate(
        cb : (() -> Unit)?,
        transform : (Settings) -> Settings,
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            settingsStore.updateData(transform)
            cb?.let { it() }
        }
    }

    private fun settingsUpdate(transform : (Settings) -> Settings) {
        settingsUpdate(null, transform)
    }

    private val livePlayActivityClueTabsPage : Flow<Int>
        = settingsMap { it.playActivityClueTabsPage }

    private val livePlayActivityClueTabsPage1 : Flow<Int>
        = settingsMap { it.playActivityClueTabsPage1 }

    fun getPlayClueTabsPage(viewIdx : Int, cb : (Int) -> Unit) {
        settingsOnce(cb) {
            if (viewIdx == 0)
                it.playActivityClueTabsPage
            else
                it.playActivityClueTabsPage1
        }
    }

    fun livePlayClueTabsPage(viewIdx : Int) : Flow<Int> {
        if (viewIdx == 0)
            return livePlayActivityClueTabsPage
        else
            return livePlayActivityClueTabsPage1
    }

    fun setPlayClueTabsPage(viewIdx : Int, pageNo : Int) {
        if (pageNo >= 0) {
            if (viewIdx == 0) {
                settingsUpdate { settings ->
                    settings.copy(playActivityClueTabsPage = pageNo)
                }
            } else if (viewIdx == 1) {
                settingsUpdate { settings ->
                    settings.copy(playActivityClueTabsPage1 = pageNo)
                }
            }
        }
    }

    val liveExtCrosswordSolverEnabled : Flow<Boolean>
        = settingsMap { it.crosswordSolverEnabled }

    fun getExtCrosswordSolverEnabled(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.crosswordSolverEnabled }

    fun setExtCrosswordSolverEnabled(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(crosswordSolverEnabled = value)
        }
    }

    val liveDownloadCustomDailyTitle : Flow<String>
        = settingsMap { it.customDailyTitle }

    fun getDownloadCustomDailyTitle(cb : (String) -> Unit)
        = settingsOnce(cb) { it.customDailyTitle }

    fun setDownloadCustomDailyTitle(value : String) {
        settingsUpdate { settings ->
            settings.copy(customDailyTitle = value)
        }
    }

    val liveDownloadCustomDailyURL : Flow<String>
        = settingsMap { it.customDailyUrl }

    fun getDownloadCustomDailyURL(cb : (String) -> Unit)
        = settingsOnce(cb) { it.customDailyUrl }

    fun setDownloadCustomDailyURL(value : String) {
        settingsUpdate { settings ->
            settings.copy(customDailyUrl = value)
        }
    }

    val livePlayCycleUnfilledMode : Flow<CycleUnfilledMode>
        = settingsMap { it.cycleUnfilledMode }

    fun getPlayCycleUnfilledMode(cb : (CycleUnfilledMode) -> Unit)
        = settingsOnce(cb) { it.cycleUnfilledMode }

    fun setPlayCycleUnfilledMode(value : CycleUnfilledMode) {
        settingsUpdate { settings ->
            settings.copy(cycleUnfilledMode = value)
        }
    }

    val livePlayDeleteCrossingMode : Flow<DeleteCrossingMode>
        = settingsMap(this::makeDeleteCrossingMode)

    fun getPlayDeleteCrossingMode(cb : (DeleteCrossingMode) -> Unit)
        = settingsOnce(cb, this::makeDeleteCrossingMode)

    val livePlayDeleteCrossingModeSetting : Flow<DeleteCrossingModeSetting>
        = settingsMap { it.deleteCrossingMode }

    fun getPlayDeleteCrossingModeSetting(
        cb : (DeleteCrossingModeSetting) -> Unit
    ) = settingsOnce(cb) { it.deleteCrossingMode }

    fun setPlayDeleteCrossingModeSetting(value : DeleteCrossingModeSetting) {
        settingsUpdate { settings ->
            settings.copy(deleteCrossingMode = value)
        }
    }

    val liveBrowseDeleteOnCleanup : Flow<Boolean>
        = settingsMap { it.deleteOnCleanup }

    fun getBrowseDeleteOnCleanup(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.deleteOnCleanup }

    fun setBrowseDeleteOnCleanup(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(deleteOnCleanup = value)
        }
    }

    val liveRatingsDisableRatings : Flow<Boolean>
        = settingsMap { it.disableRatings }

    fun getRatingsDisableRatings(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.disableRatings }

    fun setRatingsDisableRatings(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(disableRatings = value)
        }
    }

    val liveBrowseDisableSwipe : Flow<Boolean>
        = settingsMap { it.disableSwipe }

    fun getBrowseDisableSwipe(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.disableSwipe }

    fun setBrowseDisableSwipe(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(disableSwipe = value)
        }
    }

    val livePlayScratchDisplay : Flow<Boolean>
        = settingsMap { it.displayScratch }

    fun getPlayScratchDisplay(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.displayScratch }

    fun setPlayScratchDisplay(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(displayScratch = value)
        }
    }

    val livePlayDisplaySeparators : Flow<DisplaySeparators>
        = settingsMap { it.displaySeparators }

    fun getPlayDisplaySeparators(cb : (DisplaySeparators) -> Unit)
        = settingsOnce(cb) { it.displaySeparators }

    fun setPlayDisplaySeparators(value : DisplaySeparators) {
        settingsUpdate { settings ->
            settings.copy(displaySeparators = value)
        }
    }

    val liveBrowseLastDownload : Flow<Long>
        = settingsMap { it.dlLast }

    fun getBrowseLastDownload(cb : (Long) -> Unit)
        = settingsOnce(cb) { it.dlLast }

    fun getBrowseLastDownloadSync() : Long
        = runBlocking { settingsFlow.first().dlLast }

    fun setBrowseLastDownload(value : Long) {
        if (0 <= value && value <= System.currentTimeMillis()) {
            settingsUpdate { settings ->
                settings.copy(dlLast = value)
        }
        }
    }

    val liveDownloadOnStartUp : Flow<Boolean>
        = settingsMap { it.dlOnStartup }

    fun getDownloadOnStartUp(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.dlOnStartup }

    fun setDownloadOnStartUp(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(dlOnStartup = value)
        }
    }

    val livePlayDoubleTapFitBoard : Flow<Boolean>
        = settingsMap { it.doubleTap }

    // Consumer for BoardEditViews.java
    fun getPlayDoubleTapFitBoard(cb : Consumer<Boolean>)
        = settingsOnce(
            cb::accept,
            { it.doubleTap },
        )

    fun setPlayDoubleTapFitBoard(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(doubleTap = value)
        }
    }

    val liveDownload20Minutes : Flow<Boolean>
        = settingsMap { it.download20Minutes }

    fun getDownload20Minutes(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.download20Minutes }

    fun setDownload20Minutes(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(download20Minutes = value)
        }
    }

    val liveDownloadCustomDaily : Flow<Boolean>
        = settingsMap { it.downloadCustomDaily }

    fun getDownloadCustomDaily(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadCustomDaily }

    fun setDownloadCustomDaily(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadCustomDaily = value)
        }
    }

    val liveDownloadDeStandaard : Flow<Boolean>
        = settingsMap { it.downloadDeStandaard }

    fun getDownloadDeStandaard(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadDeStandaard }

    fun setDownloadDeStandaard(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadDeStandaard = value)
        }
    }

    val liveDownloadDeTelegraaf : Flow<Boolean>
        = settingsMap { it.downloadDeTelegraaf }

    fun getDownloadDeTelegraaf(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadDeTelegraaf }

    fun setDownloadDeTelegraaf(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadDeTelegraaf = value)
        }
    }

    val liveDownloadElPaisExperto : Flow<Boolean>
        = settingsMap { it.downloadElPaisExperto }

    fun getDownloadElPaisExperto(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadElPaisExperto }

    fun setDownloadElPaisExperto(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadElPaisExperto = value)
        }
    }

    val liveDownloadGuardianDailyCryptic : Flow<Boolean>
        = settingsMap { it.downloadGuardianDailyCryptic }

    fun getDownloadGuardianDailyCryptic(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadGuardianDailyCryptic }

    fun setDownloadGuardianDailyCryptic(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadGuardianDailyCryptic = value)
        }
    }

    val liveDownloadGuardianWeeklyQuiptic : Flow<Boolean>
        = settingsMap { it.downloadGuardianWeeklyQuiptic }

    fun getDownloadGuardianWeeklyQuiptic(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadGuardianWeeklyQuiptic }

    fun setDownloadGuardianWeeklyQuiptic(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadGuardianWeeklyQuiptic = value)
        }
    }

    val liveDownloadHamAbend : Flow<Boolean>
        = settingsMap { it.downloadHamAbend }

    fun getDownloadHamAbend(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadHamAbend }

    fun setDownloadHamAbend(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadHamAbend = value)
        }
    }

    val liveDownloadIndependentDailyCryptic : Flow<Boolean>
        = settingsMap { it.downloadIndependentDailyCryptic }

    fun getDownloadIndependentDailyCryptic(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadIndependentDailyCryptic }

    fun setDownloadIndependentDailyCryptic(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadIndependentDailyCryptic = value)
        }
    }

    val liveDownloadIrishNewsCryptic : Flow<Boolean>
        = settingsMap { it.downloadIrishNewsCryptic }

    fun getDownloadIrishNewsCryptic(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadIrishNewsCryptic }

    fun setDownloadIrishNewsCryptic(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadIrishNewsCryptic = value)
        }
    }

    val liveDownloadJonesin : Flow<Boolean>
        = settingsMap { it.downloadJonesin }

    fun getDownloadJonesin(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadJonesin }

    fun setDownloadJonesin(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadJonesin = value)
        }
    }

    val liveDownloadJoseph : Flow<Boolean>
        = settingsMap { it.downloadJoseph }

    fun getDownloadJoseph(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadJoseph }

    fun setDownloadJoseph(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadJoseph = value)
        }
    }

    val liveDownloadLaCroix : Flow<Boolean>
        = settingsMap { it.downloadLaCroix }

    fun getDownloadLaCroix(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadLaCroix }

    fun setDownloadLaCroix(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadLaCroix = value)
        }
    }

    val liveDownloadLATimes : Flow<Boolean>
        = settingsMap { it.downloadLATimes }

    fun setDownloadLATimes(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadLATimes = value)
        }
    }

    val liveDownloadLeParisienF1 : Flow<Boolean>
        = settingsMap { it.downloadLeParisienF1 }

    fun getDownloadLeParisienF1(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadLeParisienF1 }

    fun setDownloadLeParisienF1(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadLeParisienF1 = value)
        }
    }

    val liveDownloadLeParisienF2 : Flow<Boolean>
        = settingsMap { it.downloadLeParisienF2 }

    fun getDownloadLeParisienF2(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadLeParisienF2 }

    fun setDownloadLeParisienF2(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadLeParisienF2 = value)
        }
    }

    val liveDownloadLeParisienF3 : Flow<Boolean>
        = settingsMap { it.downloadLeParisienF3 }

    fun getDownloadLeParisienF3(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadLeParisienF3 }

    fun setDownloadLeParisienF3(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadLeParisienF3 = value)
        }
    }

    val liveDownloadLeParisienF4 : Flow<Boolean>
        = settingsMap { it.downloadLeParisienF4 }

    fun getDownloadLeParisienF4(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadLeParisienF4 }

    fun setDownloadLeParisienF4(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadLeParisienF4 = value)
        }
    }

    val liveDownloadMetroCryptic : Flow<Boolean>
        = settingsMap { it.downloadMetroCryptic }

    fun getDownloadMetroCryptic(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadMetroCryptic }

    fun setDownloadMetroCryptic(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadMetroCryptic = value)
        }
    }

    val liveDownloadMetroQuick : Flow<Boolean>
        = settingsMap { it.downloadMetroQuick }

    fun getDownloadMetroQuick(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadMetroQuick }

    fun setDownloadMetroQuick(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadMetroQuick = value)
        }
    }

    val liveDownloadNewYorkTimesSyndicated : Flow<Boolean>
        = settingsMap { it.downloadNewYorkTimesSyndicated }

    fun getDownloadNewYorkTimesSyndicated(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadNewYorkTimesSyndicated }

    fun setDownloadNewYorkTimesSyndicated(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadNewYorkTimesSyndicated = value)
        }
    }

    val liveDownloadNewsday : Flow<Boolean>
        = settingsMap { it.downloadNewsday }

    fun getDownloadNewsday(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadNewsday }

    fun setDownloadNewsday(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadNewsday = value)
        }
    }

    val liveDownloadNRCInHetMidden : Flow<Boolean>
        = settingsMap { it.downloadNRCInHetMidden }

    fun getDownloadNRCInHetMidden(cb: (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadNRCInHetMidden }

    fun setDownloadNRCInHetMidden(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadNRCInHetMidden = value)
        }
    }

    val liveDownloadNRCScrypto : Flow<Boolean>
        = settingsMap { it.downloadNRCScrypto }

    fun getDownloadNRCScrypto(cb: (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadNRCScrypto }

    fun setDownloadNRCScrypto(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadNRCScrypto = value)
        }
    }

    val liveDownloadPremier : Flow<Boolean>
        = settingsMap { it.downloadPremier }

    fun getDownloadPremier(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadPremier }

    fun setDownloadPremier(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadPremier = value)
        }
    }

    val liveDownloadSheffer : Flow<Boolean>
        = settingsMap { it.downloadSheffer }

    fun getDownloadSheffer(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadSheffer }

    fun setDownloadSheffer(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadSheffer = value)
        }
    }

    val liveDownloadSudOuestMotsCroises : Flow<Boolean>
        = settingsMap { it.downloadSudOuestMotsCroises }

    fun getDownloadSudOuestMotsCroises(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadSudOuestMotsCroises }

    fun setDownloadSudOuestMotsCroises(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadSudOuestMotsCroises = value)
        }
    }

    val liveDownloadSudOuestMotsFleches : Flow<Boolean>
        = settingsMap { it.downloadSudOuestMotsFleches }

    fun getDownloadSudOuestMotsFleches(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadSudOuestMotsFleches }

    fun setDownloadSudOuestMotsFleches(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadSudOuestMotsFleches = value)
        }
    }

    val liveDownloadTF1MotsCroises : Flow<Boolean>
        = settingsMap { it.downloadTF1MotsCroises }

    fun getDownloadTF1MotsCroises(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadTF1MotsCroises }

    fun setDownloadTF1MotsCroises(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadTF1MotsCroises = value)
        }
    }

    val liveDownloadTF1MotsFleches : Flow<Boolean>
        = settingsMap { it.downloadTF1MotsFleches }

    fun getDownloadTF1MotsFleches(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadTF1MotsFleches }

    fun setDownloadTF1MotsFleches(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadTF1MotsFleches = value)
        }
    }

    val liveDownloadTimeout : Flow<Int>
        = settingsMap { it.downloadTimeout }

    fun getDownloadTimeout(cb : (Int) -> Unit)
        = settingsOnce(cb) { it.downloadTimeout }

    fun setDownloadTimeout(value : Int) {
        settingsUpdate { settings ->
            settings.copy(downloadTimeout = value)
        }
    }

    val liveDownloadUSAToday : Flow<Boolean>
        = settingsMap { it.downloadUSAToday }

    fun getDownloadUSAToday(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadUSAToday }

    fun setDownloadUSAToday(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadUSAToday = value)
        }
    }

    val liveDownloadUniversal : Flow<Boolean>
        = settingsMap { it.downloadUniversal }

    fun getDownloadUniversal(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadUniversal }

    fun setDownloadUniversal(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadUniversal = value)
        }
    }

    val liveDownloadWaPoSunday : Flow<Boolean>
        = settingsMap { it.downloadWaPoSunday }

    fun getDownloadWaPoSunday(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadWaPoSunday }

    fun setDownloadWaPoSunday(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadWaPoSunday = value)
        }
    }

    val liveDownloadWsj : Flow<Boolean>
        = settingsMap { it.downloadWsj }

    fun getDownloadWsj(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.downloadWsj }

    fun setDownloadWsj(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadWsj = value)
        }
    }

    val liveExtDuckDuckGoEnabled : Flow<Boolean>
        = settingsMap { it.duckDuckGoEnabled }

    fun getExtDuckDuckGoEnabled(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.duckDuckGoEnabled }

    fun setExtDuckDuckGoEnabled(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(duckDuckGoEnabled = value)
        }
    }

    val livePlayEnsureVisible : Flow<Boolean>
        = settingsMap { it.ensureVisible }

    // Consumer for BoardEditViews.java
    fun getPlayEnsureVisible(cb : Consumer<Boolean>)
        = settingsOnce(
            cb::accept,
            { it.ensureVisible },
        )

    fun setPlayEnsureVisible(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(ensureVisible = value)
        }
    }

    val livePlayEnterChangesDirection : Flow<Boolean>
        = settingsMap { it.enterChangesDirection }

    fun getPlayEnterChangesDirection(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.enterChangesDirection }

    fun setPlayEnterChangesDirection(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(enterChangesDirection = value)
        }
    }

    val liveVoiceEqualsAnnounceClue : Flow<Boolean>
        = settingsMap { it.equalsAnnounceClue }

    fun getVoiceEqualsAnnounceClue(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.equalsAnnounceClue }

    fun setVoiceEqualsAnnounceClue(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(equalsAnnounceClue = value)
        }
    }

    val liveVoiceLLMVoiceCommands : Flow<Boolean>
        = settingsMap { it.llmVoiceCommands }

    fun getVoiceLLMVoiceCommands(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.llmVoiceCommands }

    fun setVoiceLLMVoiceCommands(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(llmVoiceCommands = value)
        }
    }

    val liveExtDictionarySetting : Flow<ExternalDictionarySetting>
        = settingsMap { it.externalDictionary }

    fun getExtDictionarySetting(cb : (ExternalDictionarySetting) -> Unit)
        = settingsOnce(cb) { it.externalDictionary }

    fun setExtDictionarySetting(value : ExternalDictionarySetting) {
        settingsUpdate { settings ->
            settings.copy(externalDictionary = value)
        }
    }

    val liveExternalToolSettings : Flow<ExternalToolSettings>
        = settingsMap(this::makeExternalToolSettings)

    fun getExternalToolSettings(cb : (ExternalToolSettings) -> Unit)
        = settingsOnce(cb, this::makeExternalToolSettings)

    val liveExtFifteenSquaredEnabled : Flow<Boolean>
        = settingsMap { it.fifteenSquaredEnabled }

    fun getExtFifteenSquaredEnabled(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.fifteenSquaredEnabled }

    fun setExtFifteenSquaredEnabled(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(fifteenSquaredEnabled = value)
        }
    }

    val liveFileHandlerSettings : Flow<FileHandlerSettings>
        = settingsMap(this::makeFileHandlerSettings)

    fun getFileHandlerSettings(cb : (FileHandlerSettings) -> Unit)
        = settingsOnce(cb, this::makeFileHandlerSettings)

    fun setFileHandlerSettings(
        settings : FileHandlerSettings,
        cb : (() -> Unit)? = null,
    ) {
        settingsUpdate(cb) {
            it.copy(
                storageLocation = settings.storageLocation,
                safRootUri = settings.safRootURI,
                safCrosswordsFolderUri = settings.safCrosswordsURI,
                safArchiveFolderUri = settings.safArchiveURI,
                safToImportFolderUri = settings.safToImportURI,
                safToImportDoneFolderUri = settings.safToImportDoneURI,
                safToImportFailedFolderUri = settings.safToImportFailedURI,
            )
        }
    }

    fun setFileHandlerSettingsSync(settings : FileHandlerSettings) {
        runBlocking {
            settingsUpdate {
                it.copy(
                    storageLocation = settings.storageLocation,
                    safRootUri = settings.safRootURI,
                    safCrosswordsFolderUri = settings.safCrosswordsURI,
                    safArchiveFolderUri = settings.safArchiveURI,
                    safToImportFolderUri = settings.safToImportURI,
                    safToImportDoneFolderUri = settings.safToImportDoneURI,
                    safToImportFailedFolderUri = settings.safToImportFailedURI,
                )
            }
        }
    }

    val liveFileHandlerStorageLocation : Flow<StorageLocation>
        = settingsMap { it.storageLocation }

    val livePlayFitToScreenMode : Flow<FitToScreenMode>
        = settingsMap { it.fitToScreenMode }

    fun getPlayFitToScreenMode(cb : (FitToScreenMode) -> Unit)
        = settingsOnce(cb) { it.fitToScreenMode }

    fun setPlayFitToScreenMode(value : FitToScreenMode) {
        settingsUpdate { settings ->
            settings.copy(fitToScreenMode = value)
        }
    }

    val livePlayFullScreen : Flow<Boolean>
        = settingsMap { it.fullScreen }

    fun getPlayFullScreen(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.fullScreen }

    fun setPlayFullScreen(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(fullScreen = value)
        }
    }

    val livePlayGridRatioPortrait : Flow<GridRatio>
        = settingsMap { it.gridRatio }

    fun getPlayGridRatioPortrait(cb : (GridRatio) -> Unit)
        = settingsOnce(cb) { it.gridRatio }

    fun setPlayGridRatioPortrait(value : GridRatio) {
        settingsUpdate { settings ->
            settings.copy(gridRatio = value)
        }
    }

    val livePlayGridRatioLandscape : Flow<GridRatio>
        = settingsMap { it.gridRatioLand }

    fun getPlayGridRatioLandscape(cb : (GridRatio) -> Unit)
        = settingsOnce(cb) { it.gridRatioLand }

    fun setPlayGridRatioLandscape(value : GridRatio) {
        settingsUpdate { settings ->
            settings.copy(gridRatioLand = value)
        }
    }

    val livePlayIndicateShowErrors : Flow<Boolean>
        = settingsMap { it.indicateShowErrors }

    fun getPlayIndicateShowErrors(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.indicateShowErrors }

    fun setPlayIndicateShowErrors(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(indicateShowErrors = value)
        }
    }

    val livePlayInferSeparators : Flow<Boolean>
        = settingsMap { it.inferSeparators }

    fun getPlayInferSeparators(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.inferSeparators }

    fun setPlayInferSeparators(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(inferSeparators = value)
        }
    }

    val liveKeyboardCompact : Flow<Boolean>
        = settingsMap { it.keyboardCompact }

    fun getKeyboardCompact(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.keyboardCompact }

    fun setKeyboardCompact(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(keyboardCompact = value)
        }
    }

    val liveKeyboardForceCaps : Flow<Boolean>
        = settingsMap { it.keyboardForceCaps }

    fun getKeyboardForceCaps(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.keyboardForceCaps }

    fun setKeyboardForceCaps(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(keyboardForceCaps = value)
        }
    }

    val liveKeyboardHaptic : Flow<Boolean>
        = settingsMap { it.keyboardHaptic }

    fun getKeyboardHaptic(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.keyboardHaptic }

    fun setKeyboardHaptic(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(keyboardHaptic = value)
        }
    }

    val liveKeyboardHideButton : Flow<Boolean>
        = settingsMap { it.keyboardHideButton }

    fun getKeyboardHideButton(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.keyboardHideButton }

    fun setKeyboardHideButton(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(keyboardHideButton = value)
        }
    }

    val liveKeyboardLayout : Flow<KeyboardLayout>
        = settingsMap { it.keyboardLayout }

    fun getKeyboardLayout(cb : (KeyboardLayout) -> Unit)
        = settingsOnce(cb) { it.keyboardLayout }

    fun setKeyboardLayout(value : KeyboardLayout) {
        settingsUpdate { settings ->
            settings.copy(keyboardLayout = value)
        }
    }

    val liveKeyboardMode : Flow<KeyboardMode>
        = settingsMap { it.keyboardShowHide }

    fun getKeyboardMode(cb : (KeyboardMode) -> Unit)
        = settingsOnce(cb) { it.keyboardShowHide }

    fun setKeyboardMode(value : KeyboardMode) {
        settingsUpdate { settings ->
            settings.copy(keyboardShowHide = value)
        }
    }

    val liveKeyboardRepeatDelay : Flow<Int>
        = settingsMap { it.keyboardRepeatDelay }

    fun getKeyboardRepeatDelay(cb : (Int) -> Unit)
        = settingsOnce(cb) { it.keyboardRepeatDelay }

    fun setKeyboardRepeatDelay(value : Int) {
        settingsUpdate { settings ->
            settings.copy(keyboardRepeatDelay = value)
        }
    }

    val liveKeyboardRepeatInterval : Flow<Int>
        = settingsMap { it.keyboardRepeatInterval }

    fun getKeyboardRepeatInterval(cb : (Int) -> Unit)
        = settingsOnce(cb) { it.keyboardRepeatInterval }

    fun setKeyboardRepeatInterval(value : Int) {
        settingsUpdate { settings ->
            settings.copy(keyboardRepeatInterval = value)
        }
    }

    val liveKeyboardSettings : Flow<KeyboardSettings>
        = settingsMap(this::makeKeyboardSettings)

    fun getKeyboardSettings(cb : (KeyboardSettings) -> Unit)
        = settingsOnce(cb, this::makeKeyboardSettings)

    val liveKeyboardUseNative : Flow<Boolean>
        = settingsMap { it.useNativeKeyboard }

    fun getKeyboardUseNative(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.useNativeKeyboard }

    fun setKeyboardUseNative(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(useNativeKeyboard = value)
        }
    }

    val liveBrowseLastSeenVersion : Flow<String>
        = settingsMap { it.lastSeenVersion }

    fun getBrowseLastSeenVersion(cb : (String) -> Unit)
        = settingsOnce(cb) { it.lastSeenVersion }

    fun setBrowseLastSeenVersion(value : String) {
        settingsUpdate { settings ->
            settings.copy(lastSeenVersion = value)
        }
    }

    val livePlayMovementStrategy : Flow<MovementStrategy>
        = settingsMap(this::makePlayMovementStrategy)

    fun getPlayMovementStrategy(cb : (MovementStrategy) -> Unit)
        = settingsOnce(cb, this::makePlayMovementStrategy)

    val livePlayMovementStrategySetting : Flow<MovementStrategySetting>
        = settingsMap { it.movementStrategy }

    fun getPlayMovementStrategySetting(cb : (MovementStrategySetting) -> Unit)
        = settingsOnce(cb) { it.movementStrategy }

    fun setPlayMovementStrategySetting(value : MovementStrategySetting) {
        settingsUpdate { settings ->
            settings.copy(movementStrategy = value)
        }
    }

    val livePlayStopOnEndStrategy : Flow<MovementStrategy>
        = settingsMap(this::makeStopOnEndStrategy)

    val livePlayMovementStrategyClueList : Flow<MovementStrategy>
        = settingsMap(this::makeMovementStrategyClueList)

    fun getPlayMovementStrategyClueList(cb : (MovementStrategy) -> Unit)
        = settingsOnce(cb, this::makeMovementStrategyClueList)

    val livePlayMovementStrategyClueListSetting
        : Flow<MovementStrategySetting> = settingsMap(
            this::makeMovementStrategyClueListSetting,
        )

    fun getPlayMovementStrategyClueListSetting(
        cb : (MovementStrategySetting) -> Unit
    ) = settingsOnce(cb, this::makeMovementStrategyClueListSetting)

    fun setPlayMovementStrategyClueListSetting(
        value : MovementStrategySetting,
    ) {
        settingsUpdate { settings ->
            settings.copy(
                movementStrategyClueList
                    = coerceMovementStrategyClueList(value),
            )
        }
    }

    val liveAppOrientationLock : Flow<Orientation>
        = settingsMap { it.orientationLock }

    fun getAppOrientationLock(cb : (Orientation) -> Unit)
        = settingsOnce(cb) { it.orientationLock }

    fun setAppOrientationLock(value : Orientation) {
        settingsUpdate { settings ->
            settings.copy(orientationLock = value)
        }
    }

    val livePlayPlayLetterUndoEnabled : Flow<Boolean>
        = settingsMap { it.playLetterUndoEnabled }

    fun getPlayPlayLetterUndoEnabled(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.playLetterUndoEnabled }

    fun setPlayPlayLetterUndoEnabled(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(playLetterUndoEnabled = value)
        }
    }

    val livePlayPredictAnagramChars : Flow<Boolean>
        = settingsMap { it.predictAnagramChars }

    fun getPlayPredictAnagramChars(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.predictAnagramChars }

    fun setPlayPredictAnagramChars(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(predictAnagramChars = value)
        }
    }

    val livePlayPreserveCorrectLettersInShowErrors : Flow<Boolean>
        = settingsMap { it.preserveCorrectLettersInShowErrors }

    fun getPlayPreserveCorrectLettersInShowErrors(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.preserveCorrectLettersInShowErrors }

    fun setPlayPreserveCorrectLettersInShowErrors(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(preserveCorrectLettersInShowErrors = value)
        }
    }

    val livePlayRandomClueOnShake : Flow<Boolean>
        = settingsMap { it.randomClueOnShake }

    fun getPlayRandomClueOnShake(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.randomClueOnShake }

    fun setPlayRandomClueOnShake(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(randomClueOnShake = value)
        }
    }

    val livePlaySpecialEntryForceCaps : Flow<Boolean>
        = settingsMap { it.specialEntryForceCaps }

    fun getPlaySpecialEntryForceCaps(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.specialEntryForceCaps }

    fun setPlaySpecialEntryForceCaps(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(specialEntryForceCaps = value)
        }
    }

    val liveFileHandlerSafArchive : Flow<String>
        = settingsMap { it.safArchiveFolderUri }

    fun getFileHandlerSafArchive(cb : (String) -> Unit)
        = settingsOnce(cb) { it.safArchiveFolderUri }

    val liveFileHandlerSafCrosswords : Flow<String>
        = settingsMap { it.safCrosswordsFolderUri }

    fun getFileHandlerSafCrosswords(cb : (String) -> Unit)
        = settingsOnce(cb) { it.safCrosswordsFolderUri }

    val liveFileHandlerSafRoot : Flow<String>
        = settingsMap { it.safRootUri }

    fun getFileHandlerSafRoot(cb : (String) -> Unit)
        = settingsOnce(cb) { it.safRootUri }

    val liveFileHandlerSafToImportDone : Flow<String>
        = settingsMap { it.safToImportDoneFolderUri }

    fun getFileHandlerSafToImportDone(cb : (String) -> Unit)
        = settingsOnce(cb) { it.safToImportDoneFolderUri }

    val liveFileHandlerSafToImportFailed : Flow<String>
        = settingsMap { it.safToImportFailedFolderUri }

    fun getFileHandlerSafToImportFailed(cb : (String) -> Unit)
        = settingsOnce(cb) { it.safToImportFailedFolderUri }

    val liveFileHandlerSafToImport : Flow<String>
        = settingsMap { it.safToImportFolderUri }

    fun getFileHandlerSafToImport(cb : (String) -> Unit)
        = settingsOnce(cb) { it.safToImportFolderUri }

    val livePlayRenderSettings : Flow<RenderSettings>
        = settingsMap(this::makeRenderSettings)

    // Consumer for BoardEditViews.java
    fun getPlayRenderSettings(cb : Consumer<RenderSettings>)
        = settingsOnce(
            cb::accept,
            this::makeRenderSettings,
        )

    val livePlayScale : Flow<Float>
        = settingsMap { it.scale }

    fun getPlayScale(cb : (Float) -> Unit)
        = settingsOnce(cb) { it.scale }

    fun setPlayScale(value : Float) {
        if (0 <= value) {
            settingsUpdate { settings ->
                settings.copy(scale = value)
        }
        }
    }

    val liveScrapeCru : Flow<Boolean>
        = settingsMap { it.scrapeCru }

    fun getScrapeCru(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.scrapeCru }

    fun setScrapeCru(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(scrapeCru = value)
        }
    }

    val liveScrapeGuardianQuick : Flow<Boolean>
        = settingsMap { it.scrapeGuardianQuick }

    fun getScrapeGuardianQuick(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.scrapeGuardianQuick }

    fun setScrapeGuardianQuick(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(scrapeGuardianQuick = value)
        }
    }

    val liveScrapeKegler : Flow<Boolean>
        = settingsMap { it.scrapeKegler }

    fun getScrapeKegler(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.scrapeKegler }

    fun setScrapeKegler(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(scrapeKegler = value)
        }
    }

    val liveScrapePrivateEye : Flow<Boolean>
        = settingsMap { it.scrapePrivateEye }

    fun getScrapePrivateEye(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.scrapePrivateEye }

    fun setScrapePrivateEye(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(scrapePrivateEye = value)
        }
    }

    val liveScrapePrzekroj : Flow<Boolean>
        = settingsMap { it.scrapePrzekroj }

    fun getScrapePrzekroj(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.scrapePrzekroj }

    fun setScrapePrzekroj(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(scrapePrzekroj = value)
        }
    }

    val livePlayScratchMode : Flow<Boolean>
        = settingsMap { it.scratchMode }

    // Consumer for BoardEditView.java
    fun getPlayScratchMode(cb : Consumer<Boolean>)
        = settingsOnce(
            cb::accept,
            { it.scratchMode },
        )

    fun setPlayScratchMode(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(scratchMode = value)
        }
    }

    val livePlayShowClueTabs : Flow<Boolean>
        = settingsMap { it.showCluesOnPlayScreen }

    fun getPlayShowClueTabs(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.showCluesOnPlayScreen }

    fun setPlayShowClueTabs(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showCluesOnPlayScreen = value)
        }
    }

    val livePlayShowCount : Flow<Boolean>
        = settingsMap { it.showCount }

    fun getPlayShowCount(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.showCount }

    fun setPlayShowCount(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showCount = value)
        }
    }

    val livePlayShowErrorsGrid : Flow<Boolean>
        = settingsMap { it.showErrors }

    fun getPlayShowErrorsGrid(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.showErrors }

    fun setPlayShowErrorsGrid(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showErrors = value)
        }
    }

    val livePlayShowErrorsClue : Flow<Boolean>
        = settingsMap { it.showErrorsClue }

    fun getPlayShowErrorsClue(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.showErrorsClue }

    fun setPlayShowErrorsClue(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showErrorsClue = value)
        }
    }

    val livePlayShowErrorsCursor : Flow<Boolean>
        = settingsMap { it.showErrorsCursor }

    fun getPlayShowErrorsCursor(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.showErrorsCursor }

    fun setPlayShowErrorsCursor(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showErrorsCursor = value)
        }
    }

    val livePlayShowTimer : Flow<Boolean>
        = settingsMap { it.showTimer }

    fun getPlayShowTimer(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.showTimer }

    fun setPlayShowTimer(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showTimer = value)
        }
    }

    val liveClueListShowWords : Flow<Boolean>
        = settingsMap { it.showWordsInClueList }

    fun getClueListShowWords(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.showWordsInClueList }

    fun setClueListShowWords(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showWordsInClueList = value)
        }
    }

    val livePlaySkipFilled : Flow<Boolean>
        = settingsMap { it.skipFilled }

    // Consumer for BoardEditText.java
    fun getPlaySkipFilled(cb : Consumer<Boolean>)
        = settingsOnce(
            cb::accept,
            { it.skipFilled },
        )

    fun setPlaySkipFilled(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(skipFilled = value)
        }
    }

    val liveRatingsSettings : Flow<RatingsSettings>
        = settingsMap(this::makeRatingSettings)

    val liveClueListSnapToClue : Flow<Boolean>
        = settingsMap { it.snapClue }

    fun getClueListSnapToClue(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.snapClue }

    fun setClueListSnapToClue(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(snapClue = value)
        }
    }

    val liveBrowseSort : Flow<AccessorSetting>
        = settingsMap { it.sort }

    fun getBrowseSort(cb : (AccessorSetting) -> Unit)
        = settingsOnce(cb) { it.sort }

    fun setBrowseSort(value : AccessorSetting) {
        settingsUpdate { settings ->
            settings.copy(sort = value)
        }
    }

    val livePlaySpaceChangesDirection : Flow<Boolean>
        = settingsMap { it.spaceChangesDirection }

    fun getPlaySpaceChangesDirection(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.spaceChangesDirection }

    fun setPlaySpaceChangesDirection(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(spaceChangesDirection = value)
        }
    }

    val livePlaySuppressHintHighlighting : Flow<Boolean>
        = settingsMap { it.suppressHints }

    fun getPlaySuppressHintHighlighting(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.suppressHints }

    fun setPlaySuppressHintHighlighting(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(suppressHints = value)
        }
    }

    val liveDownloadSuppressMessages : Flow<Boolean>
        = settingsMap { it.suppressMessages }

    fun getDownloadSuppressIndividualNotifications(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.suppressMessages }

    fun setDownloadSuppressMessages(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(suppressMessages = value)
        }
    }

    val liveDownloadSuppressSummaryMessages : Flow<Boolean>
        = settingsMap { it.suppressSummaryMessages }

    fun getDownloadSuppressSummaryMessages(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.suppressSummaryMessages }

    fun setDownloadSuppressSummaryMessages(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(suppressSummaryMessages = value)
        }
    }

    fun disableNotifications() {
        setDownloadSuppressMessages(true)
        setDownloadSuppressSummaryMessages(true)
    }

    val liveBrowseSwipeAction : Flow<BrowseSwipeAction>
        = settingsMap { it.swipeAction }

    fun getBrowseSwipeAction(cb : (BrowseSwipeAction) -> Unit)
        = settingsOnce(cb) { it.swipeAction }

    fun setBrowseSwipeAction(value : BrowseSwipeAction) {
        settingsUpdate { settings ->
            settings.copy(swipeAction = value)
        }
    }

    val livePlayToggleBeforeMove : Flow<Boolean>
        = settingsMap { it.toggleBeforeMove }

    fun getPlayToggleBeforeMove(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.toggleBeforeMove }

    fun setPlayToggleBeforeMove(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(toggleBeforeMove = value)
        }
    }

    val liveAppDayNightMode : Flow<DayNightMode>
        = settingsMap { it.uiTheme }

    // consumer for NightModeHelper.java
    fun getAppDayNightMode(cb : Consumer<DayNightMode>)
        = settingsOnce(
            cb::accept,
            { it.uiTheme },
        )

    fun setAppDayNightMode(value : DayNightMode, cb : (() -> Unit)? = null) {
        settingsUpdate(cb) { settings ->
            settings.copy(uiTheme = value)
        }
    }

    fun setAppDayNightModeSync(value : DayNightMode) {
        runBlocking {
            settingsUpdate { settings ->
                settings.copy(uiTheme = value)
            }
        }
    }

    val liveVoiceVolumeActivatesVoice : Flow<Boolean>
        = settingsMap { it.volumeActivatesVoice }

    fun getVoiceVolumeActivatesVoice(cb : (Boolean) -> Unit)
        = settingsOnce(cb) { it.volumeActivatesVoice }

    fun setVoiceVolumeActivatesVoice(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(volumeActivatesVoice = value)
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

    private fun getFullMovementStrategy(
        movementStrategy : MovementStrategySetting,
        cycleUnfilledMode : CycleUnfilledMode,
    ) : MovementStrategy {
        val cycleForwards
            = cycleUnfilledMode != CycleUnfilledMode.CU_NEVER
        val cycleBackwards
            = cycleUnfilledMode == CycleUnfilledMode.CU_ALWAYS

        val strategy = when (movementStrategy) {
            MovementStrategySetting.MSS_MOVE_NEXT_ON_AXIS
                -> MovementStrategy.MOVE_NEXT_ON_AXIS
            MovementStrategySetting.MSS_STOP_ON_END
                -> MovementStrategy.STOP_ON_END
            MovementStrategySetting.MSS_MOVE_NEXT_CLUE
                -> MovementStrategy.MOVE_NEXT_CLUE
            MovementStrategySetting.MSS_MOVE_PARALLEL_WORD
                -> MovementStrategy.MOVE_PARALLEL_WORD
        }

        return if (cycleForwards || cycleBackwards) {
            MovementStrategy.CycleUnfilled(
                strategy, cycleForwards, cycleBackwards
            )
        } else {
            strategy
        }
    }

    /**
     * Coerce movement strategy into a value allowed on clue list
     */
    private fun coerceMovementStrategyClueList(
        settingValue : MovementStrategySetting,
    ) : MovementStrategySetting {
        return when (settingValue) {
            MovementStrategySetting.MSS_STOP_ON_END -> settingValue
            else -> MovementStrategySetting.MSS_MOVE_NEXT_CLUE
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
                json.optBoolean(key)?.let { setVoiceAlwaysAnnounceBox(it) }
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
            { s, key -> addListToJSON(json, key, s.autoDownloaders) },
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
                addListToJSON(json, key, s.backgroundDownloadDays)
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
            "llmAgent",
            { s, key -> json.put(key, s.llmAgent.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       println("FORKYZ: setts " + it)
                       setExtLLMAgent(LLMAgent.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "llmAgentAPIKey",
            { s, key -> json.put(key, s.llmAgentAPIKey) },
            { key -> json.optString(key)?.let { setExtLLMAgentAPIKey(it) } },
        )
        run(
            "llmVoiceCommands",
            { s, key -> json.put(key, s.llmVoiceCommands) },
            { key ->
                json.optBoolean(key)?.let { setVoiceLLMVoiceCommands(it) }
            },
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
            "dontConfirmCleanup",
            { s, key -> json.put(key, s.dontConfirmCleanup) },
            { key ->
                json.optBoolean(key)?.let { setBrowseDontConfirmCleanup(it) }
            },
        )
        run(
            "dontConfirmBrowseDelete",
            { s, key -> json.put(key, s.dontConfirmBrowseDelete) },
            { key ->
                json.optBoolean(key)?.let { setBrowseDontConfirmDelete(it) }
            },
        )
        run(
            "dontSnapBoardToBorders",
            { s, key -> json.put(key, s.dontSnapBoardToBorders) },
            { key ->
                json.optBoolean(key)?.let { setPlayDontSnapBoardToBorders(it) }
            },
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
            "downloadElPaisExperto",
            { s, key -> json.put(key, s.downloadElPaisExperto) },
            { key ->
                json.optBoolean(key)?.let { setDownloadElPaisExperto(it) }
            },
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
            "downloadLaCroix",
            { s, key -> json.put(key, s.downloadLaCroix) },
            { key -> json.optBoolean(key)?.let { setDownloadLaCroix(it) } },
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
            "downloadNRCInHetMidden",
            { s, key -> json.put(key, s.downloadNRCInHetMidden) },
            { key -> json.optBoolean(key)?.let { setDownloadNRCInHetMidden(it) } },
        )
        run(
            "downloadNRCScrypto",
            { s, key -> json.put(key, s.downloadNRCScrypto) },
            { key -> json.optBoolean(key)?.let { setDownloadNRCScrypto(it) } },
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
            "downloadSudOuestMotsCroises",
            { s, key -> json.put(key, s.downloadSudOuestMotsCroises) },
            { key -> json.optBoolean(key)?.let {
                setDownloadSudOuestMotsCroises(it) }
            },
        )
        run(
            "downloadSudOuestMotsFleches",
            { s, key -> json.put(key, s.downloadSudOuestMotsFleches) },
            { key -> json.optBoolean(key)?.let {
                setDownloadSudOuestMotsFleches(it) }
            },
        )
        run(
            "downloadTF1MotsCroises",
            { s, key -> json.put(key, s.downloadTF1MotsCroises) },
            { key -> json.optBoolean(key)?.let {
                setDownloadTF1MotsCroises(it) }
            },
        )
        run(
            "downloadTF1MotsFleches",
            { s, key -> json.put(key, s.downloadTF1MotsFleches) },
            { key -> json.optBoolean(key)?.let {
                setDownloadTF1MotsFleches(it) }
            },
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
            "expandableClueLine",
            { s, key -> json.put(key, s.expandableClueLine) },
            { key ->
                json.optBoolean(key)?.let { setPlayExpandableClueLine(it) }
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
            "movementStrategyClueList",
            { s, key -> json.put(key, s.movementStrategyClueList.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                        setPlayMovementStrategyClueListSetting(
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
            "clueListNameInClueLine",
            { s, key -> json.put(key, s.clueListNameInClueLine.toString()) },
            { key ->
               json.optString(key)?.let {
                   try {
                       setPlayClueListNameInClueLine(
                           ClueListClueLine.valueOf(it)
                       )
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "showCluesOnPlayScreen",
            { s, key -> json.put(key, s.showCluesOnPlayScreen) },
            { key -> json.optBoolean(key)?.let { setPlayShowClueTabs(it) } },
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
                       setBrowseSort(AccessorSetting.valueOf(it))
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
        run(
            "specialEntryForceCaps",
            { s, key -> json.put(key, s.specialEntryForceCaps) },
            { key ->
                json.optBoolean(key)?.let { setPlaySpecialEntryForceCaps(it) }
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
