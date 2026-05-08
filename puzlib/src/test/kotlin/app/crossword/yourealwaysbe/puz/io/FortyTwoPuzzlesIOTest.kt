
package app.crossword.yourealwaysbe.puz.io

import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Puzzle

class FortyTwoPuzzlesIOTest {
    @Test
    fun testPuzzle1() {
        val parser = FortyTwoPuzzlesIO()
        val puz = testPuzzle1.use { parser.parseInput(it) }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/fortytwopuzzles-1.json")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            val ACROSS_CLUES = "Across"
            val DOWN_CLUES = "Down"

            Assertions.assertEquals(LocalDate(2025, 11, 18), puz.date)

            val boxes : Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(5, boxes.size)
            Assertions.assertEquals(5, boxes[0].size)
            Assertions.assertEquals("1", boxes[0][1]!!.clueNumber)
            Assertions.assertEquals("4", boxes[1][0]!!.clueNumber)
            Assertions.assertTrue(
                boxes[0][1]!!.isStartOf(ClueID(ACROSS_CLUES, 0)),
            )
            Assertions.assertTrue(
                boxes[0][1]!!.isStartOf(ClueID(DOWN_CLUES, 0)),
            )
            Assertions.assertTrue(
                boxes[1][0]!!.isStartOf(ClueID(ACROSS_CLUES, 1)),
            )
            Assertions.assertTrue(
                boxes[1][0]!!.isStartOf(ClueID(DOWN_CLUES, 3)),
            )

            Assertions.assertEquals(boxes[0][1]!!.solution, "O")
            Assertions.assertEquals(boxes[0][2]!!.solution, "N")
            Assertions.assertEquals(boxes[0][3]!!.solution, "E")

            Assertions.assertEquals(boxes[3][0]!!.solution, "S")
            Assertions.assertEquals(boxes[3][1]!!.solution, "E")
            Assertions.assertEquals(boxes[3][2]!!.solution, "V")
            Assertions.assertEquals(boxes[3][3]!!.solution, "E")
            Assertions.assertEquals(boxes[3][4]!!.solution, "N")

            Assertions.assertTrue(isBlock(boxes[0][0]))
            Assertions.assertTrue(isBlock(boxes[4][3]))

            val acrossClues = puz.getClues(ACROSS_CLUES)
            val downClues = puz.getClues(DOWN_CLUES)

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "The first number"
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("5")!!.hint,
                "The fifth number plus an 'S'"
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Seemingly random vowels"
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Seemingly random letters"
            )
        }
    }
}

