
package app.crossword.yourealwaysbe.forkyz.util

import java.util.logging.Level
import java.util.logging.Logger

// Borrowed from
// https://medium.com/@mohaberabi98/logging-in-compose-multiplatform-a5cf750dbce0

actual object AppLogger {
    private val logger : Logger = Logger.getLogger(AppLogger::class.java.name)

    actual fun e(
        tag : String,
        message : String,
        throwable : Throwable?
    ) {
        if (throwable != null) {
            logger.log(Level.SEVERE, "ERROR: [$tag] $message", throwable)
        } else {
            logger.severe("ERROR: [$tag] $message")
        }
    }

    actual fun d(tag : String, message : String) {
        logger.info("DEBUG: [$tag] $message")
    }

    actual fun i(tag : String, message : String) {
        logger.info("INFO: [$tag] $message")
    }

    actual fun w(
        tag : String,
        message : String,
        throwable : Throwable?
    ) {
        if (throwable != null) {
            logger.log(Level.WARNING, "ERROR: [$tag] $message", throwable)
        } else {
            logger.warning("ERROR: [$tag] $message")
        }
    }

}
