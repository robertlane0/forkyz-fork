
package app.crossword.yourealwaysbe.puz.io

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.datetime.LocalDate

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import app.crossword.yourealwaysbe.puz.Box
import app.crossword.yourealwaysbe.puz.Box.Companion.isBlock
import app.crossword.yourealwaysbe.puz.Note
import app.crossword.yourealwaysbe.puz.Position
import app.crossword.yourealwaysbe.puz.PuzImage
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.PuzzleKind

class IPuzIOTest {
    @Test
    @Throws(Exception::class)
    fun testIPuz() {
        testPuzzle1InputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzle1(puz!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzWriteRead() {
        testPuzzle1InputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz!!, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)

            // hack to get around html encoding of certain chars
            Assertions.assertEquals(puz2!!.title, "Test &amp; puzzle &#x2018;")
            puz.title = puz2.title
            Assertions.assertEquals(puz, puz2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzReadPlayWriteRead() {
        testPuzzle1InputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            val across = puz!!.getClues("Across")
            val vertical = puz.getClues("Vertical")

            val clueA1 = across!!.getClueByNumber("1")
            val clueA3 = across.getClueByNumber("3")
            val clueV1 = vertical!!.getClueByNumber("1")
            val cidA1 = clueA1!!.clueID
            val cidA3 = clueA3!!.clueID
            val cidV1 = clueV1!!.clueID
            val cidV2 = vertical.getClueByNumber("2")!!.clueID

            puz.supportUrl = "http://test.url"
            puz.shareUrl = "http://testshare.url"
            puz.time = 1234L
            puz.position = Position(2, 1)
            puz.currentClueID = cidA3

            puz.updateHistory(cidA3)
            puz.updateHistory(cidV1)

            puz.setNote(
                cidA1,
                Note("test1", "test2", "test3", "test4")
            )
            puz.setNote(
                cidV2,
                Note("test5", "test6\nnew line", "test7", "test8")
            )
            clueA3.isFlagged = true
            clueV1.isFlagged = true
            clueV1.flagColor = 0x0000ff00

            puz.playerNote = Note("scratch", "a note", "anagsrc", "anagsol")


            puz.addImage(PuzImage("myimage.jpg", 2, 3, 4, 5))

            val boxes : Array<Array<Box?>> = puz.boxes

            boxes[0][1]!!.response = "X"
            boxes[1][2]!!.response = "Y"
            boxes[0][1]!!.responder = "Test"
            boxes[1][0]!!.isCheated = true
            boxes[1][1]!!.isFlagged = true
            boxes[1][0]!!.isFlagged = true
            boxes[1][0]!!.flagColor = 0x0000ff00

            puz.pinnedClueID = cidA3
            puz.rating = 5.toChar()

            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)

            val boxes2 : Array<Array<Box?>> = puz2!!.boxes

            Assertions.assertEquals(puz2.supportUrl, "http://test.url")
            Assertions.assertEquals(puz2.shareUrl, "http://testshare.url")
            Assertions.assertEquals(puz2.time, 1234L)
            Assertions.assertEquals(puz.position, puz2.position)
            Assertions.assertEquals(puz.currentClueID, cidA3)
            Assertions.assertEquals(puz.history.get(0), cidV1)
            Assertions.assertEquals(puz.history.get(1), cidA3)
            Assertions.assertEquals(puz.getNote(cidA1)!!.text, "test2")
            Assertions.assertEquals(
                puz.getNote(cidV2)!!.text,
                "test6\nnew line",
            )
            Assertions.assertEquals(puz.getNote(cidV2)!!.anagramSource, "test7")
            Assertions.assertEquals(boxes2[0][1]!!.response, "X")
            Assertions.assertEquals(boxes2[1][2]!!.response, "Y")
            Assertions.assertEquals(boxes2[0][1]!!.responder, "Test")
            Assertions.assertFalse(boxes2[0][1]!!.isCheated)
            Assertions.assertTrue(boxes2[1][0]!!.isCheated)
            Assertions.assertTrue(boxes2[1][1]!!.isFlagged)
            Assertions.assertTrue(boxes2[1][1]!!.isDefaultFlagColor())
            Assertions.assertTrue(boxes[1][0]!!.isFlagged)
            Assertions.assertFalse(boxes[1][0]!!.isDefaultFlagColor())
            Assertions.assertEquals(boxes[1][0]!!.flagColor, 0x0000ff00)
            Assertions.assertTrue(clueV1.isFlagged)
            Assertions.assertFalse(clueV1.isDefaultFlagColor)
            Assertions.assertEquals(clueV1.flagColor, 0x0000ff00)
            Assertions.assertTrue(clueA3.isFlagged)
            Assertions.assertTrue(clueA3.isDefaultFlagColor)
            Assertions.assertFalse(clueA1.isFlagged)
            Assertions.assertEquals(puz.pinnedClueID, cidA3)
            Assertions.assertEquals(puz.rating, 5.toChar())

            // get around html entity rewrite
            puz.title = puz2.title
            Assertions.assertEquals(puz, puz2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzHTML() {
        testPuzzleHTMLInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzleHTML(puz!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzWriteReadHTML() {
        testPuzzleHTMLInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz!!, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)
            Assertions.assertEquals(puz, puz2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzBarred() {
        testPuzzle2InputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzle2(puz!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzReadPlayWriteReadBarred() {
        testPuzzle2InputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            val across = puz!!.getClues("Across")
            val down = puz.getClues("Down")

            val cidD2 = down!!.getClueByNumber("2")!!.clueID
            val cidD3 = down.getClueByNumber("3")!!.clueID
            val cidD12 = down.getClueByNumber("12")!!.clueID
            val cidA1 = across!!.getClueByNumber("1")!!.clueID

            puz.supportUrl = "http://test.url"
            puz.time = 1234L
            puz.position = Position(1, 2)
            puz.currentClueID = cidD12

            puz.updateHistory(cidD3)
            puz.updateHistory(cidA1)

            puz.setNote(cidA1, Note("test1", "test2", "test3", "test4"))
            puz.setNote(
                cidD2,
                Note("test5", "test6\nnew line", "test7", "test8"),
            )

            val boxes : Array<Array<Box?>> = puz.boxes

            boxes[0][1]!!.response = "X"
            boxes[1][2]!!.response = "Y"
            boxes[0][1]!!.responder = "Test"
            boxes[1][0]!!.isCheated = true

            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)

            val boxes2 : Array<Array<Box?>> = puz2!!.boxes

            Assertions.assertEquals(puz2.supportUrl, "http://test.url")
            Assertions.assertEquals(puz2.time, 1234L)
            Assertions.assertEquals(puz.position, puz2.position)
            Assertions.assertEquals(puz.currentClueID, cidD12)
            Assertions.assertEquals(puz.history.get(0), cidA1)
            Assertions.assertEquals(puz.history.get(1), cidD3)
            Assertions.assertEquals(puz.getNote(cidA1)!!.text, "test2")
            Assertions.assertEquals(
                puz.getNote(cidD2)!!.text,
                "test6\nnew line",
            )
            Assertions.assertEquals(puz.getNote(cidD2)!!.anagramSource, "test7")
            Assertions.assertEquals(boxes2[0][1]!!.response, "X")
            Assertions.assertEquals(boxes2[1][2]!!.response, "Y")
            Assertions.assertEquals(boxes2[0][1]!!.responder, "Test")
            Assertions.assertFalse(boxes2[0][1]!!.isCheated)
            Assertions.assertTrue(boxes2[1][0]!!.isCheated)
            Assertions.assertEquals(puz, puz2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzExtras() {
        testPuzzleExtrasInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzleExtras(puz!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzWriteReadExtras() {
        testPuzzleExtrasInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz!!, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)
            Assertions.assertEquals(puz, puz2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzZones() {
        testPuzzleZonesInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzleZones(puz!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzZones1Based() {
        testPuzzleZones1BasedInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzleZones(puz!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzZonesIOV2() {
        testPuzzleZonesIOV2InputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzleZones(puz!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzWriteReadZones() {
        testPuzzleZonesInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz!!, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)
            Assertions.assertEquals(puz, puz2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzAcrostic() {
        testPuzzleAcrosticInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzleAcrostic(puz!!)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzWriteReadAcrostic() {
        testPuzzleAcrosticInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz!!, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)
            Assertions.assertEquals(puz, puz2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testJPZAcrosticWriteIPuzRead() {
        JPZIOTest.testPuzzleAcrosticInputStream.use { inStream ->
            val puz = JPZIO.readPuzzle(inStream)
            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz!!, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)

            // bit of a hack because "" is read back as null
            puz.notes = null
            // hack: skip completion msg because of html entity encoding
            puz.completionMessage = puz2!!.completionMessage
            Assertions.assertEquals(puz, puz2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzShapeReadWrite() {
        testPuzzleShapesInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzleShapes(puz!!)

            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)

            // bit of a hack because "" is read back as null
            puz.notes = null
            Assertions.assertEquals(puz, puz2)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testIPuzExtensionsWriteRead() {
        testPuzzleExtensionsInputStream.use { inStream ->
            val puz = IPuzIO.readPuzzle(inStream)
            Companion.assertIsTestPuzzleExtensions(puz!!)

            val baos = ByteArrayOutputStream()
            IPuzIO.writePuzzle(puz, baos)
            baos.close()

            val bais = ByteArrayInputStream(baos.toByteArray())

            val puz2 = IPuzIO.readPuzzle(bais)
            Assertions.assertEquals(puz, puz2)
        }
    }

    companion object {
        @JvmStatic
        val testPuzzle1InputStream : InputStream
            get() = IPuzIOTest::class.java
                .getResourceAsStream("/test.ipuz")!!

        val testPuzzle2InputStream : InputStream
            get() = IPuzIOTest::class.java
                .getResourceAsStream("/barred-test.ipuz")!!

        val testPuzzleExtrasInputStream : InputStream
            get() = IPuzIOTest::class.java
                .getResourceAsStream("/extras.ipuz")!!

        val testPuzzleZonesInputStream : InputStream
            get() = IPuzIOTest::class.java
                .getResourceAsStream("/zones-0-based.ipuz")!!

        val testPuzzleZones1BasedInputStream : InputStream
            get() = IPuzIOTest::class.java
                .getResourceAsStream("/zones-1-based.ipuz")!!

        val testPuzzleZonesIOV2InputStream : InputStream
            // same as 1-based, but check picked up by version number
            get() = IPuzIOTest::class.java
                .getResourceAsStream("/zones-io-v2.ipuz")!!

        val testPuzzleAcrosticInputStream : InputStream
            get() = IPuzIOTest::class.java
                .getResourceAsStream("/acrostic.ipuz")!!

        val testPuzzleShapesInputStream : InputStream
            get() = IPuzIOTest::class.java
                .getResourceAsStream("/shapes.ipuz")!!

        val testPuzzleExtensionsInputStream : InputStream
            get() = IPuzIOTest::class.java
                .getResourceAsStream("/extensions.ipuz")!!

        @JvmStatic
        @Throws(Exception::class)
        fun assertIsTestPuzzle1(puz : Puzzle) {
            Assertions.assertEquals(puz.kind, PuzzleKind.CROSSWORD)

            Assertions.assertEquals(puz.title, "Test &amp; puzzle ‘")
            Assertions.assertEquals(puz.author, "Test author")
            Assertions.assertEquals(puz.copyright, "Test copyright")
            Assertions.assertEquals(puz.sourceUrl, "https://testurl.com")
            Assertions.assertEquals(puz.source, "Test publisher")
            Assertions.assertEquals(puz.introMessage, "Intro")
            Assertions.assertEquals(puz.completionMessage, "Explanation")
            Assertions.assertEquals(puz.date, LocalDate(2003, 2, 1))

            Assertions.assertEquals(puz.width, 3)
            Assertions.assertEquals(puz.height, 2)

            val boxes : Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][0]!!.clueNumber, "1")
            Assertions.assertEquals(boxes[0][1]!!.clueNumber, "2")
            Assertions.assertFalse(boxes[0][1]!!.hasShape())
            Assertions.assertTrue(isBlock(boxes[0][2]))
            Assertions.assertEquals(boxes[1][0]!!.clueNumber, "3")
            Assertions.assertEquals(boxes[1][0]!!.response, "A")
            Assertions.assertEquals(boxes[1][0]!!.shape, Box.Shape.CIRCLE)

            Assertions.assertTrue(boxes[0][0]!!.isBlank())
            Assertions.assertEquals(boxes[0][1]!!.response, "B")
            Assertions.assertEquals(boxes[1][1]!!.response, "C")
            Assertions.assertTrue(boxes[1][2]!!.isBlank())

            Assertions.assertEquals(boxes[0][0]!!.solution, "A")
            Assertions.assertEquals(boxes[0][1]!!.solution, "B")
            Assertions.assertEquals(boxes[1][0]!!.solution, "A")
            Assertions.assertEquals(boxes[1][1]!!.solution, "C")
            Assertions.assertEquals(boxes[1][2]!!.solution, "D")

            Assertions.assertFalse(boxes[0][0]!!.hasMarks())
            Assertions.assertTrue(boxes[1][0]!!.hasMarks())
            Assertions.assertEquals(boxes[1][0]!!.marks!![1][2], "t")
            Assertions.assertNull(boxes[1][0]!!.marks!![0][2])

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Vertical")

            val clueOneAcross = acrossClues!!.getClueByNumber("1")
            Assertions.assertEquals(clueOneAcross!!.hint, "Test clue 1")
            Assertions.assertFalse(clueOneAcross.hasLabel())
            Assertions.assertEquals(
                acrossClues.getClueByNumber("3")!!.hint,
                "Test clue 2",
            )

            val clueOneDown = downClues!!.getClueByNumber("1")
            Assertions.assertEquals(clueOneDown!!.label, "One")
            Assertions.assertEquals(clueOneDown.hint, "Test clue 3")
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Test clue 4 (cont. 1 Across/1 Down) "
                    + "(ref. 1&2 Across) (clues 2/1/3) (3-2-1)",
            )
        }

        @Throws(Exception::class)
        fun assertIsTestPuzzle2(puz : Puzzle) {
            val boxes : Array<Array<Box?>> = puz.boxes

            Assertions.assertTrue(boxes[1][1]!!.isBarredTop())
            Assertions.assertFalse(boxes[0][2]!!.isBarredBottom())
            Assertions.assertTrue(boxes[3][4]!!.isBarredLeft())
            Assertions.assertFalse(boxes[3][4]!!.isBarredRight())

            Assertions.assertEquals(boxes[8][3]!!.solution, "V")
            Assertions.assertEquals(boxes[10][1]!!.solution, "R")
            Assertions.assertEquals(boxes[1][10]!!.solution, "W")

            Assertions.assertEquals(boxes[1][2]!!.shape, Box.Shape.CIRCLE)
            Assertions.assertFalse(boxes[2][1]!!.hasShape())

            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Down")

            Assertions.assertTrue(
                boxes[0][7]!!.isPartOf(acrossClues!!.getClueByNumber("5")),
            )
            Assertions.assertFalse(
                boxes[0][7]!!.isPartOf(downClues!!.getClueByNumber("5")),
            )
            Assertions.assertTrue(
                boxes[1][7]!!.isPartOf(downClues.getClueByNumber("6")),
            )
            Assertions.assertFalse(
                boxes[1][7]!!.isPartOf(downClues.getClueByNumber("5")),
            )

            Assertions.assertEquals(
                acrossClues.getClueByNumber("5")!!.hint,
                "Clue 5",
            )
            Assertions.assertEquals(
                downClues.getClueByNumber("2")!!.hint,
                "Clue 2d",
            )
        }

        @Throws(Exception::class)
        fun assertIsTestPuzzleExtras(puz : Puzzle) {
            Assertions.assertEquals(
                puz.introMessage,
                "Weird features: unnumbered words, clues "
                    + "in non-standards lists, colors, etc",
            )
            Assertions.assertEquals(puz.notes, "Puzzle notes")
            Assertions.assertEquals(
                puz.completionMessage,
                "You solved the puzzle",
            )

            val boxes : Array<Array<Box?>> = puz.boxes

            Assertions.assertFalse(boxes[0][6]!!.hasClueNumber())
            Assertions.assertEquals(boxes[0][8]!!.clueNumber, "5")
            Assertions.assertFalse(boxes[6][0]!!.hasClueNumber())
            Assertions.assertEquals(boxes[7][0]!!.clueNumber, "25")

            Assertions.assertFalse(boxes[2][0]!!.hasColor())
            Assertions.assertFalse(boxes[8][10]!!.hasColor())
            Assertions.assertTrue(boxes[6][0]!!.hasColor())
            Assertions.assertTrue(boxes[10][6]!!.hasColor())
            val grey1 = "DCDCDC".toInt(16)
            val grey2 = "EFEFEF".toInt(16)
            val grey3 = "EAEAEA".toInt(16)
            val darkGreen = "50c878".toInt(16)
            val yellow = "FFFAA0".toInt(16)
            Assertions.assertEquals(boxes[0][0]!!.color, yellow)
            Assertions.assertFalse(boxes[1][1]!!.hasColor())
            Assertions.assertEquals(boxes[6][0]!!.color, grey1)
            Assertions.assertEquals(boxes[10][6]!!.color, grey1)
            Assertions.assertEquals(boxes[0][2]!!.color, grey2)
            Assertions.assertEquals(boxes[0][3]!!.color, grey3)
            Assertions.assertFalse(boxes[0][3]!!.hasTextColor())
            Assertions.assertFalse(boxes[0][3]!!.hasBarColor())
            Assertions.assertEquals(boxes[0][8]!!.color, darkGreen)
            Assertions.assertTrue(boxes[0][8]!!.hasTextColor())
            Assertions.assertTrue(boxes[0][8]!!.hasBarColor())
            Assertions.assertEquals(boxes[0][8]!!.textColor, "123456".toInt(16))
            Assertions.assertEquals(boxes[0][8]!!.barColor, "654321".toInt(16))
            Assertions.assertFalse(boxes[0][9]!!.hasBarColor())
            Assertions.assertEquals(boxes[0][9]!!.textColor, "121212".toInt(16))
            Assertions.assertFalse(boxes[0][10]!!.hasTextColor())
            Assertions.assertEquals(boxes[0][10]!!.barColor, "212121".toInt(16))
            Assertions.assertEquals(boxes[1][2]!!.initialValue, "A")
            Assertions.assertFalse(boxes[1][1]!!.hasInitialValue())
            Assertions.assertEquals(boxes[1][4]!!.initialValue, "Y")

            val interestingBlock = boxes[1][3]!!
            Assertions.assertTrue(interestingBlock.isBlock)
            Assertions.assertEquals(interestingBlock.color, grey2)
            Assertions.assertEquals(interestingBlock.initialValue, "X")
            Assertions.assertEquals(interestingBlock.response, "X")

            Assertions.assertTrue(puz.hasInitialValueCells())

            val bars = boxes[0][4]!!
            Assertions.assertEquals(bars.barTop, Box.Bar.DASHED)
            Assertions.assertEquals(bars.barBottom, Box.Bar.DOTTED)
            Assertions.assertEquals(bars.barLeft, Box.Bar.SOLID)
            Assertions.assertEquals(bars.barRight, Box.Bar.NONE)

            val clueLists = puz.clueListNames
            Assertions.assertEquals(clueLists.size, 3)
            Assertions.assertTrue(clueLists.contains("OddOnes"))

            val oddClues = puz.getClues("OddOnes")

            Assertions.assertEquals(
                oddClues!!.getClueByIndex(5)!!.hint,
                "Odd sixth",
            )
            Assertions.assertEquals(
                oddClues.getClueByIndex(0)!!.hint,
                "Odd first",
            )
        }

        @Throws(Exception::class)
        fun assertIsTestPuzzleZones(puz : Puzzle) {
            val boxes : Array<Array<Box?>> = puz.boxes

            val bases = puz.getClues("Bases")
            val pathways = puz.getClues("Pathways")

            Assertions.assertTrue(
                boxes[2][2]!!.isPartOf(bases!!.getClueByNumber("&#x1f332;")),
            )
            Assertions.assertFalse(
                boxes[2][2]!!.isPartOf(pathways!!.getClueByNumber("1")),
            )
            Assertions.assertTrue(
                boxes[1][8]!!.isPartOf(bases.getClueByNumber("&#x2615;")),
            )
            Assertions.assertFalse(
                boxes[1][8]!!.isPartOf(pathways.getClueByNumber("2")),
            )
            Assertions.assertTrue(
                boxes[7][1]!!.isPartOf(pathways.getClueByNumber("5")),
            )
            Assertions.assertFalse(
                boxes[7][1]!!.isPartOf(bases.getClueByNumber("&#x1f98a;")),
            )

            val zoneTree = bases.getClueByNumber("&#x2615;")!!.zone
            val zoneThumb = bases.getClueByNumber("&#x1f44d;")!!.zone

            Assertions.assertEquals(zoneTree.size(), 8)
            Assertions.assertEquals(zoneThumb.size(), 8)

            Assertions.assertEquals(zoneTree.getPosition(3), Position(1, 8))
            Assertions.assertEquals(zoneThumb.getPosition(6), Position(6, 6))

            val zone3 = pathways.getClueByNumber("4")!!.zone

            Assertions.assertEquals(zone3.size(), 5)
            Assertions.assertEquals(zone3.getPosition(3), Position(3, 5))
        }

        @Throws(Exception::class)
        fun assertIsTestPuzzleAcrostic(puz : Puzzle) {
            Assertions.assertEquals(puz.kind, PuzzleKind.ACROSTIC)

            val words = puz.getClues("Clues")
            val quote = puz.getClues("Quote")

            val clueA = words!!.getClueByIndex(0)
            val clueC = words.getClueByIndex(2)

            Assertions.assertEquals(clueA!!.label, "A")
            Assertions.assertEquals(clueC!!.label, "C")

            val zoneA = clueA.zone
            Assertions.assertEquals(zoneA.getPosition(2), Position(0, 2))
            Assertions.assertEquals(zoneA.getPosition(3), Position(0, 4))

            val zoneQuote = quote!!.getClueByIndex(0)!!.zone
            Assertions.assertEquals(zoneQuote.size(), 13)
            Assertions.assertEquals(zoneQuote.getPosition(5), Position(1, 1))
        }

        @Throws(Exception::class)
        fun assertIsTestPuzzleShapes(puz : Puzzle) {
            val boxes : Array<Array<Box?>> = puz.boxes

            Assertions.assertEquals(boxes[0][0]!!.shape, Box.Shape.CIRCLE)
            Assertions.assertEquals(boxes[0][1]!!.shape, Box.Shape.ARROW_LEFT)
            Assertions.assertEquals(boxes[0][2]!!.shape, Box.Shape.ARROW_RIGHT)
            Assertions.assertEquals(boxes[0][3]!!.shape, Box.Shape.ARROW_UP)
            Assertions.assertEquals(boxes[0][4]!!.shape, Box.Shape.ARROW_DOWN)
            Assertions.assertEquals(
                boxes[0][5]!!.shape,
                Box.Shape.TRIANGLE_LEFT,
            )
            Assertions.assertEquals(
                boxes[0][6]!!.shape,
                Box.Shape.TRIANGLE_RIGHT,
            )
            Assertions.assertEquals(boxes[0][7]!!.shape, Box.Shape.TRIANGLE_UP)
            Assertions.assertEquals(
                boxes[0][8]!!.shape,
                Box.Shape.TRIANGLE_DOWN,
            )
            Assertions.assertEquals(boxes[0][9]!!.shape, Box.Shape.DIAMOND)

            Assertions.assertEquals(boxes[1][0]!!.shape, Box.Shape.CLUB)
            Assertions.assertEquals(boxes[1][1]!!.shape, Box.Shape.HEART)
            Assertions.assertEquals(boxes[1][2]!!.shape, Box.Shape.SPADE)
            Assertions.assertEquals(boxes[1][3]!!.shape, Box.Shape.STAR)
            Assertions.assertEquals(boxes[1][4]!!.shape, Box.Shape.SQUARE)
            Assertions.assertEquals(boxes[1][5]!!.shape, Box.Shape.RHOMBUS)
            Assertions.assertEquals(
                boxes[1][6]!!.shape,
                Box.Shape.FORWARD_SLASH,
            )
            Assertions.assertEquals(boxes[1][7]!!.shape, Box.Shape.BACK_SLASH)
        }

        @Throws(Exception::class)
        fun assertIsTestPuzzleExtensions(puz : Puzzle) {
            val acrossClues = puz.getClues("Across")
            val downClues = puz.getClues("Vertical")

            val oneAcross = acrossClues!!.getClueByNumber("1")
            Assertions.assertEquals(oneAcross!!.getSeparator(-1), null)
            Assertions.assertEquals(oneAcross.getSeparator(0), "\'")
            Assertions.assertEquals(oneAcross.getSeparator(1), " ")
            Assertions.assertEquals(oneAcross.getSeparator(2), null)

            val threeAcross = acrossClues.getClueByNumber("3")
            Assertions.assertEquals(threeAcross!!.getSeparator(-1), null)
            Assertions.assertEquals(threeAcross.getSeparator(0), null)
            Assertions.assertEquals(threeAcross.getSeparator(1), null)
            Assertions.assertEquals(threeAcross.getSeparator(2), " ")
            Assertions.assertEquals(threeAcross.getSeparator(3), null)

            val oneDown = downClues!!.getClueByNumber("1")
            Assertions.assertEquals(oneDown!!.getSeparator(-1), null)
            Assertions.assertEquals(oneDown.getSeparator(0), null)
            Assertions.assertEquals(oneDown.getSeparator(1), " ")
            Assertions.assertEquals(oneDown.getSeparator(2), null)
        }

        val testPuzzleHTMLInputStream : InputStream
            /**
             * Test HTML in various parts of puzzle
             */
            get() = JPZIOTest::class.java
                .getResourceAsStream("/html.ipuz")!!

        @Throws(Exception::class)
        fun assertIsTestPuzzleHTML(puz : Puzzle) {
            Assertions.assertEquals(
                puz.title,
                "<b>Test</b> &amp; puzzle<br>For testing",
            )
            Assertions.assertEquals(
                puz.author,
                "Test author<br><b>For<sup>Test</sup></b>",
            )
            Assertions.assertEquals(
                puz.source,
                "Test &nbsp;&nbsp;publisher<br>test<i>test</i>",
            )

            val acrossClues = puz.getClues("Across")

            Assertions.assertEquals(
                acrossClues!!.getClueByNumber("1")!!.hint,
                "Test <b>clue</b> 1<br>A clue&excl;",
            )
        }
    }
}

