
package app.crossword.yourealwaysbe.forkyz.util

expect class PlatformUri {
    fun getPath() : String?
}

expect fun makePlatformUri(uri : String) : PlatformUri

/**
 * Drop-in wrapper for android.net.Uri
 *
 * Will be expect/actual for multiplatform.
 */
data class Uri(private val uri : PlatformUri) {
    companion object {
        fun parse(uri : String) : Uri {
            return Uri(makePlatformUri(uri))
        }
    }

    fun getPath() : String? {
        return uri.getPath()
    }

    override fun toString() : String {
        return uri.toString()
    }

    fun getPlatformUri() : PlatformUri {
        return uri
    }
}

fun String.toUri() : Uri {
    return Uri.parse(this)
}

