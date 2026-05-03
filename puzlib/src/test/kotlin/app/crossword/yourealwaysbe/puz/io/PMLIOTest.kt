
package app.crossword.yourealwaysbe.puz.io

import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle

class PMLIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use { PMLIO.readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/pml.json")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test Crossword")
            Assertions.assertEquals(puz.date, LocalDate(2024, 12, 16))

            Assertions.assertEquals(puz.width, 13)
            Assertions.assertEquals(puz.height, 13)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][1]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[0][2]!!.clueNumber, "2")
            Assertions.assertTrue(isBlock(boxes[0][0]))
            Assertions.assertTrue(isBlock(boxes[3][3]))
            Assertions.assertEquals(boxes[6][4]!!.clueNumber, "14")

            Assertions.assertEquals(boxes[0][1]!!.solution, "A")
            Assertions.assertEquals(boxes[0][6]!!.solution, "F")
            Assertions.assertEquals(boxes[4][8]!!.solution, "G")

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Test clue 1 (6)",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("10")!!.hint,
                "Test clue 10 (7)",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("2")!!.hint,
                "Test clue 2 (7)",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("3")!!.hint,
                "Test clue 3 (5)",
            )

            val fiveAcross = acrossClues.getClueByNumber("5")
            val fiveAcrossZone = fiveAcross!!.zone
            Assertions.assertEquals(
                fiveAcrossZone.getPosition(0),
                Position(0, 8),
            )
            Assertions.assertEquals(
                fiveAcrossZone.getPosition(3),
                Position(0, 11),
            )
            Assertions.assertEquals(fiveAcrossZone.size(), 4)

            val sixteenDown = downClues.getClueByNumber("16")
            val sixteenDownZone = sixteenDown!!.zone
            Assertions.assertEquals(
                sixteenDownZone.getPosition(0),
                Position(6, 10),
            )
            Assertions.assertEquals(
                sixteenDownZone.getPosition(1),
                Position(7, 10),
            )
            Assertions.assertEquals(sixteenDownZone.size(), 7)
        }
    }
}

