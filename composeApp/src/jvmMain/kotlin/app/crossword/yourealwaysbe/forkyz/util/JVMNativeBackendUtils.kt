
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered

import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

import dev.dirs.ProjectDirectories

import app.crossword.yourealwaysbe.forkyz.SettingsItem
import app.crossword.yourealwaysbe.forkyz.SettingsLogcat
import app.crossword.yourealwaysbe.forkyz.SettingsNamedItem
import app.crossword.yourealwaysbe.forkyz.SettingsPage
import app.crossword.yourealwaysbe.forkyz.SettingsSubPage
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.crossword_solver_enabled
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.external_dictionary
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_announce_box
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_announce_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_open_crossword_solver
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_clue
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_clue_with_board
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_blank
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_no_ipuz
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_share_puzzle_original
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_binding_voice_input
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_force_caps
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_haptic
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_native
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_split_words
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.keyboard_use_native
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.random_clue_on_shake
import app.crossword.yourealwaysbe.forkyz.settings.FileHandlerSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.StorageLocation
import app.crossword.yourealwaysbe.forkyz.util.AppStrings
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.File
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerJVMSAF
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerSystemFile
import app.crossword.yourealwaysbe.forkyz.util.files.MetaCache
import app.crossword.yourealwaysbe.forkyz.util.toUri

/**
 * Data dir for app
 *
 * Currently hardcoded to ~/.local/share/forkyz.
 */
val XDG_DATA_DIR : File by lazy {
    File(
        ProjectDirectories.from(
            "app",
            "crossword.yourealwaysbe",
            "forkyz",
        ).dataDir,
    ).also { it.mkdirs() }
}

class JVMNativeBackendUtils(
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
            return uri.getPath()?.let { return File(it).source().buffered() }
        } catch (e : IllegalArgumentException) {
            // not a file, make IOException
            throw IOException(e.message)
        }
    }

    @Throws(IOException::class)
    override fun openOutput(uri : Uri) : Sink? {
        try {
            return uri.getPath()?.let { return File(it).sink().buffered() }
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
        return shareFile.uri
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
        val dbFile = File(getAppFilesDir(), "meta-cache.db")
        // TODO: do we want the BundledSQLiteDriver on all platforms?
        // I'm afraid of breaking existing databases on random Android
        // devices.
        val builder : RoomDatabase.Builder<MetaCache.CachedMetaDB>
            = databaseBuilder(name = dbFile.pathString)
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
        return object : FileHandlerSystemFile(
            this@JVMNativeBackendUtils,
            File(uri.getPath()!!),
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

    override fun filterSettings(
        settings : List<SettingsItem>,
    ) : List<SettingsItem> {
        return settings.filter { item ->
            val badPage = item.page in listOf(
                SettingsPage.DOWNLOAD_BACKGROUND,
                SettingsPage.VOICE_ACCESSIBILITY,
            )
            if (badPage) {
                false
            } else {
                when (item) {
                    is SettingsSubPage -> {
                        item.subpage !in listOf(
                            SettingsPage.DOWNLOAD_BACKGROUND,
                            SettingsPage.VOICE_ACCESSIBILITY,
                        )
                    }
                    is SettingsLogcat -> false
                    is SettingsNamedItem -> {
                        item.title !in listOf(
                            Res.string.crossword_solver_enabled,
                            Res.string.external_dictionary,
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
                            Res.string.keyboard_force_caps,
                            Res.string.keyboard_haptic,
                            Res.string.keyboard_native,
                            Res.string.keyboard_split_words,
                            Res.string.keyboard_use_native,
                            Res.string.random_clue_on_shake,
                        )
                    }
                    else -> true
                }
            }
        }
    }
}

