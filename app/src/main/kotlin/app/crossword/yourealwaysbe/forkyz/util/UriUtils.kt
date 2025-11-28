
package app.crossword.yourealwaysbe.forkyz.util

import android.net.Uri as AndroidUri
import androidx.core.net.toUri as toAndroidUri

/**
 * Drop-in wrapper for android.net.Uri
 *
 * Will be expect/actual for multiplatform.
 */
data class Uri(private val uri : AndroidUri) {
    companion object {
        fun parse(uri : String) : Uri {
            return Uri(uri.toAndroidUri())
        }
    }

    fun getPath() : String? {
        return uri.getPath()
    }

    override fun toString() : String {
        return uri.toString()
    }

    fun toAndroidUri() : AndroidUri {
        return uri
    }
}

fun String.toUri() : Uri {
    return Uri.parse(this)
}

fun AndroidUri.toForkyzUri() : Uri {
    return Uri(this)
}

