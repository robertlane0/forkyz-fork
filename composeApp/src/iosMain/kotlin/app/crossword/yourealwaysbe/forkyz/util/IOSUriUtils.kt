
package app.crossword.yourealwaysbe.forkyz.util

import platform.Foundation.NSURL

actual fun makePlatformUri(uri : String) : PlatformUri {
    var nsUrl = NSURL.URLWithString(uri)
    // default to file://
    if (nsUrl == null || nsUrl.scheme == null) {
        nsUrl = NSURL.fileURLWithPath(uri)
    }
    return PlatformUri(nsUrl)
}

actual data class PlatformUri(val url : NSURL) {
    actual fun getPath() : String? {
        return url.path
    }

    override fun toString() : String {
        return url.absoluteString ?: ""
    }
}
