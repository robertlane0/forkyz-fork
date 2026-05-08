
package app.crossword.yourealwaysbe.puz.io

import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Puzzle

class AmuseLabsJSONIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use { AmuseLabsJSONIO.readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1 = SourceUtils.getResource("/amuselabs.json")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test Amuse Labs")
            Assertions.assertEquals(puz.author, "Test Author")
            Assertions.assertEquals(puz.copyright, "Test Copyright")
            Assertions.assertEquals(puz.completionMessage, "End Message")
            Assertions.assertEquals(puz.date, LocalDate(2021, 8, 4))

            Assertions.assertEquals(puz.width, 15)
            Assertions.assertEquals(puz.height, 15)

            val boxes : Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][0]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[0][1]!!.clueNumber, "2")
            Assertions.assertTrue(isBlock(boxes[0][4]))
            Assertions.assertEquals(boxes[5][5]!!.clueNumber, "28")
            Assertions.assertTrue(isBlock(boxes[5][7]))

            Assertions.assertEquals(boxes[0][0]!!.solution, "A")
            Assertions.assertEquals(boxes[5][3]!!.solution, "B")

            Assertions.assertEquals(boxes[10][3]!!.shape, Box.Shape.CIRCLE)
            Assertions.assertEquals(boxes[7][6]!!.shape, Box.Shape.CIRCLE)
            Assertions.assertFalse(boxes[3][7]!!.hasShape())
            Assertions.assertFalse(boxes[5][9]!!.hasShape())

            val acrossClues = puz.getClues("ACROSS")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Clue 1a",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("21")!!.hint,
                "Clue 21a",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Clue 1d",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Clue 2d",
            )
        }
    }
}

