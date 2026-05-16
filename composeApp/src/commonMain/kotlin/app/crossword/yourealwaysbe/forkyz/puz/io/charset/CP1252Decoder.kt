
package app.crossword.yourealwaysbe.forkyz.puz.io.charset

import kotlinx.io.Source
import kotlinx.io.readUByte

class CP1252Decoder : CharsetDecoder {
    override fun decode(source : Source) : String {
        val sb = StringBuilder()
        while (!source.exhausted()) {
            sb.append(decodeByte(source.readUByte()))
        }
        return sb.toString()
    }

    companion object {
        private val CP1252_0X80_TO_0X9F = charArrayOf(
            '\u20AC', '\uFFFD', '\u201A', '\u0192', '\u201E', '\u2026',
            '\u2020', '\u2021', '\u02C6', '\u2030', '\u0160', '\u2039',
            '\u0152', '\uFFFD', '\u017D', '\uFFFD', '\uFFFD', '\u2018',
            '\u2019', '\u201C', '\u201D', '\u2022', '\u2013', '\u2014',
            '\u02DC', '\u2122', '\u0161', '\u203A', '\u0153', '\uFFFD',
            '\u017E', '\u0178',
        )

        private fun decodeByte(b : UByte) : Char {
            val i = b.toInt()
            return if (i in 0x80..0x9F)
                CP1252_0X80_TO_0X9F[i - 0x80]
            else
                i.toChar()
        }
    }
}

