
package app.crossword.yourealwaysbe.forkyz.util

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider

import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
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
class NativeBackendUtils(
    private val applicationContext : Context,
    private val utils : AndroidVersionUtils,
) {
    suspend fun getString(resourceID : Int) : String {
        return applicationContext.getString(resourceID)
    }

    suspend fun getString(resourceID : Int, vararg formatArgs: Any) : String {
        return applicationContext.getString(resourceID, *formatArgs)
    }

    suspend fun getQuantityString(
        resourceID : Int,
        quantity : Int,
        vararg formatArgs: Any,
    ) : String {
        return applicationContext.resources.getQuantityString(
            resourceID,
            quantity,
            *formatArgs,
        )
    }

    @Throws(IOException::class)
    fun getRawResource(rawAssetID : Int) : InputStream {
        return applicationContext.resources.openRawResource(rawAssetID)
    }

    fun hasNetworkConnection() : Boolean {
        return utils.hasNetworkConnection(applicationContext)
    }

    fun getApplicationVersionName() : String {
        return utils.getApplicationVersionName(applicationContext)
    }

    @Throws(IOException::class)
    fun openInputStream(uri : Uri) : InputStream? {
        return applicationContext.getContentResolver().openInputStream(uri)
    }

    @Throws(IOException::class)
    fun openOutputStream(uri : Uri) : OutputStream? {
        return applicationContext.getContentResolver().openOutputStream(uri)
    }

    fun getShareDir() : File {
        val shareDir = File(applicationContext.getCacheDir(), SHARE_DIR)
        shareDir.mkdirs()
        return shareDir
    }

    fun getShareURIForFile(shareFile : File) : Uri {
        val authority = applicationContext.getPackageName() + ".files";
        return FileProvider.getUriForFile(
            applicationContext,
            authority,
            shareFile,
        )
    }

    fun getFilterClueToAlphabeticRegex() : String {
        return utils.getFilterClueToAlphabeticRegex()
    }

    fun initialiseSAFForRoot(uri : Uri) : FileHandler? {
        return FileHandlerSAF.initialiseSAFForRoot(
            applicationContext,
            this,
            uri,
        )
    }

    fun readSAFHandlerFromSettings(
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

    fun getMetaCache() : MetaCache {
        return MetaCache(applicationContext)
    }

    fun getAppFilesDir() : File {
        return applicationContext.getFilesDir()
    }

    @Throws(IOException::class)
    fun isInternalStorageFull(minimumRequired : Long) : Boolean {
        return utils.isInternalStorageFull(applicationContext, minimumRequired)
    }

    /**
     * Get a file handler rooted at uri for export
     */
    fun getTargetFileHandler(uri : Uri) : FileHandler? {
        return FileHandlerSAF.initialiseSAFForRoot(
            applicationContext,
            this,
            uri,
            false
        )
    }

    fun getLogcat() : List<String>? {
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
}

