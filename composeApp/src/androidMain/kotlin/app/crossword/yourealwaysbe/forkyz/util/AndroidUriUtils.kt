
package app.crossword.yourealwaysbe.forkyz.util

import android.net.Uri as AndroidUri
import androidx.core.net.toUri as toAndroidUri

actual fun makePlatformUri(uri : String) : PlatformUri {
    return PlatformUri(uri.toAndroidUri())
}

actual data class PlatformUri(val uri : AndroidUri) {
    actual fun getPath() : String? {
        return uri.getPath()
    }

    override fun toString() : String {
        return uri.toString()
    }
}

fun Uri.toAndroidUri() : AndroidUri {
    return this.getPlatformUri().uri
}

fun AndroidUri.toForkyzUri() : Uri {
    return Uri(PlatformUri(this))
}

