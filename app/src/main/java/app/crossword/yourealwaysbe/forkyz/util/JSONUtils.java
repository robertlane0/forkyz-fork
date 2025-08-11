
package app.crossword.yourealwaysbe.forkyz.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {
    public static JSONObject streamToJSON(
        InputStream is
    ) throws IOException, JSONException {
        String json = getJSON(is);
        return json == null ? null : new JSONObject(json);
    }

    public static JSONArray streamToJSONArray(
        InputStream is
    ) throws IOException, JSONException {
        String json = getJSON(is);
        return json == null ? null : new JSONArray(json);
    }

    private static String getJSON(InputStream is) throws IOException {
        try (
            BufferedReader bis = new BufferedReader(
                new InputStreamReader(is)
            )
        ) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bis.readLine()) != null)
                sb.append(line);
            return sb.toString();
        }
    }
}
