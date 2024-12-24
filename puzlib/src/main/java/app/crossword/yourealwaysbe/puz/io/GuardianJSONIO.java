
package app.crossword.yourealwaysbe.puz.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Clue;
import app.crossword.yourealwaysbe.puz.Position;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.Zone;
import app.crossword.yourealwaysbe.puz.util.JSONParser;
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils;

/**
 * Read a character stream of JSON data in the format used by the
 * Guardian.
 */
public class GuardianJSONIO extends AbstractJSONIO {
    private static final Logger LOG
        = Logger.getLogger(GuardianJSONIO.class.getCanonicalName());

    private static final String ACROSS_LIST = "Across";
    private static final String DOWN_LIST = "Down";

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
            Document doc = Jsoup.parse(is, null, "");
            String cwJson = doc.select(".js-crossword")
                               .attr("data-crossword-data");
            if (!cwJson.isEmpty())
                return readPuzzleFromJSON(JSONParser.parse(cwJson));
        } catch (IOException | JSONException e) {
            // pass through
        }
        return null;
    }

    public static Puzzle readPuzzle(String jsonString) {
        try {
            JSONObject json = JSONParser.parse(jsonString);
            return readPuzzleFromJSON(json);
        } catch (JSONException e) {
            LOG.severe("Could not read Guardian JSON: " + e);
            return null;
        }
    }

    /**
     * Read puzzle from Guardian JSON format
     *
     * Does not set source or support url (this method may be moved to
     * puzlib/io at some point).
     */
    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException {
        PuzzleBuilder builder = new PuzzleBuilder(getBoxes(json))
            .setTitle(optStringNull(json, "name"));

        JSONObject creator = json.optJSONObject("creator");
        if (creator != null)
            builder.setAuthor(optStringNull(creator, "name"));

        if (json.has("date")) {
            long epochMillis = json.getLong("date");
            builder.setDate(
                LocalDate.ofEpochDay(epochMillis / (1000 * 60 * 60 * 24))
            );
        }

        addClues(json, builder);

        return builder.getPuzzle();
    }

    private static Box[][] getBoxes(JSONObject json) throws JSONException {

        JSONObject dimensions = json.getJSONObject("dimensions");
        int numRows = dimensions.getInt("rows");
        int numCols = dimensions.getInt("cols");

        Box[][] boxes = new Box[numRows][numCols];

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            JSONObject position = entry.getJSONObject("position");
            int x = position.getInt("x");
            int y = position.getInt("y");

            if (x < 0 || x >= numCols || y < 0 || y >= numRows)
                continue;

            String num = String.valueOf(entry.getInt("number"));
            String clueSol = optStringNull(entry, "solution");
            String direction = entry.getString("direction");
            int length = entry.getInt("length");

            int dx = 0;
            int dy = 0;
            if (direction.equals("across"))
                dx = 1;
            else
                dy = 1;

            int boxX = x;
            int boxY = y;
            for (int j = 0; j < length; j++) {
                if (boxX >= numCols || boxY >= numRows)
                    break;

                if (boxes[boxY][boxX] == null)
                    boxes[boxY][boxX] = new Box();

                if (clueSol != null && j < clueSol.length())
                    boxes[boxY][boxX].setSolution(clueSol.charAt(j));

                boxX += dx;
                boxY += dy;
            }

            boxes[y][x].setClueNumber(num);
        }

        return boxes;
    }

    private static void addClues(JSONObject json, PuzzleBuilder builder)
            throws JSONException {
        Map<String, ClueInfo> info = getCluesInfo(json, builder);

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            addClue(entry, info, builder);
        }
    }

    private static void addClue(
        JSONObject entry,
        Map<String, ClueInfo> info,
        PuzzleBuilder builder
    ) {
        String id = entry.getString("id");
        String num = entry.getString("humanNumber");
        boolean across = entry.getString("direction").equals("across");
        String hint = entry.getString("clue");

        JSONArray group = entry.getJSONArray("group");
        Zone zone = new Zone();

        Map<Integer, String> separators = new HashMap<>();

        if (group.length() > 0 && id.equals(group.getString(0))) {
            for (int i = 0; i < group.length(); i++) {
                String otherId = group.getString(i);
                ClueInfo otherInfo = info.get(otherId);
                if (otherInfo != null) {
                    int sepOffset = zone.size();
                    Map<Integer, String> otherSeps = otherInfo.separators();
                    if (otherSeps != null) {
                        for (int pos : otherSeps.keySet()) {
                            separators.put(sepOffset + pos, otherSeps.get(pos));
                        }
                    }

                    zone.appendZone(otherInfo.zone());
                }
            }
        }

        String listName = across ? ACROSS_LIST : DOWN_LIST;
        int index = builder.getNextClueIndex(listName);

        builder.addClue(
            new Clue(listName, index, num, null, hint, zone, separators)
        );
    }

    /**
     * Gets map from clue id to info
     *
     * Info is: zone on board, separators for zone
     */
    private static Map<String, ClueInfo> getCluesInfo(
        JSONObject json, PuzzleBuilder builder
    ) {
        Map<String, ClueInfo> info = new HashMap<>();

        JSONArray entries = json.getJSONArray("entries");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);

            String id = entry.getString("id");

            JSONObject position = entry.getJSONObject("position");
            int x = position.getInt("x");
            int y = position.getInt("y");
            boolean across = entry.getString("direction").equals("across");

            Puzzle puz = builder.getPuzzle();
            Position start = new Position(y, x);

            Zone zone = across
                ? PuzzleUtils.getAcrossZone(puz, start)
                : PuzzleUtils.getDownZone(puz, start);

            Map<Integer, String> separators = getSeparators(entry);

            info.put(id, new ClueInfo(zone, separators));
        }

        return info;
    }

    private static Map<Integer, String> getSeparators(JSONObject entry) {
        JSONObject sepObj = entry.optJSONObject("separatorLocations");
        if (sepObj == null)
            return null;

        Map<Integer, String> separators = new HashMap<>();

        sepObj.keys().forEachRemaining(sep -> {
            if (sep == null || sep.isEmpty())
                return;

            if (",".equals(sep))
                sep = " ";

            JSONArray positions = sepObj.optJSONArray(sep);
            if (positions == null)
                return;

            for (int i = 0; i < positions.length(); i++) {
                Integer pos = positions.optInt(i);
                if (pos != null)
                    separators.put(pos, sep);
            }
        });

        return separators;
    }

    private static record ClueInfo(
        Zone zone,
        Map<Integer, String> separators
    ) { }
}
