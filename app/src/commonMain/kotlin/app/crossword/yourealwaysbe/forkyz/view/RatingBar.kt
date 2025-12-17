
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val UNSELECTED_ALPHA = 0.3F

enum class RatingBarStyle {
    SMALL,
    NORMAL,
}

@Composable
expect fun RatingBar(
    modifier : Modifier = Modifier,
    rating : Float = 0.0F,
    numStars : Int = 5,
    stepSize : Float = 1.0F,
    onSet : (Float) -> Unit = { },
    isIndicator : Boolean = false,
    style : RatingBarStyle = RatingBarStyle.NORMAL,
)

