
package app.crossword.yourealwaysbe.forkyz.view

import android.widget.RatingBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun RatingBar(
    modifier : Modifier = Modifier,
    rating : Float = 0.0F,
    numStars : Int = 5,
    stepSize : Float = 1.0F,
    isIndicator : Boolean = false,
    style : Int = 0,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RatingBar(context, null, style).apply {
                setNumStars(numStars)
                setStepSize(stepSize)
                setIsIndicator(isIndicator)
            }
        },
        update = { view ->
            view.apply {
                setRating(Math.max(0.0F, Math.min(rating, numStars.toFloat())))
            }
        }
    )
}
