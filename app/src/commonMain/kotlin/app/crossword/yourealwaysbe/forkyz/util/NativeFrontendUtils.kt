
package app.crossword.yourealwaysbe.forkyz.util

import androidx.compose.ui.graphics.Color

import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.util.Uri

data class ShareFileData(
    val title : String,
    val uri : Uri,
    val mimeType : String,
)

data class NativeImportFinishedResult(
    val someFailed : Boolean,
    val someSucceeded : Boolean,
)

interface NativeFrontendUtils {
    val inputConnectionMediator : InputConnectionMediator
    val displayWidth : Int
    val displayHeight : Int
    val isPortrait : Boolean
    val isExitAfterPendingImports : Boolean

    fun onFinish()
    fun onExit()

    fun toast(msg : String) { }
    fun openAppSettings() { }
    fun getPendingImports() : Collection<Uri> { return listOf() }
    fun clearPendingImports() { }
    fun onCheckAndWarnNetworkState() { }
    fun onCheckRequestNotificationPermissions() { }
    fun onImportFinish(ifr : NativeImportFinishedResult) { }
    fun handleExternalToolEvent(tool : ExternalToolData) { }
    fun onExportPuzzles(uri : Uri) { }
    fun onShareFile(data : ShareFileData) { }
    fun setOnAppNotFoundCallback(cb : ((AppNotFoundException) -> Unit)?) { }
    fun removeOnAppNotFoundCallback(cb : ((AppNotFoundException) -> Unit)?) { }
    fun setOnImportCompleteCallback(cb : ((Boolean) -> Unit)?) { }
    fun removeOnImportCompleteCallback(cb : ((Boolean) -> Unit)?) { }
    fun setOnNotificationPermissionDeniedCallback(cb : (() -> Unit)?) { }
    fun removeOnNotificationPermissionDeniedCallback(cb : (() -> Unit)?) { }
    fun setStatusBarColor(color : Color) { }
    fun setOnVoiceCommandCallback(cb : ((List<String>?) -> Unit)?) { }
    fun removeOnVoiceCommandCallback(cb : ((List<String>?) -> Unit)?) { }
    fun launchVoiceInput() { }
    fun announce(text : CharSequence) { }
    fun onSetFullScreen() { }
    fun startShakeDetector(onHearShake : () -> Unit) { }
    fun stopShakeDetector() { }
    fun resumeShakeDetector() { }
    fun pauseShakeDetector() { }
}
