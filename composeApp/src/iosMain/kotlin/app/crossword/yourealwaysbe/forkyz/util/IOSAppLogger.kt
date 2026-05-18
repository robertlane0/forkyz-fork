
package app.crossword.yourealwaysbe.forkyz.util

import platform.Foundation.NSLog

actual object AppLogger {
    actual fun e(
        tag : String,
        message : String,
        throwable : Throwable?,
    ) {
        if (throwable != null) {
            NSLog("ERROR: [$tag] $message\n${throwable}")
        } else {
            NSLog("ERROR: [$tag] $message")
        }
    }

    actual fun d(tag : String, message : String) {
        NSLog("DEBUG: [$tag] $message")
    }

    actual fun i(tag : String, message : String) {
        NSLog("INFO: [$tag] $message")
    }

    actual fun w(
        tag : String,
        message : String,
        throwable : Throwable?,
    ) {
        if (throwable != null) {
            NSLog("WARN: [$tag] $message\n${throwable}")
        } else {
            NSLog("WARN: [$tag] $message")
        }
    }
}
