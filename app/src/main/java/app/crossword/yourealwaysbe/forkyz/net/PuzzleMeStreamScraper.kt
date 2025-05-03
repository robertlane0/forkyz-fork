
package app.crossword.yourealwaysbe.forkyz.net

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.IOException
import java.util.regex.Pattern
import java.nio.charset.Charset

import android.util.Base64
import android.util.Log

import app.crossword.yourealwaysbe.forkyz.util.getURLInputStream
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.StreamUtils
import app.crossword.yourealwaysbe.puz.io.AmuseLabsJSONIO

/**
 * Get embedded PuzzleMe puzzles
 *
 * Stage 1: look for e.g.
 *
 * <div
 *      class="pm-embed-div"
 *      data-id="a966960a"
 *      data-set="tortoisemedia-everyman"
 *      ...
 * >
 *
 * Stage 2: get embedded puzzle from
 *  https://cdn2.amuselabs.com/auth0/crossword?id=<id>&set=<set>
 *
 * Stage 3: get rawc from JSON in above file.
 *
 * Stage 4: decode rawc and parse as JPZ
 */
class PuzzleMeStreamScraper() : AbstractStreamScraper() {
    companion object {
        private val TAG = "ForkyzPuzzleMeStrmSrpr"
        private val DEFAULT_SOURCE = "PuzzleMe"
        private val STAGE2_URL_FORMAT
            = "https://cdn2.amuselabs.com/auth0/crossword?id=%s&set=%s"
        private val RAWC_REGEX = RegexScrape(
            Pattern.compile("\"rawc\":\\s*\"([^\"]*)\""),
            1,
        )
    }

    override fun parseInput(inputStream : InputStream, url : String) : Puzzle? {
        Log.i(TAG, "Trying PuzzleMe scraper on " + url)
        // so we can reuse
        val is1 = StreamUtils.makeByteArrayInputStream(inputStream)
        val is2 = getStage2InputStream(is1)

        try {
            return getRawC(is2)?.let(this::getFromRawC)
                ?.let { puz ->
                    with (puz) {
                        if (getSource() == null)
                            setSource(DEFAULT_SOURCE)
                    }
                    return puz
                }
        } catch (e : IOException) {
            Log.i(TAG, "Could not scrape PuzzleMe: " + e)
            return null
        }
    }

    /**
     * Do Stage 1 return input stream
     *
     * @return either reset original if failed or new input stream to
     * stage 2.
     */
    private fun getStage2InputStream(
        bis : ByteArrayInputStream
    ) : InputStream {
        val doc = getDocument(bis)
        bis.reset()

        val div = doc.selectFirst(".pm-embed-div")

        val cwID = div?.attribute("data-id")?.value
        val cwSet = div?.attribute("data-set")?.value

        if (cwID == null || cwSet == null)
            return bis

        return getInputStream(STAGE2_URL_FORMAT.format(cwID, cwSet))?.use {
            StreamUtils.makeByteArrayInputStream(it)
        } ?: bis
    }

    /**
     * Get rawc field from JSON in inputStream
     *
     * @return field value or null if not found
     */
    private fun getRawC(inputStream : InputStream) : String? {
        return regexScrape(inputStream, RAWC_REGEX)
    }

    /**
     * Try a variety of known rawc decodings
     */
    private fun getFromRawC(rawc : String) : Puzzle? {
        val read : (String?) -> Puzzle? = AmuseLabsJSONIO::readPuzzle
        return base64DecodeRawC(rawc)?.let(read)
            ?: everymanDeObfuscate(rawc)?.let(read)
            ?: everymanDeObfuscate2(rawc)?.let(read)
    }

    /**
     * Try to decode as plain Base64
     */
    private fun base64DecodeRawC(rawc : String) : String? {
        try {
            return String(
                Base64.decode(rawc, Base64.DEFAULT),
                Charset.forName("UTF-8"),
            )
        } catch (e : IllegalArgumentException) {
            return null
        }
    }

    /**
     * Try to deobfuscate using format used in Everyman crossword
     */
    private fun everymanDeObfuscate(rawc : String) : String? {
        val chars = rawc.toCharArray()

        everymanPhase(chars, 27, 39, 14)
        everymanPhase(chars, 41, 41, 12)
        everymanPhase(chars, 21, 51, 2)
        everymanPhase(chars, 23, 51, 2)
        everymanPhase(chars, 0, 49, 4)
        everymanPhase(chars, 4, 36, 17)
        everymanPhase(chars, 25, 51, 2)

        return base64DecodeRawC(String(chars))
    }

    private fun everymanDeObfuscate2(rawc : String) : String? {
        val chars = rawc.toCharArray()

        everymanPhase(chars, 56, 56, 3)
        everymanPhase(chars, 31, 46, 13)
        everymanPhase(chars, 14, 42, 17)
        everymanPhase(chars, 7, 52, 7)
        everymanPhase(chars, 0, 52, 7)
        everymanPhase(chars, 48, 51, 8)
        everymanPhase(chars, 44, 55, 4)

        return base64DecodeRawC(String(chars))
    }

    private fun everymanPhase(
        rawc : CharArray,
        start : Int,
        increment : Int,
        swapLen : Int,
    ) {
        var pos = start
        while (pos < rawc.size) {
            var bottom = pos
            var top = pos + swapLen - 1
            if (top >= rawc.size)
                top = rawc.size - 1
            val realSwapLen = top - bottom + 1

            while (bottom < top) {
                val tmp = rawc[top]
                rawc[top] = rawc[bottom]
                rawc[bottom] = tmp

                bottom += 1
                top -= 1
            }

            pos += realSwapLen + increment
        }
    }
}
