
package app.crossword.yourealwaysbe.puz;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClueTest {

    @Test
    public void testNoEnumeration() {
        Clue clue = new Clue(
            "", 0, "1", "A clue", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertFalse(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertNull(clue.getSeparator(0, true));
        assertNull(clue.getSeparator(1));
        assertNull(clue.getSeparator(1, true));
        assertNull(clue.getSeparator(2));
        assertNull(clue.getSeparator(2, true));
    }

    @Test
    public void testOneWord() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (5)", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertFalse(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertNull(clue.getSeparator(0, true));
        assertNull(clue.getSeparator(4));
        assertNull(clue.getSeparator(4, true));
        assertNull(clue.getSeparator(5));
        assertNull(clue.getSeparator(5, true));
        assertNull(clue.getSeparator(6));
        assertNull(clue.getSeparator(6, true));
    }

    @Test
    public void testTwoWords() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (4, 5)", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertTrue(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertNull(clue.getSeparator(0, true));
        assertNull(clue.getSeparator(3));
        assertNull(clue.getSeparator(3, true));
        assertNull(clue.getSeparator(4));
        assertEquals(clue.getSeparator(4, true), " ");
        assertNull(clue.getSeparator(6));
        assertNull(clue.getSeparator(6, true));
    }

    @Test
    public void testTwoWordsCue() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (LONG=4, 5)", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertTrue(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertNull(clue.getSeparator(0, true));
        assertNull(clue.getSeparator(3));
        assertNull(clue.getSeparator(3, true));
        assertNull(clue.getSeparator(4));
        assertEquals(clue.getSeparator(4, true), " ");
        assertNull(clue.getSeparator(6));
        assertNull(clue.getSeparator(6, true));
    }

    @Test
    public void testTwoWordsCount() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (4,5;9)", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertTrue(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertNull(clue.getSeparator(0, true));
        assertNull(clue.getSeparator(3));
        assertNull(clue.getSeparator(3, true));
        assertNull(clue.getSeparator(4));
        assertEquals(clue.getSeparator(4, true), " ");
        assertNull(clue.getSeparator(6));
        assertNull(clue.getSeparator(6, true));
    }

    @Test
    public void testSpaceDash() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (3-4,5)", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertTrue(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertNull(clue.getSeparator(0, true));
        assertNull(clue.getSeparator(3));
        assertEquals(clue.getSeparator(3, true), "-");
        assertNull(clue.getSeparator(6));
        assertNull(clue.getSeparator(6, true));
        assertNull(clue.getSeparator(7));
        assertEquals(clue.getSeparator(7, true), " ");
    }

    @Test
    public void testSpecial() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (3*-4^,5)", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertTrue(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertNull(clue.getSeparator(0, true));
        assertNull(clue.getSeparator(3));
        assertEquals(clue.getSeparator(3, true), "-");
        assertNull(clue.getSeparator(6));
        assertNull(clue.getSeparator(6, true));
        assertNull(clue.getSeparator(7));
        assertEquals(clue.getSeparator(7, true), " ");
    }

    @Test
    public void testZero() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (-3)", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertTrue(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertEquals(clue.getSeparator(0, true), "-");
        assertNull(clue.getSeparator(1));
        assertNull(clue.getSeparator(1, true));
    }

    @Test
    public void testBackticks() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (3`*-`*4`^,5`;4)", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertTrue(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertNull(clue.getSeparator(0, true));
        assertNull(clue.getSeparator(3));
        assertEquals(clue.getSeparator(3, true), "*-*");
        assertNull(clue.getSeparator(6));
        assertNull(clue.getSeparator(6, true));
        assertNull(clue.getSeparator(7));
        assertEquals(clue.getSeparator(7, true), "^,");
        assertNull(clue.getSeparator(12));
        assertEquals(clue.getSeparator(12, true), ";");
    }

    @Test
    public void testProse() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (2 wds.)", new Zone()
        );
        assertFalse(clue.hasSeparators());
        assertFalse(clue.hasSeparators(true));
    }

    @Test
    public void testTwoWordsPlus() {
        Clue clue = new Clue(
            "", 0, "1", "A clue (4+5)", new Zone()
        );

        assertFalse(clue.hasSeparators());
        assertTrue(clue.hasSeparators(true));
        assertNull(clue.getSeparator(0));
        assertNull(clue.getSeparator(0, true));
        assertNull(clue.getSeparator(3));
        assertNull(clue.getSeparator(3, true));
        assertNull(clue.getSeparator(4));
        assertEquals(clue.getSeparator(4, true), " ");
        assertNull(clue.getSeparator(6));
        assertNull(clue.getSeparator(6, true));
    }
}

