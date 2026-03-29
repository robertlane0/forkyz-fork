
package app.crossword.yourealwaysbe.puz.io

import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.time.LocalDate
import java.util.Scanner

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleBuilder
import app.crossword.yourealwaysbe.puz.io.charset.MacRoman

/**
 * Converts a puzzle from the plaintext format used by King Features Syndicate
 * puzzles to the Across Lite .puz format.  The format is:
 *
 * -Grid shape and clue numbers (redundant)
 * -Solution grid
 * -Across Clues
 * -Down Clues
 *
 * Each section begins with a { character, and each line except the last in a section
 * ends with a | character.  The charset used is Mac Roman.
 *
 * For an example puzzle in this format, see:
 * src/test/resources/premiere-20100704.txt.
 */
class KingFeaturesPlaintextIO : PuzzleParser {
    override fun parseInput(inStream : InputStream) : Puzzle? {
        return parsePuzzle(inStream)
    }

    companion object {
        private val ACROSS_LIST : String = DefaultListNames.ACROSS_LIST
        private val DOWN_LIST : String = DefaultListNames.DOWN_LIST

        /**
         * Take an InputStream containing a plaintext puzzle parsed Puzzle if
         * succeeded, or null if it fails
         * (for example, if the plaintext file is not in a valid format).
         */
        @JvmStatic
        fun parsePuzzle(inStream : InputStream) : Puzzle? {
            val scanner = Scanner(InputStreamReader(inStream, MacRoman()))

            if (!scanner.hasNextLine()) {
                System.err.println("File empty.")
                return null
            }

            var line = scanner.nextLine()
            if (!line.startsWith("{") || !scanner.hasNextLine()) {
                System.err.println("First line format incorrect.")
                return null
            }

            // Skip over redundant grid information.
            line = scanner.nextLine()
            while (!line.startsWith("{")) {
                if (!scanner.hasNextLine()) {
                    System.err.println("Unexpected EOF - Grid information.")
                    return null
                }
                line = scanner.nextLine()
            }

            // Process solution grid.
            val solGrid : MutableList<CharArray> = mutableListOf()
            line = line.substring(1, line.length - 2)
            var rowString : Array<String> = line.split(" ".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val width = rowString.size
            do {
                if (line.endsWith(" |")) {
                    line = line.substring(0, line.length - 2)
                }
                rowString = line.split(" ".toRegex())
                    .dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (rowString.size != width) {
                    System.err.println("Not a square grid.")
                    return null
                }

                val row = CharArray(width)
                for (x in 0..<width) {
                    row[x] = rowString[x].get(0)
                }
                solGrid.add(row)

                if (!scanner.hasNextLine()) {
                    System.err.println("Unexpected EOF - Solution grid.")
                    return null
                }
                line = scanner.nextLine()
            } while (!line.startsWith("{"))

            // Convert solution grid into Box grid.
            val height = solGrid.size
            val boxes = Array<Array<Box?>>(height) {
                arrayOfNulls<Box>(width)
            }
            for (x in 0..<height) {
                val row = solGrid.get(x)
                for (y in 0..<width) {
                    if (row[y] != '#') {
                        val box = Box()
                        box.setSolution(row[y])
                        box.setBlank()
                        boxes[x][y] = box
                    }
                }
            }

            val builder = PuzzleBuilder(boxes)
                .autoNumberBoxes()

            // Process clues.
            line = line.substring(1)
            var clueNum : Int
            do {
                if (line.endsWith(" |")) {
                    line = line.substring(0, line.length - 2)
                }
                clueNum = 0
                var i = 0
                while (line.get(i) != '.') {
                    if (clueNum != 0) {
                        clueNum *= 10
                    }
                    clueNum += line.get(i).code - '0'.code
                    i++
                }
                val clue = line.substring(i + 2).trim()
                builder.addAcrossClue(ACROSS_LIST, clueNum.toString(), clue)
                if (!scanner.hasNextLine()) {
                    System.err.println("Unexpected EOF - Across clues.")
                    return null
                }
                line = scanner.nextLine()
            } while (!line.startsWith("{"))

            var maxClueNum = clueNum

            line = line.substring(1)
            var finished = false
            do {
                if (line.endsWith(" |")) {
                    line = line.substring(0, line.length - 2)
                } else {
                    finished = true
                }
                clueNum = 0
                var i = 0
                while (line.get(i) != '.') {
                    if (clueNum != 0) {
                        clueNum *= 10
                    }
                    clueNum += line.get(i).code - '0'.code
                    i++
                }
                val clue = line.substring(i + 2).trim()
                builder.addDownClue(DOWN_LIST, clueNum.toString(), clue)
                if (!finished) {
                    if (!scanner.hasNextLine()) {
                        System.err.println("Unexpected EOF - Down clues.")
                        return null
                    }
                    line = scanner.nextLine()
                }
            } while (!finished)

            maxClueNum = if (clueNum > maxClueNum) clueNum else maxClueNum

            // Makeshift title
            builder.setTitle("King Features Puzzle")
            builder.setNotes("")

            return builder.puzzle
        }

        /**
         * Take an InputStream containing a plaintext puzzle to a
         * DataOutputStream containing the generated .puz file.  Returns
         * true if the process succeeded, or false if it fails (for
         * example, if the plaintext file is not in a valid format).
         */
        fun convertKFPuzzle(
            inStream : InputStream,
            os : DataOutputStream?,
            title : String?,
            author : String?,
            copyright : String?,
            date : LocalDate?,
        ) : Boolean {
            val puz : Puzzle? = parsePuzzle(inStream)
            if (puz == null)
                return false

            puz.title = title
            puz.author = author
            puz.date = date
            puz.copyright = copyright

            try {
                IO.saveNative(puz, os)
            } catch (e : IOException) {
                System.err.println("Unable to dump puzzle to output stream.")
                return false
            }

            return true
        }
    }
}

