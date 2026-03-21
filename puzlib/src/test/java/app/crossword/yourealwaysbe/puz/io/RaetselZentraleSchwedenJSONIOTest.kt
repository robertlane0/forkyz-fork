
package app.crossword.yourealwaysbe.puz.io

import java.io.InputStream

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle

class RaetselZentraleSchwedenJSONIOTest {
    @Test
    @Throws(Exception::class)
    fun testPuzzle1() {
        testPuzzle1InputStream.use { inStream ->
            val puz = RaetselZentraleSchwedenJSONIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzle1(puz!!)
        }
    }

    companion object {
        @JvmStatic
        val testPuzzle1InputStream : InputStream
            get() = JPZIOTest::class.java
                .getResourceAsStream("/raetselzentrale.json")!!

        @JvmStatic
        @Throws(Exception::class)
        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test Puzzle")

            Assertions.assertEquals(puz.width, 16)
            Assertions.assertEquals(puz.height, 16)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertTrue(isBlock(boxes[0][0]))
            Assertions.assertEquals(boxes[0][4]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[0][7]!!.clueNumber, "2")
            Assertions.assertEquals(boxes[4][13]!!.clueNumber, "19")

            Assertions.assertEquals(boxes[0][4]!!.solution, "A")
            Assertions.assertEquals(boxes[4][6]!!.solution, "I")

            Assertions.assertEquals(boxes[1][4]!!.marks!![2][2], "2")
            Assertions.assertEquals(boxes[5][5]!!.marks!![2][2], "9")

            val acrossClues = puz.getClues("Hinüber")
            val downClues = puz.getClues("Hinunter")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("6")!!.hint,
                "Test clue one",
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

            val sixAcross = acrossClues.getClueByNumber("6")
            val sixAcrossZone = sixAcross!!.zone
            Assertions.assertEquals(
                sixAcrossZone.getPosition(0),
                Position(1, 0),
            )
            Assertions.assertEquals(
                sixAcrossZone.getPosition(4),
                Position(1, 4),
            )
            Assertions.assertEquals(sixAcrossZone.size(), 5)

            val anAcross = acrossClues.getClueByNumber("14")
            val anAcrossZone = anAcross!!.zone
            Assertions.assertEquals(anAcrossZone.size(), 7)


            val winwordClues = puz.getClues("Lösungswort")
            Assertions.assertEquals(winwordClues!!.size(), 1)
            val winword = winwordClues.getClueByIndex(0)
            Assertions.assertEquals(winword!!.hint, "Lösungswort")
            val winwordZone = winword.zone
            Assertions.assertEquals(winwordZone.size(), 9)
            Assertions.assertEquals(
                winwordZone.getPosition(4),
                Position(13, 5),
            )

            Assertions.assertTrue(puz.hasPinnedClueID)
            Assertions.assertEquals(puz.pinnedClueID, winword.clueID)
        }
    }
}

