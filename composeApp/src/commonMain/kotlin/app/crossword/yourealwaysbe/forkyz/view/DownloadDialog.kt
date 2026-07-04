
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.material3.MaterialTheme

import kotlinx.datetime.LocalDate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.commit451.datepickerspinner.DateField
import com.commit451.datepickerspinner.DatePickerSpinner
import com.commit451.datepickerspinner.DatePickerSpinnerDefaults
import com.commit451.datepickerspinner.rememberDatePickerSpinnerState

import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.cancel
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.download_puzzles
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.downloader_available_future
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.downloader_available_hours
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.downloader_available_soon
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.downloader_not_available
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_april
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_august
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_december
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_february
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_january
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_july
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_june
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_march
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_may
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_november
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_october
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.month_short_september
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_downloaders_enabled
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_downloaders_enabled_msg
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.select_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.settings
import app.crossword.yourealwaysbe.forkyz.util.TimeUtils

@Composable
fun DownloadDialog(
    viewModel : DownloadDialogViewModel,
    onCancel : () -> Unit,
    onDownload : () -> Unit,
    onOpenSettings : () -> Unit,
) {

    val monthNames = listOf(
        stringResource(Res.string.month_short_january),
        stringResource(Res.string.month_short_february),
        stringResource(Res.string.month_short_march),
        stringResource(Res.string.month_short_april),
        stringResource(Res.string.month_short_may),
        stringResource(Res.string.month_short_june),
        stringResource(Res.string.month_short_july),
        stringResource(Res.string.month_short_august),
        stringResource(Res.string.month_short_september),
        stringResource(Res.string.month_short_october),
        stringResource(Res.string.month_short_november),
        stringResource(Res.string.month_short_december),
    )

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val downloaders by remember {
        derivedStateOf { state.downloaders }
    }
    downloaders?.let { downloaders ->
        // do nothing yet
        if (downloaders.size > 0) {
            val selected = (0 until downloaders.size).filter {
                state.selected.contains(downloaders.get(it).downloaderID)
            }.toSet()

            MultiListDialog(
                title = Res.string.download_puzzles,
                allOrNoneLabel = Res.string.select_sources,
                numItems = downloaders.size,
                getItem = { index -> DownloaderLabel(downloaders, index) },
                getEnabled = { index ->
                    getDownloaderEnabled(downloaders, index)
                },
                selected = selected,
                onSelect = { onDownload() },
                onDismissRequest = onCancel,
                onSelectionChange = { newSelection ->
                    viewModel.setSelection(
                        newSelection.filter { 0 <= it && it < downloaders.size }
                            .map { downloaders[it].downloaderID }
                            .toSet()
                    )
                },
                okLabel = Res.string.download,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(
                            TimeUtils.getDayNameID(state.date),
                        ),
                    )

                    val dateState = rememberDatePickerSpinnerState(
                        initialDate = state.date,
                    )
                    LaunchedEffect(dateState.selectedDate) {
                        viewModel.date = dateState.selectedDate
                    }

                    DatePickerSpinner(
                        modifier = Modifier.padding(vertical = 8.dp)
                            .fillMaxWidth(),
                        state = dateState,
                        dateFormatter = DatePickerSpinnerDefaults.dateFormatter(
                            monthNames = monthNames,
                            fieldOrder = listOf(
                                DateField.Day,
                                DateField.Month,
                                DateField.Year,
                            ),
                        ),
                    )
                }
            }
        } else {
            TwoButtonDialog(
                title = Res.string.no_downloaders_enabled,
                summary = Res.string.no_downloaders_enabled_msg,
                positiveText = Res.string.settings,
                onPositive = onOpenSettings,
                negativeText = Res.string.cancel,
                onNegative = onCancel,
            )
        }
    }
}

private fun getDownloaderEnabled(
    downloaders : List<DownloaderData>,
    index : Int,
) : Boolean {
    return downloaders.getOrNull(index)?.let { it.availableNow } ?: false
}

@Composable
private fun DownloaderLabel(
    downloaders : List<DownloaderData>,
    index : Int
) {
    downloaders.getOrNull(index)?.let { downloader ->
        val displayName = downloader.displayName
        val availableHours = downloader.availableHours

        Column(
            modifier = Modifier.padding(vertical=2.dp),
        ) {
            Text(displayName)

            val availabilityLabel = if (downloader.availableNow) {
                null
            } else if (downloader.availableHours == null) {
                stringResource(Res.string.downloader_not_available)
            } else if (availableHours < 0) {
                // should not happen because it's available now!
                null
            } else if (availableHours == 0L) {
                stringResource(Res.string.downloader_available_soon)
            } else if (availableHours < 24L) {
                pluralStringResource(
                    Res.plurals.downloader_available_hours,
                    availableHours.toInt(),
                    availableHours,
                )
            } else {
                stringResource(Res.string.downloader_available_future)
            }

            availabilityLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

