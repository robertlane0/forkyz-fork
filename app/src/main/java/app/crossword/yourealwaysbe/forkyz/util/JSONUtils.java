
package app.crossword.yourealwaysbe.forkyz.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils {
    public static JSONObject streamToJSON(
        InputStream is
    ) throws IOException, JSONException {
        try (
            BufferedReader bis = new BufferedReader(
                new InputStreamReader(is)
            )
        ) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bis.readLine()) != null)
                sb.append(line);
            return new JSONObject(sb.toString());
        }
    }
}
