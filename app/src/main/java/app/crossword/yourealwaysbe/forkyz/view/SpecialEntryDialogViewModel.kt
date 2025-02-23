
package app.crossword.yourealwaysbe.forkyz.view

import java.util.logging.Logger
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import android.app.Application
import androidx.lifecycle.AndroidViewModel

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.util.CurrentPuzzleHolder
import app.crossword.yourealwaysbe.puz.Playboard

@HiltViewModel
open class SpecialEntryDialogViewModel @Inject constructor(
    application : Application,
    val currentPuzzleHolder : CurrentPuzzleHolder,
) : AndroidViewModel(application) {
    private val LOG = Logger.getLogger(
        SpecialEntryDialogViewModel::class.toString()
    )

    private val _response = MutableStateFlow<String>("")
    val response : StateFlow<String> = _response

    init {
        getBoard()?.getCurrentBox()?.let { box ->
            if (box.isBlank)
                _response.value = ""
            else
                _response.value = box.getResponse()
        }
    }

    fun playResponse(response : String) {
        getBoard()?.let { board ->
            board.playLetter(response)
        }
        setResponse(response)
    }

    fun setResponse(response : String) {
        _response.value = response
    }

    private fun getBoard() : Playboard? = currentPuzzleHolder.board
}

