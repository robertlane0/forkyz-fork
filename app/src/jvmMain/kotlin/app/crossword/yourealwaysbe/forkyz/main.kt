
package app.crossword.yourealwaysbe.forkyz

import java.io.IOException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

import org.jetbrains.compose.resources.stringResource

import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.inject

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.app_name
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.exporting_puzzles
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
import app.crossword.yourealwaysbe.forkyz.util.ShareFileData
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.files.exportStorage
import app.crossword.yourealwaysbe.forkyz.util.toUri

fun main() = application {
    val themeHelper : ThemeHelper by inject(ThemeHelper::class.java)
    val fileHandlerProvider : FileHandlerProvider
        by inject(FileHandlerProvider::class.java)
    val backendUtils : NativeBackendUtils
        by inject(NativeBackendUtils::class.java)

    val windowState = rememberWindowState(
        width = 600.dp,
        height = 1000.dp,
    )

    val density = androidx.compose.ui.unit.Density(density = 1.0F)

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

        override fun toast(msg : String) {
            // TODO
        }

        override fun openAppSettings() {
            // noop, no such thing on desktop
        }

        override fun openURI(uri : Uri) {
            // TODO
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
            // TODO
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
            // TODO
        }

        override fun setOnAppNotFoundCallback(
            cb : ((AppNotFoundException) -> Unit)?,
        ) {
            // TODO
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
            // TODO
        }

        override fun launchVoiceInput() {
            // TODO
        }

        override fun announce(text : CharSequence) {
            // TODO
        }

        override fun onSetFullScreen() {
            // TODO
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
        ForkyzApp(
            themeHelper = themeHelper,
            utils = utils,
        )
    }
}

