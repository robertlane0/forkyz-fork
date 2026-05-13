
package app.crossword.yourealwaysbe.forkyz.puz.io

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.SpoonbillIO.Companion.readPuzzle
import app.crossword.yourealwaysbe.forkyz.util.SourceUtils

class SpoonbillIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use { readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/spoonbill.txt")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test title")
            Assertions.assertEquals(puz.author, "Test author")

            Assertions.assertEquals(puz.width, 15)
            Assertions.assertEquals(puz.height, 15)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][0]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[2][8]!!.clueNumber, "11")
            Assertions.assertTrue(isBlock(boxes[0][5]))
            Assertions.assertTrue(isBlock(boxes[5][2]))

            Assertions.assertEquals(boxes[0][0]!!.solution, "A")
            Assertions.assertEquals(boxes[1][7]!!.solution, "B")

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Test clue 1a",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("19")!!.hint,
                "Test clue 19a",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("2")!!.hint,
                "Test clue 2d",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("14")!!.hint,
                "Test clue 14d",
            )

            val zone5a = acrossClues.getClueByNumber("5")!!.zone
            Assertions.assertEquals(zone5a.size(), 7)
            Assertions.assertEquals(zone5a.getPosition(0), Position(0, 8))
            Assertions.assertEquals(zone5a.getPosition(6), Position(0, 14))

            val zone14d = downClues.getClueByNumber("14")!!.zone
            Assertions.assertEquals(zone14d.size(), 11)
            Assertions.assertEquals(zone14d.getPosition(0), Position(4, 8))
            Assertions.assertEquals(zone14d.getPosition(10), Position(14, 8))
        }
    }
}

