
package app.crossword.yourealwaysbe.forkyz.view

import kotlinx.parcelize.Parcelize

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable

@Parcelize
class ScrollingImageViewState(
    var x : Int = 0,
    var y : Int = 0,
    var scale : Float = 1.0f,
) : Parcelable

@Composable
fun rememberScrollingImageViewState() : ScrollingImageViewState {
    return rememberSaveable() { ScrollingImageViewState() }
}

