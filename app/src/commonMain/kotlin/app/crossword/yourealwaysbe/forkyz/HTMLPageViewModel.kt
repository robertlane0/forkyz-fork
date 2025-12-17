
package app.crossword.yourealwaysbe.forkyz

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale
import java.util.stream.Collectors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import org.jetbrains.compose.resources.MissingResourceException

import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.asset_not_found
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.html_file_full_path
import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

enum class HTMLFile(val unqualifiedFileName : String) {
    BROWSE_HELP("filescreen.html"),
    LICENSE("license.html"),
    PLAY_HELP("play.html"),
    RELEASE_NOTES("release.html"),
    SCRAPERS_HELP("scrapes.html"),
    VOICE_COMMANDS_HELP("voice_commands.html"),
}

private val DEFAULT_FILES_DIR = "files/en/%s"

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
                val path = utils.getString(
                    Res.string.html_file_full_path,
                    unqualifiedFileName,
                )
                val htmlData = Res.readBytes(path).decodeToString()
                _assetText.value = htmlToAnnotatedString(htmlData)
            } catch (e : MissingResourceException) {
                try {
                    val path = String.format(
                        Locale.US,
                        DEFAULT_FILES_DIR,
                        unqualifiedFileName,
                    )
                    val htmlData = Res.readBytes(path).decodeToString()
                    _assetText.value = htmlToAnnotatedString(htmlData)
                } catch (e : IOException) {
                    _assetText.value = buildAnnotatedString {
                        append(utils.getString(Res.string.asset_not_found))
                    }
                }
            } catch (e : IOException) {
                _assetText.value = buildAnnotatedString {
                    append(utils.getString(Res.string.asset_not_found))
                }
            }
        }
    }
}

