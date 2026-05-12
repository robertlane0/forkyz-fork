
package app.crossword.yourealwaysbe.forkyz.puz.io

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.Position
import app.crossword.yourealwaysbe.forkyz.puz.PuzImage
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.PrzekrojIO.Companion.readPuzzle

class PrzekrojIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use { readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/przekroj.json")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.width, 19)
            Assertions.assertEquals(puz.height, 17)

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertTrue(isBlock(boxes[0][0]))
            Assertions.assertEquals(boxes[0][5]!!.clueNumber, "1")
            Assertions.assertTrue(isBlock(boxes[1][6]))
            Assertions.assertEquals(boxes[6][12]!!.clueNumber, "17")

            Assertions.assertEquals(boxes[0][5]!!.solution, "A")
            Assertions.assertEquals(boxes[5][7]!!.solution, "B")

            val acrossClues = puz.getClues("Poziomo")
            val downClues = puz.getClues("Pionowo")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Test clue 1",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("10")!!.hint,
                "Test clue 10 żó",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Test clue 1d",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Test clue 2d",
            )

            val clue = acrossClues.getClueByNumber("6")
            val clueZone = clue!!.zone
            Assertions.assertEquals(clueZone.getPosition(0), Position(2, 7))
            Assertions.assertEquals(clueZone.getPosition(4), Position(2, 11))

            Assertions.assertEquals(
                puz.images.get(0),
                PuzImage("myimage.jpg", 0, 2, 3, 3),
            )
        }
    }
}

