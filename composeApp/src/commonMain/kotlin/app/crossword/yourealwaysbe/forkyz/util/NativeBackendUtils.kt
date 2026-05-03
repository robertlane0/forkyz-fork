
package app.crossword.yourealwaysbe.forkyz.util

import java.io.File
import java.io.OutputStream
import kotlinx.io.IOException
import kotlinx.io.Source

import androidx.room.RoomDatabase

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

import app.crossword.yourealwaysbe.forkyz.SettingsItem
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache

/**
 * Collects functions that viewmodels &c. need that depend on context
 */
interface NativeBackendUtils {
    suspend fun getString(resourceID : StringResource) : String
    suspend fun getString(
        resourceID : StringResource,
        vararg formatArgs : Any,
    ) : String
    suspend fun getQuantityString(
        resourceID : PluralStringResource,
        quantity : Int,
        vararg formatArgs : Any,
    ) : String
    fun hasNetworkConnection() : Boolean
    @Throws(IOException::class)
    fun openInput(uri : Uri) : Source?
    @Throws(java.io.IOException::class)
    fun openOutputStream(uri : Uri) : OutputStream?
    val isShareFileAvailable : Boolean
    fun getShareDir() : File?
    fun getShareURIForFile(shareFile : File) : Uri
    fun getFilterClueToAlphabeticRegex() : String
    fun initialiseSAFForRoot(uri : Uri) : FileHandler?
    fun readSAFHandlerFromSettings(
        fileHandlerSettings : FileHandlerSettings,
        settings : ForkyzSettings,
    ) : FileHandler?
    fun getMetaCache() : MetaCache
    fun getCachedMetaDBBuilder() : RoomDatabase.Builder<MetaCache.CachedMetaDB>
    fun getAppFilesDir() : File
    @Throws(IOException::class)
    fun isInternalStorageFull(minimumRequired : Long) : Boolean
    /**
     * Get a file handler rooted at uri for export
     */
    fun getTargetFileHandler(uri : Uri) : FileHandler?
    fun getLogcat() : List<String>?
    val isDynamicColorAvailable : Boolean
    fun filterSettings(settings : List<SettingsItem>) : List<SettingsItem>
}

