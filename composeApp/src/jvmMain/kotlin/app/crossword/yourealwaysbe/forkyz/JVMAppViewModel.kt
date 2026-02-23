
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.settings.ExternalDictionarySetting
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings

class JVMAppViewModel(
    val settings : ForkyzSettings,
) : ViewModel() {

    init {
        // disable some settings that don't work
        viewModelScope.launch {
            val extDict = settings.liveExtDictionarySetting.first()
            if (extDict != ExternalDictionarySetting.EDS_NONE) {
                settings.setExtDictionarySetting(
                    ExternalDictionarySetting.EDS_NONE,
                )
            }

            val extSolver = settings.liveExtCrosswordSolverEnabled.first()
            if (extSolver)
                settings.setExtCrosswordSolverEnabled(false)
        }
    }

    val undecorated : Boolean = runBlocking {
        settings.liveAppUndecorated.first()
    }
}


