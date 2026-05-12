
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import app.crossword.yourealwaysbe.forkyz.puz.Playboard
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.forkyz.util.stateInSubscribed

class SpecialEntryDialogViewModel(
    val viewModelScope : CoroutineScope,
    val settings : ForkyzSettings,
    val currentPuzzleHolder : CurrentPuzzleHolder,
) {
    private val _response = MutableStateFlow<String>("")
    val response : StateFlow<String> = _response

    val forceCaps : StateFlow<Boolean>
        = settings.livePlaySpecialEntryForceCaps.stateInSubscribed(
            viewModelScope,
            false,
        )

    init {
        getBoard()?.currentBox?.let { box ->
            if (box.isBlank())
                _response.value = ""
            else
                _response.value = box.response
        }
    }

    fun playResponse() {
        getBoard()?.let { board ->
            board.playLetter(response.value)
        }
    }

    fun setResponse(response : String) {
        settings.getPlaySpecialEntryForceCaps() { forceCaps ->
            _response.value = if (forceCaps)
                response.uppercase()
            else
                response
        }
    }

    fun setForceCaps(value : Boolean) {
        settings.setPlaySpecialEntryForceCaps(value)
    }

    private fun getBoard() : Playboard? = currentPuzzleHolder.board
}

