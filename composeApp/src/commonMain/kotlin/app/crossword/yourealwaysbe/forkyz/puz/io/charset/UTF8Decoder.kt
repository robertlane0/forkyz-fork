
package app.crossword.yourealwaysbe.forkyz.puz.io.charset

import kotlinx.io.Source
import kotlinx.io.readString

class UTF8Decoder : CharsetDecoder {
    override fun decode(source : Source) : String {
        return source.readString()
    }
}

