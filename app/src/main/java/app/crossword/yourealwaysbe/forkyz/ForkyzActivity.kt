
package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
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
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.net.toUri

import dagger.hilt.android.AndroidEntryPoint

import com.squareup.seismic.ShakeDetector

import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolLauncher
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.NightModeHelper
import app.crossword.yourealwaysbe.forkyz.util.OrientationHelper
import app.crossword.yourealwaysbe.forkyz.util.PuzzleExportService
import app.crossword.yourealwaysbe.forkyz.util.SpeechContract
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

private val TAG = "ForkyzActivity"

/**
 * Data for sharing a file using Android share
 */
data class ShareFileData(
    val title : String,
    val uri : Uri,
    val mimeType : String,
)

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

// allow import of all docs (parser will take care of detecting if it's a
// puzzle that's recognised)
private val IMPORT_MIME_TYPE =  "*/*"

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
@JvmField
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
val SETTINGS_DEFAULT_FILENAME = "forkyz_settings.json"

@AndroidEntryPoint
open class ForkyzActivity : AppCompatActivity(), ShakeDetector.Listener {

    @Inject
    lateinit var themeHelper : ThemeHelper

    @Inject
    lateinit var orientationHelper : OrientationHelper

    @Inject
    lateinit var utils : AndroidVersionUtils

    // Only used for setting nightmode for non-compose views
    // breaking mvvm here -- rather than introduce a
    // ForkyzActivityViewModel for this legacy band-aid.
    // TODO: remove with NightModeHelper once all colors from Compose
    @Inject
    lateinit var settings : ForkyzSettings

    private var nightMode : NightModeHelper? = null
    private lateinit var nm : NotificationManagerCompat
    private var pendingImports : Collection<Uri> = listOf()
    protected val inputConnectionMediator = InputConnectionMediator()
    private var ttsService : TextToSpeech? = null
    private var ttsReady : Boolean = false
    private var shakeDetector : ShakeDetector? = null
    private var speechReqCount = 0

    // callbacks from Compose
    private var onImportComplete : ((Boolean) -> Unit)? = null
    private var onImportURIs : ((Collection<Uri>) -> Unit)? = null
    private var onNotificationPermissionDenied : (() -> Unit)? = null
    private var onHearShake : (() -> Unit)? = null
    private var onVoiceCommand : ((List<String>?) -> Unit)? = null
    private var onSAFURI : ((Uri) -> Unit)? = null
    private var onExportSettingsURI : ((Uri) -> Unit)? = null
    private var onImportSettingsURI : ((Uri) -> Unit)? = null
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

    private val getImportURI = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents(),
        ActivityResultCallback<List<Uri>>() { uris ->
            onImportURIs?.let { it(uris) }
        },
    )

    /**
     * When POST_NOTIFICATIONS permission needed
     */
    private val notificationPermissionLauncher
        = registerForActivityResult(RequestPermission()) { isGranted ->
            if (!isGranted)
                onNotificationPermissionDenied?.let { it() }
        }

    private val displayWidth : Int by lazy {
        val metrics = getResources().getDisplayMetrics()
        (metrics.widthPixels / metrics.density).toInt()
    }

    private val displayHeight : Int by lazy {
        val metrics = getResources().getDisplayMetrics()
        (metrics.heightPixels / metrics.density).toInt()
    }

    private val isPortrait : Boolean by lazy {
        displayWidth <= displayHeight
    }

    private val voiceInputLauncher : ActivityResultLauncher<String> =
        registerForActivityResult(SpeechContract()) { text ->
            onVoiceCommand?.let { it(text) }
        }

    private val getSAFURI = registerForActivityResult(OpenDocumentTree()) {
        uri : Uri? -> uri?.let { uri ->
            onSAFURI?.let { it(uri) }
        }
    }

    private val importSettings = registerForActivityResult(GetContent()) {
        uri : Uri? -> uri?.let { uri ->
            onImportSettingsURI?.let { it(uri) }
        }
    }

    private val exportSettings = registerForActivityResult(
        CreateDocument(SETTINGS_MIME_TYPE)
    ) {
        uri : Uri? -> uri?.let { uri ->
            onExportSettingsURI?.let { it(uri) }
        }
    }

    private val exportPuzzles
        = registerForActivityResult(OpenDocumentTree()) {
            uri : Uri? -> uri?.let {
                try {
                    Log.i(TAG, "Starting puzzle export")
                    val intent = Intent(this, PuzzleExportService::class.java)
                    intent.putExtra(
                        PuzzleExportService.PUZZLE_EXPORT_URI,
                        uri.toString()
                    )
                    ContextCompat.startForegroundService(this, intent)
                // use IllegalState as
                // ForegroundServiceNotAllowedException requires API 31
                } catch (e : IllegalStateException) {
                    Log.i(
                        TAG,
                        "Could not start puzzle export service on foreground"
                    )
                    toast(getString(R.string.puzzles_export_failed))
                }
            }
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
                toast = this::toast,
                inputConnectionMediator = inputConnectionMediator,
                displayWidth = displayWidth,
                displayHeight = displayHeight,
                isPortrait = isPortrait,
                openAppSettings = this::openAppSettings,
                openURI = this::openURI,
                getPendingImports = this::getPendingImports,
                clearPendingImports = this::clearPendingImports,
                onFinish = this::finish,
                onCheckAndWarnNetworkState
                    = this::checkAndWarnNetworkState,
                onCheckRequestNotificationPermissions
                    = this::checkRequestNotificationPermissions,
                onImport = { getImportURI.launch(IMPORT_MIME_TYPE) },
                onImportFinish = this::onImportFinish,
                onExit = this::doExit,
                onGetSAFURI = { getSAFURI.launch(null) },
                onExportSettings
                    = { exportSettings.launch(SETTINGS_DEFAULT_FILENAME) },
                onImportSettings
                    = { importSettings.launch(SETTINGS_MIME_TYPE) },
                onExportPuzzles = { exportPuzzles.launch(null) },
                onShareFile = this::shareFile,
                handleExternalToolEvent = this::handleExternalToolEvent,
                setOnAppNotFoundCallback = { onAppNotFound = it },
                setOnImportCompleteCallback = { onImportComplete = it },
                setOnImportURIsCallback = { onImportURIs = it },
                setOnNotificationPermissionDeniedCallback = {
                    onNotificationPermissionDenied = it
                },
                setStatusBarColor = this::legacySetStatusBarColor,
                setSAFURICallback = { onSAFURI = it },
                setExportSettingsURICallback = { onExportSettingsURI = it },
                setImportSettingsURICallback = { onImportSettingsURI = it },
                setOnVoiceCommandCallback = { onVoiceCommand = it },
                launchVoiceInput = this::launchVoiceInput,
                announce = this::announce,
                onSetFullScreen = { utils.setFullScreen(getWindow()) },
                startShakeDetector = this::startShakeDetector,
                stopShakeDetector = this::stopShakeDetector,
                resumeShakeDetector = this::resumeShakeDetector,
                pauseShakeDetector = this::pauseShakeDetector,
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

    protected fun handleExternalToolEvent(tool : ExternalToolData) {
        try {
            tool.accept(ExternalToolLauncher(this))
        } catch (e : AppNotFoundException) {
            onAppNotFound?.let { it(e) }
        }
    }

    /**
     * Share a file through Android share
     */
    private fun shareFile(data : ShareFileData) {
        val shareIntent = ShareCompat.IntentBuilder(this)
            .setChooserTitle(data.title)
            .setStream(data.uri)
            .setType(data.mimeType)
            .createChooserIntent()
        startActivity(shareIntent)
    }

    /**
     * Display a long toast
     */
    private fun toast(message : String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()
    }

    private fun openAppSettings() {
        val appPackage = getPackageName()
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            ("package:" + appPackage).toUri(),
        )
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun legacySetStatusBarColor(color : Color) {
        utils.legacySetStatusBarColor(this, color.toArgb())
    }

    private fun checkAndWarnNetworkState() {
        if (!utils.hasNetworkConnection(this)) {
            toast(getString(R.string.download_but_no_active_network))
        }
    }

    private fun openURI(uri : Uri) {
        val i = Intent(Intent.ACTION_VIEW);
        i.setData(uri)
        startActivity(i);
    }

    /**
     * Request notification permissions if needed
     *
     * E.g. not if settings block them. Doesn't ask twice in an
     * activities life
     */
    private fun checkRequestNotificationPermissions() {
        if (nm.areNotificationsEnabled())
            return

        val showRationale
            = utils.shouldShowRequestNotificationPermissionRationale(
                this
            )

        if (showRationale)
            toast(getString(R.string.notifications_request_rationale))

        utils.requestPostNotifications(notificationPermissionLauncher)
    }

    private fun clearPendingImports() {
        pendingImports = listOf()
    }

    private fun setPendingImport(uri : Uri?) {
        uri?.let { setPendingImports(listOf(it)) }
    }

    private fun setPendingImports(uri : Collection<Uri>) {
        pendingImports = uri
    }

    private fun getPendingImports() : Collection<Uri> {
        return pendingImports
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
            setPendingImport(intent.data)
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
                        Uri::class.java,
                    ),
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

    private fun doExit() {
        // ask all Forkyz activities to close
        val close = Intent(FORKYZ_CLOSE_ACTION)
        // -1 so all Forkyz activities close
        close.putExtra(FORKYZ_CLOSE_TASK_ID, -1)
        close.setPackage(getPackageName())
        sendBroadcast(close)
        finish()
    }

    private fun onImportFinish(ifr : ImportFinishResult) {
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

    protected fun launchVoiceInput() {
        try {
            voiceInputLauncher.launch(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        } catch (e : ActivityNotFoundException) {
           toast(getString(R.string.no_speech_recognition_available))
        }
    }

    /**
     * Announce text with accessibility if running or tts
     *
     * @param data what to announce
     */
    protected fun announce(data : AnnounceData) {
        val text = data.message
        if (ttsService == null) {
            ttsService = TextToSpeech(
                applicationContext,
                { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        ttsReady = true
                        announce(data)
                    }
                }
            )
        } else if (!ttsReady) {
            // hopefully rare occasion where tts being prepared but not
            // ready yet
            Toast.makeText(
                this,
                R.string.speech_not_ready,
                Toast.LENGTH_SHORT,
            ).show()
        } else {
            ttsService?.speak(
                text, TextToSpeech.QUEUE_FLUSH, null,
                "ForkyzSpeak_" + (speechReqCount++)
            );
        }
    }

    override fun hearShake() {
        onHearShake?.let { it() }
    }

    private fun startShakeDetector(onHearShake : () -> Unit) {
        this.onHearShake = onHearShake
        if (shakeDetector == null)
            shakeDetector = ShakeDetector(this)
        resumeShakeDetector()
    }

    private fun resumeShakeDetector() {
        shakeDetector?.let {
            it.start(
                getSystemService(SENSOR_SERVICE) as SensorManager,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }

    private fun pauseShakeDetector() {
        shakeDetector?.let { sd ->
            sd.stop()
            val manager = (getSystemService(SENSOR_SERVICE) as SensorManager)
            manager.unregisterListener(sd)
        }
    }

    private fun stopShakeDetector() {
        pauseShakeDetector()
        shakeDetector = null
        onHearShake = null
    }
}

