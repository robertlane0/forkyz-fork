
package app.crossword.yourealwaysbe.forkyz.view

import java.time.LocalDate

import androidx.lifecycle.LiveData

import app.crossword.yourealwaysbe.forkyz.net.Downloader
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.util.liveDataFanIn

/**
 * Info on a downloader to display
 *
 * @param downloaderId the internal name (id) of the downloader
 * @param displayName the resource string of the name to display the
 * downloader with
 * @param availableHours how many hours until the downloader is
 * available, null or value >= 0 means not available
 */
data class DownloaderData(
    val internalName : String,
    val displayName : String,
    val availableHours : Long?,
) {
    val available : Boolean = !(availableHours == null || availableHours >= 0)
}

/**
 * The UI state
 *
 * @param date the currently picked date
 * @param downloaders the downloaders for the date
 * @param selected the set of selected downloaders
 */
data class DownloaderDialogUIState (
    val date : LocalDate = LocalDate.now(),
    val downloaders : List<DownloaderData> = listOf(),
    val selected : Set<String> = setOf(),
)

class DownloadDialogViewModel(
    val downloadersProvider : DownloadersProvider,
) {
    val _uiState = liveDataFanIn(
        { getUpdatedUIState() },
        downloadersProvider.liveDownloaders(),
    )
    val uiState : LiveData<DownloaderDialogUIState> = _uiState

    /**
     * Get list of currently selected available downloaders
     */
    val selectedDownloaders : List<Downloader>
        get() {
            val state = getCurrentState()
            return downloadersProvider
                .liveDownloaders()
                .value
                ?.downloaders?.filter { downloader ->
                    if (!(downloader.internalName in state.selected)) {
                        false
                    } else {
                        val availableHours = downloader
                            .getUntilAvailable(state.date)
                            ?.toHours()
                        availableHours != null && availableHours < 0
                    }
                } ?: listOf()
        }

    var date : LocalDate
        get() {
            val state = getCurrentState()
            return state.date
        }
        set(value) {
            _uiState.value = getUpdatedUIState(value)
        }

    /**
     * Set selection to set of internal ids
     */
    fun setSelection(newSelection : Set<String>) {
        _uiState.value = getCurrentState().copy(selected = newSelection)
    }

    private fun getCurrentState() : DownloaderDialogUIState {
        return uiState.value ?: DownloaderDialogUIState()
    }

    /**
     * Get updated UI state with new date if not null
     */
    private fun getUpdatedUIState(
        newDate : LocalDate? = null
    ) : DownloaderDialogUIState{
        val state = getCurrentState()
        val date = newDate ?: state.date
        val downloaders = downloadersProvider
            .liveDownloaders()
            .value
            ?.downloaders?.map { downloader ->
                val availableHours
                    = downloader.getUntilAvailable(date)?.toHours()

                DownloaderData(
                    internalName = downloader.internalName,
                    displayName = downloader.name,
                    availableHours = availableHours,
                )
            } ?: listOf()
        // if changing from empty downloaders to some, select all
        val selected = if (
            state.downloaders.isEmpty() && state.selected.isEmpty()
        ) {
            downloaders.map { it.internalName }.toSet()
        } else {
            state.selected
        }

        return state.copy(
            date = date,
            downloaders = downloaders,
            selected = selected,
        )
    }
}
