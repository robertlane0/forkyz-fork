
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings

class JVMAppViewModel(
    val settings : ForkyzSettings,
) : ViewModel() {
    val undecorated : Boolean = runBlocking {
        settings.liveAppUndecorated.first()
    }
}


