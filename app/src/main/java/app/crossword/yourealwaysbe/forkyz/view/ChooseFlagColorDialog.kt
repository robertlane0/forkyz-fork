
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.util.ColorUtils
import app.crossword.yourealwaysbe.puz.Clue

/**
 * Choose the flag color
 *
 * @param onChoose when oked with color as int
 */
@Composable
fun ChooseFlagColorDialog(
    selectedColor : Int,
    onCancel : () -> Unit,
    onChoose : (Int) -> Unit,
) {
    val colors = listOf(
        Clue.DEFAULT_FLAG_COLOR,
        getContextColor(R.color.flagColor1),
        getContextColor(R.color.flagColor2),
        getContextColor(R.color.flagColor3),
        getContextColor(R.color.flagColor4),
    )
    val labels = stringArrayResource(R.array.flag_color_labels).toList()
    var selected by remember {
        mutableIntStateOf(
            Math.max(
                0,
                colors.indexOfFirst { it == selectedColor },
            )
        )
    }

    ListDialog(
        R.string.flag_color_title,
        labels = labels,
        selected = selected,
        colorOf = { index ->
            var color = colors.getOrElse(index) { colors[0] }
            if (color == Clue.DEFAULT_FLAG_COLOR)
                color = getContextColor(R.color.flagColor)
            Color(ColorUtils.addAlpha(color))
        },
        onDismissRequest = onCancel,
        onSelect = { index ->
            onChoose(colors.getOrElse(index) { colors[0] })
        },
    )
}

/**
 * Get color from context and remove alpha channel
 */
@Composable
private fun getContextColor(id : Int) : Int {
    return ColorUtils.delAlpha(colorResource(id).toArgb())
}


