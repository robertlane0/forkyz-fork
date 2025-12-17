
package app.crossword.yourealwaysbe.forkyz

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URISyntaxException

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.AwtWindow
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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.select_puzzle_files
import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.injection.jvmModules
import app.crossword.yourealwaysbe.forkyz.injection.sharedModules
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.JVMInputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.NativeImportFinishedResult
import app.crossword.yourealwaysbe.forkyz.util.ShareFileData
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toUri

fun main() = application {
    val themeHelper : ThemeHelper by inject(ThemeHelper::class.java)

    val windowState = rememberWindowState(
        width = 600.dp,
        height = 1000.dp,
    )
    var isImportOpen by remember { mutableStateOf(false) }
    var onImportURIsCallback : ((Collection<Uri>) -> Unit)? = remember { null }

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
            // TODO
        }

        override fun openURI(uri : Uri) {
            // TODO
        }

        override fun getPendingImports() : Collection<Uri> {
            return listOf()
        }

        override fun clearPendingImports() {
            // TODO
        }

        override fun onCheckAndWarnNetworkState() {
            // TODO
        }

        override fun onCheckRequestNotificationPermissions() {
            // TODO
        }

        override fun onImport() {
            isImportOpen = true
        }

        override fun onImportFinish(ifr : NativeImportFinishedResult) {
            // TODO
        }

        override fun handleExternalToolEvent(tool : ExternalToolData) {
            // TODO
        }

        override fun onGetSAFURI() {
            // TODO
        }

        override fun onExportSettings() {
            // TODO
        }

        override fun onImportSettings() {
            // TODO
        }

        override fun onExportPuzzles() {
            // TODO
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
            // TODO
        }

        override fun setOnImportURIsCallback(
            cb : ((Collection<Uri>) -> Unit)?,
        ) {
            onImportURIsCallback = cb
        }

        override fun setOnNotificationPermissionDeniedCallback(
            cb : (() -> Unit)?,
        ) {
            // TODO
        }

        override fun setStatusBarColor(color : Color) {
            // TODO
        }

        override fun setSAFURICallback(cb : ((Uri) -> Unit)?) {
            // TODO
        }

        override fun setExportSettingsURICallback(cb : ((Uri) -> Unit)?) {
            // TODO
        }

        override fun setImportSettingsURICallback(cb : ((Uri) -> Unit)?) {
            // TODO
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
            // TODO
        }

        override fun stopShakeDetector() {
            // TODO
        }

        override fun resumeShakeDetector() {
            // TODO
        }

        override fun pauseShakeDetector() {
            // TODO
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

        if (isImportOpen) {
            FilesDialog(
                onCloseRequest = { uris ->
                    isImportOpen = false
                    onImportURIsCallback?.let { it(uris) }
                },
            )
        }
    }
}


// thanks: https://kotlinlang.org/docs/multiplatform/compose-desktop-top-level-windows-management.html#swing-interoperability
@Composable
private fun FilesDialog(
    parent : Frame? = null,
    onCloseRequest : (result : Collection<Uri>) -> Unit,
) {
    val selectFiles = stringResource(Res.string.select_puzzle_files)
    AwtWindow(
        create = {
            object : FileDialog(parent, selectFiles, LOAD) {
                init { setMultipleMode(true) }

                override fun setVisible(value : Boolean) {
                    super.setVisible(value)
                    if (value) {
                        val uris = files.map { it.toURI().toString().toUri() }
                        onCloseRequest(uris.toList())
                    }
                }
            }
        },
        dispose = FileDialog::dispose
    )
}

