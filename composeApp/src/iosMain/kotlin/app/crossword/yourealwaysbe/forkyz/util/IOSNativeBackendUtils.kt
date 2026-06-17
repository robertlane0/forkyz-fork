
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered

import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import io.github.vinceglb.filekit.PlatformFile

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

import app.crossword.yourealwaysbe.forkyz.SettingsLogcat
import app.crossword.yourealwaysbe.forkyz.SettingsNamedItem
import app.crossword.yourealwaysbe.forkyz.SettingsPage
import app.crossword.yourealwaysbe.forkyz.SettingsSubPage
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.crossword_solver_enabled
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_dictionary
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_announce_box,
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_announce_clue,
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_exit,import app.crossword.yourealwaysbe.forkyz.SettingsItem
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_crossword_solver,
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_clue_with_board
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_blank
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_no_ipuz
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_original
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_voice_input,
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_force_caps
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_haptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_native
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_split_words
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_use_native
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.random_clue_on_shake
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.storage_location
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.AppStrings
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.File
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerSystemFile
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache

val IOS_APP_SUPPORT_DIR : File by lazy {
    val urls = NSFileManager.defaultManager.URLsForDirectory(
        NSApplicationSupportDirectory,
        NSUserDomainMask,
    )
    val url = urls.first() as platform.Foundation.NSURL
    File(url.path!!).also { it.mkdirs() }
}

class IOSNativeBackendUtils(
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
        return true
    }

    @Throws(IOException::class)
    override fun openInput(uri : Uri) : Source? {
        try {
            return uri.getPath()?.let { path ->
                return PlatformFile(path).source().buffered()
            }
        } catch (e : IllegalArgumentException) {
            AppLogger.i("Forkyz", "Illegal, $e")
            throw IOException(e.message)
        }
    }

    @Throws(IOException::class)
    override fun openOutput(uri : Uri) : Sink? {
        try {
            return uri.getPath()?.let { path ->
                return PlatformFile(path).sink().buffered()
            }
        } catch (e : IllegalArgumentException) {
            throw IOException(e.message)
        }
    }

    override val isShareFileAvailable : Boolean = false

    override fun getShareDir() : File? {
        return null
    }

    override fun getShareURIForFile(shareFile : File) : Uri {
        return shareFile.uri
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
        val dbFile = File(IOS_APP_SUPPORT_DIR, "meta-cache.db")
        val builder : RoomDatabase.Builder<MetaCache.CachedMetaDB>
            = databaseBuilder(name = dbFile.pathString)
        builder.setDriver(BundledSQLiteDriver())
        return builder
    }

    override fun getAppFilesDir() : File {
        return IOS_APP_SUPPORT_DIR
    }

    @Throws(IOException::class)
    override fun isInternalStorageFull(minimumRequired : Long) : Boolean {
        return false
    }

    override fun getTargetFileHandler(uri : Uri) : FileHandler? {
        return object : FileHandlerSystemFile(
            this@IOSNativeBackendUtils,
            File(uri.getPath()!!),
        ) {
            override val isStorageMounted : Boolean = true

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

    override fun filterSettings(
        settings : List<SettingsItem>,
    ) : List<SettingsItem> {
        return settings.filter { item ->
            val badPage = item.page in listOf(
                SettingsPage.DOWNLOAD_BACKGROUND,
                SettingsPage.VOICE_ACCESSIBILITY,
                SettingsPage.DESKTOP,
            )
            if (badPage) {
                false
            } else {
                when (item) {
                    is SettingsSubPage -> {
                        item.subpage !in listOf(
                            SettingsPage.DOWNLOAD_BACKGROUND,
                            SettingsPage.VOICE_ACCESSIBILITY,
                            SettingsPage.DESKTOP,
                        )
                    }
                    is SettingsLogcat -> false
                    is SettingsNamedItem -> {
                        item.title !in listOf(
                            Res.string.external_dictionary,
                            Res.string.crossword_solver_enabled,
                            Res.string.random_clue_on_shake,
                            Res.string.keyboard_use_native,
                            Res.string.keyboard_native,
                            Res.string.keyboard_force_caps,
                            Res.string.keyboard_split_words,
                            Res.string.storage_location,
                            Res.string.keyboard_binding_announce_box,
                            Res.string.keyboard_binding_announce_clue,
                            Res.string.keyboard_binding_open_crossword_solver,
                            Res.string.keyboard_binding_share_clue,
                            Res.string.keyboard_binding_share_clue_with_board,
                            Res.string.keyboard_binding_share_puzzle,
                            Res.string.keyboard_binding_share_puzzle_blank,
                            Res.string.keyboard_binding_share_puzzle_no_ipuz,
                            Res.string.keyboard_binding_share_puzzle_original,
                            Res.string.keyboard_binding_voice_input,
                            Res.string.keyboard_binding_exit,
                        )
                    }
                    else -> true
                }
            }
        }
    }
}
