
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.channels.Channel

data class Notification(
    val title : String,
    val message : String?,
)

interface Notifications {
    val downloadIcon : Int
    val downloadDoneIcon : Int

    /**
     * A flow of notification messages for the front-end to display
     *
     * Can be used if platform does not provide a notification implementation.
     * Or may be inactive. Notifications will not be cancelled, so should be
     * timed out like toasts.
     */
    val notifications : Channel<Notification>

    /**
     * Post a notification to channel with ID
     *
     * Always opens Forkyz on click. Cancel with ID. iconID is e.g.
     * downloadIcon or downloadDoneIcon.
     */
    fun notify(
        channelID : String,
        notificationID : Int,
        title : String,
        contentText : String? = null,
        iconID : Int = -1,
    )

    fun cancel(notificationID : Int)
}

