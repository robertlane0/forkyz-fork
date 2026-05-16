
package app.crossword.yourealwaysbe.forkyz.puz.io.charset

import kotlinx.io.Sink

interface CharsetEncoder {
    fun encode(value : String, sink : Sink)
    fun encode(value : Char, sink : Sink)
}

