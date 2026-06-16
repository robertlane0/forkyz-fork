
package app.crossword.yourealwaysbe.forkyz.view

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import coil3.compose.AsyncImage

import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

import app.crossword.yourealwaysbe.forkyz.theme.BoardColorScheme

val BASE_BOX_SIZE_DP : Float = 40f
private val BOX_BORDER_DP : Dp = 0.5.dp
// fraction of box size for 1-letter responses
private val RESPONSE_BOX_FRAC : Float  = 0.7f
private val SCRATCH_BOX_FRAC : Float  = 0.6f
private val MINI_SCRATCH_BOX_FRAC : Float  = 0.5f
private val RESPONSE_TOP_PADDING_BOX_FRAC : Float  = 0.1f
private val SCRATCH_TOP_PADDING_BOX_FRAC : Float  = 0.125f
private val MARK_BOX_FRAC : Float  = 0.25f
private val BAR_WIDTH_FRAC : Float = 0.0769f
private val DASH_SIZE_FRAC : Float = 0.11f
private val SEPARATOR_DASH_SIZE_FRAC : Float = 0.2f
private val SEPARATOR_TEXT_BOX_FRAC : Float = 0.25f
private val WHITESPACE_RE = "\\s+".toRegex()
private val SEPARATOR_HORIZONTAL_TEXT_OFFSET_FRAC : Float = 0.05f
private val MULTI_CHAR_RESPONSE_SIZE_FUDGE_FACTOR : Float = 0.8f
// remember 26 characters and 100 numbers and 5 different zooms * 3 for
// good measure!
private val TEXT_MEASURE_CACHE_SIZE : Int = (26 + 100) * 5 * 3

/**
 * Rendering data
 *
 * Text styles do not have color information as that depends on the box.
 * Use getTextStyle along with the right colors from BoardColorScheme.
 */
data class BoxProfile(
    val colors : BoardColorScheme,
    val boxSize : Dp,
    val boxSizePx : Float,
    val boxBorderPx : Float,
    val barWidth : Float,
    val responseTextStyle : TextStyle,
    val responseTopPaddingPx : Float,
    val markTextStyle : TextStyle,
    val markOffset : Float,
    val markTextSizePx : Float,
    val scratchTextStyle : TextStyle,
    val scratchTopPaddingPx : Float,
    val miniScratchTextStyle : TextStyle,
    val dashedEffect : PathEffect,
    val dottedEffect : PathEffect,
    val separatorTextStyle : TextStyle,
    val separatorDashSize : Float,
    val separatorHorizontalTextOffset : Float,
) {
    companion object {
        fun build(
            density : Density,
            colors : BoardColorScheme,
            scale : Float,
            defaultTextStyle : TextStyle
        ) : BoxProfile {
            val boxSize = BASE_BOX_SIZE_DP.dp * scale
            val boxSizePx = with (density) { boxSize.toPx() }
            val boxBorderPx = with (density) { BOX_BORDER_DP.toPx() }
            val barWidth = with (density) {
                (boxSize * BAR_WIDTH_FRAC).toPx()
            }
            val responseTextSize = with (density) {
                (boxSize * RESPONSE_BOX_FRAC).toSp()
            }
            val markTextSize = with (density) {
                (boxSize * MARK_BOX_FRAC).toSp()
            }
            val markTextSizePx = with (density) {
                (boxSize * MARK_BOX_FRAC).toPx()
            }
            val scratchTextSize = with (density) {
                (boxSize * SCRATCH_BOX_FRAC).toSp()
            }
            val miniScratchTextSize = with (density) {
                (boxSize * MINI_SCRATCH_BOX_FRAC).toSp()
            }
            val separatorTextSize = with (density) {
                (boxSize * SEPARATOR_TEXT_BOX_FRAC).toSp()
            }
            val baseTextStyle = defaultTextStyle.copy(
                fontSize = responseTextSize,
                fontFamily = FontFamily.SansSerif,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                ),
                color = colors.boardLetterColor,
            )
            val responseTextStyle
                = baseTextStyle.copy(fontSize = responseTextSize)
            val responseTopPaddingPx = boxSizePx * RESPONSE_TOP_PADDING_BOX_FRAC
            val markTextStyle = baseTextStyle.copy(fontSize = markTextSize)
            val scratchTextStyle = baseTextStyle.copy(
                fontSize = scratchTextSize,
                fontWeight = FontWeight.SemiBold,
            )
            val scratchTopPaddingPx = with (density) {
                (boxSize * SCRATCH_TOP_PADDING_BOX_FRAC).toPx()
            }
            val miniScratchTextStyle
                = scratchTextStyle.copy(fontSize = miniScratchTextSize)
            val dashSize = boxSizePx * DASH_SIZE_FRAC
            val separatorTextStyle
                = baseTextStyle.copy(fontSize = separatorTextSize)

            return BoxProfile(
                colors = colors,
                boxSize = boxSize,
                boxSizePx = boxSizePx,
                boxBorderPx = boxBorderPx,
                barWidth = barWidth,
                responseTextStyle = responseTextStyle,
                responseTopPaddingPx = responseTopPaddingPx,
                markTextStyle = markTextStyle,
                markOffset = barWidth,
                markTextSizePx = markTextSizePx,
                dashedEffect = PathEffect.dashPathEffect(
                    floatArrayOf(2 * dashSize, dashSize),
                    dashSize,
                ),
                dottedEffect = PathEffect.dashPathEffect(
                    floatArrayOf(barWidth, barWidth),
                    0f,
                ),
                scratchTextStyle = scratchTextStyle,
                scratchTopPaddingPx = scratchTopPaddingPx,
                miniScratchTextStyle = miniScratchTextStyle,
                separatorTextStyle = separatorTextStyle,
                separatorDashSize = boxSizePx * SEPARATOR_DASH_SIZE_FRAC,
                separatorHorizontalTextOffset
                    = boxSizePx * SEPARATOR_HORIZONTAL_TEXT_OFFSET_FRAC,
            )
        }
    }
}

/**
 * A text measurer to use for boards
 *
 * Since lots of same letters drawn at same scale, best to share a
 * cache between word edits. The full board keeps its own because it has
 * its own scale that is unlikely to match other board views.
 */
@Composable
fun rememberBoardTextMeasurer() : TextMeasurer {
    return rememberTextMeasurer(cacheSize = TEXT_MEASURE_CACHE_SIZE)
}


/**
 * Draw full board
 *
 * Assumes on a blockColor background and skips drawing the background
 * again.
 */
@Composable
fun Board(
    modifier : Modifier = Modifier,
    width : Int,
    height : Int,
    boxes : ImmutableList<ImmutableList<BoxState?>>,
    images : ImmutableList<Image> = persistentListOf(),
    profile : BoxProfile,
) {
    val textMeasurer = rememberTextMeasurer(cacheSize = TEXT_MEASURE_CACHE_SIZE)
    Box(
        modifier = modifier
            .requiredSize(
                profile.boxSize * width,
                profile.boxSize * height,
            ),
        contentAlignment = Alignment.TopStart,
    ) {
        boxes.forEachIndexed { row, rowBoxes ->
            rowBoxes.forEachIndexed { col, box ->
                if (box != null) {
                    Square(
                        modifier = Modifier.graphicsLayer {
                            translationX = profile.boxSizePx * col
                            translationY = profile.boxSizePx * row
                        },
                        textMeasurer = textMeasurer,
                        profile = profile,
                        box = box,
                    )
                }
            }
        }

        images.forEach { image ->
            AsyncImage(
                modifier = Modifier.requiredSize(
                    profile.boxSize * image.width,
                    profile.boxSize * image.height,
                ).graphicsLayer {
                    translationX = profile.boxSizePx * image.col
                    translationY = profile.boxSizePx * image.row
                },
                model = image.url,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
            )
        }
    }
}

/**
 * Draw a single word
 *
 * Does not assume on a blockColor background
 */
@Composable
fun BoardWord(
    modifier : Modifier = Modifier,
    textMeasurer : TextMeasurer,
    profile : BoxProfile,
    boxes : ImmutableList<BoxState?>,
    widthBoxes : Int,
) {
    if (boxes.size == 0 || widthBoxes <= 0)
        return

    val textMeasurer = rememberTextMeasurer(cacheSize = TEXT_MEASURE_CACHE_SIZE)
    val width = min(boxes.size, widthBoxes)
    val height = ceil(boxes.size / widthBoxes.toDouble()).toInt()

    Box(
        modifier = modifier.requiredSize(
            profile.boxSize * width,
            profile.boxSize * height,
        ),
    ) {
        boxes.forEachIndexed { index, box ->
            if (box != null) {
                val row = index / width
                val col = index % width
                Square(
                    modifier = Modifier.graphicsLayer {
                        translationX = col * profile.boxSizePx
                        translationY = row * profile.boxSizePx
                    }.drawBehind {
                        // box size
                        val bs = profile.boxSizePx
                        // box border
                        val bb = profile.boxBorderPx
                        // half box border
                        val hbb = 0.5f * bb

                        drawRect(
                            color = profile.colors.blockColor,
                            topLeft = Offset(hbb, hbb),
                            size = Size(bs - bb, bs - bb),
                            style = Stroke(width = bb),
                        )
                    },
                    textMeasurer,
                    profile,
                    box,
                )
            }
        }
    }
}


@Composable
private fun Square(
    modifier : Modifier = Modifier,
    textMeasurer : TextMeasurer,
    profile : BoxProfile,
    box : BoxState,
) {
    val clueNumber = box.clueNumber?.let { number ->
        remember(number) { htmlToAnnotatedString(number) }
    }
    Box(
        modifier = modifier.requiredSize(profile.boxSize, profile.boxSize)
            .drawBehind {
                drawBackground(profile, box)
                drawError(profile, box)
                drawBars(profile, box)
                drawShape(profile, box)
                drawFlags(profile, box)
                drawSeparators(textMeasurer, profile, box)
                drawClueNumber(textMeasurer, profile, box, clueNumber)
                drawMarks(textMeasurer, profile, box)
                drawScratch(textMeasurer, profile, box)
                drawResponse(textMeasurer, profile, box)
            },
    )
}

private fun DrawScope.drawResponse(
    textMeasurer : TextMeasurer,
    profile : BoxProfile,
    box : BoxState,
) {
    if (!box.isBlank) {
        val baseStyle = getResponseTextStyle(profile, box)
        val style = if (box.response.length <= 1) {
            baseStyle
        } else {
            val textResult = textMeasurer.measure(
                text = box.response,
                style = baseStyle,
            )
            val size = textResult.size
            val factor = profile.boxSizePx / size.width
            val fontSize = (
                baseStyle.fontSize * min(
                    1f,
                    factor * MULTI_CHAR_RESPONSE_SIZE_FUDGE_FACTOR,
                )
            )
            baseStyle.copy(fontSize = fontSize)
        }

        drawOffsetText(
            textMeasurer,
            style,
            box.response,
            profile.boxSizePx / 2,
            profile.boxSizePx / 2 + profile.responseTopPaddingPx,
            -0.5f,
            -0.5f,
        )
    }
}

private fun DrawScope.drawBackground(
    profile : BoxProfile,
    box : BoxState,
) {
    // box size
    val bs = profile.boxSizePx
    // box border
    val bb = profile.boxBorderPx

    drawRect(
        color = getBoxColor(profile, box),
        topLeft = Offset(bb, bb),
        size = Size(bs - 2 * bb, bs - 2 * bb),
    )
}

private fun DrawScope.drawError(
    profile : BoxProfile,
    box : BoxState,
) {
    box.errorBarState?.let { barState ->
        drawBars(profile, barState, profile.colors.errorColor)
    }
}

private fun DrawScope.drawBars(
    profile : BoxProfile,
    box : BoxState,
) {
    box.barState?.let { barState ->
        drawBars(profile, barState, getBarColor(profile, box))
    }
}

private fun DrawScope.drawBars(
    profile : BoxProfile,
    barState : BarState,
    barColor : Color,
) {
    // box size
    val bs = profile.boxSizePx
    // half stroke
    val hs = 0.5f * profile.barWidth

    drawBar(profile, barColor, barState.top, 0f, hs, bs, hs)
    drawBar(profile, barColor, barState.bottom, 0f, bs - hs, bs, bs - hs)
    drawBar(profile, barColor, barState.left, hs, 0f, hs, bs)
    drawBar(profile, barColor, barState.right, bs - hs, 0f, bs - hs, bs)
}

private fun DrawScope.drawBar(
    profile : BoxProfile,
    barColor : Color,
    bar : Bar,
    left : Float,
    top : Float,
    right : Float,
    bottom : Float,
) {
    if (bar != Bar.NONE) {
        val pathEffect = when (bar) {
            Bar.DASHED -> profile.dashedEffect
            Bar.DOTTED -> profile.dottedEffect
            else -> null
        }
        drawLine(
            start = Offset(left, top),
            end = Offset(right, bottom),
            strokeWidth = profile.barWidth,
            pathEffect = pathEffect,
            color = barColor,
        )
    }
}

private fun DrawScope.drawFlags(
    profile : BoxProfile,
    box : BoxState,
) {
    val flags = box.flagsState

    // box size
    val bs = profile.boxSizePx
    // bar width
    val bw = profile.barWidth
    // half stroke
    val hs = 0.5f * bw
    // mark offset
    val mo = profile.markOffset
    // mark text size
    val mts = profile.markTextSizePx

    if (flags.flagAcross) {
        val color = flags.flagAcrossColor ?: profile.colors.flagColor
        drawLine(
            start = Offset(bw + hs, bw + mo + mts),
            end = Offset(bw + hs, bs - bw),
            strokeWidth = profile.barWidth,
            color = color,
        )
    }

    if (flags.flagDown) {
        val color = flags.flagDownColor ?: profile.colors.flagColor
        val numDigits = box.clueNumber?.length ?: 0
        val numWidth = numDigits * mts / 2f;
        drawLine(
            start = Offset(mo + numWidth + bw, bw + hs),
            end = Offset(bs - bw, bw + hs),
            strokeWidth = profile.barWidth,
            color = color,
        )
    }

    if (flags.flagCell) {
        val color = flags.flagCellColor ?: profile.colors.flagColor
        drawLine(
            start = Offset(bs - bw - hs, bw + mo + mts),
            end = Offset(bs - bw - hs, bs - bw),
            strokeWidth = profile.barWidth,
            color = color,
        )
    }
}

/**
 * Is not null and not whitespace
 */
private fun isWhitespace(s : String?) : Boolean {
    return s?.let(WHITESPACE_RE::matches) ?: false
}

private fun DrawScope.drawSeparators(
    textMeasurer : TextMeasurer,
    profile : BoxProfile,
    box : BoxState,
) {
    // box size
    val bs = profile.boxSizePx
    // box center
    val bc = profile.boxSizePx / 2f
    // bar width
    val bw = profile.barWidth
    // half stroke
    val hs = bw / 2f
    // dash size
    val ds = profile.separatorDashSize
    // horizontal text offset
    val hto = profile.separatorHorizontalTextOffset

    val separators = box.separators
    val textStyle = profile.separatorTextStyle
    val barColor = getBarColor(profile, box)

    val top = separators.top
    if (isWhitespace(top))
        drawBar(profile, barColor, Bar.DOTTED, 0f, hs, bs, hs)
    else if ("-".equals(top))
        drawBar(profile, barColor, Bar.SOLID, bc, 0f, bc, ds)
    else if (top != null)
        drawOffsetText(textMeasurer, textStyle, top, bc, 0f, -0.5f, 0f)

    val bottom = separators.bottom
    if (isWhitespace(bottom))
        drawBar(profile, barColor, Bar.DOTTED, 0f, bs - hs, bs, bs - hs)
    else if ("-".equals(bottom))
        drawBar(profile, barColor, Bar.SOLID, bc, bs - ds, bc, bs)
    else if (bottom != null)
        drawOffsetText(textMeasurer, textStyle, bottom, bc, bs, -0.5f, -1f)

    val left = separators.left
    if (isWhitespace(left))
        drawBar(profile, barColor, Bar.DOTTED, hs, 0f, hs, bs)
    else if ("-".equals(left))
        drawBar(profile, barColor, Bar.SOLID, 0f, bc, ds, bc)
    else if (left != null)
        drawOffsetText(textMeasurer, textStyle, left, hto, bc, 0f, -0.5f)

    val right = separators.right
    if (isWhitespace(right))
        drawBar(profile, barColor, Bar.DOTTED, bs - hs, 0f, bs - hs, bs)
    else if ("-".equals(right))
        drawBar(profile, barColor, Bar.SOLID, bs - ds, bc, bs, bc)
    else if (right != null)
        drawOffsetText(textMeasurer, textStyle, right, bs - hto, bc, -1f, -0.5f)
}

private fun DrawScope.drawClueNumber(
    textMeasurer : TextMeasurer,
    profile : BoxProfile,
    box : BoxState,
    clueNumber : AnnotatedString?,
) {
    clueNumber?.let { number ->
        drawText(
            textMeasurer = textMeasurer,
            text = number,
            topLeft = Offset(profile.markOffset, profile.markOffset),
            style = getMarkTextStyle(profile, box),
        )
    }
}

/**
 * Draw text at x, y and offset by fraction of size
 */
private fun DrawScope.drawOffsetText(
    textMeasurer : TextMeasurer,
    style : TextStyle,
    text : String,
    x : Float,
    y : Float,
    adjustX : Float,
    adjustY : Float,
) {
    drawOffsetText(
        textMeasurer = textMeasurer,
        style = style,
        text = AnnotatedString(text),
        x = x,
        y = y,
        adjustX = adjustX,
        adjustY = adjustY,
    )
}

/**
 * Draw text at x, y and offset by fraction of size
 */
private fun DrawScope.drawOffsetText(
    textMeasurer : TextMeasurer,
    style : TextStyle,
    text : AnnotatedString,
    x : Float,
    y : Float,
    adjustX : Float,
    adjustY : Float,
) {
    val textResult = textMeasurer.measure(
        text = text,
        style = style,
    )
    val size = textResult.size
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            x + adjustX * size.width,
            y + adjustY * size.height,
        ),
    )
}

private fun DrawScope.drawMarks(
    textMeasurer : TextMeasurer,
    profile : BoxProfile,
    box : BoxState,
) {
    if (box.marks == null)
        return

    val style = getMarkTextStyle(profile, box)

    val mo = profile.markOffset
    val bs = profile.boxSizePx
    val coords = arrayOf(mo, 0.5f * bs, bs - mo)
    val adjusts = arrayOf(0f, -0.5f, -1f)

    for (row in 0 until max(3, box.marks.size)) {
        for (col in 0 until max(3, box.marks[row].size)) {
            box.marks[row][col]?.let { mark ->
                drawOffsetText(
                    textMeasurer,
                    style,
                    // could try to use remember here to optimise
                    htmlToAnnotatedString(mark),
                    coords[col],
                    coords[row],
                    adjusts[col],
                    adjusts[row],
                )
            }
        }
    }
}

private fun DrawScope.drawScratch(
    textMeasurer : TextMeasurer,
    profile : BoxProfile,
    box : BoxState,
) {
    if (!box.isBlank)
        return

    val across = box.scratchState.across
    val down = box.scratchState.down

    if (across == null && down == null)
        return
    else if (across == down)
        across?.let { drawSingleScratch(textMeasurer, profile, box, it) }
    else if (across == null)
        down?.let { drawSingleScratch(textMeasurer, profile, box, it) }
    else if (down == null)
        drawSingleScratch(textMeasurer, profile, box, across)
    else
        drawDoubleScratch(textMeasurer, profile, box, across, down)
}

private fun DrawScope.drawSingleScratch(
    textMeasurer : TextMeasurer,
    profile : BoxProfile,
    box : BoxState,
    letter : String,
) {
    val textResult = textMeasurer.measure(
        text = letter,
        style = getScratchTextStyle(profile, box),
    )
    val center = 0.5f * profile.boxSizePx
    val size = textResult.size
    drawText(
        textLayoutResult = textResult,
        topLeft = Offset(
            center - 0.5f * size.width,
            center - 0.5f * size.height + profile.scratchTopPaddingPx,
        ),
    )
}

private fun DrawScope.drawDoubleScratch(
    textMeasurer : TextMeasurer,
    profile : BoxProfile,
    box : BoxState,
    across : String,
    down : String,
) {
    // box size
    val bs = profile.boxSizePx
    // bar width
    val bw = profile.barWidth
    val offset = 1.5f * bw

    val style = getMiniScratchTextStyle(profile, box)

    val textResultAcross = textMeasurer.measure(
        text = across,
        style = style,
    )
    val sizeAcross = textResultAcross.size
    drawText(
        textLayoutResult = textResultAcross,
        topLeft = Offset(
            offset,
            bs - offset - sizeAcross.height + profile.scratchTopPaddingPx,
        ),
    )

    val textResultDown = textMeasurer.measure(
        text = down,
        style = style,
    )
    val sizeDown = textResultDown.size
    drawText(
        textLayoutResult = textResultDown,
        topLeft = Offset(
            bs - offset - sizeDown.width,
            offset + profile.scratchTopPaddingPx,
        ),
    )
}

/**
 * Invert color against base.
 *
 * For use when "inverting" a color to appear on the board. It is the
 * pureColor if the baseColor is white, it is inverted if the baseColor
 * is black, and all shades in-between.
 *
 */
private fun getRelativeColor(baseColor : Color, pureColor : Color) : Color {
    val pure = pureColor.convert(ColorSpaces.Srgb)
    val base = baseColor.convert(ColorSpaces.Srgb)

    val mixedR = mixColors(base.red, pure.red)
    val mixedG = mixColors(base.green, pure.green)
    val mixedB = mixColors(base.blue, pure.blue)

    return Color(
        red = mixedR,
        green = mixedG,
        blue = mixedB,
        alpha = pure.alpha,
        colorSpace = ColorSpaces.Srgb,
    )
}

/**
 * Tint a 0-1 pure color against a base
 */
private fun mixColors(
    base : Float,
    pure : Float,
) : Float{
    return (base * pure) + ((1f - base) * (1f - pure))
}

/**
 * Find out the background color of the box
 *
 * Depends on if it's a block, cell, highlighted, hinted, and so on.
 */
private fun getBoxColor(profile : BoxProfile, box : BoxState) : Color {
    if (box.highlighted)
        return profile.colors.currentLetterHighlightColor
    if (box.selected)
        return profile.colors.currentWordHighlightColor
    if (box.cheated)
        return profile.colors.cheatedColor
    return getColorMixedWithBox(profile, box, box.boxColor)
}

/**
 * Block or not
 */
private fun getBaseBoxColor(profile : BoxProfile, box : BoxState) : Color {
    return if (box.isBlock)
        profile.colors.blockColor
    else
        profile.colors.cellColor
}

private fun getBarColor(profile : BoxProfile, box : BoxState) : Color {
    val default = if (box.isBlock)
        profile.colors.onBlockColor
    else
        profile.colors.blockColor
    val base = getBaseBoxColor(profile, box)
    return box.barState?.color?.let { getRelativeColor(base, it) } ?: default
}

/**
 * Get color on box
 *
 * The box defines a base color (e.g. white if it's a cell, black if a
 * block). If color is null, just use base, else make it relative to
 * base color (i.e. invert on black).
 */
private fun getColorMixedWithBox(
    profile : BoxProfile,
    box : BoxState,
    color : Color?,
) : Color {
    val base = getBaseBoxColor(profile, box)
    return color?.let { getRelativeColor(base, it) } ?: base
}

private fun getResponseTextStyle(
    profile : BoxProfile,
    box : BoxState,
) : TextStyle {
    return getTextStyle(
        profile,
        box,
        profile.responseTextStyle,
        profile.colors.boardLetterColor,
        profile.colors.onBlockColor,
        box.selected,
    )
}

private fun getMarkTextStyle(
    profile : BoxProfile,
    box : BoxState,
) : TextStyle {
    return getTextStyle(
        profile,
        box,
        profile.markTextStyle,
        profile.colors.boardLetterColor,
        profile.colors.onBlockColor,
        box.selected,
    )
}

private fun getScratchTextStyle(
    profile : BoxProfile,
    box : BoxState,
) : TextStyle {
    return getTextStyle(
        profile,
        box,
        profile.scratchTextStyle,
        profile.colors.boardNoteColor,
        profile.colors.onBlockColor,
        box.selected,
    )
}

private fun getMiniScratchTextStyle(
    profile : BoxProfile,
    box : BoxState,
) : TextStyle {
    return getTextStyle(
        profile,
        box,
        profile.miniScratchTextStyle,
        profile.colors.boardNoteColor,
        profile.colors.onBlockColor,
        box.selected,
    )
}

/**
 * Pick the right text color for the box
 *
 * Uses the box text color if given, relative to box color, or uses cell
 * or block base.
 */
private fun getTextColorForBox(
    profile : BoxProfile,
    box : BoxState,
    cellBase : Color,
    blockBase : Color,
    inCurrentWord : Boolean,
) : Color {
    val base = if (box.isBlock) blockBase else cellBase
    return if (box.textColor == null || inCurrentWord)
        base
    else
        getRelativeColor(getBoxColor(profile, box), box.textColor)
}

/**
 * Get text style for box
 */
private fun getTextStyle(
    profile : BoxProfile,
    box : BoxState,
    baseTextStyle : TextStyle,
    cellBase : Color,
    blockBase : Color,
    inCurrentWord : Boolean,
) : TextStyle {
    return baseTextStyle.copy(
        color = getTextColorForBox(
            profile,
            box,
            cellBase,
            blockBase,
            inCurrentWord,
        ),
    )
}

fun DrawScope.drawShape(
    profile : BoxProfile,
    box : BoxState,
) {
    if (box.shape == null)
        return

    // box size
    val bs = profile.boxSizePx
    // offset (plus half stroke width)
    val off = 1.5f * profile.barWidth
    // bar width
    val bw = profile.barWidth

    val color = getTextColorForBox(
        profile,
        box,
        profile.colors.boardShapeColor,
        profile.colors.blockShapeColor,
        box.selected,
    )
    val strokeStyle = Stroke(
        width = bw,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )

    when (box.shape) {
        Shape.CIRCLE -> {
            drawCircle(
                color = color,
                radius = bs / 2 - off,
                center = Offset(bs / 2, bs / 2),
                style = strokeStyle,
            )
        }
        Shape.ARROW_LEFT -> {
            val path = Path().apply {
                moveTo(bs - off, bs / 2)
                lineTo(off, bs / 2)
                lineTo(bs / 4, bs / 4)
                moveTo(off, bs / 2)
                lineTo(bs / 4, 3 * bs / 4)
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.ARROW_RIGHT -> {
            val path = Path().apply {
                moveTo(off, bs / 2)
                lineTo(bs - off, bs / 2)
                lineTo(3 * bs / 4, bs / 4)
                moveTo(bs - off, bs / 2)
                lineTo(3 * bs / 4, 3 * bs / 4)
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.ARROW_UP -> {
            val path = Path().apply {
                moveTo(bs / 2, bs - off)
                lineTo(bs / 2, off)
                lineTo(bs / 4, bs / 4)
                moveTo(bs / 2, off)
                lineTo(3 * bs / 4, bs / 4)
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.ARROW_DOWN -> {
            val path = Path().apply {
                moveTo(bs / 2, off)
                lineTo(bs / 2, bs - off)
                lineTo(bs / 4, 3 * bs / 4)
                moveTo(bs / 2, bs - off)
                lineTo(3 * bs / 4, 3 * bs / 4)
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.TRIANGLE_LEFT -> {
            val path = Path().apply {
                moveTo(bs - off, bs / 2)
                lineTo(off, off)
                lineTo(off, bs - off)
                close()
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.TRIANGLE_RIGHT -> {
            val path = Path().apply {
                moveTo(off, bs / 2)
                lineTo(bs - off, off)
                lineTo(bs - off, bs - off)
                close()
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.TRIANGLE_UP -> {
            val path = Path().apply {
                moveTo(bs / 2, bs - off)
                lineTo(off, off)
                lineTo(bs - off, off)
                close()
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.TRIANGLE_DOWN -> {
            val path = Path().apply {
                moveTo(bs / 2, off)
                lineTo(off, bs - off)
                lineTo(bs - off, bs - off)
                close()
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.DIAMOND -> {
            val path = Path().apply {
                moveTo(bs / 2, off)
                lineTo(bs - off, bs / 2)
                lineTo(bs / 2, bs - off)
                lineTo(off, bs / 2)
                close()
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.CLUB -> {
            val path = Path().apply {
                moveTo(bs / 2, 3 * bs / 5)
                quadraticTo(
                    off, bs - off,
                    off, bs / 2,
                )
                quadraticTo(
                    off, bs / 5,
                    2 * bs / 5, 2 * bs / 5,
                )
                quadraticTo(
                    off, off,
                    bs / 2, off,
                )
                quadraticTo(
                    bs - off, off,
                    3 * bs / 5, 2 * bs / 5,
                )
                quadraticTo(
                    bs - off, bs / 5,
                    bs - off, bs / 2,
                )
                quadraticTo(
                    bs - off, bs - off,
                    bs / 2, 3 * bs / 5,
                )
                quadraticTo(
                    bs / 2, 4 * bs / 5,
                    3 * bs / 4, bs - off,
                )
                lineTo(bs / 4, bs - off)
                quadraticTo(
                    bs / 2, 4 * bs / 5,
                    bs / 2, 3 * bs / 5,
                )
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.HEART -> {
            val path = Path().apply {
                moveTo(bs / 2, bs / 4)
                cubicTo(
                    bs / 2, off,
                    off, off,
                    off, bs / 3,
                )
                cubicTo(
                    off, bs / 2,
                    bs / 2, 2 * bs / 3,
                    bs / 2, bs - off,
                )
                cubicTo(
                    bs / 2, 2 * bs / 3,
                    bs - off, bs / 2,
                    bs - off, bs / 3,
                )
                cubicTo(
                    bs - off, off,
                    bs / 2, off,
                    bs / 2, bs / 4,
                )
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.SPADE -> {
            val path = Path().apply {
                moveTo(bs / 2, 3 * bs / 5)
                cubicTo(
                    bs / 2, 4 * bs / 5,
                    off, 4 * bs / 5,
                    off, 3 * bs / 5,
                )
                cubicTo(
                    off, bs / 2,
                    bs / 2, bs / 3,
                    bs / 2, off,
                )
                cubicTo(
                    bs / 2, bs / 3,
                    bs - off, bs / 2,
                    bs - off, 3 * bs / 5,
                )
                cubicTo(
                    bs - off, 4 * bs / 5,
                    bs / 2, 4 * bs / 5,
                    bs / 2, 3 * bs / 5,
                )
                quadraticTo(
                    bs / 2, 4 * bs / 5,
                    2 * bs / 3, bs - off,
                )
                lineTo(bs / 3, bs - off)
                quadraticTo(
                    bs / 2, 4 * bs / 5,
                    bs / 2, 3 * bs / 5,
                )
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.STAR -> {
            val path = Path().apply {
                moveTo(off, 2 * bs / 5)
                lineTo(2 * bs / 5, 2 * bs / 5)
                lineTo(bs / 2, off)
                lineTo(3 * bs / 5, 2 * bs / 5)
                lineTo(bs - off, 2 * bs / 5)
                lineTo(7 * bs / 10, 3 * bs / 5)
                lineTo(4 * bs / 5, bs - off)
                lineTo(bs / 2, 7 * bs / 10)
                lineTo(bs / 5, bs - off)
                lineTo(3 * bs / 10, 3 * bs / 5)
                close()
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.SQUARE -> {
            drawRect(
                color = color,
                topLeft = Offset(off, off),
                size = Size(bs - 2 * off, bs - 2 * off),
                style = strokeStyle,
            )
        }
        Shape.RHOMBUS -> {
            val path = Path().apply {
                moveTo(2 * bs / 5, off)
                lineTo(bs - off, off)
                lineTo(3 * bs / 5, bs - off)
                lineTo(off, bs - off)
                close()
            }
            drawPath(
                path = path,
                style = strokeStyle,
                color = color,
            )
        }
        Shape.FORWARD_SLASH -> {
            drawLine(
                color = color,
                start = Offset(off, off),
                end = Offset(bs - off, bs - off),
                strokeWidth = bw,
                cap = StrokeCap.Round,
            )
        }
        Shape.BACK_SLASH -> {
            drawLine(
                color = color,
                start = Offset(off, bs - off),
                end = Offset(bs - off, off),
                strokeWidth = bw,
                cap = StrokeCap.Round,
            )
        }
        Shape.X -> {
            drawLine(
                color = color,
                start = Offset(off, off),
                end = Offset(bs - off, bs - off),
                strokeWidth = bw,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(off, bs - off),
                end = Offset(bs - off, off),
                strokeWidth = bw,
                cap = StrokeCap.Round,
            )
        }
    }
}
