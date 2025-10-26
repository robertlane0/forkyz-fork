
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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RCIJeuxMCJIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return JPZIOTest.class.getResourceAsStream("/rcijeux-crossword.mcj");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test Crossword");

        assertEquals(puz.getWidth(), 12);
        assertEquals(puz.getHeight(), 12);

        Box[][] boxes = puz.getBoxes();

        assertTrue(Box.isBlock(boxes[0][4]));
        assertTrue(Box.isBlock(boxes[3][0]));
        assertEquals(boxes[0][2].getSolution(), "B");
        assertEquals(boxes[1][2].getSolution(), "C");
        assertEquals(boxes[4][7].getSolution(), "A");

        ClueList acrossClues = puz.getClues("Horiz.");
        ClueList downClues = puz.getClues("Vert.");

        Clue a1 = acrossClues.getClueByNumber("1");
        assertEquals(a1.getHint(), "Test 1");
        Zone zone1 = a1.getZone();
        assertEquals(zone1.size(), 4);
        assertEquals(zone1.getPosition(0), new Position(0, 0));
        assertEquals(zone1.getPosition(3), new Position(0, 3));

        Clue a19 = acrossClues.getClueByNumber("19");
        assertEquals(a19.getHint(), "Test B");
        Zone zone19 = a19.getZone();
        assertEquals(zone19.size(), 2);
        assertEquals(zone19.getPosition(0), new Position(4, 4));
        assertEquals(zone19.getPosition(1), new Position(4, 5));

        Clue d1 = downClues.getClueByNumber("1");
        assertEquals(d1.getHint(), "Test D");
        Zone zone1d = d1.getZone();
        assertEquals(zone1d.size(), 3);
        assertEquals(zone1d.getPosition(0), new Position(0, 0));
        assertEquals(zone1d.getPosition(1), new Position(1, 0));

        Clue d41 = downClues.getClueByNumber("41");
        assertEquals(d41.getHint(), "Test Z");
        Zone zone41 = d41.getZone();
        assertEquals(zone41.size(), 2);
        assertEquals(zone41.getPosition(0), new Position(10, 3));
        assertEquals(zone41.getPosition(1), new Position(11, 3));
    }

    @Test
    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = RCIJeuxMCJIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

