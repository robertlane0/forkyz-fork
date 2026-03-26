
package app.crossword.yourealwaysbe.puz.io

import java.io.InputStream

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.XDIO.Companion.readPuzzle

class XDIOTest {
    @Test
    @Throws(Exception::class)
    fun testPuzzleImplicit() {
        testPuzzleImplicitInputStream.use { inStream ->
            val puz = readPuzzle(inStream)
            Companion.assertIsTestPuzzle(puz!!, true, false)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testPuzzleExplicit() {
        testPuzzleExplicitInputStream.use { inStream ->
            val puz = readPuzzle(inStream)
            Companion.assertIsTestPuzzle(puz!!, false, true)
        }
    }

    companion object {
        val testPuzzleImplicitInputStream : InputStream
            get() = XDIOTest::class.java
                .getResourceAsStream("/implicitsections.xd")!!

        val testPuzzleExplicitInputStream : InputStream
            get() = XDIOTest::class.java
                .getResourceAsStream("/explicitsections.xd")!!

        /**
         * Checks if puzzle is as expected
         *
         * @param specialCircled if special squares are circled or
         * highlighted
         */
        @Throws(Exception::class)
        fun assertIsTestPuzzle(
            puz : Puzzle,
            specialCircled : Boolean,
            simpleNotes : Boolean,
        ) {
            Assertions.assertEquals(puz.width, 15)
            Assertions.assertEquals(puz.height, 15)

            Assertions.assertEquals(puz.title, "Test <b>title</b>")
            Assertions.assertEquals(puz.author, "Test <i>author</i>")
            Assertions.assertEquals(
                puz.copyright,
                "This is <u>the</u> <strike>copyright</strike>",
            )

            if (simpleNotes) {
                Assertions.assertEquals(
                    puz.notes,
                    "Test notes<br/><br/>Test line more notes",
                )
            } else {
                Assertions.assertEquals(
                    puz.notes,
                    "<p>description</p><p>notes</p>"
                        + "<p>Free form<br/><br/>notes</p>",
                )
            }

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertTrue(isBlock(boxes[0][5]))
            Assertions.assertEquals(boxes[0][0]!!.clueNumber, "1")
            Assertions.assertTrue(isBlock(boxes[2][5]))
            Assertions.assertEquals(boxes[0][11]!!.clueNumber, "10")

            Assertions.assertEquals(boxes[0][0]!!.solution, "ONE")
            Assertions.assertEquals(boxes[0][3]!!.solution, "A")
            Assertions.assertEquals(boxes[4][5]!!.solution, "B")

            if (specialCircled) {
                Assertions.assertTrue(boxes[0][7]!!.hasShape())
                Assertions.assertFalse(boxes[0][7]!!.hasColor())
            } else {
                Assertions.assertTrue(boxes[0][7]!!.hasColor())
                Assertions.assertFalse(boxes[0][7]!!.hasShape())
            }
            Assertions.assertEquals(boxes[0][7]!!.solution, "X")
            Assertions.assertFalse(boxes[0][8]!!.hasColor())
            Assertions.assertFalse(boxes[0][8]!!.hasShape())

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")
            val anonClues = puz.getClues("Clues")
            val extraClues = puz.getClues("Extra")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Clue A1",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("10")!!.hint,
                "Clue A10",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Clue D1",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Clue D2",
            )

            Assertions.assertEquals(anonClues!!.size(), 2)
            Assertions.assertEquals(extraClues!!.size(), 1)
            Assertions.assertEquals(
                anonClues.getClueByIndex(0)!!.clueNumber,
                "YZ",
            )
            Assertions.assertEquals(
                anonClues.getClueByIndex(0)!!.hint,
                "Anon clue 1",
            )
            Assertions.assertEquals(
                extraClues.getClueByIndex(0)!!.clueNumber,
                "1",
            )
            Assertions.assertEquals(
                extraClues.getClueByIndex(0)!!.hint,
                "Extra clue",
            )

            var clue = acrossClues.getClueByNumber("1")
            var clueZone = clue!!.zone
            Assertions.assertEquals(clueZone.size(), 5)
            Assertions.assertEquals(clueZone.getPosition(0), Position(0, 0))
            Assertions.assertEquals(clueZone.getPosition(4), Position(0, 4))

            clue = downClues.getClueByNumber("1")
            clueZone = clue!!.zone
            Assertions.assertEquals(clueZone.size(), 5)
            Assertions.assertEquals(clueZone.getPosition(0), Position(0, 0))
            Assertions.assertEquals(clueZone.getPosition(4), Position(4, 0))
        }
    }
}

