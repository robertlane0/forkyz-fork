
package app.crossword.yourealwaysbe.forkyz.puz.io.charset

import kotlinx.io.Sink
import kotlinx.io.writeString

class UTF8Encoder : CharsetEncoder {
    override fun encode(value : String, sink : Sink) {
        sink.writeString(value)
    }

    override fun encode(value : Char, sink : Sink) {
        sink.writeString(value.toString())
    }
}

