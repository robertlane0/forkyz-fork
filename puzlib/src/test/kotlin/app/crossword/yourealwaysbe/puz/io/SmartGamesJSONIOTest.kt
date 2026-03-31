package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.Zone

class SmartGamesJSONIOTest {
    @Test
    @Throws(IOException::class)
    fun testRead() {
        testPuzzle1InputStream.use { inStream ->
            val puz = SmartGamesJSONIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzle1(puz!!)
        }
    }

    companion object {
        val testPuzzle1InputStream: InputStream
            get() = SmartGamesJSONIO::class.java
                .getResourceAsStream("/smartgames.json")!!

        fun assertIsTestPuzzle1(puz: Puzzle) {
            Assertions.assertEquals(puz.title, null)
            Assertions.assertEquals(puz.author, "Test Author")
            Assertions.assertEquals(puz.notes, null)
            Assertions.assertEquals(puz.date, LocalDate(2025, 9, 10))

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(15, boxes.size)
            Assertions.assertEquals(15, boxes[0].size)
            Assertions.assertEquals("1", boxes[0][0]!!.clueNumber)
            Assertions.assertEquals(
                true,
                boxes[0][0]!!.isStartOf(ClueID("Across", 0)),
            )
            Assertions.assertEquals(
                true,
                boxes[0][1]!!.isStartOf(ClueID("Down", 0)),
            )
            Assertions.assertEquals(
                false,
                boxes[0][3]!!.isStartOf(ClueID("Across", 1)),
            )
            Assertions.assertEquals(
                false,
                boxes[0][8]!!.isStartOf(ClueID("Across", 2)),
            )

            Assertions.assertEquals(boxes[0][0]!!.solution, "F")
            Assertions.assertEquals(boxes[5][13]!!.solution, "E")
            Assertions.assertEquals(boxes[14][12]!!.solution, "E")
            Assertions.assertEquals(boxes[14][4]!!.solution, "R")
            Assertions.assertTrue(isBlock(boxes[1][7]))

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            val a1 = acrossClues!!.getClueByNumber("1")
            val a1zone = Zone()
            a1zone.addPosition(Position(0, 0))
            a1zone.addPosition(Position(0, 1))
            a1zone.addPosition(Position(0, 2))
            a1zone.addPosition(Position(0, 3))
            a1zone.addPosition(Position(0, 4))
            a1zone.addPosition(Position(0, 5))
            a1zone.addPosition(Position(0, 6))
            Assertions.assertEquals(a1!!.hint, "Test clue 1")
            Assertions.assertEquals(a1.zone, a1zone)

            val a19 = acrossClues.getClueByNumber("19")
            val a19zone = Zone()
            a19zone.addPosition(Position(2, 13))
            a19zone.addPosition(Position(2, 14))
            Assertions.assertEquals(a19!!.hint, "Test clue 19")
            Assertions.assertEquals(a19.zone, a19zone)

            val a27 = acrossClues.getClueByNumber("27")
            val a27zone = Zone()
            a27zone.addPosition(Position(4, 10))
            a27zone.addPosition(Position(4, 11))
            a27zone.addPosition(Position(4, 12))
            a27zone.addPosition(Position(4, 13))
            a27zone.addPosition(Position(4, 14))
            Assertions.assertEquals(a27!!.hint, "Test clue 27")
            Assertions.assertEquals(a27.zone, a27zone)

            val d28 = downClues!!.getClueByNumber("28")
            val d28zone = Zone()
            d28zone.addPosition(Position(4, 13))
            d28zone.addPosition(Position(5, 13))
            d28zone.addPosition(Position(6, 13))
            Assertions.assertEquals(d28!!.hint, "Test clue 28")
            Assertions.assertEquals(d28.zone, d28zone)
        }
    }
}

