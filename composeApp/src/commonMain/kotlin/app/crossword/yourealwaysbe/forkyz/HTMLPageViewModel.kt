
package app.crossword.yourealwaysbe.forkyz


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO // needed on IOS, not JVM or Android
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.io.IOException

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.asset_not_found
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils
import app.crossword.yourealwaysbe.forkyz.util.getFileResourceData

enum class HTMLFile(val unqualifiedFileName : String) {
    BROWSE_HELP("filescreen.html"),
    LICENSE("license.html"),
    PLAY_HELP("play.html"),
    RELEASE_NOTES("release.html"),
    SCRAPERS_HELP("scrapes.html"),
    VOICE_COMMANDS_HELP("voice_commands.html"),
}

class HTMLPageViewModel(
    private val utils : NativeBackendUtils,
) : ViewModel() {

    private val _assetText = MutableStateFlow<AnnotatedString>(
        buildAnnotatedString { }
    )
    val assetText : StateFlow<AnnotatedString> = _assetText

    /**
     * Pass one of the unqualifiedFileNames from HTMLFile
     */
    fun loadAsset(unqualifiedFileName : String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val htmlData = getFileResourceData(utils, unqualifiedFileName)
                _assetText.value = htmlToAnnotatedString(htmlData)
            } catch (e : IOException) {
                _assetText.value = buildAnnotatedString {
                    append(utils.getString(Res.string.asset_not_found))
                }
            }
        }
    }
}

