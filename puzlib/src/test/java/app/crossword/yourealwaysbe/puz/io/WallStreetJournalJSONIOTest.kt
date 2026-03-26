package app.crossword.yourealwaysbe.puz.io

import java.io.InputStream
import java.time.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle

class WallStreetJournalJSONIOTest {
    @Test
    @Throws(Exception::class)
    fun testPuzzle1() {
        testPuzzle1InputStream.use { inStream ->
            val puz = WallStreetJournalJSONIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzle1(puz!!)
        }
    }

    companion object {
        val testPuzzle1InputStream : InputStream
            get() = WallStreetJournalJSONIOTest::class.java
                .getResourceAsStream("/wsj.json")

        @Throws(Exception::class)
        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test title")
            Assertions.assertEquals(puz.author, "Test setter")
            Assertions.assertEquals(puz.completionMessage, "Correct solution")
            Assertions.assertEquals(puz.notes, "Test description")
            Assertions.assertEquals(puz.source, "Test publisher")
            Assertions.assertEquals(puz.date, LocalDate.of(2024, 1, 3))

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
                "Clue 1a",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("21")!!.hint,
                "Clue 21a",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Clue 1d",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Clue 2d",
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

