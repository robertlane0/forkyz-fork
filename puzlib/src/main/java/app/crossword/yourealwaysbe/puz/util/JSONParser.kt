
package app.crossword.yourealwaysbe.puz.util

import java.io.IOException
import java.io.InputStream

import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Get an org.json.JSONObject from an input stream
 *
 * Would be nice to use Jackson method to avoid duplicate keys causing
 * problems.
 * https://www.yellowbrackets.com/json-with-duplicate-keys/
 * But it is not compatible with Android 19
 */
object JSONParser {
    @JvmStatic
    @Throws(IOException::class, JSONException::class)
    fun parse(inStream : InputStream) : JSONObject {
        return JSONObject(JSONTokener(inStream))
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun parse(json : String) : JSONObject {
        return JSONObject(json)
    }
}
