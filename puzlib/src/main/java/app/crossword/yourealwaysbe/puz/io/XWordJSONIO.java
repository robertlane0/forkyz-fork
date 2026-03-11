
package app.crossword.yourealwaysbe.puz.io;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.util.JSONParser;

/**
 * JSON Format used by xword
 *
 * https://www.xwordinfo.com/JSON/
 *
 * "title": ..
 * "author": ..
 * "copyright": ..
 * "publisher": ..
 * "date": "m/d/yyyy"
 * "size": { "rows": n, "cols": n }
 * "grid": [ "A", "B", ".", "REBUS" ... ]
 * "gridnums": [ n, n, 0, n, 0, 0 ... ]
 * "circles": [ 0, 1 ... ]
 * "rbars": ..
 * "bbars": ..
 * "clues": {
 *   "across": [ "n. hint", ... ]
 *   "down": ...
 * }
 * "notepad": "<notes/html>"
 * "jnotes": "<notes/html>"
 */
public class XWordJSONIO extends AbstractJSONIO {
    private static final Logger LOG
        = Logger.getLogger(XWordJSONIO.class.getCanonicalName());

    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);

    private static final String ACROSS_LIST = DefaultListNames.ACROSS_LIST;
    private static final String DOWN_LIST = DefaultListNames.DOWN_LIST;

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class XWordJSONFormatException extends Exception {
        private static final long serialVersionUID = 983029533138279366L;
        public XWordJSONFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = JSONParser.parse(is);
            return readPuzzleFromJSON(json);
        } catch (IOException | JSONException | XWordJSONFormatException e) {
            LOG.info("Error parsing XWord JSON: " + e);
            return null;
        }
    }

    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException, XWordJSONFormatException {
        Box[][] boxes = getBoxes(json);
        if (boxes == null)
            return null;

        PuzzleBuilder builder = new PuzzleBuilder(boxes)
            .setTitle(optStringNull(json, "title"))
            .setAuthor(optStringNull(json, "author"))
            .setCopyright(optStringNull(json, "copyright"))
            .setSource(optStringNull(json, "publisher"))
            .setDate(getDate(json))
            .setNotes(getNotes(json));

        addClues(json, builder);

        return builder.puzzle;
    }

    private static LocalDate getDate(JSONObject json) {
        try {
            String date = optStringNull(json, "date");
            return (date == null)
                ? null
                : LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String getNotes(JSONObject json) {
        String notepad = optStringNull(json, "notepad");
        String jnotes = optStringNull(json, "jnotes");

        if (jnotes == null && notepad == null)
            return null;

        if (jnotes == null)
            return notepad;

        if (notepad == null)
            return jnotes;

        return "<p>" + notepad + "</p><p>" + jnotes + "</p>";
    }

    private static Box[][] getBoxes(JSONObject json)
            throws JSONException, XWordJSONFormatException {
        JSONObject size = json.getJSONObject("size");
        int numRows = size.getInt("rows");
        int numCols = size.getInt("cols");

        Box[][] boxes = new Box[numRows][numCols];

        JSONArray grid = json.getJSONArray("grid");
        JSONArray numbers = json.getJSONArray("gridnums");
        JSONArray circles = json.optJSONArray("circles");
        JSONArray rbars = json.optJSONArray("rbars");
        JSONArray bbars = json.optJSONArray("bbars");

        for (int cell = 0; cell < grid.length(); cell++) {
            int row = cell / numRows;
            int col = cell % numRows;

            if (row < numRows && col < numCols) {
                String contents = grid.getString(cell);
                if (!".".equals(contents)) {
                    boxes[row][col] = new Box();
                    boxes[row][col].setSolution(contents);

                    if (cell < numbers.length()) {
                        int number = numbers.getInt(cell);
                        if (number > 0)
                            boxes[row][col].setClueNumber(String.valueOf(number));
                    }

                    if (circles != null && cell < circles.length()) {
                        int circle = circles.getInt(cell);
                        if (circle != 0)
                            boxes[row][col].setShape(Box.Shape.CIRCLE);
                    }
                    if (rbars != null && cell < rbars.length()) {
                        int rbar = rbars.getInt(cell);
                        if (rbar != 0)
                            boxes[row][col].setBarRight(Box.Bar.SOLID);
                    }
                    if (bbars != null && cell < bbars.length()) {
                        int bbar = bbars.getInt(cell);
                        if (bbar != 0)
                            boxes[row][col].setBarBottom(Box.Bar.SOLID);
                    }
                }
            }
        }

        return boxes;
    }

    private static void addClues(JSONObject json, PuzzleBuilder builder)
            throws JSONException, XWordJSONFormatException {
        JSONObject clues = json.getJSONObject("clues");
        JSONArray across = clues.getJSONArray("across");
        addCluesArray(across, builder, true);
        JSONArray down= clues.getJSONArray("down");
        addCluesArray(down, builder, false);
    }

    private static void addCluesArray(
        JSONArray clues, PuzzleBuilder builder, boolean across
    ) throws XWordJSONFormatException {
        for (int i = 0; i < clues.length(); i++) {
            String clue = clues.getString(i);
            String[] split = clue.split("\\.", 2);

            if (split.length != 2) {
                throw new XWordJSONFormatException(
                    "Clue text not in num. hint format: " + clue
                );
            }

            String numString = split[0].trim();
            String hint = split[1].trim();
            try {
                String num = String.valueOf(Integer.valueOf(numString));
                if (across)
                    builder.addAcrossClue(ACROSS_LIST, num, hint);
                else
                    builder.addDownClue(DOWN_LIST, num, hint);
            } catch (NumberFormatException e) {
                throw new XWordJSONFormatException(
                    "Bad clue number " + numString
                );
            }
        }
    }
}
