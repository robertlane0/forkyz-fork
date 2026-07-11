
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO // needed on IOS, not JVM or Android
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.getFileResourceData

class AboutLibrariesPageViewModel(
    settings : ForkyzSettings,
    utils : NativeBackendUtils,
) : ForkyzPageViewModel(settings) {

    private val _rawLibraryData = MutableStateFlow<String?>(null)
    val rawLibraryData : StateFlow<String?> = _rawLibraryData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _rawLibraryData.value = getFileResourceData(
                utils,
                "aboutlibraries.json",
            )
        }
    }
}

