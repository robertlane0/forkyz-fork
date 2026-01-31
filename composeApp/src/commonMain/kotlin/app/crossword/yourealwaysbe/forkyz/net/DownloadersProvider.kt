
package app.crossword.yourealwaysbe.forkyz.net

import java.util.function.Consumer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

import androidx.annotation.MainThread


import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzle_download_failed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzle_download_no_connection
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzle_downloaded
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_downloaded_all
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_downloaded_none
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_downloaded_some
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_downloading
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_downloading_from
import app.crossword.yourealwaysbe.forkyz.settings.DownloadersSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Notifications
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandler
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.getOnce
/**
 * Convenience for constructing Downloaders objects
 *
 * Since most arguments are injectable, no need to fetch them in all
 * places.
 */
class DownloadersProvider(
    private val settings : ForkyzSettings,
    private val fileHandlerProvider : FileHandlerProvider,
    private val notifications : Notifications,
    private val utils : NativeBackendUtils,
) {
    companion object {
        val PUZZLE_DOWNLOAD_CHANNEL_ID = "forkyz.downloads"
    }

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
            utils,
            fileHandler,
            downloadersSettings,
            NotificationDownloadersListener(
                notifications,
                downloadersSettings,
                utils,
            ),
        )
    }

    private class NotificationDownloadersListener(
        private val notifications : Notifications,
        private val downloadersSettings : DownloadersSettings,
        private val utils : NativeBackendUtils,
    ) : DownloadersListener {
        private var nextNotificationID : Int = 1

        override suspend fun onNoConnection() {
            if (downloadersSettings.suppressSummaryNotifications)
                return

            notifications.notify(
                channelID = PUZZLE_DOWNLOAD_CHANNEL_ID,
                notificationID = 0,
                title = utils.getString(
                    Res.string.puzzle_download_no_connection,
                ),
                iconID = notifications.downloadDoneIcon,
            )
        }

        override suspend fun onPuzzleDownloadStarted(name : String) {
            if (downloadersSettings.suppressIndividualNotifications)
                return

            notifications.notify(
                channelID = PUZZLE_DOWNLOAD_CHANNEL_ID,
                notificationID = 0,
                title = utils.getString(Res.string.puzzles_downloading),
                contentText = utils.getString(
                    Res.string.puzzles_downloading_from,
                    name,
                ),
                iconID = notifications.downloadIcon,
            )
        }

        override suspend fun onPuzzleDownloadFinished(
            name : String,
            result : Downloader.DownloadResult,
        ) {
            if (downloadersSettings.suppressIndividualNotifications)
                return

            // don't notify unless success or failure
            // notifications about existing puzzles would be annoying
            if (!(result.isSuccess || result.isFailed))
                return

            val messageId = if (result.isSuccess)
                Res.string.puzzle_downloaded
            else
                Res.string.puzzle_download_failed;

            notifications.notify(
                channelID = PUZZLE_DOWNLOAD_CHANNEL_ID,
                notificationID = nextNotificationID++,
                title = utils.getString(messageId, name),
                iconID = notifications.downloadDoneIcon,
            )
        }

        override suspend fun onDownloadsFinished(
            someDownloaded : Boolean,
            someFailed : Boolean,
        ) {
            notifications.cancel(0)

            if (downloadersSettings.suppressSummaryNotifications)
                return

            val messageID = if (someDownloaded && someFailed)
                Res.string.puzzles_downloaded_some
            else if (someDownloaded)
                Res.string.puzzles_downloaded_all
            else if (someFailed)
                Res.string.puzzles_downloaded_none;
            else // nothing downloaded or failed
                null

            if (messageID != null) {
                notifications.notify(
                    channelID = PUZZLE_DOWNLOAD_CHANNEL_ID,
                    notificationID = 0,
                    title = utils.getString(messageID),
                    iconID = notifications.downloadDoneIcon,
                )
            }
        }
    }
}
