
package app.crossword.yourealwaysbe.puz.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDate;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.ClueID;
import app.crossword.yourealwaysbe.puz.ClueList;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;

public class FortyTwoPuzzlesIOTest {
    public static InputStream getTestPuzzle1InputStream() {
        return FortyTwoPuzzlesIOTest
            .class
            .getResourceAsStream("/fortytwopuzzles-1.json");
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        String ACROSS_CLUES = "Across";
        String DOWN_CLUES = "Down";

        assertEquals(LocalDate.of(2025, 11, 18), puz.getDate());

        Box[][] boxes = puz.getBoxes();

        assertEquals(5, boxes.length);
        assertEquals(5, boxes[0].length);
        assertEquals("1", boxes[0][1].getClueNumber());
        assertEquals("4", boxes[1][0].getClueNumber());
        assertTrue(boxes[0][1].isStartOf(new ClueID(ACROSS_CLUES, 0)));
        assertTrue(boxes[0][1].isStartOf(new ClueID(DOWN_CLUES, 0)));
        assertTrue(boxes[1][0].isStartOf(new ClueID(ACROSS_CLUES, 1)));
        assertTrue(boxes[1][0].isStartOf(new ClueID(DOWN_CLUES, 3)));

        assertEquals(boxes[0][1].getSolution(), "O");
        assertEquals(boxes[0][2].getSolution(), "N");
        assertEquals(boxes[0][3].getSolution(), "E");

        assertEquals(boxes[3][0].getSolution(), "S");
        assertEquals(boxes[3][1].getSolution(), "E");
        assertEquals(boxes[3][2].getSolution(), "V");
        assertEquals(boxes[3][3].getSolution(), "E");
        assertEquals(boxes[3][4].getSolution(), "N");

        assertTrue(Box.isBlock(boxes[0][0]));
        assertTrue(Box.isBlock(boxes[4][3]));

        ClueList acrossClues = puz.getClues(ACROSS_CLUES);
        ClueList downClues = puz.getClues(DOWN_CLUES);

        assertEquals(
                acrossClues.getClueByNumber("1").getHint(),
                "The first number"
        );
        assertEquals(
                acrossClues.getClueByNumber("5").getHint(),
                "The fifth number plus an 'S'"
        );
        assertEquals(
                downClues.getClueByNumber("1").getHint(),
                "Seemingly random vowels"
        );
        assertEquals(
                downClues.getClueByNumber("2").getHint(),
                "Seemingly random letters"
        );
    }

    @Test
    public void testPuzzle1() throws Exception {
        FortyTwoPuzzlesIO parser = new FortyTwoPuzzlesIO();
        PuzzleBuilder builder = parser.parsePuzzle(getTestPuzzle1InputStream());
        assertIsTestPuzzle1(builder.getPuzzle());
    }
}

