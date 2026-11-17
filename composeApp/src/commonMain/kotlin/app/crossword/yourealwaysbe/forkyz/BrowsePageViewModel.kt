
package app.crossword.yourealwaysbe.forkyz

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO // needed on IOS, not JVM or Android
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.io.IOException

import androidx.annotation.MainThread
import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.BuildConfig
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.blank_copied_puzzles_to_crosswords
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.browse_date_format_header
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.import_failure
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.import_success
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzle_caption_with_author
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.unable_to_read_file
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.unknown_source
import app.crossword.yourealwaysbe.forkyz.net.Downloader
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.AccessorSetting
import app.crossword.yourealwaysbe.forkyz.settings.BackgroundDownloadSettings
import app.crossword.yourealwaysbe.forkyz.settings.BrowseSwipeAction
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.RatingsSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Parcelable
import app.crossword.yourealwaysbe.forkyz.util.Parcelize
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.DirHandle
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile
import app.crossword.yourealwaysbe.forkyz.util.importPuzzleURI
import app.crossword.yourealwaysbe.forkyz.util.processPuzzleToImportDirectory
import app.crossword.yourealwaysbe.forkyz.view.DownloadDialogViewModel

private val TAG = "ForkyzBrowsePage"
private val DEFAULT_DAY_NIGHT_MODE = DayNightMode.fromValue(0)
    ?: DayNightMode.DNM_DAY

/**
 * Dialogs to be shown
 *
 * Confirm Cleanup -- OK/Cancel when cleanup about to happen
 * New Version -- new version message
 * No Cleanup Action -- shown on archive screen when all cleanup options
 * set to never so nothing will happen
 * Notifications Permission -- dialog to show after notification permission
 * denied
 * Confirm Delete Selected -- confirm whether to delete the selected files
 */
enum class BrowseDialog {
    NONE,
    CONFIRM_CLEANUP,
    NEW_VERSION,
    NO_CLEANUP_ACTION,
    NOTIFICATIONS_PERMISSION,
    CONFIRM_DELETE_SELECTED,
}

/**
 * UI state of Browse Activity
 *
 * @param isViewArchive true if archive is showing not main list
 * @param puzzleFilter non-null if list limited to puzzles containing
 * string
 * @param isBusy true if busy with something that isn't downloading
 * @param sortOrder the order the puzzles are sorted
 * @param puzzleList the list of files
 * @param disableSwipe disable swipe actions
 * @param swipeAction archive or delete when swiped
 * @param showPercentageCorrect show percentage correct cells rather
 * than filled
 * @param showRatings if to display ratings
 * @param indicateIfSolution indicate in list if a puzzle has solutions
 * @param dayNightMode the current app day night mode (for the day night
 * mode button)
 * @param displayInternalStorageMessage whether to display message
 * about internal storage
 * @param isNotificationPermissionNeeded true if downloads will require
 * notifications
 * @param isBackgroundDownloadEnabled true if background downloads are
 * enabled (if so, may require notifications)
 * @param isDownloadOnStartUp true if should download on start of
 * activity
 * @param currentVersion the current app version
 * @param selectedPuzzleIDs set of selected puzzle ids
 * @param isSwipeRefreshing true if a swipe refresh is in progress
 * @param showDialog which dialog to show (or NONE)
 * @param isCollapseHeadings if only puzzle headings should be shown
 * except those with id in expandedHeadingIDs
 * @param expandedHeadingIDs the ids of the PuzGroups that are currently
 * expanded if isCollapseHeadings is true
 */
data class UIState(
    val isViewArchive : Boolean = false,
    val puzzleFilter : String? = null,
    val isBusy : Boolean = false,
    val sortOrder : AccessorSetting = AccessorSetting.AS_DATE_DESC,
    val sortByPublicationDate : Boolean = false,
    val puzzleList : List<PuzGroup> = listOf(),
    val disableSwipe : Boolean = true,
    val swipeAction : BrowseSwipeAction = BrowseSwipeAction.BSA_ARCHIVE,
    val showPercentageCorrect : Boolean = false,
    val showRatings : Boolean = false,
    val ratingsDisabled : Boolean = false,
    val indicateIfSolution : Boolean = false,
    val dayNightMode : DayNightMode = DEFAULT_DAY_NIGHT_MODE,
    val displayInternalStorageMessage : Boolean = false,
    val isNotificationPermissionNeeded : Boolean = false,
    val isBackgroundDownloadEnabled : Boolean = false,
    val isDownloadOnStartUp : Boolean = false,
    val currentVersion : String = "",
    val selectedPuzzleIDs : Set<String> = setOf(),
    val isSwipeRefreshing : Boolean = false,
    val showDialog : BrowseDialog = BrowseDialog.NONE,
    val isCollapseHeadings : Boolean = false,
    val expandedHeadingIDs : Set<String> = setOf(),
    val suppressHintHighlighting : Boolean = false,
) {
    val displayNoPuzzlesMessage : Boolean = puzzleList.isEmpty()
    val isSelecting : Boolean = !selectedPuzzleIDs.isEmpty()
}

/**
 * Entry for puzzle in list
 *
 * Parcelable so it can be used as a key in BrowsePage
 */
@Parcelize
data class PuzEntry(
    val id : String,
    val caption : String,
    val date : LocalDate,
    val publicationDate : LocalDate?,
    val percentComplete : Int,
    val percentFilled : Int,
    val percentHinted : Int,
    val rating : Char,
    val hasSolution : Boolean,
    val source : String,
    val title : String,
    val author : String,
) : Parcelable

/**
 * Group of puzzles with shared header
 */
data class PuzGroup (
    val header : String,
    val puzzles : List<PuzEntry>,
) {
    val id : String = header
}

/**
 * Import and post result data
 */
data class ImportFinishResult(
    val someFailed : Boolean,
    val someSucceeded : Boolean,
)

enum class BrowseSubMenu {
    NONE,
    MAIN,
    SORT_ORDER
}

data class BrowseMenuState(
    val expanded : BrowseSubMenu = BrowseSubMenu.NONE,
)

/**
 * All info needed for internal puzzle map
 */
private data class PuzData(
    val puzEntry : PuzEntry,
    val puzHandle : PuzHandle,
)

private data class BrowseActionSettings(
    val disableSwipe : Boolean = false,
    val swipeAction : BrowseSwipeAction = BrowseSwipeAction.BSA_ARCHIVE,
)

private data class BrowseDisplaySettings(
    val sortOrder : AccessorSetting,
    val sortByPublicationDate : Boolean,
    val appDayNightMode : DayNightMode,
)

private data class BrowsePuzzleDisplaySettings(
    val showPercentageCorrect : Boolean,
    val ratingsSettings : RatingsSettings,
    val indicateIfSolution : Boolean,
    val suppressHintHighlighting : Boolean,
)

private data class BrowseExternalSettings(
    val fileHandlerStorageLocation : StorageLocation,
    val lastSeenVersion : String,
    val newPuzzle : Boolean,
)

private data class BrowseDownloaderSettings(
    val downloadersSettings : DownloadersSettings,
    val backgroundDownloadSettings : BackgroundDownloadSettings,
    val downloadOnStartUp : Boolean,
)

private data class BrowseSettings(
    val actionSettings : BrowseActionSettings,
    val displaySettings : BrowseDisplaySettings,
    val puzzleDisplaySettings : BrowsePuzzleDisplaySettings,
    val externalSettings : BrowseExternalSettings,
    val downloaderSettings : BrowseDownloaderSettings,
)

class BrowsePageViewModel(
    settings : ForkyzSettings,
    val utils : NativeBackendUtils,
    val fileHandlerProvider : FileHandlerProvider,
    val currentPuzzleHolder : CurrentPuzzleHolder,
    val downloadersProvider : DownloadersProvider,
    val backgroundDownloadManager : BackgroundDownloadManager,
) : ForkyzPageViewModel(settings) {

    // important that it is single thread to avoid multiple
    // simultaneous operations
    private val uiLockDispatcher
        = Dispatchers.IO.limitedParallelism(1, "UI Lock")
    private val autoDownloadDispatcher
        = Dispatchers.IO.limitedParallelism(1, "Auto Downloads")

    @OptIn(ExperimentalAtomicApi::class)
    private val uiBusy = AtomicBoolean(false)

    private val mediatedUIState = MediatedStateWithFlow<UIState, BrowseSettings>(
        viewModelScope,
        UIState(),
        { state, settings ->
            // currently relying on this being called as soon as uiState
            // has observers
            checkNeedPuzzleLoad(settings)
            getUpdatedState(state, settings)
        },
        combine(
            combine(
                settings.liveBrowseDisableSwipe,
                settings.liveBrowseSwipeAction,
                ::BrowseActionSettings,
            ),
            combine(
                settings.liveBrowseSort,
                settings.liveBrowseSortByPublicationDate,
                settings.liveAppDayNightMode,
                ::BrowseDisplaySettings,
            ),
            combine(
                settings.liveBrowseShowPercentageCorrect,
                settings.liveRatingsSettings,
                settings.liveBrowseIndicateIfSolution,
                settings.livePlaySuppressHintHighlighting,
                ::BrowsePuzzleDisplaySettings,
            ),
            combine(
                settings.liveFileHandlerStorageLocation,
                settings.liveBrowseLastSeenVersion,
                settings.liveBrowseNewPuzzle,
                ::BrowseExternalSettings,
            ),
            combine(
                settings.liveDownloadersSettings,
                settings.liveBackgroundDownloadSettings,
                settings.liveDownloadOnStartUp,
                ::BrowseDownloaderSettings,
            ),
            ::BrowseSettings,
        ),
    )
    val uiState : StateFlow<UIState> = mediatedUIState.stateFlow

    private val menuExpandStack : MutableList<BrowseSubMenu> = mutableListOf()
    private val _menuState
        = MutableStateFlow<BrowseMenuState>(BrowseMenuState())
    val menuState : StateFlow<BrowseMenuState> = _menuState

    private val _toastMessages = MutableStateFlow<List<String>>(listOf())
    val toastMessages : StateFlow<List<String>> = _toastMessages

    private val _downloadDialog
        = MutableStateFlow<DownloadDialogViewModel?>(null)
    val downloadDialog : StateFlow<DownloadDialogViewModel?> = _downloadDialog

    private val _importFinishResult
        = MutableStateFlow<ImportFinishResult?>(null)
    val importFinishResult : StateFlow<ImportFinishResult?>
        = _importFinishResult

    /**
     * True/false means import succeeded or failed now close activities
     */
    private val _importedNowExitResult = MutableStateFlow<Boolean?>(null)
    val importedNowExitResult : StateFlow<Boolean?>
        = _importedNowExitResult

    /**
     * True when a puzzle has just been loaded
     *
     * Call clearPuzzleLoaded to set back to false
     */
    private val _puzzleLoaded = MutableStateFlow<Boolean>(false)
    val puzzleLoaded : StateFlow<Boolean> = _puzzleLoaded

    // maps ID to puzzle data for all known puzzles
    private var puzzles : MutableMap<String, PuzData> = mutableMapOf()
    private var initialPuzzleLoadStarted = false

    val isViewArchive : Boolean
        get() { return currentUIState.isViewArchive }

    fun toggleSelectPuzzle(puzzleID : String) {
        currentUIState.selectedPuzzleIDs.let { selectedPuzzleIDs ->
            if (puzzleID in selectedPuzzleIDs)
                setSelectedPuzzles(selectedPuzzleIDs - puzzleID)
            else
                setSelectedPuzzles(selectedPuzzleIDs + puzzleID)
        }
    }

    fun clearSelection() {
        settings.getBrowseDisableSwipe() { disableSwipe ->
            currentUIState = currentUIState.copy(
                selectedPuzzleIDs = setOf(),
                disableSwipe = disableSwipe,
            )
        }
    }

    fun clearToastMessages() {
        _toastMessages.value = listOf()
    }

    fun setSortOrder(accessor : AccessorSetting) {
        settings.setBrowseSort(accessor)
    }

    fun startPuzzleFilter() {
        updateState(puzzleFilter = "")
    }

    fun setPuzzleFilter(filter : String) {
        updateState(puzzleFilter = filter)
    }

    fun clearPuzzleFilter() {
        updateState(puzzleFilter = null)
    }

    fun nextDayNightMode() {
        settings.getAppDayNightMode { currentMode ->
            val nextDNM = when (currentMode) {
                DayNightMode.DNM_NIGHT -> DayNightMode.DNM_SYSTEM
                DayNightMode.DNM_SYSTEM -> DayNightMode.DNM_DAY
                else -> DayNightMode.DNM_NIGHT
            }
            settings.setAppDayNightMode(nextDNM)
        }
    }

    fun expandMenu(menu : BrowseSubMenu) {
        menuExpandStack.add(menu)
        _menuState.value = menuState.value.copy(expanded = menu)
    }

    /**
     * Close most recently opened menu and return to previous
     */
    fun closeMenu() {
        if (menuExpandStack.isEmpty()) {
            _menuState.value = menuState.value.copy(
                expanded = BrowseSubMenu.NONE,
            )
        } else {
            menuExpandStack.removeAt(menuExpandStack.size - 1)
            _menuState.value = menuState.value.copy(
                expanded = menuExpandStack.last(),
            )
        }
    }

    /**
     * Dismiss menu completely
     */
    fun dismissMenu() {
        menuExpandStack.clear()
        _menuState.value = menuState.value.copy(expanded = BrowseSubMenu.NONE)
    }

    @MainThread
    fun startLoadPuzzleList() {
        startLoadPuzzleList(isViewArchive)
    }

    @MainThread
    fun swipeRefreshPuzzleList() {
        currentUIState = currentUIState.copy(
            isSwipeRefreshing = true,
        )
        startLoadPuzzleList()
    }

    @MainThread
    fun startLoadPuzzleList(archive : Boolean) {
        fileHandlerProvider.get { fileHandler ->
            threadWithUILock("load files") {
                val directory = if (archive)
                    fileHandler.archiveDirectory
                else
                    fileHandler.crosswordsDirectory

                puzzles = fileHandler.getPuzMetas(directory)
                    .map { getPuzData(it) }
                    .associateBy(
                        { it.puzEntry.id },
                        { it },
                    ).toMutableMap()

                runMain {
                    updateState(
                        isArchive = archive,
                        isNewPuzzleList = true,
                    )
                }
            }
        }
    }

    @MainThread
    fun deletePuzzle(puzzleID : String) {
        setSelectedPuzzles(setOf(puzzleID))
        deleteSelectedPuzzles()
    }

    /**
     * Start delete of selected puzzles (asks for confirmation)
     */
    @MainThread
    fun deleteSelectedPuzzles() {
        settings.getBrowseDontConfirmDelete { dontConfirm ->
            if (dontConfirm) {
                confirmDeleteSelectedPuzzles()
            } else {
                currentUIState = currentUIState.copy(
                    showDialog = BrowseDialog.CONFIRM_DELETE_SELECTED,
                )
            }
        }
    }

    /**
     * Confirm delete selected puzzles and clear selection
     */
    @MainThread
    fun confirmDeleteSelectedPuzzles() {
        clearDialog(BrowseDialog.CONFIRM_DELETE_SELECTED)
        val selectedPuzzleIDs = currentUIState.selectedPuzzleIDs
        clearSelection()
        doDeletePuzzles(selectedPuzzleIDs)
    }

    /**
     * Cancel delete selected puzzles
     */
    @MainThread
    fun cancelDeleteSelectedPuzzles() {
        clearDialog(BrowseDialog.CONFIRM_DELETE_SELECTED)
    }

    fun blankCopySelectedPuzzles() {
        val selectedPuzzleIDs = currentUIState.selectedPuzzleIDs
        clearSelection()

        fileHandlerProvider.get { fileHandler ->
            threadWithUILock("blank copy puzzles") {
                var copiedCount = 0
                for (puzzleID in selectedPuzzleIDs) {
                    val puzData = puzzles[puzzleID] ?: continue
                    try {
                        val puz
                            = fileHandler.load(puzData.puzHandle) ?: continue
                        val ph = fileHandler.saveNewPuzzle(
                            puz = puz,
                            fileNameBody = AppPuzzleUtils.generateFileName(puz),
                            writeBlank = true,
                            omitExtensions = true,
                        ) ?: continue

                        copiedCount += 1

                        if (!isViewArchive) {
                            loadPuzData(fileHandler, ph)?.let { pd->
                                puzzles[pd.puzEntry.id] = pd
                            }
                        }
                    } catch (e : IOException) {
                        AppLogger.w(
                            TAG,
                            "Failed to blank copy " + puzzleID,
                        )
                    }
                }
                if (!isViewArchive) {
                    runMain {
                        updateState(isNewPuzzleList = true)
                    }
                } else if (copiedCount > 0) {
                    sendToast(
                        utils.getQuantityString(
                            Res.plurals.blank_copied_puzzles_to_crosswords,
                            copiedCount,
                        ),
                    )
                }
            }
        }
    }

    @MainThread
    fun swipePuzzle(puzzleID : String) {
        fileHandlerProvider.get { fileHandler ->
            settings.getBrowseSwipeAction() {
                when (it) {
                    BrowseSwipeAction.BSA_DELETE -> deletePuzzle(puzzleID)
                    else -> {
                        val targetDir = if (isViewArchive)
                            fileHandler.crosswordsDirectory
                        else
                            fileHandler.archiveDirectory

                        movePuzzle(puzzleID, targetDir)
                    }
                }
            }
        }
    }

    /**
     * Move selected puzzles and clear selection
     */
    @MainThread
    fun toggleArchiveSelectedPuzzles() {
        fileHandlerProvider.get { fileHandler ->
            val destDir = if (isViewArchive)
                fileHandler.crosswordsDirectory
            else
                fileHandler.archiveDirectory

            clearSelection()
            movePuzzles(currentUIState.selectedPuzzleIDs, destDir)
        }
    }

    /**
     * Cleanup puzzles but confirm first if settings require
     */
    @MainThread
    fun cleanupPuzzles() {
        settings.getBrowseCleanupAge { maxAgeSetting ->
        settings.getBrowseCleanupAgeArchive { archiveMaxAgeSetting ->
        settings.getBrowseDontConfirmCleanup { dontConfirm ->
            if (
                isViewArchive
                && getMaxAge(maxAgeSetting) == null
                && getMaxAge(archiveMaxAgeSetting) == null
            ) {
                showDialog(BrowseDialog.NO_CLEANUP_ACTION)
            } else if (dontConfirm) {
                cleanupPuzzlesConfirmed()
            } else {
                showDialog(BrowseDialog.CONFIRM_CLEANUP)
            }
        }}}
    }

    fun clearNoCleanupAction() {
        clearDialog(BrowseDialog.NO_CLEANUP_ACTION)
    }

    fun clearConfirmCleanup() {
        clearDialog(BrowseDialog.CONFIRM_CLEANUP)
    }

    /**
     * Clean up puzzles without confirming if required
     */
    @MainThread
    fun cleanupPuzzlesConfirmed() {
        clearConfirmCleanup()
        settings.getBrowseDeleteOnCleanup { deleteOnCleanup ->
        settings.getBrowseCleanupAge { maxAgeSetting ->
        settings.getBrowseCleanupAgeArchive { archiveMaxAgeSetting ->
        fileHandlerProvider.get { fileHandler ->
            threadWithUILock("clean up puzzles") {
                val maxAge = getMaxAge(maxAgeSetting)
                val archiveMaxAge = getMaxAge(archiveMaxAgeSetting)

                val crosswords = fileHandler.crosswordsDirectory
                val archive = fileHandler.archiveDirectory

                val toArchive = mutableListOf<PuzHandle>()
                val toDelete = mutableListOf<PuzHandle>()

                fileHandler.getPuzMetas(crosswords).forEach { pm ->
                    val doClean
                        = pm.complete == 100
                        || (
                            maxAge != null
                            && getDate(pm).compareTo(maxAge) < 0
                        )

                    if (doClean) {
                        if (deleteOnCleanup) {
                            toDelete.add(pm.puzHandle)
                        } else {
                            toArchive.add(pm.puzHandle)
                        }
                    }
                }

                if (archiveMaxAge != null) {
                   fileHandler.getPuzMetas(archive).forEach { pm ->
                        if (getDate(pm).compareTo(archiveMaxAge) < 0) {
                            toDelete.add(pm.puzHandle)
                        }
                    }
                }

                toDelete.forEach { fileHandler.delete(it) }
                toArchive.forEach { fileHandler.moveTo(it, archive) }

                runMain { startLoadPuzzleList() }
            }
        }}}}
    }

    fun flagNotificationPermissionsDenied() {
        showDialog(BrowseDialog.NOTIFICATIONS_PERMISSION)
    }

    fun dismissNotificationsDialog() {
        clearDialog(BrowseDialog.NOTIFICATIONS_PERMISSION)
    }

    fun disableNotifications() {
        settings.disableNotifications()
        dismissNotificationsDialog()
    }

    /**
     * Flag seen current version and remove dialog
     */
    fun flagSeenCurrentVersion() {
        settings.setBrowseLastSeenVersion(currentVersion)
        clearDialog(BrowseDialog.NEW_VERSION)
    }

    @MainThread
    fun download(date : LocalDate, downloaders : List<Downloader>) {
        downloadersProvider.get { dls ->
            viewModelScope.launch(Dispatchers.IO) {
                dls.download(date, downloaders)
                if (!isViewArchive)
                    runMain(this@BrowsePageViewModel::startLoadPuzzleList)
            }
        }
    }

    /**
     * Runs autodownload if it hasn't run too recently
     *
     * Runs async but only runs one request at a time to avoid parallel
     * auto download jobs. Don't do anything if no network
     */
    fun autoDownloadIfRequired() {
        if (!utils.hasNetworkConnection())
            return

        settings.getDownloadersSettings { downloaderSettings ->
        downloadersProvider.get { dls ->
            // Should not run in parallel with other auto downloads.
            // If multiple calls are made rapidly, the first will
            // lock the block below and update lastDL so future
            // blocks don't redownload. Could have used a separate
            // single-thread autodownload executor instead, but this
            // avoids an extra thread and field variable.
            viewModelScope.launch(autoDownloadDispatcher) {
                val lastDL = settings.getBrowseLastDownloadSync()
                val downloadCutoff
                    = TimeUtils.currentTimeMillis() - (12 * 60 * 60 * 1000)

                val isDownload
                    = downloaderSettings.downloadOnStartUp
                    && downloadCutoff > lastDL

                if (isDownload) {
                    settings.setBrowseLastDownload(
                        TimeUtils.currentTimeMillis()
                    )
                    val now = TimeUtils.today()
                    download(now, dls.getAutoDownloaders(now))
                }
            }
        }}
    }

    /**
     * Call to load a puzzle
     *
     * Calls back via puzzleLoaded stateflow, then clear that with
     * clearPuzzleLoaded once handled.
     */
    @MainThread
    fun loadPuzzle(puzzleID : String) {
        puzzles[puzzleID]?.let { puzzleData ->
            currentPuzzleHolder.loadPuzzle(
                puzzleData.puzHandle,
                { _puzzleLoaded.value = true },
                { e ->
                    AppLogger.e(TAG, "Could not load file: " + e)

                    fileHandlerProvider.get { fileHandler ->
                        var filename : String? = null
                        try {
                            filename = fileHandler.getName(puzzleData.puzHandle)
                        } catch (ee : Exception) {
                            ee.printStackTrace()
                        }
                        viewModelScope.launch {
                            sendToast(
                                utils.getString(
                                    Res.string.unable_to_read_file,
                                    (filename ?: "")
                                )
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * Ack a loaded puzzle
     */
    fun clearPuzzleLoaded() {
        _puzzleLoaded.value = false
    }

    /**
     * Refresh the meta of the puzzle that is current
     *
     * It could have been in use by a play activity.
     */
    @MainThread
    fun refreshCurrentPuzzleMeta() {
        AppLogger.i(TAG, "Refreshing current puzzle in list with isUIBusy")
        currentPuzzleHolder.getFreshCurrentPuzzleMeta() { refreshedMeta ->
            AppLogger.i(TAG, "Done refreshing current puzzle in list with isUIBusy")
            if (refreshedMeta != null) {
                viewModelScope.launch {
                    val pd = getPuzData(refreshedMeta)
                    if (pd.puzEntry.id in puzzles) {
                        puzzles[pd.puzEntry.id] = pd
                    }
                    updateState(isNewPuzzleList = true)
                }
            }
        }
    }

    /**
     * Import files from uri to crosswords folder
     *
     * Toast message when finished
     */
    @MainThread
    fun importURIs(uris : Collection<Uri>, onDone : () -> Unit = { }) {
        importURIsAndCallback(uris) { someFailed, someSucceeded ->
            onDone()
            viewModelScope.launch {
                if (someFailed)
                    sendToast(utils.getString(Res.string.import_failure))
                else if (someSucceeded)
                    sendToast(utils.getString(Res.string.import_success))
                if (someSucceeded)
                    updateState(isNewPuzzleList = true)
            }
        }
    }

    /**
     * Import files from uri and post result to importFinishResult when done
     *
     * You have to do the finishing yourself!
     */
    @MainThread
    fun importURIsAndFinish(uris : Collection<Uri>) {
        importURIsAndCallback(uris) { someFailed, someSucceeded ->
            _importFinishResult.value = ImportFinishResult(
                someFailed,
                someSucceeded,
            )
        }
    }

    fun clearImportFinishResult() {
        _importFinishResult.value = null
    }

    fun showDownloadDialog() {
        _downloadDialog.value = DownloadDialogViewModel(
            viewModelScope,
            downloadersProvider,
            settings,
        )
    }

    fun clearDownloadDialog() {
        _downloadDialog.value = null
    }

    fun okDownloadDialog() {
        downloadDialog.value?.let {
            val date = it.date
            it.getSelectedDownloaders() { downloaders ->
                download(date, downloaders)
            }
        }
        clearDownloadDialog()
    }

    /**
     * Show dialog saying imports done app now closing
     *
     * Don't need to clear this dialog because you should exit!
     */
    fun setImportedNowExitResult(success : Boolean) {
        // True cos even failed imports might have gotten some successes
        // This should be a view model, but it's only one line
        settings.setBrowseNewPuzzle(true)
        _importedNowExitResult.value = success
    }

    /**
     * Import from uris and return someFailed, someSucceeded on main thread
     */
    @MainThread
    private fun importURIsAndCallback(
        uris : Collection<Uri>,
        cb : (Boolean, Boolean) -> Unit,
    ) {
        fileHandlerProvider.get { fileHandler ->
        settings.getDownloadTimeout { timeout ->
            threadWithUILock("import URIs") {
                var someFailed = false
                var someSucceeded = false
                var needsReload = false

                uris.forEach { uri ->
                    val ph = importPuzzleURI(
                        utils,
                        fileHandler,
                        uri,
                        timeout,
                    )

                    someFailed = someFailed || (ph == null)
                    someSucceeded = someSucceeded || (ph != null)

                    if (!isViewArchive && ph != null && !needsReload) {
                        try {
                            val pd = loadPuzData(fileHandler, ph)?.let { pd ->
                                puzzles[pd.puzEntry.id] = pd
                            }
                        } catch (e : IOException) {
                            // fall back to full reload
                            needsReload = true
                        }
                    }
                }

                if (needsReload)
                    runMain(this::startLoadPuzzleList)

                runMain { cb(someFailed, someSucceeded) }
            }
        }}
    }

    /**
     * Process to-import folder
     *
     * Import puzzles in there, move successful files to to-import-done
     */
    @MainThread
    fun processToImportDirectory() {
        fileHandlerProvider.get { fileHandler ->
            threadWithUILock("process to import dir") {
                val imported
                    = processPuzzleToImportDirectory(utils, fileHandler)

                imported.forEach { ph ->
                    try {
                        loadPuzData(fileHandler, ph)?.let { pd ->
                            puzzles[pd.puzEntry.id] = pd
                        }
                    } catch (e : IOException) {
                        AppLogger.w(TAG, "Could not load meta for handle " + ph)
                    }
                }

                runMain { updateState(isNewPuzzleList = true) }
            }
        }
    }

    fun toggleCollapseHeading(group : PuzGroup) {
        val id = group.id
        val expandedHeadingIDs = currentUIState.expandedHeadingIDs
        val newExpandedHeadingIDs = if (expandedHeadingIDs.contains(id))
            expandedHeadingIDs - id
        else
            expandedHeadingIDs + id
        currentUIState = currentUIState.copy(
            expandedHeadingIDs = newExpandedHeadingIDs,
        )
    }

    private fun getMaxAge(preferenceValue : Int) : LocalDate? {
        val cleanupValue = preferenceValue + 1
        if (cleanupValue > 0)
            return TimeUtils.plusDays(TimeUtils.today(), -cleanupValue)
        else
            return null
    }

    /**
     * crosswords if not viewing archive, else archive
     */
    private fun getViewedDirectory(fileHandler : FileHandler) : DirHandle {
        return if (isViewArchive)
            fileHandler.archiveDirectory
        else
            fileHandler.crosswordsDirectory
    }

    private fun threadWithUILock(
        taskName : String,
        run : suspend () -> Unit,
    ) {
        // No actual lock because dispatcher is single-thread
        viewModelScope.launch(uiLockDispatcher) {
            try {
                AppLogger.i(TAG, "Starting task " + taskName + " with isUIBusy")
                setUIBusy(true)
                run()
            } finally {
                setUIBusy(false)
                AppLogger.i(TAG, "Finished task " + taskName + " with isUIBusy")
            }
        }
    }

    private suspend fun entryMatches(
        pe : PuzEntry,
        filterText : String?,
    ) : Boolean {
        if (filterText == null)
            return true

        val upperFilterText = filterText.uppercase()

        if (pe.caption.uppercase().contains(upperFilterText))
            return true

        if (pe.title.uppercase().contains(upperFilterText))
            return true

        if (pe.author.uppercase().contains(upperFilterText))
            return true

        if (pe.source.uppercase().contains(upperFilterText))
            return true

        if (formatDateHeader(pe.date).uppercase().contains(upperFilterText))
            return true
        if (pe.publicationDate?.let {
            formatDateHeader(it).uppercase().contains(upperFilterText)
        } == true)
            return true

        return false
    }

    /**
     * As getUpdatedState but set the value to the state flow
     */
    private fun updateState(
        isArchive : Boolean = isViewArchive,
        isNewPuzzleList : Boolean = false,
        puzzleFilter : String? = currentUIState.puzzleFilter,
    ) {
        viewModelScope.launch {
            val disableRatings = settings.liveRatingsDisableRatings.first()
            val alwaysShowRating = settings.liveBrowseAlwaysShowRating.first()
            val isCollapseHeadings = settings.liveBrowseCollapseHeadings.first()

            val state = currentUIState
            val sortOrder = state.sortOrder
            var newPuzzleList : List<PuzGroup> = state.puzzleList
            if (
                isNewPuzzleList
                || puzzleFilter != currentUIState.puzzleFilter
            ) {
                newPuzzleList = getPuzzleList(
                    puzzleFilter,
                    state.sortOrder,
                    state.sortByPublicationDate,
                )
            }

            val displayInternalStorageMessage = (
                currentUIState.displayInternalStorageMessage
                && puzzles.isEmpty()
                && !isArchive
            )

            val showRatings = !disableRatings && (
                alwaysShowRating || isArchive
            )

            // reset to false when new puzzle list
            val isSwipeRefreshing
                = !isNewPuzzleList && state.isSwipeRefreshing

            currentUIState = state.copy(
                isViewArchive = isArchive,
                puzzleFilter = puzzleFilter,
                puzzleList = newPuzzleList,
                displayInternalStorageMessage
                    = displayInternalStorageMessage,
                showRatings = showRatings,
                isSwipeRefreshing = isSwipeRefreshing,
                isCollapseHeadings = isCollapseHeadings,
            )
        }
    }

    /**
     * Get updated state object
     *
     * @param isArchive if view is the archive
     * @param isNewPuzzleList if there is a new puzzle list to load into
     * state
     * @param puzzleFilter new puzzle filter value
     */
    private suspend fun getUpdatedState(
        state : UIState,
        browseSettings : BrowseSettings,
    ) : UIState {
        var newPuzzleList : List<PuzGroup> = state.puzzleList

        val newSortOrder = browseSettings.displaySettings.sortOrder
        val sortByPublicationDate
            = browseSettings.displaySettings.sortByPublicationDate
        if (
            state.sortOrder != newSortOrder
            || state.sortByPublicationDate != sortByPublicationDate
        ) {
            newPuzzleList = getPuzzleList(
                currentUIState.puzzleFilter,
                newSortOrder,
                sortByPublicationDate,
            )
        }

        val showPercentageCorrect
            = browseSettings.puzzleDisplaySettings.showPercentageCorrect
        val showRatings = browseSettings.puzzleDisplaySettings
            .ratingsSettings
            .let { rs ->
                !rs.disableRatings && (
                    rs.browseAlwaysShowRating || state.isViewArchive
                )
            }

        val indicateIfSolution = browseSettings.puzzleDisplaySettings
            .indicateIfSolution
        val dayNightMode = browseSettings.displaySettings.appDayNightMode
        // note, uses unfiltered puzzle list to detect if empty
        val displayInternalStorageMessage = (
            browseSettings.externalSettings.fileHandlerStorageLocation
                == StorageLocation.SL_INTERNAL
            && puzzles.isEmpty()
            && !state.isViewArchive
        )
        val isNotificationPermissionNeeded = browseSettings.downloaderSettings
            .downloadersSettings
            .isNotificationPermissionNeeded
        val isBackgroundDownloadEnabled = backgroundDownloadManager
            .getIsBackgroundDownloadEnabled(
                browseSettings.downloaderSettings.backgroundDownloadSettings,
            )
        val isDownloadOnStartUp = browseSettings.downloaderSettings
            .downloadOnStartUp

        val lastSeenVersion = browseSettings.externalSettings.lastSeenVersion
        val isNewVersion = currentVersion != lastSeenVersion
        val showDialog = if (
            state.showDialog == BrowseDialog.NONE && isNewVersion
        ) {
            BrowseDialog.NEW_VERSION
        } else {
            state.showDialog
        }
        val disableSwipe
            = browseSettings.actionSettings.disableSwipe || state.isSelecting

        return state.copy(
            sortOrder = newSortOrder,
            sortByPublicationDate = sortByPublicationDate,
            puzzleList = newPuzzleList,
            disableSwipe = disableSwipe,
            swipeAction = browseSettings.actionSettings.swipeAction,
            showPercentageCorrect = showPercentageCorrect,
            showRatings = showRatings,
            indicateIfSolution = indicateIfSolution,
            dayNightMode = dayNightMode,
            displayInternalStorageMessage = displayInternalStorageMessage,
            isNotificationPermissionNeeded = isNotificationPermissionNeeded,
            isBackgroundDownloadEnabled = isBackgroundDownloadEnabled,
            isDownloadOnStartUp = isDownloadOnStartUp,
            currentVersion = currentVersion,
            showDialog = showDialog,
            suppressHintHighlighting
                = browseSettings.puzzleDisplaySettings.suppressHintHighlighting,
        )
    }

    private suspend fun getPuzzleList(
        puzzleFilter : String?,
        sortOrder : AccessorSetting,
        sortByPublicationDate : Boolean,
    ) : List<PuzGroup> {
        return puzzles.values
            .map { it.puzEntry }
            .filter { entryMatches(it, puzzleFilter) }
            .groupBy { pe ->
                when (sortOrder) {
                    AccessorSetting.AS_SOURCE_DATE_ASC -> pe.source
                    AccessorSetting.AS_SOURCE_DATE_DESC -> pe.source
                    else -> formatDateHeader(
                        getSortDate(pe, sortByPublicationDate),
                    )
                }
            }.map { e ->
                val groupPuzzles = e.value.sortedWith { pe1, pe2 ->
                    comparePuzEntries(
                        sortOrder, pe1, pe2, sortByPublicationDate,
                    )
                }
                PuzGroup(
                    e.key,
                    groupPuzzles,
                )
            }.sortedWith { g1, g2 ->
                if (!g1.puzzles.isEmpty() && !g2.puzzles.isEmpty()) {
                    comparePuzEntries(
                        sortOrder,
                        g1.puzzles[0],
                        g2.puzzles[0],
                        sortByPublicationDate,
                    )
                } else { 0 }
            }
    }

    @OptIn(ExperimentalAtomicApi::class)
    private fun setUIBusy(value : Boolean) {
        uiBusy.store(value)
        runMain {
            currentUIState = currentUIState.copy(isBusy = uiBusy.load())
        }
    }

    /**
     * Get puz data from handle
     *
     * Requires load of meta, so prefer getPuzData with existing meta file.
     */
    @Throws(IOException::class, CancellationException::class)
    private suspend fun loadPuzData(
        fileHandler : FileHandler,
        ph : PuzHandle,
    ) : PuzData? {
        return fileHandler.loadPuzMetaFile(ph)?.let { getPuzData(it) }
    }

    private suspend fun getPuzData(pm : PuzMetaFile) : PuzData {
        val entry = PuzEntry(
            id = pm.mainFileURI,
            caption = getCaption(pm),
            date = pm.dateAdded,
            publicationDate = getPublicationDate(pm),
            percentComplete = pm.complete,
            percentFilled = pm.filled,
            percentHinted = pm.hinted,
            rating = pm.rating,
            hasSolution = pm.hasSolution,
            source = getSource(pm)
                ?: utils.getString(Res.string.unknown_source),
            title = getTitle(pm),
            author = pm.author ?: "",
        )
        return PuzData(entry, pm.puzHandle)
    }

    private fun comparePuzEntries(
        sortOrder : AccessorSetting,
        pe1 : PuzEntry,
        pe2 : PuzEntry,
        sortByPublicationDate : Boolean,
    ) : Int {
        val date1 = getSortDate(pe1, sortByPublicationDate)
        val date2 = getSortDate(pe2, sortByPublicationDate)
        when (sortOrder) {
            AccessorSetting.AS_SOURCE_DATE_ASC -> {
                val srcCmp = pe1.source.compareTo(pe2.source)
                if (srcCmp != 0)
                    return srcCmp

                val dateCmp = date1.compareTo(date2)
                if (dateCmp != 0)
                    return dateCmp
            }
            AccessorSetting.AS_SOURCE_DATE_DESC -> {
                val srcCmp = pe1.source.compareTo(pe2.source)
                if (srcCmp != 0)
                    return srcCmp

                val dateCmp = date2.compareTo(date1)
                if (dateCmp != 0)
                    return dateCmp
            }
            AccessorSetting.AS_DATE_ASC -> {
                val dateCmp = date1.compareTo(date2)
                if (dateCmp != 0)
                    return dateCmp

                val srcCmp = pe1.source.compareTo(pe2.source)
                if (srcCmp != 0)
                    return srcCmp
            }
            else -> {
                val dateCmp = date2.compareTo(date1)
                if (dateCmp != 0)
                    return dateCmp

                val srcCmp = pe1.source.compareTo(pe2.source)
                if (srcCmp != 0)
                    return srcCmp
            }
        }

        // fallback
        return pe1.id.compareTo(pe2.id)
    }

    private fun sendToast(message : String) {
        _toastMessages.value = toastMessages.value + message
    }

    @MainThread
    private fun movePuzzle(puzzleID : String, destDir : DirHandle) {
        movePuzzles(setOf(puzzleID), destDir)
    }

    @MainThread
    private fun movePuzzles(
        puzzleIDs : Collection<String>,
        destDir : DirHandle,
    ) {
        fileHandlerProvider.get { fileHandler ->
            threadWithUILock("move puzzles") {
                val directory = getViewedDirectory(fileHandler)
                val addToList = destDir.equals(directory)

                puzzleIDs.forEach { puzzleID ->
                    puzzles[puzzleID]?.let { puzzleData ->
                        val puzHandle = puzzleData.puzHandle
                        val removeFromList = puzHandle.isInDirectory(directory)

                        fileHandler.moveTo(puzHandle, destDir)

                        if (removeFromList && !addToList)
                            puzzles.remove(puzzleID)
                    }
                }

                runMain { updateState(isNewPuzzleList = true) }
            }
        }
    }

    private suspend fun getCaption(pm : PuzMetaFile) : String {
        var caption = pm.title ?: ""
        val title = getTitle(pm)
        val author = pm.author ?: ""
        val addAuthor = !author.isEmpty()
            && !title.contains(author, ignoreCase=true)
            && !caption.contains(author, ignoreCase=true)
        if (addAuthor) {
            caption = utils.getString(
                Res.string.puzzle_caption_with_author,
                caption,
                author
            )
        }
        return caption
    }

    private fun getDate(pm : PuzMetaFile) : LocalDate {
        return AppPuzzleUtils.deriveDate(pm.date, pm.fileName)
            ?: pm.dateAdded
    }

    /**
     * The metadata date falls back to the added date when no publication
     * date was found. Matching dates therefore indicate a fallback.
     */
    private fun getPublicationDate(pm : PuzMetaFile) : LocalDate? {
        return AppPuzzleUtils.deriveDate(pm.date, pm.fileName)
            ?.takeIf { it != pm.dateAdded }
    }

    private fun getSortDate(
        pe : PuzEntry,
        sortByPublicationDate : Boolean,
    ) : LocalDate {
        return if (sortByPublicationDate)
            pe.publicationDate ?: pe.date
        else
            pe.date
    }

    private fun getSource(pm : PuzMetaFile) : String? {
        return AppPuzzleUtils.deriveSource(
            pm.source,
            pm.fileName,
            pm.author,
            pm.title,
        )
    }

    public fun getTitle(pm : PuzMetaFile) : String {
        val source = getSource(pm) ?: ""
        return if (!source.isEmpty())
            source
        else
            pm.fileNameNoExtension
    }

    private fun showDialog(dialog : BrowseDialog) {
        currentUIState = currentUIState.copy(showDialog = dialog)
    }

    private fun clearDialog(dialog : BrowseDialog) {
        if (currentUIState.showDialog == dialog)
            showDialog(BrowseDialog.NONE)
    }

    @MainThread
    private fun doDeletePuzzles(puzzleIDs : Collection<String>) {
        fileHandlerProvider.get { fileHandler ->
            threadWithUILock("delete puzzles") {
                val viewedDir = getViewedDirectory(fileHandler)
                puzzleIDs.forEach { puzzleID ->
                    puzzles[puzzleID]?.let { puzzleData ->
                        fileHandler.delete(puzzleData.puzHandle)
                        if (puzzleData.puzHandle.isInDirectory(viewedDir))
                            puzzles.remove(puzzleID)
                    }
                }
                runMain { updateState(isNewPuzzleList = true) }
            }
        }
    }

    private fun setSelectedPuzzles(newSelection : Set<String>) {
        settings.getBrowseDisableSwipe() { disableSwipe ->
            currentUIState = currentUIState.copy(
                selectedPuzzleIDs = newSelection,
                disableSwipe = disableSwipe || !newSelection.isEmpty(),
            )
        }
    }

    private fun checkNeedPuzzleLoad(browseSettings : BrowseSettings) {
        if (
           !initialPuzzleLoadStarted
           || (browseSettings.externalSettings.newPuzzle && !isViewArchive)
        ) {
            initialPuzzleLoadStarted = true
            startLoadPuzzleList()
        }
        settings.setBrowseNewPuzzle(false)
    }

    private fun runMain(run : () -> Unit) {
        viewModelScope.launch(Dispatchers.Main) { run() }
    }

    private suspend fun formatDateHeader(date : LocalDate) : String {
        val dayID = TimeUtils.getDayNameID(date)
        val monthID = TimeUtils.getMonthNameID(date)
        return utils.getString(
            Res.string.browse_date_format_header,
            utils.getString(dayID),
            date.day,
            utils.getString(monthID),
            date.year,
        )
    }

    private val currentVersion : String = BuildConfig.APP_VERSION

    private var currentUIState : UIState
        get() { return mediatedUIState.current }
        set(value) { mediatedUIState.current = value }
}
