
package app.crossword.yourealwaysbe.forkyz.util

import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered

class SourceUtils {
    companion object {
        fun getResource(path : String) : Source {
            return SourceUtils::class.java
                .getResourceAsStream(path)!!.asSource().buffered()
        }
    }
}
