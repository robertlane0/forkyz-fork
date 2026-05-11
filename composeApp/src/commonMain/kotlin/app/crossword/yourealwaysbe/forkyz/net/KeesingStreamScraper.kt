
package app.crossword.yourealwaysbe.forkyz.net

import java.util.Locale
import kotlinx.io.IOException
import kotlinx.io.Source
import kotlinx.io.readLine

import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.KeesingXMLIO
import app.crossword.yourealwaysbe.puz.util.JSONException
import app.crossword.yourealwaysbe.puz.util.parseJSONObject

/**
 * Downloader for Keesing.com embedded crosswords
 *
 * First look for web.keesing.com and also a data-customerid and a
 * data-puzzleid.
 *
 * Then go to the following, where puzzlesetid is puzzleid_customerid
 *
 * https://web.keesing.com/Content/GetPuzzleInfo/?clientid=<customerid>&puzzleid=<puzzlesetid>
 *
 * Note, De Standaard adds _yyyymmdd to the end of the URL. Try without (it
 * works) because i don't know how to reliably get the date from the webpage.
 *
 * Then gets puzzle from
 *
 * https://web.keesing.com/content/getxml?clientid=<customerid>&puzzleid=<puzzleid>"
 *
 */
class KeesingStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    override suspend fun parseInput(
        input : Source,
        url : String?
    ) : Puzzle? {
        // get customerID and puzzleID
        val matchers = arrayOf<RegexScrape>(CUSTOMER_ID_MATCH, PUZZLE_ID_MATCH)
        val results = regexScrape(input, matchers)
        val customerID = results[0]
        val puzzleID = results[1]
        if (customerID == null || puzzleID == null)
            return null

        val puzzleSetID = String.format(
            Locale.US,
            PUZZLE_SET_ID_FORMAT,
            puzzleID,
            customerID,
        )

        return getPuzzleURL(customerID, puzzleSetID)?.let { getPuzzle(it) }
    }

    private suspend fun getPuzzle(puzzleURL : String) : Puzzle? {
        try {
            getURL(puzzleURL).use { input ->
                val puz = KeesingXMLIO.readPuzzle(input)
                if (puz != null) {
                    if (puz.source?.isEmpty() ?: true)
                        puz.source = DEFAULT_SOURCE
                }
                return puz
            }
        } catch (e : Exception) {
            return null
        }
    }

    /**
     * Fetch puzzle ID using passed data then make puzzle URL from it.
     *
     * Goes online.
     */
    suspend fun getPuzzleURL(
        customerID : String,
        puzzleSetID : String,
    ) : String? {
        val idUrl = String.format(
            Locale.US,
            ID_URL_FORMAT,
            customerID,
            puzzleSetID,
        )
        try {
            getURL(idUrl).use { input ->
                val rawJson = StringBuilder()
                var line : String?
                while ((input.readLine().also { line = it }) != null)
                    rawJson.append(line + "\n")

                val json = parseJSONObject(rawJson.toString())
                val puzzleID = json.getString("puzzleID")
                return String.format(
                    Locale.US,
                    XML_URL_FORMAT,
                    customerID,
                    puzzleID,
                )
            }
        } catch (e : JSONException) {
            AppLogger.e(TAG, "Could not read Keesing JSON: " + e)
            return null
        } catch (e : IOException) {
            AppLogger.e(TAG, "Could not read Keesing JSON: " + e)
            return null
        }
    }

    companion object {
        private const val TAG = "ForkyzKeesingStrmScrp"

        private val CUSTOMER_ID_MATCH
            = RegexScrape(Regex("data-customerid=\"([^\"]*)\""), 1)
        private val PUZZLE_ID_MATCH = RegexScrape(
            Regex("data-puzzleid=\"([^\"]*)\""),
            1,
        )

        // puzzleid_customerid
        private const val PUZZLE_SET_ID_FORMAT = "%s_%s"
        private val ID_URL_FORMAT = (
            "https://web.keesing.com/Content/GetPuzzleInfo/?"
            + "clientid=%s&puzzleid=%s"
        )
        private const val XML_URL_FORMAT =
            "https://web.keesing.com/content/getxml?clientid=%s&puzzleid=%s"

        private const val DEFAULT_SOURCE = "Keesing"
    }
}

