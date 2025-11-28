
package app.crossword.yourealwaysbe.forkyz.net

import java.io.BufferedInputStream
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL

import app.crossword.yourealwaysbe.forkyz.util.apacheGetURLInputStream
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
    protected fun getInputStream(
        url: URL,
        headers : Map<String, String>?,
    ): BufferedInputStream {
        if (headers != null && headers.containsKey("Origin")) {
            // use apache as it allows setting origin via headers
            return BufferedInputStream(
                apacheGetURLInputStream(
                    url.toURI(),
                    timeout,
                    headers
                )
            )
        } else {
            // more efficient implementation
            return getURLInputStream(
                url,
                timeout,
                headers
            )
        }
    }
}

