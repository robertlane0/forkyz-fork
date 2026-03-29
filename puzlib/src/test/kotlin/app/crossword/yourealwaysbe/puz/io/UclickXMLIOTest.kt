
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.UclickXMLIO.Companion.parsePuzzle

class UclickXMLIOTest {
    @Test
    @Throws(IOException::class)
    fun testRead() {
        testPuzzle1InputStream.use { inStream ->
            val puz = parsePuzzle(inStream)
            Companion.assertIsTestPuzzle1(puz!!)
        }
    }

    companion object {
        private const val TITLE = "12/15/09 LET'S BE HONEST"
        private const val AUTHOR = "by Billie Truitt, edited by Stanley Newman"

        @JvmStatic
        val testPuzzle1InputStream : InputStream
            get() = UclickXMLIOTest::class.java.getResourceAsStream(
                "/crnet091215-data.xml"
            )!!

        @JvmStatic
        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(TITLE, puz.title)
            Assertions.assertEquals(AUTHOR, puz.author)

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
                boxes[0][0]!!.isStartOf(ClueID("Down", 0)),
            )
            Assertions.assertEquals(
                false,
                boxes[0][3]!!.isStartOf(ClueID("Across", 1)),
            )

            Assertions.assertEquals(boxes[0][0]!!.solution, "G")
            Assertions.assertTrue(isBlock(boxes[5][14]))
            Assertions.assertEquals(boxes[14][14]!!.solution, "S")
            Assertions.assertEquals(boxes[14][5]!!.solution, "L")
            Assertions.assertEquals(boxes[3][6]!!.solution, "N")


            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Film legend Greta",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("50")!!.hint,
                "Distress signal",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("5")!!.hint,
                "Rampaging",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("6")!!.hint,
                "Get even for",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("7")!!.hint,
                "Nickname for an NCO",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("13")!!.hint,
                "Covered with rocks",
            )
        }
    }
}

