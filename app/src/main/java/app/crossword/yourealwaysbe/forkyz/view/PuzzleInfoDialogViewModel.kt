
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
import app.crossword.yourealwaysbe.forkyz.util.liveDataFanIn
import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Playboard
import app.crossword.yourealwaysbe.puz.Puzzle

data class PuzzleInfoUIState(
    val title : String? = null,
    val author : String = "",
    val copyright : String = "",
    val elapsed : Long = 0L,
    val progress : Int = 0,
    val filename : String = "",
    val introduction : String? = null,
    val notes : String? = null,
    val showRating : Boolean = true,
    val rating : Int = 0,
)

data class PuzzleFinishedUIState(
    val completionTime : Long = 0L,
    val numberOfClues : Int = 0,
    val numberOfBoxes : Int = 0,
    val numberOfCheatedBoxes : Int = 0,
    val percentCheated : Int = 0,
    val completionMessage : String? = null,
    val showRating : Boolean = true,
    val rating : Int = 0,
)

/**
 * Create a view model for a puzzle info dialog
 *
 * @param timer the current puzzle timer or null (in which case
 * puzzle.getTime() is used instead)
 */
class PuzzleInfoDialogViewModel(
    protected val settings : ForkyzSettings,
    protected val currentPuzzleHolder : CurrentPuzzleHolder,
    protected val fileHandlerProvider : FileHandlerProvider,
    private val timer : ImaginaryTimer?,
) {
    protected val board : Playboard?
        get() { return currentPuzzleHolder.board }
    protected val puzzle : Puzzle?
        get() { return board?.puzzle }

    private val _uiState : MutableLiveData<PuzzleInfoUIState>
        = liveDataFanIn(
            { getUpdatedUIState() },
            settings.liveBrowseShowPercentageCorrect,
            settings.liveRatingsDisableRatings,
        )
    val uiState : LiveData<PuzzleInfoUIState> = _uiState

    /**
     * To show additional completion data when finished
     */
    val finishedViewModel = PuzzleFinishedDialogViewModel(
        settings,
        currentPuzzleHolder,
    )

    init {
        fileHandlerProvider.get { fileHandler ->
            currentPuzzleHolder.puzHandle?.let { handle ->
                _uiState.value = getCurrentUIState().copy(
                    filename = fileHandler.getUri(handle).toString()
                )
            }
        }
    }

    fun setRating(rating : Int) {
        puzzle?.rating = rating.toChar()
        _uiState.value = getCurrentUIState().copy(
            rating = rating,
        )
    }

    private fun getUpdatedUIState() : PuzzleInfoUIState {
        val state = getCurrentUIState()
        val puz = puzzle
        val showPercentageCorrect
            = settings.liveBrowseShowPercentageCorrect.value ?: false
        val elapsed = timer?.let { it.elapsed } ?: puz?.time ?: 0L
        val progress = if (showPercentageCorrect)
            puz?.percentComplete ?: 0
        else
            puz?.percentFilled ?: 0

        return state.copy(
            title = puz?.title,
            author = puz?.author ?: "",
            copyright = puz?.copyright ?: "",
            elapsed = elapsed,
            progress = progress,
            introduction = if (puz?.hasIntroMessage() ?: false)
                    puz?.introMessage ?: ""
                else
                    "",
            notes = if (puz?.hasNotes() ?: false)
                    puz?.notes ?: ""
                else
                    "",
            showRating = !(settings.liveRatingsDisableRatings.value ?: false),
            rating = puz?.rating?.code ?: 0,
        )
    }

    private fun getCurrentUIState() : PuzzleInfoUIState {
        return uiState.value ?: PuzzleInfoUIState()
    }
}

class PuzzleFinishedDialogViewModel(
    protected val settings : ForkyzSettings,
    protected val currentPuzzleHolder : CurrentPuzzleHolder,
) {
    protected val board : Playboard?
        get() { return currentPuzzleHolder.board }
    protected val puzzle : Puzzle?
        get() { return board?.puzzle }

    private val _uiState : MutableLiveData<PuzzleFinishedUIState>
        = liveDataFanIn(
            { getUpdatedUIState() },
            settings.liveRatingsDisableRatings,
        )
    /**
     * UI state is null if puzzle not finished (or no puzzle)
     */
    val uiState : LiveData<PuzzleFinishedUIState> = _uiState

    fun setRating(rating : Int) {
        puzzle?.rating = rating.toChar()
        _uiState.value = uiState.value?.copy(
            rating = rating,
        )
    }

    private fun getUpdatedUIState() : PuzzleFinishedUIState? {
        val puz = puzzle
        if ((puz?.percentComplete ?: 0) < 100)
            return null

        val state = uiState.value ?: PuzzleFinishedUIState()

        val numBoxes = puz?.numberOfBoxes ?: 0
        val numCheated = puz?.numberOfCheatedBoxes ?: 0
        val percentCheated = if (numBoxes == 0)
            0
        else
            (100 * numCheated.toFloat() / numBoxes).toInt()

        return state.copy(
            completionTime = puz?.time ?: 0L,
            numberOfClues = puz?.numberOfClues ?: 0,
            numberOfBoxes = numBoxes,
            numberOfCheatedBoxes = numCheated,
            percentCheated = percentCheated,
            completionMessage = puz?.completionMessage,
            showRating = !(settings.liveRatingsDisableRatings.value ?: false),
            rating = puz?.rating?.code ?: 0,
        )
    }
}

