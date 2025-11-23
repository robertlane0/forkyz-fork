
package app.crossword.yourealwaysbe.forkyz.net

import java.io.IOException
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

import android.util.Log

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import app.crossword.yourealwaysbe.forkyz.util.getURLInputStream
import app.crossword.yourealwaysbe.forkyz.util.getURLJSON
import app.crossword.yourealwaysbe.puz.Puzzle
import app.crossword.yourealwaysbe.puz.io.JPZIO

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
    internalName : String,
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
    internalName,
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
    private val DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-M-d",
        Locale.US,
    )

    override protected fun download(
        date : LocalDate,
        headers : Map<String, String>?,
    ) : Puzzle? {
        try {
            Log.i(TAG, "Downloading " + name + " for date " + date)

            val key = getLoginKey(headers)
            if (key == null) {
                Log.i("TAG", "Could not login to King Digital")
                return null
            }

            val assets = getPuzzleAssets(headers, date, key)
            if (assets ==  null) {
                Log.i(TAG, "Could not get puzzle list from King Digital")
                return null
            }

            val url = getPuzURLForDate(assets, date)
            if (url == null) {
                Log.i(TAG, "Did not find puzzle for " + date)
                return null
            }

            return getURLInputStream(
                url = url,
                timeout = timeout,
                headers = headers,
            ).use(JPZIO::readPuzzle)?.let { puz ->
                with (puz) {
                    setDate(date)
                    setSource(name)
                    setSourceUrl(url)
                    setSupportUrl(getSupportUrl())
                    setShareUrl(getShareUrl(date))
                }
                puz
            }
        } catch (e : IOException) {
            Log.i(
                TAG,
                "Error downloading " + name + " for date " + date
                    + ": " + e
            );
            return null
        } catch (e : JSONException) {
            Log.i(
                TAG,
                "Error downloading " + name + " for date " + date
                    + ": " + e
            );
            return null
        }
    }

    private fun getLoginKey(
        headers : Map<String, String>? = null
    ) : String? {
        val loginJson = JSONObject()
        with (loginJson) {
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

        return keys?.getString("key")
    }

    private fun getPuzzleAssets(
        headers : Map<String, String>? = null,
        date : LocalDate,
        key : String,
    ) : JSONArray? {
        // get all available puzzle assets
        val getPuzUrlJson = JSONObject()
        with (getPuzUrlJson) {
            val dateStr = DATE_FORMATTER.format(date)
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

        return puzUrlResponse?.getJSONArray("assets")
    }

    private fun getPuzURLForDate(
        assets : JSONArray,
        date : LocalDate,
    ) : String? {
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("FILEEXT") == "jpz") {
                try {
                    asset.optString("PUBDATE")?.let { assetDate ->
                        LocalDate.parse(
                            assetDate,
                            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                        )
                    }?.let { assetDate ->
                        if (assetDate == date) {
                            val url = asset.optString("ASSETLINK")
                            if (url != null)
                                return url
                        }
                    }
                } catch (e : DateTimeParseException) {
                    // ignore
                }
            }
        }
        return null
    }
}
