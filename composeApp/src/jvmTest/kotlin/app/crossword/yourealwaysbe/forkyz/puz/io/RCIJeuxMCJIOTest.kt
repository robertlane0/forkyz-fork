
package app.crossword.yourealwaysbe.forkyz.puz.io

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.RCIJeuxMCJIO.Companion.readPuzzle

class RCIJeuxMCJIOTest {
    @Test
    fun testPuzzle1() {
        val puz = readPuzzle(testPuzzle1)
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/rcijeux-crossword.mcj")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test Crossword")

            Assertions.assertEquals(puz.width, 12)
            Assertions.assertEquals(puz.height, 12)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertTrue(isBlock(boxes[0][4]))
            Assertions.assertTrue(isBlock(boxes[3][0]))
            Assertions.assertEquals(boxes[0][2]!!.solution, "B")
            Assertions.assertEquals(boxes[1][2]!!.solution, "C")
            Assertions.assertEquals(boxes[4][7]!!.solution, "A")

            val acrossClues = puz.getClues("Horiz.")
            val downClues = puz.getClues("Vert.")

            val a1 = acrossClues!!.getClueByNumber("1")
            Assertions.assertEquals(a1!!.hint, "Test 1")
            val zone1 = a1.zone
            Assertions.assertEquals(zone1.size(), 4)
            Assertions.assertEquals(zone1.getPosition(0), Position(0, 0))
            Assertions.assertEquals(zone1.getPosition(3), Position(0, 3))

            val a19 = acrossClues.getClueByNumber("19")
            Assertions.assertEquals(a19!!.hint, "Test B")
            val zone19 = a19.zone
            Assertions.assertEquals(zone19.size(), 2)
            Assertions.assertEquals(zone19.getPosition(0), Position(4, 4))
            Assertions.assertEquals(zone19.getPosition(1), Position(4, 5))

            val d1 = downClues!!.getClueByNumber("1")
            Assertions.assertEquals(d1!!.hint, "Test D")
            val zone1d = d1.zone
            Assertions.assertEquals(zone1d.size(), 3)
            Assertions.assertEquals(zone1d.getPosition(0), Position(0, 0))
            Assertions.assertEquals(zone1d.getPosition(1), Position(1, 0))

            val d41 = downClues.getClueByNumber("41")
            Assertions.assertEquals(d41!!.hint, "Test Z")
            val zone41 = d41.zone
            Assertions.assertEquals(zone41.size(), 2)
            Assertions.assertEquals(zone41.getPosition(0), Position(10, 3))
            Assertions.assertEquals(zone41.getPosition(1), Position(11, 3))
        }
    }
}

