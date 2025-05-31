
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
        private val JS_URL_FORMAT = "https://cdn2.amuselabs.com/puzzleme/%s"
        private val RAWC_REGEX = RegexScrape(
            Pattern.compile("\"rawc\":\\s*\"([^\"]*)\""),
            1,
        )
        private val JS_URL_SUFFIX_REGEX = RegexScrape(
            Pattern.compile("(?<=\")[^\"]*/js/c-min.js[^\"]*"),
            0,
        )
    }

    override fun parseInput(inputStream : InputStream, url : String) : Puzzle? {
        Log.i(TAG, "Trying PuzzleMe scraper on " + url)
        // so we can reuse
        val is1 = StreamUtils.makeByteArrayInputStream(inputStream)
        val is2 = getStage2InputStream(is1)

        try {
            return getDataData(is2)?.let(this::getFromDataData)
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
    private fun getDataData(inputStream : InputStream) : DataData? {
        val results = regexScrape(
            inputStream,
            arrayOf(RAWC_REGEX, JS_URL_SUFFIX_REGEX),
        )
        val rawc = results[0]
        val jsUrlSuffix = results[1]
        if (rawc == null || jsUrlSuffix == null) {
            return null
        } else {
            return DataData(rawc, jsUrlSuffix)
        }
    }

    /**
     * Try a variety of known rawc decodings
     */
    private fun getFromDataData(dataData : DataData) : Puzzle? {
        val read : (String?) -> Puzzle? = AmuseLabsJSONIO::readPuzzle
        return base64DecodeRawC(dataData.rawc)?.let(read)
            ?: everymanDeobfuscate(dataData)?.let(read)
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
    private fun everymanDeobfuscate(dataData : DataData) : String? {
        val passes = getDeobfuscatePasses(dataData.jsUrlSuffix)
        if (passes?.isEmpty() ?: true)
            return null

        val chars = dataData.rawc.toCharArray()
        passes.forEach { pass ->
            deobfuscatePass(chars, pass.start, pass.step, pass.length)
        }

        return base64DecodeRawC(String(chars))
    }

    private fun deobfuscatePass(
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

    private fun getDeobfuscatePasses(
        jsUrlSuffix : String,
    ) : List<ObfuscatePass>? {
        try {
            val jsURL = String.format(JS_URL_FORMAT.format(jsUrlSuffix))
            val js = getInputStream(jsURL).use { it.reader().readText() }

            // for (r=start;..;r+=step) { .. ?length .. }
            val passRegex
                = "r=(\\d+);[^;]{0,10};r\\+=(\\d+)[^?]{0,50}\\?(\\d+)".toRegex()

            return passRegex.findAll(js).map { m ->
                ObfuscatePass(
                    m.groups[1]?.value?.toInt() ?: 0,
                    m.groups[2]?.value?.toInt() ?: Int.MAX_VALUE,
                    m.groups[3]?.value?.toInt() ?: 0,
                )
            }.toList()
        } catch (e : IOException) {
            return null
        }
    }
}

private data class DataData(
    val rawc : String,
    val jsUrlSuffix: String,
)

private data class ObfuscatePass(
    val start : Int,
    val step : Int,
    val length : Int,
)
