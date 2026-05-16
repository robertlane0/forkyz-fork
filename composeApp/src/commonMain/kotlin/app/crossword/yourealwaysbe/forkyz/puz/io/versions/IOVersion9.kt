
package app.crossword.yourealwaysbe.forkyz.puz.io.versions

import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CharsetDecoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.CharsetEncoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.UTF8Decoder
import app.crossword.yourealwaysbe.forkyz.puz.io.charset.UTF8Encoder

// Support utf-8
class IOVersion9 : IOVersion8() {
    override val charsetDecoder : CharsetDecoder = UTF8Decoder()
    override val charsetEncoder : CharsetEncoder = UTF8Encoder()
}
