
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val UNSELECTED_ALPHA = 0.3F
private val NORMAL_STAR_SIZE = 36.dp
private val SMALL_STAR_SIZE = 16.dp

enum class RatingBarStyle {
    SMALL,
    NORMAL,
}

/**
 * A star rating bar, styled to look like the standard Android
 * RatingBar widget. Ratings are always whole numbers of stars.
 * Clicking a star sets the rating to that star's number, except
 * clicking the first star toggles the rating between 0 and 1.
 */
@Composable
fun RatingBar(
    modifier : Modifier = Modifier,
    rating : Int = 0,
    numStars : Int = 5,
    onSet : (Int) -> Unit = { },
    isIndicator : Boolean = false,
    style : RatingBarStyle = RatingBarStyle.NORMAL,
) {
    val starSize = when (style) {
        RatingBarStyle.SMALL -> SMALL_STAR_SIZE
        RatingBarStyle.NORMAL -> NORMAL_STAR_SIZE
    }

    val selectedColor = MaterialTheme.colorScheme.primary
    val unselectedColor = selectedColor.copy(alpha = UNSELECTED_ALPHA)

    val clampedRating = rating.coerceIn(0, numStars)

    Row(modifier = modifier) {
        for (starNumber in 1..numStars) {
            val filled = starNumber <= clampedRating

            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (filled) selectedColor else unselectedColor,
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (isIndicator) {
                            Modifier
                        } else {
                            Modifier.clickable {
                                val newRating =
                                    if (starNumber == 1 && clampedRating == 1)
                                        0
                                    else
                                        starNumber
                                onSet(newRating)
                            }
                        }
                    ),
            )
        }
    }
}
