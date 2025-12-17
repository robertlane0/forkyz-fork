
package app.crossword.yourealwaysbe.forkyz.util.files

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.provider.DocumentsContract
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toAndroidUri
import app.crossword.yourealwaysbe.forkyz.util.toForkyzUri
import app.crossword.yourealwaysbe.forkyz.util.toUri
import app.crossword.yourealwaysbe.puz.io.StreamUtils

import androidx.core.net.toUri as toAndroidUri

/**
 * Construct FileHandler from context and folder URIs
 *
 * Context should be an application context, not an activity that
 * may go out of date.
 *
 * @param rootUri the tree the user has granted permission to
 * @param crosswordsFolderUri the document uri for the crosswords
 * folder
 * @param archiveFolderUri the document uri for the archive folder
 */
class FileHandlerSAF(
    private val context : Context,
    utils : NativeBackendUtils,
    private val rootUri : Uri,
    private val crosswordsFolderUri : Uri,
    private val archiveFolderUri : Uri,
    private val toImportFolderUri : Uri,
    private val toImportDoneFolderUri : Uri,
    private val toImportFailedFolderUri : Uri
) : FileHandler(utils) {
    override val crosswordsDirectory : DirHandle
        get() { return DirHandle(crosswordsFolderUri) }

    override val archiveDirectory : DirHandle
        get() { return DirHandle(archiveFolderUri) }

    override val toImportDirectory : DirHandle
        get() { return DirHandle(toImportFolderUri) }

    override val toImportDoneDirectory : DirHandle
        get() { return DirHandle(toImportDoneFolderUri) }

    override val toImportFailedDirectory : DirHandle
        get() { return DirHandle(toImportFailedFolderUri) }

    /**
     * Return a settings object for the file handler
     */
    override val settings : FileHandlerSettings
        get() {
            return FileHandlerSettings(
                StorageLocation.SL_EXTERNAL_SAF,
                rootUri.toString(),
                crosswordsFolderUri.toString(),
                archiveFolderUri.toString(),
                toImportFolderUri.toString(),
                toImportDoneFolderUri.toString(),
                toImportFailedFolderUri.toString(),
            )
        }

    override fun listFiles(dir : DirHandle) : Iterable<FileHandle> {
        val resolver = this.contentResolver
        val dirUri = dir.uri
        val dirTreeId = DocumentsContract.getDocumentId(dirUri.toAndroidUri())
        val dirTreeUri = DocumentsContract.buildDocumentUriUsingTree(
            rootUri.toAndroidUri(),
            dirTreeId,
        )

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            dirTreeUri,
            dirTreeId,
        )

        val files = mutableListOf<FileHandle>()

        resolver.query(
            childrenUri,
            arrayOf<String>(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null, null, null
        ).use { cursor ->
            cursor?.let { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val name = cursor.getString(1)
                    val modified = cursor.getLong(2)
                    val mimeType = cursor.getString(3)

                    if (DocumentsContract.Document.MIME_TYPE_DIR != mimeType) {
                        val uri = DocumentsContract.buildDocumentUriUsingTree(
                            dirUri.toAndroidUri(),
                            id,
                        )

                        files.add(
                            FileHandle(
                                uri.toForkyzUri(),
                                Meta(name, modified, mimeType),
                            )
                        )
                    }
                }
            }
        }
        return files
    }

    override fun getName(f : FileHandle) : String {
        val meta = f.meta
        if (meta == null) {
            throw IllegalArgumentException(
                "Cannot get name of file handle "
                + f
                + " that does not have SAF meta data."
            )
        }
        return meta.name
    }

    override fun getFileHandle(uri : Uri) : FileHandle? {
        val meta = getMetaFromUri(uri)
        if (meta != null)
            return FileHandle(uri, meta)
        else
            return null
    }

    override fun exists(dir : DirHandle) : Boolean {
        return exists(contentResolver, dir.uri)
    }

    override fun exists(file : FileHandle) : Boolean {
        return exists(contentResolver, file.uri)
    }

    override fun getUri(d : DirHandle) : Uri {
        return d.uri
    }

    override fun getUri(f : FileHandle) : Uri {
        return f.uri
    }

    override fun getLastModified(file : FileHandle) : Long {
        val meta = file.meta
        if (meta == null) {
            throw IllegalArgumentException(
                "Cannot get last modified of file handle "
                + file
                + " that does not have SAF meta data."
            )
        }
        return meta.lastModified
    }

    override fun deleteUnsync(fileHandle : FileHandle) {
        try {
            DocumentsContract.deleteDocument(
                this.contentResolver,
                fileHandle.uri.toAndroidUri(),
            )
        } catch (e : FileNotFoundException) {
            // seems like our work is done
        } catch (e : IllegalArgumentException) {
            // if the file does not exist, this might be thrown since
            // Android cannot determine access permissions
            if (e.cause is FileNotFoundException) {
                // ignore
            } else {
                throw e
            }
        }
    }

    override fun moveToUnsync(
        fileHandle : FileHandle,
        srcDirHandle : DirHandle,
        destDirHandle : DirHandle,
    ) {
        // outside of android version utils but awkward to move
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            moveToUnsyncLegacy(fileHandle, srcDirHandle, destDirHandle)
        } else {
            try {
                DocumentsContract.moveDocument(
                    this.contentResolver,
                    fileHandle.uri.toAndroidUri(),
                    srcDirHandle.uri.toAndroidUri(),
                    destDirHandle.uri.toAndroidUri(),
                )
            } catch (e : FileNotFoundException) {
                AppLogger.e(
                    TAG,
                    "Attempt to move " + fileHandle + " to "
                        + destDirHandle + " failed.",
                )
                e.printStackTrace()
            } catch (e : IllegalArgumentException) {
                AppLogger.e(
                    TAG,
                    "Attempt to move " + fileHandle + " to "
                        + destDirHandle + " failed.",
                )
                e.printStackTrace()
            } catch (e : IllegalStateException) {
                // assume name clash and fall back to legacy manual copy
                // its createDocument will choose a new name if needed
                moveToUnsyncLegacy(fileHandle, srcDirHandle, destDirHandle)
            }
        }
    }

    private fun moveToUnsyncLegacy(
        fileHandle : FileHandle,
        srcDirHandle : DirHandle,
        destDirHandle : DirHandle,
    ) {
        try {
            DocumentsContract.createDocument(
                contentResolver,
                destDirHandle.uri.toAndroidUri(),
                getMimeType(fileHandle),
                getName(fileHandle),
            )?.let { newUri ->
                getBufferedInputStream(fileHandle).use { inputStream ->
                    getFileHandle(newUri.toForkyzUri())?.let { outHandle ->
                        getBufferedOutputStream(outHandle).use { outputStream ->
                            StreamUtils.copyStream(inputStream, outputStream)
                            DocumentsContract.deleteDocument(
                                contentResolver,
                                fileHandle.uri.toAndroidUri(),
                            )
                        }
                    }
                }
            }
        } catch (e : IllegalArgumentException) {
            AppLogger.e(
                TAG,
                "Attempt to move " + fileHandle + " to "
                    + destDirHandle + " failed.",
            )
            e.printStackTrace()
        } catch (e : IllegalStateException) {
            AppLogger.e(
                TAG,
                "Attempt to move " + fileHandle + " to "
                    + destDirHandle + " failed.",
            )
            e.printStackTrace()
        } catch (e : IOException) {
            AppLogger.e(
                TAG,
                "Attempt to move " + fileHandle + " to "
                    + destDirHandle + " failed.",
            )
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    override fun getOutputStream(fileHandle : FileHandle) : OutputStream {
        try {
            val os = contentResolver.openOutputStream(
                fileHandle.uri.toAndroidUri(),
                "wt",
            )
            if (os == null) {
                throw IOException(
                    "Could not get output stream for " + fileHandle,
                )
            }
            return os
        } catch (e : IllegalArgumentException) {
            // happens when e.g. file was deleted, so consider as IO
            throw IOException(e)
        }
    }

    @Throws(IOException::class)
    override fun getInputStream(fileHandle : FileHandle) : InputStream {
        try {
            val inStream = contentResolver.openInputStream(
                fileHandle.uri.toAndroidUri(),
            )
            if (inStream == null) {
                throw IOException(
                    "Could not get output stream for " + fileHandle,
                )
            }
            return inStream
        } catch (e : IllegalArgumentException) {
            // happens when e.g. file was deleted, so consider as IO
            throw IOException(e)
        }
    }

    override val isStorageMounted : Boolean
        get () {
            val resolver = contentResolver
            try {
                return exists(resolver, crosswordsFolderUri)
                    && exists(resolver, archiveFolderUri)
            } catch (e : UnsupportedOperationException) {
                AppLogger.e(TAG, "Unsupported operation accessing SAF")
                e.printStackTrace()
                return false
            }
        }

    override val isStorageFull : Boolean
        get() {
            // In general storage size doesn't make sense for SAF (e.g. size
            // of cloud storage). There is a method of getting space [1] for
            // local directories, but it delays start up by about 0.5s,
            // which is noticably laggy.
            //
            // [1] : https ://stackoverflow.com/a/40848958
            return false
        }

    override fun createFileHandle(
        dir : DirHandle,
        fileName : String,
        mimeType : String,
    ) : FileHandle? {
        try {
            val uri = DocumentsContract.createDocument(
                this.contentResolver,
                dir.uri.toAndroidUri(),
                mimeType,
                fileName,
            )?.toForkyzUri()
            if (uri != null) {
                return FileHandle(
                    uri,
                    Meta(fileName, System.currentTimeMillis(), mimeType)
                )
            }
        } catch (e : FileNotFoundException) {
            // fall through
        }

        return null
    }

    private fun getMetaFromUri(uri : Uri) : Meta? {
        this.contentResolver.query(
            uri.toAndroidUri(),
            arrayOf<String>(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null,
        ).use { c ->
            if (c == null)
                return null

            // avoid exception crash if last modified is not known
            // e.g. when opening firefox download urls
            if (c.getCount() > 0 && c.moveToFirst()) {
                return Meta(
                    c.getString(0),
                    getLongColumnWithDefault(c, 1, System.currentTimeMillis()),
                    c.getString(2),
                )
            } else {
                return null
            }
        }
    }

    private fun getLongColumnWithDefault(
        c : Cursor,
        columnIndex : Int,
        defaultValue : Long,
    ) : Long {
        try {
            return c.getLong(columnIndex)
        } catch (e : Throwable) {
            return defaultValue
        }
    }

    private val contentResolver : ContentResolver
        get() = context.getContentResolver()

    fun getMimeType(f : FileHandle) : String {
        val meta = f.meta
        if (meta == null) {
            throw IllegalArgumentException(
                "Cannot get mime type of file handle "
                + f
                + " that does not have SAF meta data."
            )
        }
        return meta.mimeType
    }

    companion object {
        private const val TAG = "ForkyzFileHandlerSAF"

        private const val ARCHIVE_NAME = "archive"
        private const val TO_IMPORT_NAME = "to-import"
        private const val TO_IMPORT_DONE_NAME = "to-import-done"
        private const val TO_IMPORT_FAILED_NAME = "to-import-failed"

        /**
         * Initialise a crosswords directory in the give rootUri
         *
         * This will search the contents of rootUri to see if directories
         * already exist (and use them if so). If not, it will create the
         * required folders. It will keep the permission for the Uri if all
         * is well.
         *
         * Once this has been called, the file handler will need saving to
         * settings for readHandlerFromSettings to work
         *
         * @param applicationContext the application context
         * @param utils native backend utils
         * @param rootUri the root permitted folder for storage
         * @param keepPermission whether to keep access permission to
         * rootUri
         * @return an initiated file handler if successful, else null
         */
        /**
         * Convenience for same with keeping permission
         */
        fun initialiseSAFForRoot(
            applicationContext : Context,
            utils : NativeBackendUtils,
            rootUri : Uri,
            keepPermission : Boolean = true,
        ) : FileHandlerSAF? {
            try {
                val resolver = applicationContext.getContentResolver()
                val dirId = DocumentsContract.getTreeDocumentId(
                    rootUri.toAndroidUri(),
                )

                val crosswordsFolderUri
                    = DocumentsContract.buildDocumentUriUsingTree(
                        rootUri.toAndroidUri(),
                        dirId,
                    )?.toForkyzUri()

                val subDirs = Companion.searchCreateDirectories(
                    arrayOf<String>(
                        ARCHIVE_NAME,
                        TO_IMPORT_NAME,
                        TO_IMPORT_DONE_NAME,
                        TO_IMPORT_FAILED_NAME
                    ),
                    rootUri,
                    resolver,
                )

                val archiveFolderUri = subDirs[0]
                val toImportFolderUri = subDirs[1]
                val toImportDoneFolderUri = subDirs[2]
                val toImportFailedFolderUri = subDirs[3]

                // if all ok, keep permission
                if (
                    crosswordsFolderUri != null
                    && archiveFolderUri != null
                    && toImportFolderUri != null
                    && toImportDoneFolderUri != null
                    && toImportFailedFolderUri != null
                ) {
                    if (keepPermission) {
                        // persist permissions
                        val takeFlags = (
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        resolver.takePersistableUriPermission(
                            rootUri.toAndroidUri(),
                            takeFlags,
                        )
                    }

                    return FileHandlerSAF(
                        applicationContext,
                        utils,
                        rootUri,
                        crosswordsFolderUri,
                        archiveFolderUri,
                        toImportFolderUri,
                        toImportDoneFolderUri,
                        toImportFailedFolderUri,
                    )
                }
            } catch (e : Exception) {
                AppLogger.e(TAG, "Unable to (re-)configure SAF directory.")
            }

            return null
        }

        /**
         * Read handler using locations stored in settings.
         *
         * Requires initialiseSAFSettings to have been called first. Returns
         * null if the handler could not be created. (E.g. if there are no
         * configured directories.) Will reinitialise if the rootUri is
         * still available.
         *
         * If reinit is needed, the settings are updated with the refreshed
         * values
         */
        fun readHandlerFromSettings(
            applicationContext : Context,
            utils : NativeBackendUtils,
            fileHandlerSettings : FileHandlerSettings,
            settings : ForkyzSettings,
        ) : FileHandlerSAF? {
            var fileHandler : FileHandlerSAF? = null

            val rootFolder = fileHandlerSettings.safRootURI
            val crosswordsFolder = fileHandlerSettings.safCrosswordsURI
            val archiveFolder = fileHandlerSettings.safArchiveURI
            val toImportFolder = fileHandlerSettings.safToImportURI
            val toImportDoneFolder = fileHandlerSettings.safToImportDoneURI
            val toImportFailedFolder = fileHandlerSettings.safToImportFailedURI

            if (
                !rootFolder.isEmpty()
                && !crosswordsFolder.isEmpty()
                && !archiveFolder.isEmpty()
                && !toImportFolder.isEmpty()
                && !toImportDoneFolder.isEmpty()
                && !toImportFailedFolder.isEmpty()
            ) {
                val rootFolderUri = rootFolder.toUri()
                val crosswordsFolderUri = crosswordsFolder.toUri()
                val archiveFolderUri = archiveFolder.toUri()
                val toImportFolderUri = toImportFolder.toUri()
                val toImportDoneFolderUri = toImportDoneFolder.toUri()
                val toImportFailedFolderUri = toImportFailedFolder.toUri()

                val resolver = applicationContext.getContentResolver()

                try {
                    if (
                        exists(resolver, crosswordsFolderUri)
                        && exists(resolver, archiveFolderUri)
                        && exists(resolver, toImportFolderUri)
                        && exists(resolver, toImportDoneFolderUri)
                        && exists(resolver, toImportFailedFolderUri)
                    ) {
                        fileHandler = FileHandlerSAF(
                            applicationContext,
                            utils,
                            rootFolderUri,
                            crosswordsFolderUri,
                            archiveFolderUri,
                            toImportFolderUri,
                            toImportDoneFolderUri,
                            toImportFailedFolderUri,
                        )
                    }
                } catch (e : SecurityException) {
                    AppLogger.e(
                        TAG,
                        "Permission not granted to configured SAF directories.",
                    )
                } catch (e : UnsupportedOperationException) {
                    AppLogger.e(TAG, "Unsupported operation with SAF")
                    e.printStackTrace()
                }
            }

            if (fileHandler == null && !rootFolder.isEmpty()) {
                fileHandler = initialiseSAFForRoot(
                    applicationContext,
                    utils,
                    rootFolder.toUri(),
                )
                // update settings to good values
                if (fileHandler != null)
                    settings.setFileHandlerSettings(fileHandler.settings)
            }

            return fileHandler
        }

        private fun exists(resolver : ContentResolver, uri : Uri) : Boolean {
            try {
                resolver.query(
                    uri.toAndroidUri(),
                    arrayOf<String>(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    ),
                    null,
                    null,
                    null,
                ).use { c ->
                    return (c?.getCount() ?: 0) > 0
                }
            } catch (e : IllegalArgumentException) {
                // if the file does not exist, this is thrown
                return false
            }
        }

        /**
         * Search for named subfolders in dirUri or create
         *
         * Throws exception if fails.
         *
         * @param names the names of the desired directories
         * @param baseUri the actual baseUri
         * @param resolver a content resolver
         * @return an array of the found/created Uris
         */
        @Throws(FileNotFoundException::class)
        private fun searchCreateDirectories(
            names : Array<String>,
            baseUri : Uri,
            resolver : ContentResolver,
        ) : Array<Uri?> {
            val androidBaseUri = baseUri.toAndroidUri()
            val dirId = DocumentsContract.getTreeDocumentId(androidBaseUri)
            val dirUri = DocumentsContract.buildDocumentUriUsingTree(
                androidBaseUri,
                dirId,
            )
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                androidBaseUri,
                dirId,
            )

            // do a search
            val desiredUris = arrayOfNulls<Uri>(names.size)
            resolver.query(
                childrenUri,
                arrayOf<String>(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                ),
                null,
                null,
                null,
            ).use { cursor ->
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val curName = cursor.getString(0)
                        val curMimeType = cursor.getString(1)
                        val curID = cursor.getString(2)

                        if (
                            DocumentsContract.Document.MIME_TYPE_DIR
                                == curMimeType
                        ) {
                            for (i in names.indices) {
                                if (names[i] == curName) {
                                    desiredUris[i] = DocumentsContract
                                        .buildDocumentUriUsingTree(
                                            androidBaseUri,
                                            curID,
                                        )?.toForkyzUri()
                                }
                            }
                        }
                    }
                }
            }
            // if not found, create new
            for (i in names.indices) {
                if (desiredUris[i] == null) {
                    desiredUris[i] = DocumentsContract.createDocument(
                        resolver,
                        dirUri,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        names[i],
                    )?.toForkyzUri()
                }
            }

            return desiredUris
        }
    }
}

