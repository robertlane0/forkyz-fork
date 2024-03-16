
package app.crossword.yourealwaysbe.puz.util;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Get an org.json.JSONObject from an input stream
 *
 * Would be nice to use Jackson method to avoid duplicate keys causing
 * problems.
 * https://www.yellowbrackets.com/json-with-duplicate-keys/
 * But it is not compatible with Android 19
 */
public class JSONParser {
    public static JSONObject parse(InputStream is)
            throws IOException, JSONException {
        return new JSONObject(new JSONTokener(is));
    }

    public static JSONObject parse(String json) throws JSONException {
        return new JSONObject(json);
    }
}
