
package app.crossword.yourealwaysbe.puz.io

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.JPZIO.Companion.readPuzzle

class JPZIOTest {
    @Test
    @Throws(Exception::class)
    fun testJPZ() {
        val baos = ByteArrayOutputStream()
        StreamUtils.copyStream(testPuzzle1InputStream, baos)
        val puz = readPuzzle(
            ByteArrayInputStream(baos.toByteArray()),
        )
        Companion.assertIsTestPuzzle1(puz!!)
    }

    @Test
    @Throws(Exception::class)
    fun testJPZAcrostic() {
        val baos = ByteArrayOutputStream()
        StreamUtils.copyStream(testPuzzleAcrosticInputStream, baos)
        val puz = readPuzzle(
            ByteArrayInputStream(baos.toByteArray()),
        )
        Companion.assertIsTestPuzzleAcrostic(puz!!)
    }

    companion object {
        @JvmStatic
        val testPuzzle1InputStream : InputStream
            get() = JPZIOTest::class.java
                .getResourceAsStream("/lat_puzzle_111128.xml")!!

        @JvmStatic
        val testPuzzleAcrosticInputStream : InputStream
            get() = JPZIOTest::class.java
                .getResourceAsStream("/acrostic.jpz")!!

        @JvmStatic
        fun assertIsTestPuzzle1(puz : Puzzle) {
            val ACROSS_CLUES = "Across"
            val DOWN_CLUES = "Down"

            Assertions.assertEquals("LA Times, Mon, Nov 28, 2011", puz.title)
            Assertions.assertEquals("Jeff Chen / Ed. Rich Norris", puz.author)
            Assertions.assertEquals(
                "© 2011 Tribune Media Services, Inc.",
                puz.copyright,
            )
            Assertions.assertEquals("Instructions", puz.introMessage)
            Assertions.assertEquals("Test", puz.notes)
            Assertions.assertEquals(
                "Congratulations<br/>"
                    + "<h1>Down</h1>"
                    + "<p>22: Shower Heads v7</p>"
                    + "<p>61: I'm NOT going to ATTEND it "
                    + "/ I'm going to SKIP it</p>",
                puz.completionMessage,
            )

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(15, boxes.size)
            Assertions.assertEquals(15, boxes[0].size)
            Assertions.assertEquals("1", boxes[0][0]!!.clueNumber)
            Assertions.assertEquals(
                true,
                boxes[0][0]!!.isStartOf(ClueID(ACROSS_CLUES, 0)),
            )
            Assertions.assertEquals(
                true,
                boxes[0][0]!!.isStartOf(ClueID(DOWN_CLUES, 0)),
            )
            Assertions.assertEquals(
                false,
                boxes[0][3]!!.isStartOf(ClueID(ACROSS_CLUES, 1)),
            )

            Assertions.assertEquals(boxes[0][0]!!.solution, "C")
            Assertions.assertEquals(boxes[5][14]!!.solution, "Y")
            Assertions.assertEquals(boxes[14][14]!!.solution, "S")
            Assertions.assertEquals(boxes[3][6]!!.solution, "N")
            Assertions.assertTrue(isBlock(boxes[14][5]))
            Assertions.assertTrue(isBlock(boxes[3][0]))

            Assertions.assertTrue(boxes[2][2]!!.isBarredTop())
            Assertions.assertFalse(boxes[3][2]!!.isBarredTop())
            Assertions.assertTrue(boxes[5][2]!!.isBarredRight())
            Assertions.assertFalse(boxes[5][2]!!.isBarredLeft())
            Assertions.assertTrue(boxes[6][2]!!.isBarredBottom())
            Assertions.assertFalse(boxes[6][2]!!.isBarredLeft())
            Assertions.assertTrue(boxes[7][2]!!.isBarredLeft())
            Assertions.assertFalse(boxes[7][2]!!.isBarredRight())

            Assertions.assertEquals(boxes[2][0]!!.shape, Box.Shape.ARROW_LEFT)
            Assertions.assertFalse(boxes[2][0]!!.isBlock)
            Assertions.assertEquals(boxes[3][0]!!.shape, Box.Shape.ARROW_RIGHT)
            Assertions.assertTrue(boxes[3][0]!!.isBlock)
            Assertions.assertEquals(boxes[4][0]!!.shape, Box.Shape.ARROW_UP)
            Assertions.assertTrue(boxes[4][0]!!.isBlock)
            Assertions.assertEquals(boxes[5][0]!!.shape, Box.Shape.ARROW_DOWN)
            Assertions.assertFalse(boxes[5][0]!!.isBlock)
            Assertions.assertFalse(boxes[6][0]!!.hasShape())

            val acrossClues = puz.getClues(ACROSS_CLUES)
            val downClues = puz.getClues(DOWN_CLUES)

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Baby bovine (4)",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("5")!!.hint,
                "At the drop of __ (4)",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("13/18")!!.hint,
                "Ice cream-and-cookies brand (4)",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("23")!!.hint,
                "Stat start",
            )
            Assertions.assertFalse(
                acrossClues.getClueByNumber("18")!!.hasZone(),
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("6")!!.hint,
                "Schmooze, as with the A-list (6)",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("7")!!.hint,
                "Work like __ (4)",
            )

            val marks: Array<Array<String?>> = boxes[1][0]!!.marks!!
            Assertions.assertEquals(marks[0][0], "TL")
            Assertions.assertEquals(marks[0][1], "T")
            Assertions.assertEquals(marks[0][2], "TR")
            Assertions.assertEquals(marks[1][0], "L")
            Assertions.assertEquals(marks[1][1], "C")
            Assertions.assertEquals(marks[1][2], "R")
            Assertions.assertEquals(marks[2][0], "BL")
            Assertions.assertEquals(marks[2][1], "B")
            Assertions.assertEquals(marks[2][2], "BR")
        }

        fun assertIsTestPuzzleAcrostic(puz : Puzzle) {
            val CLUES = "Clues"
            val QUOTES = "Quote"

            val boxes: Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(12, boxes.size)
            Assertions.assertEquals(33, boxes[0].size)
            Assertions.assertEquals("1", boxes[0][0]!!.clueNumber)
            Assertions.assertEquals(
                true,
                boxes[0][0]!!.isPartOf(ClueID(CLUES, 17)),
            )
            Assertions.assertEquals(
                true,
                boxes[0][0]!!.isStartOf(ClueID(QUOTES, 0)),
            )
            Assertions.assertEquals(
                false,
                boxes[0][3]!!.isStartOf(ClueID(CLUES, 21)),
            )

            Assertions.assertEquals(boxes[0][0]!!.solution, "N")
            Assertions.assertTrue(isBlock(boxes[0][4]))
            Assertions.assertEquals(boxes[1][10]!!.initialValue, ",")
            Assertions.assertEquals(boxes[1][10]!!.response, ",")

            val clues = puz.getClues(CLUES)
            val quotes = puz.getClues(QUOTES)

            val clueB = clues!!.getClueByIndex(1)

            Assertions.assertEquals(clues.size(), 22)
            Assertions.assertEquals(
                clueB!!.hint,
                "Super hyped to watch a muppet vampire "
                    + "while sick in bed (4, 3, 3, 5)",
            )
            Assertions.assertFalse(clueB.hasClueNumber())
            Assertions.assertEquals(clueB.label, "B")
            Assertions.assertEquals(quotes!!.size(), 1)
            Assertions.assertEquals(
                quotes.getClueByIndex(0)!!.hint,
                "Quote",
            )

            val marks: Array<Array<String?>> = boxes[1][0]!!.marks!!
            Assertions.assertNull(marks[0][0])
            Assertions.assertEquals(marks[0][2], "R")
        }
    }
}

