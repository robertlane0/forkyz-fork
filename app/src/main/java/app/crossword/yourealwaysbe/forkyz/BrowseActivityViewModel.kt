
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
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.net.Downloader
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.RatingsSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.PuzzleImporter
import app.crossword.yourealwaysbe.forkyz.util.SingleLiveEvent
import app.crossword.yourealwaysbe.forkyz.util.files.Accessor
import app.crossword.yourealwaysbe.forkyz.util.files.DirHandle
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile
import app.crossword.yourealwaysbe.forkyz.util.liveDataFanIn
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

private val TAG = "ForkyzBrowseActivity"
private val MATCHER_DATE_FORMAT
    = DateTimeFormatter.ofPattern("EEEE dd MMM yyyy")

/**
 * UI state of Browse Activity
 *
 * @param isViewArchive true if archive is showing not main list
 * @param puzzleFilter non-null if list limited to puzzles containing
 * string
 * @param isBusy true if busy with something that isn't downloading
 * @param sortOrder the order the puzzles are sorted
 * @param puzzleList the list of files, each file is mutable so can be
 * observed (TODO: remove this when using compose)
 * @param disableSwipe disable swipe actions
 * @param showPercentageCorrect show percentage correct cells rather
 * than filled
 * @param showRatings if to display ratings
 * @param indicateIfSolution indicate in list if a puzzle has solutions
 */
data class UIState(
    val isViewArchive : Boolean = false,
    val puzzleFilter : String = "",
    val isBusy : Boolean = false,
    val sortOrder : Accessor = Accessor.DATE_ASC,
    val puzzleList : List<LiveData<PuzMetaFile>> = listOf(),
    val disableSwipe : Boolean = true,
    val showPercentageCorrect : Boolean = false,
    val showRatings : Boolean = false,
    val indicateIfSolution : Boolean = false,
)

@HiltViewModel
class BrowseActivityViewModel @Inject constructor(
    application : Application,
    val utils : AndroidVersionUtils,
    val settings : ForkyzSettings,
    val fileHandlerProvider : FileHandlerProvider,
    val currentPuzzleHolder : CurrentPuzzleHolder,
    val downloadersProvider : DownloadersProvider,
) : AndroidViewModel(application) {

    // important that it is single thread to avoid multiple
    // simultaneous operations
    private val executorService = Executors.newSingleThreadExecutor()
    // not fixed num in case user creates loads of downloads
    private val downloadExecutorService = Executors.newCachedThreadPool()
    private val handler = Handler(Looper.getMainLooper())

    // TODO: kept for data-binding in browse.xml, remove when Compose
    val puzzleFilter = MutableLiveData<String>("")

    private val _uiState : MediatorLiveData<UIState> = liveDataFanIn(
        this::updateState,
        settings.liveBrowseSort,
        puzzleFilter,
        settings.liveBrowseDisableSwipe,
        settings.liveBrowseShowPercentageCorrect,
        settings.liveRatingsSettings,
        settings.liveBrowseIndicateIfSolution,
    )
    val uiState : LiveData<UIState> = _uiState

    val puzzleLoadEvents = SingleLiveEvent<Void>()

    // local copy will be filtered and posted to uiState when updated
    private var puzzleList
        : MutableList<MutableLiveData<PuzMetaFile>> = mutableListOf()

    init {
        _uiState.addSource(
            settings.liveBrowseNewPuzzle,
            { newPuzzle ->
                if (newPuzzle) {
                    if (!isViewArchive)
                        startLoadFiles()
                    settings.setBrowseNewPuzzle(false)
                }
            },
        )
    }

    val isViewArchive : Boolean
        get() {
            return uiState.value?.isViewArchive ?: false
        }

    // TODO: not needed once Compose?
    val sortOrder : Accessor
        get() {
            return uiState.value?.sortOrder ?: Accessor.DATE_ASC
        }

    val puzzleFiles : List<LiveData<PuzMetaFile>>
        get() {
            return uiState.value?.puzzleList ?: listOf()
        }


    fun setSortOrder(accessor : Accessor) {
        settings.setBrowseSort(accessor)
    }

    fun clearPuzzleFilter() {
        puzzleFilter.postValue(null)
    }

    @MainThread
    fun startLoadFiles() {
        startLoadFiles(isViewArchive)
    }

    @MainThread
    fun startLoadFiles(archive : Boolean) {
        fileHandlerProvider.get() { fileHandler ->
            threadWithUILock("load files") {
                val directory = if (archive)
                    fileHandler.getArchiveDirectory()
                else
                    fileHandler.getCrosswordsDirectory()

                puzzleList = fileHandler.getPuzMetas(directory).map { pm ->
                    MutableLiveData<PuzMetaFile>(pm)
                }.toMutableList()

                handler.post() {
                    updateState(archive, true)
                }
            }
        }
    }

    @MainThread
    fun deletePuzzle(puzMeta : PuzMetaFile) {
        deletePuzzles(Collections.singleton(puzMeta))
    }

    @MainThread
    fun deletePuzzles(puzMetas : Collection<PuzMetaFile>) {
        fileHandlerProvider.get() { fileHandler ->
            threadWithUILock("delete puzzles") {
                val viewedDir = getViewedDirectory(fileHandler)
                puzMetas.forEach { puzMeta ->
                    fileHandler.delete(puzMeta)
                    if (puzMeta.isInDirectory(viewedDir))
                        removeFromPuzzleList(puzMeta)
                }
            }
        }
    }

    @MainThread
    fun movePuzzle(puzMeta : PuzMetaFile, destDir : DirHandle) {
        movePuzzles(Collections.singleton(puzMeta), destDir)
    }

    @MainThread
    fun movePuzzles(
        puzMetas : Collection<PuzMetaFile>,
        destDir : DirHandle,
    ) {
        fileHandlerProvider.get() { fileHandler ->
            threadWithUILock("move puzzles") {
                val directory = getViewedDirectory(fileHandler)
                puzMetas.forEach { puzMeta ->
                    val addToList = destDir.equals(directory)
                    val removeFromList = puzMeta.isInDirectory(directory)

                    fileHandler.moveTo(puzMeta, destDir)

                    if (addToList && !removeFromList)
                        addPuzzleToList(puzMeta)
                    else if (removeFromList && !addToList)
                        removeFromPuzzleList(puzMeta)
                }
            }
        }
    }

    @MainThread
    fun cleanUpPuzzles() {
        settings.getBrowseDeleteOnCleanup() { deleteOnCleanup ->
        settings.getBrowseCleanupAge() { maxAgeString ->
        settings.getBrowseCleanupAgeArchive() { archiveMaxAgeString ->
        fileHandlerProvider.get() { fileHandler ->
            threadWithUILock("clean up puzzles") {
                val maxAge = getMaxAge(maxAgeString)
                val archiveMaxAge = getMaxAge(archiveMaxAgeString)

                val crosswords = fileHandler.getCrosswordsDirectory()
                val archive = fileHandler.getArchiveDirectory()

                val toArchive = ArrayList<PuzMetaFile>()
                val toDelete = ArrayList<PuzMetaFile>()

                fileHandler.getPuzMetas(crosswords).forEach { pm ->
                    val doClean
                        = pm.getComplete() == 100
                        || (maxAge != null && pm.getDate().isBefore(maxAge))

                    if (doClean) {
                        if (deleteOnCleanup) {
                            toDelete.add(pm)
                        } else {
                            toArchive.add(pm)
                        }
                    }
                }

                if (archiveMaxAge != null) {
                   fileHandler.getPuzMetas(archive).forEach { pm ->
                        if (pm.getDate().isBefore(archiveMaxAge)) {
                            toDelete.add(pm)
                        }
                    }
                }

                toDelete.forEach { fileHandler.delete(it) }
                toArchive.forEach { fileHandler.moveTo(it, archive) }

                handler.post() { startLoadFiles() }
            }
        }}}}
    }

    @MainThread
    fun download(date : LocalDate, downloaders : List<Downloader>) {
        downloadersProvider.get() { dls ->
            downloadExecutorService.execute() {
                dls.download(date, downloaders)
                if (!isViewArchive)
                    handler.post(this::startLoadFiles)
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

        settings.getDownloadersSettings() { downloaderSettings ->
        downloadersProvider.get() { dls ->
            downloadExecutorService.execute() {
                // Should not run in parallel with other auto downloads.
                // If multiple calls are made rapidly, the first will
                // lock the block below and update lastDL so future
                // blocks don't redownload. Could have used a separate
                // single-thread autodownload executor instead, but this
                // avoids an extra thread and field variable.
                synchronized (this@BrowseActivityViewModel) {
                    val lastDL = settings.getBrowseLastDownloadSync()
                    val downloadCutoff = System.currentTimeMillis()
                        - (12 * 60 * 60 * 1000)

                    val isDownload
                        = downloaderSettings.downloadOnStartUp
                        && downloadCutoff > lastDL

                    if (isDownload) {
                        settings.setBrowseLastDownload(
                            System.currentTimeMillis()
                        )
                        handler.post() {
                            download(LocalDate.now(), dls.getAutoDownloaders())
                        }
                    }
                }
            }
        }}
    }

    @MainThread
    fun loadPuzzle(puzMeta : PuzMetaFile) {
        currentPuzzleHolder.loadPuzzle(
            puzMeta,
            { puzzleLoadEvents.call() },
            { e ->
                Log.e(TAG, "Could not load file: " + e)

                fileHandlerProvider.get() { fileHandler ->
                    var filename : String? = null
                    try {
                        filename = fileHandler.getName(puzMeta.getPuzHandle())
                    } catch (ee : Exception) {
                        ee.printStackTrace()
                    }

                    // TODO: don't toast from view model
                    val application : Application = getApplication()
                    Toast.makeText(
                        application,
                        application.getString(
                            R.string.unable_to_read_file,
                            (filename ?: "")
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
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
                val curHandle = currentPuzzleHolder.puzHandle
                // TODO: should be a puzzle files update when Compose
                puzzleList.forEach { pm ->
                    if (pm.value?.isSameMainFile(curHandle) ?: false) {
                        pm.postValue(refreshedMeta)
                    }
                }
            }
        }
    }

    /**
     * Import files from uri to crosswords folder
     *
     * callback when finished, arguments are someFailed and someSucceeded
     */
    @MainThread
    fun importURIs(
        uris : Collection<Uri>,
        forceReload : Boolean,
        callback : BiConsumer<Boolean, Boolean>,
    ) {
        fileHandlerProvider.get() { fileHandler ->
        settings.getDownloadTimeout() { timeout ->
            threadWithUILock("import URIs") {
                var someFailed = false
                var someSucceeded = false
                var needsReload = forceReload

                uris.forEach { uri ->
                    uri?.let { uri ->
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
                                addPuzzleToList(pm)
                            } catch (e : IOException) {
                                // fall back to full reload
                                needsReload = true
                            }
                        }
                    }
                }

                if (needsReload)
                    handler.post(this::startLoadFiles)

                val finalSomeFailed = someFailed
                val finalSomeSucceeded = someSucceeded
                handler.post() {
                    callback.accept(finalSomeFailed, finalSomeSucceeded)
                }
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
        fileHandlerProvider.get() { fileHandler ->
            threadWithUILock("process to import dir") {
                val imported = PuzzleImporter.processToImportDirectory(
                    getApplication(),
                    fileHandler,
                    utils,
                )

                imported.forEach { ph ->
                    try {
                        val pm = fileHandler.loadPuzMetaFile(ph)
                        addPuzzleToList(pm)
                    } catch (e : IOException) {
                        Log.w(TAG, "Could not load meta for handle " + ph)
                    }
                }
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
        executorService.execute() {
            try {
                setUIBusy(true)
                r.run()
            } finally {
                setUIBusy(false)
                Log.i(TAG, "Finished task " + taskName + " with isUIBusy")
            }
        }
    }

    /**
     * Don't add the same file twice!
     */
    private fun addPuzzleToList(puzMeta : PuzMetaFile) {
        puzzleList.add(MutableLiveData<PuzMetaFile>(puzMeta))
        updateState(isNewPuzzleList = true)
    }

    /**
     * Assumes files only appear once in list
     */
    private fun removeFromPuzzleList(delPuzMeta : PuzMetaFile) {
        var index = 0
        var delIndex = -1

        while (index < puzzleList.size && delIndex < 0) {
            puzzleList.get(index).value?.let { pm ->
                if (pm.isSameMainFile(delPuzMeta))
                    delIndex = index
                index += 1
            }
        }

        // TODO: should be updating state when Compose
        if (delIndex >= 0)
            puzzleList.removeAt(delIndex).postValue(null)
    }

    private fun metaMatches(
        pmLiveData : MutableLiveData<PuzMetaFile>,
        filterText : String?,
    ) : Boolean {
        val pm = pmLiveData.value

        if (pm == null)
            return false

        if (filterText == null)
            return true

        val upperFilterText = filterText.uppercase()

        if (pm.caption.uppercase().contains(upperFilterText))
            return true

        if (pm.title.uppercase().contains(upperFilterText))
            return true

        if (pm.author.uppercase().contains(upperFilterText))
            return true

        if (pm.source.uppercase().contains(upperFilterText))
            return true

        if (
            MATCHER_DATE_FORMAT.format(pm.date)
                .uppercase()
                .contains(upperFilterText)
        )
            return true

        return false
    }

    private fun updateState(
        isArchive : Boolean = isViewArchive,
        isNewPuzzleList : Boolean = false,
    ) : UIState {
        val state = uiState.value ?: UIState()
        val puzzleFilter = puzzleFilter.value ?: ""
        var newPuzzleList : List<LiveData<PuzMetaFile>> = state.puzzleList

        val newFilter = state.puzzleFilter != puzzleFilter
        if (isNewPuzzleList || newFilter) {
            newPuzzleList = puzzleList.filter {
                metaMatches(it, puzzleFilter)
            }
        }

        val newSortOrder = settings.liveBrowseSort.value ?: state.sortOrder
        val isNewSort = state.sortOrder != newSortOrder
        if (isNewPuzzleList || isNewSort) {
            newPuzzleList = newPuzzleList.sortedWith() { pm1, pm2 ->
                newSortOrder.compare(pm1.value, pm2.value)
            }
        }

        val showPercentageCorrect
            = settings.liveBrowseShowPercentageCorrect.value ?: false
        val showRatings = settings.liveRatingsSettings.value?.let { rs ->
            !rs.disableRatings
                && (rs.browseAlwaysShowRating || isViewArchive)
            } ?: false
        val indicateIfSolution
            = settings.liveBrowseIndicateIfSolution.value
                ?: state.indicateIfSolution

        return state.copy(
            isViewArchive = isArchive,
            puzzleFilter = puzzleFilter,
            sortOrder = newSortOrder,
            puzzleList = newPuzzleList,
            disableSwipe = settings.liveBrowseDisableSwipe.value ?: true,
            showPercentageCorrect = showPercentageCorrect,
            showRatings = showRatings,
            indicateIfSolution = indicateIfSolution,
        )
    }

    private fun setUIBusy(value : Boolean) {
        _uiState.postValue(uiState.value?.copy(isBusy = value))
    }
}
