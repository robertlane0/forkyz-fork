
package app.crossword.yourealwaysbe.puz.io;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.puz.util.JSONParser;

/**
 * Read a character stream of JSON data in the format used by the
 * Amuse Labs.
 */
public class AmuseLabsJSONIO extends AbstractJSONIO {
    private static final Logger LOG
        = Logger.getLogger(AmuseLabsJSONIO.class.getCanonicalName());

    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class AmuseLabsFormatException extends Exception {
        private static final long serialVersionUID = 2649433759853212815L;
        public AmuseLabsFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = JSONParser.parse(is);
            return readPuzzleFromJSON(json);
        } catch (AmuseLabsFormatException | JSONException e) {
            LOG.severe("Could not read Amuse Labs JSON: " + e);
            return null;
        }
    }

    public static Puzzle readPuzzle(String jsonString) {
        try {
            JSONObject json = JSONParser.parse(jsonString);
            return readPuzzleFromJSON(json);
        } catch (AmuseLabsFormatException | JSONException e) {
            LOG.severe("Could not read Amuse Labs JSON: " + e);
            return null;
        }
    }

    /**
     * Read puzzle from Amuse Labs JSON format
     */
    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException, AmuseLabsFormatException {
        try {
            PuzzleBuilder builder = new PuzzleBuilder(getBoxes(json));

            builder.setTitle(optStringNull(json, "title"))
                .setAuthor(optStringNull(json, "author"))
                .setCopyright(optStringNull(json, "copyright"))
                .setSource(optStringNull(json, "publisher"))
                .setCompletionMessage(optStringNull(json, "endMessage"));

            if (json.has("publishTime")) {
                long epochMillis = json.getLong("publishTime");
                builder.setDate(
                    LocalDate.ofEpochDay(epochMillis / (1000 * 60 * 60 * 24))
                );
            }

            addClues(json, builder);

            return builder.getPuzzle();
        } catch (IllegalArgumentException e) {
            throw new AmuseLabsFormatException("Could not set grid boxes from data file: " + e.getMessage());
        }
    }

    private static Box[][] getBoxes(JSONObject json)
            throws JSONException, AmuseLabsFormatException {

        int numRows = json.getInt("h");
        int numCols = json.getInt("w");

        Box[][] boxes = new Box[numRows][numCols];

        JSONArray cols = json.getJSONArray("box");
        for (int col = 0; col < cols.length(); col++) {
            JSONArray rows = cols.getJSONArray(col);
            for (int row = 0; row < rows.length(); row++) {
                String entryString = rows.getString(row);

                boolean isBox = entryString.isEmpty()
                    || entryString.charAt(0) != '\0';

                if (isBox) {
                    boxes[row][col] = new Box();
                    if (!entryString.isEmpty())
                        boxes[row][col].setSolution(entryString);
                }
            }
        }

        cols = json.optJSONArray("clueNums");
        if (cols != null) {
            for (int col = 0; col < cols.length(); col++) {
                JSONArray rows = cols.getJSONArray(col);
                for (int row = 0; row < rows.length(); row++) {
                    int clueNum = rows.optInt(row, 0);

                    if (clueNum > 0) {
                        if (boxes[row][col] == null) {
                            boxes[row][col] = new Box();
                        }
                        boxes[row][col].setClueNumber(String.valueOf(clueNum));
                    }
                }
            }
        }

        JSONArray cellInfos = json.optJSONArray("cellInfos");
        if (cellInfos != null) {
            for (int i = 0; i < cellInfos.length(); i++) {
                JSONObject cellInfo = cellInfos.getJSONObject(i);

                int row = cellInfo.getInt("y");
                int col = cellInfo.getInt("x");
                boolean circled = cellInfo.optBoolean("isCircled");

                if (circled) {
                    if (boxes[row][col] == null) {
                        boxes[row][col] = new Box();
                    }
                    boxes[row][col].setShape(Box.Shape.CIRCLE);
                }
            }
        }

        return boxes;
    }

    private static void addClues(JSONObject json, PuzzleBuilder builder)
            throws JSONException {
        JSONArray entries = json.getJSONArray("placedWords");

        if (entries == null)
            return;

        boolean wordLengthsEnabled
            = json.optBoolean("wordLengthsEnabled", false);

        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            int num = entry.getInt("clueNum");
            if (num > 0) {
                String number = String.valueOf(num);
                boolean across = entry.getBoolean("acrossNotDown");
                String clue = entry.getJSONObject("clue").getString("clue");

                Map<Integer, String> separators = null;

                if (wordLengthsEnabled) {
                    separators = getSeparators(entry);
                    String enumeration = getEnumeration(entry);
                    if (enumeration != null && !clue.endsWith(enumeration))
                        clue += " " + enumeration;
                }

                String listName = across ? ACROSS_LIST : DOWN_LIST;
                Zone zone = across
                    ? builder.getAcrossZone(number)
                    : builder.getDownZone(number);
                int index = builder.getNextClueIndex(listName);
                builder.addClue(new Clue(
                    listName,
                    index,
                    number,
                    null,
                    clue,
                    zone,
                    separators
                ));
            }
        }
    }

    private static String getEnumeration(JSONObject clue) {
        JSONArray wordLens = clue.optJSONArray("wordLens");
        if (wordLens == null)
            return null;

        StringBuilder enumeration = new StringBuilder();
        enumeration.append("(");
        for (int i = 0; i < wordLens.length(); i++) {
            if (i > 0)
                enumeration.append(",");
            enumeration.append(String.valueOf(wordLens.get(i)));
        }
        enumeration.append(")");

        return enumeration.toString();
    }

    private static Map<Integer, String> getSeparators(JSONObject clue) {
        JSONArray wordLens = clue.optJSONArray("wordLens");
        if (wordLens == null)
            return null;

        Map<Integer, String> separators = new HashMap<>();
        int totalLen = 0;

        // length - 1 to remove last marker
        for (int i = 0; i < wordLens.length() - 1; i++) {
            int len = wordLens.optInt(i, -1);
            if (len < 0)
                return null;

            totalLen += len;
            separators.put(totalLen, " ");
        }

        return separators;
    }
}
