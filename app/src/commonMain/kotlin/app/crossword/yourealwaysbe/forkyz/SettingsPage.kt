
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.launch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

import org.jetbrains.compose.resources.stringResource

import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.releaseBookmark

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.exit
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.failed_to_initialise_saf
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.filter_settings_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.filter_settings_hint
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.storage_changed_please_restart
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.storage_changed_title
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.storage_select_saf_info
import app.crossword.yourealwaysbe.forkyz.theme.ForkyzTopAppBar
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.NativeFrontendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.toUri
import app.crossword.yourealwaysbe.forkyz.view.ListDialog
import app.crossword.yourealwaysbe.forkyz.view.MultiListDialog
import app.crossword.yourealwaysbe.forkyz.view.OKCancelDialog
import app.crossword.yourealwaysbe.forkyz.view.SearchBar
import app.crossword.yourealwaysbe.forkyz.view.StringDialog

private val SETTINGS_HORIZONTAL_PADDING = 20.dp
private val SETTINGS_VERTICAL_PADDING = 14.dp
private val SWITCH_PADDING = 4.dp

private val TAG = "ForkyzSettingsPage"
private val SETTINGS_DEFAULT_FILENAME = "forkyz_settings"
private val SETTINGS_DEFAULT_EXT = "json"
// allow import of all docs (parser will take care of detecting if it's a
// puzzle that's recognised)
private val IMPORT_MIME_TYPE =  "*/*"

@Composable
private fun SettingsClickable(
    modifier : Modifier,
    title : String,
    summary : String?,
    errorMsg : String?,
    onClick : () -> Unit,
) {
    SettingsDescription(
        modifier = Modifier.clickable { onClick() }
            .then(modifier),
        title = title,
        summary = summary,
        errorMsg = errorMsg,
    )
}

@Composable
private fun SettingsBoolean(
    modifier : Modifier,
    title : String,
    summary : String?,
    errorMsg : String?,
    value : Boolean,
    onValueChange : (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable { onValueChange(!value) }
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsDescription(
            modifier = Modifier.weight(1.0f),
            title = title,
            summary = summary,
            errorMsg = errorMsg,
        )
        Switch(
            modifier = Modifier.padding(start = SWITCH_PADDING),
            checked = value,
            onCheckedChange = onValueChange,
        )
    }
}

@Composable
private fun SettingsTriState(
    modifier : Modifier,
    title : String,
    summary : String?,
    errorMsg : String?,
    value : TriState,
    onValueChange : (TriState) -> Unit,
) {
    fun onBooleanChange(newBoolean : Boolean) {
        onValueChange(if (newBoolean) TriState.ON else TriState.OFF)
    }

    val booleanValue = value != TriState.OFF
    val colors = if (value == TriState.INDETERMINATE) {
        val defaults = SwitchDefaults.colors()
        SwitchDefaults.colors(
            checkedThumbColor = defaults.uncheckedThumbColor,
            checkedTrackColor = defaults.uncheckedTrackColor,
            checkedBorderColor = defaults.uncheckedBorderColor,
        )
    } else {
        SwitchDefaults.colors()
    }

    Row(
        modifier = Modifier
            .clickable { onBooleanChange(!booleanValue) }
            .then(modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsDescription(
            modifier = Modifier.weight(1.0f),
            title = title,
            summary = summary,
            errorMsg = errorMsg,
        )
        Switch(
            modifier = Modifier.padding(start = SWITCH_PADDING),
            checked = booleanValue,
            onCheckedChange = { onBooleanChange(it) },
            colors = colors,
        )
    }
}

@Composable
private fun SettingsDescription(
    modifier : Modifier,
    title : String,
    summary : String?,
    errorMsg : String?,
) {
    Column(modifier = modifier) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium
        )
        summary?.let {
            Text(summary)
        }
        errorMsg?.let {
            Text(
                errorMsg,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SettingsHeading(
    modifier : Modifier,
    title : String,
    summary : String?,
    errorMsg : String?,
    withRule : Boolean,
) {
    Column(modifier = modifier) {
        if (withRule) {
            HorizontalDivider(
                modifier = Modifier.padding(
                    bottom=SETTINGS_VERTICAL_PADDING
                )
            )
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        summary?.let {
            Text(it)
        }
        errorMsg?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    viewModel : SettingsPageViewModel,
    themeHelper : ThemeHelper,
    utils : NativeFrontendUtils,
    pageOpener : PageOpener,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    /**
     * Launch for activity results if needed
     *
     * E.g. if a SAF URI is needed, launch getSAFURI
     */
    @Composable
    fun LaunchForExternalResults() {
        val failedToInitialiseSAF
            = stringResource(Res.string.failed_to_initialise_saf)
        val safLauncher = rememberDirectoryPickerLauncher {
            it?.let { file ->
                val uri = file.absolutePath().toUri()
                if (!viewModel.setNewExternalStorageSAFURI(uri))
                    utils.toast(failedToInitialiseSAF)
            }
        }
        val settingsExportLauncher = rememberFileSaverLauncher {
            it?.let { file ->
                val uri = file.absolutePath().toUri()
                viewModel.exportSettings(uri) {
                    file.releaseBookmark()
                }
            }
        }
        val settingsImportLauncher = rememberFilePickerLauncher {
            it?.let { file ->
                val uri = file.absolutePath().toUri()
                viewModel.importSettings(uri) {
                    file.releaseBookmark()
                }
            }
        }
        val puzzlesExportLauncher = rememberDirectoryPickerLauncher {
            it?.let { file ->
                val uri = file.absolutePath().toUri()
                utils.onExportPuzzles(uri)
            }
        }

        val needsURI by viewModel.needsURI.collectAsStateWithLifecycle()
        when (needsURI) {
            RequestedURI.SAF -> {
                val msg = stringResource(Res.string.storage_select_saf_info)
                LaunchedEffect(needsURI) {
                    utils.toast(msg)
                    safLauncher.launch()
                    viewModel.clearNeedsURI()
                }
            }
            RequestedURI.SETTINGS_EXPORT -> {
                LaunchedEffect(needsURI) {
                    settingsExportLauncher.launch(
                        suggestedName = SETTINGS_DEFAULT_FILENAME,
                        extension = SETTINGS_DEFAULT_EXT,
                    )
                    viewModel.clearNeedsURI()
                }
            }
            RequestedURI.SETTINGS_IMPORT -> {
                LaunchedEffect(needsURI) {
                    settingsImportLauncher.launch()
                    viewModel.clearNeedsURI()
                }
            }
            RequestedURI.PUZZLES_EXPORT -> {
                LaunchedEffect(needsURI) {
                    puzzlesExportLauncher.launch()
                    viewModel.clearNeedsURI()
                }
            }
            RequestedURI.NONE -> {
                // nothing to do
            }
        }
    }

    @Composable
    fun ShowMessages() {
        val messages by viewModel.messages.collectAsStateWithLifecycle()
        LaunchedEffect(messages) {
            messages.forEach(utils::toast)
            viewModel.clearMessages()
        }
    }

    fun handleBack() {
        if (viewModel.state.value.hasBackStack)
            viewModel.popState()
        else
            pageOpener.onBack()
    }

    @Composable
    fun TopAppBar(scrollBehavior : TopAppBarScrollBehavior) {
        val title by remember { derivedStateOf { state.currentPage.title } }
        themeHelper.ForkyzTopAppBar(
            title = { Text(stringResource(title)) },
            onBack = ::handleBack,
            actions = {
                IconButton(onClick = viewModel::startSearch) {
                    Icon(
                        Icons.Filled.Search,
                        stringResource(Res.string.filter_settings_hint),
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }

    @Composable
    fun SettingsItem(
        modifier : Modifier,
        item : SettingsItem,
        isFirst : Boolean,
    ) {
        // using viewmodel here as this is just a switch
        when (item) {
            is SettingsDivider -> HorizontalDivider(modifier = modifier)
            is SettingsNamedItem -> {
                val title = stringResource(item.title)
                val summary = item.summary?.let { stringResource(item.summary) }
                val errorMsg = item.errorMsg?.collectAsStateWithLifecycle()

                when (item) {
                    is SettingsSubPage -> {
                        SettingsClickable(
                            modifier = modifier,
                            title = title,
                            summary = summary,
                            errorMsg = errorMsg?.value,
                            onClick = { viewModel.setCurrentPage(item.subpage) }
                        )
                    }
                    is SettingsBoolean -> {
                        val state by item.value.collectAsStateWithLifecycle()
                        SettingsBoolean(
                            modifier = modifier,
                            title = title,
                            errorMsg = errorMsg?.value,
                            summary = summary,
                            value = state,
                            onValueChange = item.setValue,
                        )
                    }
                    is SettingsTriState -> {
                        val state by item.value.collectAsStateWithLifecycle()
                        SettingsTriState(
                            modifier = modifier,
                            title = title,
                            summary = summary,
                            value = state,
                            onValueChange = item.setValue,
                            errorMsg = errorMsg?.value,
                        )
                    }
                    is SettingsInputItem -> {
                        SettingsClickable(
                            modifier = modifier,
                            title = title,
                            summary = summary,
                            errorMsg = errorMsg?.value,
                            onClick = { viewModel.openInputSetting(item) },
                        )
                    }
                    is SettingsHeading -> {
                        SettingsHeading(
                            modifier = modifier,
                            title = title,
                            summary = summary,
                            errorMsg = errorMsg?.value,
                            withRule = !isFirst
                        )
                    }
                    is SettingsHTMLPage -> {
                        SettingsClickable(
                            modifier = modifier,
                            title = title,
                            summary = summary,
                            errorMsg = errorMsg?.value,
                            onClick = {
                                pageOpener.openHTMLPage(item.htmlPage)
                            },
                        )
                    }
                    is SettingsAction -> {
                        SettingsClickable(
                            modifier = modifier,
                            title = title,
                            summary = summary,
                            errorMsg = errorMsg?.value,
                            onClick = item.onClick,
                        )
                    }
                    is SettingsLogcat -> {
                        SettingsClickable(
                            modifier = modifier,
                            title = title,
                            summary = summary,
                            errorMsg = errorMsg?.value,
                            onClick = { pageOpener.openLogcatPage() },
                        )
                    }
                    else -> { } /* ignore */
                }
            }
            else -> { } /* ignore */
        }
    }

    @Composable
    fun StorageChangedAppExitDialog() {
        AlertDialog(
            title = { Text(stringResource(Res.string.storage_changed_title)) },
            text = {
                Text(stringResource(Res.string.storage_changed_please_restart))
            },
            onDismissRequest = utils::onExit,
            confirmButton = {
                TextButton(onClick = utils::onExit) {
                    Text(stringResource(Res.string.exit))
                }
            },
        )
    }

    @Composable
    fun ActiveListItemDialog(item : SettingsDynamicList<*>) {
        val selected by item.value.collectAsStateWithLifecycle()
        val entries by item.entries.collectAsStateWithLifecycle()

        val labels = entries.map {
                it.labelId?.let {
                    stringResource(it)
                } ?: it.labelString ?: ""
            }
        val selectedIdx = entries.indexOfFirst { it.value == selected }

        ListDialog(
            title = item.title,
            summary = item.summary,
            labels = labels,
            selected = selectedIdx,
            onSelect = { index ->
                item.selectItem(index)
                viewModel.popState()
            },
            onDismissRequest = viewModel::popState,
        )
    }

    @Composable
    fun ActiveMultiListItemDialog(item : SettingsDynamicMultiList<*>) {
        val selected by item.value.collectAsStateWithLifecycle()
        val entries by item.entries.collectAsStateWithLifecycle()

        val selectedIndices = selected.map { selItem ->
            entries.indexOfFirst { it.value == selItem }
        }.filter { it >= 0 }.toSet()

        MultiListDialog(
            title = item.title,
            summary = item.summary,
            numItems = entries.size,
            getItem = { index ->
                val entry = entries.get(index)
                val label = entry.labelId?.let {
                    stringResource(it)
                } ?: entry.labelString ?: ""
                Text(label)
            },
            selected = selectedIndices,
            onSelect = { indices ->
                item.selectItems(indices)
                viewModel.popState()
            },
            onDismissRequest = viewModel::popState,
        )
    }

    @Composable
    fun ActiveStringItemDialog(item : SettingsString) {
        val value by item.value.collectAsStateWithLifecycle()
        StringDialog(
            title = item.title,
            summary = item.dialogMsg ?: item.summary,
            hint = item.hint,
            value = value,
            onValueChange = { value ->
                item.setValue(value)
                viewModel.popState()
            },
            onDismissRequest = viewModel::popState,
        )
    }

    @Composable
    fun ActiveIntegerItemDialog(item : SettingsInteger) {
        val value by item.value.collectAsStateWithLifecycle()
        StringDialog(
            title = item.title,
            summary = item.summary,
            hint = item.hint,
            value = value.toString(),
            onValueChange = { value ->
                try {
                    item.setValue(value.toInt())
                } catch (e : NumberFormatException) {
                    // ignore, should never happen
                    // not so bad to ignore if does
                }
                viewModel.popState()
            },
            onDismissRequest = viewModel::popState,
            keyboardType = KeyboardType.Number,
        )
    }

    @Composable
    fun DisplayDialogs() {
        val storageChangedAppExit by remember {
            derivedStateOf { state.storageChangedAppExit }
        }
        if (storageChangedAppExit) {
            StorageChangedAppExitDialog()
        }

        val activeInputItem by remember {
            derivedStateOf { state.activeInputItem }
        }
        activeInputItem?.let {
            when (it) {
                is SettingsDynamicList<*> -> ActiveListItemDialog(it)
                is SettingsDynamicMultiList<*> -> ActiveMultiListItemDialog(it)
                is SettingsString -> ActiveStringItemDialog(it)
                is SettingsInteger -> ActiveIntegerItemDialog(it)
                else -> { }
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun HandleBack() {
        val hasBackStack by remember { derivedStateOf { state.hasBackStack } }
        val backState = rememberNavigationEventState(
            currentInfo = NavigationEventInfo.None,
        )
        NavigationBackHandler(
            state = backState,
            isBackEnabled = hasBackStack,
            onBackCompleted = { handleBack() },
        )
    }

    LaunchForExternalResults()
    ShowMessages()

    HandleBack()

    // Reset list state/scroll on new list
    // https://slack-chats.kotlinlang.org/t/510683/what-would-be-to-best-way-to-reset-the-scroll-of-a-lazylist-
    val settingsItems by remember { derivedStateOf { state.settingsItems } }
    val scrollBehavior = key(settingsItems) {
        TopAppBarDefaults.pinnedScrollBehavior()
    }
    val listState = key(settingsItems) {
        rememberLazyListState()
    }

    Scaffold(
        topBar = { TopAppBar(scrollBehavior) },
        modifier = Modifier.nestedScroll(
            scrollBehavior.nestedScrollConnection
        )
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            val inSearchMode by remember {
                derivedStateOf { state.inSearchMode }
            }
            val searchTerm by remember { derivedStateOf { state.searchTerm } }
            SearchBar(
                showSearchBar = inSearchMode,
                searchTerm = searchTerm,
                onValueChange = viewModel::setSearchTerm,
                onClose = viewModel::endSearch,
                hint = stringResource(Res.string.filter_settings_hint),
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
            ) {
                itemsIndexed(settingsItems) { index, item ->
                    SettingsItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = SETTINGS_VERTICAL_PADDING,
                                horizontal = SETTINGS_HORIZONTAL_PADDING,
                            ),
                        item = item,
                        isFirst = (index == 0),
                    )
                }
            }
        }

        DisplayDialogs()
    }
}
