
package app.crossword.yourealwaysbe.forkyz.settings

import java.io.BufferedWriter
import java.time.LocalDate
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

import app.crossword.yourealwaysbe.forkyz.net.DownloaderID
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

    val liveAppUndecorated : Flow<Boolean>
        = settingsMap { it.undecorated }

    fun setAppUndecorated(value : Boolean) {
        settingsUpdate { settings -> settings.copy(undecorated = value) }
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

    val liveBrowseCollapseHeadings : Flow<Boolean>
        = settingsMap { it.collapseBrowseHeadings }

    fun setBrowseCollapseHeadings(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(collapseBrowseHeadings = value)
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

    fun setDownloadAllowRoaming(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadAllowRoaming = value)
        }
    }

    fun liveDownloadAutoDownloaderBlockMon(
        downloaderID : DownloaderID,
    ) : Flow<Boolean>
        = settingsMap { downloaderID.id in it.autoDownloadersBlockMon }

    private fun setDownloadAutoDownloadersBlockMon(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(autoDownloadersBlockMon = value.toList())
        }
    }

    fun setDownloadAutoDownloaderBlockMon(
        downloaderID : DownloaderID,
        blocked : Boolean,
    ) {
        settingsUpdate { settings ->
            settings.copy(
                autoDownloadersBlockMon = toggleStringSet(
                    settings.autoDownloadersBlockMon,
                    downloaderID.id,
                    blocked,
                ),
            )
        }
    }

    fun liveDownloadAutoDownloaderBlockTue(
        downloaderID : DownloaderID,
    ) : Flow<Boolean>
        = settingsMap { downloaderID.id in it.autoDownloadersBlockTue }

    private fun setDownloadAutoDownloadersBlockTue(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(autoDownloadersBlockTue = value.toList())
        }
    }

    fun setDownloadAutoDownloaderBlockTue(
        downloaderID : DownloaderID,
        blocked : Boolean,
    ) {
        settingsUpdate { settings ->
            settings.copy(
                autoDownloadersBlockTue = toggleStringSet(
                    settings.autoDownloadersBlockTue,
                    downloaderID.id,
                    blocked,
                ),
            )
        }
    }

    fun liveDownloadAutoDownloaderBlockWed(
        downloaderID : DownloaderID,
    ) : Flow<Boolean>
        = settingsMap { downloaderID.id in it.autoDownloadersBlockWed }

    private fun setDownloadAutoDownloadersBlockWed(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(autoDownloadersBlockWed = value.toList())
        }
    }

    fun setDownloadAutoDownloaderBlockWed(
        downloaderID : DownloaderID,
        blocked : Boolean,
    ) {
        settingsUpdate { settings ->
            settings.copy(
                autoDownloadersBlockWed = toggleStringSet(
                    settings.autoDownloadersBlockWed,
                    downloaderID.id,
                    blocked,
                ),
            )
        }
    }

    fun liveDownloadAutoDownloaderBlockThu(
        downloaderID : DownloaderID,
    ) : Flow<Boolean>
        = settingsMap { downloaderID.id in it.autoDownloadersBlockThu }

    private fun setDownloadAutoDownloadersBlockThu(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(autoDownloadersBlockThu = value.toList())
        }
    }

    fun setDownloadAutoDownloaderBlockThu(
        downloaderID : DownloaderID,
        blocked : Boolean,
    ) {
        settingsUpdate { settings ->
            settings.copy(
                autoDownloadersBlockThu = toggleStringSet(
                    settings.autoDownloadersBlockThu,
                    downloaderID.id,
                    blocked,
                ),
            )
        }
    }

    fun liveDownloadAutoDownloaderBlockFri(
        downloaderID : DownloaderID,
    ) : Flow<Boolean>
        = settingsMap { downloaderID.id in it.autoDownloadersBlockFri }

    private fun setDownloadAutoDownloadersBlockFri(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(autoDownloadersBlockFri = value.toList())
        }
    }

    fun setDownloadAutoDownloaderBlockFri(
        downloaderID : DownloaderID,
        blocked : Boolean,
    ) {
        settingsUpdate { settings ->
            settings.copy(
                autoDownloadersBlockFri = toggleStringSet(
                    settings.autoDownloadersBlockFri,
                    downloaderID.id,
                    blocked,
                ),
            )
        }
    }

    fun liveDownloadAutoDownloaderBlockSat(
        downloaderID : DownloaderID,
    ) : Flow<Boolean>
        = settingsMap { downloaderID.id in it.autoDownloadersBlockSat }

    private fun setDownloadAutoDownloadersBlockSat(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(autoDownloadersBlockSat = value.toList())
        }
    }

    fun setDownloadAutoDownloaderBlockSat(
        downloaderID : DownloaderID,
        blocked : Boolean,
    ) {
        settingsUpdate { settings ->
            settings.copy(
                autoDownloadersBlockSat = toggleStringSet(
                    settings.autoDownloadersBlockSat,
                    downloaderID.id,
                    blocked,
                ),
            )
        }
    }

    fun liveDownloadAutoDownloaderBlockSun(
        downloaderID : DownloaderID,
    ) : Flow<Boolean>
        = settingsMap { downloaderID.id in it.autoDownloadersBlockSun }

    private fun setDownloadAutoDownloadersBlockSun(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(autoDownloadersBlockSun = value.toList())
        }
    }

    fun setDownloadAutoDownloaderBlockSun(
        downloaderID : DownloaderID,
        blocked : Boolean,
    ) {
        settingsUpdate { settings ->
            settings.copy(
                autoDownloadersBlockSun = toggleStringSet(
                    settings.autoDownloadersBlockSun,
                    downloaderID.id,
                    blocked,
                ),
            )
        }
    }

    val liveDownloadDays : Flow<Set<String>>
        = settingsMap { it.backgroundDownloadDays.toSet() }

    fun setDownloadDays(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadDays = value.toList())
        }
    }

    val liveDownloadDaysTime : Flow<Int>
        = settingsMap { it.backgroundDownloadDaysTime }

    fun setDownloadDaysTime(value : Int) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadDaysTime = value)
        }
    }

    fun liveDownloadEnabledDownloader(
        downloaderID : DownloaderID,
    ) : Flow<Boolean> {
        return settingsMap { downloaderID.id !in it.disabledDownloaders }
    }

    private fun setDownloadDisabledDownloaders(value : Set<String>) {
        settingsUpdate { settings ->
            settings.copy(disabledDownloaders = value.toList())
        }
    }

    /**
     * Set enabled/disabled for a given downloader
     */
    fun setDownloadEnableDownloader(
        downloaderID : DownloaderID,
        enabled : Boolean,
    ) {
        settingsUpdate { settings ->
            settings.copy(
                disabledDownloaders = toggleStringSet(
                    settings.disabledDownloaders,
                    downloaderID.id,
                    !enabled,
                ),
            )
        }
    }

    val liveDownloadHourly : Flow<Boolean>
        = settingsMap { it.backgroundDownloadHourly }

    fun setDownloadHourly(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadHourly = value)
        }
    }

    val liveDownloadRequireCharging : Flow<Boolean>
        = settingsMap { it.backgroundDownloadRequireCharging }

    fun setDownloadRequireCharging(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(backgroundDownloadRequireCharging = value)
        }
    }

    val liveDownloadRequireUnmetered : Flow<Boolean>
        = settingsMap { it.backgroundDownloadRequireUnmetered }

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

    fun setBrowseIndicateIfSolution(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(browseIndicateIfSolution = value)
        }
    }

    val liveBrowseNewPuzzle : Flow<Boolean>
        = settingsMap { it.browseNewPuzzle }

    fun setBrowseNewPuzzle(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(browseNewPuzzle = value)
        }
    }

    val liveBrowseShowPercentageCorrect : Flow<Boolean>
        = settingsMap { it.browseShowPercentageCorrect }

    fun setBrowseShowPercentageCorrect(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(browseShowPercentageCorrect = value)
        }
    }

    val liveVoiceButtonActivatesVoice : Flow<Boolean>
        = settingsMap { it.buttonActivatesVoice }

    fun setVoiceButtonActivatesVoice(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(buttonActivatesVoice = value)
        }
    }

    val liveVoiceButtonAnnounceClue : Flow<Boolean>
        = settingsMap { it.buttonAnnounceClue }

    fun setVoiceButtonAnnounceClue(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(buttonAnnounceClue = value)
        }
    }

    val liveExtLLMAgent : Flow<LLMAgent>
        = settingsMap { it.llmAgent }

    fun setExtLLMAgent(value : LLMAgent) {
        settingsUpdate { settings ->
            settings.copy(llmAgent = value)
        }
    }

    val liveExtLLMAgentAPIKey : Flow<String>
        = settingsMap { it.llmAgentAPIKey }

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

    fun setPlayClueBelowGrid(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(clueBelowGrid = value)
        }
    }

    val livePlayClueHighlight : Flow<ClueHighlight>
        = settingsMap { it.clueHighlight }

    fun setPlayClueHighlight(value : ClueHighlight) {
        settingsUpdate { settings ->
            settings.copy(clueHighlight = value)
        }
    }

    val livePlayExpandableClueLine : Flow<Boolean>
        = settingsMap { it.expandableClueLine }

    fun setPlayExpandableClueLine(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(expandableClueLine = value)
        }
    }

    val livePlayClueListNameInClueLine : Flow<ClueListClueLine>
        = settingsMap { it.clueListNameInClueLine }

    fun setPlayClueListNameInClueLine(value : ClueListClueLine) {
        settingsUpdate { settings ->
            settings.copy(clueListNameInClueLine = value)
        }
    }

    val livePlayDontSnapBoardToBorders : Flow<Boolean>
        = settingsMap { it.dontSnapBoardToBorders }

    fun setPlayDontSnapBoardToBorders(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(dontSnapBoardToBorders = value)
        }
    }

    val liveClueListClueTabsDouble : Flow<ClueTabsDouble>
        = settingsMap { it.clueTabsDouble }

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
            settings.disabledDownloaders.toSet(),
            settings.customDailyTitle,
            settings.customDailyUrl,
            settings.suppressSummaryMessages,
            settings.suppressMessages,
            settings.downloadTimeout,
            settings.dlOnStartup,
            settings.autoDownloadersBlockMon.toSet(),
            settings.autoDownloadersBlockTue.toSet(),
            settings.autoDownloadersBlockWed.toSet(),
            settings.autoDownloadersBlockThu.toSet(),
            settings.autoDownloadersBlockFri.toSet(),
            settings.autoDownloadersBlockSat.toSet(),
            settings.autoDownloadersBlockSun.toSet(),
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

    fun setExtCrosswordSolverEnabled(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(crosswordSolverEnabled = value)
        }
    }

    val liveDownloadCustomDailyTitle : Flow<String>
        = settingsMap { it.customDailyTitle }

    fun setDownloadCustomDailyTitle(value : String) {
        settingsUpdate { settings ->
            settings.copy(customDailyTitle = value)
        }
    }

    val liveDownloadCustomDailyURL : Flow<String>
        = settingsMap { it.customDailyUrl }

    fun setDownloadCustomDailyURL(value : String) {
        settingsUpdate { settings ->
            settings.copy(customDailyUrl = value)
        }
    }

    val livePlayCycleUnfilledMode : Flow<CycleUnfilledMode>
        = settingsMap { it.cycleUnfilledMode }

    fun setPlayCycleUnfilledMode(value : CycleUnfilledMode) {
        settingsUpdate { settings ->
            settings.copy(cycleUnfilledMode = value)
        }
    }

    val livePlayDeleteCrossingMode : Flow<DeleteCrossingMode>
        = settingsMap(this::makeDeleteCrossingMode)

    val livePlayDeleteCrossingModeSetting : Flow<DeleteCrossingModeSetting>
        = settingsMap { it.deleteCrossingMode }

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

    fun setPlayScratchDisplay(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(displayScratch = value)
        }
    }

    val livePlayDisplaySeparators : Flow<DisplaySeparators>
        = settingsMap { it.displaySeparators }

    fun setPlayDisplaySeparators(value : DisplaySeparators) {
        settingsUpdate { settings ->
            settings.copy(displaySeparators = value)
        }
    }

    val liveBrowseLastDownload : Flow<Long>
        = settingsMap { it.dlLast }

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

    fun setDownloadOnStartUp(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(dlOnStartup = value)
        }
    }

    val liveDownloadLastSelectedDate : Flow<LocalDate?>
        = settingsMap { settings ->
            settings.downloadLastSelectedDate
                .takeIf { it.isNotEmpty() }
                ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        }

    fun setDownloadLastSelectedDate(value : LocalDate?) {
        settingsUpdate { settings ->
            settings.copy(downloadLastSelectedDate = value?.toString() ?: "")
        }
    }

    val liveDownloadRememberDate : Flow<Boolean>
        = settingsMap { it.downloadRememberDate }

    fun setDownloadRememberDate(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(downloadRememberDate = value)
        }
    }

    val livePlayDoubleTapFitBoard : Flow<Boolean>
        = settingsMap { it.doubleTap }

    // Consumer for BoardEditViews.java
    fun setPlayDoubleTapFitBoard(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(doubleTap = value)
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

    val liveExtDuckDuckGoEnabled : Flow<Boolean>
        = settingsMap { it.duckDuckGoEnabled }

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

    fun setVoiceEqualsAnnounceClue(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(equalsAnnounceClue = value)
        }
    }

    val liveVoiceLLMVoiceCommands : Flow<Boolean>
        = settingsMap { it.llmVoiceCommands }

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

    val liveExtFifteenSquaredEnabled : Flow<Boolean>
        = settingsMap { it.fifteenSquaredEnabled }

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

    fun setPlayFullScreen(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(fullScreen = value)
        }
    }

    val livePlayGridRatioPortrait : Flow<GridRatio>
        = settingsMap { it.gridRatio }

    fun setPlayGridRatioPortrait(value : GridRatio) {
        settingsUpdate { settings ->
            settings.copy(gridRatio = value)
        }
    }

    val livePlayGridRatioLandscape : Flow<GridRatio>
        = settingsMap { it.gridRatioLand }

    fun setPlayGridRatioLandscape(value : GridRatio) {
        settingsUpdate { settings ->
            settings.copy(gridRatioLand = value)
        }
    }

    val livePlayIndicateShowErrors : Flow<Boolean>
        = settingsMap { it.indicateShowErrors }

    fun setPlayIndicateShowErrors(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(indicateShowErrors = value)
        }
    }

    val livePlayInferSeparators : Flow<Boolean>
        = settingsMap { it.inferSeparators }

    fun setPlayInferSeparators(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(inferSeparators = value)
        }
    }

    val liveKeyboardCompact : Flow<Boolean>
        = settingsMap { it.keyboardCompact }

    fun setKeyboardCompact(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(keyboardCompact = value)
        }
    }

    val liveKeyboardForceCaps : Flow<Boolean>
        = settingsMap { it.keyboardForceCaps }

    fun setKeyboardForceCaps(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(keyboardForceCaps = value)
        }
    }

    val liveKeyboardHaptic : Flow<Boolean>
        = settingsMap { it.keyboardHaptic }

    fun setKeyboardHaptic(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(keyboardHaptic = value)
        }
    }

    val liveKeyboardHideButton : Flow<Boolean>
        = settingsMap { it.keyboardHideButton }

    fun setKeyboardHideButton(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(keyboardHideButton = value)
        }
    }

    val liveKeyboardLayout : Flow<KeyboardLayout>
        = settingsMap { it.keyboardLayout }

    fun setKeyboardLayout(value : KeyboardLayout) {
        settingsUpdate { settings ->
            settings.copy(keyboardLayout = value)
        }
    }

    val liveKeyboardMode : Flow<KeyboardMode>
        = settingsMap { it.keyboardShowHide }

    fun setKeyboardMode(value : KeyboardMode) {
        settingsUpdate { settings ->
            settings.copy(keyboardShowHide = value)
        }
    }

    val liveKeyboardRepeatDelay : Flow<Int>
        = settingsMap { it.keyboardRepeatDelay }

    fun setKeyboardRepeatDelay(value : Int) {
        settingsUpdate { settings ->
            settings.copy(keyboardRepeatDelay = value)
        }
    }

    val liveKeyboardRepeatInterval : Flow<Int>
        = settingsMap { it.keyboardRepeatInterval }

    fun setKeyboardRepeatInterval(value : Int) {
        settingsUpdate { settings ->
            settings.copy(keyboardRepeatInterval = value)
        }
    }

    val liveKeyboardSettings : Flow<KeyboardSettings>
        = settingsMap(this::makeKeyboardSettings)

    fun getKeyboardSettings(cb : (KeyboardSettings) -> Unit)
        = settingsOnce(cb, this::makeKeyboardSettings)

    val liveKeyboardSplitWords : Flow<Boolean>
        = settingsMap { it.keyboardSplitWords }

    fun setKeyboardSplitWords(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(keyboardSplitWords = value)
        }
    }

    val liveKeyboardUseNative : Flow<Boolean>
        = settingsMap { it.useNativeKeyboard }

    fun setKeyboardUseNative(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(useNativeKeyboard = value)
        }
    }

    val liveBrowseLastSeenVersion : Flow<String>
        = settingsMap { it.lastSeenVersion }

    fun setBrowseLastSeenVersion(value : String) {
        settingsUpdate { settings ->
            settings.copy(lastSeenVersion = value)
        }
    }

    val livePlayMovementStrategy : Flow<MovementStrategy>
        = settingsMap(this::makePlayMovementStrategy)

    val livePlayMovementStrategySetting : Flow<MovementStrategySetting>
        = settingsMap { it.movementStrategy }

    fun setPlayMovementStrategySetting(value : MovementStrategySetting) {
        settingsUpdate { settings ->
            settings.copy(movementStrategy = value)
        }
    }

    val livePlayStopOnEndStrategy : Flow<MovementStrategy>
        = settingsMap(this::makeStopOnEndStrategy)

    val livePlayMovementStrategyClueList : Flow<MovementStrategy>
        = settingsMap(this::makeMovementStrategyClueList)

    val livePlayMovementStrategyClueListSetting
        : Flow<MovementStrategySetting> = settingsMap(
            this::makeMovementStrategyClueListSetting,
        )

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

    fun setAppOrientationLock(value : Orientation) {
        settingsUpdate { settings ->
            settings.copy(orientationLock = value)
        }
    }

    val livePlayPlayLetterUndoEnabled : Flow<Boolean>
        = settingsMap { it.playLetterUndoEnabled }

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

    fun setPlayPreserveCorrectLettersInShowErrors(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(preserveCorrectLettersInShowErrors = value)
        }
    }

    val livePlayRandomClueOnShake : Flow<Boolean>
        = settingsMap { it.randomClueOnShake }

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

    val liveFileHandlerSafCrosswords : Flow<String>
        = settingsMap { it.safCrosswordsFolderUri }

    val liveFileHandlerSafRoot : Flow<String>
        = settingsMap { it.safRootUri }

    val liveFileHandlerSafToImportDone : Flow<String>
        = settingsMap { it.safToImportDoneFolderUri }

    val liveFileHandlerSafToImportFailed : Flow<String>
        = settingsMap { it.safToImportFailedFolderUri }

    val liveFileHandlerSafToImport : Flow<String>
        = settingsMap { it.safToImportFolderUri }

    val livePlayRenderSettings : Flow<RenderSettings>
        = settingsMap(this::makeRenderSettings)

    // Consumer for BoardEditViews.java
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

    fun setPlayShowClueTabs(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showCluesOnPlayScreen = value)
        }
    }

    val livePlayShowCount : Flow<Boolean>
        = settingsMap { it.showCount }

    fun setPlayShowCount(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showCount = value)
        }
    }

    val livePlayShowErrorsGrid : Flow<Boolean>
        = settingsMap { it.showErrors }

    fun setPlayShowErrorsGrid(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showErrors = value)
        }
    }

    val livePlayShowErrorsClue : Flow<Boolean>
        = settingsMap { it.showErrorsClue }

    fun setPlayShowErrorsClue(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showErrorsClue = value)
        }
    }

    val livePlayShowErrorsCursor : Flow<Boolean>
        = settingsMap { it.showErrorsCursor }

    fun setPlayShowErrorsCursor(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showErrorsCursor = value)
        }
    }

    val livePlayShowTimer : Flow<Boolean>
        = settingsMap { it.showTimer }

    fun setPlayShowTimer(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showTimer = value)
        }
    }

    val liveClueListShowWords : Flow<Boolean>
        = settingsMap { it.showWordsInClueList }

    fun setClueListShowWords(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(showWordsInClueList = value)
        }
    }

    val livePlaySkipFilled : Flow<Boolean>
        = settingsMap { it.skipFilled }

    // Consumer for BoardEditText.java
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

    fun setPlaySuppressHintHighlighting(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(suppressHints = value)
        }
    }

    val liveDownloadSuppressMessages : Flow<Boolean>
        = settingsMap { it.suppressMessages }

    fun setDownloadSuppressMessages(value : Boolean) {
        settingsUpdate { settings ->
            settings.copy(suppressMessages = value)
        }
    }

    val liveDownloadSuppressSummaryMessages : Flow<Boolean>
        = settingsMap { it.suppressSummaryMessages }

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

    private fun JSONObject.ifHasBoolean(key : String, cb : (Boolean) -> Unit) {
        if (has(key))
            cb(optBoolean(key))
    }

    private fun JSONObject.ifHasString(key : String, cb : (String) -> Unit) {
        if (has(key))
            cb(optString(key))
    }

    private fun JSONObject.ifHasInt(key : String, cb : (Int) -> Unit) {
        if (has(key))
            cb(optInt(key))
    }

    private fun JSONObject.ifHasLong(key : String, cb : (Long) -> Unit) {
        if (has(key))
            cb(optLong(key))
    }

    private fun JSONObject.ifHasDouble(key : String, cb : (Double) -> Unit) {
        if (has(key))
            cb(optDouble(key))
    }

    private fun toggleStringSet(
        stringSet : List<String>,
        s : String,
        contained : Boolean,
    ) : List<String> {
        return if (contained) { stringSet + s } else { stringSet - s }
            .toSet().toList()
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
                json.ifHasBoolean(key) { setVoiceAlwaysAnnounceBox(it) }
            },
        )
        run(
            "alwaysAnnounceClue",
            { s, key -> json.put(key, s.alwaysAnnounceClue) },
            { key ->
                json.ifHasBoolean(key) { setVoiceAlwaysAnnounceClue(it) }
            },
        )
        run(
            "applicationTheme",
            { s, key -> json.put(key, s.applicationTheme.toString()) },
            { key ->
               json.ifHasString(key) {
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
                json.ifHasInt(key) { setBrowseCleanupAgeArchive(it) }
            },
        )
        run(
            "autoDownloadersBlockMon",
            { s, key -> addListToJSON(json, key, s.autoDownloadersBlockMon) },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadAutoDownloadersBlockMon,
                )
            },
        )
        run(
            "autoDownloadersBlockTue",
            { s, key -> addListToJSON(json, key, s.autoDownloadersBlockTue) },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadAutoDownloadersBlockTue,
                )
            },
        )
        run(
            "autoDownloadersBlockWed",
            { s, key -> addListToJSON(json, key, s.autoDownloadersBlockWed) },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadAutoDownloadersBlockWed,
                )
            },
        )
        run(
            "autoDownloadersBlockThu",
            { s, key -> addListToJSON(json, key, s.autoDownloadersBlockThu) },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadAutoDownloadersBlockThu,
                )
            },
        )
        run(
            "autoDownloadersBlockFri",
            { s, key -> addListToJSON(json, key, s.autoDownloadersBlockFri) },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadAutoDownloadersBlockFri,
                )
            },
        )
        run(
            "autoDownloadersBlockSat",
            { s, key -> addListToJSON(json, key, s.autoDownloadersBlockSat) },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadAutoDownloadersBlockSat,
                )
            },
        )
        run(
            "autoDownloadersBlockSun",
            { s, key -> addListToJSON(json, key, s.autoDownloadersBlockSun) },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadAutoDownloadersBlockSun,
                )
            },
        )
        run(
            "backgroundDownloadAllowRoaming",
            { s, key -> json.put(key, s.backgroundDownloadAllowRoaming) },
            { key ->
                json.ifHasBoolean(key) { setDownloadAllowRoaming(it) }
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
            { key -> json.ifHasInt(key) { setDownloadDaysTime(it) } },
        )
        run(
            "backgroundDownloadHourly",
            { s, key -> json.put(key, s.backgroundDownloadHourly) },
            { key -> json.ifHasBoolean(key) { setDownloadHourly(it) } },
        )
        run(
            "backgroundDownloadRequireCharging",
            { s, key -> json.put(key, s.backgroundDownloadRequireCharging) },
            { key ->
                json.ifHasBoolean(key) { setDownloadRequireCharging(it) }
            },
        )
        run(
            "backgroundDownloadRequireUnmetered",
            { s, key -> json.put(key, s.backgroundDownloadRequireUnmetered) },
            { key ->
                json.ifHasBoolean(key) { setDownloadRequireUnmetered(it) }
            },
        )
        run(
            "browseAlwaysShowRating",
            { s, key -> json.put(key, s.browseAlwaysShowRating) },
            { key ->
                json.ifHasBoolean(key) { setBrowseAlwaysShowRating(it) }
            },
        )
        run(
            "browseIndicateIfSolution",
            { s, key -> json.put(key, s.browseIndicateIfSolution) },
            { key ->
                json.ifHasBoolean(key) { setBrowseIndicateIfSolution(it) }
            },
        )
        run(
            "browseNewPuzzle",
            { s, key -> json.put(key, s.browseNewPuzzle) },
            { key -> json.ifHasBoolean(key) { setBrowseNewPuzzle(it) } },
        )
        run(
            "browseShowPercentageCorrect",
            { s, key -> json.put(key, s.browseShowPercentageCorrect) },
            { key ->
                json.ifHasBoolean(key) { setBrowseShowPercentageCorrect(it) }
            },
        )
        run(
            "buttonActivatesVoice",
            { s, key -> json.put(key, s.buttonActivatesVoice) },
            { key ->
                json.ifHasBoolean(key) { setVoiceButtonActivatesVoice(it) }
            },
        )
        run(
            "buttonAnnounceClue",
            { s, key -> json.put(key, s.buttonAnnounceClue) },
            { key ->
                json.ifHasBoolean(key) { setVoiceButtonAnnounceClue(it) }
            },
        )
        run(
            "llmAgent",
            { s, key -> json.put(key, s.llmAgent.toString()) },
            { key ->
               json.ifHasString(key) {
                   try {
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
            { key -> json.ifHasString(key) { setExtLLMAgentAPIKey(it) } },
        )
        run(
            "llmVoiceCommands",
            { s, key -> json.put(key, s.llmVoiceCommands) },
            { key ->
                json.ifHasBoolean(key) { setVoiceLLMVoiceCommands(it) }
            },
        )
        run(
            "cleanupAge",
            { s, key -> json.put(key, s.cleanupAge) },
            { key -> json.ifHasInt(key) { setBrowseCleanupAge(it) } },
        )
        run(
            "clueBelowGrid",
            { s, key -> json.put(key, s.clueBelowGrid) },
            { key -> json.ifHasBoolean(key) { setPlayClueBelowGrid(it) } },
        )
        run(
            "clueHighlight",
            { s, key -> json.put(key, s.clueHighlight.toString()) },
            { key ->
               json.ifHasString(key) {
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
               json.ifHasString(key) {
                   try {
                       setClueListClueTabsDouble(ClueTabsDouble.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "collapseBrowseHeadings",
            { s, key -> json.put(key, s.collapseBrowseHeadings) },
            { key ->
                json.ifHasBoolean(key) { setBrowseCollapseHeadings(it) }
            },
        )
        run(
            "crosswordSolverEnabled",
            { s, key -> json.put(key, s.crosswordSolverEnabled) },
            { key ->
                json.ifHasBoolean(key) { setExtCrosswordSolverEnabled(it) }
            },
        )
        run(
            "customDailyTitle",
            { s, key -> json.put(key, s.customDailyTitle) },
            { key ->
                json.ifHasString(key) { setDownloadCustomDailyTitle(it) }
            },
        )
        run(
            "customDailyUrl",
            { s, key -> json.put(key, s.customDailyUrl) },
            { key ->
                json.ifHasString(key) { setDownloadCustomDailyURL(it) }
            },
        )
        run(
            "cycleUnfilledMode",
            { s, key -> json.put(key, s.cycleUnfilledMode.toString()) },
            { key ->
               json.ifHasString(key) {
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
               json.ifHasString(key) {
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
                json.ifHasBoolean(key) { setBrowseDeleteOnCleanup(it) }
            },
        )
        run(
            "disabledDownloaders",
            { s, key -> addListToJSON(json, key, s.disabledDownloaders) },
            { key ->
                getStringSetFromJSON(
                    json, key, this::setDownloadDisabledDownloaders,
                )
            },
        )
        run(
            "disableRatings",
            { s, key -> json.put(key, s.disableRatings) },
            { key ->
                json.ifHasBoolean(key) { setRatingsDisableRatings(it) }
            },
        )
        run(
            "disableSwipe",
            { s, key -> json.put(key, s.disableSwipe) },
            { key -> json.ifHasBoolean(key) { setBrowseDisableSwipe(it) } },
        )
        run(
            "displayScratch",
            { s, key -> json.put(key, s.displayScratch) },
            { key -> json.ifHasBoolean(key) { setPlayScratchDisplay(it) } },
        )
        run(
            "displaySeparators",
            { s, key -> json.put(key, s.displaySeparators.toString()) },
            { key ->
               json.ifHasString(key) {
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
            { key -> json.ifHasLong(key) { setBrowseLastDownload(it) } },
        )
        run(
            "dlOnStartup",
            { s, key -> json.put(key, s.dlOnStartup) },
            { key -> json.ifHasBoolean(key) { setDownloadOnStartUp(it) } },
        )
        run(
            "dontConfirmCleanup",
            { s, key -> json.put(key, s.dontConfirmCleanup) },
            { key ->
                json.ifHasBoolean(key) { setBrowseDontConfirmCleanup(it) }
            },
        )
        run(
            "dontConfirmBrowseDelete",
            { s, key -> json.put(key, s.dontConfirmBrowseDelete) },
            { key ->
                json.ifHasBoolean(key) { setBrowseDontConfirmDelete(it) }
            },
        )
        run(
            "dontSnapBoardToBorders",
            { s, key -> json.put(key, s.dontSnapBoardToBorders) },
            { key ->
                json.ifHasBoolean(key) { setPlayDontSnapBoardToBorders(it) }
            },
        )
        run(
            "doubleTap",
            { s, key -> json.put(key, s.doubleTap) },
            { key ->
                json.ifHasBoolean(key) { setPlayDoubleTapFitBoard(it) }
            },
        )
        run(
            "downloadRememberDate",
            { s, key -> json.put(key, s.downloadRememberDate) },
            { key ->
                json.ifHasBoolean(key) { setDownloadRememberDate(it) }
            },
        )
        run(
            "downloadTimeout",
            { s, key -> json.put(key, s.downloadTimeout) },
            { key -> json.ifHasInt(key) { setDownloadTimeout(it) } },
        )
        run(
            "duckDuckGoEnabled",
            { s, key -> json.put(key, s.duckDuckGoEnabled) },
            { key ->
                json.ifHasBoolean(key) { setExtDuckDuckGoEnabled(it) }
            },
        )
        run(
            "ensureVisible",
            { s, key -> json.put(key, s.ensureVisible) },
            { key -> json.ifHasBoolean(key) { setPlayEnsureVisible(it) } },
        )
        run(
            "enterChangesDirection",
            { s, key -> json.put(key, s.enterChangesDirection) },
            { key ->
                json.ifHasBoolean(key) { setPlayEnterChangesDirection(it) }
            },
        )
        run(
            "equalsAnnounceClue",
            { s, key -> json.put(key, s.equalsAnnounceClue) },
            { key ->
                json.ifHasBoolean(key) { setVoiceEqualsAnnounceClue(it) }
            },
        )
        run(
            "expandableClueLine",
            { s, key -> json.put(key, s.expandableClueLine) },
            { key ->
                json.ifHasBoolean(key) { setPlayExpandableClueLine(it) }
            },
        )
        run(
            "externalDictionary",
            { s, key -> json.put(key, s.externalDictionary.toString()) },
            { key ->
               json.ifHasString(key) {
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
                json.ifHasBoolean(key) { setExtFifteenSquaredEnabled(it) }
            },
        )
        run(
            "fitToScreenMode",
            { s, key -> json.put(key, s.fitToScreenMode.toString()) },
            { key ->
               json.ifHasString(key) {
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
            { key -> json.ifHasBoolean(key) { setPlayFullScreen(it) } },
        )
        run(
            "gridRatio",
            { s, key -> json.put(key, s.gridRatio.toString()) },
            { key ->
               json.ifHasString(key) {
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
               json.ifHasString(key) {
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
                json.ifHasBoolean(key) { setPlayIndicateShowErrors(it) }
            },
        )
        run(
            "inferSeparators",
            { s, key -> json.put(key, s.inferSeparators) },
            { key -> json.ifHasBoolean(key) { setPlayInferSeparators(it) } },
        )
        run(
            "keyboardCompact",
            { s, key -> json.put(key, s.keyboardCompact) },
            { key -> json.ifHasBoolean(key) { setKeyboardCompact(it) } },
        )
        run(
            "keyboardForceCaps",
            { s, key -> json.put(key, s.keyboardForceCaps) },
            { key -> json.ifHasBoolean(key) { setKeyboardForceCaps(it) } },
        )
        run(
            "keyboardHaptic",
            { s, key -> json.put(key, s.keyboardHaptic) },
            { key -> json.ifHasBoolean(key) { setKeyboardHaptic(it) } },
        )
        run(
            "keyboardHideButton",
            { s, key -> json.put(key, s.keyboardHideButton) },
            { key -> json.ifHasBoolean(key) { setKeyboardHideButton(it) } },
        )
        run(
            "keyboardLayout",
            { s, key -> json.put(key, s.keyboardLayout.toString()) },
            { key ->
               json.ifHasString(key) {
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
            { key -> json.ifHasInt(key) { setKeyboardRepeatDelay(it) } },
        )
        run(
            "keyboardRepeatInterval",
            { s, key -> json.put(key, s.keyboardRepeatInterval) },
            { key -> json.ifHasInt(key) { setKeyboardRepeatInterval(it) } },
        )
        run(
            "keyboardShowHide",
            { s, key -> json.put(key, s.keyboardShowHide.toString()) },
            { key ->
               json.ifHasString(key) {
                   try {
                       setKeyboardMode(KeyboardMode.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "keyboardSplitWords",
            { s, key -> json.put(key, s.keyboardSplitWords) },
            { key -> json.ifHasBoolean(key) { setKeyboardSplitWords(it) } },
        )
        run(
            "lastSeenVersion",
            { s, key -> json.put(key, s.lastSeenVersion) },
            { key ->
                json.ifHasString(key) { setBrowseLastSeenVersion(it) }
            },
        )
        run(
            "movementStrategy",
            { s, key -> json.put(key, s.movementStrategy.toString()) },
            { key ->
               json.ifHasString(key) {
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
               json.ifHasString(key) {
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
               json.ifHasString(key) {
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
            { key -> json.ifHasInt(key) { setPlayClueTabsPage(0, it) } },
        )
        run(
            "playActivityClueTabsPage1",
            { s, key -> json.put(key, s.playActivityClueTabsPage1) },
            { key -> json.ifHasInt(key) { setPlayClueTabsPage(1, it) } },
        )
        run(
            "playLetterUndoEnabled",
            { s, key -> json.put(key, s.playLetterUndoEnabled) },
            { key ->
                json.ifHasBoolean(key) { setPlayPlayLetterUndoEnabled(it) }
            },
        )
        run(
            "predictAnagramChars",
            { s, key -> json.put(key, s.predictAnagramChars) },
            { key ->
                json.ifHasBoolean(key) { setPlayPredictAnagramChars(it) }
            },
        )
        run(
            "preserveCorrectLettersInShowErrors",
            { s, key -> json.put(key, s.preserveCorrectLettersInShowErrors) },
            { key ->
                json.ifHasBoolean(key) {
                    setPlayPreserveCorrectLettersInShowErrors(it)
                }
            },
        )
        run(
            "randomClueOnShake",
            { s, key -> json.put(key, s.randomClueOnShake) },
            { key ->
                json.ifHasBoolean(key) { setPlayRandomClueOnShake(it) }
            },
        )
        run(
            "scale",
            { s, key -> json.put(key, s.scale) },
            { key -> json.ifHasDouble(key) { setPlayScale(it.toFloat()) } },
        )
        run(
            "scratchMode",
            { s, key -> json.put(key, s.scratchMode) },
            { key -> json.ifHasBoolean(key) { setPlayScratchMode(it) } },
        )
        run(
            "clueListNameInClueLine",
            { s, key -> json.put(key, s.clueListNameInClueLine.toString()) },
            { key ->
               json.ifHasString(key) {
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
            { key -> json.ifHasBoolean(key) { setPlayShowClueTabs(it) } },
        )
        run(
            "showCount",
            { s, key -> json.put(key, s.showCount) },
            { key -> json.ifHasBoolean(key) { setPlayShowCount(it) } },
        )
        run(
            "showErrors",
            { s, key -> json.put(key, s.showErrors) },
            { key -> json.ifHasBoolean(key) { setPlayShowErrorsGrid(it) } },
        )
        run(
            "showErrorsClue",
            { s, key -> json.put(key, s.showErrorsClue) },
            { key -> json.ifHasBoolean(key) { setPlayShowErrorsClue(it) } },
        )
        run(
            "showErrorsCursor",
            { s, key -> json.put(key, s.showErrorsCursor) },
            { key ->
                json.ifHasBoolean(key) { setPlayShowErrorsCursor(it) }
            },
        )
        run(
            "showTimer",
            { s, key -> json.put(key, s.showTimer) },
            { key -> json.ifHasBoolean(key) { setPlayShowTimer(it) } },
        )
        run(
            "showWordsInClueList",
            { s, key -> json.put(key, s.showWordsInClueList) },
            { key -> json.ifHasBoolean(key) { setClueListShowWords(it) } },
        )
        run(
            "skipFilled",
            { s, key -> json.put(key, s.skipFilled) },
            { key -> json.ifHasBoolean(key) { setPlaySkipFilled(it) } },
        )
        run(
            "snapClue",
            { s, key -> json.put(key, s.snapClue) },
            { key -> json.ifHasBoolean(key) { setClueListSnapToClue(it) } },
        )
        run(
            "sort",
            { s, key -> json.put(key, s.sort) },
            { key ->
               json.ifHasString(key) {
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
                json.ifHasBoolean(key) { setPlaySpaceChangesDirection(it) }
            },
        )
        run(
            "suppressHints",
            { s, key -> json.put(key, s.suppressHints) },
            { key ->
                json.ifHasBoolean(key) {
                    setPlaySuppressHintHighlighting(it)
                }
            },
        )
        run(
            "suppressMessages",
            { s, key -> json.put(key, s.suppressMessages) },
            { key ->
                json.ifHasBoolean(key) { setDownloadSuppressMessages(it) }
            },
        )
        run(
            "suppressSummaryMessages",
            { s, key -> json.put(key, s.suppressSummaryMessages) },
            { key ->
                json.ifHasBoolean(key) {
                    setDownloadSuppressSummaryMessages(it)
                }
            },
        )
        run(
            "swipeAction",
            { s, key -> json.put(key, s.swipeAction.toString()) },
            { key ->
               json.ifHasString(key) {
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
                json.ifHasBoolean(key) { setPlayToggleBeforeMove(it) }
            },
        )
        run(
            "uiTheme",
            { s, key -> json.put(key, s.uiTheme.toString()) },
            { key ->
               json.ifHasString(key) {
                   try {
                       setAppDayNightMode(DayNightMode.valueOf(it))
                   } catch (e : IllegalArgumentException) {
                       // ignore
                   }
               }
            },
        )
        run(
            "undecorated",
            { s, key -> json.put(key, s.undecorated) },
            { key ->
                json.ifHasBoolean(key) { setAppUndecorated(it) }
            },
        )
        run(
            "useNativeKeyboard",
            { s, key -> json.put(key, s.useNativeKeyboard) },
            { key -> json.ifHasBoolean(key) { setKeyboardUseNative(it) } },
        )
        run(
            "volumeActivatesVoice",
            { s, key -> json.put(key, s.volumeActivatesVoice) },
            { key ->
                json.ifHasBoolean(key) { setVoiceVolumeActivatesVoice(it) }
            },
        )
        run(
            "specialEntryForceCaps",
            { s, key -> json.put(key, s.specialEntryForceCaps) },
            { key ->
                json.ifHasBoolean(key) { setPlaySpecialEntryForceCaps(it) }
            },
        )

        // bit untidy for file handler settings
        if (settings == null) {
            json.ifHasString("storageLocation") { location ->
            json.ifHasString("safRootUri") { root ->
            json.ifHasString("safCrosswordsFolderUri") { crosswords ->
            json.ifHasString("safArchiveFolderUri") { archive ->
            json.ifHasString("safToImportFolderUri") { import ->
            json.ifHasString("safToImportDoneFolderUri") { importDone ->
            json.ifHasString("safToImportFailedFolderUri") { importFailed ->
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
