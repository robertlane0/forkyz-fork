
package app.crossword.yourealwaysbe.forkyz.util

import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

import org.jetbrains.compose.resources.getString

import org.koin.android.ext.android.inject

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.exporting_puzzles
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_export_failed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_exported
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.exportStorage
import app.crossword.yourealwaysbe.forkyz.util.toUri
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

class PuzzleExportService() : Service() {
    companion object {
        val PUZZLE_EXPORT_URI = "puzzleExportURI"
        final val NOTIFICATION_CHANNEL_ID = "forkyz.export"
        private final val NOTIFICATION_ID = 10103
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val ioScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private val mainScope = CoroutineScope(Dispatchers.Main)

    // TODO: koin application context?
    private val context : Context by inject()
    private val versionUtils : AndroidVersionUtils by inject()
    private val fileHandlerProvider : FileHandlerProvider by inject()
    private val utils : NativeBackendUtils by inject()

    override fun onBind(intent : Intent): IBinder? {
        return null
    }

    override fun onStartCommand(
        intent : Intent?,
        flags : Int,
        startId : Int,
    ) : Int {
        val uri = intent?.getStringExtra(PUZZLE_EXPORT_URI)?.toUri()
        if (intent == null || uri == null)
            return START_NOT_STICKY

        // can't really launch before notification
        val title = runBlocking { getString(Res.string.exporting_puzzles) }
        val notification = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID,
        ).setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle(title)
            .setWhen(System.currentTimeMillis())
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            versionUtils.foregroundServiceTypeDataSync(),
        )

        mainScope.launch {
            Toast.makeText(
                context,
                getString(Res.string.exporting_puzzles),
                Toast.LENGTH_SHORT
            ).show()
        }

        exportPuzzles(uri) { success ->
            val resultMsg = if (success)
                Res.string.puzzles_exported
            else
                Res.string.puzzles_export_failed

            mainScope.launch {
                Toast.makeText(
                    context,
                    getString(resultMsg),
                    Toast.LENGTH_SHORT
                ).show()

                stopSelf()
            }
        }

        // don't re-export if killed
        return START_NOT_STICKY
    }

    private fun exportPuzzles(uri : Uri, cb : (Boolean) -> Unit = { }) {
        fileHandlerProvider.get() { fileHandler ->
            ioScope.launch(Dispatchers.IO) {
                var success = false
                try {
                    success = exportStorage(
                        utils,
                        fileHandler,
                        uri,
                    )
                } catch (e : IOException) {
                    // fall through
                }
                mainScope.launch {
                    cb(success)
                }
            }
        }
    }
}

