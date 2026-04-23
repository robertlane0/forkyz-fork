
package app.crossword.yourealwaysbe.forkyz.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.core5.http.ClassicHttpRequest
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder

import app.crossword.yourealwaysbe.puz.io.StreamUtils
import app.crossword.yourealwaysbe.puz.util.JSONException
import app.crossword.yourealwaysbe.puz.util.JSONObject
import app.crossword.yourealwaysbe.puz.util.parseJSONObject

private val HTTP_OK_RESPONSE = 200

/**
 * Get a buffered input stream for URL
 *
 * @param url url to open
 * @param timeout the timeout in millis
 * @param headers extra request headers
 * @param data JSON data (will send as POST if not null)
 * @return a buffered input stream (in stream or err stream if error)
 * @throws IOException
 */
@Throws(IOException::class)
fun getURLInputStream(
    url : URL,
    timeout : Int,
    headers : Map<String, String>? = null,
    data : JSONObject? = null,
) : BufferedInputStream {
    val conn = url.openConnection()
    when (conn) {
        is HttpURLConnection -> {
            with (conn) {
                setConnectTimeout(timeout)
                setReadTimeout(timeout)
                setRequestProperty("Connection", "close")
                setRequestProperty("Accept","*/*")
                headers?.forEach { key, value ->
                    setRequestProperty(key, value)
                }
            }

            if (data != null) {
                with (conn) {
                    setRequestMethod("POST")
                    setRequestProperty(
                        "Content-Type", "application/json",
                    )
                    setDoOutput(true)
                    BufferedOutputStream(getOutputStream()).use { os ->
                        val bytes = data.toString().toByteArray()
                        os.write(bytes, 0, bytes.size)
                    }
                }
            }

            if (conn.getResponseCode() != HTTP_OK_RESPONSE) {
                return BufferedInputStream(conn.getErrorStream())
            } else {
                return BufferedInputStream(conn.getInputStream())
            }
        }
        else -> {
            return BufferedInputStream(conn.getInputStream())
        }
    }
}

@Throws(IOException::class, URISyntaxException::class)
fun getURLInputStream(
    url : String,
    timeout : Int,
    headers : Map<String, String>? = null,
    data : JSONObject? = null,
) : BufferedInputStream  {
    return getURLInputStream(URI(url).toURL(), timeout, headers, data)
}

@Throws(IOException::class)
fun apacheGetURLInputStream(
    url : URI,
    timeout : Int,
    headers : Map<String, String>? = null,
) : InputStream {
    HttpClients.createDefault().use { client ->
        val builder = ClassicRequestBuilder.get(url)
        headers?.forEach { key, value -> builder.addHeader(key, value) }
        val request = builder.build()
        return client.execute(request) { response ->
            response.getEntity().getContent().use {
                StreamUtils.copyInputStream(it)
            }
        }
    }
}

@Throws(IOException::class)
fun apacheGetURLInputStream(
    url : String,
    timeout : Int,
    headers : Map<String, String>? = null,
) : InputStream {
    return apacheGetURLInputStream(URI(url), timeout, headers)
}

/**
 * Makes a JSON post request and reads JSON response
 */
@Throws(IOException::class, JSONException::class)
fun getURLJSON(
    url : String,
    timeout : Int,
    headers : Map<String, String>? = null,
    data : JSONObject? = null,
) : JSONObject {
    return getURLInputStream(url, timeout, headers, data).use(::parseJSONObject)
}

