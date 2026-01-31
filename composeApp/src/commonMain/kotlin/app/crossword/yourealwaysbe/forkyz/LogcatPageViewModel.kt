
package app.crossword.yourealwaysbe.forkyz

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.could_not_load_logcat
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.Uri
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared

data class ShareLogcatData(
    val mimeType : String,
    val uri : Uri,
)

class LogcatPageViewModel(
    val utils : NativeBackendUtils,
) : ViewModel() {

    private val _logcatText = MutableStateFlow<List<String>>(listOf())
    val logcatText : StateFlow<List<String>> = _logcatText

    private val _shareLogcatEvent = MutableStateFlow<ShareLogcatData?>(null)
    val shareLogcatEvent : StateFlow<ShareLogcatData?> = _shareLogcatEvent

    fun loadLogcat() {
        viewModelScope.launch(Dispatchers.IO) {
            _logcatText.value = utils.getLogcat()
                ?: listOf(utils.getString(Res.string.could_not_load_logcat))
        }
    }

    fun shareLogcat() {
        FileHandlerShared.getLogcatShareUri(
            viewModelScope,
            utils,
            logcatText.value,
        ) {
            it?.let { uri ->
                val mimeType = FileHandlerShared.LOGCAT_SHARE_URI_MIME_TYPE
                _shareLogcatEvent.value = ShareLogcatData(mimeType, uri)
            }
        }
    }

    fun clearShareLogcatEvent() {
        _shareLogcatEvent.value = null
    }
}

