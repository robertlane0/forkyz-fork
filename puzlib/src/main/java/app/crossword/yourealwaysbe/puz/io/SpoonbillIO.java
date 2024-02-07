
package app.crossword.yourealwaysbe.puz.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;

/**
 * Parser for .xwc Spoonbill
 *
 * Format url:
 * https://github.com/Tw1ddle/Blind-Crossword-3D/wiki/Spoonbill-Crossword-Compiler-(.xwc)-Format-Specification
 **/
public class SpoonbillIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(SpoonbillIO.class.getCanonicalName());

    private static final String MODE_SKELETON = "Skeleton";
    private static final String MODE_SOLUTION = "Solution";
    private static final String MODE_CLUE = "Clue";
    private static final String MODE_SOLVE = "Solve";

    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";

    public static class SpoonbillFormatException extends Exception {
        private static final long serialVersionUID = 5890275381530098661L;
        public SpoonbillFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws IOException {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is)
            );

            String versionLine = expectLine(reader);

            String[] split = versionLine.split(" ", 2);
            if (!"TXWordGrid3".equalsIgnoreCase(split[0])) {
                throw new SpoonbillFormatException(
                    "Unknown Spoonbill version: " + split[0]
                );
            }
            String author = split[1];
            String title = expectLine(reader);
            String mode = expectLine(reader);

            PuzzleBuilder builder = new PuzzleBuilder(getBoxes(reader));

            builder.setTitle(title)
                .setAuthor(author);

            if (MODE_CLUE.equals(mode) || MODE_SOLVE.equals(mode)) {
                getClues(reader, true, builder);
                getClues(reader, false, builder);
            }

            return builder.getPuzzle();
        } catch (SpoonbillFormatException e) {
            LOG.info("Failed to parse Spoonbill file: " + e);
            return null;
        }
    }

    /**
     * Read boxes from file
     *
     * Will return a grid, throws an exception if error
     */
    private static Box[][] getBoxes(
        BufferedReader reader
    ) throws IOException, SpoonbillFormatException {
        try {
            int height = Integer.valueOf(expectLine(reader));
            int width = Integer.valueOf(expectLine(reader));

            Box[][] boxes = new Box[height][width];

            for (int row = 0; row < height; row++) {
                String line = expectLine(reader);
                for (int col = 0; col < width && col < line.length(); col++) {
                    char cell = line.charAt(col);
                    if (cell != '1') {
                        boxes[row][col] = new Box();
                        if (cell != '0') {
                            boxes[row][col].setSolution(
                                Character.toUpperCase(cell)
                            );
                        }
                    }
                }
            }

            return boxes;
        } catch (NumberFormatException e) {
            throw new SpoonbillFormatException(
                "Bad width or height line: " + e
            );
        }
    }

    private static void getClues(
        BufferedReader reader, boolean across, PuzzleBuilder builder
    ) throws IOException, SpoonbillFormatException {
        int numClues = Integer.valueOf(expectLine(reader));
        for (int i = 0; i < numClues; i++) {
            String line = expectLine(reader);
            String[] data = line.split("\\|");
            if (data.length < 6) {
                throw new SpoonbillFormatException(
                    "Unexpected clue line: " + line
                );
            }

            String number = data[0].trim();
            int startRow = Integer.valueOf(data[1].trim()) - 1;
            int startCol = Integer.valueOf(data[2].trim()) - 1;
            int length = Integer.valueOf(data[3].trim());
            Zone zone = getZone(startRow, startCol, length, across);
            String listName = across ? ACROSS_LIST : DOWN_LIST;
            int index = builder.getNextClueIndex(listName);
            String hint = data[5].trim();

            builder.addClue(new Clue(
                listName, index, number, null, hint, zone
            ));

            if (zone.size() > 0) {
                Box box = builder.getBox(zone.getPosition(0));
                if (box != null)
                    box.setClueNumber(number);
            }
        }
    }

    /**
     * Get the zone
     *
     * From (startRow, startCol) then moving length cells across or down
     * depending on across argument.
     */
    private static Zone getZone(
        int startRow, int startCol, int length, boolean across
    ) {
        Zone zone = new Zone();
        int row = startRow;
        int col = startCol;
        for (int i = 0; i < length; i++) {
            zone.addPosition(new Position(row, col));
            row += across ? 0 : 1;
            col += across ? 1 : 0;
        }
        return zone;
    }

    /**
     * Return trimmed next line or exception
     */
    private static String expectLine(
        BufferedReader reader
    ) throws IOException, SpoonbillFormatException {
        String line = reader.readLine();
        if (line == null) {
            throw new SpoonbillFormatException("Expected more input");
        }
        return line.trim();
    }
}
