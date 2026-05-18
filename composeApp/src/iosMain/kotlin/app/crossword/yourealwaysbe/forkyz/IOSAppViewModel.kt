
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.settings.ExternalDictionarySetting
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings

class IOSAppViewModel(
    val settings : ForkyzSettings,
) : ViewModel() {

    init {
        // disable some settings that don't work on iOS
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
}
