
package app.crossword.yourealwaysbe.forkyz.util

import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri

import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.exportStorage
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

@AndroidEntryPoint
class PuzzleExportService() : Service() {
    companion object {
        val PUZZLE_EXPORT_URI = "puzzleExportURI"

        @JvmField
        final val NOTIFICATION_CHANNEL_ID = "forkyz.export"
        private final val NOTIFICATION_ID = 10103
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val ioScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private val mainScope = CoroutineScope(Dispatchers.Main)

    @Inject
    @ApplicationContext
    lateinit var context : Context

    @Inject
    lateinit var utils : AndroidVersionUtils

    @Inject
    lateinit var fileHandlerProvider : FileHandlerProvider

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

        val notification = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID,
        ).setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentTitle(context.getString(R.string.exporting_puzzles))
            .setWhen(System.currentTimeMillis())
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            utils.foregroundServiceTypeDataSync(),
        )

        Toast.makeText(
            context,
            R.string.exporting_puzzles,
            Toast.LENGTH_SHORT
        ).show()

        exportPuzzles(uri) { success ->
            val resultMsg = if (success)
                R.string.puzzles_exported
            else
                R.string.puzzles_export_failed

            Toast.makeText(
                context,
                resultMsg,
                Toast.LENGTH_SHORT
            ).show()

            stopSelf()
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
                        context,
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

