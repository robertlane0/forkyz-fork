
package app.crossword.yourealwaysbe.forkyz.settings

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage

import okio.FileSystem
import okio.Path

import app.crossword.yourealwaysbe.forkyz.net.DownloaderID

const val SETTINGS_FILE_NAME = "settings.pb"
private val CURRENT_SETTINGS_VERSION = 3

val DEFAULT_SETTINGS : Settings = Settings(
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
    clueTabsDouble = ClueTabsDouble.CTD_NEVER,
    collapseBrowseHeadings = false,
    crosswordSolverEnabled = true,
    customDailyTitle = "",
    customDailyUrl = "",
    cycleUnfilledMode = CycleUnfilledMode.CU_NEVER,
    deleteCrossingMode = DeleteCrossingModeSetting.DCMS_DELETE,
    deleteOnCleanup = false,
    disableRatings = false,
    disableSwipe = true,
    displayScratch = false,
    displaySeparators = DisplaySeparators.DS_ALWAYS,
    dlLast = 0,
    dlOnStartup = false,
    doubleTap = false,
    download20Minutes = true,
    downloadCustomDaily = false,
    downloadDeStandaard = true,
    downloadDeTelegraaf = true,
    downloadElPaisExperto = true,
    downloadGuardianDailyCryptic = true,
    downloadGuardianWeeklyQuiptic = true,
    downloadHamAbend = true,
    downloadIndependentDailyCryptic = true,
    downloadIrishNewsCryptic = true,
    downloadJonesin = true,
    downloadJoseph = true,
    downloadLaCroix = true,
    downloadLATimes = true,
    downloadLastSelectedDate = "",
    downloadLeParisienF1 = true,
    downloadLeParisienF2 = true,
    downloadLeParisienF3 = true,
    downloadLeParisienF4 = true,
    downloadMetroCryptic = true,
    downloadMetroQuick = true,
    downloadNewYorkTimesSyndicated = true,
    downloadNewsday = true,
    downloadNRCInHetMidden = true,
    downloadNRCScrypto = true,
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
    keyboardSplitWords = false,
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
    showErrorsMode = ShowErrorsModeSetting.SEMS_NONE,
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
    swipeAction = BrowseSwipeAction.BSA_ARCHIVE,
    toggleBeforeMove = false,
    uiTheme = DayNightMode.DNM_DAY,
    useNativeKeyboard = false,
    volumeActivatesVoice = false,
    specialEntryForceCaps = false,
    dontConfirmCleanup = false,
    dontConfirmBrowseDelete = false,
    expandableClueLine = false,
    clueListNameInClueLine = ClueListClueLine.CLCL_FULL,
    downloadTF1MotsFleches = true,
    downloadSudOuestMotsFleches = true,
    downloadSudOuestMotsCroises = true,
    downloadTF1MotsCroises = true,
    dontSnapBoardToBorders = false,
    disabledDownloaders = listOf(),
    settingsVersion = CURRENT_SETTINGS_VERSION,
    undecorated = false,
)

fun createDataStore(
    fileSystem : FileSystem,
    producePath : () -> Path,
    defaultSettings : Settings = DEFAULT_SETTINGS,
): DataStore<Settings> = DataStoreFactory.create(
    storage = OkioStorage(
        fileSystem = fileSystem,
        producePath = producePath,
        serializer = SettingsSerializer(defaultSettings),
    ),
    migrations = listOf(
        DownloaderBooleanToSetMigration(),
        AutoDownloadersToDayBlocksMigration(),
        ShowErrorsModeFromBooleansMigration(),
    ),
)


/**
 * Migration to convert downloadX and scrapeX booleans to disabledDownloaders set
 */
class DownloaderBooleanToSetMigration : DataMigration<Settings> {
    companion object {
        const val MIGRATION_VERSION = 1
    }

    override suspend fun shouldMigrate(currentData: Settings): Boolean {
        return currentData.settingsVersion < MIGRATION_VERSION
    }

    override suspend fun migrate(currentData: Settings): Settings {
        // Migrate downloadX and scrapeX booleans to disabledDownloaders set
        val disabled = mutableSetOf<String>()

        if (!currentData.download20Minutes)
            disabled.add(DownloaderID.TWENTY_MINUTES.id)
        if (!currentData.downloadCustomDaily)
          disabled.add(DownloaderID.CUSTOM.id)
        if (!currentData.downloadDeStandaard)
          disabled.add(DownloaderID.DE_STANDAARD.id)
        if (!currentData.downloadDeTelegraaf)
          disabled.add(DownloaderID.DE_TELEGRAAF.id)
        if (!currentData.downloadElPaisExperto)
          disabled.add(DownloaderID.EL_PAIS_EXPERTO.id)
        if (!currentData.downloadGuardianDailyCryptic)
          disabled.add(DownloaderID.GUARDIAN_DAILY_CRYPTIC.id)
        if (!currentData.downloadGuardianWeeklyQuiptic)
          disabled.add(DownloaderID.GUARDIAN_WEEKLY_QUIPTIC.id)
        if (!currentData.downloadHamAbend)
          disabled.add(DownloaderID.HAM_ABEND.id)
        if (!currentData.downloadIndependentDailyCryptic)
          disabled.add(DownloaderID.INDEPENDENT_DAILY_CRYPTIC.id)
        if (!currentData.downloadIrishNewsCryptic)
          disabled.add(DownloaderID.IRISH_NEWS_CRYPTIC.id)
        if (!currentData.downloadJonesin)
          disabled.add(DownloaderID.JONESIN.id)
        if (!currentData.downloadJoseph)
          disabled.add(DownloaderID.JOSEPH.id)
        if (!currentData.downloadLaCroix)
          disabled.add(DownloaderID.LA_CROIX.id)
        if (!currentData.downloadLATimes)
          disabled.add(DownloaderID.LA_TIMES.id)
        if (!currentData.downloadLeParisienF1)
          disabled.add(DownloaderID.LE_PARISIEN_F1.id)
        if (!currentData.downloadLeParisienF2)
          disabled.add(DownloaderID.LE_PARISIEN_F2.id)
        if (!currentData.downloadLeParisienF3)
          disabled.add(DownloaderID.LE_PARISIEN_F3.id)
        if (!currentData.downloadLeParisienF4)
          disabled.add(DownloaderID.LE_PARISIEN_F4.id)
        if (!currentData.downloadMetroCryptic)
          disabled.add(DownloaderID.METRO_CRYPTIC.id)
        if (!currentData.downloadMetroQuick)
          disabled.add(DownloaderID.METRO_QUICK.id)
        if (!currentData.downloadNewsday)
          disabled.add(DownloaderID.NEWSDAY.id)
        if (!currentData.downloadNewYorkTimesSyndicated)
          disabled.add(DownloaderID.NYT_SYNDICATED.id)
        if (!currentData.downloadNRCInHetMidden)
          disabled.add(DownloaderID.NRC_IN_HET_MIDDEN.id)
        if (!currentData.downloadNRCScrypto)
          disabled.add(DownloaderID.NRC_SCRYPTO.id)
        if (!currentData.downloadPremier)
          disabled.add(DownloaderID.PREMIER.id)
        if (!currentData.downloadSheffer)
          disabled.add(DownloaderID.SHEFFER.id)
        if (!currentData.downloadSudOuestMotsCroises)
          disabled.add(DownloaderID.SUD_OUEST_MOTS_CROISES.id)
        if (!currentData.downloadSudOuestMotsFleches)
          disabled.add(DownloaderID.SUD_OUEST_MOTS_FLECHES.id)
        if (!currentData.downloadTF1MotsCroises)
          disabled.add(DownloaderID.TF1_MOTS_CROISES.id)
        if (!currentData.downloadTF1MotsFleches)
          disabled.add(DownloaderID.TF1_MOTS_FLECHES.id)
        if (!currentData.downloadUniversal)
          disabled.add(DownloaderID.UNIVERSAL.id)
        if (!currentData.downloadUSAToday)
          disabled.add(DownloaderID.USA_TODAY.id)
        if (!currentData.downloadWaPoSunday)
          disabled.add(DownloaderID.WAPO_SUNDAY.id)
        if (!currentData.downloadWsj)
          disabled.add(DownloaderID.WSJ.id)

        if (!currentData.scrapeCru)
          disabled.add(DownloaderID.CRYPTIC_CRU.id)
        if (!currentData.scrapeGuardianQuick)
          disabled.add(DownloaderID.GUARDIAN_QUICK.id)
        if (!currentData.scrapeKegler)
          disabled.add(DownloaderID.KEGLAR.id)
        if (!currentData.scrapePrivateEye)
          disabled.add(DownloaderID.PRIVATE_EYE.id)
        if (!currentData.scrapePrzekroj)
          disabled.add(DownloaderID.PRZEKROJ.id)

        // Clear old boolean fields by setting them to proto3 default (false)
        return currentData.copy(
            disabledDownloaders = disabled.toList(),
            settingsVersion = MIGRATION_VERSION,
            download20Minutes = false,
            downloadCustomDaily = false,
            downloadDeStandaard = false,
            downloadDeTelegraaf = false,
            downloadElPaisExperto = false,
            downloadGuardianDailyCryptic = false,
            downloadGuardianWeeklyQuiptic = false,
            downloadHamAbend = false,
            downloadIndependentDailyCryptic = false,
            downloadIrishNewsCryptic = false,
            downloadJonesin = false,
            downloadJoseph = false,
            downloadLaCroix = false,
            downloadLATimes = false,
            downloadLeParisienF1 = false,
            downloadLeParisienF2 = false,
            downloadLeParisienF3 = false,
            downloadLeParisienF4 = false,
            downloadMetroCryptic = false,
            downloadMetroQuick = false,
            downloadNewsday = false,
            downloadNewYorkTimesSyndicated = false,
            downloadNRCInHetMidden = false,
            downloadNRCScrypto = false,
            downloadPremier = false,
            downloadSheffer = false,
            downloadSudOuestMotsCroises = false,
            downloadSudOuestMotsFleches = false,
            downloadTF1MotsCroises = false,
            downloadTF1MotsFleches = false,
            downloadUniversal = false,
            downloadUSAToday = false,
            downloadWaPoSunday = false,
            downloadWsj = false,
            scrapeCru = false,
            scrapeGuardianQuick = false,
            scrapeKegler = false,
            scrapePrivateEye = false,
            scrapePrzekroj = false,
        )
    }

    override suspend fun cleanUp() {
        // Nothing to do
    }
}

/**
 * Migration to convert autoDownloaders to autoDownloadersBlock<Day>
 */
class AutoDownloadersToDayBlocksMigration : DataMigration<Settings> {
    companion object {
        const val MIGRATION_VERSION = 2
    }

    override suspend fun shouldMigrate(currentData: Settings): Boolean {
        return currentData.settingsVersion < MIGRATION_VERSION
    }

    override suspend fun migrate(currentData: Settings): Settings {
        val block = mutableSetOf<String>()

        val isAutoDownloadEnabled = (
            currentData.backgroundDownloadHourly
            || currentData.dlOnStartup
            || currentData.backgroundDownloadDays.size > 0
        )

        if (isAutoDownloadEnabled) {
            for (downloaderID in DownloaderID.values()) {
                val id = downloaderID.id
                if (
                    id !in currentData.disabledDownloaders
                    && id !in currentData.autoDownloaders
                ) {
                    block.add(id)
                }
            }
        }

        // Clear autoDownloaders, setup blocks
        return currentData.copy(
            autoDownloaders = listOf(),
            autoDownloadersBlockMon = block.toList(),
            autoDownloadersBlockTue = block.toList(),
            autoDownloadersBlockWed = block.toList(),
            autoDownloadersBlockThu = block.toList(),
            autoDownloadersBlockFri = block.toList(),
            autoDownloadersBlockSat = block.toList(),
            autoDownloadersBlockSun = block.toList(),
            settingsVersion = MIGRATION_VERSION,
        )
    }

    override suspend fun cleanUp() {
        // Nothing to do
    }
}

/**
 * Migration to consolidate four showErrors booleans into a single enum
 */
class ShowErrorsModeFromBooleansMigration : DataMigration<Settings> {
    companion object {
        const val MIGRATION_VERSION = 3
    }

    override suspend fun shouldMigrate(currentData : Settings) : Boolean {
        return currentData.settingsVersion < MIGRATION_VERSION
    }

    override suspend fun migrate(currentData : Settings) : Settings {
        val newMode = when {
            currentData.showErrorsWholeWord -> ShowErrorsModeSetting.SEMS_WHOLE_WORD
            currentData.showErrors -> ShowErrorsModeSetting.SEMS_GRID
            currentData.showErrorsCursor -> ShowErrorsModeSetting.SEMS_CURSOR
            currentData.showErrorsClue -> ShowErrorsModeSetting.SEMS_CLUE
            else -> ShowErrorsModeSetting.SEMS_NONE
        }
        return currentData.copy(
            showErrorsMode = newMode,
            showErrors = false,
            showErrorsClue = false,
            showErrorsCursor = false,
            showErrorsWholeWord = false,
            settingsVersion = MIGRATION_VERSION,
        )
    }

    override suspend fun cleanUp() {
        // Nothing to do
    }
}

