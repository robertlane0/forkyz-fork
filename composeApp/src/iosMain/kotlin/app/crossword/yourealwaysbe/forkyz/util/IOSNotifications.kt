
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class IOSNotifications : Notifications {
    override val downloadIcon : Int = -1
    override val downloadDoneIcon : Int = -1

    override val notifications = Channel<Notification>()

    @OptIn(DelicateCoroutinesApi::class)
    override fun notify(
        channelID : String,
        notificationID : Int,
        title : String,
        contentText : String?,
        iconID : Int,
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            notifications.send(Notification(title, contentText))
        }
    }

    override fun cancel(notificationID : Int) {
        // currently noop
    }
}
