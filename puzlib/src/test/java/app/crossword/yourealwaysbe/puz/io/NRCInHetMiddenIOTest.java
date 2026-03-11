
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

public class NRCInHetMiddenIOTest {
    public static InputStream getTestPuzzleInputStream(String path) {
        return NRCInHetMiddenIOTest
                .class
                .getResourceAsStream(path);
    }

    public static void assertIsTestPuzzle1(Puzzle puz) {
        String ACROSS_CLUES = "Across";
        String DOWN_CLUES = "Down";

        assertEquals(LocalDate.of(2025, 11, 18), puz.getDate());

        Box[][] boxes = puz.getBoxes();

        assertEquals(5, boxes.length);
        assertEquals(5, boxes[0].length);
        assertEquals("1", boxes[0][1].getClueNumber());
        assertEquals("2", boxes[1][0].getClueNumber());
        assertEquals("3", boxes[2][2].getClueNumber());
        assertEquals("m", boxes[2][1].getClueNumber());
        assertTrue(boxes[2][2].isStartOf(new ClueID(DOWN_CLUES, 1)));

        assertEquals(boxes[2][1].getSolution(), "M");
        assertEquals(boxes[2][2].getSolution(), "I");
        assertEquals(boxes[2][3].getSolution(), "D");

        assertEquals(boxes[0][1].getSolution(), "O");
        assertEquals(boxes[1][1].getSolution(), "O");
        assertEquals(boxes[2][1].getSolution(), "M");
        assertEquals(boxes[3][1].getSolution(), "E");

        assertFalse(Box.isBlock(boxes[2][1]));

        ClueList acrossClues = puz.getClues(ACROSS_CLUES);
        ClueList downClues = puz.getClues(DOWN_CLUES);

        assertEquals(acrossClues.getClueByNumber("m").getHint(), "MIDDENWOORD");
    }

    public static void assertIsTestPuzzle2(Puzzle puz) {
        String ACROSS_CLUES = "Across";

        Box[][] boxes = puz.getBoxes();

        assertEquals("3", boxes[2][1].getClueNumber());

        assertEquals(boxes[2][1].getSolution(), "M");
        assertEquals(boxes[2][2].getSolution(), "I");
        assertEquals(boxes[2][3].getSolution(), "D");

        assertEquals(boxes[3][1].getSolution(), "E");
        assertEquals(boxes[4][1].getSolution(), "X");

        ClueList acrossClues = puz.getClues(ACROSS_CLUES);

        assertEquals(acrossClues.getClueByNumber("3").getHint(), "MIDDENWOORD");
    }

    public static void assertIsTestPuzzle3(Puzzle puz) {
        String ACROSS_CLUES = "Across";

        Box[][] boxes = puz.getBoxes();

        assertEquals("m1", boxes[2][0].getClueNumber());
        assertEquals("m2", boxes[2][3].getClueNumber());
        assertEquals("3", boxes[2][1].getClueNumber());

        ClueList acrossClues = puz.getClues(ACROSS_CLUES);
        assertEquals(
            acrossClues.getClueByNumber("m1").getHint(),
            "MIDDENWOORD"
        );
        assertEquals(
            acrossClues.getClueByNumber("m2").getHint(),
            "MIDDENWOORD"
        );
    }

    @Test
    public void testPuzzle1() throws Exception {
        NRCInHetMiddenIO parser = new NRCInHetMiddenIO();
        PuzzleBuilder builder = parser.parsePuzzle(
            getTestPuzzleInputStream("/inhetmidden-1.json")
        );
        assertIsTestPuzzle1(builder.puzzle);
    }

    @Test
    public void testPuzzle2() throws Exception {
        NRCInHetMiddenIO parser = new NRCInHetMiddenIO();
        PuzzleBuilder builder = parser.parsePuzzle(
            getTestPuzzleInputStream("/inhetmidden-2.json")
        );
        assertIsTestPuzzle2(builder.puzzle);
    }

    @Test
    public void testPuzzle3() throws Exception {
        NRCInHetMiddenIO parser = new NRCInHetMiddenIO();
        PuzzleBuilder builder = parser.parsePuzzle(
            getTestPuzzleInputStream("/inhetmidden-3.json")
        );
        assertIsTestPuzzle3(builder.puzzle);
    }
}

