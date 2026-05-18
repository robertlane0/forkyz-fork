
package app.crossword.yourealwaysbe.forkyz.util.files

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.LocalDate
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.IO
import app.crossword.yourealwaysbe.forkyz.puz.io.IPuzIO
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache.MetaRecord
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle.IPuz
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle.VisitorIO

/**
 * Abstraction layer for file operations
 *
 * Implementations provided for different file backends
 */
abstract class FileHandler {
    // to avoid concurrent writes to meta cache
    val metaCacheMutex = Mutex()
    // to avoid concurrent access to filesystem (e.g. reading a file being
    // written to)
    val ioMutex = Mutex()

    protected val nativeBackendUtils : NativeBackendUtils
    private val metaCache : MetaCache

    protected constructor(utils : NativeBackendUtils) {
        this.nativeBackendUtils = utils
        this.metaCache = utils.getMetaCache()
    }

    /**
     * For testing allow a meta cache to be passed in
     */
    protected constructor(utils : NativeBackendUtils, metaCache : MetaCache) {
        this.nativeBackendUtils = utils
        this.metaCache = metaCache
    }

    abstract val crosswordsDirectory : DirHandle
    abstract val archiveDirectory : DirHandle
    abstract val toImportDirectory : DirHandle
    abstract val toImportDoneDirectory : DirHandle
    abstract val toImportFailedDirectory : DirHandle
    abstract val isStorageMounted : Boolean
    abstract val isStorageFull : Boolean
    abstract val settings : FileHandlerSettings

    abstract fun listFiles(dir : DirHandle) : Iterable<FileHandle>
    abstract fun getName(f : FileHandle) : String

    protected abstract fun exists(dir : DirHandle) : Boolean
    protected abstract fun exists(file : FileHandle) : Boolean
    abstract fun getUri(d : DirHandle) : Uri
    abstract fun getUri(f : FileHandle) : Uri
    protected abstract fun getLastModified(file : FileHandle) : Long

    protected abstract fun getFileHandle(uri : Uri) : FileHandle?

    /**
     * Get output stream to file, erasing previous contents
     */
    @Throws(IOException::class)
    abstract fun getOutput(fileHandle : FileHandle) : RawSink

    @Throws(IOException::class)
    abstract fun getInput(fileHandle : FileHandle) : RawSource

    /**
     * Create a new file in the directory with the given display name
     *
     * Do not save puzzles using these file handles, instead go via
     * save(Puzzle, DirHandle, String) that will use the preferred
     * backend file format for puzzles.
     *
     * Default visibility for StorageExporter to use.
     *
     * Return null if could not be created. E.g. if the file already
     * exists.
     */
    abstract fun createFileHandle(
        dir : DirHandle,
        fileName : String,
        mimeType : String,
    ) : FileHandle?

    /**
     * Provide a moveTo implementation
     *
     * Assume in a synced class
     */
    protected abstract fun moveToUnsync(
        fileHandle : FileHandle,
        srcDirHandle : DirHandle,
        destDirHandle : DirHandle,
    )

    /**
     * Provide a delete implementation
     *
     * Assume in a synced class
     */
    protected abstract fun deleteUnsync(fileHandle : FileHandle)

    fun getUri(puzHandle : PuzHandle) : Uri {
        return getUri(puzHandle.mainFileHandle)
    }

    fun getName(puzHandle : PuzHandle) : String {
        return getName(puzHandle.mainFileHandle)
    }

    suspend fun exists(ph : PuzHandle) : Boolean {
        if (!exists(ph.mainFileHandle))
            return false

        return ph.accept<Boolean>(
            object : PuzHandle.Visitor<Boolean> {
                override suspend fun visit(
                    puzHandle : PuzHandle.Puz,
                ) : Boolean {
                    return puzHandle.metaFileHandle?.let { metaHandle ->
                        exists(metaHandle)
                    } ?: true
                }

                override suspend fun visit(ipuzHandle : IPuz) : Boolean {
                    return true
                }
            }
        )
    }

    suspend fun delete(ph : PuzHandle) {
        withIOMutex {
            delete(ph.mainFileHandle)

            ph.accept<Unit>(
                object : PuzHandle.Visitor<Unit> {
                    override suspend fun visit(
                        puzHandle : PuzHandle.Puz,
                    ) : Unit {
                        puzHandle.metaFileHandle?.let { metaHandle ->
                            delete(metaHandle)
                        }
                    }

                    override suspend fun visit(
                        ipuzHandle : PuzHandle.IPuz,
                    ) : Unit {
                        // nothing to do
                    }
                }
            )

            withMetaCacheMutex {
                metaCache.deleteRecord(this@FileHandler, ph)
            }
        }
    }

    suspend fun moveTo(
        ph : PuzHandle,
        destDirHandle : DirHandle,
    ) {
        withIOMutex {
            val srcDirHandle = ph.dirHandle

            moveToUnsync(ph.mainFileHandle, srcDirHandle, destDirHandle)

            ph.setDirectory(destDirHandle)

            ph.accept<Unit>(
                object : PuzHandle.Visitor<Unit> {
                    override suspend fun visit(
                        puzHandle : PuzHandle.Puz,
                    ) : Unit {
                        val metaHandle = puzHandle.metaFileHandle
                        if (metaHandle != null) {
                            moveToUnsync(
                                metaHandle,
                                srcDirHandle,
                                destDirHandle,
                            )
                        }
                    }

                    override suspend fun visit(ipuzHandle : IPuz) : Unit {
                        // do nothing
                    }
                }
            )

            withMetaCacheMutex {
                metaCache.deleteRecord(this@FileHandler, ph)
            }
        }
    }

    /**
     * Move a file to a different directory
     *
     * @param fileHandle file to move
     * @param srcDirHandle the directory containing the file to move
     * @param destDirHandle the destination directory
     */
    suspend fun moveTo(
        fileHandle : FileHandle,
        srcDirHandle : DirHandle,
        destDirHandle : DirHandle,
    ) {
        withIOMutex {
            moveToUnsync(fileHandle, srcDirHandle, destDirHandle)
        }
    }

    /**
     * Get puz files in directory, will create meta files when missing
     */
    suspend fun getPuzMetas(dirHandle : DirHandle) : List<PuzMetaFile> {
        val metas = mutableListOf<PuzMetaFile>()

        val rawFileList = listFiles(dirHandle)
        val cachedMetas = withMetaCacheMutex {
            metaCache.getDirCache(this@FileHandler, dirHandle)
        }

        loadMetasFromPuzFiles(dirHandle, rawFileList, cachedMetas, metas)
        loadMetasFromIPuzFiles(dirHandle, rawFileList, cachedMetas, metas)

        withMetaCacheMutex {
            metaCache.cleanupCache(this@FileHandler, dirHandle, metas)
        }

        return metas
    }

    /**
     * The set of puzzle names stored by Forkyz
     *
     * File names are names without the file extension or directories
     */
    fun getPuzzleNames() : Set<String> {
        val puzzleNames = mutableSetOf<String>()
        for (fh in listFiles(crosswordsDirectory)) {
            getPuzzleFileName(getName(fh))?.let { puzzleName ->
                puzzleNames.add(puzzleName)
            }
        }
        for (fh in listFiles(archiveDirectory)) {
            getPuzzleFileName(getName(fh))?.let { puzzleName ->
                puzzleNames.add(puzzleName)
            }
        }
        return puzzleNames
    }

    /**
     * Synchronized to avoid reading/writing from the same file at the same
     * time.
     *
     * @return null if could not be loaded
     */
    @Throws(IOException::class,CancellationException::class)
    suspend fun loadPuzMetaFile(puzHandle : PuzHandle) : PuzMetaFile? {
        // both load and getRefreshedPuzMetaFile acquire mutex
        return load(puzHandle)?.let { puz ->
            getRefreshedPuzMetaFile(puzHandle, puz)
        }
    }

    /**
     * Get the meta record for the given puzzle
     */
    suspend fun getRefreshedPuzMetaFile(
        puzHandle : PuzHandle,
        puz : Puzzle,
    ) : PuzMetaFile {
        return withIOMutex {
            val metaRecord = withMetaCacheMutex {
                metaCache.addRecord(this@FileHandler, puzHandle, puz)
            }
            PuzMetaFile(this, puzHandle, metaRecord)
        }
    }

    /**
     * Loads puzzle with meta
     *
     * If the meta file of puz handle is null, loads without meta
     *
     * Synchronized to avoid reading/writing from the same file at the same
     * time.
     */
    @Throws(IOException::class,CancellationException::class)
    suspend fun load(ph : PuzHandle) : Puzzle? {
        return withIOMutex {
            val puz = ph.accept<Puzzle?>(
                object : VisitorIO<Puzzle?> {
                    @Throws(IOException::class,CancellationException::class)
                    override suspend fun visit(
                        puzHandle : PuzHandle.Puz,
                    ) : Puzzle? {
                        return load(puzHandle)
                    }

                    @Throws(IOException::class,CancellationException::class)
                    override suspend fun visit(
                        ipuzHandle : IPuz,
                    ) : Puzzle? {
                        return load(ipuzHandle)
                    }
                }
            )

            if (puz != null) {
                val mr = withMetaCacheMutex {
                    metaCache.addRecord(this@FileHandler, ph, puz)
                }
                // if the puzzle doesn't have its own date, use the detected
                // one
                if (puz.date == null)
                    puz.date = mr.date
            }

            puz
        }
    }

    /** Save puzzle and meta data
     *
     * If puzHandle's meta handle is null, a new meta file will be created and
     * puzHandle is updated with the new meta file handle
     */
    @Throws(IOException::class,CancellationException::class)
    suspend fun save(puz : Puzzle, ph : PuzHandle) {
        withIOMutex {
            val success = ph.accept<Boolean>(
                object : VisitorIO<Boolean> {
                    @Throws(IOException::class,CancellationException::class)
                    override suspend fun visit(
                        puzHandle : PuzHandle.Puz,
                    ) : Boolean {
                        return save(puz, puzHandle)
                    }

                    @Throws(IOException::class,CancellationException::class)
                    override suspend fun visit(ipuzHandle : IPuz) : Boolean {
                        return save(puz, ipuzHandle)
                    }
                }
            )

            // Cannot be done on main thread
            if (success) {
                withMetaCacheMutex {
                    metaCache.addRecord(this@FileHandler, ph, puz)
                }
            }
        }
    }

    /**
     * Save a (new) puzzle to the given directory
     *
     * Use this instead of createFile to save puzzles -- let the file
     * handler decide the backend file format.
     *
     * @param puz the puzzle to save
     * @param dirHandle the directory to save under
     * @param fileNameBody the name to give the file without file
     * extension
     * @return new puzzle handle if saved success
     */
    @Throws(IOException::class,CancellationException::class)
    suspend fun saveNewPuzzle(
        puz : Puzzle,
        fileNameBody : String,
    ) : PuzHandle? {
        val dirHandle = this.crosswordsDirectory

        val mainFile = createFileHandle(
            dirHandle,
            fileNameBody + FILE_EXT_IPUZ,
            MIME_TYPE_IPUZ,
        )

        if (mainFile == null) {
            return null
        } else {
            try {
                val ph : PuzHandle = IPuz(dirHandle, mainFile)
                save(puz, ph)
                return ph
            } catch (e : Exception) {
                delete(mainFile)
                throw e
            }
        }
    }

    @Throws(IOException::class)
    fun getBufferedInput(fileHandle : FileHandle) : Source {
        return getInput(fileHandle).buffered()
    }

    fun getModifiedDate(file : FileHandle) : LocalDate {
        return TimeUtils.fromEpochMilliseconds(getLastModified(file))
    }

    @Throws(IOException::class)
    fun getBufferedOutput(
        fileHandle : FileHandle,
    ) : Sink {
        return getOutput(fileHandle).buffered()
    }

    protected suspend fun delete(fileHandle : FileHandle) {
        deleteUnsync(fileHandle)
    }

    protected fun getMetaFileName(puzFile : FileHandle) : String {
        val name = getName(puzFile)
        return name.substring(0, name.lastIndexOf(".")) + FILE_EXT_FORKYZ
    }

    /**
     * Extract puzzle file name from fileName
     *
     * Checks whether the file name is a backend puzzle file (by
     * extension), and returns the name without the extension if so.
     *
     * @return puzzle file name or null if not a puzzle file
     */
    private fun getPuzzleFileName(fileName : String) : String? {
        for (storedExt in FORKYZ_STORED_PUZZLE_EXTS) {
            if (fileName.endsWith(storedExt)) {
                return fileName.substring(
                    0,
                    fileName.length - storedExt.length,
                )
            }
        }
        return null
    }

    @Throws(IOException::class,CancellationException::class)
    private suspend fun load(ph : PuzHandle.Puz) : Puzzle? {
        ph.metaFileHandle?.let { metaFile ->
            try {
                getBufferedInput(ph.mainFileHandle).use { pis ->
                    getBufferedInput(metaFile).use { mis ->
                        return IO.load(pis, mis)
                    }
                }
            } catch (e : IOException) {
                AppLogger.w(
                    TAG,
                    "Could not load "
                        + ph.mainFileHandle.uri
                        + ". Trying again without metadata.",
                )
                // fall through to try without meta
            }
        }

        getBufferedInput(ph.mainFileHandle).use { fis ->
            return IO.loadNative(fis)
        }
    }

    @Throws(IOException::class,CancellationException::class)
    private suspend fun load(ph : IPuz) : Puzzle? {
        return getBufferedInput(ph.mainFileHandle).use { input ->
            IPuzIO.readPuzzle(input)
        }
    }

    @Throws(IOException::class,CancellationException::class)
    private suspend fun save(puz : Puzzle, ph : PuzHandle.Puz) : Boolean {
        val puzFile = ph.mainFileHandle
        var metaFile = ph.metaFileHandle
        val puzDir = ph.dirHandle

        var metaCreated = false

        if (metaFile == null) {
            val metaName = getMetaFileName(puzFile)
            metaFile = createFileHandle(
                ph.dirHandle,
                metaName,
                MIME_TYPE_META,
            )
            if (metaFile == null)
                throw IOException("Could not create meta file")
            metaCreated = true
        }

        // Write to buffers first so that we don't erase old data in
        // case of a problem encoding puzzle.
        val bufPuz = Buffer()
        val bufMeta = Buffer()

        IO.save(puz, bufPuz, bufMeta)

        // Write meta first, if doesn't throw, write main puzzle
        var metaSuccess = false

        try {
            getBufferedOutput(metaFile).use { meta ->
                bufMeta.transferTo(meta)
                metaSuccess = true
            }
        } finally {
            if (!metaSuccess && metaCreated)
                delete(metaFile)
        }

        var puzSuccess = false

        try {
            getBufferedOutput(puzFile).use { puzzle ->
                bufPuz.transferTo(puzzle)
                puzSuccess = true
            }
        } finally {
            if (!puzSuccess && metaCreated)
                delete(metaFile)
            else
                ph.metaFileHandle = metaFile
        }

        return true
    }

    @Throws(IOException::class,CancellationException::class)
    private suspend fun save(puz : Puzzle, ph : IPuz) : Boolean {
        val ipuzFile = ph.mainFileHandle
        val puzDir = ph.dirHandle

        // Write to buffer first -- get puzzle written to a stream
        // before truncating the previously saved file
        val buffer = Buffer()
        IPuzIO.writePuzzle(puz, buffer)

        return getBufferedOutput(ipuzFile).use { output ->
            buffer.transferTo(output)
            true
        }
    }

    /**
     * Load .puz/.forkyz file data into the loadedPuzMetas array
     *
     * @param dirHandle the directory the files are in
     * @param files the list of files in the directory
     * @param cachedMetas cached meta data for the file URIs
     * @param loadedPuzMetas the list into which to store the metas
     */
    private suspend fun loadMetasFromPuzFiles(
        dirHandle : DirHandle,
        files : Iterable<FileHandle>,
        cachedMetas : Map<Uri, MetaRecord>,
        loadedPuzMetas : MutableList<PuzMetaFile>,
    ) {
        // Load files into data structures to avoid repeated interaction
        // with filesystem (which is good for content resolver)
        val puzFiles = mutableSetOf<FileHandle>()
        val metaFiles = mutableMapOf<String, FileHandle>()

        for (f in files) {
            val fileName = getName(f)
            if (fileName.endsWith(FILE_EXT_PUZ)) {
                puzFiles.add(f)
            } else if (fileName.endsWith(FILE_EXT_FORKYZ)) {
                metaFiles.put(fileName, f)
            } else {
                // ignore
            }
        }

        for (puzFile in puzFiles) {
            val metaName = getMetaFileName(puzFile)
            var metaFile : FileHandle? = null

            if (metaFiles.containsKey(metaName)) {
                metaFile = metaFiles.get(metaName)
            }

            val ph : PuzHandle = PuzHandle.Puz(dirHandle, puzFile, metaFile)

            val pm = getPuzMetaFile(ph, cachedMetas)
            if (pm != null)
                loadedPuzMetas.add(pm)
        }
    }

    /**
     * Load .ipuz file data into the loadedPuzMetas array
     *
     * @param dirHandle the directory the files are in
     * @param files the list of files in the directory
     * @param cachedMetas cached meta data for the file URIs
     * @param loadedPuzMetas the list into which to store the metas
     */
    private suspend fun loadMetasFromIPuzFiles(
        dirHandle : DirHandle,
        files : Iterable<FileHandle>,
        cachedMetas : Map<Uri, MetaRecord>,
        loadedPuzMetas : MutableList<PuzMetaFile>
    ) {
        for (f in files) {
            val fileName = getName(f)
            if (fileName.endsWith(FILE_EXT_IPUZ)) {
                val ph : PuzHandle = IPuz(dirHandle, f)
                getPuzMetaFile(ph, cachedMetas)?.let { pm ->
                    loadedPuzMetas.add(pm)
                }
            }
        }
    }

    /**
     * Get the PuzMetaFile from the handle
     *
     * Uses cache if possible, else loads data from file
     */
    private suspend fun getPuzMetaFile(
        ph : PuzHandle,
        cachedMetas : Map<Uri, MetaRecord>,
    ) : PuzMetaFile? {
        val puzFileUri = getUri(ph)
        val metaRecord = cachedMetas.get(puzFileUri)

        if (metaRecord != null) {
            return PuzMetaFile(this, ph, metaRecord)
        } else {
            try {
                return loadPuzMetaFile(ph)
            } catch (e : IOException) {
                AppLogger.w(TAG, "Could not load puz meta for " + ph + " : " + e)
                return null
            }
        }
    }

    private suspend fun <T> withMetaCacheMutex(run : suspend () -> T) : T {
        return metaCacheMutex.withLock { run() }
    }

    private suspend fun <T> withIOMutex(run : suspend () -> T) : T {
        return ioMutex.withLock { run() }
    }

    companion object {
        private const val TAG = "ForkyzFileHandler"

        // private for now because downloaders shouldn't be directly
        // creating puzzle files but instead saving Puzzle objects with
        // names
        private const val MIME_TYPE_PUZ = "application/x-crossword"
        private const val MIME_TYPE_META = "application/octet-stream"

        // Android messes with application/json by adding .json extension  :(
        const val MIME_TYPE_IPUZ : String = "application/octet-stream"

        const val MIME_TYPE_PLAIN_TEXT : String = "text/plain"
        const val MIME_TYPE_GENERIC : String = "application/octet-stream"
        const val MIME_TYPE_GENERIC_XML : String = "text/xml"

        const val FILE_EXT_PUZ : String = ".puz"
        const val FILE_EXT_FORKYZ : String = ".forkyz"
        const val FILE_EXT_IPUZ : String = ".ipuz"

        // the file extensions forkyz supports as backend storage
        val FORKYZ_STORED_PUZZLE_EXTS : Array<String> = arrayOf<String>(
            FILE_EXT_PUZ, FILE_EXT_IPUZ
        )

        val MINIMUM_STORAGE_REQUIRED : Long = 1024L * 1024L
    }
}

