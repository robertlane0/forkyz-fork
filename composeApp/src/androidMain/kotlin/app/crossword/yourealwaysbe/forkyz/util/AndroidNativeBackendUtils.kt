
package app.crossword.yourealwaysbe.forkyz.util

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

import android.content.Context
import androidx.core.content.FileProvider
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.google.android.material.color.DynamicColors

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

import app.crossword.yourealwaysbe.forkyz.SettingsItem
import app.crossword.yourealwaysbe.forkyz.SettingsPage
import app.crossword.yourealwaysbe.forkyz.SettingsSubPage
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.AppStrings
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerSAF
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache
import app.crossword.yourealwaysbe.forkyz.util.files.PuzHandle
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

private val SHARE_AUTHORITY = "app.crossword.yourealwaysbe.forkyzfiles"
private val SHARE_DIR = "shared"

/**
 * Collects functions that viewmodels &c. need that depend on context
 */
class AndroidNativeBackendUtils(
    private val applicationContext : Context,
    private val utils : AndroidVersionUtils,
    private val strings : AppStrings,
) : NativeBackendUtils {
    override suspend fun getString(resourceID : StringResource) : String {
        return strings.getString(resourceID)
    }

    override suspend fun getString(
        resourceID : StringResource,
        vararg formatArgs: Any,
    ) : String {
        return strings.getString(resourceID, *formatArgs)
    }

    override suspend fun getQuantityString(
        resourceID : PluralStringResource,
        quantity : Int,
        vararg formatArgs: Any,
    ) : String {
        return strings.getPluralString(resourceID, quantity, *formatArgs)
    }

    override fun hasNetworkConnection() : Boolean {
        return utils.hasNetworkConnection(applicationContext)
    }

    @Throws(IOException::class)
    override fun openInputStream(uri : Uri) : InputStream? {
        return applicationContext
            .getContentResolver()
            .openInputStream(uri.toAndroidUri())
    }

    @Throws(IOException::class)
    override fun openOutputStream(uri : Uri) : OutputStream? {
        return applicationContext
            .getContentResolver()
            .openOutputStream(uri.toAndroidUri())
    }

    override val isShareFileAvailable : Boolean = true

    override fun getShareDir() : File? {
        val shareDir = File(applicationContext.getCacheDir(), SHARE_DIR)
        shareDir.mkdirs()
        return shareDir
    }

    override fun getShareURIForFile(shareFile : File) : Uri {
        val authority = applicationContext.getPackageName() + ".files";
        return FileProvider.getUriForFile(
            applicationContext,
            authority,
            shareFile,
        ).toForkyzUri()
    }

    override fun getFilterClueToAlphabeticRegex() : String {
        return utils.filterClueToAlphabeticRegex
    }

    override fun initialiseSAFForRoot(uri : Uri) : FileHandler? {
        return FileHandlerSAF.initialiseSAFForRoot(
            applicationContext,
            this,
            uri,
        )
    }

    override fun readSAFHandlerFromSettings(
        fileHandlerSettings : FileHandlerSettings,
        settings : ForkyzSettings,
    ) : FileHandler? {
        return FileHandlerSAF.readHandlerFromSettings(
            applicationContext,
            this,
            fileHandlerSettings,
            settings,
        )
    }

    override fun getMetaCache() : MetaCache {
        return MetaCache(this)
    }

    override fun getCachedMetaDBBuilder()
            : RoomDatabase.Builder<MetaCache.CachedMetaDB> {
        return databaseBuilder(
            applicationContext,
            MetaCache.CachedMetaDB::class.java,
            "meta-cache-db",
        )
    }

    override fun getAppFilesDir() : File {
        return applicationContext.getFilesDir()
    }

    @Throws(IOException::class)
    override fun isInternalStorageFull(minimumRequired : Long) : Boolean {
        return utils.isInternalStorageFull(applicationContext, minimumRequired)
    }

    /**
     * Get a file handler rooted at uri for export
     */
    override fun getTargetFileHandler(uri : Uri) : FileHandler? {
        return FileHandlerSAF.initialiseSAFForRoot(
            applicationContext,
            this,
            uri,
        )
    }

    override fun getLogcat() : List<String>? {
        val uid = utils.getApplicationUID(applicationContext)
        val uidFlag : String = if (uid < 0) {
            ""
        } else {
            " --uid " + uid.toString()
        }
        val process = Runtime.getRuntime().exec("logcat -d" + uidFlag)
        try {
            BufferedReader(
                InputStreamReader(process.getInputStream())
            ).useLines {
                return it.toList()
            }
        } catch (e : IOException) {
            return null
        }
    }

    override val isDynamicColorAvailable : Boolean
        get() {
            return DynamicColors.isDynamicColorAvailable()
        }

    override fun filterSettings(
        settings : List<SettingsItem>,
    ) : List<SettingsItem> {
        return settings.filter { item ->
            val badPage = item.page in listOf(SettingsPage.DESKTOP)
            if (badPage) {
                false
            } else {
                when (item) {
                    is SettingsSubPage -> {
                        item.subpage !in listOf(
                            SettingsPage.DESKTOP,
                        )
                    }
                    else -> true
                }
            }
        }
    }
}

