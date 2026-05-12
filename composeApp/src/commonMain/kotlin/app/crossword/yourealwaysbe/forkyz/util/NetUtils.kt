
package app.crossword.yourealwaysbe.forkyz.util

import java.net.URL
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.utils.io.asSource

import app.crossword.yourealwaysbe.forkyz.puz.io.StreamUtils
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.puz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.puz.util.parseJSONObject

private val HTTP_OK_RESPONSE = 200

/**
 * Get a buffered source for URL
 *
 * @param url url to open
 * @param timeout the timeout in millis
 * @param headers extra request headers
 * @param data JSON data (will send as POST if not null)
 * @return a buffered input stream (in stream or err stream if error)
 * @throws IOException
 */
suspend fun getURL(
    url : URL,
    timeoutMillis : Int,
    headers : Map<String, String>? = null,
    data : JSONObject? = null,
) : Source {
    return getURL(url.toString(), timeoutMillis, headers, data)
}

/**
 * Get a source for URL
 *
 * @param url url to open
 * @param timeout the timeout in millis
 * @param headers extra request headers
 * @param data JSON data (will send as POST if not null)
 * @return a buffered input stream (in stream or err stream if error)
 * @throws IOException
 */
suspend fun getURL(
    url : String,
    timeoutMillis : Int,
    headers : Map<String, String>? = null,
    data : JSONObject? = null,
) : Source {
    val client = HttpClient(CIO) {
        engine {
            requestTimeout = timeoutMillis.toLong()
            endpoint {
                connectTimeout = timeoutMillis.toLong()
            }
        }
    }

    try {
        val response = client.request(url) {
            method = if (data == null) HttpMethod.Get else HttpMethod.Post
            headers?.forEach { (k, v) -> header(k, v) }
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.Connection, "close")

            if (data != null) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(data.toString())
            }
        }

        return response.bodyAsChannel()
            .asSource()
            .withClientClose(client)
            .buffered()
    } catch (e : Exception) {
        throw IOException(e)
    } finally {
        client.close()
    }
}

/**
 * Makes a JSON post request and reads JSON response
 */
@Throws(IOException::class, JSONException::class)
suspend fun getURLJSON(
    url : String,
    timeout : Int,
    headers : Map<String, String>? = null,
    data : JSONObject? = null,
) : JSONObject {
    return getURL(url, timeout, headers, data).use(::parseJSONObject)
}

internal fun RawSource.withClientClose(
    client : HttpClient,
) : RawSource = object : RawSource {
    override fun readAtMostTo(sink : Buffer, byteCount : Long) : Long
        = this@withClientClose.readAtMostTo(sink, byteCount)

    override fun close() {
        try {
            this@withClientClose.close()
        } finally {
            client.close()
        }
    }
}
