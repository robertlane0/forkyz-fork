package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject
import kotlinx.coroutines.launch

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.app.ShareCompat

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LogcatActivity : ForkyzActivity() {
    private val PADDING = 20.dp

    private val viewModel by viewModels<LogcatActivityViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(bundle : Bundle?) {
        super.onCreate(bundle)

        viewModel.loadLogcat()

        setContent() {
            Launchers()

            AppTheme() {
                val scrollBehavior
                    = TopAppBarDefaults.enterAlwaysScrollBehavior()
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        themeHelper.ForkyzTopAppBar(
                            title = { Text(stringResource(R.string.logcat)) },
                            onBack = this::finish,
                            scrollBehavior = scrollBehavior,
                            actions = {
                                IconButton(onClick = viewModel::shareLogcat) {
                                    Icon(
                                        Icons.Filled.Share,
                                        stringResource(
                                            R.string.share_logcat
                                        ),
                                    )
                                }
                            },

                        )
                    },
                    modifier = Modifier.nestedScroll(
                        scrollBehavior.nestedScrollConnection
                    ),
                    floatingActionButton = {
                        if (listState.canScrollForward) {
                            SmallFloatingActionButton(
                                onClick = {
                                    scope.launch {
                                        listState.scrollToItem(
                                            viewModel.logcatText.value.size
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    stringResource(R.string.jump_to_end),
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    val logcatText
                        by viewModel.logcatText.collectAsStateWithLifecycle()

                    LazyColumn(
                        modifier = Modifier.padding(innerPadding)
                            .padding(horizontal=PADDING),
                        state = listState,
                    ) {
                        items(logcatText) { line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Launchers() {
        val event by viewModel.shareLogcatEvent.collectAsStateWithLifecycle()
        LaunchedEffect(event) {
            event?.let { event ->
                val title = getString(R.string.share_logcat)
                viewModel.clearShareLogcatEvent()

                val shareIntent = ShareCompat.IntentBuilder(this@LogcatActivity)
                    .setChooserTitle(title)
                    .setStream(event.uri)
                    .setType(event.mimeType)
                    .createChooserIntent()
                startActivity(shareIntent)
            }
        }
    }
}
