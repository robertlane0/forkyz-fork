
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_downloads_not_available
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.background_downloads_not_available_desc
import app.crossword.yourealwaysbe.forkyz.settings.BackgroundDownloadSettings

class JVMBackgroundDownloadManager(
    private val utils : NativeBackendUtils,
    private val notifications : Notifications,
) : BackgroundDownloadManager {
    @OptIn(DelicateCoroutinesApi::class)
    override fun getIsBackgroundDownloadEnabled(
        bgSettings : BackgroundDownloadSettings,
    ) : Boolean {
        val isEnabled = bgSettings.hourly || bgSettings.days.size > 0
        if (isEnabled) {
            GlobalScope.launch(Dispatchers.Main) {
                notifications.notify(
                    channelID = "",
                    notificationID = 0,
                    title = utils.getString(
                        Res.string.background_downloads_not_available,
                    ),
                    contentText = utils.getString(
                        Res.string.background_downloads_not_available_desc,
                    ),
                )
            }
        }
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

