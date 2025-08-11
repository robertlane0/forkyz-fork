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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TCAJSONIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return TCAJSONIO.class.getResourceAsStream("/tca.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        assertEquals(puz.getTitle(), "Test Crossword");
        assertEquals(puz.getAuthor(), "Test Author");
        assertEquals(puz.getNotes(), "Test Notes");
        assertEquals(puz.getDate(), LocalDate.of(2025, 07, 27));

        Box[][] boxes = puz.getBoxes();

        assertEquals(21, boxes.length);
        assertEquals(21, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertEquals(true, boxes[0][0].isStartOf(new ClueID("Across", 0)));
        assertEquals(true, boxes[0][0].isStartOf(new ClueID("Down", 0)));
        assertEquals(false, boxes[0][3].isStartOf(new ClueID("Across", 1)));

        assertEquals(boxes[0][0].getSolution(), "F");
        assertEquals(boxes[5][13].getSolution(), "E");
        assertEquals(boxes[14][12].getSolution(), "E");
        assertEquals(boxes[14][4].getSolution(), "R");
        assertTrue(Box.isBlock(boxes[1][5]));

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        Clue a1 = acrossClues.getClueByNumber("1");
        Zone a1zone = new Zone();
        a1zone.addPosition(new Position(0, 0));
        a1zone.addPosition(new Position(0, 1));
        a1zone.addPosition(new Position(0, 2));
        a1zone.addPosition(new Position(0, 3));
        a1zone.addPosition(new Position(0, 4));
        assertEquals(a1.getHint(), "Test clue 1");
        assertEquals(a1.getZone(), a1zone);

        Clue a10 = acrossClues.getClueByNumber("10");
        Zone a10zone = new Zone();
        a10zone.addPosition(new Position(0, 12));
        a10zone.addPosition(new Position(0, 13));
        a10zone.addPosition(new Position(0, 14));
        assertEquals(a10.getHint(), "Test clue 10");
        assertEquals(a10.getZone(), a10zone);

        Clue a27 = acrossClues.getClueByNumber("27");
        Zone a27zone = new Zone();
        a27zone.addPosition(new Position(3, 10));
        a27zone.addPosition(new Position(3, 11));
        a27zone.addPosition(new Position(3, 12));
        assertEquals(a27.getHint(), "Test clue 27");
        assertEquals(a27.getZone(), a27zone);

        Clue d4 = downClues.getClueByNumber("4");
        Zone d4zone = new Zone();
        d4zone.addPosition(new Position(0, 3));
        d4zone.addPosition(new Position(1, 3));
        d4zone.addPosition(new Position(2, 3));
        d4zone.addPosition(new Position(3, 3));
        assertEquals(d4.getHint(), "Test clue 4d");
        assertEquals(d4.getZone(), d4zone);
    }

    @Test
    public void testRead() throws IOException {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = TCAJSONIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}
