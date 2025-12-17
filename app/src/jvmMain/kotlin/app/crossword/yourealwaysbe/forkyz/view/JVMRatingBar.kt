
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun RatingBar(
    modifier : Modifier,
    rating : Float,
    numStars : Int,
    stepSize : Float,
    onSet : (Float) -> Unit,
    isIndicator : Boolean,
    style : RatingBarStyle,
) {
    // TODO
}

