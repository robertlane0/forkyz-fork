
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class JVMNotifications : Notifications {
    override val downloadIcon : Int = -1
    override val downloadDoneIcon : Int = -1

    override val notifications = Channel<Notification>()

    /**
     * Post a notification to channel with ID
     *
     * Always opens Forkyz on click. Cancel with ID. iconID is e.g.
     * downloadIcon or downloadDoneIcon.
     */
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

