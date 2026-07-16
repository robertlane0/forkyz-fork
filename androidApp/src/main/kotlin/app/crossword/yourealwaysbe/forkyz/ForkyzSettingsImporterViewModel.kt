
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_import_failed
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.preferences_imported
import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri

private val TAG = "ForkyzSettingsImporterViewModel"

class ForkyzSettingsImporterViewModel(
    private val uri : Uri,
) : ViewModel(), KoinComponent {
    private val settings : ForkyzSettings by inject()
    private val utils : NativeBackendUtils by inject()

    private val _finishMessage = MutableStateFlow<String?>(null)
    val finishMessage : StateFlow<String?> = _finishMessage

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val messageRes = try {
                utils.openInput(uri).use(settings::importSettings)
                Res.string.preferences_imported
            } catch (e : IOException) {
                AppLogger.e(TAG, "Failed to import settings", e)
                Res.string.preferences_import_failed
            }
            _finishMessage.value = utils.getString(messageRes)
        }
    }
}

