
package app.crossword.yourealwaysbe.puz.io

import org.junit.jupiter.api.Test

class PuzzleStreamReaderTest {
    @Test
    fun testAcrossLite() {
        IOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                IOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    fun testJPZ() {
        JPZIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                JPZIOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    fun testBrainsOnly() {
        BrainsOnlyIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                BrainsOnlyIOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    fun testKingFeaturesPlaintext() {
        KingFeaturesPlaintextIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                KingFeaturesPlaintextIOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    fun testUclick() {
        UclickXMLIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                UclickXMLIOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    @Throws(Exception::class)
    fun testIPuz() {
        IPuzIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                IPuzIOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    @Throws(Exception::class)
    fun testRaetzelZentraleSchweden() {
        RaetselZentraleSchwedenJSONIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                RaetselZentraleSchwedenJSONIOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    @Throws(Exception::class)
    fun testRCIJeuxMFJ() {
        RCIJeuxMFJIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                RCIJeuxMFJIOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    @Throws(Exception::class)
    fun testPrzekroj() {
        PrzekrojIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                PrzekrojIOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    @Throws(Exception::class)
    fun testKeesingXML() {
        KeesingXMLIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                KeesingXMLIOTest.testPuzzle1InputStream,
            )!!
        )
    }

    @Test
    @Throws(Exception::class)
    fun testPML() {
        PMLIOTest.assertIsTestPuzzle1(
            PuzzleStreamReader.parseInputStatic(
                PMLIOTest.testPuzzle1InputStream,
            )!!
        )
    }
}

