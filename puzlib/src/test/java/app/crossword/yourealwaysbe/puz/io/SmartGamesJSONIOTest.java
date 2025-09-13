
package app.crossword.yourealwaysbe.puz.io;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmartGamesJSONIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return SmartGamesJSONIO.class.getResourceAsStream("/smartgames.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        assertEquals(puz.getTitle(), null);
        assertEquals(puz.getAuthor(), "Test Author");
        assertEquals(puz.getNotes(), null);
        assertEquals(puz.getDate(), LocalDate.of(2025, 9, 10));

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isStartOf(new ClueID("Across", 0)));
        assertEquals(true, boxes[0][1].isStartOf(new ClueID("Down", 0)));
        assertEquals(false, boxes[0][3].isStartOf(new ClueID("Across", 1)));
        assertEquals(false, boxes[0][8].isStartOf(new ClueID("Across", 2)));

        assertEquals(boxes[0][0].getSolution(), "F");
        assertEquals(boxes[5][13].getSolution(), "E");
        assertEquals(boxes[14][12].getSolution(), "E");
        assertEquals(boxes[14][4].getSolution(), "R");
        assertTrue(Box.isBlock(boxes[1][7]));

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        Clue a1 = acrossClues.getClueByNumber("1");
        Zone a1zone = new Zone();
        a1zone.addPosition(new Position(0, 0));
        a1zone.addPosition(new Position(0, 1));
        a1zone.addPosition(new Position(0, 2));
        a1zone.addPosition(new Position(0, 3));
        a1zone.addPosition(new Position(0, 4));
        a1zone.addPosition(new Position(0, 5));
        a1zone.addPosition(new Position(0, 6));
        assertEquals(a1.getHint(), "Test clue 1");
        assertEquals(a1.getZone(), a1zone);

        Clue a19 = acrossClues.getClueByNumber("19");
        Zone a19zone = new Zone();
        a19zone.addPosition(new Position(2, 13));
        a19zone.addPosition(new Position(2, 14));
        assertEquals(a19.getHint(), "Test clue 19");
        assertEquals(a19.getZone(), a19zone);

        Clue a27 = acrossClues.getClueByNumber("27");
        Zone a27zone = new Zone();
        a27zone.addPosition(new Position(4, 10));
        a27zone.addPosition(new Position(4, 11));
        a27zone.addPosition(new Position(4, 12));
        a27zone.addPosition(new Position(4, 13));
        a27zone.addPosition(new Position(4, 14));
        assertEquals(a27.getHint(), "Test clue 27");
        assertEquals(a27.getZone(), a27zone);

        Clue d28 = downClues.getClueByNumber("28");
        Zone d28zone = new Zone();
        d28zone.addPosition(new Position(4, 13));
        d28zone.addPosition(new Position(5, 13));
        d28zone.addPosition(new Position(6, 13));
        assertEquals(d28.getHint(), "Test clue 28");
        assertEquals(d28.getZone(), d28zone);
    }

    @Test
    public void testRead() throws IOException {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = SmartGamesJSONIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}
