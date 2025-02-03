
package app.crossword.yourealwaysbe.forkyz

import javax.inject.Inject
import kotlinx.coroutines.launch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import dagger.hilt.android.AndroidEntryPoint

import app.crossword.yourealwaysbe.forkyz.theme.AppTheme
import app.crossword.yourealwaysbe.forkyz.theme.ForkyzTopAppBar
import app.crossword.yourealwaysbe.forkyz.theme.ThemeHelper
import app.crossword.yourealwaysbe.forkyz.util.ExitActivity
import app.crossword.yourealwaysbe.forkyz.util.OrientationHelper
import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    private val SETTINGS_HORIZONTAL_PADDING = 20.dp
    private val SETTINGS_VERTICAL_PADDING = 14.dp
    private val DIALOG_PADDING = 20.dp
    private val DIALOG_BUTTON_PADDING = 4.dp
    private val DIALOG_ROUNDED_CORNERS = RoundedCornerShape(26.dp)
    private val SWITCH_PADDING = 4.dp

    @Inject
    lateinit var themeHelper : ThemeHelper

    @Inject
    lateinit var utils : AndroidVersionUtils

    @Inject
    lateinit var orientationHelper : OrientationHelper

    private val viewModel by viewModels<SettingsActivityViewModel>()

    private val getSAFURI = registerForActivityResult(OpenDocumentTree()) {
        uri : Uri? -> uri?.let {
            if (!viewModel.setNewExternalStorageSAFURI(it)) {
                Toast.makeText(
                    getApplication(),
                    R.string.failed_to_initialise_saf,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private val getImportSettingsURI = registerForActivityResult(GetContent()) {
        uri : Uri? -> uri?.let {
            viewModel.importSettings(uri)
        }
    }
    private val getExportSettingsURI = registerForActivityResult(
        CreateDocument(SETTINGS_MIME_TYPE)
    ) {
        uri : Uri? -> uri?.let {
            viewModel.exportSettings(uri)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)
        orientationHelper.applyOrientation(this)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchForActivityResults()
            ShowMessages()

            BackHandler(enabled = state.hasBackStack) {
                handleBack()
            }

            themeHelper.AppTheme() {
                // Reset list state/scroll on new list
                // https://slack-chats.kotlinlang.org/t/510683/what-would-be-to-best-way-to-reset-the-scroll-of-a-lazylist-
                val scrollBehavior = key(state.settingsItems) {
                    TopAppBarDefaults.pinnedScrollBehavior()
                }
                val listState = key(state.settingsItems) {
                    rememberLazyListState()
                }

                Scaffold(
                    topBar = { TopAppBar(scrollBehavior) },
                    modifier = Modifier.nestedScroll(
                        scrollBehavior.nestedScrollConnection
                    )
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        SearchBar(
                            showSearchBar = state.inSearchMode,
                            searchTerm = state.searchTerm,
                            onValueChange = viewModel::setSearchTerm,
                            onClose = viewModel::endSearch,
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            state = listState,
                        ) {
                            itemsIndexed(state.settingsItems) { index, item ->
                                SettingsItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical
                                                = SETTINGS_VERTICAL_PADDING,
                                            horizontal
                                                = SETTINGS_HORIZONTAL_PADDING,
                                        ),
                                    item = item,
                                    isFirst = (index == 0),
                                )
                            }
                        }
                    }

                    DisplayDialogs(state)
                }
            }
        }
    }

    /**
     * Launch for activity results if needed
     *
     * E.g. if a SAF URI is needed, launch getSAFURI
     */
    @Composable
    private fun LaunchForActivityResults() {
        val needsURI by viewModel.needsURI.collectAsStateWithLifecycle()
        when (needsURI) {
            RequestedURI.SAF -> {
                LaunchedEffect(needsURI) {
                    Toast.makeText(
                        getApplication(),
                        R.string.storage_select_saf_info,
                        Toast.LENGTH_LONG
                    ).show()
                    getSAFURI.launch(null)
                    viewModel.clearNeedsURI()
                }
            }
            RequestedURI.SETTINGS_EXPORT -> {
                LaunchedEffect(needsURI) {
                    getExportSettingsURI.launch(SETTINGS_DEFAULT_FILENAME)
                    viewModel.clearNeedsURI()
                }
            }
            RequestedURI.SETTINGS_IMPORT -> {
                LaunchedEffect(needsURI) {
                    getImportSettingsURI.launch(SETTINGS_MIME_TYPE)
                    viewModel.clearNeedsURI()
                }
            }
            RequestedURI.NONE -> {
                // nothing to do
            }
        }
    }

    @Composable
    private fun ShowMessages() {
        val messages by viewModel.messages.collectAsStateWithLifecycle()
        LaunchedEffect(messages) {
            val app = getApplication()
            for (msg in messages) {
                Toast.makeText(app, msg, Toast.LENGTH_LONG).show()
            }
            viewModel.clearMessages()
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    private fun TopAppBar(scrollBehavior : TopAppBarScrollBehavior) {
        // using view model in here because it's not intended to be
        // reused elsewhere
        val state by viewModel.state.collectAsStateWithLifecycle()
        themeHelper.ForkyzTopAppBar(
            title = { Text(stringResource(id = state.currentPage.title)) },
            onBack = this::handleBack,
            actions = {
                IconButton(onClick = viewModel::startSearch) {
                    Icon(
                        Icons.Filled.Search,
                        stringResource(R.string.filter_settings_hint),
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }

    @Composable
    private fun SearchBar(
        showSearchBar : Boolean,
        searchTerm : String,
        onValueChange : (String) -> Unit,
        onClose : () -> Unit,
    ) {
        if (showSearchBar) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    label = {
                        Text(stringResource(R.string.filter_settings_hint))
                    },
                    value = searchTerm,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1.0f)
                        .padding(4.dp),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Filled.Close,
                        stringResource(R.string.filter_settings_close),
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsItem(
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
                        val state by item.value.observeAsState()
                        SettingsBoolean(
                            modifier = modifier,
                            title = title,
                            errorMsg = errorMsg?.value,
                            summary = summary,
                            value = state ?: false,
                            onValueChange = item.setValue,
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
                            onClick = { openHTMLPage(item.url) },
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
                    else -> { } /* ignore */
                }
            }
            else -> { } /* ignore */
        }
    }

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
    fun SettingsBoolean(
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
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            errorMsg?.let {
                Text(
                    errorMsg,
                    style = MaterialTheme.typography.bodyMedium,
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
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            errorMsg?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    @Composable
    private fun DisplayDialogs(state : SettingsActivityViewState) {
        if (state.storageChangedAppExit) {
            StorageChangedAppExitDialog()
        }

        state.activeInputItem?.let {
            when (it) {
                is SettingsDynamicList<*> -> ActiveListItemDialog(it)
                is SettingsDynamicMultiList<*> -> ActiveMultiListItemDialog(it)
                is SettingsString -> ActiveStringItemDialog(it)
                is SettingsInteger -> ActiveIntegerItemDialog(it)
                else -> { }
            }
        }
    }

    @Composable
    private fun StorageChangedAppExitDialog() {
        AlertDialog(
            title = { Text(stringResource(R.string.storage_changed_title)) },
            text = {
                Text(stringResource(R.string.storage_changed_please_restart))
            },
            onDismissRequest = this::appExit,
            confirmButton = {
                TextButton(onClick = this::appExit) {
                    Text(stringResource(R.string.exit))
                }
            },
        )
    }

    @Composable
    private fun ActiveListItemDialog(item : SettingsDynamicList<*>) {
        val selected by item.value.observeAsState()
        val entries by item.entries.observeAsState()

        val labels = entries?.map {
                it.labelId?.let {
                    stringResource(it)
                } ?: it.labelString ?: ""
            }
        val selectedIdx = entries?.indexOfFirst { it.value == selected }

        if (labels != null && selectedIdx != null) {
            SettingsListDialog(
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
    }

    @Composable
    private fun ActiveMultiListItemDialog(item : SettingsDynamicMultiList<*>) {
        val selected by item.value.observeAsState()
        val entries by item.entries.observeAsState()

        val labels = entries?.map {
                it.labelId?.let {
                    stringResource(it)
                } ?: it.labelString ?: ""
            }
        val selectedIndices = selected?.map { selItem ->
                entries?.indexOfFirst { it.value == selItem }
            }?.filterNotNull()?.filter { it >= 0 }?.toSet()
            ?: setOf()

        if (labels != null) {
            SettingsMultiListDialog(
                title = item.title,
                summary = item.summary,
                labels = labels,
                selected = selectedIndices,
                onSelect = { indices ->
                    item.selectItems(indices)
                    viewModel.popState()
                },
                onDismissRequest = viewModel::popState,
            )
        }
    }

    @Composable
    private fun ActiveStringItemDialog(item : SettingsString) {
        val value by item.value.observeAsState()
        SettingsStringDialog(
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
    private fun ActiveIntegerItemDialog(item : SettingsInteger) {
        val value by item.value.observeAsState()
        SettingsStringDialog(
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

    /**
     * List selection dialog
     *
     * @param labels list of labels to display by android resource id
     * @param selected index of item to be initially selected
     * @param onSelect call back when the indexth item is selected and oked
     * @param onDismissRequest call back when dismissed
     */
    @Composable
    private fun SettingsListDialog(
        title : Int,
        summary : Int?,
        labels : List<String>,
        selected : Int,
        onSelect : (Int) -> Unit,
        onDismissRequest : () -> Unit,
    ) {
        var selectedState by remember { mutableIntStateOf(selected) }
        // set again in case this is a relayout
        selectedState = selected

        OKCancelDialog(
            title = title,
            summary = summary,
            onCancel = onDismissRequest,
            onOK = { onSelect(selectedState) }
        ) {
            BoxWithConstraints() {
                LazyColumn(
                    modifier = Modifier.heightIn(0.dp, maxHeight * 0.7f),
                ) {
                    itemsIndexed(labels) { index, label ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { selectedState = index },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = index == selectedState,
                                onClick = { selectedState = index },
                            )
                            Text(label)
                        }
                    }
                }
            }
        }
    }

    /**
     * List multi-selection dialog
     *
     * @param labels list of labels to display by android resource id
     * @param selected set of indices of items to be initially selected
     * @param onSelect call back when the set of selected is oked
     * @param onDismissRequest call back when dismissed
     */
    @Composable
    private fun SettingsMultiListDialog(
        title : Int,
        summary : Int?,
        labels : List<String>,
        selected : Set<Int>,
        onSelect : (Set<Int>) -> Unit,
        onDismissRequest : () -> Unit,
    ) {
        var selectedState by remember { mutableStateOf(selected) }
        // set again in case relayout
        selectedState = selected

        OKCancelDialog(
            title = title,
            summary = summary,
            onCancel = onDismissRequest,
            onOK = { onSelect(selectedState) }
        ) {
            BoxWithConstraints() {
                LazyColumn(
                    modifier = Modifier.heightIn(0.dp, maxHeight * 0.7f),
                ) {
                    itemsIndexed(labels) { index, label ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    selectedState = setMembership(
                                        index,
                                        selectedState,
                                        !selectedState.contains(index),
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedState.contains(index),
                                onCheckedChange = { checked ->
                                    selectedState = setMembership(
                                        index, selectedState, checked
                                    )
                                },
                            )
                            Text(label)
                        }
                    }
                }
            }
        }
    }

    /**
     * New set with item added/removed to elements by add param
     */
    private fun <T> setMembership(
        item : T, elements : Set<T>, add : Boolean
    ) : Set<T> = if (add)
            elements + item
        else
            elements - item

    /**
     * String input dialog
     *
     * @param value the initial value or null if none
     * @param onValueChange call back when new value oked
     * @param onDismissRequest call back when dismissed
     */
    @Composable
    private fun SettingsStringDialog(
        title : Int,
        summary : Int?,
        hint : Int?,
        value : String?,
        onValueChange : (String) -> Unit,
        onDismissRequest : () -> Unit,
        keyboardType : KeyboardType = KeyboardType.Text,
    ) {
        var currentValue by remember { mutableStateOf(value ?: "") }
        // set again in case relayout
        currentValue = value ?: ""

        OKCancelDialog(
            title = title,
            summary = summary,
            onCancel = onDismissRequest,
            onOK = { onValueChange(currentValue) },
        ) {
            OutlinedTextField(
                label = {
                    hint?.let { Text(stringResource(it)) }
                },
                value = currentValue,
                onValueChange = { currentValue = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = keyboardType,
                ),
            )
        }
    }

    /**
     * OK Cancel dialog with customisable body
     *
     * @param title the title resource id
     * @param summary the summary resource id or null
     * @param onCancel call back for cancel pressed
     * @param onOK call back for OK pressed
     * @param body the dialog body
     */
    @Composable
    private fun OKCancelDialog(
        title : Int,
        summary : Int?,
        onCancel : () -> Unit,
        onOK : () -> Unit,
        body : @Composable () -> Unit,
    ) {
        Dialog(onDismissRequest = onCancel) {
            Card(shape = DIALOG_ROUNDED_CORNERS) {
                Column(
                    modifier = Modifier.padding(DIALOG_PADDING),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        modifier = Modifier.padding(bottom = DIALOG_PADDING),
                        text = stringResource(title),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    summary?.let { summary ->
                        Text(
                            modifier = Modifier.fillMaxWidth()
                                .padding(bottom = DIALOG_PADDING),
                            text = stringResource(summary),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    body()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.padding(
                                horizontal = DIALOG_BUTTON_PADDING,
                            ),
                        ) {
                            Text(stringResource(android.R.string.cancel))
                        }
                        TextButton(
                            onClick = onOK,
                            modifier = Modifier.padding(
                                horizontal = DIALOG_BUTTON_PADDING,
                            ),
                        ) {
                            Text(stringResource(android.R.string.ok))
                        }
                    }
                }
            }
        }
    }

    private fun handleBack() {
        if (viewModel.state.value.hasBackStack)
            viewModel.popState()
        else
            finish()
    }

    private fun appExit() {
        ExitActivity.exit(getApplication());
    }

    private fun openHTMLPage(url : String) {
        startActivity(Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url),
            this,
            HTMLActivity::class.java
        ))
    }
}
