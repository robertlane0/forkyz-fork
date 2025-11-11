package app.crossword.yourealwaysbe.forkyz

import androidx.activity.compose.setContent
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

import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper

private val PADDING = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HTMLPage(
    viewModel : HTMLPageViewModel,
    themeHelper : ThemeHelper,
    rawAssetID : Int,
    onBack : () -> Unit,
) {
    LaunchedEffect(rawAssetID) {
        viewModel.loadAsset(rawAssetID)
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        topBar = {
            themeHelper.ForkyzTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                onBack = onBack,
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

