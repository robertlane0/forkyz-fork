
package app.crossword.yourealwaysbe.forkyz.net

import kotlinx.io.IOException
import kotlinx.io.Source

import app.crossword.yourealwaysbe.forkyz.util.getURL

/**
 * Base class for downloaders
 *
 * Provides some useful methods. Recommended to always use
 * getURL. If not, set a timeout of getTimeout().
 */
abstract class AbstractDownloader : Downloader {
    override var timeout : Int = 30000

    protected suspend fun getURL(
        url : String,
        headers : Map<String, String>? = null,
    ) : Source {
        return getURL(url, timeout, headers)
    }
}

