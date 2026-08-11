
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.util.SourceUtils

class USATodayJSONIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use {
            runBlocking { USATodayJSONIO.readPuzzle(it) }
        }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1 = SourceUtils.getResource("/usatoday.json")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test Crossword")
            Assertions.assertEquals(puz.author, "Test Author, ed. Test Editor")
            Assertions.assertEquals(puz.copyright, "Test Copyright")
            Assertions.assertEquals(puz.date, LocalDate(2026, 8, 11))

            Assertions.assertEquals(puz.width, 15)
            Assertions.assertEquals(puz.height, 15)

            val boxes : Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][0]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[0][1]!!.clueNumber, "2")
            Assertions.assertTrue(isBlock(boxes[0][5]))
            Assertions.assertEquals(boxes[5][6]!!.clueNumber, "26")
            Assertions.assertTrue(isBlock(boxes[5][5]))

            Assertions.assertEquals(boxes[0][2]!!.solution, "B")
            Assertions.assertEquals(boxes[2][4]!!.solution, "C")

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Test 1",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("9")!!.hint,
                "Test 9",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("2")!!.hint,
                "Test 2",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("4")!!.hint,
                "Test 4",
            )
        }
    }
}

