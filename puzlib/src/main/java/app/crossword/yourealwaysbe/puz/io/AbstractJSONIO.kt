
package app.crossword.yourealwaysbe.puz.io

import java.io.InputStream

import org.json.JSONObject

import app.crossword.yourealwaysbe.puz.Puzzle

/**
 * Base class containing convenience functions for JSON IO
 */
abstract class AbstractJSONIO : PuzzleParser {
    @Throws(Exception::class)
    abstract override fun parseInput(inStream : InputStream) : Puzzle?

    companion object {
        /**
         * Reads field from json, returns null if not there or empty val
         */
        internal fun optStringNull(
            json : JSONObject,
            field : String,
        ) : String? {
            val value = json.optString(field)
            if (value == null || isJSONNull(value) || value.isEmpty())
                return null
            return value
        }

        internal fun isJSONNull(obj : Any?) : Boolean {
            return obj == null || JSONObject.NULL == obj
        }
    }
}
