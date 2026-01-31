
package app.crossword.yourealwaysbe.forkyz.versions

import java.io.IOException
import kotlinx.coroutines.runBlocking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import androidx.annotation.RequiresApi

import org.jetbrains.compose.resources.getString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_notification_channel_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_notification_channel_name
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.export_notification_channel_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.export_notification_channel_name
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.save_notification_channel_desc
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.save_notification_channel_name
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.PuzzleExportService
import app.crossword.yourealwaysbe.forkyz.util.PuzzleSaveService

private val TAG = "ForkyzOreoUtil"

@RequiresApi(api = Build.VERSION_CODES.O)
open class OreoUtil : NougatUtil() {
    public override fun createNotificationChannels(context : Context) {
        AppLogger.i(TAG, "Creating notification channels")

        // think need all channels right away
        runBlocking {
            val notificationManager
                = context.getSystemService<NotificationManager>(
                    NotificationManager::class.java,
                )

            var name : CharSequence = getString(
                Res.string.download_notification_channel_name,
            )
            var description = getString(
                Res.string.download_notification_channel_desc,
            )
            val importance = NotificationManager.IMPORTANCE_LOW
            var channel = NotificationChannel(
                DownloadersProvider.PUZZLE_DOWNLOAD_CHANNEL_ID,
                name,
                importance,
            )
            channel.setDescription(description)
            notificationManager.createNotificationChannel(channel)

            name = getString(Res.string.save_notification_channel_name)
            description = getString(Res.string.save_notification_channel_desc)
            channel = NotificationChannel(
                PuzzleSaveService.NOTIFICATION_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.setDescription(description)
            notificationManager.createNotificationChannel(channel)

            name = getString(Res.string.export_notification_channel_name)
            description = getString(Res.string.export_notification_channel_desc)
            channel = NotificationChannel(
                PuzzleExportService.NOTIFICATION_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW,
            )
            channel.setDescription(description)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @Throws(IOException::class)
    public override fun isInternalStorageFull(
        context : Context, minimumBytesFree : Long
    ) : Boolean {
        val storageManager = context.getSystemService<StorageManager>(
            StorageManager::class.java,
        )
        val appSpecificInternalDirUuid = storageManager.getUuidForPath(
            context.getFilesDir(),
        )
        val availableBytes = storageManager.getAllocatableBytes(
            appSpecificInternalDirUuid,
        )
        return availableBytes < minimumBytesFree
    }
}

