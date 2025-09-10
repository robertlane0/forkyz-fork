
package app.crossword.yourealwaysbe.forkyz.net

import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

import android.content.Context
import androidx.annotation.MainThread
import androidx.core.app.NotificationManagerCompat

import dagger.hilt.android.qualifiers.ApplicationContext

import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.getOnce
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

/**
 * Convenience for constructing Downloaders objects
 *
 * Since most arguments are injectable, no need to fetch them in all
 * places.
 */
@Singleton
class DownloadersProvider @Inject constructor(
    @ApplicationContext private val applicationContext : Context,
    private val utils : AndroidVersionUtils,
    private val settings : ForkyzSettings,
    private val fileHandlerProvider : FileHandlerProvider,

) {
    val liveDownloaders : Flow<Downloaders?> by lazy {
        combine(
            settings.liveDownloadersSettings,
            fileHandlerProvider.liveFileHandler.filterNotNull(),
            this::constructFromSettings,
        )
    }

    /**
     * Builds a new downloaders object from settings
     */
    @MainThread
    fun get(cb : Consumer<Downloaders>) {
        liveDownloaders.filterNotNull().getOnce { cb.accept(it) }
    }

    private fun constructFromSettings(
        downloadersSettings : DownloadersSettings,
        fileHandler : FileHandler,
    ) : Downloaders {
        return Downloaders(
            applicationContext,
            utils,
            fileHandler,
            downloadersSettings,
            NotificationManagerCompat.from(applicationContext)
        )
    }
}
