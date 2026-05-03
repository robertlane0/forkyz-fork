
package app.crossword.yourealwaysbe.puz.io

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.KeesingXMLIO.Companion.readPuzzle

class KeesingXMLIOTest {
    @Test
    fun testKeesingXML() {
        val puz = testPuzzle1.use { readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    @Test
    fun testKeesingXMLArrow() {
        val puz = testPuzzle2.use { readPuzzle(it) }
        assertIsTestPuzzle2(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/keesing.xml")
        val testPuzzle2
            get() = SourceUtils.getResource("/keesingarrow.xml")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            val ACROSS_CLUES = "Across"
            val DOWN_CLUES = "Down"

            Assertions.assertEquals("Test Crossword", puz.title)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(10, boxes.size)
            Assertions.assertEquals(11, boxes[0].size)
            Assertions.assertEquals("1", boxes[0][0]!!.clueNumber)
            Assertions.assertTrue(
                boxes[0][0]!!.isStartOf(ClueID(ACROSS_CLUES, 0)),
            )
            Assertions.assertTrue(
                boxes[0][0]!!.isStartOf(ClueID(DOWN_CLUES, 0)),
            )
            Assertions.assertTrue(
                boxes[0][6]!!.isStartOf(ClueID(DOWN_CLUES, 3)),
            )
            Assertions.assertFalse(
                boxes[0][2]!!.isStartOf(ClueID(DOWN_CLUES, 3)),
            )

            Assertions.assertEquals(boxes[0][0]!!.solution, "T")
            Assertions.assertEquals(boxes[2][7]!!.solution, "Y")
            Assertions.assertEquals(boxes[9][9]!!.solution, "S")
            Assertions.assertEquals(boxes[3][6]!!.solution, "N")
            Assertions.assertTrue(isBlock(boxes[0][5]))
            Assertions.assertTrue(isBlock(boxes[4][2]))

            val acrossClues = puz.getClues(ACROSS_CLUES)
            val downClues = puz.getClues(DOWN_CLUES)

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Test clue 1",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("13")!!.hint,
                "Test clue 13",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("26")!!.hint,
                "Test clue 26",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("14")!!.hint,
                "Test clue 14",
            )
        }

        fun assertIsTestPuzzle2(puz: Puzzle) {
            val ACROSS_CLUES = "Across"
            val DOWN_CLUES = "Down"

            Assertions.assertEquals("Test Arrowword", puz.title)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(12, boxes.size)
            Assertions.assertEquals(12, boxes[0].size)
            Assertions.assertEquals("1", boxes[0][1]!!.clueNumber)

            Assertions.assertEquals(boxes[2][1]!!.solution, "A")
            Assertions.assertEquals(boxes[3][3]!!.solution, "Z")
            Assertions.assertTrue(isBlock(boxes[2][0]))
            Assertions.assertTrue(isBlock(boxes[3][6]))

            val acrossClues = puz.getClues(ACROSS_CLUES)
            val downClues = puz.getClues(DOWN_CLUES)

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("7")!!.hint,
                "Clue 2",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("13")!!.hint,
                "Clue 4",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Clue 1",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("4")!!.hint,
                "Clue 3",
            )
        }
    }
}

