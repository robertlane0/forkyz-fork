
package app.crossword.yourealwaysbe.forkyz.util

class JVMNotifications : Notifications {
    override val downloadIcon : Int = -1
    override val downloadDoneIcon : Int = -1

    /**
     * Post a notification to channel with ID
     *
     * Always opens Forkyz on click. Cancel with ID. iconID is e.g.
     * downloadIcon or downloadDoneIcon.
     */
    override fun notify(
        channelID : String,
        notificationID : Int,
        iconID : Int,
        title : String,
        contentText : String?,
    ) {
        // TODO
    }

    override fun cancel(notificationID : Int) {
        // TODO
    }
}

