
package app.crossword.yourealwaysbe.forkyz

import kotlinx.serialization.Serializable

import android.net.Uri
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute

import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.InputConnectionMediator
import app.crossword.yourealwaysbe.puz.ClueID

/**
 * Navigation routes
 */
@Serializable
private object Browse
@Serializable
private object Play
@Serializable
private object ClueList
@Serializable
private data class Notes(
    val listName : String?,
    val clueIndex : Int?,
)
@Serializable
private object Settings
@Serializable
private data class HTML(val asset : String)
@Serializable
private object Logcat

@Composable
fun ForkyzApp(
    themeHelper : ThemeHelper,
    inputConnectionMediator : InputConnectionMediator,
    displayWidth : Int,
    displayHeight : Int,
    isPortrait : Boolean,
    toast : (String) -> Unit,
    openAppSettings : () -> Unit,
    openURI: (Uri) -> Unit,
    getPendingImports : (() -> Collection<Uri>),
    clearPendingImports : (() -> Unit),
    onFinish : () -> Unit,
    onCheckAndWarnNetworkState : () -> Unit,
    onCheckRequestNotificationPermissions : () -> Unit,
    onImport : () -> Unit,
    onImportFinish : (ImportFinishResult) -> Unit,
    onExit : () -> Unit,
    handleExternalToolEvent : (ExternalToolData) -> Unit,
    onGetSAFURI : () -> Unit,
    onExportSettings : () -> Unit,
    onImportSettings : () -> Unit,
    onExportPuzzles : () -> Unit,
    onShareFile : (ShareFileData) -> Unit,
    setOnAppNotFoundCallback : (((AppNotFoundException) -> Unit)?) -> Unit,
    setOnImportCompleteCallback : (((Boolean) -> Unit)?) -> Unit,
    setOnImportURIsCallback : (((Collection<Uri>) -> Unit)?) -> Unit,
    setOnNotificationPermissionDeniedCallback : ((() -> Unit)?) -> Unit,
    setStatusBarColor : (Color) -> Unit,
    setSAFURICallback : (((Uri) -> Unit)?) -> Unit,
    setExportSettingsURICallback : (((Uri) -> Unit)?) -> Unit,
    setImportSettingsURICallback : (((Uri) -> Unit)?) -> Unit,
    setOnVoiceCommandCallback : (((List<String>?) -> Unit)?) -> Unit,
    launchVoiceInput : () -> Unit,
    announce : (AnnounceData, View) -> Unit,
    onSetFullScreen : () -> Unit,
    startShakeDetector : (() -> Unit) -> Unit,
    stopShakeDetector : () -> Unit,
    resumeShakeDetector : () -> Unit,
    pauseShakeDetector : () -> Unit,
) {
    themeHelper.AppTheme() {
        val navController = rememberNavController()

        fun doBackAction() {
            if (!navController.popBackStack())
                onFinish()
        }

        fun openClueListPage() {
            navController.navigate(route = ClueList)
        }

        fun openNotesPage(cid : ClueID?) {
            navController.navigate(
                route = Notes(cid?.listName, cid?.index),
            )
        }

        fun openHTMLPage(asset : String) {
            navController.navigate(route = HTML(asset))
        }

        val onlineSourcesURL = stringResource(R.string.online_sources_url)
        fun openOnlineSources() {
            openURI(onlineSourcesURL.toUri())
        }

        fun openPlayPage() {
            navController.navigate(route = Play)
        }

        fun openReleaseNotes() {
            openHTMLPage("release.html")
        }

        fun openSettingsPage() {
            navController.navigate(route = Settings)
        }

        fun openLogcatPage() {
            navController.navigate(route = Logcat)
        }

        NavHost(navController, startDestination = Browse) {
            composable<Browse> {
                val viewModel = hiltViewModel<BrowsePageViewModel>()
                BrowsePage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    toast = toast,
                    openAppSettings = openAppSettings,
                    openHTMLPage = ::openHTMLPage,
                    openOnlineSources = ::openOnlineSources,
                    openPlayPage = ::openPlayPage,
                    openReleaseNotes = ::openReleaseNotes,
                    openSettingsPage = ::openSettingsPage,
                    getPendingImports = getPendingImports,
                    clearPendingImports = clearPendingImports,
                    onBack = ::doBackAction,
                    onCheckAndWarnNetworkState = onCheckAndWarnNetworkState,
                    onCheckRequestNotificationPermissions
                        = onCheckRequestNotificationPermissions,
                    onImport = onImport,
                    onImportFinish = onImportFinish,
                    onExit = onExit,
                    setOnImportCompleteCallback = setOnImportCompleteCallback,
                    setOnImportURIsCallback = setOnImportURIsCallback,
                    setOnNotificationPermissionDeniedCallback
                        = setOnNotificationPermissionDeniedCallback,
                )
            }
            composable<Play> {
                val viewModel = hiltViewModel<PlayPageViewModel>()
                PlayPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    inputConnectionMediator = inputConnectionMediator,
                    displayWidth = displayWidth,
                    displayHeight = displayHeight,
                    isPortrait = isPortrait,
                    openClueListPage = ::openClueListPage,
                    openNotesPage = ::openNotesPage,
                    openHTMLPage = ::openHTMLPage,
                    openSettingsPage = ::openSettingsPage,
                    openURI = openURI,
                    onFinish = onFinish,
                    onBack = ::doBackAction,
                    toast = toast,
                    handleExternalToolEvent = handleExternalToolEvent,
                    setOnAppNotFoundCallback = setOnAppNotFoundCallback,
                    setStatusBarColor = setStatusBarColor,
                    setOnVoiceCommandCallback = setOnVoiceCommandCallback,
                    launchVoiceInput = launchVoiceInput,
                    announce = announce,
                    onSetFullScreen = onSetFullScreen,
                    startShakeDetector = startShakeDetector,
                    stopShakeDetector = stopShakeDetector,
                    resumeShakeDetector = resumeShakeDetector,
                    pauseShakeDetector = pauseShakeDetector,
                )
            }
            composable<ClueList> {
                val viewModel = hiltViewModel<ClueListPageViewModel>()
                ClueListPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    inputConnectionMediator = inputConnectionMediator,
                    displayWidth = displayWidth,
                    displayHeight = displayHeight,
                    isPortrait = isPortrait,
                    openClueListPage = ::openClueListPage,
                    openNotesPage = ::openNotesPage,
                    openHTMLPage = ::openHTMLPage,
                    openSettingsPage = ::openSettingsPage,
                    openURI = openURI,
                    onFinish = onFinish,
                    onBack = ::doBackAction,
                    toast = toast,
                    handleExternalToolEvent = handleExternalToolEvent,
                    setOnAppNotFoundCallback = setOnAppNotFoundCallback,
                    setStatusBarColor = setStatusBarColor,
                    setOnVoiceCommandCallback = setOnVoiceCommandCallback,
                    launchVoiceInput = launchVoiceInput,
                    announce = announce,
                )
            }
            composable<Notes> { backStackEntry ->
                val viewModel = hiltViewModel<NotesPageViewModel>()
                val notes : Notes = backStackEntry.toRoute()
                // TODO: should this be set here?
                viewModel.notesClueID = if (
                    notes.listName != null && notes.clueIndex != null
                ) ClueID(notes.listName, notes.clueIndex) else null
                NotesPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    inputConnectionMediator = inputConnectionMediator,
                    displayWidth = displayWidth,
                    displayHeight = displayHeight,
                    isPortrait = isPortrait,
                    openClueListPage = ::openClueListPage,
                    openNotesPage = ::openNotesPage,
                    openHTMLPage = ::openHTMLPage,
                    openSettingsPage = ::openSettingsPage,
                    openURI = openURI,
                    onFinish = onFinish,
                    onBack = ::doBackAction,
                    toast = toast,
                    handleExternalToolEvent = handleExternalToolEvent,
                    setOnAppNotFoundCallback = setOnAppNotFoundCallback,
                    setStatusBarColor = setStatusBarColor,
                    setOnVoiceCommandCallback = setOnVoiceCommandCallback,
                    launchVoiceInput = launchVoiceInput,
                    announce = announce,
                )
            }
            composable<Settings> {
                val viewModel = hiltViewModel<SettingsPageViewModel>()
                SettingsPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    onExit = onExit,
                    toast = toast,
                    openHTMLPage = ::openHTMLPage,
                    openLogcatPage = ::openLogcatPage,
                    onBack = ::doBackAction,
                    onGetSAFURI = onGetSAFURI,
                    onExportSettings = onExportSettings,
                    onImportSettings = onImportSettings,
                    onExportPuzzles = onExportPuzzles,
                    setSAFURICallback = setSAFURICallback,
                    setExportSettingsURICallback = setExportSettingsURICallback,
                    setImportSettingsURICallback = setImportSettingsURICallback,
                )
            }
            composable<HTML> { backStackEntry ->
                val viewModel = hiltViewModel<HTMLPageViewModel>()
                val html : HTML = backStackEntry.toRoute()
                HTMLPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    assetName = html.asset,
                    onBack = ::doBackAction,
                )
            }
            composable<Logcat> {
                val viewModel = hiltViewModel<LogcatPageViewModel>()
                LogcatPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    onShareFile = onShareFile,
                    onBack = ::doBackAction,
                )
            }
        }

        themeHelper.LegacyStatusBarColor(setStatusBarColor)
    }
}

