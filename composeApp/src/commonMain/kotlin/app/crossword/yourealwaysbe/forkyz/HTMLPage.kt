package app.crossword.yourealwaysbe.forkyz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.app_name
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.onKeyEventKeyboardBindings

private val PADDING = 20.dp

/**
 * HTML Page
 *
 * @param unqualifiedFileName should come from HTMLFile class in view
 * model
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HTMLPage(
    viewModel : HTMLPageViewModel,
    themeHelper : ThemeHelper,
    utils : NativeFrontendUtils,
    unqualifiedFileName : String,
    onBack : () -> Unit,
) {
    LaunchedEffect(unqualifiedFileName) {
        viewModel.loadAsset(unqualifiedFileName)
    }

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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(
            scrollBehavior.nestedScrollConnection
        ).onKeyEvent(::onKeyEvent),
        topBar = {
            themeHelper.ForkyzTopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            val assetText
                by viewModel.assetText.collectAsStateWithLifecycle()
            Text(
                modifier = Modifier.padding(PADDING),
                text = assetText,
            )
        }
    }
}

