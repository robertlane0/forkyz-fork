
package app.crossword.yourealwaysbe.forkyz.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object JSONUtils {
    @Throws(IOException::class, JSONException::class)
    fun streamToJSON(inputStream : InputStream) : JSONObject {
        val json = getJSON(inputStream)
        return JSONObject(json)
    }

    @Throws(IOException::class, JSONException::class)
    fun streamToJSONArray(inputStream : InputStream) : JSONArray {
        val json = getJSON(inputStream)
        return JSONArray(json)
    }

    @Throws(IOException::class)
    private fun getJSON(inputStream : InputStream) : String {
        BufferedReader(InputStreamReader(inputStream)).use { bis ->
            val sb = StringBuilder()
            var line : String?
            while ((bis.readLine().also { line = it }) != null)
                sb.append(line)
            return sb.toString()
        }
    }
}
