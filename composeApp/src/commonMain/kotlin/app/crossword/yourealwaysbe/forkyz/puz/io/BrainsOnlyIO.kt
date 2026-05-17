
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding
import kotlinx.io.EOFException
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readLine

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.forkyz.util.HtmlUtil.htmlString

/**
 * Created with IntelliJ IDEA.
 * User: keber_000
 * Date: 2/9/14
 * Time: 5:57 PM
 * To change this template use File | Settings | File Templates.
 */
class BrainsOnlyIO : PuzzleParser {
    @Throws(IOException::class)
    override suspend fun parseInput(input : Source) : Puzzle? {
        return parse(input)
    }

    companion object {
        private const val NOTEPAD_START_TAG = "<NOTEPAD>"
        private const val NOTEPAD_END_TAG = "</NOTEPAD>"
        private val DATE_FORMATTER = LocalDate.Format {
            yearTwoDigits(2000)
            monthNumber(Padding.ZERO)
            day(Padding.ZERO)
        }

        @Throws(IOException::class)
        suspend fun parse(input : Source) : Puzzle? {
            val date : String = readLineAtOffset(input, 2)
            val title : String = readLineAtOffset(input, 1)
            val puzTitle = htmlString(title)
            var author : String = readLineAtOffset(input, 1)

            // Not sure if note should be part of author line or a separate line
            // Assume part of author for now as De Telegraaf replaces author line
            // with it
            var notes : String = ""
            val authorUpper = author.uppercase()
            val notesStart : Int = authorUpper.indexOf(NOTEPAD_START_TAG)
            if (notesStart > -1) {
                val notesEnd : Int = authorUpper.indexOf(NOTEPAD_END_TAG)

                notes = htmlString(
                    author.substring(
                        notesStart + NOTEPAD_START_TAG.length,
                        notesEnd
                    )
                )!!

                author = htmlString(author.substring(0, notesStart))!!
            } else {
                author = htmlString(author)!!
            }

            val width = readLineAtOffset(input, 1).toInt()
            val height = readLineAtOffset(input, 1).toInt()
            if (width == 0 || height == 0) {
                throw IOException("Invalid puzzle contents")
            }
            readLineAtOffset(input, 4)
            val boxes = Array<Array<Box?>>(height) { arrayOfNulls<Box>(width) }
            for (down in 0..<height) {
                val line : String = readLineAtOffset(input, 0)

                var nextCircled = false
                var nextShaded = false
                var addToPrevious = false
                var across = 0
                for (i in 0..<line.length) {
                    val c = line.get(i)
                    if (c == '%') {
                        nextCircled = true
                        continue
                    } else if (c == '^') {
                        nextShaded = true
                        continue
                    } else if (c == ',') {
                        addToPrevious = true
                        continue
                    }

                    if (addToPrevious) {
                        boxes[down][across - 1]?.let { b ->
                            b.solution = b.solution + c
                        }
                    } else {
                        if (c != '#' && c != '.') {
                            val b = Box()
                            b.setSolution(c)
                            if (nextCircled)
                                b.shape = Box.Shape.CIRCLE
                            if (nextShaded)
                                b.color = IPuzIO.HIGHLIGHT_COLOR
                            boxes[down][across] = b
                        }

                        across += 1
                    }

                    nextCircled = false
                    nextShaded = false
                    addToPrevious = false
                }

                if (across != width) throw IOException(
                    "Unexpected line length for width $width grid: $line",
                )
            }

            var parsedDate : LocalDate? = null
            try {
                parsedDate = DATE_FORMATTER.parse(date)
            } catch (e : IllegalArgumentException) {
                // fall through with no date
            }

            val builder = PuzzleBuilder(boxes)
            builder.autoNumberBoxes()
                .setTitle(puzTitle)
                .setAuthor(author)
                .setNotes(notes)
                .setDate(parsedDate)

            readLineAtOffset(input, 0)
            val acrossClues = ArrayList<String?>()
            run {
                var clue : String = Companion.readLineAtOffset(input, 0)
                while ("" != clue) {
                    acrossClues.add(clue)
                    clue = Companion.readLineAtOffset(input, 0)
                }
            }

            val downClues = ArrayList<String>()
            var clue : String = readLineAtOffset(input, 0)
            while ("" != clue) {
                downClues.add(clue)
                clue = readLineAtOffset(input, 0)
            }

            var acrossIdx = 0
            for (h in 0..<height) {
                for (w in 0..<width) {
                    val box = builder.getBox(h, w)
                    if (
                        box != null
                        && !isBlock(box)
                        && box.hasClueNumber()
                        && builder.isStartClue(h, w, true)
                    ) {
                        builder.addAcrossClue(
                            DefaultListNames.acrossList(),
                            box.clueNumber,
                            htmlString(acrossClues.get(acrossIdx))!!,
                        )
                        acrossIdx += 1
                    }
                }
            }

            var downIdx = 0
            for (h in boxes.indices) {
                for (w in 0..<width) {
                    val box = builder.getBox(h, w)
                    if (
                        box != null
                        && !isBlock(box)
                        && box.hasClueNumber()
                        && builder.isStartClue(h, w, false)
                    ) {
                        builder.addDownClue(
                            DefaultListNames.downList(),
                            box.clueNumber,
                            htmlString(downClues.get(downIdx))!!,
                        )
                        downIdx += 1
                    }
                }
            }

            return builder.puzzle
        }

        @Throws(IOException::class)
        private fun readLineAtOffset(
            input : Source,
            offset : Int,
        ) : String {
            var read : String? = null
            var i = 0
            while (i <= offset) {
                read = input.readLine()
                if (read == null) {
                    throw IOException("Offset past end of file")
                }
                i++
            }
            if (read == null) {
                throw EOFException("End of line")
            }
            return read.trim()
        }
    }
}

