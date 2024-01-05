
package app.crossword.yourealwaysbe.puz.io;

import java.io.InputStream;

import org.json.JSONObject;

import app.crossword.yourealwaysbe.puz.Puzzle;

/**
 * Base class containing convenience functions for JSON IO
 */
public abstract class AbstractJSONIO implements PuzzleParser {

    @Override
    public abstract Puzzle parseInput(InputStream is) throws Exception;

    /**
     * Reads field from json, returns null if not there or empty val
     */
    protected static String optStringNull(JSONObject json, String field) {
        String value = json.optString(field);
        if (isJSONNull(value) || value.isEmpty())
            return null;
        return value;
    }

    protected static boolean isJSONNull(Object obj) {
        return obj == null || JSONObject.NULL.equals(obj);
    }
}
