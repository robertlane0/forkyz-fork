
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.PuzzleBuilder.BasicClue
import app.crossword.yourealwaysbe.puz.util.HtmlUtil.parseHtmlColor
import app.crossword.yourealwaysbe.puz.util.JSONException
import app.crossword.yourealwaysbe.puz.util.JSONObject
import app.crossword.yourealwaysbe.puz.util.parseJSONObject


/**
 * Parser for puzzles distributed by 42puzzles.com.
 *
 * The format is in JSON, and contains a bit more than just the puzzle. The
 * puzzle itself is found under the "puzzle" key, which is akin to IPuz, but
 * slightly different, so we can't use it directly.
 */
open class FortyTwoPuzzlesIO : AbstractJSONIO() {
    private var width = 0
    private var height = 0

    private val DATE_FORMATS = listOf(
        LocalDate.Format {
            day(Padding.ZERO)
            char('-')
            monthNumber(Padding.ZERO)
            char('-')
            year(Padding.ZERO)
        },
        LocalDate.Formats.ISO,
    )

    @Throws(Exception::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return parsePuzzle(inStream)?.puzzle
    }

    @Throws(Exception::class)
    open protected fun parsePuzzle(inStream : InputStream) : PuzzleBuilder? {
        val copy = StreamUtils.makeByteArrayInputStream(inStream)
        val json : JSONObject
        try {
            json = parseJSONObject(copy)
        } catch (e : IOException) {
            return null
        } catch (e : JSONException) {
            return null
        }

        return json.getJSONObject("puzzle").let { srcPuzzle ->
            width = srcPuzzle.getInt("width")
            height = srcPuzzle.getInt("height")

            val boxes = parseBoxes(srcPuzzle)
            val builder = PuzzleBuilder(boxes, width, height)
            parseClues(
                builder,
                srcPuzzle.getJSONObject("hclues"),
                DefaultListNames.ACROSS_LIST,
            )
            parseClues(
                builder,
                srcPuzzle.getJSONObject("vclues"),
                DefaultListNames.DOWN_LIST,
            )

            setMetadata(srcPuzzle, builder)

            builder
        }
    }

    protected fun parseBoxes(srcPuzzle : JSONObject) : Array<Box?> {
        val srcValues = srcPuzzle.getJSONArray("values")
        val srcSolutions = srcPuzzle.getJSONArray("solution")
        val boxes = arrayOfNulls<Box>(width * height)

        for (row in 0..<height) {
            for (col in 0..<width) {
                val index = row * width + col
                // Might be int, might be string, so convert it explicitly to
                // string
                val value = srcValues.get(index).toString()
                val solution = srcSolutions.getString(index)
                boxes[index] = createBox(value, solution)
            }
        }

        return boxes
    }

    protected open fun createBox(value : String, solution : String) : Box {
        var value = value
        var solution = solution
        val box = Box()

        // 42puzzles seems to use the # sign as a mark that boxes should be
        // highlighted. This mark is prepended to whatever else is in the value
        // string, so it needs to be cut off for further processing.
        var isHighlighted = false
        if (value.contains("#")) {
            isHighlighted = true
            value = value.replace("#", "")
        }

        if (value == "@") {
            box.isBlock = true
        } else if (value == "·") {
            box.isBlock = true
        } else {
            box.clueNumber = value
        }

        if (isHighlighted) {
            if (box.isBlock) {
                box.color = parseHtmlColor("#fafa33")
            } else {
                box.color = parseHtmlColor("#fffaa0")
            }
        }

        if (solution != "@") {
            // 42games uses the Dutch "Ĳ "digram which is not supported in
            // general. It is common in crossword puzzles to use the "Y" in
            // place.
            solution = solution.replace("Ĳ", "Y")

            box.solution = solution
        }

        return box
    }

    protected fun parseClues(
        builder : PuzzleBuilder,
        srcClues : JSONObject,
        listName : String,
    ) {
        // The JSONObject returns its keys in random order. Since we know that
        // all keys are numbers, we can convert them to actual numbers and sort
        // them this way.
        val clueNums = srcClues.keys
            .stream()
            .map<Int> { it.toInt() }
            .sorted()
            .toList()
        for (clueNum in clueNums) {
            val clueStr = clueNum.toString()
            val clue = BasicClue(
                clueStr,
                srcClues.getString(clueStr),
            )
            if (listName == DefaultListNames.ACROSS_LIST) {
                builder.addAcrossClue(listName, clue)
            } else {
                builder.addDownClue(listName, clue)
            }
        }
    }

    protected fun setMetadata(srcPuzzle : JSONObject, builder : PuzzleBuilder) {
        srcPuzzle.optString("date")?.let { dateStr ->
            for (dateFormat in DATE_FORMATS) {
                try {
                    val date = dateFormat.parse(dateStr)
                    builder.setDate(date)
                    break
                } catch (e : IllegalArgumentException) {
                    // try next
                }
            }
        }
    }
}

