
package app.crossword.yourealwaysbe.forkyz

import java.io.IOException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

import org.jetbrains.compose.resources.stringResource

import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_not_implemented
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.app_name
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.exporting_puzzles
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.feature_not_implemented
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_export_failed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.puzzles_exported
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.select_puzzle_files
import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.injection.jvmModules
import app.crossword.yourealwaysbe.forkyz.injection.sharedModules
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.JVMInputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.NativeImportFinishedResult
import app.crossword.yourealwaysbe.forkyz.util.Notifications
import app.crossword.yourealwaysbe.forkyz.util.ShareFileData
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.exportStorage
import app.crossword.yourealwaysbe.forkyz.util.toUri

val TOAST_PANEL_PADDING = 30.dp
val TOAST_OUTER_PADDING = 2.dp
val TOAST_INNER_PADDING = 10.dp
val TOAST_SHAPE = RoundedCornerShape(10.dp)
val TOAST_DURATION = 5000L // ms

fun main() = application {
    val themeHelper : ThemeHelper by inject(ThemeHelper::class.java)
    val fileHandlerProvider : FileHandlerProvider
        by inject(FileHandlerProvider::class.java)
    val backendUtils : NativeBackendUtils
        by inject(NativeBackendUtils::class.java)
    val notifications : Notifications
        by inject(Notifications::class.java)

    val windowState = rememberWindowState(
        width = 600.dp,
        height = 1000.dp,
    )
    val activeToasts = remember { mutableStateListOf<String>() }
    val density = androidx.compose.ui.unit.Density(density = 1.0F)
    val featureNotImplemented
        = stringResource(Res.string.feature_not_implemented)
    val announceNotImplemented
        = stringResource(Res.string.announce_not_implemented)

    val utils = object : NativeFrontendUtils {
        override val inputConnectionMediator = JVMInputConnectionMediator()

        override val displayWidth : Int
            get() {
                return with(density) { windowState.size.width.roundToPx() }
            }
        override val displayHeight : Int
            get() {
                return with(density) { windowState.size.height.roundToPx() }
            }
        override val isPortrait : Boolean
            get() { return displayWidth < displayHeight }

        override fun onFinish() {
            exitApplication()
        }

        override fun onExit() {
            exitApplication()
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun toast(msg : String) {
            activeToasts.add(msg)
            GlobalScope.launch(Dispatchers.Main) {
                delay(TOAST_DURATION)
                activeToasts.removeFirst()
            }
        }

        override fun openAppSettings() {
            // noop, no such thing on desktop
        }

        override fun getPendingImports() : Collection<Uri> {
            // never any pending
            return listOf()
        }

        override fun clearPendingImports() {
            // noop, see getPendingImports
        }

        override fun onCheckAndWarnNetworkState() {
            // assume always connected
        }

        override fun onCheckRequestNotificationPermissions() {
            // permissions not needed
        }

        override fun onImportFinish(ifr : NativeImportFinishedResult) {
            // noop, since no pending imports (see getPendingImports)
        }

        override fun handleExternalToolEvent(tool : ExternalToolData) {
            notifyFeatureNotImplemented()
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun onExportPuzzles(uri : Uri) {
            fileHandlerProvider.get() { fileHandler ->
                GlobalScope.launch(Dispatchers.Main) {
                    val exportingPuzzles
                        = backendUtils.getString(Res.string.exporting_puzzles)
                    toast(exportingPuzzles)
                }
                GlobalScope.launch(Dispatchers.IO) {
                    var success = false
                    try {
                        success = exportStorage(backendUtils, fileHandler, uri)
                    } catch (e : IOException) {
                        // fall through
                    }
                    GlobalScope.launch(Dispatchers.Main) {
                        val msg = if (success) {
                            backendUtils.getString(Res.string.puzzles_exported)
                        } else {
                            backendUtils.getString(
                                Res.string.puzzles_export_failed,
                            )
                        }
                        toast(msg)
                    }
                }
            }
        }

        override fun onShareFile(data : ShareFileData) {
            notifyFeatureNotImplemented()
        }

        override fun setOnAppNotFoundCallback(
            cb : ((AppNotFoundException) -> Unit)?,
        ) {
            // noop
        }

        override fun setOnImportCompleteCallback(cb : ((Boolean) -> Unit)?) {
            // noop since no pending imports, see getPendingImports
        }

        override fun setOnNotificationPermissionDeniedCallback(
            cb : (() -> Unit)?,
        ) {
            // noop, since permissions not needed, see
            // onCheckRequestNotificationPermissions
        }

        override fun setStatusBarColor(color : Color) {
            // noop, since no status bar
        }

        override fun setOnVoiceCommandCallback(
            cb : ((List<String>?) -> Unit)?,
        ) {
            // noop
        }

        override fun launchVoiceInput() {
            notifyFeatureNotImplemented()
        }

        override fun announce(text : CharSequence) {
            toast(announceNotImplemented)
        }

        override fun onSetFullScreen() {
            windowState.placement = WindowPlacement.Fullscreen
        }

        override fun startShakeDetector(onHearShake : () -> Unit) {
            // noop, no plans to implement on desktop
        }

        override fun stopShakeDetector() {
            // noop, no plans to implement on desktop
        }

        override fun resumeShakeDetector() {
            // noop, no plans to implement on desktop
        }

        override fun pauseShakeDetector() {
            // noop, no plans to implement on desktop
        }

        private fun notifyFeatureNotImplemented() {
            toast(featureNotImplemented)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            val notification = notifications.notifications.receive()
            val msg = listOf(
                notification.title,
                notification.message,
            ).filterNotNull().joinToString("\n")
            utils.toast(msg)
        }
    }

    @Composable
    fun Toasts(modifier : Modifier) {
        Column(modifier = modifier) {
            activeToasts.forEach { msg ->
                key (msg) {
                    Text(
                        modifier = Modifier
                            .padding(TOAST_OUTER_PADDING)
                            .clip(TOAST_SHAPE)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                            )
                            .padding(TOAST_INNER_PADDING),
                        text = msg,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }

    startKoin {
        modules(sharedModules, jvmModules)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
        state = windowState,
        undecorated = true,
        resizable = true,
    ) {
        Box() {
            ForkyzApp(
                themeHelper = themeHelper,
                utils = utils,
            )

            themeHelper.AppTheme {
                Toasts(
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(TOAST_PANEL_PADDING),
                )
            }
        }
    }
}

