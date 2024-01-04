
package app.crossword.yourealwaysbe.puz.io;

import java.io.InputStream;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WallStreetJournalJSONIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return WallStreetJournalJSONIOTest
            .class
            .getResourceAsStream("/wsj.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) throws Exception {
        assertEquals(puz.getTitle(), "Test title");
        assertEquals(puz.getAuthor(), "Test setter");
        assertEquals(puz.getCompletionMessage(), "Correct solution");
        assertEquals(puz.getNotes(), "Test description");
        assertEquals(puz.getSource(), "Test publisher");
        assertEquals(puz.getDate(), LocalDate.of(2024, 1, 3));

        assertEquals(puz.getWidth(), 15);
        assertEquals(puz.getHeight(), 15);

        Box[][] boxes = puz.getBoxes();

        assertEquals(boxes[0][0].getClueNumber(), "1");
        assertEquals(boxes[0][5].getClueNumber(), "5");
        assertTrue(Box.isBlock(boxes[0][4]));
        assertEquals(boxes[5][4].getClueNumber(), "27");
        assertTrue(Box.isBlock(boxes[5][7]));

        assertEquals(boxes[0][0].getSolution(), "A");
        assertEquals(boxes[5][4].getSolution(), "B");

        assertEquals(boxes[7][0].getShape(), Box.Shape.CIRCLE);
        assertEquals(boxes[7][1].getShape(), Box.Shape.CIRCLE);
        assertFalse(boxes[7][3].hasShape());

        ClueList acrossClues = puz.getClues("Across");
        ClueList downClues = puz.getClues("Down");

        assertEquals(acrossClues.getClueByNumber("1").getHint(), "Clue 1a");
        assertEquals(acrossClues.getClueByNumber("21").getHint(), "Clue 21a");
        assertEquals(downClues.getClueByNumber("1").getHint(), "Clue 1d");
        assertEquals(downClues.getClueByNumber("2").getHint(), "Clue 2d");
    }

    @Test
    public void testPuzzle1() throws Exception {
        try (InputStream is = getTestPuzzle1InputStream()) {
            Puzzle puz = WallStreetJournalJSONIO.readPuzzle(is);
            assertIsTestPuzzle1(puz);
        }
    }
}

