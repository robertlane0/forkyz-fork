
package app.crossword.yourealwaysbe.forkyz.puz.io

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.PAPuzzlesIO.Companion.readPuzzle

class PAPuzzlesIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use { readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/papuzzles.html")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.width, 13)
            Assertions.assertEquals(puz.height, 13)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertTrue(isBlock(boxes[0][0]))
            Assertions.assertEquals(boxes[0][5]!!.clueNumber, "3")
            Assertions.assertTrue(isBlock(boxes[1][5]))
            Assertions.assertEquals(boxes[9][10]!!.clueNumber, "33")

            Assertions.assertEquals(boxes[5][0]!!.solution, "Y")
            Assertions.assertEquals(boxes[7][5]!!.solution, "Z")

            val acrossClues = puz.getClues("ACROSS")
            val downClues = puz.getClues("DOWN")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("8")!!.hint,
                "AClue 8",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("10")!!.hint,
                "AClue 10",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "DClue 1",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "DClue 2",
            )

            val clue = acrossClues.getClueByNumber("8")
            val clueZone = clue!!.zone
            Assertions.assertEquals(clueZone.getPosition(0), Position(1, 0))
            Assertions.assertEquals(clueZone.getPosition(4), Position(1, 4))
        }
    }
}

