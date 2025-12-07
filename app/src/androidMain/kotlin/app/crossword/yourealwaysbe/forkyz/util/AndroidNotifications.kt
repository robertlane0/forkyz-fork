
package app.crossword.yourealwaysbe.forkyz.util

import android.app.Notification
import android.content.Intent
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.annotation.SuppressLint
import android.content.Context

import app.crossword.yourealwaysbe.forkyz.ForkyzActivity
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

class AndroidNotifications(
    val applicationContext : Context,
    val utils : AndroidVersionUtils,
) : Notifications {
    private val notificationManager
        = NotificationManagerCompat.from(applicationContext)

    override val downloadIcon = android.R.drawable.stat_sys_download
    override val downloadDoneIcon = android.R.drawable.stat_sys_download_done

    /**
     * Post a notification to channel with ID
     *
     * Always opens Forkyz on click. Cancel with ID.
     */
    @SuppressLint("MissingPermission") // checked via utils
    override fun notify(
        channelID : String,
        notificationID : Int,
        iconID : Int,
        title : String,
        contentText : String?,
    ) {
        if (utils.hasPostNotificationsPermission(applicationContext)) {
            val notification = NotificationCompat.Builder(
                applicationContext,
                channelID,
            ).setSmallIcon(iconID)
                .setContentTitle(title)
                .setContentText(contentText)
                .setWhen(System.currentTimeMillis())
                .build()
            notificationManager.notify(notificationID, notification)
        }
    }

    override fun cancel(notificationID : Int) {
        notificationManager.cancel(notificationID)
    }

    /**
     * Make a content intent for a notification
     */
    private fun getContentIntent() : PendingIntent {
        val notificationIntent = Intent(
            applicationContext,
            ForkyzActivity::class.java,
        )
        return PendingIntent.getActivity(
            applicationContext,
            0,
            notificationIntent,
            utils.immutablePendingIntentFlag(),
        )
    }
}

