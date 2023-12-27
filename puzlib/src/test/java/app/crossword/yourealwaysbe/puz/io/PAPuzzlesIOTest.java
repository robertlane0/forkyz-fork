
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

public class PAPuzzlesIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return PAPuzzlesIOTest.class.getResourceAsStream("/papuzzles.html");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getWidth(), 13);
        assertEquals(puz.getHeight(), 13);

        Box[][] boxes = puz.getBoxes();

        assertTrue(Box.isBlock(boxes[0][0]));
        assertEquals(boxes[0][5].getClueNumber(), "3");
        assertTrue(Box.isBlock(boxes[1][5]));
        assertEquals(boxes[9][10].getClueNumber(), "33");

        assertEquals(boxes[5][0].getSolution(), "Y");
        assertEquals(boxes[7][5].getSolution(), "Z");

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(acrossClues.getClueByNumber("8").getHint(), "AClue 8");
        assertEquals(
            acrossClues.getClueByNumber("10").getHint(),
            "AClue 10"
        );
        assertEquals(downClues.getClueByNumber("1").getHint(), "DClue 1");
        assertEquals(downClues.getClueByNumber("2").getHint(), "DClue 2");

        Clue clue = acrossClues.getClueByNumber("8");
        Zone clueZone = clue.getZone();
        assertEquals(clueZone.getPosition(0), new Position(1, 0));
        assertEquals(clueZone.getPosition(4), new Position(1, 4));
    }

    @Test
    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = PAPuzzlesIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

