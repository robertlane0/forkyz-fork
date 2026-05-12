
package app.crossword.yourealwaysbe.forkyz.net

import kotlin.time.Duration
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.io.IOException

import app.crossword.yourealwaysbe.forkyz.puz.Puzzle
import app.crossword.yourealwaysbe.forkyz.puz.io.JPZIO
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONArray
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.puz.util.buildJSONObject
import app.crossword.yourealwaysbe.forkyz.util.AppLogger
import app.crossword.yourealwaysbe.forkyz.util.getURL
import app.crossword.yourealwaysbe.forkyz.util.getURLJSON

/**
 * King Digital Downloader for API with login
 *
 * I don't know what the "q" parameter stands for!
 *
 * Request login JSON:
 *
 *  t: json
 *  uid: username
 *  pwd: password
 *
 * Puzzle fetch JSON:
 *
 *  t: json
 *  k: key from login
 *  s: https://www.arkadium.com/games/premier-crossword-kingsfeatures/2025-4-20
 *  e: 2025-4-20
 *  q: PCC01
 *
 * @param username username for KingFeatures login for puzzle
 * @param password password for KingFeatures login for puzzle
 * @param requstSourcePrefix the pre-date prefix for s argument
 * @param q the q argument
 */
class KingDigitalLoginDownloader(
    crosswordSet : String,
    downloaderID : DownloaderID,
    downloaderName : String,
    days : Array<DayOfWeek>,
    utcAvailabilityOffset : Duration,
    supportUrl : String,
    sourceUrlPattern : String,
    shareUrlPattern : String,
    private val username : String,
    private val password : String,
    private val requestSourcePrefix : String,
    private val q : String,
) : AbstractDateDownloader(
    downloaderID,
    downloaderName,
    days,
    utcAvailabilityOffset,
    supportUrl,
    JPZIO(),
    sourceUrlPattern,
    shareUrlPattern,
) {
    private val TAG = "ForkyzKingDigitalLgnDL"

    private val LOGIN_URL = "https://kdig.kingfeatures.net/login/"
    private val DOWNLOAD_URL = "https://kdig.kingfeatures.net/"
    private val DATE_FORMATTER = LocalDate.Format {
        year()
        char('-')
        monthNumber(Padding.NONE)
        char('-')
        day(Padding.NONE)
    }

    override protected suspend fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        try {
            AppLogger.i(TAG, "Downloading " + name + " for date " + date)

            val key = getLoginKey(headers)
            if (key == null) {
                AppLogger.i("TAG", "Could not login to King Digital")
                return null
            }

            val assets = getPuzzleAssets(headers, date, key)
            if (assets ==  null) {
                AppLogger.i(TAG, "Could not get puzzle list from King Digital")
                return null
            }

            val url = getPuzURLForDate(assets, date)
            if (url == null) {
                AppLogger.i(TAG, "Did not find puzzle for " + date)
                return null
            }

            return getURL(
                url = url,
                timeoutMillis = timeout,
                headers = headers,
            ).use(JPZIO::readPuzzle)?.let { puz ->
                with (puz) {
                    this.date = date
                    this.source = name
                    this.sourceUrl = url
                    this.supportUrl = supportUrl
                    this.shareUrl = getShareUrl(date)
                }
                puz
            }
        } catch (e : IOException) {
            AppLogger.i(
                TAG,
                "Error downloading " + name + " for date " + date
                    + ": " + e
            );
            return null
        } catch (e : JSONException) {
            AppLogger.i(
                TAG,
                "Error downloading " + name + " for date " + date
                    + ": " + e
            );
            return null
        }
    }

    private suspend fun getLoginKey(
        headers : Map<String, String>? = null
    ) : String? {
        val loginJson = buildJSONObject {
            put("t", "json")
            put("uid", username)
            put("pwd", password)
        }

        val keys = getURLJSON(
            url = LOGIN_URL,
            timeout = timeout,
            headers = headers,
            data = loginJson,
        )

        return keys.getString("key")
    }

    private suspend fun getPuzzleAssets(
        headers : Map<String, String>? = null,
        date : LocalDate,
        key : String,
    ) : JSONArray? {
        // get all available puzzle assets
        val getPuzUrlJson = buildJSONObject {
            val dateStr = date.format(DATE_FORMATTER)
            put("t", "json")
            put("k", key)
            put("e", dateStr)
            put("s", requestSourcePrefix + dateStr)
            put("q", q)
        }

        val puzUrlResponse = getURLJSON(
            url = DOWNLOAD_URL,
            timeout = timeout,
            headers = headers,
            data = getPuzUrlJson
        )

        return puzUrlResponse.getJSONArray("assets")
    }

    private fun getPuzURLForDate(
        assets : JSONArray,
        date : LocalDate,
    ) : String? {
        for (i in 0 until assets.size) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("FILEEXT") == "jpz") {
                try {
                    val assetDate = asset.optString("PUBDATE") ?: ""
                    if (!assetDate.isEmpty()) {
                        val parsedDate = LocalDateTime.Formats.ISO.parse(
                            assetDate,
                        ).date
                        if (parsedDate == date) {
                            val url = asset.optString("ASSETLINK") ?: ""
                            if (!url.isEmpty())
                                return url
                        }
                    }
                } catch (e : IllegalArgumentException) {
                    // ignore
                }
            }
        }
        return null
    }
}

