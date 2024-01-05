
package app.crossword.yourealwaysbe.puz.io;

import java.io.InputStream;
import java.time.LocalDate;

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

public class XWordJSONIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return XWordJSONIOTest.class.getResourceAsStream("/xword.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test title");
        assertEquals(puz.getAuthor(), "Test author");
        assertEquals(puz.getNotes(), "Test notes");
        assertEquals(puz.getSource(), "Test publisher");
        assertEquals(puz.getDate(), LocalDate.of(2024, 1, 5));

        assertEquals(puz.getWidth(), 15);
        assertEquals(puz.getHeight(), 15);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), "1");
        assertEquals(boxes[1][0].getClueNumber(), "13");
        assertTrue(Box.isBlock(boxes[0][6]));

        assertEquals(boxes[0][0].getSolution(), "A");
        assertEquals(boxes[1][6].getSolution(), "B");

        assertEquals(boxes[0][0].getShape(), Box.Shape.CIRCLE);
        assertFalse(boxes[0][1].hasShape());
        assertEquals(boxes[0][0].getBarRight(), Box.Bar.NONE);
        assertEquals(boxes[0][0].getBarBottom(), Box.Bar.NONE);
        assertEquals(boxes[0][1].getBarRight(), Box.Bar.SOLID);
        assertEquals(boxes[0][1].getBarBottom(), Box.Bar.NONE);

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(acrossClues.getClueByNumber("1").getHint(), "Clue 1a");
        assertEquals(acrossClues.getClueByNumber("18").getHint(), "Clue 18a");
        assertEquals(downClues.getClueByNumber("1").getHint(), "Clue 1d");
        assertEquals(downClues.getClueByNumber("17").getHint(), "Clue 17d");

        Clue clue1a = acrossClues.getClueByNumber("1");
        Zone zone1a = clue1a.getZone();
        assertEquals(zone1a.size(), 2);
        assertEquals(zone1a.getPosition(0), new Position(0, 0));
        assertEquals(zone1a.getPosition(1), new Position(0, 1));

        Clue clue1d = downClues.getClueByNumber("1");
        Zone zone1d = clue1d.getZone();
        assertEquals(zone1d.size(), 6);
        assertEquals(zone1d.getPosition(0), new Position(0, 0));
        assertEquals(zone1d.getPosition(5), new Position(5, 0));
    }

    @Test
    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = XWordJSONIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

