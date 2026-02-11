
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import dev.darkokoa.datetimewheelpicker.WheelDatePicker
import dev.darkokoa.datetimewheelpicker.core.format.CjkSuffixConfig
import dev.darkokoa.datetimewheelpicker.core.format.DateFormatter
import dev.darkokoa.datetimewheelpicker.core.format.MonthDisplayStyle
import dev.darkokoa.datetimewheelpicker.core.format.dateFormatter

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
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_downloaders_enabled
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.no_downloaders_enabled_msg
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.select_sources
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.settings

@Composable
fun DownloadDialog(
    viewModel : DownloadDialogViewModel,
    onCancel : () -> Unit,
    onDownload : () -> Unit,
    onOpenSettings : () -> Unit,
) {
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
                    val date by remember { derivedStateOf { state.date } }
                    val day = date.getDayOfWeek()
                        .getDisplayName(
                            TextStyle.FULL,
                            java.util.Locale.getDefault(),
                        )

                    Text(text = day)
                    WheelDatePicker(
                        modifier = Modifier.padding(vertical = 8.dp),
                        startDate = date.toKotlinLocalDate(),
                        dateFormatter = dateFormatter(
                            locale = Locale.current,
                            monthDisplayStyle = MonthDisplayStyle.SHORT,
                            cjkSuffixConfig = CjkSuffixConfig.ShowAll,
                        ),
                        onSnappedDate = { newDate ->
                            viewModel.date = newDate.toJavaLocalDate()
                        },
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

