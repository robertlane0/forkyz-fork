
package app.crossword.yourealwaysbe.forkyz.settings

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

import android.content.SharedPreferences
import android.os.SystemClock
import androidx.datastore.core.DataStore

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.anyFloat
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyLong
import org.mockito.Mockito.anyString
import org.mockito.Mockito.eq
import org.mockito.Mockito.mock

import app.crossword.yourealwaysbe.forkyz.net.DownloaderID

class TestDataStore(initialDataStoreSettings : Settings) : DataStore<Settings> {
    private var currentDataStoreSettings = initialDataStoreSettings

    override val data = flow {
        while (true)
            emit(currentDataStoreSettings)
    }

    override suspend fun updateData(
        transform : suspend (Settings) -> Settings,
    ) : Settings {
        currentDataStoreSettings = runBlocking {
            transform(currentDataStoreSettings)
        }
        return currentDataStoreSettings
    }
}

class ForkyzSettingsTest {
    val settingsValue1 : Settings = Settings(
        alwaysAnnounceBox = false,
        alwaysAnnounceClue = false,
        applicationTheme = Theme.T_STANDARD,
        archiveCleanupAge = -1,
        autoDownloadersBlockMon = listOf(),
        autoDownloadersBlockTue = listOf(),
        autoDownloadersBlockWed = listOf(),
        autoDownloadersBlockThu = listOf(),
        autoDownloadersBlockFri = listOf(),
        autoDownloadersBlockSat = listOf(),
        autoDownloadersBlockSun = listOf(),
        backgroundDownloadAllowRoaming = false,
        backgroundDownloadDays = listOf(),
        backgroundDownloadDaysTime = 8,
        backgroundDownloadHourly = false,
        backgroundDownloadRequireCharging = false,
        backgroundDownloadRequireUnmetered = true,
        browseAlwaysShowRating = false,
        browseIndicateIfSolution = false,
        browseNewPuzzle = false,
        browseShowPercentageCorrect = false,
        buttonActivatesVoice = false,
        buttonAnnounceClue = false,
        llmAgent = LLMAgent.LA_NONE,
        llmAgentAPIKey = "",
        llmVoiceCommands = false,
        cleanupAge = -1,
        clueBelowGrid = false,
        clueHighlight = ClueHighlight.CH_RADIO_BUTTON,
        clueListNameInClueLine = ClueListClueLine.CLCL_FULL,
        clueTabsDouble = ClueTabsDouble.CTD_NEVER,
        crosswordSolverEnabled = true,
        customDailyTitle = "",
        customDailyUrl = "",
        cycleUnfilledMode = CycleUnfilledMode.CU_NEVER,
        deleteCrossingMode = DeleteCrossingModeSetting.DCMS_DELETE,
        deleteOnCleanup = false,
        disableRatings = false,
        disableSwipe = false,
        displayScratch = false,
        displaySeparators = DisplaySeparators.DS_ALWAYS,
        dlLast = 0,
        dlOnStartup = false,
        dontConfirmCleanup = true,
        dontConfirmBrowseDelete = false,
        dontSnapBoardToBorders = false,
        doubleTap = false,
        download20Minutes = true,
        downloadCustomDaily = false,
        downloadDeStandaard = true,
        downloadDeTelegraaf = true,
        downloadGuardianDailyCryptic = true,
        downloadGuardianWeeklyQuiptic = true,
        downloadHamAbend = true,
        downloadIndependentDailyCryptic = true,
        downloadIrishNewsCryptic = true,
        downloadJonesin = true,
        downloadJoseph = true,
        downloadLaCroix = true,
        downloadLastSelectedDate = "",
        downloadLeParisienF1 = true,
        downloadLeParisienF2 = true,
        downloadLeParisienF3 = true,
        downloadLeParisienF4 = true,
        downloadMetroCryptic = true,
        downloadMetroQuick = true,
        downloadNewYorkTimesSyndicated = true,
        downloadNewsday = true,
        downloadPremier = true,
        downloadRememberDate = false,
        downloadSheffer = true,
        downloadTimeout = 30000,
        downloadUSAToday = true,
        downloadUniversal = true,
        downloadWaPoSunday = true,
        downloadWsj = true,
        duckDuckGoEnabled = true,
        ensureVisible = true,
        enterChangesDirection = true,
        equalsAnnounceClue = true,
        expandableClueLine = false,
        externalDictionary = ExternalDictionarySetting.EDS_FREE,
        fifteenSquaredEnabled = true,
        fitToScreenMode = FitToScreenMode.FTSM_NEVER,
        fullScreen = false,
        gridRatio = GridRatio.GR_PUZZLE_SHAPE,
        gridRatioLand = GridRatio.GR_PUZZLE_SHAPE,
        highlightCrossingClue = false,
        indicateShowErrors = true,
        inferSeparators = false,
        keyboardCompact = false,
        keyboardForceCaps = true,
        keyboardHaptic = true,
        keyboardHideButton = false,
        keyboardLayout = KeyboardLayout.KL_QWERTY,
        keyboardRepeatDelay = 300,
        keyboardRepeatInterval = 75,
        keyboardShowHide = KeyboardMode.KM_HIDE_MANUAL,
        lastSeenVersion = "",
        mcpRunning = false,
        mcpHostPort = "",
        movementStrategy = MovementStrategySetting.MSS_MOVE_NEXT_ON_AXIS,
        movementStrategyClueList = MovementStrategySetting.MSS_MOVE_NEXT_CLUE,
        orientationLock = Orientation.O_UNLOCKED,
        playActivityClueTabsPage = 0,
        playActivityClueTabsPage1 = 1,
        playLetterUndoEnabled = false,
        predictAnagramChars = true,
        preserveCorrectLettersInShowErrors = false,
        randomClueOnShake = false,
        safArchiveFolderUri = "",
        safCrosswordsFolderUri = "",
        safRootUri = "",
        safToImportDoneFolderUri = "",
        safToImportFailedFolderUri = "",
        safToImportFolderUri = "",
        scale = 1.0F,
        scrapeCru = true,
        scrapeGuardianQuick = true,
        scrapeKegler = true,
        scrapePrivateEye = true,
        scrapePrzekroj = true,
        scratchMode = false,
        showCluesOnPlayScreen = true,
        showCount = false,
        showErrors = false,
        showErrorsClue = false,
        showErrorsCursor = false,
        showTimer = false,
        showWordsInClueList = false,
        skipFilled = false,
        snapClueMode = SnapClueMode.SCM_OFF,
        sort = AccessorSetting.AS_DATE_DESC,
        spaceChangesDirection = true,
        storageLocation = StorageLocation.SL_INTERNAL,
        suppressHints = false,
        suppressMessages = false,
        suppressSummaryMessages = false,
        swipeAction = BrowseSwipeAction.BSA_DELETE,
        toggleBeforeMove = false,
        uiTheme = DayNightMode.DNM_DAY,
        undecorated = false,
        useNativeKeyboard = false,
        volumeActivatesVoice = false,
    )

    val settingsValue2 : Settings = Settings(
        alwaysAnnounceBox = true,
        alwaysAnnounceClue = true,
        applicationTheme = Theme.T_DYNAMIC,
        archiveCleanupAge = 10,
        autoDownloadersBlockMon = listOf("mon"),
        autoDownloadersBlockTue = listOf("tue"),
        autoDownloadersBlockWed = listOf("wed"),
        autoDownloadersBlockThu = listOf("thu"),
        autoDownloadersBlockFri = listOf("fri"),
        autoDownloadersBlockSat = listOf("sat"),
        autoDownloadersBlockSun = listOf("sun"),
        backgroundDownloadAllowRoaming = true,
        backgroundDownloadDays = listOf("1"),
        backgroundDownloadDaysTime = 9,
        backgroundDownloadHourly = true,
        backgroundDownloadRequireCharging = true,
        backgroundDownloadRequireUnmetered = false,
        browseAlwaysShowRating = true,
        browseIndicateIfSolution = true,
        browseNewPuzzle = true,
        browseShowPercentageCorrect = true,
        buttonActivatesVoice = true,
        buttonAnnounceClue = true,
        llmAgent = LLMAgent.LA_CHATGPT,
        llmAgentAPIKey = "xx",
        llmVoiceCommands = true,
        cleanupAge = 10,
        clueBelowGrid = true,
        clueHighlight = ClueHighlight.CH_BOTH,
        clueListNameInClueLine = ClueListClueLine.CLCL_NONE,
        clueTabsDouble = ClueTabsDouble.CTD_WIDE,
        crosswordSolverEnabled = false,
        customDailyTitle = "xx",
        customDailyUrl = "xx",
        cycleUnfilledMode = CycleUnfilledMode.CU_ALWAYS,
        deleteCrossingMode
            = DeleteCrossingModeSetting.DCMS_PRESERVE_FILLED_CELLS,
        deleteOnCleanup = true,
        disableRatings = true,
        disableSwipe = true,
        displayScratch = true,
        displaySeparators = DisplaySeparators.DS_NEVER,
        dlLast = 0,
        dlOnStartup = true,
        dontConfirmCleanup = false,
        dontConfirmBrowseDelete = true,
        dontSnapBoardToBorders = true,
        doubleTap = true,
        download20Minutes = true,
        downloadCustomDaily = false,
        downloadDeStandaard = true,
        downloadDeTelegraaf = true,
        downloadGuardianDailyCryptic = true,
        downloadGuardianWeeklyQuiptic = true,
        downloadHamAbend = true,
        downloadIndependentDailyCryptic = true,
        downloadIrishNewsCryptic = true,
        downloadJonesin = true,
        downloadJoseph = true,
        downloadLaCroix = true,
        downloadLastSelectedDate = "2024-01-15",
        downloadLeParisienF1 = true,
        downloadLeParisienF2 = true,
        downloadLeParisienF3 = true,
        downloadLeParisienF4 = true,
        downloadMetroCryptic = true,
        downloadMetroQuick = true,
        downloadNewYorkTimesSyndicated = true,
        downloadNewsday = true,
        downloadPremier = true,
        downloadRememberDate = true,
        downloadSheffer = true,
        downloadTimeout = 40000,
        downloadUSAToday = true,
        downloadUniversal = true,
        downloadWaPoSunday = true,
        downloadWsj = true,
        duckDuckGoEnabled = false,
        ensureVisible = false,
        enterChangesDirection = false,
        equalsAnnounceClue = false,
        expandableClueLine = true,
        externalDictionary = ExternalDictionarySetting.EDS_NONE,
        fifteenSquaredEnabled = false,
        fitToScreenMode = FitToScreenMode.FTSM_START,
        fullScreen = true,
        gridRatio = GridRatio.GR_ONE_TO_ONE,
        gridRatioLand = GridRatio.GR_FIFTY_PCNT,
        highlightCrossingClue = true,
        indicateShowErrors = false,
        inferSeparators = true,
        keyboardCompact = true,
        keyboardForceCaps = false,
        keyboardHaptic = false,
        keyboardHideButton = true,
        keyboardLayout = KeyboardLayout.KL_DVORAK,
        keyboardRepeatDelay = 301,
        keyboardRepeatInterval = 85,
        keyboardShowHide = KeyboardMode.KM_NEVER_SHOW,
        lastSeenVersion = "xx",
        mcpRunning = true,
        mcpHostPort = "12:34",
        movementStrategy = MovementStrategySetting.MSS_STOP_ON_END,
        movementStrategyClueList = MovementStrategySetting.MSS_STOP_ON_END,
        orientationLock = Orientation.O_PORTRAIT,
        playActivityClueTabsPage = 1,
        playActivityClueTabsPage1 = 0,
        playLetterUndoEnabled = true,
        predictAnagramChars = false,
        preserveCorrectLettersInShowErrors = true,
        randomClueOnShake = true,
        safArchiveFolderUri = "xx",
        safCrosswordsFolderUri = "xx",
        safRootUri = "xx",
        safToImportDoneFolderUri = "xx",
        safToImportFailedFolderUri = "xx",
        safToImportFolderUri = "xx",
        scale = 1.2F,
        scrapeCru = true,
        scrapeGuardianQuick = true,
        scrapeKegler = true,
        scrapePrivateEye = true,
        scrapePrzekroj = true,
        scratchMode = true,
        showCluesOnPlayScreen = false,
        showCount = true,
        showErrors = true,
        showErrorsClue = true,
        showErrorsCursor = true,
        showTimer = true,
        showWordsInClueList = true,
        skipFilled = true,
        snapClueMode = SnapClueMode.SCM_CROSSING,
        sort = AccessorSetting.AS_DATE_ASC,
        spaceChangesDirection = false,
        storageLocation = StorageLocation.SL_EXTERNAL_SAF,
        suppressHints = true,
        suppressMessages = true,
        suppressSummaryMessages = true,
        swipeAction = BrowseSwipeAction.BSA_ARCHIVE,
        toggleBeforeMove = true,
        uiTheme = DayNightMode.DNM_NIGHT,
        undecorated = true,
        useNativeKeyboard = true,
        volumeActivatesVoice = true,
    )

    val settingsValue3 = settingsValue2.copy(
        clueListNameInClueLine = ClueListClueLine.CLCL_FULL,
        dontConfirmCleanup = true,
        dontConfirmBrowseDelete = false,
        dontSnapBoardToBorders = false,
        downloadLaCroix = true,
        expandableClueLine = false,
        llmVoiceCommands = false,
        movementStrategyClueList = MovementStrategySetting.MSS_MOVE_NEXT_CLUE,
        autoDownloadersBlockMon = listOf(),
        autoDownloadersBlockTue = listOf(),
        autoDownloadersBlockWed = listOf(),
        autoDownloadersBlockThu = listOf(),
        autoDownloadersBlockFri = listOf(),
        autoDownloadersBlockSat = listOf(),
        autoDownloadersBlockSun = listOf(),
        // these were available when migration from shared prefs
        // happened
        disabledDownloaders = listOf(
            DownloaderID.TWENTY_MINUTES.id,
            DownloaderID.JOSEPH.id,
            DownloaderID.PREMIER.id,
            DownloaderID.SHEFFER.id,
            DownloaderID.CRYPTIC_CRU.id,
            DownloaderID.DE_STANDAARD.id,
            DownloaderID.DE_TELEGRAAF.id,
            DownloaderID.GUARDIAN_DAILY_CRYPTIC.id,
            DownloaderID.GUARDIAN_QUICK.id,
            DownloaderID.GUARDIAN_WEEKLY_QUIPTIC.id,
            DownloaderID.HAM_ABEND.id,
            DownloaderID.INDEPENDENT_DAILY_CRYPTIC.id,
            DownloaderID.IRISH_NEWS_CRYPTIC.id,
            DownloaderID.JONESIN.id,
            DownloaderID.KEGLAR.id,
            DownloaderID.LE_PARISIEN_F1.id,
            DownloaderID.LE_PARISIEN_F2.id,
            DownloaderID.LE_PARISIEN_F3.id,
            DownloaderID.LE_PARISIEN_F4.id,
            DownloaderID.METRO_CRYPTIC.id,
            DownloaderID.METRO_QUICK.id,
            DownloaderID.NEWSDAY.id,
            DownloaderID.NYT_SYNDICATED.id,
            DownloaderID.PRIVATE_EYE.id,
            DownloaderID.PRZEKROJ.id,
            DownloaderID.UNIVERSAL.id,
            DownloaderID.USA_TODAY.id,
            DownloaderID.WAPO_SUNDAY.id,
            DownloaderID.WSJ.id,
        ),
        downloadLastSelectedDate = "",
        downloadRememberDate = false,
        mcpRunning = false,
        mcpHostPort = "",
        undecorated = false,
        snapClueMode = SnapClueMode.SCM_CLUE,
        highlightCrossingClue = false,
    )

    @ExperimentalCoroutinesApi
    private val testDispatcher = UnconfinedTestDispatcher()

    @ExperimentalCoroutinesApi
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Mock
    val prefs = mock(SharedPreferences::class.java)
    val prefsEdit = mock(SharedPreferences.Editor::class.java)

    @Before
    fun setupSharedPrefs() {
        `when`(prefs.contains(anyString())).thenReturn(true)
        `when`(prefs.getBoolean(eq("alwaysAnnounceBox"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("alwaysAnnounceClue"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getString(eq("applicationTheme"), eq(null)))
            .thenReturn("D")
        `when`(prefs.getInt(eq("archiveCleanupAge"), anyInt()))
            .thenReturn(10)
        `when`(prefs.getBoolean(eq("backgroundDownloadAllowRoaming"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getStringSet(eq("backgroundDownloadDays"), eq(null)))
            .thenReturn(setOf("1"))
        `when`(prefs.getString(eq("backgroundDownloadDaysTime"), eq(null)))
            .thenReturn("9")
        `when`(prefs.getBoolean(eq("backgroundDownloadHourly"), anyBoolean()))
            .thenReturn(true)
        `when`(
            prefs.getBoolean(eq("backgroundDownloadRequireCharging"), anyBoolean())
        ).thenReturn(true)
        `when`(
            prefs.getBoolean(eq("backgroundDownloadRequireUnmetered"), anyBoolean())
        ).thenReturn(false)
        `when`(prefs.getBoolean(eq("browseAlwaysShowRating"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("browseIndicateIfSolution"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("browseNewPuzzle"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("browseShowPercentageCorrect"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("buttonActivatesVoice"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("buttonAnnounceClue"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getString(eq("chatGPTAPIKey"), eq(null)))
            .thenReturn("xx")
        `when`(prefs.getInt(eq("cleanupAge"), anyInt()))
            .thenReturn(10)
        `when`(prefs.getBoolean(eq("clueBelowGrid"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getString(eq("clueHighlight"), eq(null)))
            .thenReturn("BOTH")
        `when`(prefs.getString(eq("clueTabsDouble"), eq(null)))
            .thenReturn("WIDE")
        `when`(prefs.getBoolean(eq("crosswordSolverEnabled"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getString(eq("customDailyTitle"), eq(null)))
            .thenReturn("xx")
        `when`(prefs.getString(eq("customDailyUrl"), eq(null)))
            .thenReturn("xx")
        `when`(
            prefs.getString(eq("cycleUnfilledMode"), eq(null))
        ).thenReturn("ALWAYS")
        `when`(
            prefs.getString(eq("deleteCrossingMode"), eq(null))
        ).thenReturn("PRESERVE_FILLED_CELLS")
        `when`(prefs.getBoolean(eq("deleteOnCleanup"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("disableRatings"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("disableSwipe"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("displayScratch"), anyBoolean()))
            .thenReturn(true)
        `when`(
            prefs.getString(eq("displaySeparators"), eq(null))
        ).thenReturn("NEVER")
        `when`(prefs.getLong(eq("dlLast"), anyLong()))
            .thenReturn(0)
        `when`(prefs.getBoolean(eq("dlOnStartup"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("doubleTap"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("download20Minutes"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadCustomDaily"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("downloadDeStandaard"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadDeTelegraaf"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadGuardianDailyCryptic"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadGuardianWeeklyQuiptic"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadHamAbend"), anyBoolean()))
            .thenReturn(false)
        `when`(
            prefs.getBoolean(eq("downloadIndependentDailyCryptic"), anyBoolean())
        ).thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadIrishNewsCryptic"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadJonesin"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadJoseph"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadLeParisienF1"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadLeParisienF2"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadLeParisienF3"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadLeParisienF4"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadMetroCryptic"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadMetroQuick"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadNewYorkTimesSyndicated"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadNewsday"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadPremier"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadSheffer"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getString(eq("downloadTimeout"), eq(null)))
            .thenReturn("40000")
        `when`(prefs.getBoolean(eq("downloadUSAToday"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadUniversal"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadWaPoSunday"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("downloadWsj"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("duckDuckGoEnabled"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("ensureVisible"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("enterChangesDirection"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("equalsAnnounceClue"), anyBoolean()))
            .thenReturn(false)
        `when`(
            prefs.getString(eq("externalDictionary"), eq(null))
        ).thenReturn("NONE")
        `when`(prefs.getBoolean(eq("fifteenSquaredEnabled"), anyBoolean()))
            .thenReturn(false)
        `when`(
            prefs.getString(eq("fitToScreenMode"), eq(null))
        ).thenReturn("START")
        `when`(prefs.getBoolean(eq("fullScreen"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getString(eq("gridRatio"), eq(null)))
            .thenReturn("0")
        `when`(prefs.getString(eq("gridRatioLand"), eq(null)))
            .thenReturn("3")
        `when`(prefs.getBoolean(eq("indicateShowErrors"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("inferSeparators"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("keyboardCompact"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("keyboardForceCaps"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("keyboardHaptic"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("keyboardHideButton"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getString(eq("keyboardLayout"), eq(null)))
            .thenReturn("2")
        `when`(prefs.getString(eq("keyboardRepeatDelay"), eq(null)))
            .thenReturn("301")
        `when`(prefs.getString(eq("keyboardRepeatInterval"), eq(null)))
            .thenReturn("85")
        `when`(prefs.getString(eq("keyboardShowHide"), eq(null)))
            .thenReturn("Never show")
        `when`(prefs.getString(eq("lastSeenVersion"), eq(null)))
            .thenReturn("xx")
        `when`(
            prefs.getString(eq("movementStrategy"), eq(null))
        ).thenReturn("STOP_ON_END")
        `when`(
            prefs.getString(eq("movementStrategyClueList"), eq(null))
        ).thenReturn("STOP_ON_END")
        `when`(prefs.getString(eq("orientationLock"), eq(null)))
            .thenReturn("PORT")
        `when`(prefs.getInt(eq("setPlayActivityClueTabsPage"), anyInt()))
            .thenReturn(1)
        `when`(prefs.getInt(eq("setPlayActivityClueTabsPage1"), anyInt()))
            .thenReturn(0)
        `when`(prefs.getBoolean(eq("playLetterUndoEnabled"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("predictAnagramChars"), anyBoolean()))
            .thenReturn(false)
        `when`(
            prefs.getBoolean(eq("preserveCorrectLettersInShowErrors"), anyBoolean())
        ).thenReturn(true)
        `when`(prefs.getBoolean(eq("randomClueOnShake"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getString(eq("safArchiveFolderUri"), eq(null)))
            .thenReturn("xx")
        `when`(prefs.getString(eq("safCrosswordsFolderUri"), eq(null)))
            .thenReturn("xx")
        `when`(prefs.getString(eq("safRootUri"), eq(null)))
            .thenReturn("xx")
        `when`(prefs.getString(eq("safToImportDoneFolderUri"), eq(null)))
            .thenReturn("xx")
        `when`(prefs.getString(eq("safToImportFailedFolderUri"), eq(null)))
            .thenReturn("xx")
        `when`(prefs.getString(eq("safToImportFolderUri"), eq(null)))
            .thenReturn("xx")
        `when`(prefs.getFloat(eq("scale"), anyFloat()))
            .thenReturn(1.2F)
        `when`(prefs.getBoolean(eq("scrapeCru"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("scrapeGuardianQuick"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("scrapeKegler"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("scrapePrivateEye"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("scrapePrzekroj"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("scratchMode"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("showCluesOnPlayScreen"), anyBoolean()))
            .thenReturn(false)
        `when`(prefs.getBoolean(eq("showCount"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("showErrors"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("showErrorsClue"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("showErrorsCursor"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("showTimer"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("showWordsInClueList"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("skipFilled"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("snapClue"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getInt(eq("sort"), anyInt())).thenReturn(1)
        `when`(prefs.getBoolean(eq("spaceChangesDirection"), anyBoolean()))
            .thenReturn(false)
        `when`(
            prefs.getString(eq("storageLocation"), eq(null))
        ).thenReturn("External directory")
        `when`(prefs.getBoolean(eq("supressHints"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("supressMessages"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("supressSummaryMessages"), anyBoolean()))
            .thenReturn(true)
        `when`(
            prefs.getString(eq("swipeAction"), eq(null))
        ).thenReturn("ARCHIVE")
        `when`(prefs.getBoolean(eq("toggleBeforeMove"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getString(eq("uiTheme"), eq(null)))
            .thenReturn("NIGHT")
        `when`(prefs.getBoolean(eq("useDynamicColors"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("useNativeKeyboard"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.getBoolean(eq("volumeActivatesVoice"), anyBoolean()))
            .thenReturn(true)
        `when`(prefs.edit()).thenReturn(prefsEdit)
    }

    @Test
    fun testExportImport() {
        // export settingsValue2 to a buffer
        // import it to a second data store
        // check end up with same values except those not exported

        val datastore1 = TestDataStore(settingsValue1)
        val settings1 = ForkyzSettings(datastore1)
        val settings2 = ForkyzSettings(TestDataStore(settingsValue2))

        val baos = ByteArrayOutputStream()
        settings2.exportSettings(baos)
        baos.close();

        val bais = ByteArrayInputStream(baos.toByteArray())
        settings1.importSettings(bais)
        bais.close()

        val importedSettings = runBlocking { datastore1.data.first() }

        val expected = settingsValue2.copy(
            downloadLastSelectedDate = "",
        )
        assertEquals(importedSettings, expected)
    }

    @Test
    fun testMigrateSharedPreferences() {
        val datastore = TestDataStore(settingsValue1)
        val settings = ForkyzSettings(datastore)

        migratePreferences(prefs, settings)

        val migratedSettingsUnsorted = runBlocking { datastore.data.first() }
        val migratedSettings = migratedSettingsUnsorted.copy(
            disabledDownloaders
                = migratedSettingsUnsorted.disabledDownloaders.sorted()
        )

        assertEquals(migratedSettings, settingsValue3)
    }

    // test import
    // careful: background days, volume voice, play activity clue tabs page
}

