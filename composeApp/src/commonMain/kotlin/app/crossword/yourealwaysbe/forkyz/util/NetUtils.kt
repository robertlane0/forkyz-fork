
package app.crossword.yourealwaysbe.forkyz.util

import kotlin.coroutines.cancellation.CancellationException
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.utils.io.asSource

import app.crossword.yourealwaysbe.forkyz.util.JSONException
import app.crossword.yourealwaysbe.forkyz.util.JSONObject
import app.crossword.yourealwaysbe.forkyz.util.StreamUtils
import app.crossword.yourealwaysbe.forkyz.util.parseJSONObject

private val HTTP_OK_RESPONSE = 200

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
    val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis.toLong()
            connectTimeoutMillis = timeoutMillis.toLong()
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
@Throws(IOException::class, JSONException::class, CancellationException::class)
suspend fun getURLJSON(
    url : String,
    timeout : Int,
    headers : Map<String, String>? = null,
    data : JSONObject? = null,
) : JSONObject {
    return getURL(url, timeout, headers, data).use(::parseJSONObject)
}

internal fun resolveUrl(base : String, relative : String) : String {
    return URLBuilder(base).apply { takeFrom(relative) }.buildString()
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
