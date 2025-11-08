
package app.crossword.yourealwaysbe.forkyz

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel

import app.crossword.yourealwaysbe.forkyz.versions.AndroidVersionUtils
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.files.FileHandlerShared

data class ShareLogcatData(
    val mimeType : String,
    val uri : Uri,
)

@HiltViewModel
class LogcatPageViewModel @Inject constructor(
    application : Application,
    val utils : NativeBackendUtils,
    val versionUtils : AndroidVersionUtils,
) : AndroidViewModel(application) {

    private val _logcatText = MutableStateFlow<List<String>>(listOf())
    val logcatText : StateFlow<List<String>> = _logcatText

    private val _shareLogcatEvent = MutableStateFlow<ShareLogcatData?>(null)
    val shareLogcatEvent : StateFlow<ShareLogcatData?> = _shareLogcatEvent

    fun loadLogcat() {
        viewModelScope.launch(Dispatchers.IO) {
            val app : Application = getApplication()
            val uid = versionUtils.getApplicationUID(app)
            val uidFlag : String = if (uid < 0) {
                ""
            } else {
                " --uid " + uid.toString()
            }
            val process = Runtime.getRuntime().exec("logcat -d" + uidFlag)
            try {
                BufferedReader(
                    InputStreamReader(process.getInputStream())
                ).useLines {
                    _logcatText.value = it.toList()
                }
            } catch (e : IOException) {
                _logcatText.value = listOf(
                    app.getString(
                        R.string.could_not_load_logcat,
                        e.toString(),
                    )
                )
            }
        }
    }

    fun shareLogcat() {
        FileHandlerShared.getLogcatShareUri(
            utils,
            logcatText.value,
        ) {
            it?.let { uri ->
                val mimeType = FileHandlerShared.getLogcatShareUriMimeType()
                _shareLogcatEvent.value = ShareLogcatData(mimeType, uri)
            }
        }
    }

    fun clearShareLogcatEvent() {
        _shareLogcatEvent.value = null
    }
}

