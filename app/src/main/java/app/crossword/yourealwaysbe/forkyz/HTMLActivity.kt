package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper

private val PADDING = 20.dp

@AndroidEntryPoint
class HTMLActivity : ForkyzActivity() {
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel by viewModels<HTMLActivityViewModel>()
        val assetName = getIntent().getData().toString()

        setContent() {
            AppTheme() {
                HTMLPage(
                    viewModel = viewModel,
                    themeHelper = themeHelper,
                    assetName = assetName,
                    onFinish = this::finish,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HTMLPage(
    viewModel : HTMLActivityViewModel,
    themeHelper : ThemeHelper,
    assetName : String,
    onFinish : () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.loadAsset(assetName)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        topBar = {
            themeHelper.ForkyzTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                onBack = onFinish,
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(
            scrollBehavior.nestedScrollConnection
        ),
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

