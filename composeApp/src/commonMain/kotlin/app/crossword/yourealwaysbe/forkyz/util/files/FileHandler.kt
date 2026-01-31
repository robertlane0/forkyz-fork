
package app.crossword.yourealwaysbe.forkyz.util.files

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.runBlocking

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache.MetaRecord
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle.IPuz
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle.VisitorIO
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.IO
import app.crossword.yourealwaysbe.puz.io.IPuzIO
import app.crossword.yourealwaysbe.puz.io.StreamUtils

/**
 * Abstraction layer for file operations
 *
 * Implementations provided for different file backends
 *
 * TODO: currently uses runBlocking for MetaCache suspend funs based on
 * assumption that this is only called from contexts where IO blocking
 * is ok. A goal might be to move from executorService to coroutines and
 * make proper use of suspend funs (and locks to avoid races..).
 */
abstract class FileHandler {
    // used for saving meta cache to DB since we currently save puzzles
    // on the main thread (can be removed if/when a better save solution
    // is implemented)
    private val executorService : ExecutorService
        = Executors.newSingleThreadExecutor()

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
    abstract fun getOutputStream(fileHandle : FileHandle) : OutputStream

    @Throws(IOException::class)
    abstract fun getInputStream(fileHandle : FileHandle) : InputStream

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

    fun exists(ph : PuzHandle) : Boolean {
        if (!exists(ph.mainFileHandle))
            return false

        return ph.accept<Boolean>(
            object : PuzHandle.Visitor<Boolean> {
                override fun visit(puzHandle : PuzHandle.Puz) : Boolean {
                    return puzHandle.metaFileHandle?.let { metaHandle ->
                        exists(metaHandle)
                    } ?: true
                }

                override fun visit(ipuzHandle : IPuz) : Boolean {
                    return true
                }
            }
        )
    }

    @Synchronized
    fun delete(ph : PuzHandle) {
        delete(ph.mainFileHandle)

        ph.accept<Void?>(
            object : PuzHandle.Visitor<Void?> {
                override fun visit(puzHandle : PuzHandle.Puz) : Void? {
                    puzHandle.metaFileHandle?.let { metaHandle ->
                        delete(metaHandle)
                    }
                    return null
                }

                override fun visit(ipuzHandle : PuzHandle.IPuz) : Void? {
                    return null
                }
            }
        )

        runBlocking { metaCache.deleteRecord(this@FileHandler, ph) }
    }

    @Synchronized
    fun moveTo(
        ph : PuzHandle,
        destDirHandle : DirHandle,
    ) {
        val srcDirHandle = ph.dirHandle

        moveTo(ph.mainFileHandle, srcDirHandle, destDirHandle)

        ph.setDirectory(destDirHandle)

        ph.accept<Void?>(
            object : PuzHandle.Visitor<Void?> {
                override fun visit(puzHandle : PuzHandle.Puz) : Void? {
                    puzHandle.metaFileHandle?.let { metaHandle ->
                        moveTo(metaHandle, srcDirHandle, destDirHandle)
                    }
                    return null
                }

                override fun visit(ipuzHandle : IPuz) : Void? {
                    return null
                }
            }
        )

        // TODO : can we move record instead? What is new Uri?
        runBlocking { metaCache.deleteRecord(this@FileHandler, ph) }
    }

    /**
     * Move a file to a different directory
     *
     * @param fileHandle file to move
     * @param srcDirHandle the directory containing the file to move
     * @param destDirHandle the destination directory
     */
    @Synchronized
    fun moveTo(
        fileHandle : FileHandle,
        srcDirHandle : DirHandle,
        destDirHandle : DirHandle,
    ) {
        moveToUnsync(fileHandle, srcDirHandle, destDirHandle)
    }

    /**
     * Get puz files in directory, will create meta files when missing
     */
    fun getPuzMetas(dirHandle : DirHandle) : List<PuzMetaFile> {
        val metas = mutableListOf<PuzMetaFile>()

        val rawFileList = listFiles(dirHandle)
        val cachedMetas = runBlocking {
            metaCache.getDirCache(this@FileHandler, dirHandle)
        }

        loadMetasFromPuzFiles(dirHandle, rawFileList, cachedMetas, metas)
        loadMetasFromIPuzFiles(dirHandle, rawFileList, cachedMetas, metas)

        runBlocking {
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
    @Synchronized
    @Throws(IOException::class)
    fun loadPuzMetaFile(puzHandle : PuzHandle) : PuzMetaFile? {
        return load(puzHandle)?.let { puz ->
            getRefreshedPuzMetaFile(puzHandle, puz)
        }
    }

    /**
     * Get the meta record for the given puzzle
     */
    @Synchronized
    fun getRefreshedPuzMetaFile(
        puzHandle : PuzHandle,
        puz : Puzzle,
    ) : PuzMetaFile {
        val metaRecord = runBlocking {
            metaCache.addRecord(this@FileHandler, puzHandle, puz)
        }
        return PuzMetaFile(this, puzHandle, metaRecord)
    }

    /**
     * Loads puzzle with meta
     *
     * If the meta file of puz handle is null, loads without meta
     *
     * Synchronized to avoid reading/writing from the same file at the same
     * time.
     */
    @Synchronized
    @Throws(IOException::class)
    fun load(ph : PuzHandle) : Puzzle? {
        val puz = ph.accept<Puzzle?>(
            object : VisitorIO<Puzzle?> {
                @Throws(IOException::class)
                override fun visit(puzHandle : PuzHandle.Puz) : Puzzle? {
                    return load(puzHandle)
                }

                @Throws(IOException::class)
                override fun visit(ipuzHandle : IPuz) : Puzzle? {
                    return load(ipuzHandle)
                }
            }
        )

        if (puz != null) {
            val mr = runBlocking {
                metaCache.addRecord(this@FileHandler, ph, puz)
            }
            // if the puzzle doesn't have its own date, use the detected
            // one
            if (puz.getDate() == null)
                puz.setDate(mr.date)
        }

        return puz
    }

    /** Save puzzle and meta data
     *
     * If puzHandle's meta handle is null, a new meta file will be created and
     * puzHandle is updated with the new meta file handle
     *
     * Synchronized to avoid reading/writing from the same file at the same
     * time.
     */
    @Synchronized
    @Throws(IOException::class)
    fun save(puz : Puzzle, ph : PuzHandle) {
        val success = ph.accept<Boolean>(
            object : VisitorIO<Boolean> {
                @Throws(IOException::class)
                override fun visit(puzHandle : PuzHandle.Puz) : Boolean {
                    return save(puz, puzHandle)
                }

                @Throws(IOException::class)
                override fun visit(ipuzHandle : IPuz) : Boolean {
                    return save(puz, ipuzHandle)
                }
            }
        )

        // Cannot be done on main thread
        if (success) {
            executorService.execute {
                runBlocking {
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
    @Synchronized
    @Throws(IOException::class)
    fun saveNewPuzzle(
        puz : Puzzle,
        fileNameBody : String,
    ) : PuzHandle? {
        val dirHandle = this.crosswordsDirectory

        val mainFile = createFileHandle(
            dirHandle,
            fileNameBody + FILE_EXT_IPUZ,
            MIME_TYPE_IPUZ,
        )

        if (mainFile == null)
            return null

        try {
            val ph : PuzHandle = IPuz(dirHandle, mainFile)
            save(puz, ph)
            return ph
        } catch (e : Exception) {
            delete(mainFile)
            throw e
        }
    }

    @Throws(IOException::class)
    fun getBufferedInputStream(fileHandle : FileHandle) : BufferedInputStream {
        return BufferedInputStream(getInputStream(fileHandle))
    }

    fun getModifiedDate(file : FileHandle) : LocalDate {
        return Instant.ofEpochMilli(getLastModified(file))
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    @Throws(IOException::class)
    protected fun getBufferedOutputStream(
        fileHandle : FileHandle,
    ) : BufferedOutputStream {
        return BufferedOutputStream(getOutputStream(fileHandle))
    }

    @Synchronized
    protected fun delete(fileHandle : FileHandle) {
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

    @Synchronized
    @Throws(IOException::class)
    private fun load(ph : PuzHandle.Puz) : Puzzle? {
        ph.metaFileHandle?.let { metaFile ->
            try {
                DataInputStream(
                    getBufferedInputStream(ph.mainFileHandle)
                ).use { pis ->
                    DataInputStream(
                        getBufferedInputStream(metaFile)
                    ).use { mis ->
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

        DataInputStream(
            getBufferedInputStream(ph.mainFileHandle)
        ).use { fis ->
            return IO.loadNative(fis)
        }
    }

    @Synchronized
    @Throws(IOException::class)
    private fun load(ph : IPuz) : Puzzle? {
        getBufferedInputStream(ph.mainFileHandle).use { inputStream ->
            return IPuzIO.readPuzzle(inputStream)
        }
    }

    @Synchronized
    @Throws(IOException::class)
    private fun save(puz : Puzzle, ph : PuzHandle.Puz) : Boolean {
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
        val baosPuz = ByteArrayOutputStream()
        val baosMeta = ByteArrayOutputStream()

        DataOutputStream(baosPuz).use { dosPuz ->
            DataOutputStream(baosMeta).use { dosMeta ->
                IO.save(puz, dosPuz, dosMeta)
            }
        }
        // Write meta first, if doesn't throw, write main puzzle
        var metaSuccess = false

        try {
            ByteArrayInputStream(baosMeta.toByteArray()).use { baisMeta ->
                getBufferedOutputStream(metaFile).use { meta ->
                    StreamUtils.copyStream(baisMeta, meta)
                    metaSuccess = true
                }
            }
        } finally {
            if (!metaSuccess && metaCreated)
                delete(metaFile)
        }

        var puzSuccess = false

        try {
            ByteArrayInputStream(baosPuz.toByteArray()).use { baisPuz ->
                getBufferedOutputStream(puzFile).use { puzzle ->
                    StreamUtils.copyStream(baisPuz, puzzle)
                    puzSuccess = true
                }
            }
        } finally {
            if (!puzSuccess && metaCreated)
                delete(metaFile)
            else
                ph.metaFileHandle = metaFile
        }

        return true
    }

    @Synchronized
    @Throws(IOException::class)
    private fun save(puz : Puzzle, ph : IPuz) : Boolean {
        val ipuzFile = ph.mainFileHandle
        val puzDir = ph.dirHandle

        // Write to buffer first -- get puzzle written to a stream
        // before truncating the previously saved file
        val baos = ByteArrayOutputStream()
        IPuzIO.writePuzzle(puz, baos)

        ByteArrayInputStream(baos.toByteArray()).use { bais ->
            getBufferedOutputStream(ipuzFile).use { os ->
                StreamUtils.copyStream(bais, os)
                return true
            }
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
    private fun loadMetasFromPuzFiles(
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
    private fun loadMetasFromIPuzFiles(
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
    private fun getPuzMetaFile(
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

