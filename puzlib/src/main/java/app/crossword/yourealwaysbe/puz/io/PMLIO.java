
package app.crossword.yourealwaysbe.puz.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder.BasicClue;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.util.JSONParser;

/**
 * Read a character stream of JSON data in the format used by the
 * Guardian.
 */
public class PMLIO extends AbstractJSONIO {
    private static final Logger LOG
        = Logger.getLogger(PMLIO.class.getCanonicalName());

    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";

    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("d MMM y", Locale.US);

    private static final Pattern CW_JSON_RE = Pattern.compile(
        "^.*\"starting_puzzle\"\\s*:\\s*(\\{.*\\}).*$"
    );
    private static final int CW_JSON_GRP = 1;

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        ByteArrayInputStream copy = StreamUtils.makeByteArrayInputStream(is);
        Puzzle puz = readFromJSON(copy);
        if (puz == null) {
            copy.reset();
            puz = readFromHTML(copy);
        }
        return puz;
    }

    /**
     * Returns null if failed
     */
    public static Puzzle readFromJSON(InputStream is) {
        try {
            JSONObject json = JSONParser.parse(is);
            return readPuzzleFromJSON(json);
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    public static Puzzle readFromHTML(InputStream is) {
        try {
            String cwJson = readPuzzleJSON(is);
            if (cwJson != null && !cwJson.isEmpty())
                return readPuzzleFromJSON(JSONParser.parse(cwJson));
        } catch (JSONException e) {
            // pass through
        }
        return null;
    }

    public static Puzzle readPuzzle(String jsonString) {
        try {
            JSONObject json = JSONParser.parse(jsonString);
            return readPuzzleFromJSON(json);
        } catch (JSONException e) {
            LOG.severe("Could not read PML JSON: " + e);
            return null;
        }
    }

    /**
     * Return JSON or null if not found
     */
    private static String readPuzzleJSON(InputStream is) {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(is)
            );

            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = CW_JSON_RE.matcher(line);
                if (matcher.find())
                    return matcher.group(CW_JSON_GRP);
            }
        } catch (IOException e) {
            // fall through
        }
        return null;
    }

    /**
     * Read puzzle from PML JSON format
     */
    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException {
        JSONObject gameData = json.getJSONObject("game_data");
        PuzzleBuilder builder = new PuzzleBuilder(getBoxes(gameData))
            .setTitle(optStringNull(json, "name"))
            .setDate(getDate(json));
        addClues(gameData, builder);
        return builder.getPuzzle();
    }

    private static LocalDate getDate(
        JSONObject gameData
    ) throws JSONException {
        try {
            String date = gameData.optString("rdate");
            if (date == null)
                return null;
            return LocalDate.parse(date, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Box[][] getBoxes(JSONObject gameData) throws JSONException {

        int numRows = gameData.getInt("rows");
        int numCols = gameData.getInt("cols");

        Box[][] boxes = new Box[numRows][numCols];

        JSONArray items = gameData.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);

            int position = item.getInt("start") - 1;
            int row = position / numCols;
            int col = position % numCols;

            if (row < 0 || row >= numRows || col < 0 || col >= numCols)
                continue;

            String num = String.valueOf(item.getInt("num"));
            String clueSol = item.getString("answer");
            int length = clueSol.length();

            int drow = 0;
            int dcol = 0;
            if (isAcross(item))
                dcol = 1;
            else
                drow = 1;

            int boxRow = row;
            int boxCol = col;
            for (int j = 0; j < length; j++) {
                if (boxRow >= numRows || boxCol >= numCols)
                    break;

                if (boxes[boxRow][boxCol] == null)
                    boxes[boxRow][boxCol] = new Box();

                if (clueSol != null && j < clueSol.length())
                    boxes[boxRow][boxCol].setSolution(clueSol.charAt(j));

                boxRow += drow;
                boxCol += dcol;
            }

            boxes[row][col].setClueNumber(num);
        }

        return boxes;
    }

    private static void addClues(JSONObject gameData, PuzzleBuilder builder)
            throws JSONException {
        JSONArray items = gameData.getJSONArray("items");
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            addClue(item, builder);
        }
    }

    private static boolean isAcross(JSONObject item) {
        return item.getInt("dir") == 0;
    }

    private static void addClue(
        JSONObject item, PuzzleBuilder builder
    ) throws JSONException {
        String num = String.valueOf(item.getInt("num"));
        String hint = item.getString("clue");
        if (isAcross(item))
            builder.addAcrossClue(ACROSS_LIST, new BasicClue(num, hint));
        else
            builder.addDownClue(DOWN_LIST, new BasicClue(num, hint));
    }
}
