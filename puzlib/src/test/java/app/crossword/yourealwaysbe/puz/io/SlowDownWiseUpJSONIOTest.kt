
package app.crossword.yourealwaysbe.puz.io

import java.time.LocalDate
import java.io.InputStream

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle

class SlowDownWiseUpJSONIOTest {
    @Test
    @Throws(Exception::class)
    fun testPuzzle1() {
        testPuzzle1InputStream.use { inStream ->
            val puz = SlowDownWiseUpJSONIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzle1(puz!!)
        }
    }

    companion object {
        val testPuzzle1InputStream: InputStream
            get() = SlowDownWiseUpJSONIOTest::class.java
                .getResourceAsStream("/slowdownwiseup.json")!!

        @Throws(Exception::class)
        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test Crossword")
            Assertions.assertEquals(puz.author, "Test Author")
            Assertions.assertEquals(puz.date, LocalDate.of(2026, 3, 22))

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
    }
}

