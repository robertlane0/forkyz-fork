
package app.crossword.yourealwaysbe.forkyz

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.stream.Collectors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import app.crossword.yourealwaysbe.forkyz.util.NativeBackendUtils

class HTMLPageViewModel(
    private val utils : NativeBackendUtils,
) : ViewModel() {

    private val _assetText = MutableStateFlow<AnnotatedString>(
        buildAnnotatedString { }
    )
    val assetText : StateFlow<AnnotatedString> = _assetText

    fun loadAsset(rawAssetID : Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                BufferedReader(
                    InputStreamReader(utils.getRawResource(rawAssetID))
                ).use { reader ->
                    val htmlData
                        = reader.lines().collect(Collectors.joining("\n"))
                    _assetText.value =  AnnotatedString.fromHtml(htmlData)
                }
            } catch (e : IOException) {
                _assetText.value = buildAnnotatedString {
                    append(utils.getString(R.string.asset_not_found))
                }
            }
        }
    }
}

