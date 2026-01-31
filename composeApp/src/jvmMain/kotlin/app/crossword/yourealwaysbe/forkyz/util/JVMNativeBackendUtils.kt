
package app.crossword.yourealwaysbe.forkyz.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

import dev.dirs.ProjectDirectories

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerJVMSAF
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerJavaFile
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache
import app.crossword.yourealwaysbe.forkyz.util.toJVMURI
import app.crossword.yourealwaysbe.forkyz.util.toUri

/**
 * Data dir for app
 *
 * Currently hardcoded to ~/.local/share/forkyz.
 */
val XDG_DATA_DIR : File = File(
    ProjectDirectories.from(
        "app",
        "crossword.yourealwaysbe",
        "forkyz",
    ).dataDir,
)

class JVMNativeBackendUtils : NativeBackendUtils {
    override suspend fun getString(resourceID : StringResource) : String {
        return org.jetbrains.compose.resources.getString(resourceID)
    }

    override suspend fun getString(
        resourceID : StringResource,
        vararg formatArgs: Any,
    ) : String {
        return org.jetbrains.compose.resources.getString(
            resourceID,
            *formatArgs,
        )
    }

    override suspend fun getQuantityString(
        resourceID : PluralStringResource,
        quantity : Int,
        vararg formatArgs: Any,
    ) : String {
        return org.jetbrains.compose.resources.getPluralString(
            resourceID,
            quantity,
            *formatArgs,
        )
    }

    override fun hasNetworkConnection() : Boolean {
        return true
    }

    @Throws(IOException::class)
    override fun openInputStream(uri : Uri) : InputStream? {
        try {
            return FileInputStream(File(uri.toJVMURI()))
        } catch (e : IllegalArgumentException) {
            // not a file, make IOException
            throw IOException(e.message)
        }
    }

    @Throws(IOException::class)
    override fun openOutputStream(uri : Uri) : OutputStream? {
        try {
            return FileOutputStream(File(uri.toJVMURI()))
        } catch (e : IllegalArgumentException) {
            // not a file, make IOException
            throw IOException(e.message)
        }
    }

    override val isShareFileAvailable : Boolean = false

    override fun getShareDir() : File? {
        return null
    }

    override fun getShareURIForFile(shareFile : File) : Uri {
        return shareFile.toURI().toString().toUri()
    }

    override fun getFilterClueToAlphabeticRegex() : String {
        return "&[^;]*;|<[^>]*>|[^\\p{IsAlphabetic}]"
    }

    override fun initialiseSAFForRoot(uri : Uri) : FileHandler? {
        return FileHandlerJVMSAF(this, uri)
    }

    override fun readSAFHandlerFromSettings(
        fileHandlerSettings : FileHandlerSettings,
        settings : ForkyzSettings,
    ) : FileHandler? {
        return null
    }

    override fun getMetaCache() : MetaCache {
        return MetaCache(this)
    }

    override fun getCachedMetaDBBuilder()
            : RoomDatabase.Builder<MetaCache.CachedMetaDB> {
        val dbFile = File(XDG_DATA_DIR, "meta-cache.db")
        // TODO: do we want the BundledSQLiteDriver on all platforms?
        // I'm afraid of breaking existing databases on random Android
        // devices.
        val builder : RoomDatabase.Builder<MetaCache.CachedMetaDB>
            = databaseBuilder(name = dbFile.absolutePath)
        builder.setDriver(BundledSQLiteDriver())
        return builder
    }

    override fun getAppFilesDir() : File {
        return XDG_DATA_DIR
    }

    @Throws(IOException::class)
    override fun isInternalStorageFull(minimumRequired : Long) : Boolean {
        return false
    }

    /**
     * Get a file handler rooted at uri for export
     */
    override fun getTargetFileHandler(uri : Uri) : FileHandler? {
        return object : FileHandlerJavaFile(
            this@JVMNativeBackendUtils,
            File(uri.toJVMURI()),
        ) {
            override val isStorageMounted : Boolean = true

            // dummy values
            override val settings : FileHandlerSettings = FileHandlerSettings(
                StorageLocation.SL_EXTERNAL_SAF,
                uri.toString(),
                "",
                "",
                "",
                "",
                "",
            )

            override val isStorageFull : Boolean = false
        }
    }

    override fun getLogcat() : List<String>? {
        return null
    }

    override val isDynamicColorAvailable : Boolean = false
}

