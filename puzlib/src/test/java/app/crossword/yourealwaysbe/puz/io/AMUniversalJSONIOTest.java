
package app.crossword.yourealwaysbe.puz.io;

import java.io.InputStream;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AMUniversalJSONIOTest {

    public static InputStream getTestPuzzle1InputStream() {
        return AMUniversalJSONIOTest
            .class
            .getResourceAsStream("/amuniversal.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        String ACROSS_CLUES = "Across";
        String DOWN_CLUES = "Down";

        assertEquals("Test title", puz.getTitle());
        assertEquals("Test author", puz.getAuthor());
        assertEquals("Test copyright", puz.getCopyright());
        assertEquals(LocalDate.of(2024, 1, 2), puz.getDate());

        Box[][] boxes = puz.getBoxes();

        assertEquals(15, boxes.length);
        assertEquals(15, boxes[0].length);
        assertEquals("1", boxes[0][0].getClueNumber());
        assertTrue(boxes[0][0].isStartOf(new ClueID(ACROSS_CLUES, 0)));
        assertTrue(boxes[0][0].isStartOf(new ClueID(DOWN_CLUES, 0)));
        assertTrue(boxes[0][5].isStartOf(new ClueID(DOWN_CLUES, 4)));
        assertFalse(boxes[0][2].isStartOf(new ClueID(DOWN_CLUES, 3)));

        assertEquals(boxes[0][0].getSolution(), "T");
        assertEquals(boxes[2][7].getSolution(), "Y");
        assertEquals(boxes[9][9].getSolution(), "S");
        assertEquals(boxes[3][6].getSolution(), "N");
        assertTrue(Box.isBlock(boxes[0][4]));
        assertTrue(Box.isBlock(boxes[5][1]));

        ClueList acrossClues = puz.getClues(ACROSS_CLUES);
        ClueList downClues = puz.getClues(DOWN_CLUES);

        assertEquals(
            acrossClues.getClueByNumber("1").getHint(),
            "Test clue 1"
        );
        assertEquals(
            acrossClues.getClueByNumber("14").getHint(),
            "Test clue 14"
        );
        assertEquals(
            downClues.getClueByNumber("23").getHint(),
            "Test clue 23"
        );
        assertEquals(
            downClues.getClueByNumber("29").getHint(),
            "Test clue 29"
        );
    }

    @Test
    public void testPuzzle1() throws Exception {
        Puzzle puz = AMUniversalJSONIO.readPuzzle(getTestPuzzle1InputStream());
        assertIsTestPuzzle1(puz);
    }
}
