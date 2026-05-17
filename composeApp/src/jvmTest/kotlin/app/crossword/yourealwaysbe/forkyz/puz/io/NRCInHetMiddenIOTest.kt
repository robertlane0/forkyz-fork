
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.util.SourceUtils

class NRCInHetMiddenIOTest {
    @Test
    fun testPuzzle1() {
        val parser = NRCInHetMiddenIO()
        val puz = SourceUtils.getResource("/inhetmidden-1.json").use {
            runBlocking { parser.parseInput(it) }
        }
        assertIsTestPuzzle1(puz!!)
    }

    @Test
    fun testPuzzle2() {
        val parser = NRCInHetMiddenIO()
        val puz = SourceUtils.getResource("/inhetmidden-2.json").use {
            runBlocking { parser.parseInput(it) }
        }
        assertIsTestPuzzle2(puz!!)
    }

    @Test
    fun testPuzzle3() {
        val parser = NRCInHetMiddenIO()
        val puz = SourceUtils.getResource("/inhetmidden-3.json").use {
            runBlocking { parser.parseInput(it) }
        }
        assertIsTestPuzzle3(puz!!)
    }

    companion object {
        fun assertIsTestPuzzle1(puz : Puzzle) {
            val ACROSS_CLUES = "Across"
            val DOWN_CLUES = "Down"

            Assertions.assertEquals(LocalDate(2025, 11, 18), puz.date)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(5, boxes.size)
            Assertions.assertEquals(5, boxes[0].size)
            Assertions.assertEquals("1", boxes[0][1]!!.clueNumber)
            Assertions.assertEquals("2", boxes[1][0]!!.clueNumber)
            Assertions.assertEquals("3", boxes[2][2]!!.clueNumber)
            Assertions.assertEquals("m", boxes[2][1]!!.clueNumber)
            Assertions.assertTrue(
                boxes[2][2]!!.isStartOf(ClueID(DOWN_CLUES, 1)),
            )

            Assertions.assertEquals(boxes[2][1]!!.solution, "M")
            Assertions.assertEquals(boxes[2][2]!!.solution, "I")
            Assertions.assertEquals(boxes[2][3]!!.solution, "D")

            Assertions.assertEquals(boxes[0][1]!!.solution, "O")
            Assertions.assertEquals(boxes[1][1]!!.solution, "O")
            Assertions.assertEquals(boxes[2][1]!!.solution, "M")
            Assertions.assertEquals(boxes[3][1]!!.solution, "E")

            Assertions.assertFalse(isBlock(boxes[2][1]))

            val acrossClues = puz.getClues(ACROSS_CLUES)
            val downClues = puz.getClues(DOWN_CLUES)

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("m")!!.hint,
                "MIDDENWOORD",
            )
        }

        fun assertIsTestPuzzle2(puz : Puzzle) {
            val ACROSS_CLUES = "Across"

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals("3", boxes[2][1]!!.clueNumber)

            Assertions.assertEquals(boxes[2][1]!!.solution, "M")
            Assertions.assertEquals(boxes[2][2]!!.solution, "I")
            Assertions.assertEquals(boxes[2][3]!!.solution, "D")

            Assertions.assertEquals(boxes[3][1]!!.solution, "E")
            Assertions.assertEquals(boxes[4][1]!!.solution, "X")

            val acrossClues = puz.getClues(ACROSS_CLUES)

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("3")!!.hint,
                "MIDDENWOORD",
            )
        }

        fun assertIsTestPuzzle3(puz : Puzzle) {
            val ACROSS_CLUES = "Across"

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals("m1", boxes[2][0]!!.clueNumber)
            Assertions.assertEquals("m2", boxes[2][3]!!.clueNumber)
            Assertions.assertEquals("3", boxes[2][1]!!.clueNumber)

            val acrossClues = puz.getClues(ACROSS_CLUES)
            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("m1")!!.hint,
                "MIDDENWOORD",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("m2")!!.hint,
                "MIDDENWOORD",
            )
        }
    }
}

