
package app.crossword.yourealwaysbe.puz.io;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.puz.util.HtmlUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExolveIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return ExolveIOTest.class.getResourceAsStream("/test.exolve");
    }

    public static InputStream getTestPuzzleRebusInputStream() {
        return ExolveIOTest.class.getResourceAsStream("/rebus.exolve");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test title");
        assertEquals(puz.getAuthor(), "Test author");
        assertEquals(puz.getCopyright(), "Test copyright");
        assertEquals(
            puz.getIntroMessage(),
            "Test preamble<br/>Multiple lines<br/><br/>Test prelude"
        );
        assertEquals(
            puz.getNotes(),
            "test@email.com<br/><br/>Tester: test<br/>Tester 2: test2"
        );
        assertEquals(
            puz.getCompletionMessage(),
            "Everything is explained."
            + "<h2>Across</h2>"
            + "<p>1. ANNOT1a</p>"
            + "<p>6. ANNOT6a</p>"
            + "<h2>Down</h2>"
            + "<p>3. ANNOT3d</p>"
        );

        assertEquals(puz.getWidth(), 5);
        assertEquals(puz.getHeight(), 6);

        assertTrue(Box.isBlock(puz.checkedGetBox(1, 1)));
        assertTrue(Box.isBlock(puz.checkedGetBox(3, 3)));
        assertEquals(puz.checkedGetBox(0, 2).getSolution(), "A");
        assertEquals(puz.checkedGetBox(2, 4).getSolution(), "B");
        assertFalse(puz.checkedGetBox(2, 4).hasShape());
        assertEquals(puz.checkedGetBox(0, 1).getShape(), Box.Shape.CIRCLE);
        assertFalse(puz.checkedGetBox(2, 4).hasBars());
        assertEquals(puz.checkedGetBox(1, 0).getBarBottom(), Box.Bar.SOLID);
        assertEquals(puz.checkedGetBox(1, 0).getBarRight(), Box.Bar.NONE);
        assertEquals(puz.checkedGetBox(1, 2).getBarBottom(), Box.Bar.NONE);
        assertEquals(puz.checkedGetBox(1, 2).getBarRight(), Box.Bar.SOLID);
        assertEquals(puz.checkedGetBox(1, 4).getBarBottom(), Box.Bar.SOLID);
        assertEquals(puz.checkedGetBox(1, 4).getBarRight(), Box.Bar.SOLID);
        assertEquals(puz.checkedGetBox(2, 1).getBarBottom(), Box.Bar.SOLID);
        assertEquals(puz.checkedGetBox(3, 2).getInitialValue(), "C");
        assertFalse(puz.checkedGetBox(3, 0).hasInitialValue());
        assertEquals(puz.checkedGetBox(0, 4).getSolution(), "&");
        assertEquals(puz.checkedGetBox(4, 1).getSolution(), "1");
        assertEquals(puz.checkedGetBox(4, 2).getSolution(), "2");

        assertEquals(puz.checkedGetBox(0, 0).getClueNumber(), "1");
        assertEquals(puz.checkedGetBox(2, 0).getClueNumber(), "N");
        assertEquals(puz.checkedGetBox(4, 4).getClueNumber(), "6");

        int green = HtmlUtil.parseHtmlColor("green");
        int red = HtmlUtil.parseHtmlColor("red");
        assertEquals(puz.checkedGetBox(0, 1).getColor(), green);
        assertEquals(puz.checkedGetBox(0, 4).getColor(), green);
        assertEquals(puz.checkedGetBox(4, 1).getColor(), green);
        assertEquals(puz.checkedGetBox(4, 3).getColor(), green);
        assertEquals(puz.checkedGetBox(0, 2).getColor(), red);
        assertEquals(puz.checkedGetBox(3, 2).getColor(), red);
        assertEquals(puz.checkedGetBox(2, 1).getColor(), red);

        Clue clue1a = puz.getClues("Across").getClueByNumber("1");
        Zone zone1a = clue1a.getZone();
        assertEquals(clue1a.getHint(), "Clue 1a (5)");
        assertEquals(zone1a.size(), 5);
        assertEquals(zone1a.getPosition(0), new Position(0, 0));
        assertEquals(zone1a.getPosition(4), new Position(0, 4));

        Clue clueNa = puz.getClues("Across").getClueByNumber("N");
        Zone zoneNa = clueNa.getZone();
        assertEquals(clueNa.getHint(), "Clue Na (5)");
        assertEquals(zoneNa.size(), 2);
        assertEquals(zoneNa.getPosition(0), new Position(2, 0));
        assertEquals(zoneNa.getPosition(1), new Position(2, 1));

        Clue clue6a = puz.getClues("Across").getClueByNumber("6");
        Zone zone6a = clue6a.getZone();
        assertEquals(zone6a.size(), 5);
        assertEquals(zone6a.getPosition(0), new Position(4, 4));
        assertEquals(zone6a.getPosition(1), new Position(4, 3));
        assertEquals(zone6a.getPosition(4), new Position(4, 0));

        Clue clueFiller = puz.getClues("Across").getClueByIndex(1);
        assertFalse(clueFiller.hasClueNumber());
        assertEquals(clueFiller.getHint(), "All filler");

        Clue clue2d = puz.getClues("Down").getClueByNumber("2");
        Zone zone2d = clue2d.getZone();
        assertEquals(clue2d.getHint(), "Clue 2d");
        assertEquals(zone2d.size(), 5);
        assertEquals(zone2d.getPosition(0), new Position(0, 2));
        assertEquals(zone2d.getPosition(4), new Position(4, 2));

        Clue clue3d = puz.getClues("Down").getClueByNumber("3");
        Zone zone3d = clue3d.getZone();
        assertEquals(clue3d.getHint(), ", 4a & [N] Clue <u>3d</u>");
        assertEquals(zone3d.size(), 7);
        assertEquals(zone3d.getPosition(0), new Position(0, 4));
        assertEquals(zone3d.getPosition(2), new Position(2, 2));
        assertEquals(zone3d.getPosition(5), new Position(2, 0));
        assertEquals(zone3d.getPosition(6), new Position(2, 1));

        Clue clue6d = puz.getClues("Down").getClueByNumber("6");
        Zone zone6d = clue6d.getZone();
        assertEquals(zone6d.size(), 2);
        assertEquals(zone6d.getPosition(0), new Position(4, 4));
        assertEquals(zone6d.getPosition(1), new Position(5, 4));


        Clue clueA = puz.getClues("NODIR").getClueByNumber("A");
        Zone zoneA = clueA.getZone();
        assertEquals(clueA.getHint(), "ClueA");
        assertEquals(zoneA.size(), 3);
        assertEquals(zoneA.getPosition(0), new Position(0, 0));
        assertEquals(zoneA.getPosition(1), new Position(1, 2));
        assertEquals(zoneA.getPosition(2), new Position(4, 3));

        assertNull(puz.getClues("Across").getClueByNumber("8"));
    }

    public static void assertIsTestPuzzleRebus(Puzzle puz) throws Exception {
        assertEquals(puz.getWidth(), 3);
        assertEquals(puz.getHeight(), 2);
        assertEquals(puz.checkedGetBox(0, 0).getSolution(), "ONE");
        assertEquals(puz.checkedGetBox(0, 1).getSolution(), Box.BLANK);
        assertEquals(puz.checkedGetBox(0, 2).getSolution(), "TWO");
        assertEquals(puz.checkedGetBox(1, 0).getSolution(), Box.BLANK);
        assertEquals(puz.checkedGetBox(1, 1).getSolution(), "THREE");
        assertEquals(puz.checkedGetBox(1, 2).getSolution(), Box.BLANK);
        assertFalse(puz.checkedGetBox(0, 0).hasClueNumber());
        assertFalse(puz.checkedGetBox(0, 1).hasClueNumber());
        assertFalse(puz.checkedGetBox(0, 2).hasClueNumber());
        assertFalse(puz.checkedGetBox(1, 0).hasClueNumber());
        assertFalse(puz.checkedGetBox(1, 1).hasClueNumber());
        assertFalse(puz.checkedGetBox(1, 2).hasClueNumber());
    }

    @Test
    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = ExolveIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }

    @Test
    public void testPuzzleRebus() throws Exception {
        try (InputStream is = getTestPuzzleRebusInputStream()) {
            Puzzle puz = ExolveIO.readPuzzle(is);
            assertIsTestPuzzleRebus(puz);
        }
    }
}

