
package app.crossword.yourealwaysbe.forkyz.puz.io.charset

import kotlinx.io.Source

interface CharsetDecoder {
    fun decode(source : Source) : String
}

