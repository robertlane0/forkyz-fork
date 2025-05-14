
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
import androidx.lifecycle.distinctUntilChanged

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.net.Downloader
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.FilteredList
import app.crossword.yourealwaysbe.forkyz.util.PuzzleImporter
import app.crossword.yourealwaysbe.forkyz.util.SingleLiveEvent
import app.crossword.yourealwaysbe.forkyz.util.files.DirHandle
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.util.files.PuzMetaFile
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

private val TAG = "ForkyzBrowseActivity"
private val MATCHER_DATE_FORMAT
    = DateTimeFormatter.ofPattern("EEEE dd MMM yyyy")

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

    var isViewArchive = false
        private set

    val puzzleFilter = MutableLiveData<String>()

    // busy with something that isn't downloading
    private val _isUIBusy = MutableLiveData<Boolean>(false)
    val isUIBusy : LiveData<Boolean> = _isUIBusy
    val puzzleLoadEvents = SingleLiveEvent<Void>()

    /**
     * Get list of puzzle files in currently viewed directory
     *
     * List of mutable live data. Each live data is for one puzzle --
     * the base puzzle may change, but it might get updated with new
     * meta data (e.g. new % completed). If it gets set to null, it
     * means the puzzle was removed from the puzzle list.
     *
     * The puzzle list might have items added to it, a new value will be
     * posted, containing the same list with the new item at the end.
     */
    private val _puzzleFiles
        = MediatorLiveData<FilteredList<MutableLiveData<PuzMetaFile>>>()
    val puzzleFiles : LiveData<out FilteredList<out LiveData<PuzMetaFile>>>
        = _puzzleFiles
    init {
        _puzzleFiles.addSource(
            settings.liveBrowseNewPuzzle.distinctUntilChanged(),
            { newPuzzle ->
                // first load, or if new puzzles
                if (
                    puzzleFiles.value == null
                    || ((newPuzzle ?: false) && !isViewArchive)
                ) {
                    startLoadFiles()
                    if (newPuzzle)
                        settings.setBrowseNewPuzzle(false)
                }
            },
        )
        _puzzleFiles.addSource(
            puzzleFilter,
            this::setNewPuzzleFilter
        )
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

                val puzFiles = FilteredList<MutableLiveData<PuzMetaFile>>()

                fileHandler.getPuzMetas(directory).forEach { pm ->
                    puzFiles.add(MutableLiveData<PuzMetaFile>(pm))
                }

                // use handler for this so viewArchive changes when
                // puzzleFiles does
                handler.post() {
                    isViewArchive = archive
                    setNewPuzzleList(puzFiles)
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
        _isUIBusy.value = true
        currentPuzzleHolder.getFreshCurrentPuzzleMeta() { refreshedMeta ->
            _isUIBusy.value = false
            Log.i(TAG, "Done refreshing current puzzle in list with isUIBusy")
            if (refreshedMeta != null) {
                val curHandle = currentPuzzleHolder.puzHandle
                _puzzleFiles.value?.let { pmList ->
                    var index = -1
                    pmList.forEach { pm ->
                        index += 1
                        if (pm.value?.isSameMainFile(curHandle) ?: false) {
                            pm.postValue(refreshedMeta)
                        }
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
                _isUIBusy.postValue(true)
                r.run()
            } finally {
                _isUIBusy.postValue(false)
                Log.i(TAG, "Finished task " + taskName + " with isUIBusy")
            }
        }
    }

    /**
     * Don't add the same file twice!
     */
    private fun addPuzzleToList(puzMeta : PuzMetaFile) {
        _puzzleFiles.value?.let { files ->
            files.add(MutableLiveData<PuzMetaFile>(puzMeta))
            _puzzleFiles.postValue(files)
        }
    }

    private fun setNewPuzzleList(
        newList : FilteredList<MutableLiveData<PuzMetaFile>>,
    ) {
        setNewPuzzleListAndFilter(newList, puzzleFilter.value)
    }

    private fun setNewPuzzleFilter(filterValue : String?) {
        setNewPuzzleListAndFilter(_puzzleFiles.value, filterValue)
    }

    private fun setNewPuzzleListAndFilter(
        newList : FilteredList<MutableLiveData<PuzMetaFile>>?,
        newFilter : String?
    ) {
        if (newList != null) {
            if (newFilter == null || newFilter.length == 0)
                newList.clearFilter()
            else
                newList.applyFilter() { meta -> metaMatches(meta, newFilter) }
        }
        _puzzleFiles.value = newList
    }

    /**
     * Assumes files only appear once in list
     */
    private fun removeFromPuzzleList(delPuzMeta : PuzMetaFile) {
        _puzzleFiles.value?.let { puzList ->
            var index = 0
            var delIndex = -1

            while (index < puzList.size && delIndex < 0) {
                puzList.get(index).value?.let { pm ->
                    if (pm.isSameMainFile(delPuzMeta))
                        delIndex = index
                    index += 1
                }
            }

            if (delIndex >= 0)
                puzList.removeAt(delIndex).postValue(null)
        }
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
}
