
package app.crossword.yourealwaysbe.puz.io;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.util.JSONParser;

/**
 * Format used by amuniversal.com
 *
 * JSON:
 *
 * Grid shape given by layout field. Each box is two characters. 00 for blank
 * cell, -1 for no cell, else number of cell. "Linei" field for ith line, 1-based.
 *
 * "Layout": { "Line1": "010203-100...", ... }
 *
 * "Solution": { "Line1": "ANSWER ANSWER ANSWER", ... }
 *
 * In solution, spaces are the blocks.
 *
 * "DownClue": "num|hint\nnum|hint\n..."
 * "AcrossClue": "num|hint\nnum|hint\n..."
 *
 * Clues are one string, one line per clue, each clue num|hint.
 *
 * Other fields:
 *
 * "Date": "yyyymmdd"
 * "Author"
 * "Editor"
 * "Copyright"
 * "Title"
 * "Width"
 * "Height"
 */
public class AMUniversalJSONIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(AMUniversalJSONIO.class.getCanonicalName());

    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.US);

    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class AMUniversalFormatException extends Exception {
        private static final long serialVersionUID = 1880295331132898661L;
        public AMUniversalFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = JSONParser.parse(is);
            return readPuzzleFromJSON(json);
        } catch (IOException | JSONException | AMUniversalFormatException e) {
            LOG.info("Error parsing AMUniversal JSON: " + e);
            return null;
        }
    }

    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException, AMUniversalFormatException {
        PuzzleBuilder builder = new PuzzleBuilder(getBoxes(json))
            .setTitle(json.optString("Title"))
            .setAuthor(json.optString("Author"))
            .setCopyright(json.optString("Copyright"))
            .setDate(getDate(json));

        addClues(json, builder);

        return builder.getPuzzle();
    }

    private static LocalDate getDate(JSONObject json) {
        try {
            String date = json.optString("Date");
            return (date == null)
                ? null
                : LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Box[][] getBoxes(JSONObject json)
            throws JSONException, AMUniversalFormatException {
        int numRows = json.getInt("Height");
        int numCols = json.getInt("Width");

        Box[][] boxes = new Box[numRows][numCols];

        JSONObject layout = json.getJSONObject("Layout");
        for (int row = 0; row < numRows; row++) {
            String rowData = layout.getString("Line" + (row + 1));

            // split into twos
            for (int col = 0; col < numCols; col++) {
                if (2 * col + 1 < rowData.length()) {
                    try {
                        String cell = ""
                            + rowData.charAt(2 * col)
                            + rowData.charAt(2 * col + 1);
                        int cellVal = Integer.valueOf(cell);
                        if (cellVal >= 0) {
                            boxes[row][col] = new Box();
                            if (cellVal > 0) {
                                boxes[row][col].setClueNumber(
                                    String.valueOf(cellVal)
                                );
                            }
                        }
                    } catch (NumberFormatException e) {
                        throw new AMUniversalFormatException(
                            "Bad number format for cell in " + row
                        );
                    }
                }
            }
        }

        JSONObject solution = json.optJSONObject("Solution");
        if (solution != null) {
            for (int row = 0; row < numRows; row++) {
                String rowData = solution.getString("Line" + (row + 1));
                for (int col = 0; col < numCols; col++) {
                    if (col < rowData.length()) {
                        char answer = rowData.charAt(col);
                        if (answer != ' ' && boxes[row][col] != null) {
                            boxes[row][col].setSolution(answer);
                        }
                    }
                }
            }
        }

        return boxes;
    }

    private static void addClues(JSONObject json, PuzzleBuilder builder)
            throws JSONException, AMUniversalFormatException {
        String across = json.optString("AcrossClue");
        if (across != null)
            addCluesFromString(across, builder, true);
        String down = json.optString("DownClue");
        if (down != null)
            addCluesFromString(down, builder, false);
    }

    private static void addCluesFromString(
        String clues, PuzzleBuilder builder, boolean across
    ) throws AMUniversalFormatException {
        String[] lines = clues.split("\n");
        for (String clue : lines) {
            String[] parts = clue.split("\\|", 2);
            if (parts.length >= 2) {
                String numString = parts[0];
                String hint = parts[1];
                try {
                    String num = String.valueOf(Integer.valueOf(numString));
                    if (across)
                        builder.addAcrossClue(ACROSS_LIST, num, hint);
                    else
                        builder.addDownClue(DOWN_LIST, num, hint);
                } catch (NumberFormatException e) {
                    throw new AMUniversalFormatException(
                        "Bad clue number " + numString
                    );
                }
            }
        }
    }
}
