
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle

class SlowDownWiseUpJSONIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use { SlowDownWiseUpJSONIO.readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    @Test
    fun testPuzzle2() {
        val puz = testPuzzle2.use { SlowDownWiseUpJSONIO.readPuzzle(it) }
        assertIsTestPuzzle2(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/slowdownwiseup.json")
        val testPuzzle2
            get() = SourceUtils.getResource("/wsj.json")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test Crossword")
            Assertions.assertEquals(puz.author, "Test Author")
            Assertions.assertEquals(puz.date, LocalDate(2026, 3, 22))

            Assertions.assertEquals(puz.width, 15)
            Assertions.assertEquals(puz.height, 15)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][0]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[0][2]!!.clueNumber, "2")
            Assertions.assertTrue(isBlock(boxes[0][4]))
            Assertions.assertEquals(boxes[0][5]!!.clueNumber, "3")

            Assertions.assertEquals(boxes[0][0]!!.solution, "A")
            Assertions.assertEquals(boxes[0][6]!!.solution, "B")

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Test Clue 1 (4)",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("10")!!.hint,
                "Test Clue 10 (10)",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Test Clue 1d (10)",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Test Clue 2d (3,2,5)",
            )

            val twoDown = downClues.getClueByNumber("2")
            val twoDownZone = twoDown!!.zone
            Assertions.assertEquals(
                twoDownZone.getPosition(6),
                Position(6, 2),
            )
            Assertions.assertEquals(
                twoDownZone.getPosition(7),
                Position(7, 2),
            )
            Assertions.assertEquals(twoDown.getSeparator(0), null)
            Assertions.assertEquals(twoDown.getSeparator(2), null)
            Assertions.assertEquals(twoDown.getSeparator(3), " ")
            Assertions.assertEquals(twoDown.getSeparator(4), null)
            Assertions.assertEquals(twoDown.getSeparator(5), " ")
        }

        fun assertIsTestPuzzle2(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test title")
            Assertions.assertEquals(puz.author, "Test setter")
            Assertions.assertEquals(puz.completionMessage, "Correct solution")
            Assertions.assertEquals(puz.notes, "Test description")
            Assertions.assertEquals(puz.source, "Test publisher")
            Assertions.assertEquals(puz.date, LocalDate(2024, 1, 3))

            Assertions.assertEquals(puz.width, 15)
            Assertions.assertEquals(puz.height, 15)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][0]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[0][5]!!.clueNumber, "5")
            Assertions.assertTrue(isBlock(boxes[0][4]))
            Assertions.assertEquals(boxes[5][4]!!.clueNumber, "27")
            Assertions.assertTrue(isBlock(boxes[5][7]))

            Assertions.assertEquals(boxes[0][0]!!.solution, "A")
            Assertions.assertEquals(boxes[5][4]!!.solution, "B")

            Assertions.assertEquals(boxes[7][0]!!.shape, Box.Shape.CIRCLE)
            Assertions.assertEquals(boxes[7][1]!!.shape, Box.Shape.CIRCLE)
            Assertions.assertFalse(boxes[7][3]!!.hasShape())

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Clue 1a (4)",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("21")!!.hint,
                "Clue 21a (9)",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Clue 1d (5)",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Clue 2d (5)",
            )

            val zone5a = acrossClues.getClueByNumber("5")!!.zone
            Assertions.assertEquals(zone5a.size(), 3)
            Assertions.assertEquals(zone5a.getPosition(0), Position(0, 5))
            Assertions.assertEquals(zone5a.getPosition(2), Position(0, 7))

            val zone14d = downClues.getClueByNumber("18")!!.zone
            Assertions.assertEquals(zone14d.size(), 4)
            Assertions.assertEquals(zone14d.getPosition(0), Position(2, 8))
            Assertions.assertEquals(zone14d.getPosition(3), Position(5, 8))
        }
    }
}

