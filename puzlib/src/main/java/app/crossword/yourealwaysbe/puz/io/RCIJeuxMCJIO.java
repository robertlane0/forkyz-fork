
package app.crossword.yourealwaysbe.puz.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;

/**
 * Format used by RCI Jeux for crossword puzzles
 */
public class RCIJeuxMCJIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(RCIJeuxMCJIO.class.getCanonicalName());

    // Not internationalised, assume French
    private static final String ACROSS_LIST = "Horiz.";
    private static final String DOWN_LIST = "Vert.";
    private static final char JOIN_DASH = '–';

    private static final String ARROW_WORDS_CLUE_FIELD_NAME = "definitions";
    private static final String CROSSWORD_ACROSS_CLUE_FIELD_NAME
        = "definitionsh";
    private static final String CROSSWORD_DOWN_CLUE_FIELD_NAME
        = "definitionsv";

    public static class MCJFormatException extends Exception {
        private static final long serialVersionUID = 6288124829707522643L;
        public MCJFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            String hjson = getHJSON(is);
            if (hjson == null)
                return null;

            return readPuzzleFromHJSON(
                JsonValue.readHjson(hjson).asObject()
            );
        } catch (MCJFormatException e) {
            LOG.severe("Could not read RCIJeux MCJ: " + e);
            return null;
        }
    }

    /**
     * Extract the HJSON part from the stream
     *
     * Format is usually
     *
     * var gameData = {
     *
     * };
     *
     * just get the { ... } HJSON bit, return as a string, ignore
     * anything outside of the outer { }, doesn't have to be "var
     * gameData"
     */
    private static String getHJSON(InputStream is) throws IOException {
        try (
            BufferedReader reader
                = new BufferedReader(new InputStreamReader(is))
        ) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String wholeFile = sb.toString();
            int start = wholeFile.indexOf('{');
            int end = wholeFile.lastIndexOf('}');

            if (start < 0 || end < 0)
                return null;
            else
                return wholeFile.substring(start, end + 1);
        }
    }

    /**
     * Read puzzle from RaetselZentrale JSON format
     *
     * Does not include author, date, source.
     */
    private static Puzzle readPuzzleFromHJSON(
        JsonObject hjson
    ) throws MCJFormatException {
        String title = hjson.getString("titre", null);
        if (title == null)
            throw new MCJFormatException("No titre field in HJSON.");

        PuzzleBuilder builder = new PuzzleBuilder(getBoxes(hjson))
            .setTitle(title);

        addClues(hjson, builder);

        return builder.getPuzzle();
    }

    private static Box[][] getBoxes(
        JsonObject hjson
    ) throws MCJFormatException {
        int numRows = hjson.getInt("nbcaseshauteur", -1);
        int numCols = hjson.getInt("nbcaseslargeur", -1);

        if (numRows < 0 || numCols < 0) {
            throw new MCJFormatException(
                "Impossible grid size " + numRows + "x" + numCols + "."
            );
        }

        Box[][] boxes = new Box[numRows][numCols];
        JsonArray rows = asArray(hjson.get("grille"));

        for (int row = 0; row < numRows; row++) {
            String cols = getRowString(rows.get(row));
            for (int col = 0; col < numCols; col++) {
                char cell = cols.charAt(col);
                if (Character.isUpperCase(cell)) {
                    boxes[row][col] = new Box();
                    boxes[row][col].setSolution(String.valueOf(cell));
                    // clue numbers set with clues
                }
            }
        }

        return boxes;
    }

    private static String getRowString(
        JsonValue row
    ) throws MCJFormatException {
        if (row == null) {
            throw new MCJFormatException(
                "When getting grid row, got a null value."
            );
        } else if (row.isString()) {
            return row.asString();
        } else if (row.isArray() && row.asArray().size() == 1) {
            return asString(row.asArray().get(0));
        } else {
            throw new MCJFormatException(
                "Expected row "
                + row
                + " to be a string or array of one string."
            );
        }
    }

    private static void addClues(
        JsonObject hjson,
        PuzzleBuilder builder
    ) throws MCJFormatException {
        builder.autoNumberBoxes();
        addAcrossClues(hjson, builder);
        addDownClues(hjson, builder);
    }

    private static void addAcrossClues(
        JsonObject hjson,
        PuzzleBuilder builder
    ) throws MCJFormatException {
        JsonArray clues = asArray(hjson.get(CROSSWORD_ACROSS_CLUE_FIELD_NAME));

        for (int row = 0; row < builder.getHeight(); row++) {
            JsonArray rowClues = asArray(clues.get(row));
            int clueIdx = 0;
            for (int col = 0; col < builder.getWidth(); col++) {
                Box box = builder.getBox(row, col);
                if (
                    !Box.isBlock(box)
                    && box.hasClueNumber()
                    && builder.isStartClue(row, col, true)
                    && clueIdx < clues.size()
                ){
                    String hint = asString(rowClues.get(clueIdx));
                    builder.addAcrossClue(
                        ACROSS_LIST,
                        box.getClueNumber(),
                        hint
                    );
                    clueIdx += 1;
                }
            }
        }
    }

    private static void addDownClues(
        JsonObject hjson,
        PuzzleBuilder builder
    ) throws MCJFormatException {
        JsonArray clues = asArray(hjson.get(CROSSWORD_DOWN_CLUE_FIELD_NAME));

        for (int col = 0; col < builder.getWidth(); col++) {
            JsonArray colClues = asArray(clues.get(col));
            int clueIdx = 0;
            for (int row = 0; row < builder.getHeight(); row++) {
                Box box = builder.getBox(row, col);
                if (
                    !Box.isBlock(box)
                    && box.hasClueNumber()
                    && builder.isStartClue(row, col, false)
                    && clueIdx < clues.size()
                ){
                    String hint = asString(colClues.get(clueIdx));
                    builder.addDownClue(
                        DOWN_LIST,
                        box.getClueNumber(),
                        hint
                    );
                    clueIdx += 1;
                }
            }
        }
    }

    private static String getHint(
        JsonArray clueParts
    ) throws MCJFormatException {
        StringBuilder hint = new StringBuilder();

        for (int i = 0; i < clueParts.size(); i++) {
            hint.append(asString(clueParts.get(i)));
            int length = hint.length();
            if (hint.charAt(length - 1) == JOIN_DASH)
                hint.setLength(length - 1);
            else if (i < clueParts.size() - 1)
                hint.append(" ");
        }

        return hint.toString();
    }

    private static JsonArray asArray(JsonValue val) throws MCJFormatException {
        if (val == null || !val.isArray()) {
            throw new MCJFormatException(
                "Expected " + val + " to be an array."
            );
        }
        return val.asArray();
    }

    private static String asString(JsonValue val) throws MCJFormatException {
        if (val == null || !val.isString()) {
            throw new MCJFormatException(
                "Expect " + val + " to be a string."
            );
        }
        return val.asString();
    }
}
