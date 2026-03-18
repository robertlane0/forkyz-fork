
package app.crossword.yourealwaysbe.puz.io

import java.time.LocalDate
import java.io.InputStream

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle

class GuardianJSONIOTest {
    @Test
    @Throws(Exception::class)
    fun testPuzzle1() {
        testPuzzle1InputStream.use { inStream ->
            val puz = GuardianJSONIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzle1(puz!!)
        }
    }

    companion object {
        val testPuzzle1InputStream: InputStream
            get() = JPZIOTest::class.java
                .getResourceAsStream("/guardian.json")!!

        @Throws(Exception::class)
        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test Crossword")
            Assertions.assertEquals(puz.author, "Test Author")
            Assertions.assertEquals(puz.date, LocalDate.of(2021, 8, 12))

            Assertions.assertEquals(puz.width, 15)
            Assertions.assertEquals(puz.height, 15)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][0]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[0][2]!!.clueNumber, "2")
            Assertions.assertTrue(isBlock(boxes[0][7]))
            Assertions.assertEquals(boxes[0][8]!!.clueNumber, "5")

            Assertions.assertEquals(boxes[0][0]!!.solution, "A")
            Assertions.assertEquals(boxes[0][6]!!.solution, "B")

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Test clue 1",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("10")!!.hint,
                "Test clue 10",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Test clue 1d",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Test clue 2d",
            )

            val fiveAcross = acrossClues.getClueByNumber("5, 10")
            val fiveAcrossZone = fiveAcross!!.zone
            Assertions.assertEquals(
                fiveAcrossZone.getPosition(6),
                Position(0, 14),
            )
            Assertions.assertEquals(
                fiveAcrossZone.getPosition(7),
                Position(2, 6),
            )
            Assertions.assertEquals(fiveAcross.getSeparator(0), null)
            Assertions.assertEquals(fiveAcross.getSeparator(2), null)
            Assertions.assertEquals(fiveAcross.getSeparator(6), null)
            Assertions.assertEquals(fiveAcross.getSeparator(7), " ")
            Assertions.assertEquals(fiveAcross.getSeparator(8), null)
            Assertions.assertEquals(fiveAcross.getSeparator(9), null)
            Assertions.assertEquals(fiveAcross.getSeparator(10), " ")
            Assertions.assertEquals(fiveAcross.getSeparator(11), null)

            val tenAcross = acrossClues.getClueByNumber("10")
            val tenAcrossZone = tenAcross!!.zone
            Assertions.assertTrue(tenAcrossZone.isEmpty)

            val oneAcross = acrossClues.getClueByNumber("1")
            Assertions.assertEquals(oneAcross!!.getSeparator(0), "-")
            Assertions.assertEquals(oneAcross.getSeparator(1), null)
            Assertions.assertEquals(oneAcross.getSeparator(4), " ")
            Assertions.assertEquals(oneAcross.getSeparator(5), " ")
            Assertions.assertEquals(oneAcross.getSeparator(6), null)
        }
    }
}

