
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.KeyboardBindings

open class ForkyzPageViewModel(
    protected val settings : ForkyzSettings,
) : ViewModel() {
    var keyboardBindings = KeyboardBindings()
        private set

    init {
        viewModelScope.launch {
            settings.liveKeyboardBindings.collect {
                keyboardBindings = it
            }
        }
    }
}

