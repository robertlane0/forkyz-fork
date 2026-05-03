
package app.crossword.yourealwaysbe.forkyz.net

import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.GuardianJSONIO
import app.crossword.yourealwaysbe.puz.util.JSONException
import app.crossword.yourealwaysbe.puz.util.parseJSONObject

/**
 * Downloader for MyCrossword.co.uk embedded Crosswords
 *
 * Look for puzzle type/number in URL
 *
 * https://mycrossword.co.uk/<type>/<num>
 *
 * Get puzzle in Guardian JSON format from
 *
 * https://mycrossword.co.uk/api/crossword/getpublishedbytypenum?crosswordType=<type>&crosswordNum=<num>
 */
class MyCrosswordStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        if (url == null)
            return null

        val m = URL_RE.matcher(url)
        if (!m.matches())
            return null

        try {
            getURL(url).use { puzIn ->
                val jsonString = regexScrape(puzIn, JSON_MATCH)
                if (jsonString == null)
                    return null

                val json = parseJSONObject(jsonString)
                var data = json.optJSONObject("state")
                if (data == null)
                    return null
                data = data.optJSONObject("loaderData")
                if (data == null)
                    return null
                data = data.optJSONObject("routes/cryptic.\$crosswordId")
                if (data == null)
                    return null
                data = data.optJSONObject("crossword")
                if (data == null)
                    return null
                data = data.optJSONObject("data")
                if (data == null)
                    return null

                val puz = GuardianJSONIO.readPuzzle(data)
                if (puz.source == null)
                    puz.source = DEFAULT_SOURCE
                return puz
            }
        } catch (e : IOException) {
            /* fall through */
        } catch (e : JSONException) {
            /* fall through */
        }

        return null
    }

    companion object {
        private val URL_RE : Pattern = Pattern.compile(
            ".*mycrossword.*\\/[^/]+\\/\\d+",
            Pattern.CASE_INSENSITIVE,
        )
        private val JSON_MATCH = RegexScrape(
            Pattern.compile(
                "window.__remixContext = (\\{.*?\\});",
                Pattern.CASE_INSENSITIVE,
            ),
            1,
        )

        private val DATA_URL_FMT = (
            "https://mycrossword.co.uk/api/crossword/getpublishedbytypenum?"
            + "crosswordType=%s&crosswordNum=%s"
        )

        private const val DEFAULT_SOURCE = "MyCrossword.co.uk"
    }
}

