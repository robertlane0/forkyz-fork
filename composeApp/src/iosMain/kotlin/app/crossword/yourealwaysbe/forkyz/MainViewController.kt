
package app.crossword.yourealwaysbe.forkyz

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController

import org.jetbrains.compose.resources.stringResource

import org.koin.compose.koinInject
import org.koin.core.context.startKoin

import platform.UIKit.UIScreen

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.announce_not_implemented
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.feature_not_implemented
import app.crossword.yourealwaysbe.forkyz.exttools.AppNotFoundException
import app.crossword.yourealwaysbe.forkyz.exttools.ExternalToolData
import app.crossword.yourealwaysbe.forkyz.injection.iosModules
import app.crossword.yourealwaysbe.forkyz.injection.sharedModules
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.IOSInputConnectionMediator
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.NativeImportFinishedResult
import app.crossword.yourealwaysbe.forkyz.util.Notifications
import app.crossword.yourealwaysbe.forkyz.util.ShareFileData
import app.crossword.yourealwaysbe.forkyz.util.Uri

private val TOAST_PANEL_PADDING = 30.dp
private val TOAST_OUTER_PADDING = 2.dp
private val TOAST_INNER_PADDING = 10.dp
private val TOAST_SHAPE = RoundedCornerShape(10.dp)
private val TOAST_DURATION = 5000L // ms

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        koinStarted = true
        startKoin {
            modules(sharedModules, iosModules)
        }
    }

    MainContent()
}

@Composable
private fun MainContent() {
    val themeHelper : ThemeHelper = koinInject()
    val notifications : Notifications = koinInject()
    val currentPuzzleHolder : CurrentPuzzleHolder = koinInject()

    val activeToasts = remember { mutableStateListOf<String>() }
    var onImportComplete : ((Boolean) -> Unit)? = null

    val featureNotImplemented
        = stringResource(Res.string.feature_not_implemented)
    val announceNotImplemented
        = stringResource(Res.string.announce_not_implemented)

    val utils = object : NativeFrontendUtils {
        override val inputConnectionMediator = IOSInputConnectionMediator()

        @OptIn(ExperimentalForeignApi::class)
        override val displayWidth : Int
            get() {
                return UIScreen.mainScreen.bounds
                    .useContents { size.width.toInt() }
            }

        @OptIn(ExperimentalForeignApi::class)
        override val displayHeight : Int
            get() {
                return UIScreen.mainScreen.bounds
                    .useContents { size.height.toInt() }
            }

        override val isPortrait : Boolean
            get() { return displayWidth < displayHeight }

        override val isExitAfterPendingImports : Boolean = false

        override fun onFinish() {
            // iOS manages app lifecycle; no explicit finish needed
        }

        override fun onExit() {
            // iOS manages app lifecycle; no explicit exit needed
        }

        @OptIn(DelicateCoroutinesApi::class)
        override fun toast(msg : String) {
            activeToasts.add(msg)
            GlobalScope.launch(Dispatchers.Main) {
                delay(TOAST_DURATION)
                activeToasts.removeFirst()
            }
        }

        override fun onImportFinish(ifr : NativeImportFinishedResult) {
            onImportComplete?.let {
                it(!ifr.someFailed && ifr.someSucceeded)
            }
        }

        override fun handleExternalToolEvent(tool : ExternalToolData) {
            toast(featureNotImplemented)
        }

        override fun onShareFile(data : ShareFileData) {
            toast(featureNotImplemented)
        }

        override fun setOnImportCompleteCallback(cb : ((Boolean) -> Unit)?) {
            onImportComplete = cb
        }

        override fun removeOnImportCompleteCallback(cb : ((Boolean) -> Unit)?) {
            if (onImportComplete == cb)
                onImportComplete = null
        }

        override fun announce(text : CharSequence) {
            toast(announceNotImplemented)
        }

        override val canShareFiles : Boolean = false
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
                key(msg) {
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

    Box {
        ForkyzApp(
            themeHelper = themeHelper,
            utils = utils,
        )
        themeHelper.AppTheme {
            Toasts(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(TOAST_PANEL_PADDING),
            )
        }
    }
}
