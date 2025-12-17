
package app.crossword.yourealwaysbe.forkyz.util

import android.util.Log

// Borrowed from
// https://medium.com/@mohaberabi98/logging-in-compose-multiplatform-a5cf750dbce0

actual object AppLogger {
    actual fun e(tag : String, message : String, throwable : Throwable?) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    actual fun d(tag : String, message : String) {
        Log.d(tag, message)
    }

    actual fun i(tag : String, message : String) {
        Log.i(tag, message)
    }

    actual fun w(tag : String, message : String, throwable : Throwable?) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
}
