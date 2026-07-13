
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.launch

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.net.Uri as AndroidUri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.net.toUri as toAndroidUri
import androidx.lifecycle.lifecycleScope

import org.jetbrains.compose.resources.getString

import org.koin.android.ext.android.inject

import com.squareup.seismic.ShakeDetector

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_but_no_active_network
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_speech_recognition_available
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.notifications_request_rationale
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_export_failed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.speech_not_ready
import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolLauncher
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.AndroidInputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.NativeImportFinishedResult
import app.crossword.yourealwaysbe.forkyz.util.NightModeHelper
import app.crossword.yourealwaysbe.forkyz.util.OrientationHelper
import app.crossword.yourealwaysbe.forkyz.util.PuzzleExportService
import app.crossword.yourealwaysbe.forkyz.util.ShareFileData
import app.crossword.yourealwaysbe.forkyz.util.SpeechContract
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toAndroidUri
import app.crossword.yourealwaysbe.forkyz.util.toForkyzUri
import app.crossword.yourealwaysbe.forkyz.util.toUri
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

private val TAG = "ForkyzActivity"

/**
 * Notification that intent imports have finished
 *
 * Esp if URL download, the activity might get restarted due to
 * config changes while the import is happening. To handle this, the
 * compete message is broadcast to the current version of the
 * activity.
 */
private val INTENT_IMPORT_COMPLETE_ACTION
    = "app.crossword.yourealwaysbe.INTENT_IMPORT_COMPLETE_ACTION"

/**
 * ID of task that completed intent import
 */
private val INTENT_IMPORT_TASK_ID
    = "app.crossword.yourealwaysbe.INTENT_IMPORT_TASK_ID"

/**
 * Request other instances close
 *
 * An intent to broadcast to tell all other Forkyz activities to close
 * down (avoid multiple instances on same file system)
 *
 * launchMode singleTask is not really what we want here as it
 * prevents the user from returning to a puzzle from the home screen
 * (annoying)
 */
val FORKYZ_CLOSE_ACTION = "app.crossword.yourealwaysbe.FORKYZ_CLOSE_ACTION"

/**
 * The task ID of the ForkyzActivity requesting the close
 *
 * To avoid closing self
 */
val FORKYZ_CLOSE_TASK_ID = "app.crossword.yourealwaysbe.FORKYZ_CLOSE_TASK_ID"

/**
 * Whether some succeeded (bool)
 */
val INTENT_IMPORT_SUCCESS = "app.crossword.yourealwaysbe.INTENT_IMPORT_SUCCESS"

/**
 * For ex/importing settings
 */
val SETTINGS_MIME_TYPE = "application/json"

open class ForkyzActivity : AppCompatActivity(), ShakeDetector.Listener {
    private val versionUtils : AndroidVersionUtils by inject()
    private val themeHelper : ThemeHelper by inject()
    // Only used for setting nightmode for non-compose views
    // breaking mvvm here -- rather than introduce a
    // ForkyzActivityViewModel for this legacy band-aid.
    // TODO: remove with NightModeHelper once all colors from Compose
    private val settings : ForkyzSettings by inject()
    private val orientationHelper : OrientationHelper by inject()

    private var nightMode : NightModeHelper? = null
    private lateinit var nm : NotificationManagerCompat
    private var pendingImports : Collection<Uri> = listOf()
    private var ttsService : TextToSpeech? = null
    private var ttsReady : Boolean = false
    private var shakeDetector : ShakeDetector? = null
    private var speechReqCount = 0

    // callbacks from Compose
    private var onImportComplete : ((Boolean) -> Unit)? = null
    private var onNotificationPermissionDenied : (() -> Unit)? = null
    private var onHearShake : (() -> Unit)? = null
    private var onVoiceCommand : ((List<String>?) -> Unit)? = null
    private var onAppNotFound : ((AppNotFoundException) -> Unit)? = null

    /**
     * See note for FORKYZ_CLOSE_ACTION
     */
    private val closeActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context : Context?, intent : Intent?) {
            if (intent?.action == FORKYZ_CLOSE_ACTION) {
                val myTaskId = (this@ForkyzActivity).taskId
                val otherTaskId
                    = intent.getIntExtra(FORKYZ_CLOSE_TASK_ID, myTaskId)

                if (myTaskId != otherTaskId) {
                    (this@ForkyzActivity).finishAndRemoveTask()
                }
            }
        }
    }

    /**
     * See note for INTENT_IMPORT_COMPLETE_ACTION
     */
    private val intentImportCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context : Context?, intent : Intent?) {
            if (intent?.action == INTENT_IMPORT_COMPLETE_ACTION) {
                val myTaskId = (this@ForkyzActivity).taskId
                val otherTaskId
                    = intent.getIntExtra(INTENT_IMPORT_TASK_ID, myTaskId)

                if (myTaskId == otherTaskId) {
                    onImportComplete?.let {
                        it(intent.getBooleanExtra(INTENT_IMPORT_SUCCESS, false))
                    }
                }
            }
        }
    }

    /**
     * When POST_NOTIFICATIONS permission needed
     */
    private val notificationPermissionLauncher
        = registerForActivityResult(RequestPermission()) { isGranted ->
            if (!isGranted)
                onNotificationPermissionDenied?.let { it() }
        }

    private val voiceInputLauncher : ActivityResultLauncher<String> =
        registerForActivityResult(SpeechContract()) { text ->
            onVoiceCommand?.let { it(text) }
        }

    override protected fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        orientationHelper.applyOrientation(this)

        nm = NotificationManagerCompat.from(this)

        // ask others to close down
        sendBroadcast(
            Intent(FORKYZ_CLOSE_ACTION).putExtra(
                FORKYZ_CLOSE_TASK_ID, getTaskId()
            ).setPackage(getPackageName())
        )

        // listen for broadcasts
        ContextCompat.registerReceiver(
            this,
            closeActionReceiver,
            IntentFilter(FORKYZ_CLOSE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            this,
            intentImportCompleteReceiver,
            IntentFilter(INTENT_IMPORT_COMPLETE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent() {
            ForkyzApp(
                themeHelper = themeHelper,
                utils = utils,
            )
        }

        handleCreateIntent(savedInstanceState);
    }

    override protected fun onPause() {
        ttsService?.let {
            it.shutdown()
            ttsService = null
            ttsReady = false
        }
        super.onPause()
    }

    override protected fun onResume() {
        super.onResume()
        if (nightMode == null) {
            nightMode = NightModeHelper.bind(this, settings)
            nightMode?.restoreNightMode()
        }
    }

    override fun hearShake() {
        onHearShake?.let { it() }
    }

    private fun setPendingImport(uri : Uri?) {
        uri?.let { setPendingImports(listOf(it)) }
    }

    private fun setPendingImports(uri : Collection<Uri>) {
        pendingImports = uri
    }

    private fun doExportPuzzles(uri : Uri) {
        try {
            AppLogger.i(TAG, "Starting puzzle export")
            val intent = Intent(this, PuzzleExportService::class.java)
            intent.putExtra(
                PuzzleExportService.PUZZLE_EXPORT_URI,
                uri.toString()
            )
            ContextCompat.startForegroundService(this, intent)
        // use IllegalState as
        // ForegroundServiceNotAllowedException requires API 31
        } catch (e : IllegalStateException) {
            AppLogger.i(
                TAG,
                "Could not start puzzle export service on foreground"
            )
            lifecycleScope.launch {
                utils.toast(getString(Res.string.puzzles_export_failed))
            }
        }
    }

    /**
     * Handle create intent given saved state
     *
     * Includes reading pending imports from saved instance state
     */
    private fun handleCreateIntent(savedInstanceState : Bundle?) {
        // if there's a saved state, then this is not the first time we've seen
        // the intent, so ignore it
        if (savedInstanceState != null)
            return

        // If this was started by a file open or a share
        val intent = getIntent()
        val action = intent.action
        if (Intent.ACTION_VIEW == action) {
            // loaded by onResume
            setPendingImport(intent.data?.toForkyzUri())
        } else if (Intent.ACTION_SEND == action) {
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                setPendingImport(
                    intent.getStringExtra(Intent.EXTRA_TEXT)?.toUri(),
                )
            } else {
                setPendingImport(
                    IntentCompat.getParcelableExtra(
                        intent,
                        Intent.EXTRA_STREAM,
                        android.net.Uri::class.java,
                    )?.let { it.toForkyzUri() },
                )
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action) {
            IntentCompat.getParcelableArrayListExtra(
                intent,
                Intent.EXTRA_STREAM,
                Uri::class.java,
            )?.let(this::setPendingImports)
        }
    }

    private val utils = object : NativeFrontendUtils {
        override val inputConnectionMediator = AndroidInputConnectionMediator()

        override val displayWidth : Dp by lazy {
            val metrics = getResources().getDisplayMetrics()
            (metrics.widthPixels / metrics.density).dp
        }

        override val displayHeight : Dp by lazy {
            val metrics = getResources().getDisplayMetrics()
            (metrics.heightPixels / metrics.density).dp
        }

        override val isPortrait : Boolean by lazy {
            displayWidth <= displayHeight
        }

        // exit after pending imports to avoid multiple copies of the app
        // running, which is hella confusing
        override val isExitAfterPendingImports : Boolean = true
        override val hasExitMenuItem : Boolean = false

        override fun onFinishByBack() { finish() }

        override fun onExit() {
            // ask all Forkyz activities to close
            val close = Intent(FORKYZ_CLOSE_ACTION)
            // -1 so all Forkyz activities close
            close.putExtra(FORKYZ_CLOSE_TASK_ID, -1)
            close.setPackage(getPackageName())
            sendBroadcast(close)
            finish()
        }

        override fun toast(msg : String) {
            Toast.makeText(getApplication(), msg, Toast.LENGTH_LONG).show()
        }

        override fun openAppSettings() {
            val appPackage = getPackageName()
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                ("package:" + appPackage).toAndroidUri(),
            )
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        override fun getPendingImports() : Collection<Uri> {
            return pendingImports
        }

        override fun clearPendingImports() {
            pendingImports = listOf()
        }

        override fun onCheckAndWarnNetworkState() {
            if (!versionUtils.hasNetworkConnection(this@ForkyzActivity)) {
                lifecycleScope.launch {
                    toast(getString(Res.string.download_but_no_active_network))
                }
            }
        }

        /**
         * Request notification permissions if needed
         *
         * E.g. not if settings block them. Doesn't ask twice in an
         * activity's life
         */
        override fun onCheckRequestNotificationPermissions() {
            if (nm.areNotificationsEnabled())
                return

            val showRationale
                = versionUtils.shouldShowRequestNotificationPermissionRationale(
                    this@ForkyzActivity
                )

            if (showRationale) {
                lifecycleScope.launch {
                    toast(getString(Res.string.notifications_request_rationale))
                }
            }

            versionUtils.requestPostNotifications(
                notificationPermissionLauncher,
            )
        }

        override fun onImportFinish(ifr : NativeImportFinishedResult) {
            sendBroadcast(
                Intent(INTENT_IMPORT_COMPLETE_ACTION).putExtra(
                    INTENT_IMPORT_TASK_ID,
                    taskId,
                ).putExtra(
                    INTENT_IMPORT_SUCCESS,
                    !ifr.someFailed && ifr.someSucceeded
                ).setPackage(getPackageName())
            )
        }

        override fun onExportPuzzles(uri : Uri) {
            doExportPuzzles(uri)
        }

        /**
         * Share a file through Android share
         */
        override fun onShareFile(data : ShareFileData) {
            val shareIntent = ShareCompat.IntentBuilder(this@ForkyzActivity)
                .setChooserTitle(data.title)
                .setStream(data.uri.toAndroidUri())
                .setType(data.mimeType)
                .createChooserIntent()
            startActivity(shareIntent)
        }

        override fun handleExternalToolEvent(tool : ExternalToolData) {
            lifecycleScope.launch {
                try {
                    tool.accept(ExternalToolLauncher(this@ForkyzActivity))
                } catch (e : AppNotFoundException) {
                    onAppNotFound?.let { it(e) }
                }
            }
        }


        override fun setOnAppNotFoundCallback(
            cb : ((AppNotFoundException) -> Unit)?,
        ) {
            onAppNotFound = cb
        }

        override fun removeOnAppNotFoundCallback(
            cb : ((AppNotFoundException) -> Unit)?,
        ) {
            if (onAppNotFound == cb)
                onAppNotFound = null
        }

        override fun setOnImportCompleteCallback(cb : ((Boolean) -> Unit)?) {
            onImportComplete = cb
        }

        override fun removeOnImportCompleteCallback(cb : ((Boolean) -> Unit)?) {
            if (onImportComplete == cb)
                onImportComplete = null
        }

        override fun setOnNotificationPermissionDeniedCallback(
            cb : (() -> Unit)?,
        ) {
            onNotificationPermissionDenied = cb
        }

        override fun removeOnNotificationPermissionDeniedCallback(
            cb : (() -> Unit)?,
        ) {
            if (onNotificationPermissionDenied == cb)
                onNotificationPermissionDenied = null
        }

        override fun setStatusBarColor(color : Color) {
            versionUtils.legacySetStatusBarColor(
                this@ForkyzActivity,
                color.toArgb(),
            )
        }

        override fun setOnVoiceCommandCallback(
            cb : ((List<String>?) -> Unit)?,
        ) {
            onVoiceCommand = cb
        }

        override fun removeOnVoiceCommandCallback(
            cb : ((List<String>?) -> Unit)?,
        ) {
            if (onVoiceCommand == cb)
                onVoiceCommand = null
        }

        override fun launchVoiceInput() {
            try {
                voiceInputLauncher.launch(
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
            } catch (e : ActivityNotFoundException) {
                lifecycleScope.launch {
                   toast(getString(Res.string.no_speech_recognition_available))
                }
            }
        }

        override fun announce(text : CharSequence) {
            if (ttsService == null) {
                ttsService = TextToSpeech(
                    applicationContext,
                    { status ->
                        if (status == TextToSpeech.SUCCESS) {
                            ttsReady = true
                            announce(text)
                        }
                    }
                )
            } else if (!ttsReady) {
                // hopefully rare occasion where tts being prepared but not
                // ready yet
                lifecycleScope.launch {
                    toast(getString(Res.string.speech_not_ready))
                }
            } else {
                ttsService?.speak(
                    text, TextToSpeech.QUEUE_FLUSH, null,
                    "ForkyzSpeak_" + (speechReqCount++)
                );
            }
        }

        override fun onSetFullScreen() {
            versionUtils.setFullScreen(getWindow())
        }

        override fun startShakeDetector(onHearShake : () -> Unit) {
            this@ForkyzActivity.onHearShake = onHearShake
            if (shakeDetector == null)
                shakeDetector = ShakeDetector(this@ForkyzActivity)
            resumeShakeDetector()
        }

        override fun resumeShakeDetector() {
            shakeDetector?.let {
                it.start(
                    getSystemService(SENSOR_SERVICE) as SensorManager,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }

        override fun pauseShakeDetector() {
            shakeDetector?.let { sd ->
                sd.stop()
                val manager = (getSystemService(SENSOR_SERVICE) as SensorManager)
                manager.unregisterListener(sd)
            }
        }

        override fun stopShakeDetector() {
            pauseShakeDetector()
            shakeDetector = null
            onHearShake = null
        }

        override val canShareFiles : Boolean = true
    }
}

