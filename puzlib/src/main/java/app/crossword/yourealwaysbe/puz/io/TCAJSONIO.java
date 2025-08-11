
package app.crossword.yourealwaysbe.puz.io;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.puz.util.JSONParser;

/**
 * Puzzle format used by Tribune
 *
 * "metadata": {
 *      "author": ..
 *      "title": ..
 *      "date": ..
 *      "format": "TCA"
 * }
 * "data": {
 *      "clues": [
 *           {
 *              "clueNo": num,
 *              "clueType": "across/down",
 *              "shape": {
 *                  "1,2,3,4,5" // list of cell IDs
 *              },
 *              "text": "clue"
 *           }
 *      ],
 *      "gridRow": num rows,
 *      "gridCol": num cols,
 *      "gCells": [
 *          {
 *              "id": cell id num,
 *              "cellNumber": string,
 *              "cellChar": char solution,
 *              "cellType": "square/disable"
 *              "xchar": "Y/N" // not sure what this means, ignore for now
 *          }
 *      ]
 * }
 */
public class TCAJSONIO extends AbstractJSONIO {
    private static final Logger LOG
        = Logger.getLogger(TCAJSONIO.class.getCanonicalName());

    private static final DateTimeFormatter DATE_META_FORMATTER
        = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_DATA_FORMATTER
        = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class TCAJSONFormatException extends Exception {
        private static final long serialVersionUID = 1849433759353222885L;
        public TCAJSONFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = JSONParser.parse(is);
            return readPuzzleFromJSON(json);
        } catch (TCAJSONFormatException | JSONException e) {
            LOG.severe("Could not read TCA JSON: " + e);
            return null;
        }
    }

    public static Puzzle readPuzzle(String jsonString) {
        try {
            JSONObject json = JSONParser.parse(jsonString);
            return readPuzzleFromJSON(json);
        } catch (TCAJSONFormatException | JSONException e) {
            LOG.severe("Could not read TCA JSON: " + e);
            return null;
        }
    }

    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException, TCAJSONFormatException {
        try {
            JSONObject metaData = json.optJSONObject("metadata");
            JSONObject data = json.optJSONObject("data");
            if (data == null)
                data = json;

            Map<String, Position> cellPositions = new HashMap<>();
            PuzzleBuilder builder
                = new PuzzleBuilder(getBoxes(data, cellPositions));

            addMetaData(json, builder);
            addClues(data, cellPositions, builder);

            return builder.getPuzzle();
        } catch (IllegalArgumentException e) {
            throw new TCAJSONFormatException(
                "Could not set grid boxes from data file: " + e.getMessage()
            );
        }
    }

    /**
     * Get boxes
     *
     * cellPositions is a map to fill from cell id to grid position
     */
    private static Box[][] getBoxes(
        JSONObject json, Map<String, Position> cellPositions
    ) throws JSONException, TCAJSONFormatException {
        int numRows = json.getInt("gridRow");
        int numCols = json.getInt("gridCol");
        JSONArray cells = json.getJSONArray("gCells");

        if (numRows == 0 || numCols == 0) {
            throw new TCAJSONFormatException(
                "Grid has a bad shape: "
                + numRows
                + " rows and "
                + numCols
                + " cols."
            );
        }

        List<Box[]> rowList = new ArrayList<>();
        Box[] rowBoxes = null;

        for (int i = 0; i < cells.length(); i++) {
            int row = i / numCols;
            int col = i % numCols;

            if (rowBoxes == null || col == 0) {
                if (rowBoxes != null)
                    rowList.add(rowBoxes);
                rowBoxes = new Box[numCols];
            }

            JSONObject cell = cells.getJSONObject(i);
            String cellType = optStringNull(cell, "cellType");
            if (cellType == null || "disable".equalsIgnoreCase(cellType))
                continue;

            Box box = new Box();

            Integer id = cell.optInt("id");
            if (id != null)
                cellPositions.put(String.valueOf(id), new Position(row, col));

            String clueNo = optStringNull(cell, "cellNumber");
            if (clueNo != null && clueNo.length() > 0)
                box.setClueNumber(clueNo);

            String solution = optStringNull(cell, "cellChar");
            if (solution != null && solution.length() > 0)
                box.setSolution(solution);

            rowBoxes[col] = box;

            // Does xchar need handling?
        }

        if (rowBoxes != null)
            rowList.add(rowBoxes);

        return rowList.toArray(new Box[0][0]);
    }

    private static void addMetaData(
        JSONObject json, PuzzleBuilder builder
    ) throws JSONException {
        JSONObject metaData = json.optJSONObject("metaData");
        JSONObject data = json.optJSONObject("data");

        if (metaData != null) {
            builder.setTitle(optStringNull(metaData, "title"));
            builder.setAuthor(optStringNull(metaData, "author"));
            builder.setNotes(optStringNull(metaData, "description"));
            try {
                String date = optStringNull(metaData, "date");
                if (date != null)
                    builder.setDate(LocalDate.parse(date, DATE_META_FORMATTER));
            } catch (DateTimeParseException e) {
                // oh well
            }
        }

        if (data != null) {
            try {
                String date = optStringNull(data, "date");
                if (date != null)
                    builder.setDate(LocalDate.parse(date, DATE_DATA_FORMATTER));
            } catch (DateTimeParseException e) {
                // oh well
            }
        }
    }

    private static void addClues(
        JSONObject json,
        Map<String, Position> cellPositions,
        PuzzleBuilder builder
    ) throws JSONException {
        JSONArray clues = json.getJSONArray("clues");
        for (int i = 0; i < clues.length(); i++) {
            JSONObject clue = clues.getJSONObject(i);

            String clueNumber = null;
            Integer clueNoInt = clue.optInt("clueNo");
            if (clueNoInt != null)
                clueNumber = String.valueOf(clueNoInt);

            boolean across
                = "across".equalsIgnoreCase(clue.getString("clueType"));

            Zone zone = new Zone();
            JSONObject shape = clue.getJSONObject("shape");
            if (shape != null) {
                String cellIDs = shape.getString("value");
                for (String id : cellIDs.split(",")) {
                    Position pos = cellPositions.get(id);
                    if (pos != null)
                        zone.addPosition(pos);
                }
            }

            String hint = clue.getString("text");

            String listName = across ? ACROSS_LIST : DOWN_LIST;
            int index = builder.getNextClueIndex(listName);
            builder.addClue(
                new Clue(listName, index, clueNumber, null, hint, zone)
            );
        }
    }
}
