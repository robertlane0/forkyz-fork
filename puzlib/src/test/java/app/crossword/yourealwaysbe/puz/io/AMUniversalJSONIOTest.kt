
package app.crossword.yourealwaysbe.puz.io

import java.io.InputStream
import java.time.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.AMUniversalJSONIO.Companion.readPuzzle

class AMUniversalJSONIOTest {
    @Test
    @Throws(Exception::class)
    fun testPuzzle1() {
        val puz = readPuzzle(testPuzzle1InputStream)
        Companion.assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1InputStream : InputStream
            get() = AMUniversalJSONIOTest::class.java
                .getResourceAsStream("/amuniversal.json")!!

        fun assertIsTestPuzzle1(puz : Puzzle) {
            val ACROSS_CLUES = "Across"
            val DOWN_CLUES = "Down"

            Assertions.assertEquals("Test title", puz.title)
            Assertions.assertEquals("Test author", puz.author)
            Assertions.assertEquals("Test copyright", puz.copyright)
            Assertions.assertEquals(LocalDate.of(2024, 1, 2), puz.date)

            val boxes : Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(15, boxes.size)
            Assertions.assertEquals(15, boxes[0].size)
            Assertions.assertEquals("1", boxes[0][0]!!.clueNumber)
            Assertions.assertTrue(
                boxes[0][0]!!.isStartOf(ClueID(ACROSS_CLUES, 0)),
            )
            Assertions.assertTrue(
                boxes[0][0]!!.isStartOf(ClueID(DOWN_CLUES, 0)),
            )
            Assertions.assertTrue(
                boxes[0][5]!!.isStartOf(ClueID(DOWN_CLUES, 4)),
            )
            Assertions.assertFalse(
                boxes[0][2]!!.isStartOf(ClueID(DOWN_CLUES, 3)),
            )

            Assertions.assertEquals(boxes[0][0]!!.solution, "T")
            Assertions.assertEquals(boxes[2][7]!!.solution, "Y")
            Assertions.assertEquals(boxes[9][9]!!.solution, "S")
            Assertions.assertEquals(boxes[3][6]!!.solution, "N")
            Assertions.assertTrue(isBlock(boxes[0][4]))
            Assertions.assertTrue(isBlock(boxes[5][1]))

            val acrossClues = puz.getClues(ACROSS_CLUES)
            val downClues = puz.getClues(DOWN_CLUES)

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Test clue 1",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("14")!!.hint,
                "Test clue 14",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("23")!!.hint,
                "Test clue 23",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("29")!!.hint,
                "Test clue 29",
            )
        }
    }
}

