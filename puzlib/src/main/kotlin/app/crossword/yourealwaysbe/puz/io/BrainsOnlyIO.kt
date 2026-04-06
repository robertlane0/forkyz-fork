
package app.crossword.yourealwaysbe.puz.io

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format.Padding

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.util.HtmlUtil.htmlString

/**
 * Created with IntelliJ IDEA.
 * User: keber_000
 * Date: 2/9/14
 * Time: 5:57 PM
 * To change this template use File | Settings | File Templates.
 */
class BrainsOnlyIO : PuzzleParser {
    @Throws(IOException::class)
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return parse(inStream)
    }

    companion object {
        private val CHARSET : Charset = Charset.forName("ISO-8859-1")
        private val ACROSS_LIST : String = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST : String = DefaultListNames.DOWN_LIST
        private const val NOTEPAD_START_TAG = "<NOTEPAD>"
        private const val NOTEPAD_END_TAG = "</NOTEPAD>"
        private val DATE_FORMATTER = LocalDate.Format {
            yearTwoDigits(2000)
            monthNumber(Padding.ZERO)
            day(Padding.ZERO)
        }

        fun convertBrainsOnly(
            inStream : InputStream,
            os : DataOutputStream,
            date : LocalDate,
        ) : Boolean {
            try {
                val puz : Puzzle? = parse(inStream)
                if (puz != null) {
                    puz.date = date
                    IO.saveNative(puz, os)
                }
            } catch (e : IOException) {
                System.err.println("Unable to dump puzzle to output stream.")
                e.printStackTrace()
                return false
            }

            return true
        }

        @JvmStatic
        @Throws(IOException::class)
        fun parse(inStream : InputStream) : Puzzle? {
            val reader = BufferedReader(InputStreamReader(inStream, CHARSET))

            val date : String = readLineAtOffset(reader, 2)
            val title : String = readLineAtOffset(reader, 1)
            val puzTitle = htmlString(title)
            var author : String = readLineAtOffset(reader, 1)

            // Not sure if note should be part of author line or a separate line
            // Assume part of author for now as De Telegraaf replaces author line
            // with it
            var notes : String = ""
            val authorUpper = author.uppercase(Locale.getDefault())
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

            val width = readLineAtOffset(reader, 1).toInt()
            val height = readLineAtOffset(reader, 1).toInt()
            if (width == 0 || height == 0) {
                throw IOException("Invalid puzzle contents")
            }
            readLineAtOffset(reader, 4)
            val boxes = Array<Array<Box?>>(height) { arrayOfNulls<Box>(width) }
            for (down in 0..<height) {
                val line : String = readLineAtOffset(reader, 0)

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
                    String.format(
                        "Unexpected line length for width %d grid: %s",
                        width,
                        line
                    )
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

            readLineAtOffset(reader, 0)
            val acrossClues = ArrayList<String?>()
            run {
                var clue : String = Companion.readLineAtOffset(reader, 0)
                while ("" != clue) {
                    acrossClues.add(clue)
                    clue = Companion.readLineAtOffset(reader, 0)
                }
            }

            val downClues = ArrayList<String>()
            var clue : String = readLineAtOffset(reader, 0)
            while ("" != clue) {
                downClues.add(clue)
                clue = readLineAtOffset(reader, 0)
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
                            ACROSS_LIST,
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
                            DOWN_LIST,
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
            reader : BufferedReader,
            offset : Int,
        ) : String {
            var read : String? = null
            var i = 0
            while (i <= offset) {
                read = reader.readLine()
                if (read == null) {
                    throw EOFException("Offset past end of file")
                }
                if (read.endsWith("\r")) {
                    i++
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

