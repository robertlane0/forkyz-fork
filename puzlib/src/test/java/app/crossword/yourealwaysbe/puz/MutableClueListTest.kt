
package app.crossword.yourealwaysbe.puz

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MutableClueListTest {
    @Test
    fun testEmptyList() {
        val cl: ClueList = MutableClueList()
        Assertions.assertNull(cl.getClueByIndex(1))
        Assertions.assertNull(cl.getClueByNumber("1"))
        Assertions.assertEquals(0, cl.getClues().size)
        Assertions.assertEquals(-1, cl.getFirstZonedIndex())
        Assertions.assertEquals(-1, cl.getLastZonedIndex())
        Assertions.assertEquals(-1, cl.getClueIndex("1"))
        Assertions.assertEquals(-1, cl.getNextZonedIndex(0, true))
        Assertions.assertEquals(-1, cl.getPreviousZonedIndex(1, true))
    }

    @Test
    fun testSingletonList() {
        val cl = MutableClueList()

        val clue1 = Clue("TestList", 0, "1", "The first clue", Zone())
        cl.addClue(clue1)

        Assertions.assertEquals(clue1, cl.getClueByIndex(0))
        Assertions.assertNull(cl.getClueByIndex(1))
        Assertions.assertEquals(clue1, cl.getClueByNumber("1"))
        Assertions.assertNull(cl.getClueByNumber("2"))
        Assertions.assertEquals(1, cl.getClues().size)
        Assertions.assertEquals(-1, cl.getFirstZonedIndex())
        Assertions.assertEquals(-1, cl.getLastZonedIndex())
        Assertions.assertEquals(0, cl.getClueIndex("1"))
        Assertions.assertEquals(-1, cl.getClueIndex("2"))
        Assertions.assertEquals(-1, cl.getNextZonedIndex(0, false))
        Assertions.assertEquals(-1, cl.getPreviousZonedIndex(0, false))
    }

    @Test
    fun testSingletonZonedList() {
        val cl = MutableClueList()

        val zone1 = Zone()
        zone1.addPosition(Position(0, 0))
        val clue1 = Clue("TestList", 0, "1", "The first clue", zone1)
        cl.addClue(clue1)

        Assertions.assertEquals(0, cl.getFirstZonedIndex())
        Assertions.assertEquals(0, cl.getLastZonedIndex())
        Assertions.assertEquals(-1, cl.getNextZonedIndex(0, false))
        Assertions.assertEquals(-1, cl.getPreviousZonedIndex(0, false))
        Assertions.assertEquals(0, cl.getNextZonedIndex(0, true))
        Assertions.assertEquals(0, cl.getPreviousZonedIndex(0, true))
    }


    @Test
    fun testMultiList() {
        val cl = MutableClueList()

        val clue1 = Clue("TestList", 0, "1", "The first clue", Zone())
        val clue2 = Clue("TestList", 1, "2", "The second clue", Zone())
        val clue3 = Clue("TestList", 2, "3", "The third clue", Zone())
        cl.addClue(clue1)
        cl.addClue(clue2)
        cl.addClue(clue3)

        Assertions.assertEquals(clue2, cl.getClueByIndex(1))
        Assertions.assertNull(cl.getClueByIndex(3))
        Assertions.assertEquals(clue1, cl.getClueByNumber("1"))
        Assertions.assertNull(cl.getClueByNumber("4"))
        Assertions.assertEquals(3, cl.getClues().size)
        Assertions.assertEquals(-1, cl.getFirstZonedIndex())
        Assertions.assertEquals(-1, cl.getLastZonedIndex())
        Assertions.assertEquals(1, cl.getClueIndex("2"))
        Assertions.assertEquals(-1, cl.getClueIndex("0"))
        Assertions.assertEquals(-1, cl.getNextZonedIndex(0, false))
        Assertions.assertEquals(-1, cl.getPreviousZonedIndex(3, false))
    }

    @Test
    fun testHasZone() {
        val cl = MutableClueList()

        val clue1 = Clue("TestList", 0, "1", "The first clue", Zone())
        val zone2 = Zone()
        zone2.addPosition(Position(0, 0))
        val clue2 = Clue("TestList", 1, "2", "The second clue", zone2)
        val clue3 = Clue("TestList", 2, "3", "The third clue", Zone())
        cl.addClue(clue1)
        cl.addClue(clue2)
        cl.addClue(clue3)

        Assertions.assertEquals(1, cl.getFirstZonedIndex())
        Assertions.assertEquals(1, cl.getLastZonedIndex())
        Assertions.assertEquals(1, cl.getNextZonedIndex(0, false))
        Assertions.assertEquals(-1, cl.getNextZonedIndex(1, false))
        Assertions.assertEquals(1, cl.getNextZonedIndex(1, true))
        Assertions.assertEquals(1, cl.getPreviousZonedIndex(2, false))
        Assertions.assertEquals(-1, cl.getPreviousZonedIndex(1, false))
        Assertions.assertEquals(1, cl.getPreviousZonedIndex(1, true))
    }

    @Test
    fun testTwoZones() {
        val cl = MutableClueList()

        val clue1 = Clue("TestList", 0, "1", "The first clue", Zone())
        val zone2 = Zone()
        zone2.addPosition(Position(0, 0))
        val clue2 = Clue("TestList", 1, "2", "The second clue", zone2)
        val clue3 = Clue("TestList", 2, "3", "The third clue", zone2)
        cl.addClue(clue1)
        cl.addClue(clue2)
        cl.addClue(clue3)

        Assertions.assertEquals(1, cl.getFirstZonedIndex())
        Assertions.assertEquals(2, cl.getLastZonedIndex())
        Assertions.assertEquals(1, cl.getNextZonedIndex(0, false))
        Assertions.assertEquals(2, cl.getNextZonedIndex(1, false))
        Assertions.assertEquals(1, cl.getPreviousZonedIndex(2, false))
        Assertions.assertEquals(-1, cl.getPreviousZonedIndex(1, false))
        Assertions.assertEquals(2, cl.getPreviousZonedIndex(1, true))
    }

    @Test
    fun testBadIndex() {
        var addedClue1 = false
        var addedClue2 = false
        try {
            val cl = MutableClueList()

            val clue1 = Clue("TestList", 0, "1", "The first clue", Zone())
            val clue2 = Clue("TestList", 2, "2", "The second clue", Zone())

            cl.addClue(clue1)
            addedClue1 = true
            cl.addClue(clue2)
            addedClue2 = true
        } catch (e: IllegalArgumentException) {
            // do nothing
        }
        Assertions.assertTrue(addedClue1)
        Assertions.assertFalse(addedClue2)
    }
}

