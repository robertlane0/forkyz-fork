
package app.crossword.yourealwaysbe.forkyz.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URISyntaxException
import java.net.URL

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
import io.ktor.utils.io.jvm.javaio.toInputStream

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
suspend fun getURLInputStream(
    url : URL,
    timeoutMillis : Int,
    headers : Map<String, String>? = null,
    data : JSONObject? = null,
) : BufferedInputStream {
    val client = HttpClient(CIO) {
        engine {
            requestTimeout = timeoutMillis.toLong()
            endpoint {
                connectTimeout = timeoutMillis.toLong()
            }
        }
    }

    try {
        val response = client.request(url.toString()) {
            method = if (data == null) HttpMethod.Get else HttpMethod.Post
            headers?.forEach { (k, v) -> header(k, v) }
            header(HttpHeaders.Accept, "*/*")
            header(HttpHeaders.Connection, "close")

            if (data != null) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(data.toString())
            }
        }

        return BufferedInputStream(
            response.bodyAsChannel().toInputStream().withClientClose(client),
        )
    } catch (e: Exception) {
        throw IOException(e)
    } finally {
        client.close()
    }
}

@Throws(IOException::class, URISyntaxException::class)
suspend fun getURLInputStream(
    url : String,
    timeout : Int,
    headers : Map<String, String>? = null,
    data : JSONObject? = null,
) : BufferedInputStream  {
    return getURLInputStream(URI(url).toURL(), timeout, headers, data)
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
    return getURLInputStream(url, timeout, headers, data).use(::parseJSONObject)
}

internal fun InputStream.withClientClose(
    client : HttpClient,
) : InputStream = object : InputStream() {
    override fun read() : Int
        = this@withClientClose.read()

    override fun read(b : ByteArray, off : Int, len : Int) : Int
        = this@withClientClose.read(b, off, len)

    override fun close() {
        try {
            this@withClientClose.close()
        } finally {
            client.close()
        }
    }
}
