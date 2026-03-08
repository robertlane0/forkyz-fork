
package app.crossword.yourealwaysbe.puz.io;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class XDIOTest {

    public static InputStream getTestPuzzleImplicitInputStream() {
        return XDIOTest.class.getResourceAsStream("/implicitsections.xd");
    }

    public static InputStream getTestPuzzleExplicitInputStream() {
        return XDIOTest.class.getResourceAsStream("/explicitsections.xd");
    }

    /**
     * Checks if puzzle is as expected
     *
     * @param specialCircled if special squares are circled or
     * highlighted
     */
    public static void assertIsTestPuzzle(
        Puzzle puz, boolean specialCircled, boolean simpleNotes
    ) throws Exception {
        assertEquals(puz.getWidth(), 15);
        assertEquals(puz.getHeight(), 15);

        assertEquals(puz.getTitle(), "Test <b>title</b>");
        assertEquals(puz.getAuthor(), "Test <i>author</i>");
        assertEquals(
                puz.getCopyright(),
            "This is <u>the</u> <strike>copyright</strike>"
        );

        if (simpleNotes) {
            assertEquals(
                    puz.getNotes(), "Test notes<br/><br/>Test line more notes"
            );
        } else {
            assertEquals(
                    puz.getNotes(),
                "<p>description</p><p>notes</p><p>Free form<br/><br/>notes</p>"
            );
        }

        Box[][] boxes = puz.getBoxes();

        assertTrue(Box.isBlock(boxes[0][5]));
        assertEquals(boxes[0][0].getClueNumber(), "1");
        assertTrue(Box.isBlock(boxes[2][5]));
        assertEquals(boxes[0][11].getClueNumber(), "10");

        assertEquals(boxes[0][0].getSolution(), "ONE");
        assertEquals(boxes[0][3].getSolution(), "A");
        assertEquals(boxes[4][5].getSolution(), "B");

        if (specialCircled) {
            assertTrue(boxes[0][7].hasShape());
            assertFalse(boxes[0][7].hasColor());
        } else {
            assertTrue(boxes[0][7].hasColor());
            assertFalse(boxes[0][7].hasShape());
        }
        assertEquals(boxes[0][7].getSolution(), "X");
        assertFalse(boxes[0][8].hasColor());
        assertFalse(boxes[0][8].hasShape());

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");
        ClueList anonClues = puz.getClues("Clues");
        ClueList extraClues = puz.getClues("Extra");

        assertEquals(acrossClues.getClueByNumber("1").getHint(), "Clue A1");
        assertEquals(
            acrossClues.getClueByNumber("10").getHint(),
            "Clue A10"
        );
        assertEquals(downClues.getClueByNumber("1").getHint(), "Clue D1");
        assertEquals(downClues.getClueByNumber("2").getHint(), "Clue D2");

        assertEquals(anonClues.size(), 2);
        assertEquals(extraClues.size(), 1);
        assertEquals(anonClues.getClueByIndex(0).getClueNumber(), "YZ");
        assertEquals(anonClues.getClueByIndex(0).getHint(), "Anon clue 1");
        assertEquals(extraClues.getClueByIndex(0).getClueNumber(), "1");
        assertEquals(extraClues.getClueByIndex(0).getHint(), "Extra clue");

        Clue clue = acrossClues.getClueByNumber("1");
        Zone clueZone = clue.getZone();
        assertEquals(clueZone.size(), 5);
        assertEquals(clueZone.getPosition(0), new Position(0, 0));
        assertEquals(clueZone.getPosition(4), new Position(0, 4));

        clue = downClues.getClueByNumber("1");
        clueZone = clue.getZone();
        assertEquals(clueZone.size(), 5);
        assertEquals(clueZone.getPosition(0), new Position(0, 0));
        assertEquals(clueZone.getPosition(4), new Position(4, 0));
    }

    @Test
    public void testPuzzleImplicit() throws Exception {
        try (InputStream is = getTestPuzzleImplicitInputStream()) {
            Puzzle puz = XDIO.readPuzzle(is);
            assertIsTestPuzzle(puz, true, false);
        }
    }

    @Test
    public void testPuzzleExplicit() throws Exception {
        try (InputStream is = getTestPuzzleExplicitInputStream()) {
            Puzzle puz = XDIO.readPuzzle(is);
            assertIsTestPuzzle(puz, false, true);
        }
    }
}

