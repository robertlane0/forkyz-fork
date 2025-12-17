
package app.crossword.yourealwaysbe.forkyz.util

import app.crossword.yourealwaysbe.forkyz.settings.BackgroundDownloadSettings

class JVMBackgroundDownloadManager : BackgroundDownloadManager {
    override fun getIsBackgroundDownloadEnabled(
        bgSettings : BackgroundDownloadSettings,
    ) : Boolean {
        return false
    }

    override fun scheduleNextDailyDownload(
        bgSettings : BackgroundDownloadSettings,
    ) {
        // ignore
    }

    override fun setHourlyBackgroundDownloadPeriod() {
        // ignore
    }
}

