
package app.crossword.yourealwaysbe.forkyz.view

import kotlin.math.max

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

import org.jetbrains.compose.resources.stringArrayResource

import app.crossword.yourealwaysbe.forkyz.app.generated.resources.Res
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.flag_color_labels
import app.crossword.yourealwaysbe.forkyz.app.generated.resources.flag_color_title
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.theme.BoardColorScheme
import app.crossword.yourealwaysbe.forkyz.util.addAlpha
import app.crossword.yourealwaysbe.forkyz.util.delAlpha

/**
 * Choose the flag color
 *
 * @param onChoose when oked with color as int
 */
@Composable
fun ChooseFlagColorDialog(
    selectedColor : Int,
    boardColorScheme : BoardColorScheme,
    onCancel : () -> Unit,
    onChoose : (Int) -> Unit,
) {
    val colors = listOf(
        Clue.DEFAULT_FLAG_COLOR,
        colorToInt(boardColorScheme.flagColor1),
        colorToInt(boardColorScheme.flagColor2),
        colorToInt(boardColorScheme.flagColor3),
        colorToInt(boardColorScheme.flagColor4),
    )
    val labels = stringArrayResource(Res.array.flag_color_labels).toList()
    var selected by remember {
        mutableIntStateOf(
            max(0, colors.indexOfFirst { it == selectedColor })
        )
    }

    ListDialog(
        Res.string.flag_color_title,
        labels = labels,
        selected = selected,
        colorOf = { index ->
            var color = colors.getOrElse(index) { colors[0] }
            if (color == Clue.DEFAULT_FLAG_COLOR)
                color = colorToInt(boardColorScheme.flagColor)
            Color(addAlpha(color))
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
private fun colorToInt(color : Color) : Int {
    return delAlpha(color.toArgb())
}

