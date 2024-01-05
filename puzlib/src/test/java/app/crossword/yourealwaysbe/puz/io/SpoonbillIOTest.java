
package app.crossword.yourealwaysbe.puz.io;

import java.io.InputStream;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.Zone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpoonbillIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return SpoonbillIOTest
            .class
            .getResourceAsStream("/spoonbill.txt");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test title");
        assertEquals(puz.getAuthor(), "Test author");

        assertEquals(puz.getWidth(), 15);
        assertEquals(puz.getHeight(), 15);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), "1");
        assertEquals(boxes[2][8].getClueNumber(), "11");
        assertTrue(Box.isBlock(boxes[0][5]));
        assertTrue(Box.isBlock(boxes[5][2]));

        assertEquals(boxes[0][0].getSolution(), "A");
        assertEquals(boxes[1][7].getSolution(), "B");

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(acrossClues.getClueByNumber("1").getHint(), "Test clue 1a");
        assertEquals(acrossClues.getClueByNumber("19").getHint(), "Test clue 19a");
        assertEquals(downClues.getClueByNumber("2").getHint(), "Test clue 2d");
        assertEquals(downClues.getClueByNumber("14").getHint(), "Test clue 14d");

        Zone zone5a = acrossClues.getClueByNumber("5").getZone();
        assertEquals(zone5a.size(), 7);
        assertEquals(zone5a.getPosition(0), new Position(0, 8));
        assertEquals(zone5a.getPosition(6), new Position(0, 14));

        Zone zone14d = downClues.getClueByNumber("14").getZone();
        assertEquals(zone14d.size(), 11);
        assertEquals(zone14d.getPosition(0), new Position(4, 8));
        assertEquals(zone14d.getPosition(10), new Position(14, 8));
    }

    @Test
    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = SpoonbillIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

