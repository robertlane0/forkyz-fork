
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.channels.Channel

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

class AndroidNotifications(
    val applicationContext : Context,
    val utils : AndroidVersionUtils,
) : Notifications {
    private val notificationManager
        = NotificationManagerCompat.from(applicationContext)

    override val downloadIcon = android.R.drawable.stat_sys_download
    override val downloadDoneIcon = android.R.drawable.stat_sys_download_done

    // not used
    override val notifications = Channel<Notification>()

    /**
     * Post a notification to channel with ID
     *
     * Always opens Forkyz on click. Cancel with ID.
     */
    @SuppressLint("MissingPermission") // checked via utils
    override fun notify(
        channelID : String,
        notificationID : Int,
        title : String,
        contentText : String?,
        iconID : Int,
    ) {
        if (utils.hasPostNotificationsPermission(applicationContext)) {
            val builder = NotificationCompat.Builder(
                applicationContext,
                channelID,
            ).setContentTitle(title)
                .setContentText(contentText)
                .setWhen(System.currentTimeMillis())

            if (iconID >= 0)
                builder.setSmallIcon(iconID)

            val notification = builder.build()
            notificationManager.notify(notificationID, notification)
        }
    }

    override fun cancel(notificationID : Int) {
        notificationManager.cancel(notificationID)
    }
}

