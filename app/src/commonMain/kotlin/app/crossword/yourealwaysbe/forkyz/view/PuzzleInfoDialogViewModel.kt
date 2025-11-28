
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.ImaginaryTimer
import app.crossword.yourealwaysbe.forkyz.util.MediatedStateWithFlow
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerProvider
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

private data class PuzzleInfoSettings(
    val showPercentageCorrect : Boolean,
    val disableRatings : Boolean,
)

/**
 * Create a view model for a puzzle info dialog
 *
 * @param timer the current puzzle timer or null (in which case
 * puzzle.getTime() is used instead)
 */
class PuzzleInfoDialogViewModel(
    private val viewModelScope : CoroutineScope,
    private val settings : ForkyzSettings,
    private val currentPuzzleHolder : CurrentPuzzleHolder,
    private val fileHandlerProvider : FileHandlerProvider,
    private val timer : ImaginaryTimer?,
) {
    protected val board : Playboard?
        get() { return currentPuzzleHolder.board }
    protected val puzzle : Puzzle?
        get() { return board?.puzzle }

    private val mediatedUIState
        = MediatedStateWithFlow<PuzzleInfoUIState, PuzzleInfoSettings>(
            viewModelScope,
            PuzzleInfoUIState(),
            this::getUpdatedUIState,
            combine(
                settings.liveBrowseShowPercentageCorrect,
                settings.liveRatingsDisableRatings,
                ::PuzzleInfoSettings,
            )
        )
    val uiState : StateFlow<PuzzleInfoUIState> = mediatedUIState.stateFlow

    /**
     * To show additional completion data when finished
     */
    val finishedViewModel = PuzzleFinishedDialogViewModel(
        viewModelScope,
        settings,
        currentPuzzleHolder,
    )

    init {
        fileHandlerProvider.get { fileHandler ->
            currentPuzzleHolder.puzHandle?.let { handle ->
                currentUIState = currentUIState.copy(
                    filename = fileHandler.getUri(handle).toString()
                )
            }
        }
    }

    fun setRating(rating : Int) {
        puzzle?.rating = rating.toChar()
        currentUIState = currentUIState.copy(
            rating = rating,
        )
    }

    private fun getUpdatedUIState(
        state : PuzzleInfoUIState,
        puzzleInfoSettings : PuzzleInfoSettings,
    ) : PuzzleInfoUIState {
        val puz = puzzle
        val showPercentageCorrect = puzzleInfoSettings.showPercentageCorrect
        val elapsed = timer?.let { it.getElapsed() } ?: puz?.time ?: 0L
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
            showRating = !puzzleInfoSettings.disableRatings,
            rating = puz?.rating?.code ?: 0,
        )
    }

    private var currentUIState : PuzzleInfoUIState
        get() { return mediatedUIState.current }
        set(value) { mediatedUIState.current = value }
}

class PuzzleFinishedDialogViewModel(
    private val viewModelScope : CoroutineScope,
    private val settings : ForkyzSettings,
    private val currentPuzzleHolder : CurrentPuzzleHolder,
) {
    protected val board : Playboard?
        get() { return currentPuzzleHolder.board }
    protected val puzzle : Puzzle?
        get() { return board?.puzzle }

    private val mediatedUIState
        = MediatedStateWithFlow<PuzzleFinishedUIState?, Boolean>(
            viewModelScope,
            null,
            this::getUpdatedUIState,
            settings.liveRatingsDisableRatings,
        )
    /**
     * UI state is null if puzzle not finished (or no puzzle)
     */
    val uiState : StateFlow<PuzzleFinishedUIState?> = mediatedUIState.stateFlow

    fun setRating(rating : Int) {
        puzzle?.rating = rating.toChar()
        currentUIState = currentUIState?.copy(rating = rating)
    }

    private fun getUpdatedUIState(
        state : PuzzleFinishedUIState?,
        disableRatings : Boolean,
    ) : PuzzleFinishedUIState? {
        val puz = puzzle
        if ((puz?.percentComplete ?: 0) < 100)
            return null

        val numBoxes = puz?.numberOfBoxes ?: 0
        val numCheated = puz?.numberOfCheatedBoxes ?: 0
        val percentCheated = if (numBoxes == 0)
            0
        else
            (100 * numCheated.toFloat() / numBoxes).toInt()

        return (state ?: PuzzleFinishedUIState()).copy(
            completionTime = puz?.time ?: 0L,
            numberOfClues = puz?.numberOfClues ?: 0,
            numberOfBoxes = numBoxes,
            numberOfCheatedBoxes = numCheated,
            percentCheated = percentCheated,
            completionMessage = puz?.completionMessage,
            showRating = !disableRatings,
            rating = puz?.rating?.code ?: 0,
        )
    }

    var currentUIState : PuzzleFinishedUIState?
        get() { return mediatedUIState.current }
        set(value) { mediatedUIState.current = value }
}

