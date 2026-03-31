
package app.crossword.yourealwaysbe.puz.io

import java.io.IOException
import java.io.InputStream
import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.BrainsOnlyIO.Companion.parse

class BrainsOnlyIOTest {
    @Test
    @Throws(Exception::class)
    fun testParse() {
        val puz = parse(testPuzzle1InputStream)
        assertIsTestPuzzle1(puz!!)
    }

    @Test
    @Throws(Exception::class)
    fun testParse2() {
        val puz = parse(
            BrainsOnlyIOTest::class.java
                .getResourceAsStream("/brainsonly2.txt")!!
        )
        Assertions.assertEquals(
            puz!!.title,
            "02/09/14 OCCUPIED NATIONS: Surrounding the long answers",
        )
        val acrossClues = puz.getClues("Across")
        Assertions.assertEquals(
            acrossClues!!.getClueByNumber("15")!!.hint,
            "Elevator guy",
        )
        Assertions.assertEquals(
            acrossClues.getClueByNumber("5")!!.hint,
            "Company with a duck mascot",
        )
    }

    @Test
    @Throws(Exception::class)
    fun testParse3() {
        try {
            // This was from http://brainsonly.com/servlets-newsday-crossword/newsdaycrossword?date=150903
            parse(
                BrainsOnlyIOTest::class.java
                    .getResourceAsStream("/brainsonly3.txt")!!
            )
        } catch (e : IOException) {
            return
        }
        Assertions.fail<String>("Expected brainsonly3.txt to fail to parse")
    }

    companion object {
        @JvmStatic
        val testPuzzle1InputStream : InputStream
            get() = BrainsOnlyIOTest::class.java
                .getResourceAsStream("/brainsonly.txt")!!

        @JvmStatic
        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals("02/07/14 SODA SPEAK", puz.title)
            Assertions.assertEquals(
                "S.N. &amp; Robert Francis, edited by Stanley Newman",
                puz.author,
            )
            Assertions.assertEquals("Test note", puz.notes)
            Assertions.assertEquals(LocalDate(2014, 2, 7), puz.date)

            val boxes : Array<Array<Box?>> = puz.boxes

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

            Assertions.assertEquals(boxes[0][0]!!.solution, "D")
            Assertions.assertEquals(boxes[5][14]!!.solution, "Y")
            Assertions.assertEquals(boxes[14][14]!!.solution, "P")
            Assertions.assertTrue(isBlock(boxes[14][5]))
            Assertions.assertTrue(isBlock(boxes[3][6]))

            Assertions.assertEquals(boxes[5][2]!!.shape, Box.Shape.CIRCLE)
            Assertions.assertFalse(boxes[5][3]!!.hasShape())

            Assertions.assertFalse(boxes[2][2]!!.hasColor())
            Assertions.assertTrue(boxes[2][3]!!.hasColor())

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Toss out",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("41")!!.hint,
                "Straighten out",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("1")!!.hint,
                "Sancho Panza's mount",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("59")!!.hint,
                "Part of pewter",
            )
        }
    }
}

