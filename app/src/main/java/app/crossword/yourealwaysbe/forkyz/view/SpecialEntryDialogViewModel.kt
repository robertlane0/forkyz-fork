
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import androidx.lifecycle.LiveData

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.puz.Playboard

class SpecialEntryDialogViewModel(
    val settings : ForkyzSettings,
    val currentPuzzleHolder : CurrentPuzzleHolder,
) {
    private val _response = MutableStateFlow<String>("")
    val response : StateFlow<String> = _response

    val forceCaps : LiveData<Boolean> = settings.livePlaySpecialEntryForceCaps

    init {
        getBoard()?.getCurrentBox()?.let { box ->
            if (box.isBlank)
                _response.value = ""
            else
                _response.value = box.getResponse()
        }
    }

    fun playResponse() {
        getBoard()?.let { board ->
            board.playLetter(response.value)
        }
    }

    fun setResponse(response : String) {
        _response.value = if (forceCaps.value ?: false)
            response.uppercase()
        else
            response
    }

    fun setForceCaps(value : Boolean) {
        settings.setPlaySpecialEntryForceCaps(value)
    }

    private fun getBoard() : Playboard? = currentPuzzleHolder.board
}

