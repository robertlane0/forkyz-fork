
package app.crossword.yourealwaysbe.puz

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ClueTest {
    @Test
    fun testNoEnumeration() {
        val clue = Clue("", 0, "1", "A clue", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertFalse(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertNull(clue.getSeparator(0, true))
        Assertions.assertNull(clue.getSeparator(1))
        Assertions.assertNull(clue.getSeparator(1, true))
        Assertions.assertNull(clue.getSeparator(2))
        Assertions.assertNull(clue.getSeparator(2, true))
    }

    @Test
    fun testOneWord() {
        val clue = Clue("", 0, "1", "A clue (5)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertFalse(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertNull(clue.getSeparator(0, true))
        Assertions.assertNull(clue.getSeparator(4))
        Assertions.assertNull(clue.getSeparator(4, true))
        Assertions.assertNull(clue.getSeparator(5))
        Assertions.assertNull(clue.getSeparator(5, true))
        Assertions.assertNull(clue.getSeparator(6))
        Assertions.assertNull(clue.getSeparator(6, true))
    }

    @Test
    fun testTwoWords() {
        val clue = Clue("", 0, "1", "A clue (4, 5)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertNull(clue.getSeparator(0, true))
        Assertions.assertNull(clue.getSeparator(3))
        Assertions.assertNull(clue.getSeparator(3, true))
        Assertions.assertNull(clue.getSeparator(4))
        Assertions.assertEquals(clue.getSeparator(4, true), " ")
        Assertions.assertNull(clue.getSeparator(6))
        Assertions.assertNull(clue.getSeparator(6, true))
    }

    @Test
    fun testTwoWordsCue() {
        val clue = Clue("", 0, "1", "A clue (LONG=4, 5)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertNull(clue.getSeparator(0, true))
        Assertions.assertNull(clue.getSeparator(3))
        Assertions.assertNull(clue.getSeparator(3, true))
        Assertions.assertNull(clue.getSeparator(4))
        Assertions.assertEquals(clue.getSeparator(4, true), " ")
        Assertions.assertNull(clue.getSeparator(6))
        Assertions.assertNull(clue.getSeparator(6, true))
    }

    @Test
    fun testTwoWordsCount() {
        val clue = Clue("", 0, "1", "A clue (4,5;9)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertNull(clue.getSeparator(0, true))
        Assertions.assertNull(clue.getSeparator(3))
        Assertions.assertNull(clue.getSeparator(3, true))
        Assertions.assertNull(clue.getSeparator(4))
        Assertions.assertEquals(clue.getSeparator(4, true), " ")
        Assertions.assertNull(clue.getSeparator(6))
        Assertions.assertNull(clue.getSeparator(6, true))
    }

    @Test
    fun testSpaceDash() {
        val clue = Clue("", 0, "1", "A clue (3-4,5)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertNull(clue.getSeparator(0, true))
        Assertions.assertNull(clue.getSeparator(3))
        Assertions.assertEquals(clue.getSeparator(3, true), "-")
        Assertions.assertNull(clue.getSeparator(6))
        Assertions.assertNull(clue.getSeparator(6, true))
        Assertions.assertNull(clue.getSeparator(7))
        Assertions.assertEquals(clue.getSeparator(7, true), " ")
    }

    @Test
    fun testSpecial() {
        val clue = Clue("", 0, "1", "A clue (3*-4^,5)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertNull(clue.getSeparator(0, true))
        Assertions.assertNull(clue.getSeparator(3))
        Assertions.assertEquals(clue.getSeparator(3, true), "-")
        Assertions.assertNull(clue.getSeparator(6))
        Assertions.assertNull(clue.getSeparator(6, true))
        Assertions.assertNull(clue.getSeparator(7))
        Assertions.assertEquals(clue.getSeparator(7, true), " ")
    }

    @Test
    fun testZero() {
        val clue = Clue("", 0, "1", "A clue (-3)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertEquals(clue.getSeparator(0, true), "-")
        Assertions.assertNull(clue.getSeparator(1))
        Assertions.assertNull(clue.getSeparator(1, true))
    }

    @Test
    fun testBackticks() {
        val clue = Clue("", 0, "1", "A clue (3`*-`*4`^,5`;4)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertNull(clue.getSeparator(0, true))
        Assertions.assertNull(clue.getSeparator(3))
        Assertions.assertEquals(clue.getSeparator(3, true), "*-*")
        Assertions.assertNull(clue.getSeparator(6))
        Assertions.assertNull(clue.getSeparator(6, true))
        Assertions.assertNull(clue.getSeparator(7))
        Assertions.assertEquals(clue.getSeparator(7, true), "^,")
        Assertions.assertNull(clue.getSeparator(12))
        Assertions.assertEquals(clue.getSeparator(12, true), ";")
    }

    @Test
    fun testProse() {
        val clue = Clue("", 0, "1", "A clue (2 wds.)", Zone())
        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertFalse(clue.hasSeparators(true))
    }

    @Test
    fun testThreeWordsNoSpaces() {
        val clue = Clue("", 0, "1", "A clue (2,1,4)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(2))
        Assertions.assertEquals(clue.getSeparator(2, true), " ")
        Assertions.assertNull(clue.getSeparator(3))
        Assertions.assertEquals(clue.getSeparator(3, true), " ")
    }

    @Test
    fun testThreeWordsTrailingSpace() {
        // clues from some sources have a trailing space
        val clue = Clue("", 0, "1", "A clue (2,1,4) ", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(2))
        Assertions.assertEquals(clue.getSeparator(2, true), " ")
        Assertions.assertNull(clue.getSeparator(3))
        Assertions.assertEquals(clue.getSeparator(3, true), " ")
    }

    @Test
    fun testHtmlWrapped() {
        // clues from some sources wrap hints in HTML spans
        val clue = Clue(
            "",
            0,
            "1",
            "<span>I begin to grasp one conceptualist question (2,2,3)</span>",
            Zone()
        )

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(2))
        Assertions.assertEquals(clue.getSeparator(2, true), " ")
        Assertions.assertNull(clue.getSeparator(4))
        Assertions.assertEquals(clue.getSeparator(4, true), " ")
    }

    @Test
    fun testTwoWordsPlus() {
        val clue = Clue("", 0, "1", "A clue (4+5)", Zone())

        Assertions.assertFalse(clue.hasSeparators())
        Assertions.assertTrue(clue.hasSeparators(true))
        Assertions.assertNull(clue.getSeparator(0))
        Assertions.assertNull(clue.getSeparator(0, true))
        Assertions.assertNull(clue.getSeparator(3))
        Assertions.assertNull(clue.getSeparator(3, true))
        Assertions.assertNull(clue.getSeparator(4))
        Assertions.assertEquals(clue.getSeparator(4, true), " ")
        Assertions.assertNull(clue.getSeparator(6))
        Assertions.assertNull(clue.getSeparator(6, true))
    }
}

