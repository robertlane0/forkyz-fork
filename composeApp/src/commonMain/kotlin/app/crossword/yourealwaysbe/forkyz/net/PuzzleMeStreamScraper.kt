
package app.crossword.yourealwaysbe.forkyz.net

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.regex.Pattern
import kotlin.collections.ArrayDeque
import kotlin.io.encoding.Base64

import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.org.json.JSONException

import app.crossword.yourealwaysbe.forkyz.util.getURLInputStream
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.AmuseLabsJSONIO
import app.crossword.yourealwaysbe.puz.io.StreamUtils
import app.crossword.yourealwaysbe.puz.util.JSONParser

private val DEOBFUSCATE_TIMEOUT = 30000L
private val KEY_LEN = 7
private val MAX_KEY_VALUE = 20

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
 * or from URL similar to Stage 2
 *
 * Stage 2: get embedded puzzle from
 *  https://cdn2.amuselabs.com/puzzleme/crossword?id=<id>&set=<set>
 *
 * Stage 3: get rawc from JSON in above file.
 *
 * Stage 4: decode rawc and parse as JPZ
 */
class PuzzleMeStreamScraper() : AbstractStreamScraper() {
    companion object {
        private val TAG = "ForkyzPuzzleMeStrmSrpr"
        private val DEFAULT_SOURCE = "PuzzleMe"
        private val DEFAULT_CDN = "cdn2"
        private val DEFAULT_SUBDIR = "puzzleme"
        private val STAGE2_URL_FORMAT
            = "https://%s.amuselabs.com/%s/crossword?id=%s&set=%s"
        private val STAGE2_URL_RE = Pattern.compile(
            "https://(cdn.).amuselabs.com/([^/]*)/crossword[^\"]*"
                + "id=([^&\"]*)[^\"]*set=([^&\"]*).*"
        )
        private val CDN_REGEX = RegexScrape(STAGE2_URL_RE, 1)
        private val SUBDIR_REGEX = RegexScrape(STAGE2_URL_RE, 2)
        private val CW_ID_REGEX = RegexScrape(STAGE2_URL_RE, 3)
        private val CW_SET_REGEX = RegexScrape(STAGE2_URL_RE, 4)

        private val STAGE2_URL_ALT_RE = Pattern.compile(
            "https://(cdn.).amuselabs.com/",
        )
        private val CDN_ALT_REGEX = RegexScrape(STAGE2_URL_ALT_RE, 1)
        // quotey
        private val CW_ID_ALT_REGEX = RegexScrape(
            Pattern.compile("data-id=\\\\*\"([^\"\\\\]*)\\\\*\""),
            1,
        )
        private val CW_SET_ALT_REGEX = RegexScrape(
            Pattern.compile("data-set=\\\\*\"([^\"\\\\]*)\\\\*\""),
            1,
        )

        private val RAWC_REGEX = RegexScrape(
            Pattern.compile("\"rawc\":\\s*\"([^\"]*)\""),
            1,
        )
    }

    override fun parseInput(
        inputStream : InputStream,
        url : String?,
    ) : Puzzle? {
        AppLogger.i(TAG, "Trying PuzzleMe scraper on " + url)
        // so we can reuse
        val is1 = StreamUtils.makeByteArrayInputStream(inputStream)
        val is2 = getStage2InputStream(is1)

        try {
            return getRawc(is2)?.let(this::getFromRawc)
                ?.let { puz ->
                    with (puz) {
                        if (source == null)
                            source = DEFAULT_SOURCE
                    }
                    return puz
                }
        } catch (e : IOException) {
            AppLogger.i(TAG, "Could not scrape PuzzleMe: " + e)
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

        if (doc == null)
            return bis

        val div = doc.selectFirst(".pm-embed-div")

        var cwID = div?.attribute("data-id")?.value
        var cwSet = div?.attribute("data-set")?.value
        var cdn : String? = DEFAULT_CDN
        var subdir : String? = DEFAULT_SUBDIR
        if (cwID == null || cwSet == null) {
            val results = regexScrape(
                bis,
                arrayOf(CDN_REGEX, SUBDIR_REGEX, CW_ID_REGEX, CW_SET_REGEX),
            )
            bis.reset()

            if (results.size == 4) {
                cdn = results[0]
                subdir = results[1]
                cwID = results[2]
                cwSet = results[3]
            }

            if (
                cdn == null
                || subdir == null
                || cwID == null
                || cwSet == null
            ) {
                val results = regexScrape(
                    bis,
                    arrayOf(
                        CDN_ALT_REGEX,
                        CW_ID_ALT_REGEX,
                        CW_SET_ALT_REGEX,
                    ),
                )
                bis.reset()

                if (results.size == 3) {
                    cdn = results[0]
                    subdir = DEFAULT_SUBDIR
                    cwID = results[1]
                    cwSet = results[2]
                }
            }
        }
        if (cdn == null || subdir == null || cwID == null || cwSet == null) {
            return bis
        } else {
            return getInputStream(
                STAGE2_URL_FORMAT.format(cdn, subdir, cwID, cwSet),
            ).use {
                StreamUtils.makeByteArrayInputStream(it)
            }
        }
    }

    /**
     * Get rawc field from JSON in inputStream
     *
     * @return field value or null if not found
     */
    private fun getRawc(inputStream : InputStream) : String? {
        return regexScrape(inputStream, RAWC_REGEX)
    }

    /**
     * Try a variety of known rawc decodings
     */
    private fun getFromRawc(rawc : String) : Puzzle? {
        val read : (String?) -> Puzzle? = AmuseLabsJSONIO::readPuzzle
        return base64Decode(rawc)?.let(read)
            ?: deobfuscateRawc(rawc).let(read)
    }

    /**
     * Try to decode as plain Base64
     */
    private fun base64Decode(s : String) : String? {
        try {
            return String(
                Base64.Default.decode(s),
                Charset.forName("UTF-8"),
            )
        } catch (e : IllegalArgumentException) {
            return null
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Deobfuscation code converted from xword-dl
    // License: https://github.com/thisisparker/xword-dl/blob/main/LICENSE
    // which in turn was adapted from Kotwords
    // License: https://github.com/jpd236/kotwords/blob/master/LICENSE

    /**
     * Determine if the given key prefix could be valid.
     *
     * Simulates reversing chunks of the string and validates that:
     * 1. Base64 portions decode successfully
     * 2. Decoded bytes contain only valid UTF-8 (no invalid continuation bytes)
     */
    private fun isValidKeyPrefix(
        rawc : String,
        keyPrefix : List<Int>,
        spacing : Int,
    ): Boolean {
        return try {
            var pos = 0

            while (pos < rawc.length) {
                val startPos = pos
                var keyIndex = 0

                val chunk = mutableListOf<String>()
                // Assemble a chunk by reversing segments of specified lengths
                while (keyIndex < keyPrefix.size && pos < rawc.length) {
                    val chunkLength = minOf(
                        keyPrefix[keyIndex],
                        rawc.length - pos,
                    )
                    chunk.add(rawc.substring(pos, pos + chunkLength).reversed())
                    pos += chunkLength
                    keyIndex++
                }
                val chunkStr = chunk.joinToString("")

                // Align to 4-byte Base64 boundaries
                val base64Start = ((startPos + 3) / 4) * 4 - startPos
                val base64End = (pos / 4) * 4 - startPos

                if (
                    base64Start >= chunkStr.length
                    || base64End <= base64Start
                ) {
                    pos += spacing
                    continue
                }

                val b64Chunk = chunkStr.substring(base64Start, base64End)
                val decoded = Base64.Default.decode(b64Chunk)

                // Check for invalid UTF-8 bytes
                for (byte in decoded) {
                    val ubyte = byte.toUByte()
                    if (
                        (
                            ubyte < 32.toUByte() && ubyte !in listOf(
                                0x09.toUByte(),
                                0x0A.toUByte(),
                                0x0D.toUByte(),
                            )
                        )
                        || ubyte == 0xC0.toUByte()
                        || ubyte == 0xC1.toUByte()
                        || ubyte >= 0xF5.toUByte()
                    ) {
                        return false
                    }
                }

                pos += spacing
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deobfuscate using a known key.
     *
     * Reverses successive chunks of the string (using key digits as
     * chunk lengths), then Base64-decodes the result.
     *
     * Returns null if failed
     */
    fun deobfuscateRawcWithKey(rawc : String, key : List<Int>) : String? {
        return try {
            val buffer = rawc.toMutableList()
            var i = 0
            var segmentCount = 0

            // Reverse chunks based on key digits
            while (i < buffer.size - 1) {
                val segmentLength
                    = minOf(key[segmentCount % key.size], buffer.size - i)
                segmentCount++

                var left = i
                var right = i + segmentLength - 1
                while (left < right) {
                    val temp = buffer[left]
                    buffer[left] = buffer[right]
                    buffer[right] = temp
                    left++
                    right--
                }

                i += segmentLength
            }

            val reversedStr = buffer.joinToString("")
            base64Decode(reversedStr)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Brute-force deobfuscate obfuscated crossword puzzle data.
     */
    fun deobfuscateRawc(rawc : String) : String {
        val startTime = System.currentTimeMillis()
        val candidateQueue = ArrayDeque<List<Int>>(listOf(listOf()))

        while (candidateQueue.isNotEmpty()) {
            val timeSoFar = System.currentTimeMillis() - startTime
            if (timeSoFar > DEOBFUSCATE_TIMEOUT)
                return "{}"

            val candidateKeyPrefix = candidateQueue.removeFirst()

            if (candidateKeyPrefix.size == KEY_LEN) {
                val deobfuscated
                    = deobfuscateRawcWithKey(rawc, candidateKeyPrefix)
                if (deobfuscated != null) {
                    try {
                        JSONParser.parse(deobfuscated)
                        return deobfuscated
                    } catch (e : JSONException) {
                        continue
                    }
                }
            }

            // Expand by trying next digits (2-max)
            for (nextDigit in 2..MAX_KEY_VALUE) {
                val newCandidate = candidateKeyPrefix + nextDigit

                val remainingDigits = KEY_LEN - newCandidate.size
                val minSpacing = 2 * remainingDigits
                val maxSpacing = MAX_KEY_VALUE * remainingDigits

                // Test if any spacing within bounds produces valid output
                val validPrefix = (minSpacing..maxSpacing).any { spacing ->
                    isValidKeyPrefix(rawc, newCandidate, spacing)
                }
                if (validPrefix)
                    candidateQueue.addLast(newCandidate)
            }
        }

        return "{}"
    }

    ///////////////////////////////////////////////////////////////////////////
}
