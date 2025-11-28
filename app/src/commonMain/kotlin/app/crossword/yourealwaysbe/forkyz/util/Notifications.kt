
package app.crossword.yourealwaysbe.forkyz.util

interface Notifications {
    /**
     * Post a notification to channel with ID
     *
     * Always opens Forkyz on click. Cancel with ID.
     */
    fun notify(
        channelID : String,
        notificationID : Int,
        iconID : Int,
        title : String,
        contentText : String? = null,
    )

    fun cancel(notificationID : Int)
}

