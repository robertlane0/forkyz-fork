
package app.crossword.yourealwaysbe.forkyz.puz.io

import kotlinx.coroutines.runBlocking

import org.junit.jupiter.api.Test

class PuzzleStreamReaderTest {
    @Test
    fun testAcrossLite() {
        IOTest.testPuzzle1.use { input ->
            IOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testJPZ() {
        JPZIOTest.testPuzzle1.use { input ->
            JPZIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testBrainsOnly() {
        BrainsOnlyIOTest.testPuzzle1.use { input ->
            BrainsOnlyIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testKingFeaturesPlaintext() {
        KingFeaturesPlaintextIOTest.testPuzzle1.use { input ->
            KingFeaturesPlaintextIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testUclick() {
        UclickXMLIOTest.testPuzzle1.use { input ->
            UclickXMLIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testIPuz() {
        IPuzIOTest.testPuzzle1.use { input ->
            IPuzIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testRaetzelZentraleSchweden() {
        RaetselZentraleSchwedenJSONIOTest.testPuzzle1.let { input ->
            RaetselZentraleSchwedenJSONIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testRCIJeuxMFJ() {
        RCIJeuxMFJIOTest.testPuzzle1.let { input ->
            RCIJeuxMFJIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testPrzekroj() {
        PrzekrojIOTest.testPuzzle1.use { input ->
            PrzekrojIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testKeesingXML() {
        KeesingXMLIOTest.testPuzzle1.let { input ->
            KeesingXMLIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }

    @Test
    fun testPML() {
        PMLIOTest.testPuzzle1.let { input ->
            PMLIOTest.assertIsTestPuzzle1(
                runBlocking { PuzzleStreamReader.parseInputStatic(input)!! },
            )
        }
    }
}

