
package app.crossword.yourealwaysbe.forkyz.view

import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

import app.crossword.yourealwaysbe.forkyz.net.Downloader
import app.crossword.yourealwaysbe.forkyz.net.DownloaderID
import app.crossword.yourealwaysbe.forkyz.net.Downloaders
import app.crossword.yourealwaysbe.forkyz.net.DownloadersProvider
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.getOnce
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed

/**
 * If is available right now
 *
 * That is "available" being sometime today, and refined via
 * availableHours
 */
fun isAvailableNow(availableHours : Long?, available : Boolean) : Boolean {
    return available && (availableHours == null || availableHours < 0)
}

/**
 * Info on a downloader to display
 *
 * @param downloaderID the internal name (id) of the downloader
 * @param displayName the resource string of the name to display the
 * downloader with
 * @param availableHours how many hours until the downloader is
 * available, null means not known
 * @param availableNow if available now
 */
data class DownloaderData(
    val downloaderID : DownloaderID,
    val displayName : String,
    val availableHours : Long?,
    val availableNow : Boolean,
)

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
    val selected : Set<DownloaderID> = setOf(),
)

class DownloadDialogViewModel(
    private val viewModelScope : CoroutineScope,
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
            viewModelScope.launch {
                cb(
                    downloaders.getDownloaders().filter { downloader ->
                        if (downloader.downloaderID in state.selected) {
                            isAvailableNow(
                                downloader.getUntilAvailable(date)?.toHours(),
                                downloader.isAvailable(date),
                            )
                        } else {
                            false
                        }
                    }
                )
            }
        }
    }

    var date : LocalDate
        get() { return mediatedUIState.current.date }
        set(value) {
            downloadersState.filterNotNull().getOnce() { downloadersSettings ->
                viewModelScope.launch {
                    mediatedUIState.current = getUpdatedUIState(
                        mediatedUIState.current,
                        downloadersSettings,
                        value,
                    )
                }
            }
        }

    /**
     * Set selection to set of internal ids
     */
    fun setSelection(newSelection : Set<DownloaderID>) {
        mediatedUIState.current = mediatedUIState.current
            .copy(selected = newSelection)
    }

    /**
     * Get updated UI state with new date if not null
     */
    private suspend fun getUpdatedUIState(
        state : DownloaderDialogUIState,
        downloadersSettings : Downloaders,
        newDate : LocalDate? = null
    ) : DownloaderDialogUIState{
        val date = newDate ?: state.date
        val downloaders = downloadersSettings
            .getDownloaders()
            .map { downloader ->
                val availableHours
                    = downloader.getUntilAvailable(date)?.toHours()
                val availableNow = isAvailableNow(
                    availableHours,
                    downloader.isAvailable(date),
                )
                DownloaderData(
                    downloaderID = downloader.downloaderID,
                    displayName = downloader.name,
                    availableHours = availableHours,
                    availableNow = availableNow,
                )
            }
        // if changing from empty downloaders to some, select all
        val selected = if (
            (state.downloaders?.isEmpty() ?: true) && state.selected.isEmpty()
        ) {
            downloaders.map { it.downloaderID }.toSet()
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
