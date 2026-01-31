
package app.crossword.yourealwaysbe.forkyz.view

import android.content.res.ColorStateList
import android.widget.RatingBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

private val UNSELECTED_ALPHA = 0.3F

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
    val selected = MaterialTheme.colorScheme.primary.toArgb()
    val unselected = MaterialTheme.colorScheme
        .primary
        .copy(alpha=UNSELECTED_ALPHA)
        .toArgb()
    val selectedStateList = ColorStateList(
        arrayOf(intArrayOf()),
        intArrayOf(selected),
    )
    val unSelectedStateList = ColorStateList(
        arrayOf(intArrayOf()),
        intArrayOf(unselected),
    )
    val androidStyle = when (style) {
        RatingBarStyle.SMALL -> android.R.attr.ratingBarStyleSmall
        else -> 0
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val view = if (androidStyle != 0)
                RatingBar(context, null, androidStyle)
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

