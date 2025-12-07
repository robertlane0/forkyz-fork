
package app.crossword.yourealwaysbe.forkyz.util

interface Notifications {
    val downloadIcon : Int
    val downloadDoneIcon : Int

    /**
     * Post a notification to channel with ID
     *
     * Always opens Forkyz on click. Cancel with ID. iconID is e.g.
     * downloadIcon or downloadDoneIcon.
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

