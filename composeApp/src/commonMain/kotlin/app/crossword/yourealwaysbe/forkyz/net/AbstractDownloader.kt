
package app.crossword.yourealwaysbe.forkyz.net

import java.io.BufferedInputStream
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL

import app.crossword.yourealwaysbe.forkyz.util.getURLInputStream

/**
 * Base class for downloaders
 *
 * Provides some useful methods. Recommended to always use
 * getInputStream. If not, set a timeout of getTimeout().
 */
abstract class AbstractDownloader : Downloader {
    override var timeout : Int = 30000

    @Throws(IOException::class, URISyntaxException::class)
    protected suspend fun getInputStream(
        url : URL,
        headers : Map<String, String>? = null,
    ) : BufferedInputStream {
        return getURLInputStream(
            url,
            timeout,
            headers
        )
    }

    protected suspend fun getInputStream(
        url : String,
        headers : Map<String, String>? = null,
    ) : BufferedInputStream {
        return getURLInputStream(
            url,
            timeout,
            headers
        )
    }
}

