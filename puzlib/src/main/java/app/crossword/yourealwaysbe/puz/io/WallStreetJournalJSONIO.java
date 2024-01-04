
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
 * Read a character stream of JSON data in the format used by the
 * Wall Street Journal
 *
 * "data": {
 *   "grid": [
 *     [ { "Number": "<num>", "Letter": "<solution>", "Blank": "<blank>" } ...]
 *     ..
 *   ],
 *   "copy": {
 *     "title": "Test title",
 *     "description": "..",
 *     "setter": "..",
 *     "publisher": "..",
 *     "date-publish": "..",
 *     "correctsolutionmessagetext": "Correct solution",
 *     "type": "block",
 *     "clues": [ {
 *         "title": "listname",
 *         "clues": [
 *           { "word": <id>, "number": n, "clue": ".." }, ...
 *         ]
 *       }
 *     ],
 *   },
 *   "words": [
 *      { id: n, "x": "1-5", "y": "3" }, ...
 *   ]
 * }
 */
public class WallStreetJournalJSONIO implements PuzzleParser {
    private static final Logger LOG
        = Logger.getLogger(AmuseLabsJSONIO.class.getCanonicalName());

    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("EEEE, d MMMM y");

    private static final String DEFAULT_CLUE_LIST_NAME = "Clues";

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class WSJFormatException extends Exception {
        private static final long serialVersionUID = 1849433759353222885L;
        public WSJFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = JSONParser.parse(is);
            return readPuzzleFromJSON(json);
        } catch (WSJFormatException | JSONException e) {
            LOG.severe("Could not read WSJ JSON: " + e);
            return null;
        }
    }

    public static Puzzle readPuzzle(String jsonString) {
        try {
            JSONObject json = JSONParser.parse(jsonString);
            return readPuzzleFromJSON(json);
        } catch (WSJFormatException | JSONException e) {
            LOG.severe("Could not read WSJ JSON: " + e);
            return null;
        }
    }

    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException, WSJFormatException {
        try {
            JSONObject data = json.optJSONObject("data");
            if (data == null)
                data = json;

            PuzzleBuilder builder = new PuzzleBuilder(getBoxes(data));

            addMetaData(data, builder);
            addClues(data, builder);

            return builder.getPuzzle();
        } catch (IllegalArgumentException e) {
            throw new WSJFormatException(
                "Could not set grid boxes from data file: " + e.getMessage()
            );
        }
    }

    private static Box[][] getBoxes(JSONObject json)
            throws JSONException {
        List<Box[]> rowList = new ArrayList<>();

        JSONArray rows = json.getJSONArray("grid");
        for (int row = 0; row < rows.length(); row++) {
            JSONArray rowArray = rows.getJSONArray(row);
            Box[] rowBoxes = new Box[rowArray.length()];
            for (int col = 0; col < rowArray.length(); col++) {
                JSONObject cell = rowArray.getJSONObject(col);
                if (!"blank".equalsIgnoreCase(optStringNull(cell, "Blank"))) {
                    Box box = new Box();

                    box.setClueNumber(optStringNull(cell, "Number"));
                    box.setSolution(optStringNull(cell, "Letter"));

                    JSONObject style = cell.optJSONObject("style");
                    if (style != null) {
                        String shape = optStringNull(style, "shapebg");
                        if ("circle".equalsIgnoreCase(shape)) {
                            box.setShape(Box.Shape.CIRCLE);
                        } else {
                            // see if it's an IPuz value why not
                            Box.Shape boxShape = IPuzIO.getShape(shape);
                            if (boxShape != null)
                                box.setShape(boxShape);
                        }

                        if (style.optBoolean("highlight", false)) {
                            box.setColor(IPuzIO.HIGHLIGHT_COLOR);
                        }
                    }

                    rowBoxes[col] = box;
                }
            }
            rowList.add(rowBoxes);
        }

        return rowList.toArray(new Box[0][0]);
    }

    private static void addMetaData(
        JSONObject json, PuzzleBuilder builder
    ) throws JSONException {
        JSONObject copy = json.getJSONObject("copy");
        builder.setTitle(optStringNull(copy, "title"));
        builder.setAuthor(optStringNull(copy, "setter"));
        builder.setSource(optStringNull(copy, "publisher"));
        builder.setNotes(optStringNull(copy, "description"));
        builder.setCompletionMessage(
            optStringNull(copy, "correctsolutionmessagetext")
        );

        try {
            String date = optStringNull(copy, "date-publish");
            if (date != null)
                builder.setDate(LocalDate.parse(date, DATE_FORMATTER));
        } catch (DateTimeParseException e) {
            // oh well
        }
    }

    private static void addClues(
        JSONObject json, PuzzleBuilder builder
    ) throws JSONException {
        Map<Integer, Zone> zones = getZones(json);
        JSONArray clues = json.getJSONObject("copy").getJSONArray("clues");
        for (int i = 0; i < clues.length(); i++) {
            addCluesList(clues.getJSONObject(i), zones, builder);
        }
    }

    private static void addCluesList(
        JSONObject json,
        Map<Integer, Zone> zones,
        PuzzleBuilder builder
    ) throws JSONException {
        String listName = optStringNull(json, "title");
        if (listName == null)
            listName = DEFAULT_CLUE_LIST_NAME;

        JSONArray clues = json.getJSONArray("clues");
        for (int i = 0; i < clues.length(); i++) {
            JSONObject clue = clues.getJSONObject(i);

            Zone zone = null;
            int zoneID = clue.optInt("word", -1);
            if (zoneID >= 0) {
                zone = zones.get(zoneID);
            }

            String number = optStringNull(clue, "number");
            String hint = optStringNull(clue, "clue");

            int index = builder.getNextClueIndex(listName);
            builder.addClue(
                new Clue(listName, index, number, null, hint, zone)
            );
        }
    }

    private static Map<Integer, Zone> getZones(
        JSONObject json
    ) throws JSONException {
        JSONObject copy = json.getJSONObject("copy");
        JSONArray words = copy.getJSONArray("words");

        Map<Integer, Zone> zones = new HashMap<>();

        for (int i = 0; i < words.length(); i++) {
            JSONObject word = words.getJSONObject(i);
            int id = word.getInt("id");
            String x = optStringNull(word, "x");
            String y = optStringNull(word, "y");
            if (x != null && y != null)
                zones.put(id, parseCells(x, y));
        }

        return zones;
    }

    /**
     * Parse cells data into zone
     *
     * E.g. x="1-3" y = "2" is (2, 1), (2, 2), (2, 3);
     */
    private static Zone parseCells(String x, String y) {
        Zone zone = new Zone();

        String[] xs = x.split("-");
        int xstart = Integer.valueOf(xs[0]) - 1;
        int xend = (xs.length > 1)
            ? Integer.valueOf(xs[1]) - 1
            : xstart;

        String[] ys = y.split("-");
        int ystart = Integer.valueOf(ys[0]) - 1;
        int yend = (ys.length > 1)
            ? Integer.valueOf(ys[1]) - 1
            : ystart;

        for (int row = ystart; row <= yend; row++)
            for (int col = xstart; col <= xend; col++)
                zone.addPosition(new Position(row, col));

        return zone;
    }

    private static String optStringNull(JSONObject obj, String field) {
        String value = obj.optString(field);
        if (value == null || value.isEmpty())
            return null;
        return value;
    }
}
