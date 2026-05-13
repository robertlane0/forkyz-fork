
package app.crossword.yourealwaysbe.forkyz.puz.io

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Clue
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.RCIJeuxMFJIO.Companion.readPuzzle
import app.crossword.yourealwaysbe.forkyz.util.SourceUtils

class RCIJeuxMFJIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use { readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/rcijeux-motsfleches.mfj")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test Puzzle RCI")

            Assertions.assertEquals(puz.width, 14)
            Assertions.assertEquals(puz.height, 9)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertTrue(isBlock(boxes[0][0]))
            Assertions.assertTrue(isBlock(boxes[6][10]))
            Assertions.assertEquals(boxes[0][1]!!.solution, "A")
            Assertions.assertEquals(boxes[2][4]!!.solution, "B")
            Assertions.assertEquals(boxes[4][12]!!.solution, "C")

            val acrossClues = puz.getClues("Horiz.")
            val downClues = puz.getClues("Vert.")

            val clues = arrayOf<Clue>(
                downClues!!.getClueByNumber(boxes[0][1]!!.clueNumber!!)!!,
                acrossClues!!.getClueByNumber(boxes[1][0]!!.clueNumber!!)!!,
                acrossClues.getClueByNumber(boxes[2][3]!!.clueNumber!!)!!,
                downClues.getClueByNumber(boxes[3][2]!!.clueNumber!!)!!,
                acrossClues.getClueByNumber(boxes[7][10]!!.clueNumber!!)!!,
            )

            for (i in clues.indices) {
                Assertions.assertEquals(clues[i].hint, "Test clue " + (i + 1))
            }

            val zone3 = clues[2].zone
            Assertions.assertEquals(zone3.size(), 6)
            Assertions.assertEquals(zone3.getPosition(0), Position(2, 3))
            Assertions.assertEquals(zone3.getPosition(5), Position(2, 8))

            val zone1 = clues[0].zone
            Assertions.assertEquals(zone1.size(), 9)
            Assertions.assertEquals(zone1.getPosition(0), Position(0, 1))
            Assertions.assertEquals(zone1.getPosition(8), Position(8, 1))
        }
    }
}

