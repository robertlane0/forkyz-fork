
package app.crossword.yourealwaysbe.forkyz.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

import app.crossword.yourealwaysbe.forkyz.R
import app.crossword.yourealwaysbe.forkyz.theme.CircleProgressBarColorScheme

@Composable
fun CircleProgressBar(
    modifier : Modifier = Modifier,
    colors : CircleProgressBarColorScheme,
    complete : Boolean,
    percentFilled : Int,
) {
    val icPlay = painterResource(id = R.drawable.ic_play)
    val icDone = painterResource(id = R.drawable.ic_done)
    val textMeasurer = rememberTextMeasurer()
    val localDensity = LocalDensity.current
    val density = localDensity.density

    Canvas(modifier = modifier) {
        val circleStroke = density * 6F
        val circleFine = density * 2f
        val halfWidth = size.width / 2
        val halfStroke = circleStroke / 2

        fun Text(
            text : String,
            color : Color,
        ) {
            val fontSize = with (localDensity) {
                (halfWidth * 0.5f).toSp()
            }
            val textResult = textMeasurer.measure(
                text = text,
                style = TextStyle(
                    color = color,
                    fontSize = fontSize,
                    fontFamily = FontFamily.SansSerif,
                ),
            )
            val textSize = textResult.size
            drawText(
                textLayoutResult = textResult,
                topLeft = Offset(
                    x  = halfWidth - textSize.width * 0.5f,
                    y = halfWidth - textSize.height * 0.5f,
                ),
            )
        }

        if (complete) {
            drawCircle(
                colors.progressDone,
                radius = halfWidth - halfStroke - density * 2f,
                style = Stroke(width = circleStroke),
            )
            val start = halfWidth * 0.5f
            translate(left = start, top = start) {
                with (icDone) {
                    draw(
                        colorFilter = ColorFilter.tint(colors.progressDone),
                        size = Size(halfWidth, halfWidth),
                    )
                }
            }
        } else if (percentFilled < 0) {
            drawCircle(
                color = colors.progressError,
                radius = halfWidth - halfStroke - density * 2f,
                style =  Stroke(width = circleStroke),
            )
            Text(text = "?", color = colors.progressError)
        } else if (percentFilled == 0) {
            val size = halfWidth * 0.75f
            val start = halfWidth - size * 0.5f
            translate(left = start, top = start) {
                with(icPlay) {
                    draw(
                        colorFilter = ColorFilter.tint(colors.progressNull),
                        size = Size(
                            halfWidth * 0.75f,
                            halfWidth * 0.75f,
                        ),
                    )
                }
            }
        } else {
            drawCircle(
                color = colors.progressInProgress,
                radius = halfWidth - halfStroke - 1f,
                style =  Stroke(width = circleFine),
            )

            val arcOffset = Offset(0 + circleStroke, 0 + circleStroke)
            val arcSize = Size(
                size.width - 2 * circleStroke,
                size.width - 2 * circleStroke,
            )
            drawArc(
                color = colors.progressInProgress,
                startAngle = -90f,
                sweepAngle = 360F * percentFilled / 100F,
                useCenter = false,
                topLeft = arcOffset,
                size = arcSize,
                style = Stroke(width = circleStroke)
            )

            Text(
                text = percentFilled.toString() + "%",
                color = colors.progressInProgress,
            )
        }
    }
}

