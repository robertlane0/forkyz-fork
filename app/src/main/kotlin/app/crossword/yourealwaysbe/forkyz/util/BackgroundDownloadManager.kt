
package app.crossword.yourealwaysbe.forkyz.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Set
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.content.Context
import android.util.Log
import androidx.annotation.MainThread
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.ListenableFuture

import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.settings.BackgroundDownloadSettings
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

private val TAG = "ForkyzBackgroundDLMan"
private val DOWNLOAD_WORK_NAME_HOURLY = "backgroundDownloadHourly"
private val DOWNLOAD_WORK_NAME_DAILY = "backgroundDownloadDaily"

/**
 * Schedule background downloads using Android WorkManager
 */
class BackgroundDownloadManager(
    private val workManager : WorkManager,
    private val settings : ForkyzSettings,
) {
    init {
        settings.liveBackgroundDownloadSettings
            .observeForeverWithLifecycle { value ->
                Log.i(
                    TAG,
                    "Triggering update background download work with: " + value,
                )
                updateBackgroundDownloads()
            }
    }

    /**
     * Get if download is enabled according to given settings values
     */
    fun getIsBackgroundDownloadEnabled(
        bgSettings : BackgroundDownloadSettings,
    ) : Boolean {
        return (
            bgSettings.hourly
            || getNextDailyDownloadDelay(bgSettings) >= 0
        )
    }

    @MainThread
    private fun updateBackgroundDownloads() {
        settings.getBackgroundDownloadSettings { bgSettings ->
            Log.i(
                TAG,
                "Updating background download work schedule with: "
                    + bgSettings,
            )
            if (bgSettings.hourly) {
                cancelDailyDownloads()
                scheduleHourlyDownloads(bgSettings)
            } else {
                cancelHourlyDownloads()
                scheduleNextDailyDownload(bgSettings)
            }
        }
    }

    private fun scheduleHourlyDownloads(
        bgSettings : BackgroundDownloadSettings,
    ) {
        val request = PeriodicWorkRequest.Builder(
            HourlyDownloadWorker::class.java,
            1,
            TimeUnit.HOURS,
        ).setConstraints(getConstraints(bgSettings))
            .build()

        getWorkManager().enqueueUniquePeriodicWork(
            DOWNLOAD_WORK_NAME_HOURLY,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun scheduleNextDailyDownload(
        bgSettings : BackgroundDownloadSettings,
    ) {
        val nextDelay = getNextDailyDownloadDelay(bgSettings)
        if (nextDelay < 0) {
            cancelDailyDownloads()
            return
        }

        val request
            = OneTimeWorkRequest.Builder(DailyDownloadWorker::class.java)
                .setConstraints(getConstraints(bgSettings))
                .setInitialDelay(nextDelay, TimeUnit.MILLISECONDS)
                .build()

        getWorkManager().enqueueUniqueWork(
            DOWNLOAD_WORK_NAME_DAILY,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    /**
     * Set the download period to 1 hour
     */
    fun setHourlyBackgroundDownloadPeriod() {
        settings.setDownloadHourly(true)
    }

    /**
     * Number of millis to next daily download
     *
     * @return -1 if none to schedule
     */
    private fun getNextDailyDownloadDelay(
        bgSettings : BackgroundDownloadSettings,
    ) : Long {

        val days = bgSettings.days
        val downloadTime = bgSettings.daysTime
        var nextDownloadDelay = -1L

        days.forEach { dayString ->
            val day = Integer.valueOf(dayString)
            val delay = getDelay(day, downloadTime)
            if (nextDownloadDelay < 0 || delay < nextDownloadDelay)
                nextDownloadDelay = delay
        }

        return nextDownloadDelay
    }

    private fun cancelHourlyDownloads() {
        getWorkManager().cancelUniqueWork(DOWNLOAD_WORK_NAME_HOURLY)
    }

    private fun cancelDailyDownloads() {
        getWorkManager().cancelUniqueWork(DOWNLOAD_WORK_NAME_DAILY)
    }

    private fun getConstraints(
        bgSettings : BackgroundDownloadSettings,
    ) : Constraints {
        val requireUnmetered = bgSettings.requireUnmetered
        val allowRoaming = bgSettings.allowRoaming
        val requireCharging = bgSettings.requireCharging

        val constraintsBuilder = Constraints.Builder()

        if (requireUnmetered)
            constraintsBuilder.setRequiredNetworkType(NetworkType.UNMETERED)
        else if (!allowRoaming)
            constraintsBuilder.setRequiredNetworkType(NetworkType.NOT_ROAMING)
        else
            constraintsBuilder.setRequiredNetworkType(NetworkType.CONNECTED)

        constraintsBuilder.setRequiresCharging(requireCharging)

        return constraintsBuilder.build()
    }

    /**
     * Get num millis to next day/time
     *
     * @param dayOfWeek 1-7 like java DayOfWeek
     * @param hourOfDay 0-23
     */
    private fun getDelay(dayOfWeek : Int, hourOfDay : Int) : Long {
        // start from now and adjust
        val now = LocalDateTime.now()
        val nowDayOfWeek = now.getDayOfWeek().getValue()

        var nextDownload = now.plusDays(dayOfWeek.toLong() - nowDayOfWeek)
            .withHour(hourOfDay)
            .truncatedTo(ChronoUnit.HOURS)

        if (!nextDownload.isAfter(LocalDateTime.now()))
            nextDownload = nextDownload.plusDays(7)

        return ChronoUnit.MILLIS.between(now, nextDownload)
    }

    private fun getWorkManager() : WorkManager { return workManager }
}

abstract class BaseDownloadWorker(
    context : Context,
    params : WorkerParameters,
    private val utils : AndroidVersionUtils,
    protected val settings : ForkyzSettings,
    private val fileHandlerProvider : FileHandlerProvider,
    private val downloadersProvider : DownloadersProvider,
) : ListenableWorker(
    context,
    params,
) {
    private val downloadScope = CoroutineScope(Dispatchers.IO)

    protected fun doDownload(
        completer : CallbackToFutureAdapter.Completer<ListenableWorker.Result>,
    ) {
        Log.i(TAG, "Downloading most recent puzzles")

        downloadersProvider.get { dls ->
            downloadScope.launch {
                try {
                    val now = LocalDate.now()
                    dls.downloadLatestInRange(
                        now, now, dls.getAutoDownloaders()
                    )

                    completer.set(ListenableWorker.Result.success())
                    Log.i(TAG, "Download success.")

                    // This is used to tell BrowseActivity that
                    // puzzles may have been updated while paused.
                    settings.setBrowseNewPuzzle(true)
                } catch (e : Exception) {
                    Log.i(TAG, "Download exception: ")
                    completer.setException(e)
                }
            }
        }
    }
}

class HourlyDownloadWorker(
    context : Context,
    params : WorkerParameters,
    utils : AndroidVersionUtils,
    settings : ForkyzSettings,
    fileHandlerProvider : FileHandlerProvider,
    downloadersProvider : DownloadersProvider,
) : BaseDownloadWorker(
    context,
    params,
    utils,
    settings,
    fileHandlerProvider,
    downloadersProvider,
) {
    override fun startWork() : ListenableFuture<ListenableWorker.Result> {
        Log.i(TAG, "Starting hourly download.")
        return CallbackToFutureAdapter.getFuture { completer ->
            doDownload(completer)
            "BaseDownloadWorker.doDownload"
        }
    }
}

class DailyDownloadWorker(
    context : Context,
    params : WorkerParameters,
    utils : AndroidVersionUtils,
    settings : ForkyzSettings,
    fileHandlerProvider : FileHandlerProvider,
    downloadersProvider : DownloadersProvider,
    private val manager : BackgroundDownloadManager,
) : BaseDownloadWorker(
    context,
    params,
    utils,
    settings,
    fileHandlerProvider,
    downloadersProvider,
) {
    override fun startWork() : ListenableFuture<ListenableWorker.Result> {
        Log.i(TAG, "Starting daily download.")
        return CallbackToFutureAdapter.getFuture { completer ->
            settings.getBackgroundDownloadSettings { bgSettings ->
                manager.scheduleNextDailyDownload(bgSettings)
            }
            doDownload(completer)
            "BaseDownloadWorker.doDownload"
        }
    }
}

