
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlinx.coroutines.runBlocking

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

import org.jetbrains.compose.resources.getString

import org.koin.android.ext.android.inject

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.mcp_server_notification_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.mcp_stop
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

class MCPService : Service() {
    companion object {
        const val ACTION_EXIT = "app.crossword.yourealwaysbe.forkyz.MCP_EXIT"
        const val EXTRA_HOST_PORT = "hostPort"
        const val NOTIFICATION_CHANNEL_ID = "forkyz.mcp"
        private const val NOTIFICATION_ID = 10105
    }

    private val mcpBoardManager : MCPBoardManager by inject()
    private val versionUtils : AndroidVersionUtils by inject()

    override fun onBind(intent : Intent): IBinder? = null

    override fun onStartCommand(
        intent : Intent?,
        flags : Int,
        startId : Int,
    ) : Int {
        if (intent?.action == ACTION_EXIT) {
            mcpBoardManager.stop()
            return START_NOT_STICKY
        }

        val hostPort = intent?.getStringExtra(EXTRA_HOST_PORT)
            ?: return START_NOT_STICKY

        val (title, exitLabel) = runBlocking {
            Pair(
                getString(Res.string.mcp_server_notification_title),
                getString(Res.string.mcp_stop),
            )
        }

        val exitPendingIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, MCPService::class.java).setAction(ACTION_EXIT),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(MCPBoard.getUrlFromHostPort(hostPort))
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                exitLabel,
                exitPendingIntent,
            ).build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            versionUtils.foregroundServiceTypeDataSync(),
        )

        mcpBoardManager.startService(hostPort)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        mcpBoardManager.stopServiceBlocking()
        super.onDestroy()
    }
}

