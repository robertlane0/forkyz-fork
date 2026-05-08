
package app.crossword.yourealwaysbe.puz.io

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.ExolveIO.Companion.readPuzzle
import app.crossword.yourealwaysbe.puz.util.HtmlUtil.parseHtmlColor

class ExolveIOTest {
    @Test
    fun testPuzzle1() {
        val puz = testPuzzle1.use { readPuzzle(it) }
        assertIsTestPuzzle1(puz!!)
    }

    @Test
    fun testPuzzleRebus() {
        val puz = testPuzzleRebus.use { readPuzzle(it) }
        assertIsTestPuzzleRebus(puz!!)
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/test.exolve")
        val testPuzzleRebus
            get() = SourceUtils.getResource("/rebus.exolve")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.title, "Test title")
            Assertions.assertEquals(puz.author, "Test author")
            Assertions.assertEquals(puz.copyright, "Test copyright")
            Assertions.assertEquals(
                puz.introMessage,
                "Test preamble<br/>Multiple lines<br/><br/>Test prelude",
            )
            Assertions.assertEquals(
                puz.notes,
                "test@email.com<br/><br/>Tester: test<br/>Tester 2: test2",
            )
            Assertions.assertEquals(
                puz.completionMessage,
                (
                    "Everything is explained."
                    + "<h2>Across</h2>"
                    + "<p>1. ANNOT1a</p>"
                    + "<p>6. ANNOT6a</p>"
                    + "<h2>Down</h2>"
                    + "<p>3. ANNOT3d</p>"
                ),
            )

            Assertions.assertEquals(puz.width, 5)
            Assertions.assertEquals(puz.height, 6)

            Assertions.assertTrue(isBlock(puz.checkedGetBox(1, 1)))
            Assertions.assertTrue(isBlock(puz.checkedGetBox(3, 3)))
            Assertions.assertEquals(puz.checkedGetBox(0, 2)!!.solution, "A")
            Assertions.assertEquals(puz.checkedGetBox(2, 4)!!.solution, "B")
            Assertions.assertFalse(puz.checkedGetBox(2, 4)!!.hasShape())
            Assertions.assertEquals(
                puz.checkedGetBox(0, 1)!!.shape,
                Box.Shape.CIRCLE,
            )
            Assertions.assertFalse(puz.checkedGetBox(2, 4)!!.hasBars())
            Assertions.assertEquals(
                puz.checkedGetBox(1, 0)!!.barBottom,
                Box.Bar.SOLID,
            )
            Assertions.assertEquals(
                puz.checkedGetBox(1, 0)!!.barRight,
                Box.Bar.NONE,
            )
            Assertions.assertEquals(
                puz.checkedGetBox(1, 2)!!.barBottom,
                Box.Bar.NONE,
            )
            Assertions.assertEquals(
                puz.checkedGetBox(1, 2)!!.barRight,
                Box.Bar.SOLID,
            )
            Assertions.assertEquals(
                puz.checkedGetBox(1, 4)!!.barBottom,
                Box.Bar.SOLID,
            )
            Assertions.assertEquals(
                puz.checkedGetBox(1, 4)!!.barRight,
                Box.Bar.SOLID,
            )
            Assertions.assertEquals(
                puz.checkedGetBox(2, 1)!!.barBottom,
                Box.Bar.SOLID,
            )
            Assertions.assertEquals(
                puz.checkedGetBox(3, 2)!!.initialValue,
                "C",
            )
            Assertions.assertFalse(puz.checkedGetBox(3, 0)!!.hasInitialValue())
            Assertions.assertEquals(puz.checkedGetBox(0, 4)!!.solution, "&")
            Assertions.assertEquals(puz.checkedGetBox(4, 1)!!.solution, "1")
            Assertions.assertEquals(puz.checkedGetBox(4, 2)!!.solution, "2")

            Assertions.assertEquals(puz.checkedGetBox(0, 0)!!.clueNumber, "1")
            Assertions.assertEquals(puz.checkedGetBox(2, 0)!!.clueNumber, "N")
            Assertions.assertEquals(puz.checkedGetBox(4, 4)!!.clueNumber, "6")

            val green = parseHtmlColor("green")
            val red = parseHtmlColor("red")
            Assertions.assertEquals(puz.checkedGetBox(0, 1)!!.color, green)
            Assertions.assertEquals(puz.checkedGetBox(0, 4)!!.color, green)
            Assertions.assertEquals(puz.checkedGetBox(4, 1)!!.color, green)
            Assertions.assertEquals(puz.checkedGetBox(4, 3)!!.color, green)
            Assertions.assertEquals(puz.checkedGetBox(0, 2)!!.color, red)
            Assertions.assertEquals(puz.checkedGetBox(3, 2)!!.color, red)
            Assertions.assertEquals(puz.checkedGetBox(2, 1)!!.color, red)

            val clue1a = puz.getClues("Across")!!.getClueByNumber("1")
            val zone1a = clue1a!!.zone
            Assertions.assertEquals(clue1a.hint, "Clue 1a (5)")
            Assertions.assertEquals(zone1a.size(), 5)
            Assertions.assertEquals(zone1a.getPosition(0), Position(0, 0))
            Assertions.assertEquals(zone1a.getPosition(4), Position(0, 4))

            val clueNa = puz.getClues("Across")!!.getClueByNumber("N")
            val zoneNa = clueNa!!.zone
            Assertions.assertEquals(clueNa.hint, "Clue Na (5)")
            Assertions.assertEquals(zoneNa.size(), 2)
            Assertions.assertEquals(zoneNa.getPosition(0), Position(2, 0))
            Assertions.assertEquals(zoneNa.getPosition(1), Position(2, 1))

            val clue6a = puz.getClues("Across")!!.getClueByNumber("6")
            val zone6a = clue6a!!.zone
            Assertions.assertEquals(zone6a.size(), 5)
            Assertions.assertEquals(zone6a.getPosition(0), Position(4, 4))
            Assertions.assertEquals(zone6a.getPosition(1), Position(4, 3))
            Assertions.assertEquals(zone6a.getPosition(4), Position(4, 0))

            val clueFiller = puz.getClues("Across")!!.getClueByIndex(1)
            Assertions.assertFalse(clueFiller!!.hasClueNumber())
            Assertions.assertEquals(clueFiller.hint, "All filler")

            val clue2d = puz.getClues("Down")!!.getClueByNumber("2")
            val zone2d = clue2d!!.zone
            Assertions.assertEquals(clue2d.hint, "Clue 2d")
            Assertions.assertEquals(zone2d.size(), 5)
            Assertions.assertEquals(zone2d.getPosition(0), Position(0, 2))
            Assertions.assertEquals(zone2d.getPosition(4), Position(4, 2))

            val clue3d = puz.getClues("Down")!!.getClueByNumber("3")
            val zone3d = clue3d!!.zone
            Assertions.assertEquals(clue3d.hint, ", 4a & [N] Clue <u>3d</u>")
            Assertions.assertEquals(zone3d.size(), 7)
            Assertions.assertEquals(zone3d.getPosition(0), Position(0, 4))
            Assertions.assertEquals(zone3d.getPosition(2), Position(2, 2))
            Assertions.assertEquals(zone3d.getPosition(5), Position(2, 0))
            Assertions.assertEquals(zone3d.getPosition(6), Position(2, 1))

            val clue6d = puz.getClues("Down")!!.getClueByNumber("6")
            val zone6d = clue6d!!.zone
            Assertions.assertEquals(zone6d.size(), 2)
            Assertions.assertEquals(zone6d.getPosition(0), Position(4, 4))
            Assertions.assertEquals(zone6d.getPosition(1), Position(5, 4))


            val clueA = puz.getClues("NODIR")!!.getClueByNumber("A")
            val zoneA = clueA!!.zone
            Assertions.assertEquals(clueA.hint, "ClueA")
            Assertions.assertEquals(zoneA.size(), 3)
            Assertions.assertEquals(zoneA.getPosition(0), Position(0, 0))
            Assertions.assertEquals(zoneA.getPosition(1), Position(1, 2))
            Assertions.assertEquals(zoneA.getPosition(2), Position(4, 3))

            Assertions.assertNull(puz.getClues("Across")!!.getClueByNumber("8"))
        }

        fun assertIsTestPuzzleRebus(puz: Puzzle) {
            Assertions.assertEquals(puz.width, 3)
            Assertions.assertEquals(puz.height, 2)
            Assertions.assertEquals(puz.checkedGetBox(0, 0)!!.solution, "ONE")
            Assertions.assertEquals(
                puz.checkedGetBox(0, 1)!!.solution,
                Box.BLANK,
            )
            Assertions.assertEquals(puz.checkedGetBox(0, 2)!!.solution, "TWO")
            Assertions.assertEquals(
                puz.checkedGetBox(1, 0)!!.solution,
                Box.BLANK,
            )
            Assertions.assertEquals(puz.checkedGetBox(1, 1)!!.solution, "THREE")
            Assertions.assertEquals(
                puz.checkedGetBox(1, 2)!!.solution,
                Box.BLANK,
            )
            Assertions.assertFalse(puz.checkedGetBox(0, 0)!!.hasClueNumber())
            Assertions.assertFalse(puz.checkedGetBox(0, 1)!!.hasClueNumber())
            Assertions.assertFalse(puz.checkedGetBox(0, 2)!!.hasClueNumber())
            Assertions.assertFalse(puz.checkedGetBox(1, 0)!!.hasClueNumber())
            Assertions.assertFalse(puz.checkedGetBox(1, 1)!!.hasClueNumber())
            Assertions.assertFalse(puz.checkedGetBox(1, 2)!!.hasClueNumber())
        }
    }
}

