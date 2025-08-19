
package app.crossword.yourealwaysbe.forkyz

import java.io.IOException
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.BiConsumer
import javax.inject.Inject

import android.app.Application
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.net.Downloader
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.AccessorSetting
import app.crossword.yourealwaysbe.forkyz.settings.BrowseSwipeAction
import app.crossword.yourealwaysbe.forkyz.settings.DayNightMode
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.RatingsSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.AppPuzzleUtils
import app.crossword.yourealwaysbe.forkyz.util.BackgroundDownloadManager
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.PuzzleImporter
import app.crossword.yourealwaysbe.forkyz.util.files.DirHandle
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile
import app.crossword.yourealwaysbe.forkyz.util.liveDataFanIn
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.forkyz.view.DownloadDialogViewModel

private val TAG = "ForkyzBrowseActivity"
private val HEADER_DATE_FORMAT
    = DateTimeFormatter.ofPattern("EEEE dd MMM yyyy")
private val DEFAULT_DAY_NIGHT_MODE = DayNightMode.forNumber(0)

/**
 * Dialogs to be shown
 *
 * Confirm Cleanup -- OK/Cancel when cleanup about to happen
 * New Version -- new version message
 * No Cleanup Action -- all cleanup options set to never so nothing will happen
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
 */
data class UIState(
    val isViewArchive : Boolean = false,
    val puzzleFilter : String? = null,
    val isBusy : Boolean = false,
    val sortOrder : AccessorSetting = AccessorSetting.AS_DATE_DESC,
    val puzzleList : List<PuzGroup> = listOf(),
    val disableSwipe : Boolean = true,
    val swipeAction : BrowseSwipeAction = BrowseSwipeAction.BSA_ARCHIVE,
    val showPercentageCorrect : Boolean = false,
    val showRatings : Boolean = false,
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
) {
    val displayNoPuzzlesMessage : Boolean = puzzleList.isEmpty()
    val isSelecting : Boolean = !selectedPuzzleIDs.isEmpty()
}

/**
 * Entry for puzzle in list
 */
data class PuzEntry(
    val id : String,
    val caption : String,
    val date : LocalDate,
    val percentComplete : Int,
    val percentFilled : Int,
    val rating : Char,
    val hasSolution : Boolean,
    val source : String,
    val title : String,
    val author : String,
)

/**
 * Group of puzzles with shared header
 */
data class PuzGroup (
    val header : String,
    val puzzles : List<PuzEntry>,
)

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
    val isViewArchive : Boolean = false,
    val expanded : BrowseSubMenu = BrowseSubMenu.NONE,
)

/**
 * All info needed for internal puzzle map
 */
private data class PuzData(
    val puzEntry : PuzEntry,
    val puzHandle : PuzHandle,
)

@HiltViewModel
class BrowseActivityViewModel @Inject constructor(
    application : Application,
    val utils : AndroidVersionUtils,
    val settings : ForkyzSettings,
    val fileHandlerProvider : FileHandlerProvider,
    val currentPuzzleHolder : CurrentPuzzleHolder,
    val downloadersProvider : DownloadersProvider,
    val backgroundDownloadManager : BackgroundDownloadManager,
) : AndroidViewModel(application) {

    // important that it is single thread to avoid multiple
    // simultaneous operations
    private val executorService = Executors.newSingleThreadExecutor()
    // not fixed num in case user creates loads of downloads
    private val downloadExecutorService = Executors.newCachedThreadPool()
    private val handler = Handler(Looper.getMainLooper())

    private val _uiState : MediatorLiveData<UIState> = liveDataFanIn(
        this::getUpdatedState,
        settings.liveBrowseSort,
        settings.liveBrowseDisableSwipe,
        settings.liveBrowseSwipeAction,
        settings.liveBrowseShowPercentageCorrect,
        settings.liveRatingsSettings,
        settings.liveBrowseIndicateIfSolution,
        settings.liveAppDayNightMode,
        settings.liveFileHandlerStorageLocation,
        settings.liveDownloadersSettings,
        backgroundDownloadManager.liveIsBackgroundDownloadEnabled,
        settings.liveDownloadOnStartUp,
        settings.liveBrowseLastSeenVersion,
    )
    val uiState : LiveData<UIState> = _uiState

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

    init {
        // load puzzles when new puzzle flagged, or if puzzles haven't
        // been loaded yet
        _uiState.addSource(
            settings.liveBrowseNewPuzzle,
            { newPuzzle ->
                if (newPuzzle || !initialPuzzleLoadStarted) {
                    if (!isViewArchive || !initialPuzzleLoadStarted) {
                        initialPuzzleLoadStarted = true
                        startLoadPuzzleList()
                    }
                    settings.setBrowseNewPuzzle(false)
                }
            },
        )
    }

    val isViewArchive : Boolean
        get() {
            return uiState.value?.isViewArchive ?: false
        }

    fun toggleSelectPuzzle(puzzleID : String) {
        uiState.value?.selectedPuzzleIDs?.let { selectedPuzzleIDs ->
            if (puzzleID in selectedPuzzleIDs)
                setSelectedPuzzles(selectedPuzzleIDs - puzzleID)
            else
                setSelectedPuzzles(selectedPuzzleIDs + puzzleID)
        }
    }

    fun clearSelection() {
        _uiState.value = uiState.value?.copy(
            selectedPuzzleIDs = setOf(),
            disableSwipe = isDisableSwipeSetting,
        )
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
        val state = menuState.value ?: BrowseMenuState()
        _menuState.value = state.copy(expanded = menu)
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
        _uiState.value = uiState.value?.copy(
            isSwipeRefreshing = true,
        )
        startLoadPuzzleList()
    }

    @MainThread
    fun startLoadPuzzleList(archive : Boolean) {
        fileHandlerProvider.get { fileHandler ->
            threadWithUILock("load files") {
                val directory = if (archive)
                    fileHandler.getArchiveDirectory()
                else
                    fileHandler.getCrosswordsDirectory()

                puzzles = fileHandler.getPuzMetas(directory)
                    .map(this::getPuzData)
                    .associateBy(
                        { it.puzEntry.id },
                        { it },
                    ).toMutableMap()

                handler.post() {
                    updateState(archive, true)
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
                _uiState.value = uiState.value?.copy(
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
        uiState.value?.selectedPuzzleIDs?.let { selectedPuzzleIDs ->
            clearDialog(BrowseDialog.CONFIRM_DELETE_SELECTED)
            clearSelection()
            doDeletePuzzles(selectedPuzzleIDs)
        }
    }

    /**
     * Cancel delete selected puzzles
     */
    @MainThread
    fun cancelDeleteSelectedPuzzles() {
        clearDialog(BrowseDialog.CONFIRM_DELETE_SELECTED)
    }

    @MainThread
    fun swipePuzzle(puzzleID : String) {
        fileHandlerProvider.get { fileHandler ->
            settings.getBrowseSwipeAction() {
                when (it) {
                    BrowseSwipeAction.BSA_DELETE -> deletePuzzle(puzzleID)
                    else -> {
                        val targetDir = if (isViewArchive)
                            fileHandler.getCrosswordsDirectory()
                        else
                            fileHandler.getArchiveDirectory()

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
            uiState.value?.selectedPuzzleIDs?.let { selectedPuzzleIDs ->
                val destDir = if (isViewArchive)
                    fileHandler.getCrosswordsDirectory()
                else
                    fileHandler.getArchiveDirectory()

                clearSelection()
                movePuzzles(selectedPuzzleIDs, destDir)
            }
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
                getMaxAge(maxAgeSetting) == null
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

                if (maxAge != null || archiveMaxAge != null) {
                    val crosswords = fileHandler.getCrosswordsDirectory()
                    val archive = fileHandler.getArchiveDirectory()

                    val toArchive = ArrayList<PuzHandle>()
                    val toDelete = ArrayList<PuzHandle>()

                    fileHandler.getPuzMetas(crosswords).forEach { pm ->
                        val doClean
                            = pm.getComplete() == 100
                            || (maxAge != null && getDate(pm).isBefore(maxAge))

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
                            if (getDate(pm).isBefore(archiveMaxAge)) {
                                toDelete.add(pm.puzHandle)
                            }
                        }
                    }

                    toDelete.forEach { fileHandler.delete(it) }
                    toArchive.forEach { fileHandler.moveTo(it, archive) }

                    handler.post { startLoadPuzzleList() }
                }
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
            downloadExecutorService.execute {
                dls.download(date, downloaders)
                if (!isViewArchive)
                    handler.post(this::startLoadPuzzleList)
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
        if (!utils.hasNetworkConnection(getApplication()))
            return

        settings.getDownloadersSettings { downloaderSettings ->
        downloadersProvider.get { dls ->
            downloadExecutorService.execute {
                // Should not run in parallel with other auto downloads.
                // If multiple calls are made rapidly, the first will
                // lock the block below and update lastDL so future
                // blocks don't redownload. Could have used a separate
                // single-thread autodownload executor instead, but this
                // avoids an extra thread and field variable.
                synchronized (this@BrowseActivityViewModel) {
                    val lastDL = settings.getBrowseLastDownloadSync()
                    val downloadCutoff
                        = System.currentTimeMillis() - (12 * 60 * 60 * 1000)

                    val isDownload
                        = downloaderSettings.downloadOnStartUp
                        && downloadCutoff > lastDL

                    if (isDownload) {
                        settings.setBrowseLastDownload(
                            System.currentTimeMillis()
                        )
                        handler.post {
                            download(LocalDate.now(), dls.getAutoDownloaders())
                        }
                    }
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
                    Log.e(TAG, "Could not load file: " + e)

                    fileHandlerProvider.get { fileHandler ->
                        var filename : String? = null
                        try {
                            filename = fileHandler.getName(puzzleData.puzHandle)
                        } catch (ee : Exception) {
                            ee.printStackTrace()
                        }

                        val application : Application = getApplication()
                        sendToast(
                            application.getString(
                                R.string.unable_to_read_file,
                                (filename ?: "")
                            )
                        )
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
        Log.i(TAG, "Refreshing current puzzle in list with isUIBusy")
        setUIBusy(true)
        currentPuzzleHolder.getFreshCurrentPuzzleMeta() { refreshedMeta ->
            setUIBusy(false)
            Log.i(TAG, "Done refreshing current puzzle in list with isUIBusy")
            if (refreshedMeta != null) {
                val pd = getPuzData(refreshedMeta)
                if (pd.puzEntry.id in puzzles)
                    puzzles[pd.puzEntry.id] = pd
                updateState(isNewPuzzleList = true)
            }
        }
    }

    /**
     * Import files from uri to crosswords folder
     *
     * Toast message when finished
     */
    @MainThread
    fun importURIs(uris : Collection<Uri>) {
        importURIsAndCallback(uris) { someFailed, someSucceeded ->
            val application : Application = getApplication()
            if (someFailed)
                sendToast(application.getString(R.string.import_failure))
            else if (someSucceeded)
                sendToast(application.getString(R.string.import_success))
            if (someSucceeded)
                updateState(isNewPuzzleList = true)
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
        _downloadDialog.value = DownloadDialogViewModel(downloadersProvider)
    }

    fun clearDownloadDialog() {
        _downloadDialog.value = null
    }

    fun okDownloadDialog() {
        downloadDialog.value?.let {
            val date = it.date
            val downloaders = it.selectedDownloaders
            download(date, downloaders)
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
                    val ph = PuzzleImporter.importUri(
                        getApplication(),
                        fileHandler,
                        utils,
                        uri,
                        timeout,
                    )

                    someFailed = someFailed || (ph == null)
                    someSucceeded = someSucceeded || (ph != null)

                    if (!isViewArchive && ph != null && !needsReload) {
                        try {
                            val pm = fileHandler.loadPuzMetaFile(ph)
                            val pd = getPuzData(pm)
                            puzzles[pd.puzEntry.id] = pd
                        } catch (e : IOException) {
                            // fall back to full reload
                            needsReload = true
                        }
                    }
                }

                if (needsReload)
                    handler.post(this::startLoadPuzzleList)

                handler.post { cb(someFailed, someSucceeded) }
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
                val imported = PuzzleImporter.processToImportDirectory(
                    getApplication(),
                    fileHandler,
                    utils,
                )

                imported.forEach { ph ->
                    try {
                        val pm = fileHandler.loadPuzMetaFile(ph)
                        val pd = getPuzData(pm)
                        puzzles[pd.puzEntry.id] = pd
                    } catch (e : IOException) {
                        Log.w(TAG, "Could not load meta for handle " + ph)
                    }
                }

                handler.post { updateState(isNewPuzzleList = true) }
            }
        }
    }

    private fun getMaxAge(preferenceValue : Int) : LocalDate? {
        val cleanupValue = preferenceValue + 1
        if (cleanupValue > 0)
            return LocalDate.now().minus(Period.ofDays(cleanupValue))
        else
            return null
    }

    /**
     * crosswords if not viewing archive, else archive
     */
    private fun getViewedDirectory(fileHandler : FileHandler) : DirHandle {
        return if (isViewArchive)
            fileHandler.getArchiveDirectory()
        else
            fileHandler.getCrosswordsDirectory()
    }

    override protected fun onCleared() {
        executorService.shutdown()
        downloadExecutorService.shutdown()
    }

    private fun threadWithUILock(taskName : String, r : Runnable) {
        // no lock actually needed because executorService is single
        // threaded guaranteed
        Log.i(TAG, "Starting task " + taskName + " with isUIBusy")
        executorService.execute {
            try {
                setUIBusy(true)
                r.run()
            } finally {
                setUIBusy(false)
                Log.i(TAG, "Finished task " + taskName + " with isUIBusy")
            }
        }
    }

    private fun entryMatches(
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

        if (
            HEADER_DATE_FORMAT.format(pe.date)
                .uppercase()
                .contains(upperFilterText)
        )
            return true

        return false
    }

    /**
     * As getUpdatedState but set the value to the state flow
     */
    private fun updateState(
        isArchive : Boolean = isViewArchive,
        isNewPuzzleList : Boolean = false,
        puzzleFilter : String? = uiState.value?.puzzleFilter,
    ) {
        val state = getUpdatedState(isArchive, isNewPuzzleList, puzzleFilter)
        _uiState.value = state
    }

    /**
     * Get updated state object
     *
     * @param isArchive if view is the archive
     * @param isNewPuzzleList if there is a new puzzle list to load into
     * state
     * @param puzzleFilter new puzzle filter value
     */
    private fun getUpdatedState(
        isArchive : Boolean = isViewArchive,
        isNewPuzzleList : Boolean = false,
        puzzleFilter : String? = uiState.value?.puzzleFilter,
    ) : UIState {
        val state = uiState.value ?: UIState()
        var newPuzzleList : List<PuzGroup> = state.puzzleList

        val isNewFilter = state.puzzleFilter != puzzleFilter
        val newSortOrder = settings.liveBrowseSort.value ?: state.sortOrder
        val isNewSort = state.sortOrder != newSortOrder
        if (isNewPuzzleList || isNewFilter || isNewSort) {
            newPuzzleList = puzzles.values
                .map { it.puzEntry }
                .filter { entryMatches(it, puzzleFilter) }
                .groupBy { pe ->
                    when (newSortOrder) {
                        AccessorSetting.AS_SOURCE -> pe.source
                        else -> HEADER_DATE_FORMAT.format(pe.date)
                    }
                }.map { e ->
                    PuzGroup(
                        e.key,
                        e.value.sortedWith { pe1, pe2 ->
                            pe1.id.compareTo(pe2.id)
                        }
                    )
                }.sortedWith { g1, g2 ->
                    if (!g1.puzzles.isEmpty() && !g2.puzzles.isEmpty()) {
                        comparePuzEntries(
                            newSortOrder,
                            g1.puzzles[0],
                            g2.puzzles[0],
                        )
                    } else { 0 }
                }
        }

        val showPercentageCorrect
            = settings.liveBrowseShowPercentageCorrect.value ?: false
        val showRatings = settings.liveRatingsSettings.value?.let { rs ->
            !rs.disableRatings
                && (rs.browseAlwaysShowRating || isArchive)
            } ?: false
        val indicateIfSolution
            = settings.liveBrowseIndicateIfSolution.value
                ?: state.indicateIfSolution
        val dayNightMode
            = settings.liveAppDayNightMode.value ?: DEFAULT_DAY_NIGHT_MODE
        // note, uses unfiltered puzzle list to detect if empty
        val displayInternalStorageMessage = (
            settings.liveFileHandlerStorageLocation.value
                == StorageLocation.SL_INTERNAL
            && puzzles.isEmpty()
            && !isArchive
        )
        val isNotificationPermissionNeeded = settings.liveDownloadersSettings
            .value
            ?.isNotificationPermissionNeeded
            ?: false
        val isBackgroundDownloadEnabled = backgroundDownloadManager
            .liveIsBackgroundDownloadEnabled.value
            ?: false
        val isDownloadOnStartUp = settings.liveDownloadOnStartUp.value ?: false

        val lastSeenVersion = settings.liveBrowseLastSeenVersion.value
            ?: currentVersion
        val isNewVersion = currentVersion != lastSeenVersion
        val showDialog = if (
            state.showDialog == BrowseDialog.NONE && isNewVersion
        ) {
            BrowseDialog.NEW_VERSION
        } else {
            state.showDialog
        }
        val disableSwipe = isDisableSwipeSetting || state.isSelecting
        // reset to false when new puzzle list
        val isSwipeRefreshing = !isNewPuzzleList && state.isSwipeRefreshing

        return state.copy(
            isViewArchive = isArchive,
            puzzleFilter = puzzleFilter,
            sortOrder = newSortOrder,
            puzzleList = newPuzzleList,
            disableSwipe = disableSwipe,
            swipeAction = settings.liveBrowseSwipeAction.value ?:
                BrowseSwipeAction.BSA_ARCHIVE,
            showPercentageCorrect = showPercentageCorrect,
            showRatings = showRatings,
            indicateIfSolution = indicateIfSolution,
            dayNightMode = dayNightMode,
            displayInternalStorageMessage = displayInternalStorageMessage,
            isNotificationPermissionNeeded = isNotificationPermissionNeeded,
            isBackgroundDownloadEnabled = isBackgroundDownloadEnabled,
            isDownloadOnStartUp = isDownloadOnStartUp,
            currentVersion = currentVersion,
            isSwipeRefreshing = isSwipeRefreshing,
            showDialog = showDialog,
        )
    }

    private fun setUIBusy(value : Boolean) {
        handler.post {
            _uiState.value = uiState.value?.copy(isBusy = value)
        }
    }

    private fun getPuzData(pm : PuzMetaFile) : PuzData {
        val application : Application = getApplication()
        val entry = PuzEntry(
            id = pm.mainFileURI,
            caption = getCaption(pm),
            date = getDate(pm),
            percentComplete = pm.complete,
            percentFilled = pm.filled,
            rating = pm.rating,
            hasSolution = pm.hasSolution(),
            source = getSource(pm)
                ?: application.getString(R.string.unknown_source),
            title = getTitle(pm),
            author = pm.author ?: "",
        )
        return PuzData(entry, pm.puzHandle)
    }

    private fun comparePuzEntries(
        sortOrder : AccessorSetting,
        pe1 : PuzEntry,
        pe2 : PuzEntry,
    ) : Int {
        return when (sortOrder) {
            AccessorSetting.AS_SOURCE -> pe1.source.compareTo(pe2.source)
            AccessorSetting.AS_DATE_ASC -> pe1.date.compareTo(pe2.date)
            else -> pe2.date.compareTo(pe1.date)
        }
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

                handler.post { updateState(isNewPuzzleList = true) }
            }
        }
    }

    private fun getCaption(pm : PuzMetaFile) : String {
        var caption = pm.title ?: ""
        val title = getTitle(pm)
        val author = pm.author ?: ""
        val addAuthor = !author.isEmpty()
            && !title.contains(author, ignoreCase=true)
            && !caption.contains(author, ignoreCase=true)
        if (addAuthor) {
            val application : Application = getApplication()
            caption = application.getString(
                R.string.puzzle_caption_with_author,
                caption,
                author
            )
        }
        return caption
    }

    private fun getDate(pm : PuzMetaFile) : LocalDate {
        return AppPuzzleUtils.deriveDate(pm.date, pm.fileName)
            ?: pm.fileModifiedDate
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
        _uiState.value = uiState.value?.copy(
            showDialog = dialog,
        )
    }

    private fun clearDialog(dialog : BrowseDialog) {
        if (uiState.value?.showDialog == dialog)
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
                handler.post() {
                    updateState(isNewPuzzleList = true)
                }
            }
        }
    }

    private fun setSelectedPuzzles(newSelection : Set<String>) {
        _uiState.value = uiState.value?.copy(
            selectedPuzzleIDs = newSelection,
            disableSwipe = isDisableSwipeSetting || !newSelection.isEmpty(),
        )
    }

    private val currentVersion : String
        = utils.getApplicationVersionName(getApplication())

    private val isDisableSwipeSetting : Boolean
        get() {
            return settings.liveBrowseDisableSwipe?.value ?: true
        }
}
