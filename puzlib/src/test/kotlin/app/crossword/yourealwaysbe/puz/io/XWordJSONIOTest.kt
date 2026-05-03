package app.crossword.yourealwaysbe.puz.io

import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.XWordJSONIO.Companion.readPuzzle

class XWordJSONIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use { readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/xword.json")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test title")
            Assertions.assertEquals(puz.author, "Test author")
            Assertions.assertEquals(puz.notes, "Test notes")
            Assertions.assertEquals(puz.source, "Test publisher")
            Assertions.assertEquals(puz.date, LocalDate(2024, 1, 5))

            Assertions.assertEquals(puz.width, 15)
            Assertions.assertEquals(puz.height, 15)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][0]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[1][0]!!.clueNumber, "13")
            Assertions.assertTrue(isBlock(boxes[0][6]))

            Assertions.assertEquals(boxes[0][0]!!.solution, "A")
            Assertions.assertEquals(boxes[1][6]!!.solution, "B")

            Assertions.assertEquals(boxes[0][0]!!.shape, Box.Shape.CIRCLE)
            Assertions.assertFalse(boxes[0][1]!!.hasShape())
            Assertions.assertEquals(boxes[0][0]!!.barRight, Box.Bar.NONE)
            Assertions.assertEquals(boxes[0][0]!!.barBottom, Box.Bar.NONE)
            Assertions.assertEquals(boxes[0][1]!!.barRight, Box.Bar.SOLID)
            Assertions.assertEquals(boxes[0][1]!!.barBottom, Box.Bar.NONE)

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Clue 1a",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("18")!!.hint,
                "Clue 18a",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Clue 1d",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("17")!!.hint,
                "Clue 17d",
            )

            val clue1a = acrossClues.getClueByNumber("1")
            val zone1a = clue1a!!.zone
            Assertions.assertEquals(zone1a.size(), 2)
            Assertions.assertEquals(zone1a.getPosition(0), Position(0, 0))
            Assertions.assertEquals(zone1a.getPosition(1), Position(0, 1))

            val clue1d = downClues.getClueByNumber("1")
            val zone1d = clue1d!!.zone
            Assertions.assertEquals(zone1d.size(), 6)
            Assertions.assertEquals(zone1d.getPosition(0), Position(0, 0))
            Assertions.assertEquals(zone1d.getPosition(5), Position(5, 0))
        }
    }
}

