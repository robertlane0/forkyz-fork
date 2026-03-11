
package app.crossword.yourealwaysbe.puz.io;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import app.crossword.yourealwaysbe.puz.Box;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleBuilder;
import app.crossword.yourealwaysbe.puz.util.JSONParser;
import app.crossword.yourealwaysbe.puz.util.PuzzleUtils;

/**
 * Puzzle format used by Smart Games
 *
 * "data": {
 *   "attributes": {
 *     "author": "..",
 *     "publicationDate": "yyyy-mm-dd",
 *     "config": {
 *       "board": "AA#A\r\nA#AA\r\n#AAA...", // # block \r\n splits rows
 *       "entries": {
 *         "across": {
 *           "1": {
 *             "col": x, // 0-indexed
 *             "row": y, // 0-indexed
 *             "clue": "hint",
 *           },
 *           ..
 *         },
 *         "down": {
 *           ..
 *         },
 *       },
 *       "highlighted_cells": [] // dunno format, ignored for now
 *     },
 *   }
 * }
 */
public class SmartGamesJSONIO extends AbstractJSONIO {
    private static final Logger LOG
        = Logger.getLogger(SmartGamesJSONIO.class.getCanonicalName());

    private static final DateTimeFormatter DATE_FORMATTER
        = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String ACROSS_LIST = DefaultListNames.ACROSS_LIST;
    private static final String DOWN_LIST = DefaultListNames.DOWN_LIST;

    /**
     * An unfancy exception indicating error while parsing
     */
    public static class SmartGamesJSONFormatException extends Exception {
        private static final long serialVersionUID = 1841133259356227885L;
        public SmartGamesJSONFormatException(String msg) { super(msg); }
    }

    @Override
    public Puzzle parseInput(InputStream is) throws Exception {
        return readPuzzle(is);
    }

    public static Puzzle readPuzzle(InputStream is) throws IOException {
        try {
            JSONObject json = JSONParser.parse(is);
            return readPuzzleFromJSON(json);
        } catch (SmartGamesJSONFormatException | JSONException e) {
            LOG.severe("Could not read SmartGames JSON: " + e);
            return null;
        }
    }

    public static Puzzle readPuzzle(String jsonString) {
        try {
            JSONObject json = JSONParser.parse(jsonString);
            return readPuzzleFromJSON(json);
        } catch (SmartGamesJSONFormatException | JSONException e) {
            LOG.severe("Could not read SmartGames JSON: " + e);
            return null;
        }
    }

    private static Puzzle readPuzzleFromJSON(
        JSONObject json
    ) throws JSONException, SmartGamesJSONFormatException {
        try {
            JSONObject data = json.optJSONObject("data");
            if (data == null)
                data = json;
            JSONObject attributes = data.optJSONObject("attributes");
            if (attributes != null)
                data = attributes;

            PuzzleBuilder builder = new PuzzleBuilder(getBoxes(data));
            addMetaData(data, builder);
            addClues(data, builder);

            return builder.puzzle;
        } catch (IllegalArgumentException e) {
            throw new SmartGamesJSONFormatException(
                "Could not set grid boxes from data file: " + e.getMessage()
            );
        }
    }

    private static Box[][] getBoxes(
        JSONObject json
    ) throws JSONException, SmartGamesJSONFormatException {
        JSONObject config = json.getJSONObject("config");
        String[] cells = config.getString("board").split("\r\n");

        int numRows = cells.length;
        int numCols = Arrays.stream(cells)
            .map(r -> r.length())
            .max(Comparator.naturalOrder())
            .orElse(0);

        if (numCols == 0) {
            throw new SmartGamesJSONFormatException(
                "Grid has a bad shape: "
                + numRows
                + " rows and "
                + numCols
                + " cols."
            );
        }

        Box[][] boxes = new Box[numRows][];

        for (int row = 0; row < numRows; row++) {
            boxes[row] = new Box[numCols];
            for (int col = 0; col < cells[row].length(); col++) {
                char cell = cells[row].charAt(col);
                if (Character.isAlphabetic(cell)) {
                    boxes[row][col] = new Box();
                    boxes[row][col].setSolution(cell);
                }
            }
        }

        return boxes;
    }

    private static void addMetaData(
        JSONObject json, PuzzleBuilder builder
    ) throws JSONException {
        if (json != null) {
            builder.setAuthor(optStringNull(json, "author"));
            try {
                String date = optStringNull(json, "publicationDate");
                if (date != null)
                    builder.setDate(LocalDate.parse(date, DATE_FORMATTER));
            } catch (DateTimeParseException e) {
                // oh well
            }
        }
    }

    private static void addClues(
        JSONObject json, PuzzleBuilder builder
    ) throws JSONException {
        JSONObject config = json.getJSONObject("config");
        JSONObject clues = config.getJSONObject("entries");
        addClueList(clues.optJSONObject("across"), true, builder);
        addClueList(clues.optJSONObject("down"), false, builder);
    }

    private static void addClueList(
        JSONObject clues,
        boolean across,
        PuzzleBuilder builder
    ) throws JSONException {
        if (clues == null)
            return;

        clues.keySet()
            .stream()
            .sorted(PuzzleUtils::compareStringNumbers)
            .forEach(number -> {
                JSONObject clue = clues.getJSONObject(number);
                String hint = clue.getString("clue");
                int row = clue.getInt("row");
                int col = clue.getInt("col");

                builder.setBoxClueNumber(row, col, number);
                if (across) {
                    builder.addAcrossClue(ACROSS_LIST, number, hint);
                } else
                    builder.addDownClue(DOWN_LIST, number, hint);
            });
    }
}
