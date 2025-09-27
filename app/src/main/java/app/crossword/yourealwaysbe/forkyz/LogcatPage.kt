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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper

private val PADDING = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatPage(
    viewModel : LogcatPageViewModel,
    themeHelper : ThemeHelper,
    onShareFile : (ShareFileData) -> Unit,
    onBack : () -> Unit,
) {
    @Composable
    fun Launchers() {
        LaunchedEffect(Unit) {
            viewModel.loadLogcat()
        }

        val event by viewModel.shareLogcatEvent.collectAsStateWithLifecycle()
        val title = stringResource(R.string.share_logcat)
        LaunchedEffect(event) {
            event?.let { event ->
                onShareFile(ShareFileData(title, event.uri, event.mimeType))
            }
        }
    }

    @Composable
    fun PageBody() {
        val scrollBehavior
            = TopAppBarDefaults.enterAlwaysScrollBehavior()
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        Scaffold(
            topBar = {
                themeHelper.ForkyzTopAppBar(
                    title = { Text(stringResource(R.string.logcat)) },
                    onBack = onBack,
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
                    Text(line)
                }
            }
        }
    }

    Launchers()
    PageBody()
}

