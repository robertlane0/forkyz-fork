package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.Zone
import app.crossword.yourealwaysbe.forkyz.util.SourceUtils

class TCAJSONIOTest {
    @Test
    fun testRead() {
        val puz = testPuzzle1.use { TCAJSONIO.readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/tca.json")

        fun assertIsTestPuzzle1(puz: Puzzle) {
            Assertions.assertEquals(puz.title, "Test Crossword")
            Assertions.assertEquals(puz.author, "Test Author")
            Assertions.assertEquals(puz.notes, "Test Notes")
            Assertions.assertEquals(puz.date, LocalDate(2025, 7, 27))

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
            Assertions.assertEquals(boxes[5][13]!!.solution, "E")
            Assertions.assertEquals(boxes[14][12]!!.solution, "E")
            Assertions.assertEquals(boxes[14][4]!!.solution, "R")
            Assertions.assertTrue(isBlock(boxes[1][5]))

            Assertions.assertNull(boxes[0][2]!!.shape)
            Assertions.assertEquals(Box.Shape.CIRCLE, boxes[0][3]!!.shape)

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            val a1 = acrossClues!!.getClueByNumber("1")
            val a1zone = Zone()
            a1zone.addPosition(Position(0, 0))
            a1zone.addPosition(Position(0, 1))
            a1zone.addPosition(Position(0, 2))
            a1zone.addPosition(Position(0, 3))
            a1zone.addPosition(Position(0, 4))
            Assertions.assertEquals(a1!!.hint, "Test clue 1")
            Assertions.assertEquals(a1.zone, a1zone)

            val a10 = acrossClues.getClueByNumber("10")
            val a10zone = Zone()
            a10zone.addPosition(Position(0, 12))
            a10zone.addPosition(Position(0, 13))
            a10zone.addPosition(Position(0, 14))
            Assertions.assertEquals(a10!!.hint, "Test clue 10")
            Assertions.assertEquals(a10.zone, a10zone)

            val a27 = acrossClues.getClueByNumber("27")
            val a27zone = Zone()
            a27zone.addPosition(Position(3, 10))
            a27zone.addPosition(Position(3, 11))
            a27zone.addPosition(Position(3, 12))
            Assertions.assertEquals(a27!!.hint, "Test clue 27")
            Assertions.assertEquals(a27.zone, a27zone)

            val d4 = downClues!!.getClueByNumber("4")
            val d4zone = Zone()
            d4zone.addPosition(Position(0, 3))
            d4zone.addPosition(Position(1, 3))
            d4zone.addPosition(Position(2, 3))
            d4zone.addPosition(Position(3, 3))
            Assertions.assertEquals(d4!!.hint, "Test clue 4d")
            Assertions.assertEquals(d4.zone, d4zone)
        }
    }
}

