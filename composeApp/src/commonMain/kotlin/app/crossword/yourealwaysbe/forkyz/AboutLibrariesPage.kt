
package app.crossword.yourealwaysbe.forkyz

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import org.jetbrains.compose.resources.stringResource

import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.about_libraries
import app.crossword.yourealwaysbe.forkyz.theme.ForkyzScaffold
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.onKeyEventKeyboardBindings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLibrariesPage(
    viewModel : AboutLibrariesPageViewModel,
    themeHelper : ThemeHelper,
    utils : NativeFrontendUtils,
    onBack : () -> Unit,
) {
    fun onKeyEvent(event : KeyEvent) : Boolean {
        val handled = onKeyEventKeyboardBindings(
            event = event,
            binds = viewModel.keyboardBindings,
            onBackDown = onBack,
            onExitDown = utils::onExit,
        )

        val isDown = event.type == KeyEventType.KeyDown
        when (event.key) {
            Key.Escape -> {
                if (isDown)
                    onBack()
                return true
            }
        }
        return false
    }

    ForkyzScaffold(
        modifier = Modifier.onKeyEvent(::onKeyEvent),
        topBar = {
            themeHelper.ForkyzTopAppBar(
                title = { Text(stringResource(Res.string.about_libraries)) },
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        val data by viewModel.rawLibraryData.collectAsStateWithLifecycle()
        val libraries by produceLibraries(data ?: "")
        LibrariesContainer(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            libraries = libraries,
        )
    }
}

