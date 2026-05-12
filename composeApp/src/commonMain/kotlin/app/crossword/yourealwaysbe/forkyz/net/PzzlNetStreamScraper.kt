package app.crossword.yourealwaysbe.forkyz.net

import java.util.Locale
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.BrainsOnlyIO

/**
 * Downloader for Pzzl.net embedded Crosswords
 *
 * Look for "pzzl.net/app/<id>" and window.pzzl = { xword: { date: '<date>' } }
 * then get pzzl.net/servlet/<id>/netcrossword?date=<date> and it's BrainsOnly
 * text.
 */
class PzzlNetStreamScraper : AbstractStreamScraper() {
    @Throws(Exception::class)
    public override suspend fun parseInput(
        input : Source,
        url : String?,
    ) : Puzzle? {
        val txtURL = getTxtURL(input)

        if (txtURL == null)
            return null

        try {
            getURL(txtURL).use { txtIn ->
                val puz = BrainsOnlyIO.parse(txtIn)
                puz?.source = DEFAULT_SOURCE
                return puz
            }
        } catch (e : Exception) {
            // fall through
        }

        return null
    }

    fun getTxtURL(input : Source) : String? {
        val matchers = arrayOf(ID_MATCH, DATE_MATCH)
        val results = regexScrape(input, matchers)
        val setID = results[0]
        val date = results[1]
        if (setID != null && date != null) {
            return String.format(
                Locale.US,
                TXT_URL_FORMAT,
                setID,
                date,
            )
        } else {
            return null
        }
    }

    companion object {
        private val ID_MATCH = RegexScrape(
            Regex("pzzl.net/app/([^/]*)/"),
            1,
        )
        private val DATE_MATCH = RegexScrape(
            Regex("xword:\\s*\\{\\s*date:\\s*('|\")(\\d+)('|\")"),
            2,
        )

        private const val DEFAULT_SOURCE = "Pzzl.net"

        // args: id, date
        private const val TXT_URL_FORMAT
            = "https://pzzl.net/servlet/%s/netcrossword?date=%s"
    }
}

