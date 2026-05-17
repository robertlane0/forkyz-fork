
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.coroutines.runBlocking

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.KingFeaturesPlaintextIO.Companion.parsePuzzle
import app.crossword.yourealwaysbe.forkyz.util.SourceUtils

class KingFeaturesPlaintextIOTest {
    @Test
    fun testRead() {
        val puz = testPuzzle1.use { runBlocking { parsePuzzle(it) } }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/premiere-20100704.txt")

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

