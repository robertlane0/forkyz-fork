
package app.crossword.yourealwaysbe.forkyz.util

import java.net.URI

actual fun makePlatformUri(uri : String) : PlatformUri {
    return PlatformUri(URI(uri))
}

actual data class PlatformUri(val uri : URI) {
    actual fun getPath() : String? {
        return uri.getPath()
    }

    override fun toString() : String {
        return uri.toString()
    }
}

fun Uri.toJVMURI() : URI {
    return this.getPlatformUri().uri
}

