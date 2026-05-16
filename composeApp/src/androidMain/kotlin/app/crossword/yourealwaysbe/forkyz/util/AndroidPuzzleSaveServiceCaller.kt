
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.coroutines.runBlocking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

import org.jetbrains.compose.resources.getString

import org.koin.android.ext.android.inject

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.saving_puzzle
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

private val TAG = "ForkyzSaveSrvcCllr";

class AndroidPuzzleSaveServiceCaller(
    val context : Context,
) : PuzzleSaveServiceCaller {
    override fun saveBoard(currentPuzzleHolder : CurrentPuzzleHolder) {
        // If calling from onPause the app is usually in the foreground,
        // except if onPause is being called during the wakeup cycle, in
        // which case you get paused without ever being foregrounded, so you
        // get an exception.
        try {
            AppLogger.i(TAG, "Saving board on foreground")
            ContextCompat.startForegroundService(
                context,
                Intent(context, PuzzleSaveService::class.java),
            )
        // use IllegalState as ForegroundServiceNotAllowedException
        // requires API 31
        } catch (e : IllegalStateException) {
            AppLogger.i(
                TAG,
                "Save on foreground not allowed, save in background"
            )
            currentPuzzleHolder.saveBoardService()
        }
    }
}

class PuzzleSaveService() : Service() {
    companion object {
        final val NOTIFICATION_CHANNEL_ID = "forkyz.save"
        private final val NOTIFICATION_ID = 10102
    }

    private val context : Context by inject()
    private val currentPuzzleHolder : CurrentPuzzleHolder by inject()
    private val utils : AndroidVersionUtils by inject()

    override fun onBind(intent : Intent): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent : Intent,
        flags : Int,
        startId : Int,
    ) : Int {
        // can't really launch coroutine here
        val savingPuzzle = runBlocking { getString(Res.string.saving_puzzle) }
        val notification = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID,
        ).setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle(savingPuzzle)
            .setWhen(TimeUtils.currentTimeMillis())
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            utils.foregroundServiceTypeShortService(),
        )

        currentPuzzleHolder.saveBoardService(cb = this::stopSelf)

        return START_STICKY
    }
}

