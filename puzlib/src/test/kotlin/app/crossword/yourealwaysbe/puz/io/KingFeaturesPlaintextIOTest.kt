
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.KingFeaturesPlaintextIO.Companion.parsePuzzle

class KingFeaturesPlaintextIOTest {
    @Test
    @Throws(IOException::class)
    fun testRead() {
        testPuzzle1InputStream.use { inStream ->
            val puz = parsePuzzle(inStream)
            Companion.assertIsTestPuzzle1(puz!!)
        }
    }

    companion object {
        @JvmStatic
        val testPuzzle1InputStream: InputStream
            get() = KingFeaturesPlaintextIOTest::class.java.getResourceAsStream(
                "/premiere-20100704.txt"
            )!!

        @JvmStatic
        fun assertIsTestPuzzle1(puz: Puzzle) {
            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(21, boxes.size)
            Assertions.assertEquals(21, boxes[0].size)
            Assertions.assertEquals("1", boxes[0][0]!!.clueNumber)
            Assertions.assertEquals(
                true,
                boxes[0][0]!!.isStartOf(ClueID("Across", 0)),
            )
            Assertions.assertEquals(
                true,
                boxes[0][0]!!.isStartOf(ClueID("Down", 0)),
            )
            Assertions.assertEquals(
                false,
                boxes[0][3]!!.isStartOf(ClueID("Across", 1)),
            )

            Assertions.assertEquals(boxes[0][0]!!.solution, "F")
            Assertions.assertEquals(boxes[5][14]!!.solution, "E")
            Assertions.assertEquals(boxes[14][14]!!.solution, "E")
            Assertions.assertEquals(boxes[14][5]!!.solution, "R")
            Assertions.assertTrue(isBlock(boxes[1][7]))

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Murals on plaster",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("8")!!.hint,
                "Glucose-level regulator",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("23")!!.hint,
                "Cocky retort to a bully",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("5")!!.hint,
                "One preserving fruit, e.g.",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("7")!!.hint,
                "In stitches",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("14")!!.hint,
                "Napoleonic marshal Michel",
            )
        }
    }
}

