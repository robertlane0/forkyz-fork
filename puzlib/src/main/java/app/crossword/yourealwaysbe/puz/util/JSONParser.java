
package app.crossword.yourealwaysbe.puz.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Get an org.json.JSONObject from an input stream
 *
 * Uses Jackson method to avoid duplicate keys causing problems.
 * https://www.yellowbrackets.com/json-with-duplicate-keys/
 */
public class JSONParser {
    // go for the most lenient parser available
    public static final ObjectMapper objectMapper
        = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_LEADING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_PLUS_SIGN_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
            .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_TRAILING_DECIMAL_POINT_FOR_NUMBERS)
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_YAML_COMMENTS)
            .build();

    public static JSONObject parse(InputStream is)
            throws IOException, JSONException {
        try {
            return new JSONObject(objectMapper.readValue(is, Map.class));
        } catch (JsonProcessingException e) {
            throw new JSONException(e);
        }
    }

    public static JSONObject parse(String json) throws JSONException {
        try {
            return new JSONObject(objectMapper.readValue(json, Map.class));
        } catch (JsonProcessingException e) {
            throw new JSONException(e);
        }
    }
}
