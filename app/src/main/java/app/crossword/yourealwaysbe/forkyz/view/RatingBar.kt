
package app.crossword.yourealwaysbe.forkyz.view

import android.content.res.ColorStateList
import android.widget.RatingBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun RatingBar(
    modifier : Modifier = Modifier,
    rating : Float = 0.0F,
    numStars : Int = 5,
    stepSize : Float = 1.0F,
    onSet : (Float) -> Unit = { },
    isIndicator : Boolean = false,
    style : Int = 0,
) {
    val primary = MaterialTheme.colorScheme.primary.toArgb()
    val background = MaterialTheme.colorScheme.background.toArgb()
    val selectedStateList = ColorStateList(
        arrayOf(intArrayOf()),
        intArrayOf(primary),
    )
    val unSelectedStateList = ColorStateList(
        arrayOf(intArrayOf()),
        intArrayOf(background),
    )
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = if (style != 0)
                RatingBar(context, null, style)
            else
                RatingBar(context)


            view.apply {
                setNumStars(numStars)
                setStepSize(stepSize)
                setIsIndicator(isIndicator)
                setProgressTintList(selectedStateList)
                setProgressBackgroundTintList(unSelectedStateList)
            }

            view.setOnRatingBarChangeListener(
                { _, rating, _ ->  onSet(rating) }
            )

            view
        },
        update = { view ->
            view.apply {
                setRating(Math.max(0.0F, Math.min(rating, numStars.toFloat())))
            }
        }
    )
}
