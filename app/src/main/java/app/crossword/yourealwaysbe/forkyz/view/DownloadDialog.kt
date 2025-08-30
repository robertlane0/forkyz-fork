
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.material3.MaterialTheme

import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.format.DateFormatter
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter

import app.crossword.yourealwaysbe.forkyz.R

@Composable
fun DownloadDialog(
    viewModel : DownloadDialogViewModel,
    onCancel : () -> Unit,
    onDownload : () -> Unit,
    onOpenSettings : () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val downloaders : List<DownloaderData>? = state.downloaders
    if (downloaders == null) {
        // do nothing yet
    } else if (downloaders.size > 0) {
        val selected = (0 until downloaders.size).filter {
            state.selected.contains(downloaders.get(it).internalName)
        }.toSet()

        MultiListDialog(
            title = R.string.download_puzzles,
            allOrNoneLabel = R.string.select_sources,
            numItems = downloaders.size,
            getItem = { index -> DownloaderLabel(downloaders, index) },
            getEnabled = { index -> getDownloaderEnabled(downloaders, index) },
            selected = selected,
            onSelect = { onDownload() },
            onDismissRequest = onCancel,
            onSelectionChange = { newSelection ->
                viewModel.setSelection(
                    newSelection.filter { 0 <= it && it < downloaders.size }
                        .map { downloaders[it].internalName }
                        .toSet()
                )
            },
            okLabel = R.string.download,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val date = state.date
                val day = date.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, java.util.Locale.getDefault())

                Text(text = day)
                WheelDatePicker(
                    modifier = Modifier.padding(vertical = 8.dp),
                    startDate = date.toKotlinLocalDate(),
                    dateFormatter = dateFormatter(
                        Locale.current,
                        MonthDisplayStyle.SHORT,
                    ),
                    onSnappedDate = { newDate ->
                        viewModel.date = newDate.toJavaLocalDate()
                    },
                )
            }
        }
    } else {
        TwoButtonDialog(
            title = R.string.no_downloaders_enabled,
            summary = R.string.no_downloaders_enabled_msg,
            positiveText = R.string.settings,
            onPositive = onOpenSettings,
            negativeText = android.R.string.cancel,
            onNegative = onCancel,
        )
    }
}

private fun getDownloaderEnabled(
    downloaders : List<DownloaderData>,
    index : Int,
) : Boolean {
    return downloaders.get(index)?.let { it.available } ?: false
}

@Composable
private fun DownloaderLabel(
    downloaders : List<DownloaderData>,
    index : Int
) {
    downloaders.get(index)?.let { downloader ->
        val displayName = downloader.displayName
        val availableHours = downloader.availableHours

        Column(
            modifier = Modifier.padding(vertical=2.dp),
        ) {
            Text(displayName)

            val availabilityLabel = if (downloader.availableHours == null) {
                stringResource(R.string.downloader_not_available)
            } else if (availableHours < 0) {
                null
            } else if (availableHours == 0L) {
                stringResource(R.string.downloader_available_soon)
            } else if (availableHours < 24L) {
                pluralStringResource(
                    R.plurals.downloader_available_hours,
                    availableHours.toInt(),
                    availableHours,
                )
            } else {
                stringResource(R.string.downloader_available_future)
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

