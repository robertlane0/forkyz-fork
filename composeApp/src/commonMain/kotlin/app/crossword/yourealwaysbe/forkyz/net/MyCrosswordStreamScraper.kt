
package app.crossword.yourealwaysbe.forkyz.net

import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.GuardianJSONIO
import app.crossword.yourealwaysbe.forkyz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.util.parseJSONObject

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

        if (!URL_RE.matches(url))
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
        private val URL_RE = Regex(
            ".*mycrossword.*\\/[^/]+\\/\\d+",
            RegexOption.IGNORE_CASE,
        )
        private val JSON_MATCH = RegexScrape(
            Regex(
                "window.__remixContext = (\\{.*?\\});",
                RegexOption.IGNORE_CASE,
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

