
package app.crossword.yourealwaysbe.forkyz.view

import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.filterNotNull

import app.crossword.yourealwaysbe.forkyz.net.Downloader
import app.crossword.yourealwaysbe.forkyz.net.Downloaders
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.getOnce
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed

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
 * @param downloaders the downloaders for the date, null if not loaded yet
 * @param selected the set of selected downloaders
 */
data class DownloaderDialogUIState (
    val date : LocalDate = LocalDate.now(),
    val downloaders : List<DownloaderData>? = null,
    val selected : Set<String> = setOf(),
)

class DownloadDialogViewModel(
    viewModelScope : CoroutineScope,
    private val downloadersProvider : DownloadersProvider,
) {
    // use to cache an up to date downloaders object
    private val downloadersState : StateFlow<Downloaders?>
        = downloadersProvider.liveDownloaders.stateInSubscribed(
            viewModelScope,
            null,
        )
    private val mediatedUIState
        = MediatedStateWithFlow<DownloaderDialogUIState, Downloaders>(
            viewModelScope,
            DownloaderDialogUIState(),
            this::getUpdatedUIState,
            downloadersState.filterNotNull(),
        )
    val uiState : StateFlow<DownloaderDialogUIState> = mediatedUIState.stateFlow

    /**
     * Get list of internal names of selected available downloaders
     */
    fun getSelectedDownloaders(cb : (List<Downloader>) -> Unit) {
        val state = mediatedUIState.current
        downloadersState.filterNotNull().getOnce() { downloaders ->
            cb(
                downloaders.downloaders.filter { downloader ->
                    if (!(downloader.internalName in state.selected)) {
                        false
                    } else {
                        val availableHours = downloader
                            .getUntilAvailable(state.date)
                            ?.toHours()
                        availableHours != null && availableHours < 0
                    }
                }
            )
        }
    }

    var date : LocalDate
        get() { return mediatedUIState.current.date }
        set(value) {
            downloadersState.filterNotNull().getOnce() { downloadersSettings ->
                mediatedUIState.current = getUpdatedUIState(
                    mediatedUIState.current,
                    downloadersSettings,
                    value,
                )
            }
        }

    /**
     * Set selection to set of internal ids
     */
    fun setSelection(newSelection : Set<String>) {
        mediatedUIState.current = mediatedUIState.current
            .copy(selected = newSelection)
    }

    /**
     * Get updated UI state with new date if not null
     */
    private fun getUpdatedUIState(
        state : DownloaderDialogUIState,
        downloadersSettings : Downloaders,
        newDate : LocalDate? = null
    ) : DownloaderDialogUIState{
        val date = newDate ?: state.date
        val downloaders = downloadersSettings
            .downloaders
            ?.map { downloader ->
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
            (state.downloaders?.isEmpty() ?: true) && state.selected.isEmpty()
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
