
package app.crossword.yourealwaysbe.puz.io

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.ObjectOutputStream

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.ClueID
import app.crossword.yourealwaysbe.puz.Note
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.IO.Companion.load
import app.crossword.yourealwaysbe.puz.io.IO.Companion.loadNative

/**
 *
 * @author kebernet
 */
class IOTest {
    @Test
    @Throws(Exception::class)
    fun testLoad() {
        testPuzzle1InputStream.use { inStream ->
            val puz = IO.loadNative(inStream)
            Companion.assertIsTestPuzzle1(puz!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testSave() {
        IOTest::class.java.getResourceAsStream("/test.puz").use { inStream ->
            val puz = loadNative(inStream)
            println("Loaded.")
            val tmp = File.createTempFile("test", ".puz")
            tmp.deleteOnExit()

            FileOutputStream(tmp).use { os ->
                IO.saveNative(puz!!, os)
            }
            FileInputStream(tmp).use { is2 ->
                val puz2 = loadNative(is2)
                Assertions.assertEquals(puz, puz2)
            }
            FileInputStream(tmp).use { isp ->
                val p = loadNative(isp)
                p!!.source = "Unit Test"
                // Check HTML stripped/restored
                p.notes = "X &nbsp; X<br/>"

                // play
                p.playerNote = Note("scratch", "a note", "anagsrc", "anagsol")


                val metaFile = File(
                    tmp.getParentFile(),
                    tmp.getName().substring(
                        0,
                        tmp.getName().lastIndexOf("."),
                    ) + ".forkyz"
                )
                metaFile.deleteOnExit()

                FileOutputStream(tmp).use { puzOS ->
                    FileOutputStream(metaFile).use { metaOS ->
                        IO.save(p, puzOS, metaOS)
                    }
                }
                DataInputStream(FileInputStream(tmp)).use { isp2 ->
                    DataInputStream(FileInputStream(metaFile)).use { ism ->
                        val pin = load(isp2, ism)
                        Assertions.assertEquals(p, pin)
                    }
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGext() {
        IOTest::class.java
            .getResourceAsStream("/2010-7-4-LosAngelesTimes.puz")
            .use { inStream ->
                var puz = loadNative(inStream)
                val tmp = File.createTempFile("test", ".puz")
                tmp.deleteOnExit()
                FileOutputStream(tmp).use { dos ->
                    IO.saveNative(puz!!, dos)
                    FileInputStream(tmp).use { is2 ->
                        puz = loadNative(is2)
                        Assertions.assertEquals(
                            puz!!.boxes[2][2]!!.shape,
                            Box.Shape.CIRCLE
                        )
                    }
                }
            }
    }

    @Test
    @Throws(Exception::class)
    fun testCrack() {
        println("testCrack")
        IOTest::class.java
            .getResourceAsStream("/puz_110523margulies.puz")
            .use { inStream ->
                val p = loadNative(inStream)
                val b = IO.crack(p!!)
            }
    }

    /**
     * Note: This is a sanity check, but any changes to unlock functionality should be tested more extensively.
     */
    @Test
    @Throws(Exception::class)
    fun testUnlockCode() {
        IOTest::class.java
            .getResourceAsStream("/2010-7-19-NewYorkTimes.puz")
            .use { inStream ->
                val puz = loadNative(inStream)
                for (b in puz!!.boxesList) {
                    if (!isBlock(b))
                        print(b!!.solution + " ")
                }
                println()
                try {
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
                    println()

                    ObjectOutputStream(
                        ByteArrayOutputStream()
                    ).use { oos ->
                        oos.writeObject(puz)
                    }
                } catch (e : Exception) {
                    e.printStackTrace()
                }
            }
    }

    @Test
    @Throws(Exception::class)
    fun testRebus() {
        testPuzzleRebusInputStream.use { inStream ->
            val puz = IO.loadNative(inStream)
            Companion.assertIsTestPuzzleRebus(puz!!)

            puz.checkedGetBox(1, 7)!!.response = "NEW RESPONSE 1"
            puz.checkedGetBox(7, 7)!!.response = "NEW RESPONSE 2"
            puz.checkedGetBox(1, 8)!!.response = "NEW REBUS"

            val tmp = File.createTempFile("test", ".puz")
            tmp.deleteOnExit()

            val metaFile = File(
                tmp.getParentFile(),
                tmp.getName().substring(
                    0,
                    tmp.getName().lastIndexOf("."),
                ) + ".forkyz"
            )
            metaFile.deleteOnExit()

            FileOutputStream(tmp).use { puzOS ->
                FileOutputStream(metaFile).use { metaOS ->
                    IO.save(puz, puzOS, metaOS)
                }
            }
            DataInputStream(FileInputStream(tmp)).use { isp2 ->
                DataInputStream(FileInputStream(metaFile)).use { ism ->
                    val pin = load(isp2, ism)
                    Assertions.assertEquals(puz, pin)
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testEmptySolution() {
        testPuzzleEmptySolInputStream.use { inStream ->
            val puz = IO.loadNative(inStream)
            Assertions.assertFalse(puz!!.hasSolution())

            val tmp = File.createTempFile("test", ".puz")
            tmp.deleteOnExit()

            FileOutputStream(tmp).use { puzOS ->
                IO.saveNative(puz, puzOS)
            }
            DataInputStream(FileInputStream(tmp)).use { isp2 ->
                val pin = IO.loadNative(isp2)
                Assertions.assertFalse(pin!!.hasSolution())
            }
        }
    }

    companion object {
        @JvmStatic
        val testPuzzle1InputStream : InputStream
            get() = IOTest::class.java.getResourceAsStream("/test.puz")!!

        val testPuzzleRebusInputStream : InputStream
            get() = IOTest::class.java
                .getResourceAsStream("/test_rebus.puz")!!

        val testPuzzleEmptySolInputStream : InputStream
            get() = IOTest::class.java
                .getResourceAsStream("/test_empty_sol.puz")!!

        @JvmStatic
        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals("NY Times, Fri, Nov 13, 2009", puz.title)
            Assertions.assertEquals("Dana Motley / Will Shortz", puz.author)
            Assertions.assertEquals(
                "&copy; 2009, The New York Times",
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
