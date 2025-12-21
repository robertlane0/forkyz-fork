
package app.crossword.yourealwaysbe.forkyz.util

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerJavaFile
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache
import app.crossword.yourealwaysbe.forkyz.util.toUri

class TestNativeBackendUtils : NativeBackendUtils {
    override suspend fun getString(resourceID : StringResource) : String {
        return ""
    }

    override suspend fun getString(
        resourceID : StringResource,
        vararg formatArgs: Any,
    ) : String {
        return ""
    }

    override suspend fun getQuantityString(
        resourceID : PluralStringResource,
        quantity : Int,
        vararg formatArgs: Any,
    ) : String {
        return ""
    }

    override fun hasNetworkConnection() : Boolean {
        return true
    }

    override fun getApplicationVersionName() : String? {
        return null
    }

    @Throws(IOException::class)
    override fun openInputStream(uri : Uri) : InputStream? {
        return null
    }

    @Throws(IOException::class)
    override fun openOutputStream(uri : Uri) : OutputStream? {
        return null
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
        return null
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
        throw UnsupportedOperationException()
    }

    override fun getAppFilesDir() : File {
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun isInternalStorageFull(minimumRequired : Long) : Boolean {
        return false
    }

    /**
     * Get a file handler rooted at uri for export
     */
    override fun getTargetFileHandler(uri : Uri) : FileHandler? {
        throw UnsupportedOperationException()
    }

    override fun getLogcat() : List<String>? {
        return null
    }

    override val isDynamicColorAvailable : Boolean = false
}

