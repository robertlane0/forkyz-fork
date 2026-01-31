
package app.crossword.yourealwaysbe.forkyz.util

import app.crossword.yourealwaysbe.forkyz.settings.BackgroundDownloadSettings

interface BackgroundDownloadManager {
    /**
     * Get if download is enabled according to given settings values
     */
    fun getIsBackgroundDownloadEnabled(
        bgSettings : BackgroundDownloadSettings,
    ) : Boolean

    fun scheduleNextDailyDownload(bgSettings : BackgroundDownloadSettings)

    /**
     * Set the download period to 1 hour
     */
    fun setHourlyBackgroundDownloadPeriod()
}

