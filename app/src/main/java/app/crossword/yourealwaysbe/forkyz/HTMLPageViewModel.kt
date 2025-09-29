
package app.crossword.yourealwaysbe.forkyz

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.stream.Collectors
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import android.app.Application
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class HTMLPageViewModel @Inject constructor(
    application : Application,
) : AndroidViewModel(application) {

    private val _assetText = MutableStateFlow<AnnotatedString>(
        buildAnnotatedString { }
    )
    val assetText : StateFlow<AnnotatedString> = _assetText

    fun loadAsset(rawAssetID : Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val app : Application = getApplication()
            try {
                BufferedReader(
                    InputStreamReader(
                        app.getResources().openRawResource(rawAssetID)
                    )
                ).use { reader ->
                    val htmlData
                        = reader.lines().collect(Collectors.joining("\n"))
                    _assetText.value =  AnnotatedString.fromHtml(htmlData)
                }
            } catch (e : IOException) {
                _assetText.value = buildAnnotatedString {
                    append(app.getString(R.string.asset_not_found))
                }
            }
        }
    }
}

