
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.coroutines.runBlocking
import kotlinx.io.Buffer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.forkyz.puz.Box
import app.crossword.yourealwaysbe.forkyz.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.forkyz.puz.ClueID
import app.crossword.yourealwaysbe.forkyz.puz.Note
import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.load
import app.crossword.yourealwaysbe.forkyz.puz.io.IO.Companion.loadNative
import app.crossword.yourealwaysbe.forkyz.util.SourceUtils

/**
 *
 * @author kebernet
 */
class IOTest {
    @Test
    fun testLoad() {
        val puz = testPuzzle1.use { runBlocking { IO.loadNative(it) } }
        assertIsTestPuzzle1(puz!!)
    }

    @Test
    fun testSave() {
        val puz = testPuzzle1.use { runBlocking { loadNative(it) } }
        val tmp = Buffer()
        IO.saveNative(puz!!, tmp)
        val puz2 = runBlocking { loadNative(tmp.peek()) }
        Assertions.assertEquals(puz, puz2)

        val p = runBlocking { loadNative(tmp) }
        p!!.source = "Unit Test"
        // Check HTML stripped/restored
        p.notes = "X &nbsp; X<br/>"

        // play
        p.playerNote = Note("scratch", "a note", "anagsrc", "anagsol")

        val tmp2 = Buffer()
        val tmpMeta = Buffer()

        IO.save(p, tmp2, tmpMeta)
        val pin = runBlocking { load(tmp2, tmpMeta) }
        Assertions.assertEquals(p, pin)
    }

    @Test
    fun testGext() {
        var puz =  SourceUtils.getResource("/2010-7-4-LosAngelesTimes.puz").use {
           runBlocking { loadNative(it) }
        }
        val tmp = Buffer()
        IO.saveNative(puz!!, tmp)
        puz = runBlocking { loadNative(tmp) }
        Assertions.assertEquals(puz!!.boxes[2][2]!!.shape, Box.Shape.CIRCLE)
    }

    @Test
    fun testCrack() {
        println("testCrack")
        SourceUtils.getResource("/puz_110523margulies.puz").use { input ->
            val p = runBlocking { loadNative(input) }
            IO.crack(p!!)
        }
    }

    /**
     * Note: This is a sanity check, but any changes to unlock
     * functionality should be tested more extensively.
     */
    @Test
    fun testUnlockCode() {
        val puz = SourceUtils.getResource("/2010-7-19-NewYorkTimes.puz").use {
            runBlocking { loadNative(it) }
        }
        for (b in puz!!.boxesList) {
            if (!isBlock(b))
                print(b!!.solution + " ")
        }
        println()
        Assertions.assertTrue(
            IO.tryUnscramble(
                puz,
                2465,
                IO.initializeUnscrambleData(puz),
            )
        )
        for (b in puz.boxesList) {
            if (!isBlock(b))
                print(b!!.solution + " ")
        }
    }

    @Test
    fun testRebus() {
        val puz = testPuzzleRebus.use { runBlocking { IO.loadNative(it) } }
        assertIsTestPuzzleRebus(puz!!)

        puz.checkedGetBox(1, 7)!!.response = "NEW RESPONSE 1"
        puz.checkedGetBox(7, 7)!!.response = "NEW RESPONSE 2"
        puz.checkedGetBox(1, 8)!!.response = "NEW REBUS"

        val tmp = Buffer()
        val tmpMeta = Buffer()

        runBlocking { IO.save(puz, tmp, tmpMeta) }
        val pin = runBlocking { load(tmp, tmpMeta) }

        Assertions.assertEquals(puz, pin)
    }

    @Test
    fun testEmptySolution() {
        val puz = testPuzzleEmptySol.use { runBlocking { IO.loadNative(it) } }
        Assertions.assertFalse(puz!!.hasSolution())

        val tmp = Buffer()
        IO.saveNative(puz, tmp)
        val pin = runBlocking { IO.loadNative(tmp) }
        Assertions.assertFalse(pin!!.hasSolution())
    }

    companion object {
        val testPuzzle1
            get() = SourceUtils.getResource("/test.puz")
        val testPuzzleRebus
            get() = SourceUtils.getResource("/test_rebus.puz")
        val testPuzzleEmptySol
            get() = SourceUtils.getResource("/test_empty_sol.puz")

        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals("NY Times, Fri, Nov 13, 2009", puz.title)
            Assertions.assertEquals("Dana Motley / Will Shortz", puz.author)
            Assertions.assertEquals(
                "© 2009, The New York Times",
                puz.copyright,
            )

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

            Assertions.assertEquals(boxes[0][0]!!.solution, "R")
            Assertions.assertTrue(isBlock(boxes[5][14]))
            Assertions.assertEquals(boxes[14][14]!!.solution, "S")
            Assertions.assertEquals(boxes[14][5]!!.solution, "T")
            Assertions.assertEquals(boxes[3][6]!!.solution, "E")

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "&lt;ring to perfection",
            )
            Assertions.assertEquals(
                acrossClues.getClueByNumber("23")!!.hint,
                "Surprised reaction",
            )
            Assertions.assertEquals(
                downClues!!.getClueByNumber("5")!!.hint,
                "Sch. whose sports teams are the Violets",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("6")!!.hint,
                "Not work at all",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("7")!!.hint,
                "Kale kin",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("13")!!.hint,
                "President who was born a King",
            )
        }

        fun assertIsTestPuzzleRebus(puz : Puzzle) {
            val boxes : Array<Array<Box?>> = puz.boxes
            Assertions.assertEquals("REBUS", boxes[1][7]!!.solution)
            Assertions.assertEquals("ENTRY", boxes[1][7]!!.response)
            Assertions.assertEquals("X", boxes[1][8]!!.solution)
            Assertions.assertEquals("REBUS", boxes[7][7]!!.solution)
            Assertions.assertEquals("ENTRY", boxes[7][7]!!.response)
            Assertions.assertEquals("X", boxes[7][8]!!.solution)
            Assertions.assertEquals("REBUS", boxes[13][7]!!.solution)
            Assertions.assertEquals("ENTRY", boxes[13][7]!!.response)
            Assertions.assertEquals("X", boxes[13][8]!!.solution)
        }
    }
}
